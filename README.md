# Building Tag Shortcuts

A small JOSM plugin for faster building edits:

- `1` ... `9`: set `building:levels=1` ... `building:levels=9` on the current selection.
- Hold one digit and press another digit to set two-digit levels, for example hold `1` and press `8` to update `building:levels` from `1` to `18`.
- The second digit may be `0`, and repeated digits are supported by mixing main-keyboard and numpad keys, such as main `1` plus numpad `1` for `11`.
- `Ctrl+1` ... `Ctrl+9`: set `name=1栋` ... `name=9栋` by default.
- Hold `Ctrl` and one digit, then press another digit to set two-digit building names, for example `Ctrl+2` then `3` updates `name` from `2栋` to `23栋`.
- Building-name two-digit input supports `0` as the second digit and repeated digits via main-keyboard/numpad combinations.
- `Ctrl+Shift+D`: toggle selected objects between `building=*` and `building:part=*`, preserving the value.
- `Ctrl+Shift+Q`: open the Building Height Tool.

`Ctrl+Shift+D` is multi-selection aware:

- all selected objects are `building=*`: convert all to `building:part=*`
- all selected objects are `building:part=*`: convert all to `building=*`
- mixed `building=*` and `building:part=*`: convert only `building=*` objects; existing `building:part=*` objects stay unchanged
- no `building` or `building:part`: add `building:part=yes`
- both tags on one object: remove only `building=*`

The Building Height Tool has two sections:

- Simple mode: set or update `height=<per-level height>*building:levels` on selected objects.
- Segmented mode: calculate height from lower levels/lower height, upper levels/upper height, and total levels.

Valid numeric edits in either section are applied to the selected objects immediately, including mouse-wheel changes, so JOSM 3D previews can update while you tune values.

Each time the height tool is opened with `Ctrl+Shift+Q`, simple mode resets to `3.6` m and immediately applies that value to the current selection. If all selected objects share one valid `building:levels` value, that value is copied into segmented mode's total levels field. If selected objects have different `building:levels` values, total levels is left empty.

In segmented mode, total levels is the master value. It can be set from `building:levels` or typed manually. Lower levels plus upper levels always equals total levels; changing lower levels updates upper levels, and changing upper levels updates lower levels.

Segmented mode resets lower levels to `1` each time the tool opens. Segment level inputs are constrained so neither lower nor upper levels can exceed the total levels.

Consecutive mouse-wheel realtime height changes on the same selection are grouped into one undo step. If the selection changes or another edit happens in between, the next wheel changes start a separate undo step.

In simple mode only:

- If `building:min_level` exists, the tool also writes `min_height=<per-level height>*building:min_level`.
- If `roof:height` exists, the tool writes `height=<per-level height>*building:levels+roof:height`.

Height fields default to `3.6` m and can be adjusted with the mouse wheel in `0.1` m steps. Level-count fields also support mouse-wheel adjustment in `0.5` level steps.

The height tool uses compact numeric fields and shows Chinese labels when the JOSM/default locale is Chinese.

The building-name suffix is configurable in JOSM preferences. Defaults are `栋`, `幢`, and `号楼`, with `栋` selected by default.

The settings page appears as a top-level item in JOSM preferences: `Edit -> Preferences -> Building Tag Shortcuts`.

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

The current installed build uses `1..9` and held two-digit combinations for levels, `Ctrl+digit` combinations for building names, `Ctrl+Shift+D` for toggling, and `Ctrl+Shift+Q` for the height tool. Older experimental shortcuts `Ctrl+Alt+L` and `Ctrl+Alt+B` are no longer used.
