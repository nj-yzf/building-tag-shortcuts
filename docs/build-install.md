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
- Hold one digit and press another digit to set two-digit levels, such as pressing `1` first and then holding it while pressing `8` to update `building:levels` from `1` to `18`.
- The second digit can be `0`; repeated digits such as `11` are supported by using main-keyboard and numpad keys.
- Press `Ctrl+1` ... `Ctrl+9` to set building names with the configured suffix, such as `name=1栋`.
- Hold `Ctrl` and one digit, then press another digit to set two-digit building names, such as updating `name` from `2栋` to `23栋`.
- Press `Ctrl+Shift+D` to toggle `building` and `building:part`.
- Press `Ctrl+Shift+Q` to open the Building Height Tool.
- Use JOSM undo normally if a change is wrong.

`Ctrl+Shift+D` rules:

- all `building=*` -> all `building:part=*`
- all `building:part=*` -> all `building=*`
- mixed `building=*` and `building:part=*` -> only `building=*` objects convert
- no building tag -> add `building:part=yes`
- both tags on one object -> remove `building=*`

The height tool has:

- Simple mode: use a custom per-level height to calculate `height` from existing `building:levels`.
- Segmented mode: calculate total height from lower levels/lower height and upper levels/upper height. Enter any two of lower levels, upper levels, and total levels to fill the missing count.

Simple mode also handles optional tags:

- `building:min_level` -> `min_height=<per-level height>*building:min_level`
- `roof:height` is added to the calculated `height`

Height fields default to `3.6` m and support mouse-wheel adjustment in `0.1` m steps.

The height tool labels adapt to Chinese or English based on the default locale.

## 5. Settings

Open:

```text
Edit -> Preferences -> Building Tag Shortcuts
```

The settings page currently supports building-name suffixes. The default suffix list is `栋`, `幢`, `号楼`, and `栋` is selected by default. The page is designed as the plugin's general settings area, so more options can be added later.

It should appear as a top-level item in the preferences sidebar, not only as raw keys in Advanced Preferences.

## 6. Shortcut notes

JOSM already uses direct `1..9` for view/zoom actions. The plugin captures those key presses first when focus is not inside a text field, so they should work during normal map editing.

If JOSM does not show the desired toggle shortcut, open:

```text
Edit -> Preferences -> Keyboard Shortcuts
```

Search for:

```text
Building Tag Shortcuts
```

Then assign `Ctrl+Shift+D` or `Ctrl+Shift+Q` manually if needed. The direct `1..9` level shortcuts and `Ctrl+digit` name shortcuts are handled by the plugin's key dispatcher, so they may not behave like ordinary JOSM shortcut-preference entries.
