package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.managers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopRewireConnectionsToolUI;
import org.hkijena.jipipe.desktop.app.grapheditor.nodefinder.JIPipeDesktopNodeFinderDialogUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopAddAlgorithmSlotPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopEditAlgorithmSlotPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterKeyPickerUI;
import org.hkijena.jipipe.plugins.multiparameters.nodes.DefineParametersTableAlgorithm;
import org.hkijena.jipipe.plugins.parameters.library.table.ParameterTable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class JIPipeDesktopGraphNodeUISlotManager {
    private final JIPipeDesktopGraphNodeUI nodeUI;

    public JIPipeDesktopGraphNodeUISlotManager(JIPipeDesktopGraphNodeUI nodeUI) {
        this.nodeUI = nodeUI;
    }


    public void openAddSlotDialog(JIPipeSlotType slotType) {
        if (!JIPipeDesktopProjectWorkbench.canModifySlots(nodeUI.getDesktopWorkbench())) {
            JOptionPane.showMessageDialog(nodeUI.getGraphCanvasUI().getDesktopWorkbench().getWindow(), "Slots cannot be modified!", "Add slot", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JIPipeDesktopAddAlgorithmSlotPanel.showDialog(nodeUI, nodeUI.getGraphCanvasUI().getHistoryJournal(), nodeUI.getNode(), slotType);
    }


    public void createParameterSetsNode(JIPipeDataSlot slot) {

        placeCursorAboveInput(slot);

        List<JIPipeDesktopParameterKeyPickerUI.ParameterEntry> result = JIPipeDesktopParameterKeyPickerUI.showPickerDialog(nodeUI.getGraphCanvasUI().getGraphEditorUI(),
                "Select parameters",
                nodeUI.getGraphCanvasUI().getVisibleNodes(),
                nodeUI.getNode());

        if (!result.isEmpty()) {
            ParameterTable table = new ParameterTable();
            for (JIPipeDesktopParameterKeyPickerUI.ParameterEntry entry : result) {
                ParameterTable.ParameterColumn column = new ParameterTable.ParameterColumn(entry.getName(), entry.getKey(), entry.getFieldClass());
                table.addColumn(column, entry.getInitialValue());
            }
            table.addRow();

            DefineParametersTableAlgorithm node = JIPipe.createNode(DefineParametersTableAlgorithm.class);
            node.setParameterTable(table);
            nodeUI.getGraphCanvasUI().getGraph().insertNode(node, nodeUI.getGraphCanvasUI().getCompartmentUUID());
        }

    }

    List<JIPipeDataSlot> sortSlotsByDistance(JIPipeDataSlot slot, Set<JIPipeDataSlot> unsorted) {
        Point thisLocation = nodeUI.getGraphCanvasUI().getSlotLocation(slot);
        if (thisLocation == null)
            return new ArrayList<>(unsorted);
        Map<JIPipeDataSlot, Double> distances = new HashMap<>();
        for (JIPipeDataSlot dataSlot : unsorted) {
            Point location = nodeUI.getGraphCanvasUI().getSlotLocation(dataSlot);
            if (location != null) {
                distances.put(dataSlot, Math.pow(location.x - thisLocation.x, 2) + Math.pow(location.y - thisLocation.y, 2));
            } else {
                distances.put(dataSlot, Double.POSITIVE_INFINITY);
            }
        }
        return unsorted.stream().sorted(Comparator.comparing(distances::get)).collect(Collectors.toList());
    }

    private void placeCursorBelowOutput(JIPipeDataSlot slot) {
        Point cursorLocation = new Point();
        Point slotLocation = nodeUI.getSlotLocation(slot).min;
        cursorLocation.x = nodeUI.getX() + slotLocation.x;
        cursorLocation.y = nodeUI.getBottomY() + JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT;
        nodeUI.getGraphCanvasUI().setGraphEditCursor(cursorLocation);
        nodeUI.invalidateAndRepaint(false, true);
    }


    private void placeCursorAboveInput(JIPipeDataSlot slot) {
        Point cursorLocation = new Point();
        Point slotLocation = nodeUI.getSlotLocation(slot).min;
        cursorLocation.x = nodeUI.getX() + slotLocation.x;
        cursorLocation.y = nodeUI.getY() - JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT * 4;
        nodeUI.getGraphCanvasUI().setGraphEditCursor(cursorLocation);
        nodeUI.invalidateAndRepaint(false, true);
    }

    public void openRewireInputTool(JIPipeDataSlot slot, Set<JIPipeDataSlot> sourceSlots) {
        JIPipeDesktopRewireConnectionsToolUI ui = new JIPipeDesktopRewireConnectionsToolUI(nodeUI.getGraphCanvasUI(), slot, sourceSlots);
        ui.setTitle("Rewire input");
        ui.setLocationRelativeTo(nodeUI.getGraphCanvasUI().getGraphEditorUI());
        ui.setVisible(true);
        ui.revalidate();
        ui.repaint();
    }

    public void openRewireOutputTool(JIPipeDataSlot slot, Set<JIPipeDataSlot> targetSlots) {
        JIPipeDesktopRewireConnectionsToolUI ui = new JIPipeDesktopRewireConnectionsToolUI(nodeUI.getGraphCanvasUI(), slot, targetSlots);
        ui.setTitle("Rewire output");
        ui.setLocationRelativeTo(nodeUI.getGraphCanvasUI().getGraphEditorUI());
        ui.setVisible(true);
        ui.revalidate();
        ui.repaint();
    }

    public void openOutputAlgorithmFinder(JIPipeDataSlot slot) {
//        JIPipeAlgorithmTargetFinderUI algorithmFinderUI = new JIPipeAlgorithmTargetFinderUI(getGraphCanvasUI(), slot);
//        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Find matching algorithm");
//        UIUtils.addEscapeListener(dialog);
//        dialog.setModal(true);
//        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
//        dialog.setContentPane(algorithmFinderUI);
//        dialog.pack();
//        dialog.setSize(800, 600);
//        dialog.setLocationRelativeTo(this);
//
//        algorithmFinderUI.getAlgorithmFinderSuccessEventEmitter().subscribeLambda((emitter, event) -> dialog.dispose());
//
        boolean layoutHelperEnabled = nodeUI.getGraphCanvasUI().getSettings() != null && nodeUI.getGraphCanvasUI().getSettings().isLayoutAfterAlgorithmFinder();
        if (layoutHelperEnabled) {
            placeCursorBelowOutput(slot);
        }
//
//        dialog.setVisible(true);
        JIPipeDesktopNodeFinderDialogUI dialogUI = new JIPipeDesktopNodeFinderDialogUI(nodeUI.getGraphCanvasUI(), slot);
        dialogUI.setVisible(true);
    }

    public void openInputAlgorithmFinder(JIPipeDataSlot slot) {
//        JIPipeAlgorithmSourceFinderUI algorithmFinderUI = new JIPipeAlgorithmSourceFinderUI(getGraphCanvasUI(), slot);
//        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Find matching algorithm");
//        UIUtils.addEscapeListener(dialog);
//        dialog.setModal(true);
//        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
//        dialog.setContentPane(algorithmFinderUI);
//        dialog.pack();
//        dialog.setSize(800, 600);
//        dialog.setLocationRelativeTo(this);
//
//        algorithmFinderUI.getAlgorithmFinderSuccessEventEmitter().subscribeLambda((emitter, event) -> dialog.dispose());
//
        boolean layoutHelperEnabled = nodeUI.getGraphCanvasUI().getSettings() != null &&
                nodeUI.getGraphCanvasUI().getSettings().isLayoutAfterAlgorithmFinder();
        if (layoutHelperEnabled) {
            placeCursorAboveInput(slot);
        }
//
//        dialog.setVisible(true);
        JIPipeDesktopNodeFinderDialogUI dialogUI = new JIPipeDesktopNodeFinderDialogUI(nodeUI.getGraphCanvasUI(), slot);
        dialogUI.setVisible(true);
    }


    public void editSlot(JIPipeDataSlot slot) {
        if (!JIPipeDesktopProjectWorkbench.canModifySlots(nodeUI.getDesktopWorkbench()))
            return;
        JIPipeDesktopEditAlgorithmSlotPanel.showDialog(nodeUI, nodeUI.getGraphCanvasUI().getHistoryJournal(), slot);
    }

    public void relabelSlot(JIPipeDataSlot slot) {
        String newLabel = JOptionPane.showInputDialog(nodeUI,
                "Please enter a new label for the slot.\nLeave the text empty to remove an existing label.",
                slot.getInfo().getCustomName());
        if (newLabel == null)
            return;
        if (nodeUI.getGraphCanvasUI().getHistoryJournal() != null) {
            nodeUI.getGraphCanvasUI().getHistoryJournal().snapshotBeforeLabelSlot(slot, slot.getNode().getCompartmentUUIDInParentGraph());
        }
        slot.getInfo().setCustomName(newLabel);
        nodeUI.updateView(false, true, true);
        nodeUI.getGraphCanvasUI().getDesktopWorkbench().setProjectModified(true);
    }

    public void deleteSlot(JIPipeDataSlot slot) {
        if (!JIPipeDesktopProjectWorkbench.canModifySlots(nodeUI.getDesktopWorkbench()))
            return;
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) slot.getNode().getSlotConfiguration();
        if (nodeUI.getGraphCanvasUI().getHistoryJournal() != null) {
            nodeUI.getGraphCanvasUI().getHistoryJournal().snapshotBeforeRemoveSlot(slot.getNode(), slot.getInfo(), slot.getNode().getCompartmentUUIDInParentGraph());
        }
        if (slot.isInput())
            slotConfiguration.removeInputSlot(slot.getName(), true);
        else if (slot.isOutput())
            slotConfiguration.removeOutputSlot(slot.getName(), true);
    }


    public void setSaveOutputs(JIPipeDataSlot slot, boolean saveOutputs) {
        slot.getInfo().setStoreToDisk(saveOutputs);
    }


    public void moveSlotRight(JIPipeDataSlot slot) {
        if (slot != null) {
            if (nodeUI.getGraphCanvasUI().getHistoryJournal() != null) {
                nodeUI.getGraphCanvasUI().getHistoryJournal().snapshotBeforeMoveSlot(slot, slot.getNode().getCompartmentUUIDInParentGraph());
            }
            ((JIPipeMutableSlotConfiguration) nodeUI.getNode().getSlotConfiguration()).moveDown(slot.getName(), slot.getSlotType());
            nodeUI.invalidateAndRepaint(true, true);
        }
    }

    public void moveSlotLeft(JIPipeDataSlot slot) {
        if (slot != null) {
            if (nodeUI.getGraphCanvasUI().getHistoryJournal() != null) {
                nodeUI.getGraphCanvasUI().getHistoryJournal().snapshotBeforeMoveSlot(slot, slot.getNode().getCompartmentUUIDInParentGraph());
            }
            ((JIPipeMutableSlotConfiguration) nodeUI.getNode().getSlotConfiguration()).moveUp(slot.getName(), slot.getSlotType());
            nodeUI.invalidateAndRepaint(true, true);
        }
    }
}
