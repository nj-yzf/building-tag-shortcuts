// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.buildingtagshortcuts;

import java.awt.event.ActionEvent;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Adds shortcuts for common building tag edits.
 */
public class BuildingTagShortcutsPlugin extends Plugin {
    private static final String BUILDING = "building";
    private static final String BUILDING_PART = "building:part";
    private static final String BUILDING_LEVELS = "building:levels";
    private static final SetLevelsAction[] LEVEL_ACTIONS = new SetLevelsAction[10];
    private static ToggleBuildingPartAction toggleAction;
    private static boolean keyDispatcherRegistered;

    /**
     * Constructs the plugin and registers menu actions.
     *
     * @param info plugin information from JOSM
     */
    public BuildingTagShortcutsPlugin(PluginInformation info) {
        super(info);
        registerActions();
        registerKeyDispatcher();
    }

    private static void registerActions() {
        JMenu dataMenu = MainApplication.getMenu().dataMenu;
        JMenu pluginMenu = new JMenu("Building Tag Shortcuts");

        for (int level = 1; level <= 9; level++) {
            SetLevelsAction action = new SetLevelsAction(level);
            LEVEL_ACTIONS[level] = action;
            JMenuItem item = MainMenu.add(pluginMenu, action);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0 + level, 0));
        }

        pluginMenu.addSeparator();
        toggleAction = new ToggleBuildingPartAction();
        JMenuItem toggleItem = MainMenu.add(pluginMenu, toggleAction);
        toggleItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        dataMenu.add(pluginMenu);
        dataMenu.setVisible(true);
    }

    private static void registerKeyDispatcher() {
        if (keyDispatcherRegistered) {
            return;
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new BuildingKeyDispatcher());
        keyDispatcherRegistered = true;
    }

    private static final class BuildingKeyDispatcher implements KeyEventDispatcher {
        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getID() != KeyEvent.KEY_PRESSED || isTextFocus()) {
                return false;
            }

            int keyCode = event.getKeyCode();
            if (hasNoModifiers(event)) {
                int level = levelForKeyCode(keyCode);
                if (level > 0 && LEVEL_ACTIONS[level] != null) {
                    LEVEL_ACTIONS[level].actionPerformed(null);
                    return true;
                }
            }

            if (keyCode == KeyEvent.VK_D
                    && event.isControlDown()
                    && event.isShiftDown()
                    && !event.isAltDown()
                    && !event.isMetaDown()
                    && toggleAction != null) {
                toggleAction.actionPerformed(null);
                return true;
            }

            return false;
        }

        private static boolean isTextFocus() {
            return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextComponent;
        }

        private static boolean hasNoModifiers(KeyEvent event) {
            return !event.isAltDown()
                    && !event.isControlDown()
                    && !event.isShiftDown()
                    && !event.isMetaDown();
        }

        private static int levelForKeyCode(int keyCode) {
            if (keyCode >= KeyEvent.VK_1 && keyCode <= KeyEvent.VK_9) {
                return keyCode - KeyEvent.VK_0;
            }
            if (keyCode >= KeyEvent.VK_NUMPAD1 && keyCode <= KeyEvent.VK_NUMPAD9) {
                return keyCode - KeyEvent.VK_NUMPAD0;
            }
            return -1;
        }
    }

    private abstract static class BuildingSelectionAction extends JosmAction {
        BuildingSelectionAction(String name, String tooltip, Shortcut shortcut, String toolbarId) {
            super(name, (String) null, tooltip, shortcut, true, toolbarId, true);
        }

        @Override
        protected void updateEnabledState() {
            updateEnabledStateOnCurrentSelection();
        }

        @Override
        protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
            updateEnabledStateOnModifiableSelection(selection);
        }

        final Collection<OsmPrimitive> getSelection() {
            DataSet dataSet = MainApplication.getLayerManager().getEditDataSet();
            if (dataSet == null) {
                showWarning("No editable OSM data layer is active.");
                return null;
            }

            Collection<OsmPrimitive> selection = dataSet.getSelected();
            if (selection.isEmpty()) {
                showWarning("Select at least one object first.");
                return null;
            }

            return selection;
        }

        final boolean confirmOutlying(Collection<OsmPrimitive> selection) {
            return checkAndConfirmOutlyingOperation(
                    "change building tags",
                    "Change building tags",
                    "The selected objects are outside the downloaded area. Continue?",
                    "The selected objects are incomplete. Continue?",
                    selection,
                    null);
        }
    }

    private static final class SetLevelsAction extends BuildingSelectionAction {
        private final int level;

        SetLevelsAction(int level) {
            super(
                    "Set building:levels=" + level,
                    "Set building:levels=" + level + " on selected objects",
                    Shortcut.registerShortcut(
                            "buildingtagshortcuts:levels:" + level,
                            "Building Tag Shortcuts: Set building:levels=" + level,
                            KeyEvent.VK_0 + level,
                            Shortcut.DIRECT),
                    "buildingtagshortcuts:levels:" + level);
            this.level = level;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            setLevels(this, level);
        }
    }

    private static final class ToggleBuildingPartAction extends BuildingSelectionAction {
        ToggleBuildingPartAction() {
            super(
                    "Toggle building/building:part",
                    "Convert selected objects between building=* and building:part=*",
                    Shortcut.registerShortcut(
                            "buildingtagshortcuts:toggle-building-part",
                            "Building Tag Shortcuts: Toggle building/building:part",
                            KeyEvent.VK_D,
                            Shortcut.CTRL_SHIFT),
                    "buildingtagshortcuts:toggle-building-part");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            toggleBuildingPart(this);
        }
    }

    private static void setLevels(BuildingSelectionAction support, int level) {
        Collection<OsmPrimitive> selection = support.getSelection();
        if (selection == null || !support.confirmOutlying(selection)) {
            return;
        }

        UndoRedoHandler.getInstance().add(
                new ChangePropertyCommand(selection, BUILDING_LEVELS, Integer.toString(level)));
        showInfo("Set building:levels=" + level + " on " + selection.size() + " object(s).");
    }

    private static void toggleBuildingPart(BuildingSelectionAction support) {
        Collection<OsmPrimitive> selection = support.getSelection();
        if (selection == null || !support.confirmOutlying(selection)) {
            return;
        }

        List<Command> commands = new ArrayList<>();
        int buildingToPart = 0;
        int partToBuilding = 0;

        for (OsmPrimitive primitive : selection) {
            String buildingValue = primitive.get(BUILDING);
            String buildingPartValue = primitive.get(BUILDING_PART);

            if (!isEmpty(buildingValue)) {
                commands.add(changeTag(primitive, BUILDING_PART, buildingValue));
                commands.add(changeTag(primitive, BUILDING, null));
                buildingToPart++;
            } else if (!isEmpty(buildingPartValue)) {
                commands.add(changeTag(primitive, BUILDING, buildingPartValue));
                commands.add(changeTag(primitive, BUILDING_PART, null));
                partToBuilding++;
            }
        }

        if (commands.isEmpty()) {
            showWarning("No selected object has building=* or building:part=*.");
            return;
        }

        UndoRedoHandler.getInstance().add(new SequenceCommand("Toggle building/building:part", commands));
        showInfo("Converted " + buildingToPart + " building tag(s) and "
                + partToBuilding + " building:part tag(s).");
    }

    private static Command changeTag(OsmPrimitive primitive, String key, String value) {
        return new ChangePropertyCommand(Collections.singleton(primitive), key, value);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private static void showInfo(String message) {
        new Notification(message).setIcon(JOptionPane.INFORMATION_MESSAGE).show();
    }

    private static void showWarning(String message) {
        new Notification(message).setIcon(JOptionPane.WARNING_MESSAGE).show();
    }
}
