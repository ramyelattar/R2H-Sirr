# Source-only verification. DOES NOT build or install APKs.
# Run in the repository root. Requires Python 3.10+.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Invoke-Checked([string]$Description, [string]$Command, [string[]]$Arguments) {
    Write-Host "M23: $Description" -ForegroundColor Cyan
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) { throw "M23 FAILED: $Description (exit $LASTEXITCODE)" }
}

$python = (Get-Command python -ErrorAction SilentlyContinue)
if (-not $python) { throw 'Python 3.10+ is required on PATH for source-only verification.' }
Invoke-Checked -Description 'Canonical Kotlin and backend content sync' -Command $python.Source -Arguments @('tools/generate_content_v2.py', '--check')
Invoke-Checked -Description 'Arabic content uniqueness and schema' -Command $python.Source -Arguments @('tools/check_m22c_content_quality.py')
Invoke-Checked -Description 'Android UX and privacy source contracts' -Command $python.Source -Arguments @('tools/check_m8_ui_source.py')
Write-Host 'M23 SOURCE-ONLY CHECKS PASS. No Gradle/Kotlin compilation, JVM tests or APK build was run.' -ForegroundColor Green
