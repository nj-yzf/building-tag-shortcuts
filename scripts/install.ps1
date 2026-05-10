param(
    [string]$JosmPluginsDir = (Join-Path $env:APPDATA "JOSM\plugins")
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path ".").Path
$buildScript = Join-Path $root "scripts\build.ps1"
$jarPath = Join-Path $root "dist\building-tag-shortcuts.jar"

& $buildScript

New-Item -ItemType Directory -Force -Path $JosmPluginsDir | Out-Null
Copy-Item -LiteralPath $jarPath -Destination (Join-Path $JosmPluginsDir "building-tag-shortcuts.jar") -Force

Write-Host "Installed to $JosmPluginsDir\building-tag-shortcuts.jar"
Write-Host "Restart JOSM, then check Data menu and keyboard shortcut preferences."

