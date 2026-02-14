# Deployment Architecture & Flow

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        GitHub Actions (CI/CD)                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  1. Run Tests                                                        │
│  2. Build Docker Image (JVM & Native)                              │
│  3. Push to GHCR (ghcr.io/org/espacogeek-backend:latest)           │
│  4. Trigger SSH Deployment                                         │
│                                                                       │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             │ SSH Connection
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Hostinger Server                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ Docker Daemon                                               │   │
│  │                                                             │   │
│  │  ┌─────────────────────┐         ┌──────────────────────┐ │   │
│  │  │  Container Status   │         │ Network              │ │   │
│  │  │                     │         │                      │ │   │
│  │  │  [OLD] espacogeek-  │         │ Port 8080           │ │   │
│  │  │   old (backup)      │         │ ↓                   │ │   │
│  │  │                     │         │ [NEW] espacogeek    │ │   │
│  │  │  Running: 1.0-old   │         │ Running: 1.0-new    │ │   │
│  │  │  Can be restored    │         │ Health: ✓           │ │   │
│  │  └─────────────────────┘         └──────────────────────┘ │   │
│  │                                                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ Filesystem                                                  │   │
│  │                                                             │   │
│  │  ~/espacogeek-backups/                                     │   │
│  │  ├── espacogeek_backup_20260214_150130.tar  (5 kept)      │   │
│  │  ├── espacogeek_backup_20260214_145000.tar                │   │
│  │  ├── espacogeek_backup_20260214_140000.tar                │   │
│  │  ├── espacogeek_backup_20260214_130000.tar                │   │
│  │  └── espacogeek_backup_20260213_120000.tar (oldest)       │   │
│  │                                                             │   │
│  │  .env.espacogeek (created, then deleted after deploy)     │   │
│  │  deploy.sh (downloaded from GitHub first time)            │   │
│  │  /opt/espacogeek/deploy.sh (persistent)                   │   │
│  │                                                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

## Deployment State Transitions

### Scenario: Normal Update (Container Already Running)

```
BEFORE DEPLOYMENT
═════════════════

┌─────────────────────────┐
│ Container: espacogeek   │
│ Status: Running         │
│ Version: 1.0 (old)      │
│ Health: ✓               │
├─────────────────────────┤
│ • Serving requests      │
│ • Backup available      │
│ • No backup container   │
└─────────────────────────┘


DURING DEPLOYMENT (Step 1-3)
════════════════════════════

1️⃣  Backup Created
   └─ espacogeek_backup_TIMESTAMP.tar

2️⃣  Container Renamed

   ┌─────────────────────────┐
   │ Container: espacogeek-old (renamed)
   │ Status: Stopped         │
   │ Version: 1.0 (old)      │
   │ Backup: Ready for restore
   └─────────────────────────┘


DURING DEPLOYMENT (Step 4-5)
════════════════════════════

3️⃣  New Container Created

   ┌─────────────────────────┐         ┌─────────────────────────┐
   │ espacogeek-old (backup) │         │ espacogeek (new)        │
   │ Status: Stopped         │         │ Status: Starting        │
   │ Version: 1.0 (old)      │         │ Version: 1.0 (new)      │
   │                         │         │ Health: Checking...     │
   └─────────────────────────┘         └─────────────────────────┘


DURING DEPLOYMENT (Step 6)
═════════════════════════

4️⃣  Health Checks (30 attempts)

   ✓ Container running?
   ✓ Database connected?
   ✓ Spring Boot started?
   ✓ /actuator/health responding?


   SUCCESS PATH              │  FAILURE PATH
   ──────────────────────────┼──────────────────────────
   All checks pass ✓         │  Health check fails ✗
   │                          │  │
   ▼                          ▼
   Proceed to cleanup         Trigger rollback:
                              • Stop new container
                              • Remove new container
                              • Rename old back to espacogeek
                              • Start old container
                              • Exit with error


AFTER DEPLOYMENT (SUCCESS)
═══════════════════════════

Step 7️⃣  Cleanup Old Container

   ┌─────────────────────────┐
   │ Container: espacogeek   │
   │ Status: Running         │
   │ Version: 1.0 (new)      │
   │ Health: ✓               │
   ├─────────────────────────┤
   │ • Serving requests      │
   │ • Backup available      │
   │ • No backup container   │
   └─────────────────────────┘

   OLD backup removed (cleanup)


AFTER DEPLOYMENT (ROLLBACK)
═══════════════════════════

Automatic Recovery:

   ┌─────────────────────────┐
   │ Container: espacogeek   │
   │ Status: Running         │
   │ Version: 1.0 (old)      │
   │ Health: ✓               │
   ├─────────────────────────┤
   │ • Restored from backup  │
   │ • Serving requests      │
   │ • Backup available      │
   │ • ERROR reported to CI  │
   └─────────────────────────┘
```

## Deployment Timeline

```
TIME    ACTION                          DURATION    STATUS
────────────────────────────────────────────────────────────
0s      GitHub Actions triggered
        └─ Tests start                           ⏳ Running

15s     Tests complete                  ~15s      ✓ Pass
        └─ Build Docker image starts             ⏳ Running

45s     Image build complete            ~30s      ✓ Built
        └─ Push to GHCR starts                   ⏳ Uploading

75s     Push to GHCR complete          ~30s      ✓ Pushed
        └─ SSH deployment triggered              ⏳ Connecting

80s     SSH connected
        └─ Create backup                        ⏳ Exporting

90s     Backup complete                ~10s      ✓ Backed up
        └─ Rename old container                 ⏳ Renaming

92s     Old container renamed           ~2s       ✓ Renamed
        └─ Pull new image                       ⏳ Pulling

120s    Image pulled                    ~28s      ✓ Pulled
        └─ Start new container                  ⏳ Starting

132s    Container started               ~12s      ✓ Started
        └─ Health checks begin                  ⏳ Validating

142s    Health checks pass              ~10s      ✓ Healthy
        └─ Cleanup old container                ⏳ Removing

145s    Cleanup complete                ~3s       ✓ Cleaned
        └─ Deployment finished                  ✓ SUCCESS

TOTAL DEPLOYMENT TIME: ~2-3 minutes
```

## File Lifecycle During Deployment

```
PHASE 1: PREPARATION
═════════════════════

GitHub Actions                  Hostinger Server
────────────────────────────   ──────────────────
Create env file
└─ .env.espacogeek (GitHub)
   Contains secrets

                               Create env file locally
                               └─ .env.espacogeek
                                  (contains secrets)


PHASE 2: BACKUP & RENAME
═════════════════════════

                               Backup created
                               └─ ~/espacogeek-backups/
                                  └─ espacogeek_backup_
                                     YYYYMMDD_HHMMSS.tar

                               Container renamed
                               └─ espacogeek → espacogeek-old


PHASE 3: DEPLOY
════════════════

                               Pull & start new container
                               └─ espacogeek (running)


PHASE 4: VALIDATE
═══════════════════

                               Health checks
                               └─ curl http://localhost:8080/actuator/health


PHASE 5: CLEANUP
═════════════════

                               Old container removed
                               └─ espacogeek-old → deleted

                               Env file deleted
                               └─ .env.espacogeek → deleted

                               Keep backups
                               └─ Last 5 backups retained
```

## Database Connection Flow

```
Old Container (espacogeek)     Database
├─ Spring Boot running         │
├─ Connected to MySQL          ├─ Active connection
├─ Serving requests            │
└─ STOP signal received        │
   │                           │
   │ (connection close)        │
   └──────────────────────────►│ Connection closed
                               │
                               │
New Container (espacogeek)     │
├─ Spring Boot starting        │
├─ Loading config              │
├─ Connecting to MySQL         │
│  └─ SPRING_DATASOURCE_URL    │
│  └─ SPRING_DATASOURCE_USER   │
│  └─ SPRING_DATASOURCE_PASS   │
│                              │
│  (connection established)    │
└──────────────────────────────┤
   ├─ Connected!               │ New connection active
   ├─ Serving requests         │
   └─ Ready                    │
```

## Failure Recovery Diagram

```
Normal Deployment                Deployment with Failure
══════════════════════          ════════════════════════

✓ Pull image                    ✓ Pull image
  │                              │
✓ Start container                ✓ Start container
  │                              │
✓ Health check pass              ✗ Health check FAIL
  │                              │
✓ Remove old container           ╱─ Automatic Recovery ─╲
  │                             │                       │
✓ Success                        ✗ Stop new container    │
  │                             ✗ Remove new container   │
  │                             ✓ Rename old back       │
  └─ Application: v1.0 new      ✓ Start old container   │
                                │                       │
                                ✓ Success (old version) │
                                 ╲───────────────────────╱
                                 │
                                 └─ Application: v1.0 old
                                    (Rollback Active)
```

## Storage Impact Over Time

```
Week 1: First Deployments
────────────────────────────
Deployment 1 → backup_001.tar (150 MB)
Deployment 2 → backup_002.tar (150 MB)
Deployment 3 → backup_003.tar (150 MB)
Deployment 4 → backup_004.tar (150 MB)
Deployment 5 → backup_005.tar (150 MB)
Total: 750 MB
└─ Only 5 most recent backups kept


Week 2: Continuing Deployments
────────────────────────────────
Deployment 6 → backup_006.tar (150 MB)
             └─ backup_001.tar deleted (oldest removed)
Deployment 7 → backup_007.tar (150 MB)
             └─ backup_002.tar deleted
...

Steady State: Always 5 backups max
Disk usage: ~750 MB - 1 GB (constant)
└─ Automatic rotation maintains size
```

## Monitoring Dashboard (Recommended Future)

```
┌──────────────────────────────────────────────────────────────┐
│              EspacoGeek Deployment Dashboard                 │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Last Deployment: 2026-02-14 15:30:00                      │
│  Status: ✓ SUCCESS                                          │
│  Duration: 2m 45s                                           │
│  Version: v1.0.0                                            │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Container Status                                       │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ Container: espacogeek                                 │ │
│  │ Status: Running ✓                                     │ │
│  │ Uptime: 2h 15m                                        │ │
│  │ Memory: 256 MB / 512 MB                               │ │
│  │ CPU: 1.2%                                             │ │
│  │ Health: ✓ OK (response time: 42ms)                    │ │
│  │ Requests/min: 1,240                                   │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Backup Status                                          │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ Backups Stored: 5                                      │ │
│  │ Storage Used: 750 MB                                   │ │
│  │ Available Space: 9.2 GB                                │ │
│  │ Latest Backup: 2026-02-14 15:20:00 (150 MB)          │ │
│  │ Auto-Cleanup: Enabled (5 backups max)                │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Deployment History (Last 10)                          │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ 1. 2026-02-14 15:30 - v1.0.0 - SUCCESS ✓             │ │
│  │ 2. 2026-02-14 14:00 - v1.0.0-rc1 - SUCCESS ✓         │ │
│  │ 3. 2026-02-14 12:30 - v0.9.9 - ROLLBACK ⚠️           │ │
│  │ 4. 2026-02-14 11:00 - v0.9.8 - SUCCESS ✓             │ │
│  │ 5. 2026-02-13 15:20 - v0.9.7 - SUCCESS ✓             │ │
│  │ ...                                                    │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

