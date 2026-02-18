# EspacoGeek

<div align="center">
  <a href="https://www.buymeacoffee.com/vitorhugo1207">
    <img src="https://img.buymeacoffee.com/button-api/?text=Buy%20me%20a%20coffee&emoji=&slug=vitorhugo1207&button_colour=FFDD00&font_colour=000000&font_family=Cookie&outline_colour=000000&coffee_colour=ffffff" />
  </a>
</div>

## Usage
 See the GraphQL guide: [GraphQL Guide](graphql_guide.md)
 See the license: [Licenses](LICENSE.txt)

**Other Docs**: quick links to other markdown files in the project
- `INDEX.md`: Project index and quick references - [INDEX.md](INDEX.md)
- `MONITORING.md`: Monitoring & observability notes - [MONITORING.md](MONITORING.md)
- `docker/ARCHITECTURE.md`: Docker / architecture notes - [docker/ARCHITECTURE.md](docker/ARCHITECTURE.md)
- `docker/DEPLOYMENT.md`: Docker deployment guide - [docker/DEPLOYMENT.md](docker/DEPLOYMENT.md)
- `docker/QUICK_REFERENCE.md`: Docker quick reference - [docker/QUICK_REFERENCE.md](docker/QUICK_REFERENCE.md)
- `docker/SETUP.md`: Docker setup guide - [docker/SETUP.md](docker/SETUP.md)
- `docker/TESTING.md`: Docker testing notes - [docker/TESTING.md](docker/TESTING.md)
- `graphql_guide.md`: GraphQL guide - [graphql_guide.md](graphql_guide.md)
## Setup

### Run & Build Guide (JVM and Native)


## Batch Endpoint

This project exposes a simple administrative Batch API for running and controlling Spring Batch jobs at runtime. The controller is available at the path `/admin/batch` and provides three endpoints:

- **Run job**: `GET /admin/batch/run/{jobName}`
  - Description: Triggers a job bean (by Spring bean name) found in the application context.
  - Notes: Adds a timestamp parameter to ensure a new JobParameters set for each run.
  - Authorization: requires `admin` role (method guarded by `@PreAuthorize("hasRole('admin')")`).
  - Response: HTTP 200 with `Job ID: <id>` when submitted, or HTTP 500 with error message on failure.

- **Stop job execution**: `GET /admin/batch/stop/{executionId}`
  - Description: Requests to stop a running job execution via `JobOperator.stop(executionId)`.
  - Authorization: requires `admin` role.
  - Response: HTTP 200 with `stop requested: true|false` or HTTP 500 on error.

- **Abandon job execution**: `GET /admin/batch/abandon/{executionId}`
  - Description: Marks a failed/stopped execution as abandoned using `JobOperator.abandon(executionId)`.
  - Authorization: requires `admin` role.
  - Response: HTTP 200 with `abandoned` or HTTP 500 on error.

Implementation notes:
- The controller locates jobs by bean name from the `ApplicationContext` and uses an asynchronous `JobLauncher` (qualified as `asyncJobLauncher`) to start the job, returning the `JobExecution` id.
- For stop/abandon actions the controller calls the provided `JobOperator` to control executions.
- Make sure the `admin` role is available and that requests are authenticated; adjust security configuration if necessary for your environment.

#### 1. Project Overview
Java 21 / Spring Boot 3 application with optional native image build using GraalVM.

#### 2. JVM (Standard) Run
1. Install Java 21 (GraalVM or any JDK 21).
2. Clone repository.
3. Build: `./gradlew clean build`
4. Run dev (hot reload): `./gradlew bootRun`

#### 3. Native Image (Windows)
Prerequisites:
- GraalVM JDK 21 installed (same drive as the project to avoid the "'other' has different root" error).
- Visual Studio 2022 (Community or Build Tools) with workload: Desktop development with C++.
  Required components:
  - MSVC v143 x64/x86 build tools
  - Windows 10/11 SDK
  - C++ CMake tools for Windows
  (Optional: Ninja, Git)

Checks:
```
where cl
where native-image
java -version
```
If `cl` not found, open the proper developer prompt.

Build (from an "x64 Native Tools Command Prompt for VS 2022"):
```
cd E:\Script\EspacoGeek\backend
"C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" amd64 && .\gradlew clean nativeCompile --stacktrace
```
Resulting binary (Windows): `build\native\nativeCompile\espaco-geek.exe`

If you moved GraalVM or project: ensure both are on same drive letter (e.g. E:).

#### 4. Native Image (Ubuntu 24.04 / WSL2)
Install system dependencies:
```
sudo apt update
sudo apt install -y curl zip unzip build-essential zlib1g-dev libssl-dev pkg-config
```
Install GraalVM via SDKMAN:
```
curl -s https://get.sdkman.io | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.8-graal
sdk use java 21.0.8-graal
```
(If `gu` not present but `native-image` symlink exists, you can still build. Otherwise download tar.gz release and extract to /opt.)

Verify:
```
java -version
native-image --version
```
Build native (Linux):
```
cd /path/to/backend
./gradlew clean nativeCompile --stacktrace
```
Binary: `build/native/nativeCompile/espaco-geek`
Run: `./build/native/nativeCompile/espaco-geek`

WSL Performance Tip: For faster native builds, move project inside Linux filesystem (`~/EspacoGeek`) instead of `/mnt/<drive>`.

#### 5. Database (MySQL) Example
Create DB:
```
CREATE DATABASE espacogeek CHARACTER SET utf8mb4;
CREATE USER 'geek'@'localhost' IDENTIFIED BY 'strongpass';
GRANT ALL ON espacogeek.* TO 'geek'@'localhost';
FLUSH PRIVILEGES;
```
Environment variables (Linux example):
```
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/espacogeek
export SPRING_DATASOURCE_USERNAME=geek
export SPRING_DATASOURCE_PASSWORD=strongpass
```
Run JVM:
```
./gradlew bootRun
```
Run native:
```
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/espacogeek \
SPRING_DATASOURCE_USERNAME=geek \
SPRING_DATASOURCE_PASSWORD=strongpass \
./build/native/nativeCompile/espaco-geek
```

#### 6. Useful Gradle Tasks
```
./gradlew clean
./gradlew build
./gradlew bootRun
./gradlew test
./gradlew nativeCompile
```

#### 7. Troubleshooting
| Issue | Cause | Fix |
|-------|-------|-----|
| `'other' has different root` | Project & GraalVM on different Windows drives | Place both on same drive |
| `Failed to find 'vcvarsall.bat'` | MSVC toolchain missing | Install VS 2022 C++ workload |
| `cl not found` | Environment not initialized | Use Native Tools Command Prompt / run `vcvarsall.bat` |
| `native-image not found` | PATH or install incomplete | Ensure GraalVM bin in PATH / reinstall |
| Slow WSL build | Project on /mnt | Move to `~/` inside WSL |
| Experimental option warnings | `-H:Name` etc. | Remove those flags or add `-H:+UnlockExperimentalVMOptions` |

#### 8. Clean Native Artifacts
```
./gradlew clean
rm -rf build/native
```

#### 9. Docker Usage
Supports three modes: JVM, JVM Debug, Native (GraalVM). Uses `docker-compose` to orchestrate optional MySQL.

Directory: `docker/`
- `Dockerfile.jvm` : Multi-stage build producing a JVM runtime image.
- `Dockerfile.native` : Multi-stage native-image build (Linux binary).
- `docker-compose.yml` : Services: app-jvm, app-jvm-debug, app-native, db.

Prerequisites:
- Docker Engine + Compose Plugin (Docker Desktop or distro packages)
- (Native image build) Sufficient RAM (≥4‑6GB) & CPU time.

Build & Run (all services):
```
cd docker
docker compose up --build
```
This will start:
- `db` (MySQL)
- `app-jvm` on port 8080
- `app-jvm-debug` on port 8081 (debug port 5006)
- `app-native` on port 8082

Run only a specific service (example native):
```
cd docker
docker compose build app-native
docker compose up app-native db
```

JVM Debug attach example (IDE):
- Host: localhost
- Port: 5006
- Mode: Attach (Socket)

Environment overrides:
```
SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/espacogeek
SPRING_DATASOURCE_USERNAME=geek
SPRING_DATASOURCE_PASSWORD=strongpass
```
Adjust in `docker-compose.yml` or via `--env` flags.

Rebuild after code changes (JVM):
```
docker compose build app-jvm && docker compose up -d app-jvm
```
Faster iterative JVM dev (mount source) – optional example (not enabled by default):
Add in service:
```
volumes:
  - ../:/workspace
```
Then run a dev command override.

Cleaning images/containers:
```
docker compose down
# Remove dangling images
docker image prune -f
```

Native image notes:
- Native build happens in container; local GraalVM install not required.
- Result binary stays inside the image (distroless run stage).
- For lower image size you could add strip flags (`-H:StripDebugInfo`).

Production suggestions:
- Use a separate network/database.
- Externalize secrets via Docker secrets or env file (do not commit `.env` with real credentials).

#### 10. Run Without Docker (Recap)
- JVM: `./gradlew bootRun`
- Native (host): `./gradlew nativeCompile && ./build/native/nativeCompile/espaco-geek`
- Configure DB via environment variables as shown earlier.

---
Short path: JVM dev = `./gradlew bootRun` | Docker all = `cd docker && docker compose up --build` | Native docker = `docker compose build app-native`.

### Email Server Settings

Add the following environment variables to your `.env` file:

```properties
# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
APP_NAME=EspacoGeek
FRONTEND_URL=http://localhost:3000
```
