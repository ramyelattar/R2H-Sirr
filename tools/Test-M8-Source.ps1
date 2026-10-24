# Windows offline-safe M8 source checks. Does not run Android/Gradle or install an APK.
# From PowerShell:
#   powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\Test-M8-Source.ps1
[CmdletBinding()]
param()
$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $root
try {
    $python = Get-Command python -ErrorAction SilentlyContinue
    if (-not $python) { throw "Python 3 is required. Install it first and rerun." }
    $node = Get-Command node -ErrorAction SilentlyContinue
    if (-not $node) { throw "Node.js 22+ is required. Install it first and rerun." }
    Write-Host "[1/3] Verify Egyptian v4 catalogs and generated files"
    & python tools/generate_content_v2.py --check
    if ($LASTEXITCODE -ne 0) { throw "Canonical catalog mismatch" }
    Write-Host "[2/3] Check Android UX source regression guards (static checks only)"
    & python tools/check_m8_ui_source.py
    if ($LASTEXITCODE -ne 0) { throw "M8 UX static checks failed" }
    Write-Host "[3/3] Test backend protocol and gameplay logic"
    Push-Location (Join-Path $root "backend")
    try {
        if (-not (Test-Path "node_modules/ws")) {
            Write-Host "Installing Node.js test dependencies; network may be required on first run"
            & npm install --ignore-scripts --no-audit --no-fund --no-package-lock
            if ($LASTEXITCODE -ne 0) { throw "npm install failed" }
        }
        & npm test
        if ($LASTEXITCODE -ne 0) { throw "Node.js tests failed" }
        & node --check server.mjs
        if ($LASTEXITCODE -ne 0) { throw "Node syntax failed" }
        & node --check public/app.js
        if ($LASTEXITCODE -ne 0) { throw "Browser syntax failed" }
    } finally { Pop-Location }
    Write-Host "M8 source checks passed. Android compilation, runtime and visual acceptance NOT tested."
} finally { Pop-Location }
