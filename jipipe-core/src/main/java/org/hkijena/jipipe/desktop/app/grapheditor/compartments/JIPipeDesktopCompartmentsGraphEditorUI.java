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

package org.hkijena.jipipe.desktop.app.grapheditor.compartments;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseRole;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorLogPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorMinimap;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.*;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.contextmenu.clipboard.GraphCompartmentCopyNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.contextmenu.clipboard.GraphCompartmentCutNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.contextmenu.clipboard.GraphCompartmentPasteNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.dragdrop.JIPipeCompartmentGraphDragAndDropBehavior;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.properties.JIPipeDesktopCompartmentsQuickGuidePanel;
import org.hkijena.jipipe.desktop.app.history.JIPipeDesktopHistoryJournalUI;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Graph editor UI for a project compartment graph
 */
public class JIPipeDesktopCompartmentsGraphEditorUI extends JIPipeDesktopGraphEditorUI {

    /**
     * @param workbenchUI The workbench UI
     */
    public JIPipeDesktopCompartmentsGraphEditorUI(JIPipeDesktopProjectWorkbench workbenchUI) {
        super(workbenchUI, workbenchUI.getProject().getCompartmentGraph(), null, workbenchUI.getProject().getHistoryJournal());
        initializeDefaultPanels();

        getCanvasUI().setDragAndDropBehavior(new JIPipeCompartmentGraphDragAndDropBehavior());
        List<NodeUIContextAction> actions = Arrays.asList(
                new AddNewCompartmentUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SelectAllNodeUIContextAction(),
                new InvertSelectionNodeUIContextAction(),
                new AddBookmarkNodeUIContextAction(),
                new RemoveBookmarkNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new GraphCompartmentCutNodeUIContextAction(),
                new GraphCompartmentCopyNodeUIContextAction(),
                new GraphCompartmentPasteNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new RunAndShowResultsNodeUIContextAction(),
                new UpdateCacheNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new RunAndShowIntermediateResultsNodeUIContextAction(),
                new UpdateCacheShowIntermediateNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new ClearCacheNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new ExportCompartmentAsJsonNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new DeleteCompartmentUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SendToForegroundUIContextAction(),
                new RaiseUIContextAction(),
                new LowerUIContextAction(),
                new SendToBackgroundUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SelectAndMoveNodeHereNodeUIContextAction(),
                new LockNodeLocationSizeUIContextAction(),
                new UnlockNodeLocationSizeUIContextAction()
        );
        // Custom entries (from registry)
        List<NodeUIContextAction> registeredEntries = JIPipe.getCustomMenus().getRegisteredContextMenuActions().stream()
                .filter(NodeUIContextAction::showInCompartmentGraph)
                .sorted(Comparator.comparing(NodeUIContextAction::getName))
                .collect(Collectors.toList());
        if (!registeredEntries.isEmpty()) {
            actions.add(NodeUIContextAction.SEPARATOR);
            actions.addAll(registeredEntries);
        }
        getCanvasUI().setContextActions(actions);
    }

    private void initializeDefaultPanels() {

        getDockPanel().addDockPanel(JIPipeDesktopGraphEditorUI.DOCK_MAP,
                "Overview map",
                UIUtils.getIcon32FromResources("actions/zoom.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopLeft,
                true,
                new JIPipeDesktopGraphEditorMinimap(this));
        getDockPanel().addDockPanel("QUICK_GUIDE",
                "Quick guide",
                UIUtils.getIcon32FromResources("actions/help-about.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                true,
                new JIPipeDesktopCompartmentsQuickGuidePanel(getDesktopWorkbench(), this));
        getDockPanel().addDockPanel(JIPipeDesktopGraphEditorUI.DOCK_BOOKMARKS,
                "Bookmarks",
                UIUtils.getIcon32FromResources("actions/bookmarks.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                new JIPipeDesktopBookmarkListPanel(getDesktopWorkbench(), getGraph(), this, null));
        getDockPanel().addDockPanel(JIPipeDesktopGraphEditorUI.DOCK_HISTORY,
                "History",
                UIUtils.getIcon32FromResources("actions/edit-undo-history.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                new JIPipeDesktopHistoryJournalUI(getHistoryJournal()));
        getDockPanel().addDockPanel(JIPipeDesktopGraphEditorUI.DOCK_LOG,
                "Log",
                UIUtils.getIcon32FromResources("actions/rabbitvcs-show_log.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomRight,
                false,
                new JIPipeDesktopGraphEditorLogPanel(getDesktopWorkbench()));
    }


//    @Override
//    public void installNodeUIFeatures(JIPipeAlgorithmUI ui) {
//        ui.installContextMenu(Arrays.asList(
//                new OpenSettingsAlgorithmContextMenuFeature(),
//                new AddToSelectionAlgorithmContextMenuFeature(),
//                new CutCopyAlgorithmContextMenuFeature(),
//                new DeleteCompartmentContextMenuFeature()
//        ));
//    }

    @Override
    protected void updateSelection() {
        super.updateSelection();

        getDockPanel().removeDockPanelsIf(panel -> panel.getId().startsWith("_"));

//        if (disableUpdateOnSelection)
//            return;
//        if (getSelection().isEmpty()) {
//            setPropertyPanel(defaultPanel, true);
//        } else if (getSelection().size() == 1) {
//            JIPipeGraphNode node = getSelection().iterator().next().getNode();
//            if (node instanceof JIPipeProjectCompartment) {
//                setPropertyPanel(new JIPipeDesktopSingleCompartmentSelectionPanelUI(this,
//                        (JIPipeProjectCompartment) node), true);
//            } else {
//                setPropertyPanel(new JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI(this, node), true);
//            }
//        } else {
//            if (getSelection().stream().allMatch(ui -> ui.getNode() instanceof JIPipeProjectCompartment)) {
//                setPropertyPanel(new JIPipeDesktopMultiCompartmentSelectionPanelUI((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(),
//                        getSelection().stream().map(ui -> (JIPipeProjectCompartment) ui.getNode()).collect(Collectors.toSet()), getCanvasUI()), true);
//            } else {
//                setPropertyPanel(new JIPipeDesktopPipelineMultiAlgorithmSelectionPanelUI((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(), getCanvasUI(),
//                        getSelection().stream().map(JIPipeDesktopGraphNodeUI::getNode).collect(Collectors.toSet())), true);
//            }
//        }
    }

    private JIPipeProject getProject() {
        return getDesktopWorkbench().getProject();
    }

    private JIPipeDesktopProjectWorkbench getProjectWorkbench() {
        return (JIPipeDesktopProjectWorkbench) getDesktopWorkbench();
    }

    private void addCompartment() {
        if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(getDesktopWorkbench()))
            return;
        String compartmentName = JOptionPane.showInputDialog(this, "Please enter the name of the compartment", "Compartment");
        if (compartmentName != null && !compartmentName.trim().isEmpty()) {
            if (getHistoryJournal() != null) {
                getHistoryJournal().snapshotBeforeAddCompartment(compartmentName);
            }
            getProject().addCompartment(compartmentName);
        }
    }

    /**
     * Should be triggered when a user double-clicks a graph node to open it in the graph editor
     *
     * @param event Generated event
     */
    @Override
    public void onDefaultNodeUIActionRequested(JIPipeDesktopGraphNodeUI.DefaultNodeUIActionRequestedEvent event) {
        if (event.getUi() != null && event.getUi().getNode() instanceof JIPipeProjectCompartment) {
            getProjectWorkbench().getOrOpenPipelineEditorTab((JIPipeProjectCompartment) event.getUi().getNode(), true);
        }
    }

    /**
     * Listens to events of algorithms requesting some action
     *
     * @param event the event
     */
    @Override
    public void onNodeUIActionRequested(JIPipeDesktopGraphNodeUI.NodeUIActionRequestedEvent event) {
//        if (event.getAction() instanceof JIPipeDesktopRunAndShowResultsAction) {
//            disableUpdateOnSelection = true;
//            selectOnly(event.getUi());
//            JIPipeDesktopSingleCompartmentSelectionPanelUI panel = new JIPipeDesktopSingleCompartmentSelectionPanelUI(this,
//                    (JIPipeProjectCompartment) event.getUi().getNode());
//            setPropertyPanel(panel, true);
//            panel.executeQuickRun(true,
//                    false,
//                    true,
//                    ((JIPipeDesktopRunAndShowResultsAction) event.getAction()).isStoreIntermediateResults(),
//                    false);
//            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
//        } else if (event.getAction() instanceof JIPipeDesktopUpdateCacheAction) {
//            disableUpdateOnSelection = true;
//            selectOnly(event.getUi());
//            JIPipeDesktopSingleCompartmentSelectionPanelUI panel = new JIPipeDesktopSingleCompartmentSelectionPanelUI(this,
//                    (JIPipeProjectCompartment) event.getUi().getNode());
//            setPropertyPanel(panel, true);
//            panel.executeQuickRun(false,
//                    true,
//                    false,
//                    ((JIPipeDesktopUpdateCacheAction) event.getAction()).isStoreIntermediateResults(),
//                    ((JIPipeDesktopUpdateCacheAction) event.getAction()).isOnlyPredecessors());
//            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
//        }
    }

    @Override
    public JIPipeNodeDatabaseRole getNodeDatabaseRole() {
        return JIPipeNodeDatabaseRole.CompartmentNode;
    }
}
