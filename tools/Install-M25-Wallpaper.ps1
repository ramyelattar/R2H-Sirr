param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$ImagePath
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$source = (Resolve-Path -LiteralPath $ImagePath).Path
$destinationDir = Join-Path $root 'app\src\main\res\drawable-nodpi'
$destination = Join-Path $destinationDir 'r2h_neon_wallpaper.webp'
if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
    throw "Wallpaper file not found: $ImagePath"
}
New-Item -ItemType Directory -Force -Path $destinationDir | Out-Null
Copy-Item -LiteralPath $source -Destination $destination -Force
Write-Host "M25 wallpaper installed: $destination" -ForegroundColor Green
Write-Host "Build locally when ready. Nothing installed on your Android device."
