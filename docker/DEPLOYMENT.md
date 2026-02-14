# EspacoGeek Deployment Guide

## Overview

This document describes the improved deployment process for the EspacoGeek backend application on Hostinger.

## Deployment Features

### 1. **Container Existence Verification** ✓
- Before stopping or removing a container, the script verifies if it exists
- Uses `docker ps -a` to check container state
- Prevents errors when deploying for the first time or after manual cleanup

### 2. **Automatic Backup** ✓
- Creates a backup of the old container before deployment
- Backups are exported as `.tar` files in `~/espacogeek-backups/`
- Naming format: `espacogeek_backup_YYYYMMDD_HHMMSS.tar`
- **Keeps only the last 5 backups** to prevent disk space issues
- Backups can be used to restore container state if needed

### 3. **Smart Container Renaming** ✓
- Old container is renamed to `espacogeek-old` instead of being deleted immediately
- This allows for quick rollback if the new container fails
- Old container is only permanently removed after health checks pass

### 4. **Container Health Validation** ✓
- Validates that the new container is actually running
- Attempts to reach the health endpoint: `GET /actuator/health`
- **Max 30 attempts** with 2-second intervals (60 seconds total)
- Checks both container running status and application responsiveness
- Falls back gracefully if health endpoint is not available

### 5. **Automatic Rollback** ✓
- If health validation fails, automatically restores the old container
- Process:
  1. Stops and removes the new failing container
  2. Renames `espacogeek-old` back to `espacogeek`
  3. Restarts the old container
  4. Exits with error code to notify GitHub Actions

## Deployment Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Verify environment file exists                           │
├─────────────────────────────────────────────────────────────┤
│ 2. Backup old container (if exists)                         │
│    └─> Exports to ~/espacogeek-backups/espacogeek_backup_*  │
├─────────────────────────────────────────────────────────────┤
│ 3. Rename old container to espacogeek-old                   │
│    └─> Allows quick rollback                                │
├─────────────────────────────────────────────────────────────┤
│ 4. Pull new image from GHCR                                 │
│    └─> Exits if pull fails                                  │
├─────────────────────────────────────────────────────────────┤
│ 5. Start new container                                      │
│    └─> Exits if start fails, triggers rollback              │
├─────────────────────────────────────────────────────────────┤
│ 6. Validate container health (30 attempts)                  │
│    ├─> Check if container is running                        │
│    ├─> Check /actuator/health endpoint                      │
│    └─> If FAIL: Trigger rollback                            │
├─────────────────────────────────────────────────────────────┤
│ 7. Cleanup old container (if health OK)                     │
│    └─> Permanently removes espacogeek-old                   │
├─────────────────────────────────────────────────────────────┤
│ 8. Success: Show status and logs                            │
└─────────────────────────────────────────────────────────────┘
```

## Directory Structure

After deployments, you'll have:

```
~/espacogeek-backups/
├── espacogeek_backup_20260214_150130.tar  (newest)
├── espacogeek_backup_20260214_145000.tar
├── espacogeek_backup_20260214_140000.tar
├── espacogeek_backup_20260214_130000.tar
└── espacogeek_backup_20260213_120000.tar  (oldest kept)
```

## Manual Operations

### View Current Container Status
```bash
docker ps -a | grep espacogeek
```

### View Current Logs
```bash
docker logs espacogeek
docker logs espacogeek-old  # if rollback happened
```

### Restore from Backup
```bash
# Stop current container
docker stop espacogeek
docker rm espacogeek

# Import from backup
docker import ~/espacogeek-backups/espacogeek_backup_YYYYMMDD_HHMMSS.tar espacogeek-restored:latest

# Create new container from imported image
docker run -d --name espacogeek \
  -p 8080:8080 \
  --restart unless-stopped \
  --env-file .env.espacogeek \
  espacogeek-restored:latest
```

### Manual Rollback
```bash
# If deployment failed but automated rollback didn't work:
docker stop espacogeek
docker rm espacogeek
docker rename espacogeek-old espacogeek
docker start espacogeek
```

## Environment Variables

The deployment uses these environment variables from GitHub Secrets:

- `SPRING_DATASOURCE_URL` - Database connection URL
- `SPRING_DATASOURCE_USERNAME` - Database user
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `SPRING_MVC_CORS_ALLOWED_ORIGINS` - CORS origins
- `SECURITY_JWT_ISSUER` - JWT issuer
- `SECURITY_JWT_EXPIRATION_MS` - JWT expiration time
- `SECURITY_JWT_SECRET` - JWT secret key
- `SAMESITE_WHEN_SAME_SITE` - SameSite cookie setting
- `ALLOWED_ORIGINS` - Allowed origins for API
- `MAIL_HOST` - Email server host
- `MAIL_PORT` - Email server port
- `MAIL_USERNAME` - Email server username
- `MAIL_PASSWORD` - Email server password
- `FRONTEND_URL` - Frontend URL

## GitHub Actions Integration

The CI/CD pipeline (`cicd.yml`) triggers deployment when:
- ✓ All tests pass
- ✓ Building and pushing to GHCR succeeds
- ✓ Event is a push to master or manual workflow dispatch

Deployment is skipped for:
- ✗ Pull requests (only build, no deploy)
- ✗ Failed tests
- ✗ Failed image build

## Monitoring & Alerts

Currently, GitHub Actions logs show:
- Deployment progress with color-coded output
- Success/failure status
- Container health check results
- Rollback notifications (if applicable)

### Future Improvements
- Email notifications on deployment
- Slack/Discord notifications
- Deployment history dashboard
- Automated alerts for failed health checks

## Troubleshooting

### Issue: "Failed to pull image"
- Check GitHub Secrets for `GHCR_TOKEN` and `GHCR_USER`
- Verify the image was actually pushed to GHCR
- Check GHCR repository settings

### Issue: "Container failed health check"
- Check application logs: `docker logs espacogeek`
- Verify environment variables are correct
- Check if Spring Boot is actually starting
- Ensure database connection is working

### Issue: "Failed to start container"
- Check if port 8080 is already in use: `docker ps`
- Verify environment file exists and is readable
- Check Docker daemon status

### Issue: Manual cleanup needed
```bash
# Force remove all espacogeek containers
docker rm -f $(docker ps -a --filter name=espacogeek -q)

# Clean backups
rm -f ~/espacogeek-backups/*.tar
```

## Performance Notes

- Backup export: ~5-30 seconds (depends on container size)
- Image pull: ~30-60 seconds (depends on image size and network)
- Container startup: ~10-30 seconds
- Health check wait: up to 60 seconds
- **Total deployment time: ~2-3 minutes**

## Security Considerations

- Environment file (`.env.espacogeek`) is created fresh each deployment
- Environment file is deleted after container startup
- Backups are stored in user home directory
- Consider setting up automatic backup retention policies

