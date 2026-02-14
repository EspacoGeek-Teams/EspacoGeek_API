# Quick Reference - Deployment Commands

## Container Management

### View Status
```bash
# See running containers
docker ps

# See all containers (including stopped)
docker ps -a

# See specific container
docker ps | grep espacogeek

# Get detailed info
docker inspect espacogeek

# View container resource usage
docker stats espacogeek
```

### View Logs
```bash
# Last 50 lines
docker logs espacogeek

# Follow logs in real-time
docker logs -f espacogeek

# Last 100 lines
docker logs --tail 100 espacogeek

# View logs with timestamps
docker logs -t espacogeek

# Since specific time
docker logs --since 2026-02-14T15:00:00 espacogeek
```

### Container Operations
```bash
# Start container
docker start espacogeek

# Stop container
docker stop espacogeek

# Restart container
docker restart espacogeek

# Remove container
docker rm espacogeek

# Force remove running container
docker rm -f espacogeek

# Execute command in container
docker exec espacogeek curl http://localhost:8080/actuator/health
```

## Backups & Restore

### View Backups
```bash
# List all backups
ls -lh ~/espacogeek-backups/

# Count backups
ls -1 ~/espacogeek-backups/*.tar | wc -l

# Show sizes
du -sh ~/espacogeek-backups/*

# Most recent first
ls -lht ~/espacogeek-backups/
```

### Restore from Backup
```bash
# Get latest backup
BACKUP=$(ls -t ~/espacogeek-backups/*.tar | head -1)

# Stop current container
docker stop espacogeek
docker rm espacogeek

# Import backup as image
docker import "$BACKUP" espacogeek:restored

# Create container from restored image
docker run -d --name espacogeek \
  -p 8080:8080 \
  --restart unless-stopped \
  --env-file .env.espacogeek \
  espacogeek:restored

# Verify
docker logs espacogeek
```

### Clean Backups
```bash
# Remove all backups except last 5
ls -t ~/espacogeek-backups/*.tar | tail -n +6 | xargs rm -f

# Remove all backups
rm -f ~/espacogeek-backups/*.tar

# Remove directory (if needed)
rm -rf ~/espacogeek-backups
```

## Image Management

### View Images
```bash
# List all images
docker images

# Show specific images
docker images | grep espacogeek

# Show image details
docker inspect ghcr.io/org/espacogeek-backend:latest

# Get image size
docker images --format "{{.Repository}}:{{.Tag}} {{.Size}}" | grep espacogeek
```

### Pull/Push Images
```bash
# Pull from GHCR
docker pull ghcr.io/ORG/espacogeek-backend:latest

# Tag image
docker tag ghcr.io/ORG/espacogeek-backend:latest espacogeek:latest

# Remove image
docker rmi ghcr.io/ORG/espacogeek-backend:latest

# Remove dangling images
docker image prune -f
```

### Docker Registry
```bash
# Login to GHCR
echo "YOUR_TOKEN" | docker login ghcr.io -u "YOUR_USERNAME" --password-stdin

# Logout
docker logout ghcr.io
```

## Network & Port Diagnostics

### Check Port Status
```bash
# Is port 8080 in use?
netstat -tuln | grep 8080

# What process is using port 8080?
lsof -i :8080

# Kill process using port
lsof -i :8080 | grep -v PID | awk '{print $2}' | xargs kill -9
```

### Container Networking
```bash
# Check container IP
docker inspect -f '{{.NetworkSettings.IPAddress}}' espacogeek

# See port mappings
docker port espacogeek

# Test connectivity from host
curl http://localhost:8080/actuator/health

# Test connectivity from container
docker exec espacogeek curl http://localhost:8080/actuator/health
```

## Database Diagnostics

### Check Connection
```bash
# Test DB connection from container
docker exec espacogeek wget -q -O- http://localhost:8080/actuator/health

# View DB logs (if DB is in container)
docker logs espacogeek-db

# Connect to DB directly
mysql -h HOST -u USER -p -e "SELECT 1"
```

## Environment & Configuration

### View Environment Variables
```bash
# Show all env vars in running container
docker inspect espacogeek --format='{{json .Config.Env}}' | jq

# Show specific env var
docker inspect espacogeek --format='{{index .Config.Env 0}}'

# View env file
cat .env.espacogeek
```

### Configuration
```bash
# Create env file
cat > .env.espacogeek << 'EOF'
SPRING_DATASOURCE_URL=...
SPRING_DATASOURCE_USERNAME=...
EOF

# Use env file when starting
docker run -d --env-file .env.espacogeek espacogeek

# Override specific env var
docker run -d -e JAVA_OPTS="-Xmx1g" espacogeek
```

## Deployment Scripts

### Run Deployment Manually
```bash
# Set variables
export GHCR_OWNER_LC="your-org"
export APP_NAME="espacogeek-backend"
export IMAGE_TAG="latest"

# Execute deployment script
/opt/espacogeek/deploy.sh "$GHCR_OWNER_LC" "$APP_NAME" "$IMAGE_TAG" ".env.espacogeek"
```

### Run Diagnostics
```bash
bash ~/espacogeek-backups/../diagnose.sh
```

## Emergency Procedures

### Hard Reset Everything
```bash
# Stop and remove all espacogeek containers
docker stop espacogeek espacogeek-old 2>/dev/null || true
docker rm espacogeek espacogeek-old 2>/dev/null || true

# Remove all backups
rm -f ~/espacogeek-backups/*.tar

# Remove env file
rm -f .env.espacogeek

# Clean Docker (optional)
docker system prune -f
```

### Manual Rollback
```bash
# If automated rollback failed
docker stop espacogeek 2>/dev/null || true
docker rm espacogeek 2>/dev/null || true

# Restore old version
docker rename espacogeek-old espacogeek
docker start espacogeek

# Verify
docker logs espacogeek
```

### Full Manual Deploy
```bash
# Login
echo "TOKEN" | docker login ghcr.io -u "USER" --password-stdin

# Create env file
cat > .env.espacogeek << 'EOF'
SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/db
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=pass
# ... other vars
EOF

# Pull image
docker pull ghcr.io/org/espacogeek-backend:latest

# Stop old container
docker stop espacogeek 2>/dev/null || true

# Remove old container
docker rm espacogeek 2>/dev/null || true

# Create new container
docker run -d --name espacogeek \
  -p 8080:8080 \
  --restart unless-stopped \
  --env-file .env.espacogeek \
  ghcr.io/org/espacogeek-backend:latest

# Verify
docker logs espacogeek
sleep 5
curl http://localhost:8080/actuator/health
```

## Performance Optimization

### Monitor Resource Usage
```bash
# Real-time stats
docker stats

# Stats for specific container
docker stats espacogeek

# Historical data (if available)
docker stats --no-stream
```

### Reduce Memory Usage
```bash
# Set memory limit
docker run -m 512m espacogeek

# Set CPU limit
docker run --cpus=1 espacogeek

# Update running container (requires recreate)
docker update -m 512m espacogeek
```

## Cleanup Commands

### Remove Unused Resources
```bash
# Remove stopped containers
docker container prune -f

# Remove dangling images
docker image prune -f

# Remove unused volumes
docker volume prune -f

# Full system cleanup (WARNING: removes much)
docker system prune -a -f
```

### Archive Old Backups
```bash
# Archive backups older than 30 days
find ~/espacogeek-backups -name "*.tar" -mtime +30 -exec tar -czf {}.gz {} \; -delete

# List archived files
ls -lh ~/espacogeek-backups/*.tar.gz
```

## Monitoring Checklist

### Daily
- [ ] Container is running: `docker ps | grep espacogeek`
- [ ] No errors in logs: `docker logs espacogeek | grep -i error`
- [ ] Health check passes: `curl http://localhost:8080/actuator/health`

### Weekly
- [ ] Backup count reasonable: `ls ~/espacogeek-backups | wc -l`
- [ ] Disk space sufficient: `df -h /`
- [ ] Run diagnostics: `bash ~/diagnose.sh`

### Monthly
- [ ] Review backup strategy
- [ ] Archive old backups
- [ ] Test restore procedure
- [ ] Review container resources

## GitHub Actions

### View Workflow Logs
- Go to: Repository → Actions tab
- Click on workflow run
- Expand deployment step
- Search for error messages

### Common Issues & Logs to Check
```
Issue: Image not pushed
Look for: "Push enabled: false"

Issue: Deployment didn't run
Look for: "needs.tests.result == 'success'"

Issue: SSH connection failed
Look for: "Connection refused"

Issue: Container won't start
Look for: "docker run" command output
```

## Useful Aliases

Add to `~/.bashrc` or `~/.zshrc`:

```bash
# Container shortcuts
alias ek-ps="docker ps -a | grep espacogeek"
alias ek-logs="docker logs espacogeek"
alias ek-health="curl http://localhost:8080/actuator/health"
alias ek-restart="docker restart espacogeek"
alias ek-stop="docker stop espacogeek"
alias ek-diagnose="bash ~/espacogeek-backups/../diagnose.sh"
alias ek-backups="ls -lh ~/espacogeek-backups/"

# Reload with: source ~/.bashrc
```

Then use:
```bash
ek-ps            # Show containers
ek-logs          # Show logs
ek-health        # Check health
ek-diagnose      # Run diagnostics
```

---

Need help? Check these files:
- `DEPLOYMENT.md` - Detailed deployment info
- `TESTING.md` - Test procedures
- `ARCHITECTURE.md` - System diagrams
- `SETUP.md` - Initial setup guide

