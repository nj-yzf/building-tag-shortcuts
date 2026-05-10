param(
    [string]$JdkUrl = "https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip"
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path ".").Path
$toolsDir = Join-Path $root ".tools"
$downloadDir = Join-Path $toolsDir "downloads"
$zipPath = Join-Path $downloadDir "microsoft-jdk-17.zip"
$jdkDir = Join-Path $toolsDir "jdk-17"
$extractDir = Join-Path $toolsDir "jdk-extract"

if (Test-Path -LiteralPath (Join-Path $jdkDir "bin\javac.exe")) {
    Write-Host "Local JDK already exists at $jdkDir"
    & (Join-Path $jdkDir "bin\java.exe") -version
    exit 0
}

New-Item -ItemType Directory -Force -Path $downloadDir | Out-Null

if (-not (Test-Path -LiteralPath $zipPath)) {
    Write-Host "Downloading JDK 17..."
    Invoke-WebRequest -Uri $JdkUrl -OutFile $zipPath
}

if (Test-Path -LiteralPath $extractDir) {
    Remove-Item -LiteralPath $extractDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $extractDir | Out-Null

Write-Host "Extracting JDK..."
Expand-Archive -LiteralPath $zipPath -DestinationPath $extractDir -Force

$javac = Get-ChildItem -Path $extractDir -Recurse -Filter javac.exe | Select-Object -First 1
if (-not $javac) {
    throw "Downloaded archive did not contain javac.exe."
}

$jdkRoot = Split-Path (Split-Path $javac.FullName -Parent) -Parent
if (Test-Path -LiteralPath $jdkDir) {
    Remove-Item -LiteralPath $jdkDir -Recurse -Force
}
Move-Item -LiteralPath $jdkRoot -Destination $jdkDir
Remove-Item -LiteralPath $extractDir -Recurse -Force

Write-Host "Local JDK installed at $jdkDir"
& (Join-Path $jdkDir "bin\java.exe") -version
