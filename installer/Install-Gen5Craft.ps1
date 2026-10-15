# Gen5Craft Windows installer (portable, no admin rights needed).
# Run from the extracted release folder (next to Gen5CraftLauncher.exe or its folder).
# Installs the launcher under %LOCALAPPDATA%\Programs\Gen5Craft and creates a desktop shortcut.
$ErrorActionPreference = "Stop"
$target = Join-Path $env:LOCALAPPDATA "Programs\Gen5Craft"
New-Item -ItemType Directory -Force $target | Out-Null

$source = $PSScriptRoot
if (-not (Test-Path (Join-Path $source "Gen5CraftLauncher\Gen5CraftLauncher.exe"))) {
    if (Test-Path (Join-Path $source "Gen5CraftLauncher.exe")) {
        Copy-Item (Join-Path $source "*") $target -Recurse -Force
    } else {
        Write-Host "Run this script from the extracted Gen5Craft release folder." -ForegroundColor Yellow
        exit 1
    }
} else {
    Copy-Item (Join-Path $source "Gen5CraftLauncher\*") $target -Recurse -Force
}

$exe = Join-Path $target "Gen5CraftLauncher.exe"
if (Test-Path $exe) {
    $shell = New-Object -ComObject WScript.Shell
    $shortcutPath = Join-Path ([Environment]::GetFolderPath("Desktop")) "Gen5Craft Launcher.lnk"
    $shortcut = $shell.CreateShortcut($shortcutPath)
    $shortcut.TargetPath = $exe
    $shortcut.WorkingDirectory = $target
    $shortcut.Save()
    Write-Host "Gen5Craft Launcher installed to $target and a desktop shortcut was created."
} else {
    Write-Host "Installed files to $target (launcher exe not found; check the extracted folder)."
}
Write-Host "Start it, then use Check / Update client and Play. Java 25 is bundled in the Windows app image."
