# Shortcut Analysis

Checked source: JOSM generated shortcut list, `DevelopersGuide/ShortcutsList`.

## Requested Level Shortcuts

The requested level shortcuts were changed to direct `1` ... `9`.

The official shortcut list shows direct digit keys are used by zoom/view actions. The plugin therefore uses a `KeyEventDispatcher` to capture `1..9` before JOSM's normal action handling when focus is not inside a text field.

The dispatcher also accepts numpad `1..9`.

Two-digit level entry is handled as a short key state:

- Press one digit to set a one-digit level immediately.
- Hold the first digit and press a second digit to set `10*first+second`.
- For example, `1` writes `1` immediately; pressing `8` while `1` is still held updates it to `18`.
- The second digit can be `0`.
- Repeated two-digit values like `11` are supported by holding main-keyboard `1` and pressing numpad `1`; exact same-key repeats are ignored to avoid auto-repeat noise.

The original `Ctrl+1..9` request conflicted with JOSM recent-tag actions (`properties:recent:1` ... `properties:recent:9`) and is no longer used.

## Toggle Shortcut

Chosen shortcut: `Ctrl+Shift+D`.

From the requested candidates, the checked list already contains conflicts for many exact combinations, including `Ctrl+Shift+A/B/C/E/F/R/S/V/W/X/Z`. No exact `Ctrl+Shift+D` entry was found.

The plugin also captures `Ctrl+Shift+D` through the same dispatcher, so it does not depend solely on menu accelerators.

The toggle logic is selection-aware:

- all building-only objects are converted to building parts;
- all building-part-only objects are converted to buildings;
- mixed building-only and part-only selections convert only building-only objects;
- untagged objects receive `building:part=yes`;
- objects with both tags have `building=*` removed.

## Building Name Shortcuts

Current shortcut: `Ctrl+digit`.

The action writes `name=<number><suffix>` for selected objects. The suffix is stored in JOSM preferences and defaults to `栋`.

Two-digit entry follows the same immediate-update style as level entry:

- `Ctrl+2` writes `name=2栋` by default.
- Holding `Ctrl+2` and pressing `3` updates it to `name=23栋`.
- `0` is allowed as the second digit, and repeated digits are supported with main-keyboard/numpad combinations.

The suffix list is configurable from the plugin preference page. Defaults are `栋`, `幢`, and `号楼`.

## Height Tool Shortcut

Chosen shortcut: `Ctrl+Shift+Q`.

The checked list contains `Q`, `Shift+Q`, and `Ctrl+Q`, but no exact `Ctrl+Shift+Q` entry was found. It is also a left-hand shortcut and is captured through the plugin dispatcher.

The shortcut opens the Building Height Tool instead of directly writing height.

The tool has:

- Simple mode: `height=<custom per-level height>*building:levels`.
- Segmented mode: `height=lowerLevels*lowerHeight + upperLevels*upperHeight`.
- Lower levels, upper levels, and total levels are linked; entering any two is enough to calculate the third.
- Height fields default to `3.6` m and use mouse-wheel increments of `0.1` m.

Simple mode only:

- `building:min_level` produces `min_height=<custom per-level height>*building:min_level`.
- `roof:height` is added to the calculated `height`.

## Verified Behavior

The user verified that the current shortcut set works well:

- `1..9` set `building:levels`.
- `Ctrl+Shift+D` toggles `building` and `building:part`.
- `Ctrl+Shift+Q` opens the height tool.

Earlier `Ctrl+Alt+L` did not work on the user's machine even though the menu action and `Ctrl+Alt+B` did, so `Ctrl+Alt` chords were removed from the active design.
