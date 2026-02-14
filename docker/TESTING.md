# Testing Guide - Deployment Features

## Overview

This guide helps you test all the deployment features to ensure everything works as expected.

## Pre-Test Checklist

- [ ] SSH access to server is working
- [ ] Docker is installed and running on server
- [ ] GitHub Secrets are all configured
- [ ] Code is committed and ready to push
- [ ] You have access to GitHub Actions logs

## Test 1: First Deployment (Clean State)

**Purpose**: Verify deployment works when no container exists yet.

### Steps

1. SSH to server and clean up (if needed):
```bash
docker stop espacogeek espacogeek-old 2>/dev/null || true
docker rm espacogeek espacogeek-old 2>/dev/null || true
```

2. Push to master:
```bash
git add .
git commit -m "Test: First deployment"
git push origin master
```

3. Monitor GitHub Actions:
- [ ] Tests pass
- [ ] Docker image built
- [ ] Image pushed to GHCR
- [ ] SSH deployment starts

4. Expected output in logs:
```
[INFO] Starting deployment of espacogeek-backend:latest
[INFO] No existing container to backup
[INFO] No old container to rename
[INFO] Pulling image from GHCR...
[✓] Image pulled successfully
[INFO] Starting new container...
[✓] Container started with ID: ...
[INFO] Validating container health...
[✓] Container is healthy
[✓] DEPLOYMENT SUCCESSFUL
```

5. Verify on server:
```bash
docker ps | grep espacogeek
docker logs espacogeek | tail -20
curl http://localhost:8080/actuator/health
```

### Success Criteria
- ✓ GitHub Actions completes successfully
- ✓ Container is running
- ✓ Health endpoint responds
- ✓ No `espacogeek-old` container exists
- ✓ One backup exists in `~/espacogeek-backups/`

---

## Test 2: Update Deployment (Container Already Running)

**Purpose**: Verify backup, rename, and cleanup of old container.

### Steps

1. Make a code change and push:
```bash
echo "# Test update" >> README.md
git add .
git commit -m "Test: Update deployment"
git push origin master
```

2. Monitor GitHub Actions workflow

3. Verify during deployment:
```bash
# In another terminal, watch containers
watch docker ps -a

# You should see:
# - Old container stopped and renamed to espacogeek-old
# - New container created as espacogeek
# - Old container removed after health check passes
```

4. Expected deployment sequence in logs:
```
[✓] Environment file found
[INFO] Creating backup of old container...
[✓] Container backup created: ...espacogeek_backup_YYYYMMDD_HHMMSS.tar
[INFO] Renaming old container...
[✓] Old container renamed to espacogeek-old
[INFO] Pulling new image...
[✓] Image pulled successfully
[INFO] Starting new container...
[✓] Container started with ID: ...
[INFO] Validating container health...
[✓] Container is healthy
[INFO] Cleaning up old container...
[✓] Old container removed
[✓] DEPLOYMENT SUCCESSFUL
```

5. Verify on server:
```bash
docker ps -a | grep espacogeek
# Should only show "espacogeek" (running)
# Should NOT show "espacogeek-old"

ls -lh ~/espacogeek-backups/
# Should have 2 backups now

docker logs espacogeek | tail -5
```

### Success Criteria
- ✓ Old container backed up before changes
- ✓ Old container renamed to `espacogeek-old`
- ✓ New container created and running
- ✓ Health checks passed
- ✓ Old container removed after success
- ✓ Backup count increased
- ✓ Application responds normally

---

## Test 3: Health Check Failure (Simulated)

**Purpose**: Verify automatic rollback when container fails health check.

### Steps

1. SSH to server and prepare for test:
```bash
# Get current container ID
CURRENT_ID=$(docker ps --filter name=espacogeek -q)
echo "Current container: $CURRENT_ID"

# Note the health endpoint response time
curl -w "\nResponse time: %{time_total}s\n" http://localhost:8080/actuator/health
```

2. Modify Dockerfile to break the container temporarily:

   **Option A: Add artificial delay** (simulates slow startup)
   ```dockerfile
   # Add before ENTRYPOINT in Dockerfile.jvm
   RUN sleep 120  # This makes container take 2 minutes to start
   ```

   **Option B: Break environment** (simulates bad config)
   ```yaml
   # In GitHub Secrets, temporarily set invalid DB URL
   SPRING_DATASOURCE_URL: jdbc:mysql://invalid-host:3306/db
   ```

3. Push changes:
```bash
git add .
git commit -m "Test: Simulate deployment failure"
git push origin master
```

4. Monitor GitHub Actions and watch containers:
```bash
watch docker ps -a
```

5. Expected rollback sequence in logs:
```
[INFO] Validating container health (max 30 attempts)...
[INFO] Waiting for container to be ready (attempt 1/30)...
[INFO] Waiting for container to be ready (attempt 2/30)...
...
[✗] Container failed health check
[WARN] Starting rollback procedure...
[INFO] Stopping new container...
[INFO] Restoring old container from backup: espacogeek-old
[✓] Rollback successful: Old container restored
```

6. Verify on server:
```bash
docker ps -a | grep espacogeek
# Should show "espacogeek" running with OLD version

# Check it's really running
curl http://localhost:8080/actuator/health

# View what happened
docker logs espacogeek | tail -10
```

### Success Criteria
- ✓ New container fails health check
- ✓ Automatic rollback triggered
- ✓ Old container `espacogeek-old` exists
- ✓ Rollback renames it back to `espacogeek`
- ✓ Application continues serving (old version)
- ✓ GitHub Actions shows FAILURE
- ✓ Backup file preserved

### Cleanup
```bash
# Revert your breaking change
git revert HEAD
git push origin master
# Let it deploy again successfully
```

---

## Test 4: Backup Retention Policy

**Purpose**: Verify only last 5 backups are kept.

### Steps

1. Perform 8 deployments (requires code changes each time):
```bash
for i in {1..8}; do
  echo "# Deploy $i" >> README.md
  git add .
  git commit -m "Test deploy $i"
  git push origin master
  sleep 120  # Wait for deployment to complete
done
```

2. Check backup directory:
```bash
ls -1 ~/espacogeek-backups/ | wc -l
# Should show 5 (not 8)

ls -lht ~/espacogeek-backups/ | head -10
# Should show 5 most recent backups
```

3. Verify file sizes are consistent:
```bash
du -sh ~/espacogeek-backups/
# Should be ~5 × (your image size)
```

### Success Criteria
- ✓ Only 5 most recent backups exist
- ✓ Older backups automatically removed
- ✓ Disk space remains constant
- ✓ File naming follows YYYYMMDD_HHMMSS pattern

---

## Test 5: Manual Diagnostic Tool

**Purpose**: Verify the diagnostic script works correctly.

### Steps

1. SSH to server and run diagnostic:
```bash
bash ~/espacogeek-backups/../diagnose.sh
```

2. Expected output sections:
```
[✓] Docker daemon is running
[✓] Container 'espacogeek' is running
    ℹ Container ID: <ID>
    ℹ Status: running
    ℹ Started at: <timestamp>
    ℹ Memory usage: <usage>
[ℹ] No backup container found (last deployment was successful)
[✓] EspacoGeek images found
<application logs>
[✓] Health endpoint responded
[✓] Port 8080 is in use
[✓] Backup directory found
    ℹ Number of backups: 5
```

### Success Criteria
- ✓ Diagnostic runs without errors
- ✓ Shows all relevant information
- ✓ Container status accurate
- ✓ Health check visible
- ✓ Backup information correct

---

## Test 6: Port Conflict Resolution

**Purpose**: Verify deployment handles port conflicts.

### Steps

1. Start something on port 8080:
```bash
sudo nc -l -p 8080 &
```

2. Attempt deployment:
```bash
git add .
git commit -m "Test: Port conflict"
git push origin master
```

3. Expected behavior:
```
[INFO] Starting new container...
[✗] Failed to start container
Error response from daemon: Ports are not available...
[WARN] Starting rollback...
```

4. Cleanup and retry:
```bash
# Kill what's using port 8080
sudo lsof -i :8080 | grep -v PID | awk '{print $2}' | xargs kill -9

# Deployment should work now
```

### Success Criteria
- ✓ Deployment detects port conflict
- ✓ New container fails to start
- ✓ Automatic rollback triggered
- ✓ Old container restored

---

## Test 7: Database Connectivity

**Purpose**: Verify deployment validates database connection.

### Steps

1. SSH to server:
```bash
docker logs espacogeek | grep -i "datasource\|database\|connection"
```

2. Temporarily stop database (if applicable):
```bash
docker stop espacogeek-db 2>/dev/null || true
```

3. Attempt deployment:
```bash
git add .
git commit -m "Test: DB connectivity"
git push origin master
```

4. Monitor health check output:
```bash
# Logs should show health check failures
watch docker logs espacogeek
```

5. Expected behavior:
```
[WARN] Container status is: running
[INFO] Waiting for container to be ready...
[✗] Container failed health check
[WARN] Starting rollback...
```

6. Restart database and redeploy:
```bash
docker start espacogeek-db
# Wait a moment for DB to be ready
git add .
git commit -m "Test: Retry deployment"
git push origin master
```

### Success Criteria
- ✓ Health check detects DB issues
- ✓ Deployment doesn't mark as success without DB
- ✓ Rollback happens automatically
- ✓ After DB comes back, deployment succeeds

---

## Test 8: Performance & Timing

**Purpose**: Measure and verify deployment timing.

### Steps

1. Deploy and measure each step:
```bash
# In one terminal, watch deployment
watch -n 1 'docker ps -a --format "table {{.Names}}\t{{.Status}}"'

# In GitHub Actions, note the timestamps
```

2. Record timing:
```
- Tests: _____ seconds
- Build: _____ seconds
- Push: _____ seconds
- Backup: _____ seconds
- Pull: _____ seconds
- Container start: _____ seconds
- Health check: _____ seconds
- Cleanup: _____ seconds
Total: _____ seconds (should be ~2-3 minutes)
```

3. Compare multiple deployments to find variance

### Success Criteria
- ✓ Deployment completes in <3 minutes
- ✓ Each step completes within expected time
- ✓ Health checks respond within 2 seconds
- ✓ No significant slowdown over time

---

## Test 9: Concurrent Operations

**Purpose**: Verify deployment handles edge cases.

### Steps

1. Test rapid successive deployments:
```bash
git add . && git commit -m "Deploy A" && git push origin master &
sleep 10
git add . && git commit -m "Deploy B" && git push origin master &
wait
```

2. Monitor GitHub Actions - should handle gracefully with concurrency protection

3. SSH server and check status:
```bash
docker ps -a
ls -la ~/espacogeek-backups/
```

### Success Criteria
- ✓ Second deployment waits for first to complete (GitHub Actions concurrency)
- ✓ No container conflicts
- ✓ Clean state after both deployments
- ✓ Backups created correctly

---

## Test 10: Recovery Procedures

**Purpose**: Verify manual recovery procedures work.

### Steps

1. Simulate catastrophic failure:
```bash
docker stop espacogeek
docker rm espacogeek
rm .env.espacogeek
```

2. Manual recovery using diagnostic:
```bash
bash ~/espacogeek-backups/../diagnose.sh
# Will show: ✗ Container 'espacogeek' not found
```

3. Execute recovery from latest backup:
```bash
# Find latest backup
BACKUP=$(ls -t ~/espacogeek-backups/*.tar | head -1)
echo "Using: $BACKUP"

# Option 1: If you have docker image still available
docker run -d --name espacogeek \
  -p 8080:8080 \
  --restart unless-stopped \
  ghcr.io/YOUR-ORG/espacogeek-backend:latest

# Option 2: If you need to restore from tar backup
docker import "$BACKUP" espacogeek-restored:latest
docker run -d --name espacogeek \
  -p 8080:8080 \
  --restart unless-stopped \
  espacogeek-restored:latest
```

4. Verify recovery:
```bash
docker logs espacogeek
curl http://localhost:8080/actuator/health
```

### Success Criteria
- ✓ Container can be recovered from backups
- ✓ Application runs after restore
- ✓ Health endpoint responds

---

## Summary Test Matrix

| Test | Purpose | Pass/Fail | Notes |
|------|---------|-----------|-------|
| Test 1 | First deployment | ___ | |
| Test 2 | Update deployment | ___ | |
| Test 3 | Health check failure | ___ | |
| Test 4 | Backup retention | ___ | |
| Test 5 | Diagnostic tool | ___ | |
| Test 6 | Port conflict | ___ | |
| Test 7 | Database connectivity | ___ | |
| Test 8 | Performance timing | ___ | |
| Test 9 | Concurrent operations | ___ | |
| Test 10 | Recovery procedures | ___ | |

---

## Next Steps

After all tests pass:
1. ✓ Document any issues found
2. ✓ Update deployment procedures if needed
3. ✓ Schedule regular deployment drills
4. ✓ Monitor production deployments
5. ✓ Collect metrics for optimization

Enjoy your improved deployment process! 🚀

