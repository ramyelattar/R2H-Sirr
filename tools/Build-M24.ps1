<#
R2H-Play M24 BUILD ONLY. No install, uninstall, backup deletion, ADB, GitHub Actions or CI.
Uses Gradle 8.13 and JDK 17. Downloads official Gradle only if necessary
and checks SHA-256 before extraction. Only writes Gradle build outputs.
#>
param()
$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $root
try {
    $branch = (& git branch --show-current).Trim()
    if ($LASTEXITCODE -ne 0 -or $branch -ne "feature/m24-local-build-ready") {
        throw "Wrong Git branch: '$branch'. Check out feature/m12-r2h-play-identity-sound first."
    }

    $localJdk = "D:\R2H-Dev\Java\jdk-17"
    $localSdk = "D:\R2H-Dev\Android\SDK"
    if (Test-Path (Join-Path $localJdk "bin\java.exe")) {
        $env:JAVA_HOME = $localJdk
    }
    if (-not $env:JAVA_HOME) { throw "JDK 17 is not configured." }
    if (Test-Path (Join-Path $localSdk "platform-tools\adb.exe")) {
        $env:ANDROID_HOME = $localSdk
        $env:ANDROID_SDK_ROOT = $localSdk
    }
    if (-not $env:ANDROID_HOME) { throw "Android SDK missing. Configure ANDROID_HOME." }

    $gradle = $null
    $wrapper = Join-Path $root "gradlew.bat"
    if (Test-Path $wrapper) {
        $gradle = $wrapper
    } elseif ($env:GRADLE_HOME -and (Test-Path (Join-Path $env:GRADLE_HOME "bin\gradle.bat"))) {
        $gradle = Join-Path $env:GRADLE_HOME "bin\gradle.bat"
    } else {
        $command = Get-Command gradle.bat, gradle -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($command) { $gradle = $command.Source }
    }

    if (-not $gradle) {
        $wrapperCache = Join-Path $HOME ".gradle\wrapper\dists\gradle-8.13-bin"
        if (Test-Path $wrapperCache) {
            $cached = Get-ChildItem -Path $wrapperCache -Filter "gradle.bat" -File -Recurse -ErrorAction SilentlyContinue |
                Where-Object { $_.FullName.EndsWith("\gradle-8.13\bin\gradle.bat", [StringComparison]::OrdinalIgnoreCase) } |
                Select-Object -First 1
            if ($cached) { $gradle = $cached.FullName }
        }
    }

    if (-not $gradle) {
        # SHA-256: https://gradle.org/release-checksums/ (8.13 binary ZIP)
        $officialSha256 = "20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78"
        $baseDir = if ($env:LOCALAPPDATA) { $env:LOCALAPPDATA } else { $HOME }
        $cache = Join-Path $baseDir "R2H\Gradle"
        New-Item -Path $cache -ItemType Directory -Force | Out-Null
        $installDir = Join-Path $cache "gradle-8.13"
        $gradle = Join-Path $installDir "bin\gradle.bat"

        if (-not (Test-Path $gradle)) {
            $archive = Join-Path $cache "gradle-8.13-bin.zip"
            if (Test-Path $archive) {
                $cachedHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
                if ($cachedHash -ne $officialSha256) { Remove-Item -LiteralPath $archive -Force }
            }
            if (-not (Test-Path $archive)) {
                $partialDownload = "$archive.download"
                try {
                    Write-Host "Downloading official Gradle 8.13 (first run only)..."
                    Invoke-WebRequest -UseBasicParsing -Uri "https://services.gradle.org/distributions/gradle-8.13-bin.zip" -OutFile $partialDownload -ErrorAction Stop
                    $checksum = (Get-FileHash -LiteralPath $partialDownload -Algorithm SHA256).Hash.ToLowerInvariant()
                    if ($checksum -ne $officialSha256) {
                        throw "Gradle checksum mismatch. Download was rejected."
                    }
                    Move-Item -LiteralPath $partialDownload -Destination $archive -Force
                } finally {
                    if (Test-Path $partialDownload) { Remove-Item -LiteralPath $partialDownload -Force }
                }
            }
            $extractDir = Join-Path $cache ("unpack-" + [Guid]::NewGuid().ToString("N"))
            try {
                Write-Host "Extracting verified Gradle 8.13..."
                Expand-Archive -LiteralPath $archive -DestinationPath $extractDir -Force
                $sourceDir = Join-Path $extractDir "gradle-8.13"
                if (-not (Test-Path (Join-Path $sourceDir "bin\gradle.bat"))) {
                    throw "Official Gradle archive did not contain gradle.bat."
                }
                if (Test-Path $installDir) { Remove-Item -LiteralPath $installDir -Recurse -Force }
                Move-Item -LiteralPath $sourceDir -Destination $installDir
            } finally {
                if (Test-Path $extractDir) { Remove-Item -LiteralPath $extractDir -Recurse -Force }
            }
        }
        if (-not (Test-Path $gradle)) { throw "Gradle setup failed. APK was not built or installed." }
    }

    Write-Host "Building R2H-Play M24 debug APK using: $gradle"
    & $gradle ":app:assembleDebug" "--no-daemon"
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed (exit code $LASTEXITCODE). No phone installation attempted."
    }
    $apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apk)) { throw "No APK found after build: $apk" }
    Write-Host "APK built: $apk"
    Write-Host "M24 APK build complete. NO installation or data modification was performed." -ForegroundColor Green
} finally {
    Pop-Location
}
