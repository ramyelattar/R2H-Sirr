<#
R2H-Sirr M9: local Android debug build and in-place device update.
Run only after checking out feature/m9-stabilization. This script NEVER uninstalls,
clears app data, resets a phone, or uses Gradle/ADB during GitHub source development.
Requires a locally installed Gradle 8.13 because this repository has no Gradle wrapper JAR.
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
        throw "Gradle not found. This source-only repository has no gradle-wrapper.jar. Install/use Gradle 8.13 (AGP 8.13.2), then run again."
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
