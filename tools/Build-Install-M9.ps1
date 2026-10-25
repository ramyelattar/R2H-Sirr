<#
R2H-Sirr M9: local Android debug build and in-place device update.
Run only after checking out feature/m9-stabilization. This script NEVER uninstalls,
clears app data, resets a phone, or uses Gradle/ADB during GitHub source development.
Bootstraps checksum-verified official Gradle 8.13 once if it is not installed (no wrapper JAR in repo).
#>
param(
    [string]$DeviceSerial = "",
    [switch]$BuildOnly
)
$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $root
try {
    $branch = (& git branch --show-current).Trim()
    if ($LASTEXITCODE -ne 0 -or $branch -ne "feature/m9-stabilization") {
        throw "Wrong Git branch: '$branch'. Check out feature/m9-stabilization first."
    }
    if (-not $env:JAVA_HOME -and (Test-Path "D:\R2H-Dev\Java\jdk-17\bin\java.exe")) {
        $env:JAVA_HOME = "D:\R2H-Dev\Java\jdk-17"
    }
    if (-not $env:ANDROID_HOME -and (Test-Path "D:\R2H-Dev\Android\SDK\platform-tools\adb.exe")) {
        $env:ANDROID_HOME = "D:\R2H-Dev\Android\SDK"
    }
    $gradle = $null
    if (Test-Path (Join-Path $root "gradlew.bat")) {
        $gradle = Join-Path $root "gradlew.bat"
    } elseif ($env:GRADLE_HOME -and (Test-Path (Join-Path $env:GRADLE_HOME "bin\gradle.bat"))) {
        $gradle = Join-Path $env:GRADLE_HOME "bin\gradle.bat"
    } else {
        $command = Get-Command gradle.bat, gradle -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($command) { $gradle = $command.Source }
    }
    if (-not $gradle) {
        # Reuse an existing official Gradle 8.13 Wrapper distribution if present.
        $wrapperCache = Join-Path $HOME ".gradle\wrapper\dists\gradle-8.13-bin"
        if (Test-Path $wrapperCache) {
            $cached = Get-ChildItem -Path $wrapperCache -Filter "gradle.bat" -File -Recurse -ErrorAction SilentlyContinue |
                Where-Object { $_.FullName -match '[\\/]gradle-8\.13[\\/]bin[\\/]gradle\.bat
    Write-Host "Building M9 debug APK using: $gradle"
    & $gradle ":app:assembleDebug" "--no-daemon"
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit code $LASTEXITCODE). No app installation attempted." }
    $apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apk)) { throw "Build returned success but APK was not found: $apk" }
    Write-Host "APK built: $apk"
    if ($BuildOnly) { return }

    $adb = $null
    if ($env:ANDROID_HOME -and (Test-Path (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"))) {
        $adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
    } else {
        $command = Get-Command adb.exe -ErrorAction SilentlyContinue
        if ($command) { $adb = $command.Source }
    }
    if (-not $adb) { throw "ADB not found; APK built, but device update skipped." }
    $connected = @(& $adb devices | Where-Object { $_ -match "^\S+\s+device\s*$" } | ForEach-Object { ($_ -split "\s+")[0] })
    if ($LASTEXITCODE -ne 0) { throw "adb devices failed; APK built, no installation attempted." }
    if ($DeviceSerial) {
        if ($DeviceSerial -notin $connected) { throw "Selected device '$DeviceSerial' not connected. APK built, installation skipped." }
        $args = @("-s", $DeviceSerial)
    } else {
        if ($connected.Count -ne 1) { throw "Exactly one authorized Android device is required, or specify -DeviceSerial. Connected devices: $($connected.Count). APK built, no installation attempted." }
        $args = @("-s", $connected[0])
    }
    Write-Host "Installing in place; existing app data is not deliberately erased..."
    & $adb @args install -r $apk
    if ($LASTEXITCODE -ne 0) { throw "adb install -r failed. No uninstall or data-clear was attempted." }
    Write-Host "Installed com.r2h.sirr (M9 preview). Open the app to perform the manual acceptance checks."
} finally {
    Pop-Location
}
 } |
                Select-Object -First 1
            if ($cached) { $gradle = $cached.FullName }
        }
    }
    if (-not $gradle) {
        # No global Gradle and no complete Wrapper: bootstrap the pinned distribution.
        # SHA-256 from https://gradle.org/release-checksums/ (Gradle 8.13 binary ZIP).
        $version = "8.13"
        $sha256 = "20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78"
        $cacheBase = if ($env:LOCALAPPDATA) { $env:LOCALAPPDATA } else { $HOME }
        $cache = Join-Path $cacheBase "R2H\Gradle"
        New-Item -ItemType Directory -Path $cache -Force | Out-Null
        $installDir = Join-Path $cache "gradle-$version"
        $gradle = Join-Path $installDir "bin\gradle.bat"

        if (-not (Test-Path $gradle)) {
            $archive = Join-Path $cache "gradle-$version-bin.zip"
            if ((Test-Path $archive) -and ((Get-FileHash -Path $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $sha256)) {
                Remove-Item -LiteralPath $archive -Force
            }
            if (-not (Test-Path $archive)) {
                $temporaryZip = "$archive.download"
                Write-Host "Fetching official Gradle $version distribution (one-time setup)..."
                try {
                    Invoke-WebRequest -UseBasicParsing -Uri "https://services.gradle.org/distributions/gradle-$version-bin.zip" -OutFile $temporaryZip -ErrorAction Stop
                    $actualHash = (Get-FileHash -Path $temporaryZip -Algorithm SHA256).Hash.ToLowerInvariant()
                    if ($actualHash -ne $sha256) { throw "Gradle ZIP SHA-256 mismatch. Download discarded." }
                    Move-Item -LiteralPath $temporaryZip -Destination $archive -Force
                } finally {
                    if (Test-Path $temporaryZip) { Remove-Item -LiteralPath $temporaryZip -Force }
                }
            }
            $extraction = Join-Path $cache ("gradle-extract-" + [Guid]::NewGuid().ToString("N"))
            try {
                Write-Host "Extracting verified Gradle $version..."
                Expand-Archive -LiteralPath $archive -DestinationPath $extraction -Force
                $extractedDir = Join-Path $extraction "gradle-$version"
                if (-not (Test-Path (Join-Path $extractedDir "bin\gradle.bat"))) {
                    throw "Verified Gradle archive is missing gradle.bat."
                }
                if (Test-Path $installDir) {
                    Remove-Item -LiteralPath $installDir -Recurse -Force
                }
                Move-Item -LiteralPath $extractedDir -Destination $installDir
            } finally {
                if (Test-Path $extraction) { Remove-Item -LiteralPath $extraction -Recurse -Force }
            }
        }
        if (-not (Test-Path $gradle)) { throw "Gradle 8.13 could not be prepared. No installation attempted." }
        Write-Host "Using verified, locally cached Gradle $version."
    }
    Write-Host "Building M9 debug APK using: $gradle"
    & $gradle ":app:assembleDebug" "--no-daemon"
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit code $LASTEXITCODE). No app installation attempted." }
    $apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apk)) { throw "Build returned success but APK was not found: $apk" }
    Write-Host "APK built: $apk"
    if ($BuildOnly) { return }

    $adb = $null
    if ($env:ANDROID_HOME -and (Test-Path (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"))) {
        $adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
    } else {
        $command = Get-Command adb.exe -ErrorAction SilentlyContinue
        if ($command) { $adb = $command.Source }
    }
    if (-not $adb) { throw "ADB not found; APK built, but device update skipped." }
    $connected = @(& $adb devices | Where-Object { $_ -match "^\S+\s+device\s*$" } | ForEach-Object { ($_ -split "\s+")[0] })
    if ($LASTEXITCODE -ne 0) { throw "adb devices failed; APK built, no installation attempted." }
    if ($DeviceSerial) {
        if ($DeviceSerial -notin $connected) { throw "Selected device '$DeviceSerial' not connected. APK built, installation skipped." }
        $args = @("-s", $DeviceSerial)
    } else {
        if ($connected.Count -ne 1) { throw "Exactly one authorized Android device is required, or specify -DeviceSerial. Connected devices: $($connected.Count). APK built, no installation attempted." }
        $args = @("-s", $connected[0])
    }
    Write-Host "Installing in place; existing app data is not deliberately erased..."
    & $adb @args install -r $apk
    if ($LASTEXITCODE -ne 0) { throw "adb install -r failed. No uninstall or data-clear was attempted." }
    Write-Host "Installed com.r2h.sirr (M9 preview). Open the app to perform the manual acceptance checks."
} finally {
    Pop-Location
}
