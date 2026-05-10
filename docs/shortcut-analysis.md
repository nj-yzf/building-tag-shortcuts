# Shortcut Analysis

Checked source: JOSM generated shortcut list, `DevelopersGuide/ShortcutsList`.

## Requested Level Shortcuts

The requested level shortcuts were changed to direct `1` ... `9`.

The official shortcut list shows direct digit keys are used by zoom/view actions. The plugin therefore uses a `KeyEventDispatcher` to capture `1..9` before JOSM's normal action handling when focus is not inside a text field.

The dispatcher also accepts numpad `1..9`.

The original `Ctrl+1..9` request conflicted with JOSM recent-tag actions (`properties:recent:1` ... `properties:recent:9`) and is no longer used.

## Toggle Shortcut

Chosen shortcut: `Ctrl+Shift+D`.

From the requested candidates, the checked list already contains conflicts for many exact combinations, including `Ctrl+Shift+A/B/C/E/F/R/S/V/W/X/Z`. No exact `Ctrl+Shift+D` entry was found.

The plugin also captures `Ctrl+Shift+D` through the same dispatcher, so it does not depend solely on menu accelerators.

## Height Shortcut

Chosen shortcut: `Ctrl+Shift+Q`.

The checked list contains `Q`, `Shift+Q`, and `Ctrl+Q`, but no exact `Ctrl+Shift+Q` entry was found. It is also a left-hand shortcut and is captured through the plugin dispatcher.

The action writes `height=3.6*building:levels` for selected objects that already have a parseable positive `building:levels` value. Objects without `building:levels` are not modified.

## Verified Behavior

The user verified that the current shortcut set works well:

- `1..9` set `building:levels`.
- `Ctrl+Shift+D` toggles `building` and `building:part`.
- `Ctrl+Shift+Q` sets `height` from `building:levels`.

Earlier `Ctrl+Alt+L` did not work on the user's machine even though the menu action and `Ctrl+Alt+B` did, so `Ctrl+Alt` chords were removed from the active design.
