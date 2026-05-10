# Build and Install Guide

## 1. Install a JDK

Install JDK 11 or newer. JOSM documentation says Java 11 works and Java 17 LTS is preferred.

If you do not want to configure Java manually, run:

```powershell
.\scripts\setup-jdk.ps1
```

If Windows blocks script execution, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-jdk.ps1
```

This downloads a portable JDK 17 into `.tools\jdk-17`. It does not change system Java settings.

After installation, make sure one of these works in PowerShell:

```powershell
javac -version
```

or set `JAVA_HOME` to your JDK directory, for example:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
```

## 2. Build the plugin

Run:

```powershell
.\scripts\build.ps1
```

If Windows blocks script execution, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

The script downloads `josm-tested.jar` into `lib\` if it is missing, compiles the plugin, and writes:

```text
dist\building-tag-shortcuts.jar
```

## 3. Install into JOSM

Run:

```powershell
.\scripts\install.ps1
```

If Windows blocks script execution, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1
```

This copies the jar to:

```text
%APPDATA%\JOSM\plugins\building-tag-shortcuts.jar
```

Restart JOSM after installing.

Because this is a manually placed jar, open:

```text
Edit -> Preferences -> Plugins
```

The plugin should appear in the list after restart. Tick its checkbox if it is not enabled yet.

## 4. Use

- Select one or more OSM objects.
- Press `1` ... `9` to set `building:levels`.
- Press `Ctrl+Shift+D` to toggle `building` and `building:part`.
- Press `Ctrl+Shift+Q` to set or update `height=3.6*building:levels` on selected objects that already have `building:levels`.
- Use JOSM undo normally if a change is wrong.

## 5. Shortcut notes

JOSM already uses direct `1..9` for view/zoom actions. The plugin captures those key presses first when focus is not inside a text field, so they should work during normal map editing.

If JOSM does not show the desired toggle shortcut, open:

```text
Edit -> Preferences -> Keyboard Shortcuts
```

Search for:

```text
Building Tag Shortcuts
```

Then assign `Ctrl+Shift+D` or `Ctrl+Shift+Q` manually if needed. The direct `1..9` level shortcuts are handled by the plugin's key dispatcher, so they may not behave like ordinary JOSM shortcut-preference entries.
