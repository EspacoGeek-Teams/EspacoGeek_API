# Deployment Improvements Checklist

## What Was Implemented ✅

### 1. Container Existence Verification
- [x] Script checks if container exists before operations
- [x] No errors if container doesn't exist (first deployment)
- [x] Uses `docker ps -a` to verify state

### 2. Automatic Backup
- [x] Old container exported to tar file before deployment
- [x] Backups stored in `~/espacogeek-backups/`
- [x] Named with timestamp: `espacogeek_backup_YYYYMMDD_HHMMSS.tar`
- [x] Only last 5 backups kept (automatic cleanup)

### 3. Smart Container Renaming
- [x] Old container renamed to `espacogeek-old` (not immediately deleted)
- [x] Allows quick rollback if new container fails
- [x] Permanently removed only after health checks pass

### 4. Container Health Validation
- [x] Validates that new container is running
- [x] Checks Spring Boot `/actuator/health` endpoint
- [x] Max 30 attempts with 2-second intervals (up to 60 seconds)
- [x] Graceful fallback if health endpoint unavailable

### 5. Automatic Rollback
- [x] If health check fails, automatically restores old container
- [x] Stops and removes failing new container
- [x] Renames `espacogeek-old` back to `espacogeek`
- [x] Restarts old container
- [x] Notifies GitHub Actions with error code

## Files Created/Modified

### New Files
- ✅ `docker/deploy.sh` - Improved deployment script with all features
- ✅ `docker/DEPLOYMENT.md` - Comprehensive deployment documentation
- ✅ `docker/diagnose.sh` - Diagnostic tool for troubleshooting

### Modified Files
- ✅ `.github/workflows/cicd.yml` - Updated to use enhanced deployment flow

## Before Deployment - Setup

### 1. Configure GitHub Secrets
Make sure these secrets are set in your GitHub repository:

```
HOSTINGER_HOST          → Your server IP/domain
HOSTINGER_USER          → SSH username
HOSTINGER               → SSH private key (or password)
HOSTINGER_PORT          → SSH port (default 22)

GHCR_TOKEN              → GitHub token (for GHCR authentication)
GHCR_USER               → GitHub username

SPRING_DATASOURCE_URL   → Database URL
SPRING_DATASOURCE_USERNAME → Database user
SPRING_DATASOURCE_PASSWORD → Database password

# ... (other Spring Boot configs from DEPLOYMENT.md)
```

### 2. Server Prerequisites
Make sure the server has:
- ✓ Docker installed and running
- ✓ Sufficient disk space (~10GB recommended)
- ✓ Port 8080 available and open
- ✓ SSH access configured

### 3. Create Initial Backup Directory
First time setup on server:
```bash
mkdir -p ~/espacogeek-backups
chmod 755 ~/espacogeek-backups
```

## First Deployment

### Step 1: Push to Master
```bash
git add .
git commit -m "Deploy improved CI/CD pipeline"
git push origin master
```

### Step 2: Monitor GitHub Actions
- Go to repository → Actions tab
- Watch the workflow run
- Should see color-coded output with deployment steps

### Step 3: Verify Deployment
After successful deployment:

```bash
# SSH into server
ssh -p PORT USER@HOST

# Check container status
docker ps -a | grep espacogeek

# View logs
docker logs espacogeek

# Test application
curl http://localhost:8080/actuator/health
```

## Testing Scenarios

### Scenario 1: Normal Deployment (No Previous Container)
1. Push to master
2. Tests run ✓
3. Images built and pushed ✓
4. Deployment creates new container ✓
5. Health checks pass ✓
6. Application ready ✓

### Scenario 2: Update Deployment (Container Already Running)
1. Push to master
2. Tests run ✓
3. Images built and pushed ✓
4. Old container backed up ✓
5. Old container renamed to `espacogeek-old` ✓
6. New container created ✓
7. Health checks pass ✓
8. Old container removed ✓
9. Application ready ✓

### Scenario 3: Failed Deployment (Health Check Fails)
1. Push to master
2. Tests run ✓
3. Images built and pushed ✓
4. Old container backed up ✓
5. Old container renamed ✓
6. New container created ✓
7. Health checks FAIL ✗
8. Automatic rollback: ✓
   - New container stopped and removed
   - Old container renamed back to `espacogeek`
   - Old container restarted
9. Application running on old version ✓
10. GitHub Actions shows FAILURE ✗

### Scenario 4: Manual Rollback (If Needed)
```bash
ssh -p PORT USER@HOST

# Stop current container
docker stop espacogeek
docker rm espacogeek

# Restore old container
docker rename espacogeek-old espacogeek
docker start espacogeek

# Verify
docker logs espacogeek
```

## Monitoring & Alerts

### View Current Status
```bash
# SSH to server
ssh -p PORT USER@HOST

# Run diagnostics
bash ~/espacogeek-backups/../diagnose.sh

# Or individual checks:
docker ps -a | grep espacogeek
docker logs espacogeek
docker inspect espacogeek --format='{{.State}}'
```

### Available Backups
```bash
ls -lh ~/espacogeek-backups/
```

### Restore from Backup
```bash
# If needed to restore an older version
docker stop espacogeek
docker rm espacogeek
docker import ~/espacogeek-backups/espacogeek_backup_YYYYMMDD_HHMMSS.tar espacogeek:restored
docker run -d --name espacogeek \
  -p 8080:8080 \
  --restart unless-stopped \
  --env-file .env.espacogeek \
  espacogeek:restored
```

## Performance Impact

### Deployment Time
- Backup export: 5-30 seconds
- Image pull: 30-60 seconds
- Container startup: 10-30 seconds
- Health checks: up to 60 seconds
- **Total: ~2-3 minutes**

### Storage Impact
- Each backup: ~100-500 MB (depends on image size)
- Last 5 backups: ~500 MB - 2.5 GB
- Consider purging old backups if space is limited

## Troubleshooting

### Health Check Timeout
If container takes too long to start:
1. Check application logs: `docker logs espacogeek`
2. Verify database connection
3. Increase health check timeout in `deploy.sh` (change `max_attempts`)

### Rollback Didn't Work
If automated rollback failed:
1. Check backup container: `docker ps -a | grep espacogeek-old`
2. Manual restore:
```bash
docker rename espacogeek-old espacogeek
docker start espacogeek
```

### Port Already in Use
```bash
# Find what's using port 8080
lsof -i :8080
# Or: netstat -tuln | grep 8080

# Kill the process or remove conflicting container
docker ps | grep 8080
docker rm -f <container-id>
```

## Next Steps

1. ✓ Review the deployment script in `docker/deploy.sh`
2. ✓ Read `docker/DEPLOYMENT.md` for full documentation
3. ✓ Ensure all GitHub Secrets are configured
4. ✓ Prepare the server with prerequisites
5. ✓ Do a test deployment to master
6. ✓ Monitor first deployment carefully
7. ✓ Keep backup directory clean with rotation policy

## Support

For issues or questions:
1. Check `docker/DEPLOYMENT.md` for detailed documentation
2. Review GitHub Actions logs for deployment errors
3. SSH to server and run `diagnose.sh` for system status
4. Check Docker logs: `docker logs espacogeek`

## Emergency Procedures

### Complete Restart (if all else fails)
```bash
ssh -p PORT USER@HOST

# Stop everything
docker stop espacogeek espacogeek-old 2>/dev/null || true
docker rm espacogeek espacogeek-old 2>/dev/null || true

# Clean up
rm -f .env.espacogeek

# Manual redeploy
docker login ghcr.io
docker pull ghcr.io/YOUR-ORG/espacogeek-backend:latest
docker run -d --name espacogeek \
  -p 8080:8080 \
  --restart unless-stopped \
  ghcr.io/YOUR-ORG/espacogeek-backend:latest
```

Good luck with your deployments! 🚀

