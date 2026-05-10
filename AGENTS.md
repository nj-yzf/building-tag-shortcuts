# Project Notes

## Project
- Name: building-tag-shortcuts
- Purpose: JOSM plugin for fast building tag edits.
- Workspace: `E:\Claude\JOSM个人插件`
- Primary package: `org.openstreetmap.josm.plugins.buildingtagshortcuts`

## Current User-Facing Behavior
- `1..9`: set `building:levels=1..9` on selected OSM objects.
- `Ctrl+Shift+D`: toggle selected objects between `building=*` and `building:part=*`, preserving the original value.
- Actions are also available under JOSM `Data -> Building Tag Shortcuts`.
- Current shortcut set has been user-verified as working well.

## User Goals
- Set `building:levels=1..9` on selected OSM objects through shortcuts.
- Toggle selected objects between `building=*` and `building:part=*`, preserving the original value.
- Keep the workflow usable for a user without Java background.
- Follow JOSM plugin APIs and document shortcut conflicts.

## Design Decisions
- Build as a minimal JOSM plugin jar, not as a JOSM preset, because the toggle operation must preserve per-object tag values.
- Use JOSM `JosmAction` for menu actions and keyboard shortcut registration.
- Use `ChangePropertyCommand` and `SequenceCommand` through `UndoRedoHandler`, so every operation is undoable in JOSM.
- Add actions under JOSM `Data` menu, because JOSM documents that menu as appropriate for plugin actions related to tagging schemes.
- Use a `KeyEventDispatcher` for direct `1..9` and `Ctrl+Shift+D`, so level shortcuts override JOSM view shortcuts during normal map editing while still avoiding text fields.

## Shortcut Analysis
- Original `Ctrl+1..9`: JOSM's official generated shortcut list shows these are already used by `TagEditHelper` recent-tag actions (`properties:recent:1` ... `properties:recent:9`).
- Current `1..9`: direct digit keys intentionally override JOSM view shortcuts when focus is not inside a text field.
- `Ctrl+Alt+L` did not work on the user's machine while the menu action and `Ctrl+Alt+B` did; likely intercepted before JOSM/Swing.
- Current `Ctrl+Shift+D`: selected from user-requested left-hand candidates because no exact conflict was found in the checked JOSM shortcut list.

## Build Requirements
- JDK 11 or newer is required to compile and run against current JOSM. JOSM documentation says Java 11 works and Java 17 LTS is preferred.
- This machine has a project-local portable Microsoft OpenJDK 17 under `.tools\jdk-17`.
- Build script downloads `josm-tested.jar` into `lib\` if missing.
- Install script copies the produced jar into `%APPDATA%\JOSM\plugins`.

## Progress
- 2026-05-01: Researched JOSM plugin development docs, shortcut docs, `JosmAction`, `Shortcut`, `ChangePropertyCommand`, `UndoRedoHandler`, `MainMenu`, and current shortcut list.
- 2026-05-01: Created initial plugin source and build/install documentation.
- 2026-05-01: Added `scripts/setup-jdk.ps1` and installed a portable Microsoft OpenJDK 17 under `.tools\jdk-17`.
- 2026-05-01: Built `dist\building-tag-shortcuts.jar` successfully.
- 2026-05-01: Installed plugin jar to `%APPDATA%\JOSM\plugins\building-tag-shortcuts.jar`.
- 2026-05-01: Changed toggle implementation to two explicit `ChangePropertyCommand` operations per object after testing.
- 2026-05-01: Replaced shortcuts with direct `1..9` for levels and `Ctrl+Shift+D` for toggle; added `KeyEventDispatcher`.
- 2026-05-01: User confirmed the current shortcut set works well; documentation updated to reflect final active behavior.

## Important References
- JOSM plugin development: https://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins
- JOSM plugin build-common manifest fields: https://josm.openstreetmap.de/browser/osm/applications/editors/josm/plugins/build-common.xml
- `JosmAction`: https://josm.openstreetmap.de/doc/org/openstreetmap/josm/actions/JosmAction.html
- `Shortcut`: https://josm.openstreetmap.de/doc/org/openstreetmap/josm/tools/Shortcut.html
- `ChangePropertyCommand`: https://josm.openstreetmap.de/browser/josm/trunk/src/org/openstreetmap/josm/command/ChangePropertyCommand.java
- `UndoRedoHandler`: https://josm.openstreetmap.de/doc/org/openstreetmap/josm/data/UndoRedoHandler.html
- JOSM shortcut list: https://josm.openstreetmap.de/wiki/DevelopersGuide/ShortcutsList
