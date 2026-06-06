# Project Notes

## Project
- Name: building-tag-shortcuts
- Purpose: JOSM plugin for fast building edits.
- Workspace: `E:\Claude\JOSM个人插件`
- Primary package: `org.openstreetmap.josm.plugins.buildingtagshortcuts`

## Current User-Facing Behavior
- `1..9`: set `building:levels=1..9` on selected OSM objects.
- Hold one digit and press another digit to set a two-digit `building:levels` value, e.g. hold `1` then press `8` for `18`.
- `Ctrl+1..9`: set `name=<number><suffix>` on selected OSM objects; default suffix is `栋`.
- Hold `Ctrl` and one digit, then press another digit to set two-digit building names such as `name=23栋`.
- `Ctrl+Shift+D`: toggle selected objects between `building=*` and `building:part=*`, preserving the original value.
- `Ctrl+Shift+Q`: open the Building Height Tool.
- JOSM preferences include a `Building Tag Shortcuts` page with configurable building-name suffixes.
- Actions are also available under JOSM `Data -> Building Tag Shortcuts`.
- The `1..9` and `Ctrl+Shift+D` shortcut set was user-verified as working well before the height feature was added.

## User Goals
- Set `building:levels=1..9` on selected OSM objects through shortcuts.
- Toggle selected objects between `building=*` and `building:part=*`, preserving the original value.
- Calculate and write `height` through a GUI tool with simple and segmented modes.
- In simple height mode, account for optional `building:min_level` and `roof:height`.
- Set building-number names with configurable suffixes such as `栋`, `幢`, and `号楼`.
- Skip objects without `building:levels`.
- Keep the workflow usable for a user without Java background.
- Follow JOSM plugin APIs and document shortcut conflicts.

## Design Decisions
- Build as a minimal JOSM plugin jar, not as a JOSM preset, because operations must preserve per-object tag values and compute derived tags.
- Use JOSM `JosmAction` for menu actions and keyboard shortcut registration.
- Use `ChangePropertyCommand` and `SequenceCommand` through `UndoRedoHandler`, so every operation is undoable in JOSM.
- Add actions under JOSM `Data` menu, because JOSM documents that menu as appropriate for plugin actions related to tagging schemes.
- Use a `KeyEventDispatcher` for direct `1..9`, `Ctrl+Shift+D`, and `Ctrl+Shift+Q`, so level shortcuts override JOSM view shortcuts during normal map editing while still avoiding text fields.
- Use `Plugin#getPreferenceSetting()` with `DefaultTabPreferenceSetting` and a non-null icon name to expose a top-level plugin settings page that can grow over time.
- Use a reusable non-modal Swing `JDialog` for advanced height calculation.
- Height tool UI uses compact numeric fields and `uiText(zh,en)` locale-adaptive labels.
- Documentation rule: keep existing English docs, and add/update separate Chinese versions for user-facing project docs.

## Shortcut Analysis
- Original `Ctrl+1..9`: JOSM's official generated shortcut list shows these are already used by `TagEditHelper` recent-tag actions (`properties:recent:1` ... `properties:recent:9`).
- Current `1..9`: direct digit keys intentionally override JOSM view shortcuts when focus is not inside a text field.
- Two-digit levels are handled by writing the first digit immediately, then updating to `10*first+second` if a second digit arrives while the first is held.
- Building names use `Ctrl+digit` and the same immediate-update two-digit pattern.
- Two-digit input supports `0` as the second digit and repeated digits when the second key is a different physical key, such as main-keyboard `1` plus numpad `1`.
- Toggle behavior is multi-selection aware: all building-only toggles to parts, all part-only toggles to buildings, mixed building/part converts only building-only objects, untagged objects get `building:part=yes`, and objects with both tags lose `building=*`.
- `Ctrl+Alt+L` did not work on the user's machine while the menu action and `Ctrl+Alt+B` did; likely intercepted before JOSM/Swing.
- Current `Ctrl+Shift+D`: selected from user-requested left-hand candidates because no exact conflict was found in the checked JOSM shortcut list.
- Current `Ctrl+Shift+Q`: selected for height-from-levels because the checked JOSM shortcut list contains `Q`, `Shift+Q`, and `Ctrl+Q`, but no exact `Ctrl+Shift+Q`.

## Build Requirements
- JDK 11 or newer is required to compile and run against current JOSM. JOSM documentation says Java 11 works and Java 17 LTS is preferred.
- This machine has a project-local portable Microsoft OpenJDK 17 under `.tools\jdk-17`.
- Build script downloads `josm-tested.jar` into `lib\` if missing.
- Install script copies the produced jar into `%APPDATA%\JOSM\plugins`.

## Backups
- 2026-05-10: Stable pre-height-feature backup created at `backups\stable-20260510-105722`.

## Progress
- 2026-05-01: Researched JOSM plugin development docs, shortcut docs, `JosmAction`, `Shortcut`, `ChangePropertyCommand`, `UndoRedoHandler`, `MainMenu`, and current shortcut list.
- 2026-05-01: Created initial plugin source and build/install documentation.
- 2026-05-01: Added `scripts/setup-jdk.ps1` and installed a portable Microsoft OpenJDK 17 under `.tools\jdk-17`.
- 2026-05-01: Built `dist\building-tag-shortcuts.jar` successfully.
- 2026-05-01: Installed plugin jar to `%APPDATA%\JOSM\plugins\building-tag-shortcuts.jar`.
- 2026-05-01: Changed toggle implementation to two explicit `ChangePropertyCommand` operations per object after testing.
- 2026-05-01: Replaced shortcuts with direct `1..9` for levels and `Ctrl+Shift+D` for toggle; added `KeyEventDispatcher`.
- 2026-05-01: User confirmed the current shortcut set works well; documentation updated to reflect final active behavior.
- 2026-05-10: Added initial `Ctrl+Shift+Q` height-from-levels feature.
- 2026-06-06: Added held two-digit level input while preserving one-digit shortcuts.
- 2026-06-06: Added project documentation rule requiring separate Chinese versions alongside existing English docs.
- 2026-06-06: Added building-name shortcuts and a JOSM preferences page for configurable suffixes.
- 2026-06-06: Fixed preferences page registration by giving the tab a non-null icon name so it appears in the left preference sidebar.
- 2026-06-06: Upgraded `Ctrl+Shift+Q` from direct height writing to a Building Height Tool dialog with simple and segmented calculation modes.
- 2026-06-06: Compacted the height tool layout and added Chinese/English adaptive labels.
- 2026-06-06: Enhanced simple height mode to write `min_height` from `building:min_level` and add `roof:height` to `height`.
- 2026-06-06: Fixed two-digit input edge cases for second digit `0` and repeated digits via main-keyboard/numpad combinations.
- 2026-06-06: Refined `Ctrl+Shift+D` multi-selection behavior for mixed building/building:part selections, untagged objects, and objects with both tags.

## Important References
- JOSM plugin development: https://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins
- JOSM plugin build-common manifest fields: https://josm.openstreetmap.de/browser/osm/applications/editors/josm/plugins/build-common.xml
- `JosmAction`: https://josm.openstreetmap.de/doc/org/openstreetmap/josm/actions/JosmAction.html
- `Shortcut`: https://josm.openstreetmap.de/doc/org/openstreetmap/josm/tools/Shortcut.html
- `ChangePropertyCommand`: https://josm.openstreetmap.de/browser/josm/trunk/src/org/openstreetmap/josm/command/ChangePropertyCommand.java
- `UndoRedoHandler`: https://josm.openstreetmap.de/doc/org/openstreetmap/josm/data/UndoRedoHandler.html
- JOSM shortcut list: https://josm.openstreetmap.de/wiki/DevelopersGuide/ShortcutsList
