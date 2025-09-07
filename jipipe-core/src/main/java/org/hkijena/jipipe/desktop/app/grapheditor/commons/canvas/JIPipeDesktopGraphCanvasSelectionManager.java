/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas;

import com.google.common.collect.ImmutableSet;
import org.hkijena.jipipe.api.grapheditortool.JIPipeToggleableGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.JIPipeToggleableGraphEditorToolNodeLayerMask;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectedEventEmitter;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectionChangedEventEmitter;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopAnnotationGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class JIPipeDesktopGraphCanvasSelectionManager {
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final Set<JIPipeDesktopGraphInteractiveObjectUI> selection = new LinkedHashSet<>();
    private final JIPipeDesktopGraphCanvasUINodeSelectionChangedEventEmitter nodeSelectionChangedEventEmitter = new JIPipeDesktopGraphCanvasUINodeSelectionChangedEventEmitter();
    private final JIPipeDesktopGraphCanvasUINodeSelectedEventEmitter nodeUISelectedEventEmitter = new JIPipeDesktopGraphCanvasUINodeSelectedEventEmitter();
    private Set<JIPipeGraphNode> scheduledSelection = new HashSet<>();

    public JIPipeDesktopGraphCanvasSelectionManager(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    public void selectAll() {
        selection.addAll(canvasUI.getNodeUIs().values());
        updateSelection();
    }

    public void invertSelection() {
        ImmutableSet<JIPipeDesktopGraphInteractiveObjectUI> originalSelection = ImmutableSet.copyOf(selection);
        selection.clear();
        for (JIPipeDesktopGraphNodeUI ui : canvasUI.getNodeUIs().values()) {
            if (!originalSelection.contains(ui))
                selection.add(ui);
        }
        updateSelection();
    }

    /**
     * @return the set of selected nodes
     */
    public Set<JIPipeGraphNode> getSelectedNodes() {
        Set<JIPipeGraphNode> selected = new HashSet<>();
        for (JIPipeDesktopGraphInteractiveObjectUI interactiveObjectUI : selection) {
            if (interactiveObjectUI instanceof JIPipeDesktopGraphNodeUI) {
                selected.add(((JIPipeDesktopGraphNodeUI) interactiveObjectUI).getNode());
            }
        }
        return selected;
    }

    public <T extends JIPipeDesktopGraphInteractiveObjectUI> Set<T> getSelectionByType(Class<T> klass) {
        Set<T> result = new HashSet<>();
        for (JIPipeDesktopGraphInteractiveObjectUI interactiveObjectUI : selection) {
            if (klass.isInstance(interactiveObjectUI)) {
                result.add((T) interactiveObjectUI);
            }
        }
        return result;
    }

    public JIPipeDesktopGraphCanvasUINodeSelectionChangedEventEmitter getNodeSelectionChangedEventEmitter() {
        return nodeSelectionChangedEventEmitter;
    }

    public JIPipeDesktopGraphCanvasUINodeSelectedEventEmitter getNodeUISelectedEventEmitter() {
        return nodeUISelectedEventEmitter;
    }

    public Set<JIPipeDesktopGraphInteractiveObjectUI> getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    public void clearSelection() {
        clearSelection(true);
    }

    /**
     * Clears the list of selected algorithms
     */
    public void clearSelection(boolean update) {
        selection.clear();
        if (update) {
            updateSelection();
        }
    }

    public void addAllToSelection(Set<JIPipeDesktopGraphInteractiveObjectUI> newSelection, boolean update) {
        selection.addAll(newSelection);
        if (update) {
            updateSelection();
        }
    }

    public void setSelection(Set<? extends JIPipeDesktopGraphInteractiveObjectUI> nodeUIs) {
        clearSelection();
        if (nodeUIs != null) {
            for (JIPipeDesktopGraphInteractiveObjectUI ui : nodeUIs) {
                selection.add(ui);
                if (!(ui instanceof JIPipeDesktopAnnotationGraphNodeUI)) {
                    canvasUI.moveToFrontLayer(ui);
                }
            }
            updateSelection();
        }
    }

    public Set<JIPipeGraphNode> getScheduledSelection() {
        return scheduledSelection;
    }

    public void setScheduledSelection(Set<JIPipeGraphNode> scheduledSelection) {
        this.scheduledSelection = scheduledSelection;
    }


    /**
     * Selects only the specified algorithm
     *
     * @param ui The algorithm UI
     */
    public void selectOnly(JIPipeDesktopGraphInteractiveObjectUI ui) {
        if (ui == null) {
            clearSelection();
            return;
        }
        if (selection.isEmpty()) {
            addToSelection(ui);
        } else if (selection.size() == 1) {
            if (selection.iterator().next() != ui) {
                clearSelection();
                addToSelection(ui);
            }
        } else {
            clearSelection();
            addToSelection(ui);
        }
    }

    public void removeFromSelection(JIPipeDesktopGraphInteractiveObjectUI ui) {
        removeFromSelection(ui, true);
    }

    /**
     * Removes an algorithm from the selection
     *
     * @param ui The algorithm UI
     */
    public void removeFromSelection(JIPipeDesktopGraphInteractiveObjectUI ui, boolean update) {
        if (selection.contains(ui)) {
            selection.remove(ui);
            if (update) {
                updateSelection();
            }
        }
    }

    /**
     * Adds an algorithm to the selection
     *
     * @param ui The algorithm UI
     */
    public void addToSelection(JIPipeDesktopGraphInteractiveObjectUI ui) {
        selection.add(ui);
        if (!(ui instanceof JIPipeDesktopAnnotationGraphNodeUI)) {
            canvasUI.moveToFrontLayer(ui);
        }
        updateSelection();
    }

    public void updateSelection() {
        canvasUI.updateAnnotationNodeLayers();
        canvasUI.repaint(50);
        canvasUI.requestFocusInWindow();
        nodeSelectionChangedEventEmitter.emit(new JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent(canvasUI));

        // Update resize handles
        if (selection.size() == 1) {
            var ui = selection.iterator().next();
            if (ui instanceof JIPipeDesktopAnnotationGraphNodeUI) {
                canvasUI.getResizeManager().setCurrentResizeTarget((JIPipeDesktopAnnotationGraphNodeUI) ui);
            } else {
                canvasUI.getResizeManager().setCurrentResizeTarget(null);
            }
        } else {
            canvasUI.getResizeManager().setCurrentResizeTarget(null);
        }
    }

    public void applyScheduledSelection() {
        if (scheduledSelection != null && !scheduledSelection.isEmpty()) {
            if (scheduledSelection.equals(getSelectedNodes()))
                return;
            clearSelection();
            for (JIPipeGraphNode node : scheduledSelection) {
                JIPipeDesktopGraphNodeUI selected = canvasUI.getNodeUIs().getOrDefault(node, null);
                if (selected != null) {
                    addToSelection(selected);
                }
            }
            scheduledSelection.clear();
        }
    }

    public void enforceToolMasking(JIPipeToggleableGraphEditorTool currentTool) {
        JIPipeToggleableGraphEditorToolNodeLayerMask mask;
        if (currentTool != null) {
            mask = currentTool.getNodeLayerMask();
        } else {
            mask = JIPipeToggleableGraphEditorToolNodeLayerMask.WorkflowOnly;
        }
        if(mask != JIPipeToggleableGraphEditorToolNodeLayerMask.None) {
            if (selection.removeIf(ui -> !mask.test(ui))) {
                updateSelection();
            }
        }
    }
}
