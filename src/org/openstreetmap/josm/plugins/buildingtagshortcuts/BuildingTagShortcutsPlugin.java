// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.buildingtagshortcuts;

import java.awt.event.ActionEvent;
import java.awt.KeyEventDispatcher;
import java.awt.GridBagConstraints;
import java.awt.KeyboardFocusManager;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Adds shortcuts for common building tag edits.
 */
public class BuildingTagShortcutsPlugin extends Plugin {
    private static final String BUILDING = "building";
    private static final String BUILDING_PART = "building:part";
    private static final String BUILDING_LEVELS = "building:levels";
    private static final String BUILDING_MIN_LEVEL = "building:min_level";
    private static final String HEIGHT = "height";
    private static final String MIN_HEIGHT = "min_height";
    private static final String ROOF_HEIGHT = "roof:height";
    private static final String NAME = "name";
    private static final String PREF_NAME_SUFFIXES = "buildingtagshortcuts.name-suffixes";
    private static final String PREF_NAME_SUFFIX = "buildingtagshortcuts.name-suffix";
    private static final List<String> DEFAULT_NAME_SUFFIXES = Arrays.asList("栋", "幢", "号楼");
    private static final String DEFAULT_NAME_SUFFIX = "栋";
    private static final String PREF_SIMPLE_LEVEL_HEIGHT = "buildingtagshortcuts.height.simple-level-height";
    private static final String PREF_LOWER_LEVELS = "buildingtagshortcuts.height.lower-levels";
    private static final String PREF_LOWER_LEVEL_HEIGHT = "buildingtagshortcuts.height.lower-level-height";
    private static final String PREF_UPPER_LEVELS = "buildingtagshortcuts.height.upper-levels";
    private static final String PREF_UPPER_LEVEL_HEIGHT = "buildingtagshortcuts.height.upper-level-height";
    private static final String PREF_TOTAL_LEVELS = "buildingtagshortcuts.height.total-levels";
    private static final double DEFAULT_LEVEL_HEIGHT = 3.6;
    private static final double LEVEL_COUNT_STEP = 0.5;
    private static final int COMPACT_FIELD_COLUMNS = 5;
    private static final SetLevelsAction[] LEVEL_ACTIONS = new SetLevelsAction[10];
    private static ToggleBuildingPartAction toggleAction;
    private static OpenHeightToolAction openHeightToolAction;
    private static HeightToolDialog heightToolDialog;
    private static boolean keyDispatcherRegistered;
    private static WheelHeightCommand activeWheelHeightCommand;

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

    @Override
    public PreferenceSetting getPreferenceSetting() {
        return new BuildingTagShortcutsPreferenceSetting();
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

        openHeightToolAction = new OpenHeightToolAction();
        JMenuItem heightItem = MainMenu.add(pluginMenu, openHeightToolAction);
        heightItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));

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
        private int heldLevelDigit = -1;
        private int heldLevelKeyCode = -1;
        private int heldNameDigit = -1;
        private int heldNameKeyCode = -1;

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (isTextFocus()) {
                resetHeldDigits();
                return false;
            }

            int keyCode = event.getKeyCode();
            int digit = digitForKeyCode(keyCode);
            if (event.getID() == KeyEvent.KEY_RELEASED) {
                if (digit >= 0 && heldNameKeyCode == keyCode) {
                    resetHeldNameDigit();
                    return true;
                }
                if (keyCode == KeyEvent.VK_CONTROL) {
                    resetHeldNameDigit();
                    return false;
                }
            }

            if (hasNoModifiers(event)) {
                if (digit > 0 || (digit == 0 && heldLevelDigit >= 0)) {
                    return handleLevelDigit(event, digit);
                }
            }

            if (hasOnlyControlModifier(event)) {
                if (digit > 0 || (digit == 0 && heldNameDigit >= 0)) {
                    return handleNameDigit(event, digit, keyCode);
                }
            }

            if (event.getID() != KeyEvent.KEY_PRESSED) {
                return false;
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

            if (keyCode == KeyEvent.VK_Q
                    && event.isControlDown()
                    && event.isShiftDown()
                    && !event.isAltDown()
                    && !event.isMetaDown()
                    && openHeightToolAction != null) {
                openHeightToolAction.actionPerformed(null);
                return true;
            }

            return false;
        }

        private boolean handleLevelDigit(KeyEvent event, int digit) {
            if (event.getID() == KeyEvent.KEY_PRESSED) {
                if (heldLevelDigit < 0) {
                    if (digit == 0) {
                        return false;
                    }
                    heldLevelDigit = digit;
                    heldLevelKeyCode = event.getKeyCode();
                    setLevel(digit, digit);
                    return true;
                }

                if (heldLevelKeyCode == event.getKeyCode()) {
                    return true;
                }

                int combinedLevel = heldLevelDigit * 10 + digit;
                setLevel(combinedLevel, heldLevelDigit);
                return true;
            }

            if (event.getID() == KeyEvent.KEY_RELEASED) {
                if (heldLevelKeyCode == event.getKeyCode()) {
                    resetHeldLevelDigit();
                    return true;
                }

                return heldLevelDigit >= 0;
            }

            return false;
        }

        private boolean handleNameDigit(KeyEvent event, int digit, int keyCode) {
            if (event.getID() == KeyEvent.KEY_PRESSED) {
                if (heldNameDigit < 0) {
                    if (digit == 0) {
                        return false;
                    }
                    heldNameDigit = digit;
                    heldNameKeyCode = keyCode;
                    setBuildingName(digit);
                    return true;
                }

                if (heldNameKeyCode == keyCode) {
                    return true;
                }

                setBuildingName(heldNameDigit * 10 + digit);
                return true;
            }

            if (event.getID() == KeyEvent.KEY_RELEASED) {
                if (heldNameDigit == digit) {
                    resetHeldNameDigit();
                    return true;
                }

                return heldNameDigit >= 0;
            }

            return false;
        }

        private static void setLevel(int level, int supportDigit) {
            SetLevelsAction support = actionForLevel(supportDigit);
            if (support != null) {
                setLevels(support, level);
            }
        }

        private static SetLevelsAction actionForLevel(int level) {
            if (level >= 1 && level < LEVEL_ACTIONS.length) {
                return LEVEL_ACTIONS[level];
            }
            return new SetLevelsAction(level);
        }

        private void resetHeldLevelDigit() {
            heldLevelDigit = -1;
            heldLevelKeyCode = -1;
        }

        private void resetHeldNameDigit() {
            heldNameDigit = -1;
            heldNameKeyCode = -1;
        }

        private void resetHeldDigits() {
            resetHeldLevelDigit();
            resetHeldNameDigit();
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

        private static boolean hasOnlyControlModifier(KeyEvent event) {
            return event.isControlDown()
                    && !event.isAltDown()
                    && !event.isShiftDown()
                    && !event.isMetaDown();
        }

        private static int digitForKeyCode(int keyCode) {
            if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9) {
                return keyCode - KeyEvent.VK_0;
            }
            if (keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9) {
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

    private static final class OpenHeightToolAction extends BuildingSelectionAction {
        OpenHeightToolAction() {
            super(
                    "Open height tool",
                    "Open height calculation tool",
                    Shortcut.registerShortcut(
                            "buildingtagshortcuts:open-height-tool",
                            "Building Tag Shortcuts: Open height tool",
                            KeyEvent.VK_Q,
                            Shortcut.CTRL_SHIFT),
                    "buildingtagshortcuts:open-height-tool");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            openHeightTool(this);
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
        int bothRemovedBuilding = 0;
        int buildingOnlyCount = 0;
        int partOnlyCount = 0;
        int untaggedCount = 0;

        for (OsmPrimitive primitive : selection) {
            boolean hasBuilding = !isEmpty(primitive.get(BUILDING));
            boolean hasBuildingPart = !isEmpty(primitive.get(BUILDING_PART));

            if (hasBuilding && hasBuildingPart) {
                bothRemovedBuilding++;
            } else if (hasBuilding) {
                buildingOnlyCount++;
            } else if (hasBuildingPart) {
                partOnlyCount++;
            } else {
                untaggedCount++;
            }
        }

        boolean allBuildingOnly = buildingOnlyCount == selection.size();
        boolean allPartOnly = partOnlyCount == selection.size();
        boolean mixedBuildingAndPart = buildingOnlyCount > 0 && partOnlyCount > 0;

        for (OsmPrimitive primitive : selection) {
            String buildingValue = primitive.get(BUILDING);
            String buildingPartValue = primitive.get(BUILDING_PART);
            boolean hasBuilding = !isEmpty(buildingValue);
            boolean hasBuildingPart = !isEmpty(buildingPartValue);

            if (hasBuilding && hasBuildingPart) {
                commands.add(changeTag(primitive, BUILDING, null));
            } else if (hasBuilding && (allBuildingOnly || mixedBuildingAndPart || partOnlyCount == 0)) {
                commands.add(changeTag(primitive, BUILDING_PART, buildingValue));
                commands.add(changeTag(primitive, BUILDING, null));
            } else if (hasBuildingPart && allPartOnly) {
                commands.add(changeTag(primitive, BUILDING, buildingPartValue));
                commands.add(changeTag(primitive, BUILDING_PART, null));
            } else if (!hasBuilding && !hasBuildingPart) {
                commands.add(changeTag(primitive, BUILDING_PART, "yes"));
            }
        }

        if (commands.isEmpty()) {
            showWarning("No building tags changed.");
            return;
        }

        UndoRedoHandler.getInstance().add(new SequenceCommand("Toggle building/building:part", commands));
        showInfo("Updated building tags on " + selection.size() + " selected object(s)."
                + " building-only=" + buildingOnlyCount
                + ", part-only=" + partOnlyCount
                + ", untagged=" + untaggedCount
                + ", both=" + bothRemovedBuilding + ".");
    }

    private static void openHeightTool(BuildingSelectionAction support) {
        if (heightToolDialog == null) {
            heightToolDialog = new HeightToolDialog(support);
        }
        heightToolDialog.refreshFromSelection();
        heightToolDialog.applyDefaultSimpleHeightOnOpen();
        heightToolDialog.setVisible(true);
        heightToolDialog.toFront();
    }

    private static void applySimpleHeightFromLevels(BuildingSelectionAction support, String levelHeightText) {
        applySimpleHeightFromLevels(support, levelHeightText, true);
    }

    private static void applySimpleHeightFromLevels(BuildingSelectionAction support,
            String levelHeightText, boolean showMessages) {
        applySimpleHeightFromLevels(support, levelHeightText, showMessages, false);
    }

    private static void applySimpleHeightFromLevels(BuildingSelectionAction support,
            String levelHeightText, boolean showMessages, boolean mergeWheelUndo) {
        Double levelHeight = parsePositiveNumber(levelHeightText);
        if (levelHeight == null) {
            if (showMessages) {
                showWarning("Enter a valid positive per-level height.");
            }
            return;
        }

        Config.getPref().put(PREF_SIMPLE_LEVEL_HEIGHT, formatHeight(levelHeight));
        applySimpleHeightFromLevelsWithMultiplier(support, levelHeight, showMessages, mergeWheelUndo);
    }

    private static void applySimpleHeightFromLevelsWithMultiplier(BuildingSelectionAction support,
            double levelHeight, boolean showMessages) {
        applySimpleHeightFromLevelsWithMultiplier(support, levelHeight, showMessages, false);
    }

    private static void applySimpleHeightFromLevelsWithMultiplier(BuildingSelectionAction support,
            double levelHeight, boolean showMessages, boolean mergeWheelUndo) {
        Collection<OsmPrimitive> selection = support.getSelection();
        if (selection == null || !support.confirmOutlying(selection)) {
            return;
        }

        List<Command> commands = new ArrayList<>();
        Map<OsmPrimitive, Map<String, String>> finalTags = new LinkedHashMap<>();
        int eligible = 0;
        int changed = 0;
        int skippedInvalid = 0;
        int minHeightChanged = 0;
        int roofHeightAdded = 0;

        for (OsmPrimitive primitive : selection) {
            String levelsValue = primitive.get(BUILDING_LEVELS);
            if (isEmpty(levelsValue)) {
                continue;
            }

            Double levels = parsePositiveNumber(levelsValue);
            if (levels == null) {
                skippedInvalid++;
                continue;
            }
            eligible++;

            double height = levelHeight * levels;
            Double roofHeight = parsePositiveNumber(primitive.get(ROOF_HEIGHT));
            if (roofHeight != null) {
                height += roofHeight;
                roofHeightAdded++;
            }

            if (addTagChange(commands, finalTags, primitive, HEIGHT, formatHeight(height))) {
                changed++;
            }

            Double minLevel = parsePositiveNumber(primitive.get(BUILDING_MIN_LEVEL));
            if (minLevel != null) {
                if (addTagChange(commands, finalTags, primitive, MIN_HEIGHT, formatHeight(levelHeight * minLevel))) {
                    minHeightChanged++;
                }
            }
        }

        if (commands.isEmpty()) {
            if (!showMessages) {
                return;
            }
            if (eligible > 0) {
                showInfo("Height tags are already up to date.");
                return;
            }
            if (skippedInvalid > 0) {
                showWarning("No height changed. " + skippedInvalid
                        + " object(s) have invalid building:levels values.");
            } else {
                showWarning("No selected object has building:levels.");
            }
            return;
        }

        addHeightCommand("Set height from building:levels", commands, selection, finalTags, mergeWheelUndo);
        if (!showMessages) {
            return;
        }
        String message = "Set height on " + changed + " object(s).";
        if (minHeightChanged > 0) {
            message += " Set min_height on " + minHeightChanged + " object(s).";
        }
        if (roofHeightAdded > 0) {
            message += " Added roof:height on " + roofHeightAdded + " object(s).";
        }
        showInfo(message);
    }

    private static void applySegmentHeight(BuildingSelectionAction support,
            String lowerLevelsText, String lowerHeightText,
            String upperLevelsText, String upperHeightText,
            String totalLevelsText) {
        applySegmentHeight(support, lowerLevelsText, lowerHeightText, upperLevelsText, upperHeightText,
                totalLevelsText, true);
    }

    private static void applySegmentHeight(BuildingSelectionAction support,
            String lowerLevelsText, String lowerHeightText,
            String upperLevelsText, String upperHeightText,
            String totalLevelsText, boolean showMessages) {
        applySegmentHeight(support, lowerLevelsText, lowerHeightText, upperLevelsText, upperHeightText,
                totalLevelsText, showMessages, false);
    }

    private static void applySegmentHeight(BuildingSelectionAction support,
            String lowerLevelsText, String lowerHeightText,
            String upperLevelsText, String upperHeightText,
            String totalLevelsText, boolean showMessages, boolean mergeWheelUndo) {
        Double lowerHeight = parsePositiveNumber(lowerHeightText);
        Double upperHeight = parsePositiveNumber(upperHeightText);
        if (lowerHeight == null || upperHeight == null) {
            if (showMessages) {
                showWarning("Enter valid positive lower and upper level heights.");
            }
            return;
        }

        Double lowerLevels = parsePositiveNumber(lowerLevelsText);
        Double upperLevels = parsePositiveNumber(upperLevelsText);
        Double totalLevels = parsePositiveNumber(totalLevelsText);

        int knownLevelCounts = 0;
        knownLevelCounts += lowerLevels == null ? 0 : 1;
        knownLevelCounts += upperLevels == null ? 0 : 1;
        knownLevelCounts += totalLevels == null ? 0 : 1;
        if (knownLevelCounts < 2) {
            if (showMessages) {
                showWarning("Enter at least two of lower levels, upper levels, and total levels.");
            }
            return;
        }

        if (totalLevels == null) {
            totalLevels = lowerLevels + upperLevels;
        } else if (lowerLevels == null) {
            lowerLevels = totalLevels - upperLevels;
        } else if (upperLevels == null) {
            upperLevels = totalLevels - lowerLevels;
        }

        if (lowerLevels == null || upperLevels == null || totalLevels == null
                || lowerLevels <= 0 || upperLevels <= 0 || totalLevels <= 0) {
            if (showMessages) {
                showWarning("Computed level counts must be positive.");
            }
            return;
        }

        double tolerance = 0.000001;
        if (Math.abs((lowerLevels + upperLevels) - totalLevels) > tolerance) {
            if (showMessages) {
                showWarning("Lower levels plus upper levels must equal total levels.");
            }
            return;
        }

        double height = lowerLevels * lowerHeight + upperLevels * upperHeight;
        setSegmentHeightOnSelection(support, height, totalLevels, showMessages, mergeWheelUndo);

        Config.getPref().put(PREF_LOWER_LEVELS, formatHeight(lowerLevels));
        Config.getPref().put(PREF_LOWER_LEVEL_HEIGHT, formatHeight(lowerHeight));
        Config.getPref().put(PREF_UPPER_LEVELS, formatHeight(upperLevels));
        Config.getPref().put(PREF_UPPER_LEVEL_HEIGHT, formatHeight(upperHeight));
        Config.getPref().put(PREF_TOTAL_LEVELS, formatHeight(totalLevels));
    }

    private static void setHeightOnSelection(BuildingSelectionAction support, double height) {
        Collection<OsmPrimitive> selection = support.getSelection();
        if (selection == null || !support.confirmOutlying(selection)) {
            return;
        }

        UndoRedoHandler.getInstance().add(new ChangePropertyCommand(selection, HEIGHT, formatHeight(height)));
        showInfo("Set height=" + formatHeight(height) + " on " + selection.size() + " object(s).");
    }

    private static void setSegmentHeightOnSelection(BuildingSelectionAction support,
            double height, double totalLevels, boolean showMessages) {
        setSegmentHeightOnSelection(support, height, totalLevels, showMessages, false);
    }

    private static void setSegmentHeightOnSelection(BuildingSelectionAction support,
            double height, double totalLevels, boolean showMessages, boolean mergeWheelUndo) {
        Collection<OsmPrimitive> selection = support.getSelection();
        if (selection == null || !support.confirmOutlying(selection)) {
            return;
        }

        List<Command> commands = new ArrayList<>();
        Map<OsmPrimitive, Map<String, String>> finalTags = new LinkedHashMap<>();
        addSelectionTagChange(commands, finalTags, selection, HEIGHT, formatHeight(height));
        addSelectionTagChange(commands, finalTags, selection, BUILDING_LEVELS, formatHeight(totalLevels));
        if (commands.isEmpty()) {
            if (showMessages) {
                showInfo("Segmented height tags are already up to date.");
            }
            return;
        }
        addHeightCommand("Set segmented building height", commands, selection, finalTags, mergeWheelUndo);
        if (showMessages) {
            showInfo("Set height=" + formatHeight(height) + " and building:levels="
                    + formatHeight(totalLevels) + " on " + selection.size() + " object(s).");
        }
    }

    private static void setBuildingName(int number) {
        SetLevelsAction support = LEVEL_ACTIONS[1];
        if (support == null) {
            showWarning("Building name action is not ready.");
            return;
        }

        Collection<OsmPrimitive> selection = support.getSelection();
        if (selection == null || !support.confirmOutlying(selection)) {
            return;
        }

        String name = number + getNameSuffix();
        UndoRedoHandler.getInstance().add(new ChangePropertyCommand(selection, NAME, name));
        showInfo("Set name=" + name + " on " + selection.size() + " object(s).");
    }

    private static Command changeTag(OsmPrimitive primitive, String key, String value) {
        return new ChangePropertyCommand(Collections.singleton(primitive), key, value);
    }

    private static boolean addTagChange(List<Command> commands, Map<OsmPrimitive, Map<String, String>> finalTags,
            OsmPrimitive primitive, String key, String value) {
        if (value.equals(primitive.get(key))) {
            return false;
        }
        commands.add(changeTag(primitive, key, value));
        putFinalTag(finalTags, primitive, key, value);
        return true;
    }

    private static boolean addSelectionTagChange(List<Command> commands,
            Map<OsmPrimitive, Map<String, String>> finalTags,
            Collection<OsmPrimitive> selection, String key, String value) {
        boolean changed = false;
        for (OsmPrimitive primitive : selection) {
            if (!value.equals(primitive.get(key))) {
                changed = true;
                putFinalTag(finalTags, primitive, key, value);
            }
        }
        if (changed) {
            commands.add(new ChangePropertyCommand(selection, key, value));
        }
        return changed;
    }

    private static void putFinalTag(Map<OsmPrimitive, Map<String, String>> finalTags,
            OsmPrimitive primitive, String key, String value) {
        finalTags.computeIfAbsent(primitive, ignored -> new LinkedHashMap<>()).put(key, value);
    }

    private static void addHeightCommand(String description, List<Command> commands,
            Collection<OsmPrimitive> selection, Map<OsmPrimitive, Map<String, String>> finalTags,
            boolean mergeWheelUndo) {
        if (!mergeWheelUndo) {
            activeWheelHeightCommand = null;
            UndoRedoHandler.getInstance().add(new SequenceCommand(description, commands));
            return;
        }

        if (finalTags.isEmpty()) {
            return;
        }

        Command lastCommand = UndoRedoHandler.getInstance().getLastCommand();
        if (activeWheelHeightCommand != null
                && lastCommand == activeWheelHeightCommand
                && activeWheelHeightCommand.matches(selection)) {
            activeWheelHeightCommand.updateFinalTags(finalTags);
        } else {
            activeWheelHeightCommand = new WheelHeightCommand(description, selection, finalTags);
            UndoRedoHandler.getInstance().add(activeWheelHeightCommand);
        }
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private static Double parsePositiveNumber(String value) {
        if (value == null) {
            return null;
        }
        try {
            double number = Double.parseDouble(value.trim());
            if (Double.isFinite(number) && number > 0) {
                return number;
            }
        } catch (NumberFormatException ignore) {
            // Invalid OSM tag values are skipped without modifying the object.
        }
        return null;
    }

    private static String formatHeight(double height) {
        double rounded = Math.round(height * 10.0) / 10.0;
        if (rounded == Math.rint(rounded)) {
            return Integer.toString((int) rounded);
        }
        return Double.toString(rounded);
    }

    private static String uiText(String zh, String en) {
        return Locale.getDefault().getLanguage().equals(Locale.CHINESE.getLanguage()) ? zh : en;
    }

    private static List<String> getNameSuffixes() {
        List<String> values = Config.getPref().getList(PREF_NAME_SUFFIXES, DEFAULT_NAME_SUFFIXES);
        return normalizeSuffixes(values);
    }

    private static String getNameSuffix() {
        String suffix = Config.getPref().get(PREF_NAME_SUFFIX, DEFAULT_NAME_SUFFIX);
        if (isEmpty(suffix)) {
            return DEFAULT_NAME_SUFFIX;
        }
        return suffix;
    }

    private static final class WheelHeightCommand extends Command {
        private final String description;
        private final List<OsmPrimitive> primitives;
        private final Map<OsmPrimitive, PrimitiveData> originalData = new LinkedHashMap<>();
        private Map<OsmPrimitive, Map<String, String>> finalTags;

        WheelHeightCommand(String description, Collection<OsmPrimitive> primitives,
                Map<OsmPrimitive, Map<String, String>> finalTags) {
            super(primitives.iterator().next().getDataSet());
            this.description = description;
            this.primitives = new ArrayList<>(primitives);
            for (OsmPrimitive primitive : this.primitives) {
                originalData.put(primitive, primitive.save());
            }
            this.finalTags = copyTags(finalTags);
        }

        boolean matches(Collection<OsmPrimitive> selection) {
            return primitives.equals(new ArrayList<>(selection));
        }

        void updateFinalTags(Map<OsmPrimitive, Map<String, String>> tags) {
            finalTags = copyTags(tags);
            applyFinalTags();
        }

        @Override
        public boolean executeCommand() {
            super.executeCommand();
            applyFinalTags();
            return true;
        }

        @Override
        public void undoCommand() {
            for (Map.Entry<OsmPrimitive, PrimitiveData> entry : originalData.entrySet()) {
                entry.getKey().load(entry.getValue());
            }
        }

        @Override
        public void fillModifiedData(Collection<OsmPrimitive> modified,
                Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
            modified.addAll(primitives);
        }

        @Override
        public String getDescriptionText() {
            return description;
        }

        @Override
        public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
            return primitives;
        }

        private void applyFinalTags() {
            for (Map.Entry<OsmPrimitive, Map<String, String>> entry : finalTags.entrySet()) {
                OsmPrimitive primitive = entry.getKey();
                for (Map.Entry<String, String> tag : entry.getValue().entrySet()) {
                    String value = tag.getValue();
                    if (value == null) {
                        primitive.remove(tag.getKey());
                    } else {
                        primitive.put(tag.getKey(), value);
                    }
                }
            }
        }

        private static Map<OsmPrimitive, Map<String, String>> copyTags(
                Map<OsmPrimitive, Map<String, String>> tags) {
            Map<OsmPrimitive, Map<String, String>> copy = new LinkedHashMap<>();
            for (Map.Entry<OsmPrimitive, Map<String, String>> entry : tags.entrySet()) {
                copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
            }
            return copy;
        }
    }

    private static List<String> normalizeSuffixes(Collection<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }

        if (normalized.isEmpty()) {
            normalized.addAll(DEFAULT_NAME_SUFFIXES);
        }
        return new ArrayList<>(normalized);
    }

    private static final class HeightToolDialog extends JDialog {
        private final transient BuildingSelectionAction support;
        private final JosmTextField simpleLevelHeightField = new JosmTextField(COMPACT_FIELD_COLUMNS);
        private final JosmTextField lowerLevelsField = new JosmTextField(COMPACT_FIELD_COLUMNS);
        private final JosmTextField lowerHeightField = new JosmTextField(COMPACT_FIELD_COLUMNS);
        private final JosmTextField upperLevelsField = new JosmTextField(COMPACT_FIELD_COLUMNS);
        private final JosmTextField upperHeightField = new JosmTextField(COMPACT_FIELD_COLUMNS);
        private final JosmTextField totalLevelsField = new JosmTextField(COMPACT_FIELD_COLUMNS);
        private boolean suppressRealtimeApply;
        private boolean realtimeApplyQueued;
        private boolean simpleRealtimeDirty;
        private boolean segmentRealtimeDirty;
        private boolean simpleWheelDirty;
        private boolean segmentWheelDirty;
        private JosmTextField lastEditedSegmentLevelField;

        HeightToolDialog(BuildingSelectionAction support) {
            super(MainApplication.getMainFrame(), uiText("建筑高度工具", "Building Height Tool"), false);
            this.support = support;
            setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            buildGui();
            pack();
            setLocationRelativeTo(MainApplication.getMainFrame());
        }

        void refreshFromSelection() {
            suppressRealtimeApply = true;
            simpleLevelHeightField.setText(formatHeight(DEFAULT_LEVEL_HEIGHT));
            if (isEmpty(lowerHeightField.getText())) {
                lowerHeightField.setText(Config.getPref().get(
                        PREF_LOWER_LEVEL_HEIGHT, formatHeight(DEFAULT_LEVEL_HEIGHT)));
            }
            if (isEmpty(upperHeightField.getText())) {
                upperHeightField.setText(Config.getPref().get(
                        PREF_UPPER_LEVEL_HEIGHT, formatHeight(DEFAULT_LEVEL_HEIGHT)));
            }
            lowerLevelsField.setText("1");
            upperLevelsField.setText("");
            totalLevelsField.setText(getCommonSelectedBuildingLevels());
            syncSegmentCountsFromTotal();
            suppressRealtimeApply = false;
        }

        void applyDefaultSimpleHeightOnOpen() {
            applySimpleHeightFromLevels(support, formatHeight(DEFAULT_LEVEL_HEIGHT), false);
        }

        private void buildGui() {
            JPanel content = new JPanel(new GridBagLayout());
            content.add(new JLabel(uiText("按楼层数计算", "Simple height")),
                    GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 0, 4).anchor(GridBagConstraints.WEST));

            JPanel simplePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            simplePanel.add(new JLabel(uiText("每层高(m):", "Per-level (m):")));
            simpleLevelHeightField.setText(Config.getPref().get(
                    PREF_SIMPLE_LEVEL_HEIGHT, formatHeight(DEFAULT_LEVEL_HEIGHT)));
            addHeightWheel(simpleLevelHeightField);
            simplePanel.add(simpleLevelHeightField);

            JButton simpleApplyButton = new JButton(uiText("应用", "Apply"));
            simpleApplyButton.addActionListener(event ->
                    applySimpleHeightFromLevels(support, simpleLevelHeightField.getText()));
            simplePanel.add(simpleApplyButton);
            content.add(simplePanel, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 0, 6));

            content.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0, 8, 0, 8));
            content.add(new JLabel(uiText("分段计算", "Segmented height")),
                    GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 0, 4).anchor(GridBagConstraints.WEST));

            JPanel segmentedFields = new JPanel(new GridBagLayout());
            addLabeledField(segmentedFields, uiText("下段层数:", "Lower levels:"), lowerLevelsField, false, false);
            addLabeledField(segmentedFields, uiText("下段层高:", "Lower height:"), lowerHeightField, true, true);
            addLabeledField(segmentedFields, uiText("上段层数:", "Upper levels:"), upperLevelsField, false, false);
            addLabeledField(segmentedFields, uiText("上段层高:", "Upper height:"), upperHeightField, true, true);
            addLabeledField(segmentedFields, uiText("总层数:", "Total levels:"), totalLevelsField, false, true);
            content.add(segmentedFields, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 0, 4));

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            JButton fillButton = new JButton(uiText("补全层数", "Fill count"));
            fillButton.addActionListener(event -> {
                fillMissingSegmentCount();
                applySegmentHeightRealtime();
            });
            buttons.add(fillButton);

            JButton segmentedApplyButton = new JButton(uiText("应用分段高度", "Apply segmented"));
            segmentedApplyButton.addActionListener(event -> applySegmentHeight(
                    support,
                    lowerLevelsField.getText(), lowerHeightField.getText(),
                    upperLevelsField.getText(), upperHeightField.getText(),
                    totalLevelsField.getText()));
            buttons.add(segmentedApplyButton);

            content.add(buttons, GBC.eol().fill(GBC.HORIZONTAL));
            setContentPane(content);
            addRealtimeHandlers();
        }

        private void addLabeledField(JPanel panel, String label, JosmTextField field,
                boolean heightField, boolean endOfLine) {
            panel.add(new JLabel(label), GBC.std().anchor(GridBagConstraints.EAST).insets(0, 0, 4, 4));
            if (heightField) {
                field.setText(Config.getPref().get(
                        (label.startsWith("下") || label.startsWith("Lower"))
                                ? PREF_LOWER_LEVEL_HEIGHT : PREF_UPPER_LEVEL_HEIGHT,
                        formatHeight(DEFAULT_LEVEL_HEIGHT)));
                addHeightWheel(field);
            } else {
                addLevelWheel(field);
            }
            if (endOfLine) {
                panel.add(field, GBC.eol().anchor(GridBagConstraints.WEST).insets(0, 0, 8, 4));
            } else {
                panel.add(field, GBC.std().anchor(GridBagConstraints.WEST).insets(0, 0, 12, 4));
            }
        }

        private void fillMissingSegmentCount() {
            Double lower = parsePositiveNumber(lowerLevelsField.getText());
            Double upper = parsePositiveNumber(upperLevelsField.getText());
            Double total = parsePositiveNumber(totalLevelsField.getText());

            if (total == null) {
                showWarning("Enter total levels first.");
                return;
            }

            if (lower == null && upper == null) {
                showWarning("Enter lower or upper levels first.");
            } else if (lower == null) {
                double computed = total - upper;
                if (computed <= 0) {
                    showWarning("Computed lower levels must be positive.");
                    return;
                }
                setFieldTextSilently(lowerLevelsField, formatHeight(computed));
            } else if (upper == null) {
                double computed = total - lower;
                if (computed <= 0) {
                    showWarning("Computed upper levels must be positive.");
                    return;
                }
                setFieldTextSilently(upperLevelsField, formatHeight(computed));
            } else if (Math.abs((lower + upper) - total) > 0.000001) {
                showWarning("Lower levels plus upper levels must equal total levels.");
            }
        }

        private void addHeightWheel(JosmTextField field) {
            field.addMouseWheelListener(event -> adjustNumericField(field, event, 0.1, true));
        }

        private void addLevelWheel(JosmTextField field) {
            field.addMouseWheelListener(event -> adjustNumericField(field, event, LEVEL_COUNT_STEP, false));
        }

        private void adjustNumericField(JosmTextField field, MouseWheelEvent event,
                double step, boolean simpleMode) {
            double current = step;
            Double parsed = parsePositiveNumber(field.getText());
            if (parsed != null) {
                current = parsed;
            }

            double adjusted = Math.max(step, current - event.getWheelRotation() * step);
            if (simpleMode) {
                simpleWheelDirty = true;
            } else {
                segmentWheelDirty = true;
            }
            field.setText(formatHeight(adjusted));
            event.consume();
        }

        private void addRealtimeHandlers() {
            addRealtimeHandler(simpleLevelHeightField, true);
            addSegmentLevelHandler(lowerLevelsField);
            addRealtimeHandler(lowerHeightField, false);
            addSegmentLevelHandler(upperLevelsField);
            addRealtimeHandler(upperHeightField, false);
            addSegmentLevelHandler(totalLevelsField);
        }

        private void addRealtimeHandler(JosmTextField field, boolean simpleMode) {
            field.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    run();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    run();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    run();
                }

                private void run() {
                    if (!suppressRealtimeApply) {
                        queueRealtimeApply(simpleMode);
                    }
                }
            });
        }

        private void addSegmentLevelHandler(JosmTextField field) {
            field.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    run();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    run();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    run();
                }

                private void run() {
                    if (!suppressRealtimeApply) {
                        lastEditedSegmentLevelField = field;
                        queueRealtimeApply(false);
                    }
                }
            });
        }

        private void queueRealtimeApply(boolean simpleMode) {
            if (simpleMode) {
                simpleRealtimeDirty = true;
            } else {
                segmentRealtimeDirty = true;
            }
            if (realtimeApplyQueued) {
                return;
            }

            realtimeApplyQueued = true;
            SwingUtilities.invokeLater(() -> {
                realtimeApplyQueued = false;
                if (!suppressRealtimeApply) {
                    runQueuedRealtimeApply();
                }
            });
        }

        private void runQueuedRealtimeApply() {
            boolean runSimple = simpleRealtimeDirty;
            boolean runSegment = segmentRealtimeDirty;
            boolean mergeSimpleWheel = simpleWheelDirty;
            boolean mergeSegmentWheel = segmentWheelDirty;
            simpleRealtimeDirty = false;
            segmentRealtimeDirty = false;
            simpleWheelDirty = false;
            segmentWheelDirty = false;

            if (runSegment) {
                syncSegmentCountsFromTotal();
                applySegmentHeightRealtime(mergeSegmentWheel);
            }
            if (runSimple) {
                applySimpleHeightRealtime(mergeSimpleWheel);
            }
        }

        private void syncSegmentCountsFromTotal() {
            Double total = parsePositiveNumber(totalLevelsField.getText());
            if (total == null) {
                return;
            }
            if (total <= LEVEL_COUNT_STEP) {
                return;
            }

            Double lower = parsePositiveNumber(lowerLevelsField.getText());
            Double upper = parsePositiveNumber(upperLevelsField.getText());
            if (lastEditedSegmentLevelField == upperLevelsField && upper != null) {
                upper = clampSegmentLevel(upper, total);
                setFieldTextSilently(upperLevelsField, formatHeight(upper));
                double computedLower = total - upper;
                setFieldTextSilently(lowerLevelsField, formatHeight(computedLower));
            } else if (lower != null) {
                lower = clampSegmentLevel(lower, total);
                setFieldTextSilently(lowerLevelsField, formatHeight(lower));
                double computedUpper = total - lower;
                setFieldTextSilently(upperLevelsField, formatHeight(computedUpper));
            } else if (upper != null) {
                upper = clampSegmentLevel(upper, total);
                setFieldTextSilently(upperLevelsField, formatHeight(upper));
                double computedLower = total - upper;
                setFieldTextSilently(lowerLevelsField, formatHeight(computedLower));
            }
        }

        private double clampSegmentLevel(double value, double total) {
            return Math.max(LEVEL_COUNT_STEP, Math.min(value, total - LEVEL_COUNT_STEP));
        }

        private void setFieldTextSilently(JosmTextField field, String value) {
            if (value.equals(field.getText())) {
                return;
            }

            boolean previous = suppressRealtimeApply;
            suppressRealtimeApply = true;
            field.setText(value);
            suppressRealtimeApply = previous;
        }

        private void applySimpleHeightRealtime() {
            applySimpleHeightRealtime(false);
        }

        private void applySimpleHeightRealtime(boolean mergeWheelUndo) {
            applySimpleHeightFromLevels(support, simpleLevelHeightField.getText(), false, mergeWheelUndo);
        }

        private void applySegmentHeightRealtime() {
            applySegmentHeightRealtime(false);
        }

        private void applySegmentHeightRealtime(boolean mergeWheelUndo) {
            applySegmentHeight(
                    support,
                    lowerLevelsField.getText(), lowerHeightField.getText(),
                    upperLevelsField.getText(), upperHeightField.getText(),
                    totalLevelsField.getText(), false, mergeWheelUndo);
        }

        private String getCommonSelectedBuildingLevels() {
            Collection<OsmPrimitive> selection = support.getSelection();
            if (selection == null) {
                return "";
            }

            String common = null;
            for (OsmPrimitive primitive : selection) {
                String levels = primitive.get(BUILDING_LEVELS);
                if (isEmpty(levels)) {
                    continue;
                }
                if (common == null) {
                    common = levels;
                } else if (!common.equals(levels)) {
                    return "";
                }
            }
            return common == null ? "" : common;
        }
    }

    private static final class BuildingTagShortcutsPreferenceSetting extends DefaultTabPreferenceSetting {
        private DefaultListModel<String> suffixModel;
        private JList<String> suffixList;
        private JComboBox<String> defaultSuffixCombo;
        private JosmTextField suffixInput;

        BuildingTagShortcutsPreferenceSetting() {
            super("dialogs/propertiesdialog", "Building Tag Shortcuts", "Settings for building tag shortcuts");
        }

        @Override
        public void addGui(PreferenceTabbedPane gui) {
            JPanel panel = new JPanel(new GridBagLayout());

            panel.add(new JLabel("Building name suffixes used by Ctrl+number shortcuts:"),
                    GBC.eol().insets(0, 0, 0, 6));

            suffixModel = new DefaultListModel<>();
            for (String suffix : getNameSuffixes()) {
                suffixModel.addElement(suffix);
            }

            suffixList = new JList<>(suffixModel);
            suffixList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            panel.add(new JScrollPane(suffixList), GBC.eol().fill().weight(1.0, 1.0));

            JPanel editPanel = new JPanel(new GridBagLayout());
            suffixInput = new JosmTextField(12);
            editPanel.add(suffixInput, GBC.std().fill(GBC.HORIZONTAL).weight(1.0, 0.0).insets(0, 6, 6, 0));

            JButton addButton = new JButton("Add");
            addButton.addActionListener(event -> addSuffix());
            editPanel.add(addButton, GBC.std().insets(6, 6, 0, 0));

            JButton removeButton = new JButton("Remove");
            removeButton.addActionListener(event -> removeSelectedSuffix());
            editPanel.add(removeButton, GBC.eol().insets(6, 6, 0, 0));
            panel.add(editPanel, GBC.eol().fill(GBC.HORIZONTAL));

            panel.add(new JLabel("Default building name suffix:"), GBC.std().insets(0, 10, 6, 0));
            defaultSuffixCombo = new JComboBox<>();
            refreshDefaultSuffixCombo(getNameSuffix());
            panel.add(defaultSuffixCombo, GBC.eol().fill(GBC.HORIZONTAL).weight(1.0, 0.0).insets(6, 10, 0, 0));

            panel.add(GBC.glue(0, 0), GBC.eol().fill().weight(1.0, 1.0));
            createPreferenceTabWithScrollPane(gui, panel);
        }

        @Override
        public boolean ok() {
            List<String> suffixes = getSuffixesFromModel();
            if (suffixes.isEmpty()) {
                suffixes = new ArrayList<>(DEFAULT_NAME_SUFFIXES);
            }

            String selected = (String) defaultSuffixCombo.getSelectedItem();
            if (isEmpty(selected)) {
                selected = suffixes.get(0);
            }
            if (!suffixes.contains(selected)) {
                suffixes.add(selected);
            }

            Config.getPref().putList(PREF_NAME_SUFFIXES, suffixes);
            Config.getPref().put(PREF_NAME_SUFFIX, selected);
            return false;
        }

        @Override
        public boolean isExpert() {
            return false;
        }

        private void addSuffix() {
            String value = suffixInput.getText();
            if (value == null) {
                return;
            }

            String suffix = value.trim();
            if (suffix.isEmpty() || containsSuffix(suffix)) {
                return;
            }

            suffixModel.addElement(suffix);
            suffixInput.setText("");
            refreshDefaultSuffixCombo(suffix);
        }

        private void removeSelectedSuffix() {
            int index = suffixList.getSelectedIndex();
            if (index < 0) {
                return;
            }

            String selectedDefault = (String) defaultSuffixCombo.getSelectedItem();
            suffixModel.remove(index);
            if (suffixModel.isEmpty()) {
                for (String suffix : DEFAULT_NAME_SUFFIXES) {
                    suffixModel.addElement(suffix);
                }
            }
            refreshDefaultSuffixCombo(selectedDefault);
        }

        private boolean containsSuffix(String suffix) {
            for (int i = 0; i < suffixModel.size(); i++) {
                if (suffix.equals(suffixModel.get(i))) {
                    return true;
                }
            }
            return false;
        }

        private List<String> getSuffixesFromModel() {
            List<String> suffixes = new ArrayList<>();
            for (int i = 0; i < suffixModel.size(); i++) {
                suffixes.add(suffixModel.get(i));
            }
            return normalizeSuffixes(suffixes);
        }

        private void refreshDefaultSuffixCombo(String preferred) {
            List<String> suffixes = getSuffixesFromModel();
            defaultSuffixCombo.setModel(new DefaultComboBoxModel<>(suffixes.toArray(new String[0])));
            if (!isEmpty(preferred) && suffixes.contains(preferred)) {
                defaultSuffixCombo.setSelectedItem(preferred);
            } else if (!suffixes.isEmpty()) {
                defaultSuffixCombo.setSelectedIndex(0);
            }
        }
    }

    private static void showInfo(String message) {
        new Notification(message).setIcon(JOptionPane.INFORMATION_MESSAGE).show();
    }

    private static void showWarning(String message) {
        new Notification(message).setIcon(JOptionPane.WARNING_MESSAGE).show();
    }
}
