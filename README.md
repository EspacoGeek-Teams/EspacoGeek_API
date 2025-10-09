# EspacoGeek

## Run & Build Guide (JVM and Native)

### 1. Project Overview
Java 21 / Spring Boot 3 application with optional native image build using GraalVM.

### 2. JVM (Standard) Run
1. Install Java 21 (GraalVM or any JDK 21).
2. Clone repository.
3. Build: `./gradlew clean build`
4. Run dev (hot reload): `./gradlew bootRun`

### 3. Native Image (Windows)
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

### 4. Native Image (Ubuntu 24.04 / WSL2)
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

### 5. Database (MySQL) Example
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

### 6. Useful Gradle Tasks
```
./gradlew clean
./gradlew build
./gradlew bootRun
./gradlew test
./gradlew nativeCompile
```

### 7. Troubleshooting
| Issue | Cause | Fix |
|-------|-------|-----|
| `'other' has different root` | Project & GraalVM on different Windows drives | Place both on same drive |
| `Failed to find 'vcvarsall.bat'` | MSVC toolchain missing | Install VS 2022 C++ workload |
| `cl not found` | Environment not initialized | Use Native Tools Command Prompt / run `vcvarsall.bat` |
| `native-image not found` | PATH or install incomplete | Ensure GraalVM bin in PATH / reinstall |
| Slow WSL build | Project on /mnt | Move to `~/` inside WSL |
| Experimental option warnings | `-H:Name` etc. | Remove those flags or add `-H:+UnlockExperimentalVMOptions` |

### 8. Clean Native Artifacts
```
./gradlew clean
rm -rf build/native
```

### 9. Security Notes
Rotate database credentials and avoid committing secrets. Use environment variables or a secrets manager.

### 10. License
See `LICENSE.txt`.

---
Short path: JVM dev = `./gradlew bootRun` | Native = `./gradlew nativeCompile` then run binary.
