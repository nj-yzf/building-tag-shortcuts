# Building Tag Shortcuts

A small JOSM plugin for faster building edits:

- `1` ... `9`: set `building:levels=1` ... `building:levels=9` on the current selection.
- `Ctrl+Shift+D`: toggle selected objects between `building=*` and `building:part=*`, preserving the value.
- `Ctrl+Shift+Q`: set or update `height=3.6*building:levels` on selected objects that already have `building:levels`.

The toggle is per object. For example:

- `building=yes` becomes `building:part=yes`
- `building=apartments` becomes `building:part=apartments`
- `building:part=yes` becomes `building=yes`

If both `building` and `building:part` exist on one selected object, the plugin treats `building` as the source and overwrites `building:part` with that value.

The height action skips selected objects without `building:levels` and skips invalid or non-positive level values.

## Shortcut Conflict

JOSM's generated shortcut list shows direct `1` ... `9` are already assigned to zoom/view actions. This plugin intentionally captures those keys first when focus is not inside a text field, because the level shortcuts are meant to override the view shortcuts during normal map editing.

The toggle shortcut `Ctrl+Shift+D` was chosen from the requested left-hand candidates because no exact `Ctrl+Shift+D` entry was found in the checked JOSM shortcut list.

The height shortcut `Ctrl+Shift+Q` was chosen because no exact `Ctrl+Shift+Q` entry was found in the checked JOSM shortcut list.

These shortcuts were tested in JOSM after installation and reported working.

## Build

You need JDK 11 or newer. JOSM currently requires Java 11+; Java 17 LTS is recommended by JOSM documentation.

If you do not have a JDK, run this once:

```powershell
.\scripts\setup-jdk.ps1
```

From this directory:

```powershell
.\scripts\build.ps1
```

If Windows blocks script execution, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

The plugin jar will be created at:

```text
dist\building-tag-shortcuts.jar
```

## Install

```powershell
.\scripts\install.ps1
```

If Windows blocks script execution, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1
```

Then restart JOSM and enable the plugin if needed.

For manual jars, JOSM documentation says the jar should appear in the plugin list after restart; tick its checkbox there to finish enabling it.

The actions are also available under JOSM's `Data` menu.

## Current Status

The current installed build uses `1..9` for levels, `Ctrl+Shift+D` for toggling, and `Ctrl+Shift+Q` for height-from-levels. Older experimental shortcuts `Ctrl+Alt+L` and `Ctrl+Alt+B` are no longer used.
