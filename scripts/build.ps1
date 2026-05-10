param(
    [string]$JosmJar = ".\lib\josm-tested.jar"
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path ".").Path

function Resolve-JdkTool {
    param(
        [string]$Name
    )

    $candidates = @()
    $localJdkTool = Join-Path $Root ".tools\jdk-17\bin\$Name.exe"
    $candidates += $localJdkTool

    if ($env:JDK_HOME) {
        $candidates += Join-Path $env:JDK_HOME "bin\$Name.exe"
    }
    if ($env:JAVA_HOME) {
        $candidates += Join-Path $env:JAVA_HOME "bin\$Name.exe"
    }

    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($cmd) {
        $candidates += $cmd.Source
    }

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    throw "Cannot find $Name.exe. Install JDK 11 or newer, set JAVA_HOME, or run .\scripts\setup-jdk.ps1."
}

function Assert-Jdk11Plus {
    param(
        [string]$JavaExe
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $versionOutput = & $JavaExe -version 2>&1
    $ErrorActionPreference = $previousErrorActionPreference
    $versionText = $versionOutput -join "`n"
    if ($versionText -match 'version "1\.([0-9]+)\.') {
        $major = [int]$Matches[1]
    } elseif ($versionText -match 'version "([0-9]+)') {
        $major = [int]$Matches[1]
    } else {
        throw "Unable to parse Java version:`n$versionText"
    }

    if ($major -lt 11) {
        throw "JDK 11 or newer is required. Detected:`n$versionText"
    }
}

$root = $Root
$libDir = Join-Path $root "lib"
$buildDir = Join-Path $root "build"
$classesDir = Join-Path $buildDir "classes"
$distDir = Join-Path $root "dist"
$manifestPath = Join-Path $buildDir "MANIFEST.MF"
$jarPath = Join-Path $distDir "building-tag-shortcuts.jar"

New-Item -ItemType Directory -Force -Path $libDir, $classesDir, $distDir | Out-Null

if (-not (Test-Path -LiteralPath $JosmJar)) {
    Write-Host "Downloading josm-tested.jar..."
    Invoke-WebRequest -Uri "https://josm.openstreetmap.de/josm-tested.jar" -OutFile $JosmJar
}

$javac = Resolve-JdkTool "javac"
$jar = Resolve-JdkTool "jar"
$java = Resolve-JdkTool "java"
Assert-Jdk11Plus $java

if (Test-Path -LiteralPath $classesDir) {
    Remove-Item -LiteralPath $classesDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

$sources = Get-ChildItem -Path (Join-Path $root "src") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
if (-not $sources) {
    throw "No Java source files found."
}

Write-Host "Compiling plugin..."
& $javac -encoding UTF-8 --release 11 -classpath (Resolve-Path -LiteralPath $JosmJar).Path -d $classesDir $sources
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE."
}

$manifest = @(
    "Manifest-Version: 1.0"
    "Plugin-Mainversion: 19044"
    "Plugin-Version: 1"
    "Plugin-Class: org.openstreetmap.josm.plugins.buildingtagshortcuts.BuildingTagShortcutsPlugin"
    "Plugin-Description: Shortcuts for setting building:levels and toggling building/building:part tags."
    "Plugin-Canloadatruntime: true"
    "Plugin-Minimum-Java-Version: 11"
    "Author: local"
    ""
)
$manifest | Set-Content -LiteralPath $manifestPath -Encoding ASCII

if (Test-Path -LiteralPath $jarPath) {
    Remove-Item -LiteralPath $jarPath -Force
}

Write-Host "Creating $jarPath..."
& $jar cfm $jarPath $manifestPath -C $classesDir .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE."
}

Write-Host "Built $jarPath"
