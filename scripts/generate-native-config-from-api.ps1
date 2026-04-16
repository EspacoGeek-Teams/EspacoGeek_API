param(
    [string]$ApiUrl = "http://localhost:19080",
    [int]$ServerPort = 19080,
    [int]$ManagementPort = 19081,
    [string]$HintsDir = "build/native-image-hints-auto",
    [string]$Email = "",
    [string]$Password = "NativeAgent!123",
    [string]$Username = "nativeagent",
    [string]$DeviceInfo = "graalvm-agent-script",
    [int]$StartupTimeoutSec = 180,
    [switch]$DoNotCopyToProjectConfig,
    [switch]$UsePlaywrightFlow,
    [string]$FrontendPath = "..\\frontend",
    [string]$PlaywrightEmail = "",
    [string]$PlaywrightPassword = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "[native-agent] $Message"
}

function Invoke-GraphQL {
    param(
        [string]$Url,
        [string]$Query,
        [hashtable]$Variables,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [hashtable]$Headers
    )

    $body = @{ query = $Query; variables = $Variables } | ConvertTo-Json -Depth 10

    try {
        return Invoke-RestMethod -Method Post -Uri $Url -ContentType "application/json" -Body $body -WebSession $Session -Headers $Headers -TimeoutSec 20
    }
    catch {
        Write-Warning "GraphQL call failed: $($_.Exception.Message)"
        return $null
    }
}

function Get-GraphQlCandidateUrls {
    param([string]$InputUrl)

    $normalized = $InputUrl.TrimEnd('/')
    $uri = [System.Uri]::new($normalized)
    $base = "{0}://{1}" -f $uri.Scheme, $uri.Authority
    $path = $uri.AbsolutePath

    $candidates = New-Object System.Collections.Generic.List[string]
    if ($path -and $path -ne "/") {
        $candidates.Add($normalized)
    }

    $candidates.Add($base)
    $candidates.Add("$base/graphql")
    $candidates.Add("$base/api/graphql")

    return $candidates | Select-Object -Unique
}

function Stop-ListenerProcessOnPort {
    param([int]$Port)

    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1

    if (-not $conn) {
        return
    }

    $processId = $conn.OwningProcess
    if (-not $processId) {
        return
    }

    Write-Step "Stopping listener process on port $Port (PID: $processId)..."
    try {
        Stop-Process -Id $processId -ErrorAction Stop
    }
    catch {
        Stop-Process -Id $processId -Force
    }
}

$scriptRoot = Split-Path -Parent $PSCommandPath
$projectRoot = Split-Path -Parent $scriptRoot
$copyToProjectConfig = -not $DoNotCopyToProjectConfig
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$effectiveHintsDir = Join-Path $HintsDir "run-$runId"

if ([string]::IsNullOrWhiteSpace($Email)) {
    $suffix = Get-Random -Minimum 10000 -Maximum 99999
    $Email = "native-agent-$suffix@example.com"
}

Write-Step "Project root: $projectRoot"
Write-Step "GraphQL API URL (input): $ApiUrl"
Write-Step "Hints output dir: $effectiveHintsDir"

Push-Location $projectRoot

$oldAgentFlag = $env:NATIVE_IMAGE_AGENT
$oldHintsDir = $env:NATIVE_IMAGE_HINTS_DIR
$oldDevtoolsRestart = $env:SPRING_DEVTOOLS_RESTART_ENABLED

$bootRunProcess = $null

try {
    Stop-ListenerProcessOnPort -Port $ServerPort
    Stop-ListenerProcessOnPort -Port $ManagementPort

    if (Test-Path $effectiveHintsDir) {
        Remove-Item -Path $effectiveHintsDir -Recurse -Force
    }

    $env:NATIVE_IMAGE_AGENT = "true"
    $env:NATIVE_IMAGE_HINTS_DIR = $effectiveHintsDir
    $env:SPRING_DEVTOOLS_RESTART_ENABLED = "false"

    $gradleCmd = if (Test-Path ".\\gradlew.bat") { ".\\gradlew.bat" } else { "./gradlew" }
    $bootRunArgs = "bootRun --no-daemon --args=`"--server.port=$ServerPort --management.server.port=$ManagementPort --spring.devtools.restart.enabled=false --spring.devtools.livereload.enabled=false --management.endpoint.shutdown.enabled=true --management.endpoints.web.exposure.include=health,shutdown`""

    Write-Step "Starting application with native-image agent enabled..."
    $bootRunProcess = Start-Process -FilePath $gradleCmd -ArgumentList $bootRunArgs -PassThru -NoNewWindow

    $candidateUrls = Get-GraphQlCandidateUrls -InputUrl $ApiUrl
    Write-Step "GraphQL endpoint candidates: $($candidateUrls -join ', ')"

    $ready = $false
    $resolvedApiUrl = $null
    $deadline = (Get-Date).AddSeconds($StartupTimeoutSec)
    while ((Get-Date) -lt $deadline) {
        foreach ($candidate in $candidateUrls) {
            $probe = Invoke-GraphQL -Url $candidate -Query "query { __typename }" -Variables @{} -Session (New-Object Microsoft.PowerShell.Commands.WebRequestSession) -Headers @{}
            if ($probe -and $probe.data) {
                $ready = $true
                $resolvedApiUrl = $candidate
                break
            }
        }

        if ($ready) { break }
        Start-Sleep -Milliseconds 500
    }

    if (-not $ready) {
        throw "API was not ready within $StartupTimeoutSec seconds."
    }

    Write-Step "Resolved GraphQL endpoint: $resolvedApiUrl"

    Write-Step "API is up. Sending warm-up GraphQL requests..."

    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

    # Exercise generic GraphQL handling and a public resolver path.
    [void](Invoke-GraphQL -Url $resolvedApiUrl -Query "query { __typename quote { __typename } }" -Variables @{} -Session $session -Headers @{})

    if ($UsePlaywrightFlow) {
        if ([string]::IsNullOrWhiteSpace($PlaywrightEmail) -or [string]::IsNullOrWhiteSpace($PlaywrightPassword)) {
            throw "When -UsePlaywrightFlow is enabled, provide -PlaywrightEmail and -PlaywrightPassword."
        }

        $resolvedUri = [System.Uri]::new($resolvedApiUrl)
        $apiBaseForPlaywright = "{0}://{1}" -f $resolvedUri.Scheme, $resolvedUri.Authority
        $frontendFullPath = (Resolve-Path $FrontendPath).Path

        Write-Step "Executing Playwright auth flow from frontend at '$frontendFullPath'..."

        Push-Location $frontendFullPath
        $oldApiBase = $env:API_BASE_URL
        $oldFrontendOrigin = $env:FRONTEND_ORIGIN
        $oldNativeEmail = $env:NATIVE_AGENT_EMAIL
        $oldNativePassword = $env:NATIVE_AGENT_PASSWORD
        try {
            $env:API_BASE_URL = $apiBaseForPlaywright
            $env:FRONTEND_ORIGIN = "http://localhost:3000"
            $env:NATIVE_AGENT_EMAIL = $PlaywrightEmail
            $env:NATIVE_AGENT_PASSWORD = $PlaywrightPassword

            npm run native:auth-flow
            if ($LASTEXITCODE -ne 0) {
                throw "Playwright auth flow exited with code $LASTEXITCODE."
            }
        }
        finally {
            $env:API_BASE_URL = $oldApiBase
            $env:FRONTEND_ORIGIN = $oldFrontendOrigin
            $env:NATIVE_AGENT_EMAIL = $oldNativeEmail
            $env:NATIVE_AGENT_PASSWORD = $oldNativePassword
            Pop-Location
        }
    }

    # Try creating a disposable user to exercise auth/user mutation paths.
        $createUserQuery = @'
mutation CreateUser($credentials: NewUser) {
  createUser(credentials: $credentials)
}
'@

    $createUserVars = @{
        credentials = @{
            username = $Username
            email = $Email
            password = $Password
        }
    }

    if (-not $UsePlaywrightFlow) {
        [void](Invoke-GraphQL -Url $resolvedApiUrl -Query $createUserQuery -Variables $createUserVars -Session $session -Headers @{})
    }

    # Login path exercises JJWT builder/signing + cookie emission.
        $loginQuery = @'
mutation Login($email: String, $password: String, $deviceInfo: String) {
  login(email: $email, password: $password, deviceInfo: $deviceInfo) {
    accessToken
  }
}
'@

    $loginVars = @{
        email = $Email
        password = $Password
        deviceInfo = $DeviceInfo
    }

    $loginResponse = $null
    if (-not $UsePlaywrightFlow) {
        $loginResponse = Invoke-GraphQL -Url $resolvedApiUrl -Query $loginQuery -Variables $loginVars -Session $session -Headers @{}
    }

    $accessToken = $null
    if ($loginResponse -and $loginResponse.data -and $loginResponse.data.login) {
        $accessToken = $loginResponse.data.login.accessToken
    }

    if (-not [string]::IsNullOrWhiteSpace($accessToken)) {
        Write-Step "Login succeeded. Exercising authenticated flows..."

        $authHeaders = @{ Authorization = "Bearer $accessToken" }

        [void](Invoke-GraphQL -Url $resolvedApiUrl -Query "query { isLogged }" -Variables @{} -Session $session -Headers $authHeaders)

        $refreshResponse = Invoke-GraphQL -Url $resolvedApiUrl -Query "mutation { refreshToken { accessToken } }" -Variables @{} -Session $session -Headers @{}
        if ($refreshResponse -and $refreshResponse.data -and $refreshResponse.data.refreshToken -and $refreshResponse.data.refreshToken.accessToken) {
            $accessToken = $refreshResponse.data.refreshToken.accessToken
            $authHeaders = @{ Authorization = "Bearer $accessToken" }
            [void](Invoke-GraphQL -Url $resolvedApiUrl -Query "query { isLogged }" -Variables @{} -Session $session -Headers $authHeaders)
        }

        [void](Invoke-GraphQL -Url $resolvedApiUrl -Query "query { logout }" -Variables @{} -Session $session -Headers $authHeaders)
    }
    else {
        Write-Warning "Login did not return an access token. Public-path metadata was still collected."
    }

    Start-Sleep -Seconds 2

    # Graceful shutdown via actuator so GraalVM agent shutdown hooks run and flush config files.
    $shutdownUrl = "http://localhost:$ManagementPort/actuator/shutdown"
    Write-Step "Requesting graceful shutdown via $shutdownUrl ..."
    try {
           Invoke-RestMethod -Method Post -Uri $shutdownUrl -ContentType "application/json" -Body "{}" -TimeoutSec 10 -ErrorAction SilentlyContinue | Out-Null
           Write-Step "Graceful shutdown requested successfully."
    }
    catch {
        Write-Warning "Actuator shutdown call failed: $($_.Exception.Message). Falling back to process kill."
        if ($bootRunProcess -and -not $bootRunProcess.HasExited) {
            Stop-Process -Id $bootRunProcess.Id -Force
        }
        Stop-ListenerProcessOnPort -Port $ServerPort
        Stop-ListenerProcessOnPort -Port $ManagementPort
    }

    # Wait up to 60 s for the JVM to exit gracefully (agent needs time to write files).
    Write-Step "Waiting for JVM to exit and flush agent output..."
    $flushDeadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $flushDeadline) {
        $stillListening = Get-NetTCPConnection -LocalPort $ServerPort -State Listen -ErrorAction SilentlyContinue
        if (-not $stillListening) { break }
        Start-Sleep -Seconds 1
    }
    Start-Sleep -Seconds 3

    $reflectFile = Get-ChildItem -Path $effectiveHintsDir -Recurse -Filter "reflect-config.json" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    $resourceFile = Get-ChildItem -Path $effectiveHintsDir -Recurse -Filter "resource-config.json" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $reflectFile) {
        throw "reflect-config.json was not generated under '$effectiveHintsDir'."
    }

    Write-Step "Generated: $($reflectFile.FullName)"
    if ($resourceFile) {
        Write-Step "Generated: $($resourceFile.FullName)"
    }

    if ($copyToProjectConfig) {
        $targetDir = "src/main/resources/META-INF/native-image/com.espacogeek/geek"
        New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

        Copy-Item -Path $reflectFile.FullName -Destination (Join-Path $targetDir "reflect-config.json") -Force
        if ($resourceFile) {
            Copy-Item -Path $resourceFile.FullName -Destination (Join-Path $targetDir "resource-config.json") -Force
        }

        Write-Step "Copied generated configs to: $targetDir"
    }

    Write-Step "Done."
}
finally {
    if ($bootRunProcess -and -not $bootRunProcess.HasExited) {
        Write-Step "Stopping bootRun process (PID: $($bootRunProcess.Id))..."
        Stop-Process -Id $bootRunProcess.Id -Force
    }

    $env:NATIVE_IMAGE_AGENT = $oldAgentFlag
    $env:NATIVE_IMAGE_HINTS_DIR = $oldHintsDir
    $env:SPRING_DEVTOOLS_RESTART_ENABLED = $oldDevtoolsRestart

    Pop-Location
}
