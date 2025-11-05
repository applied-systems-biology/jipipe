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

package org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel.JIPipeDesktopAddNodesPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorLogPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorMinimap;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.GraphInteractiveObjectUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.actions.*;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.add.AddNewCompartmentUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.bookmarks.AddBookmarkNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.bookmarks.RemoveBookmarkNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.cache.ClearCacheNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.layers.LowerUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.layers.RaiseUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.layers.SendToBackgroundUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.layers.SendToForegroundUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.locking.LockNodeLocationSizeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.locking.UnlockNodeLocationSizeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.misc.ExportCompartmentAsJsonNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.running.RunAndShowIntermediateResultsNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.running.RunAndShowResultsNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.running.UpdateCacheNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.running.UpdateCacheShowIntermediateNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.select.InvertSelectionNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.select.SelectAllNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.select.SelectAndMoveNodeHereNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events.DefaultNodeUIActionRequestedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events.NodeUIActionRequestedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphEditorErrorPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphNodeSlotEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.actions.JIPipeDesktopCompartmentsGraphEditorRunManager;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.contextmenu.JIPipeDesktopCompartmentsCopyNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.contextmenu.JIPipeDesktopCompartmentsCutNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.contextmenu.JIPipeDesktopCompartmentsPasteNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.dragdrop.JIPipeCreateCompartmentNodesFromDraggedDataDragAndDropBehavior;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.properties.JIPipeDesktopCompartmentGraphEditorResultsPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.properties.JIPipeDesktopCompartmentsParametersPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.properties.JIPipeDesktopCompartmentsQuickGuidePanel;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.actions.JIPipeDesktopRunAndShowResultsAction;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.actions.JIPipeDesktopUpdateCacheAction;
import org.hkijena.jipipe.desktop.app.history.JIPipeDesktopHistoryJournalUI;
import org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopRunSetsListEditor;
import org.hkijena.jipipe.desktop.commons.components.tools.JIPipeDesktopExpressionCalculatorUI;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Graph editor UI for a project compartment graph
 */
public class JIPipeDesktopCompartmentsGraphEditorUI extends JIPipeDesktopGraphEditorUI {

    public static final String DOCK_ADD_NODES = "ADD_NODES";
    public static final String DOCK_NODE_CONTEXT_HELP = "_HELP";
    public static final String DOCK_NODE_CONTEXT_PARAMETERS = "_PARAMETERS";
    public static final String DOCK_NODE_CONTEXT_SLOT_MANAGER = "_SLOT_MANAGER";
    public static final String DOCK_NODE_CONTEXT_RESULTS = "_RESULTS";
    public static final String DOCK_QUICK_GUIDE = "QUICK_GUIDE";

    /**
     * @param workbenchUI The workbench UI
     */
    public JIPipeDesktopCompartmentsGraphEditorUI(JIPipeDesktopProjectWorkbench workbenchUI) {
        super(workbenchUI, workbenchUI.getProject().getCompartmentGraph(), null, workbenchUI.getProject().getHistoryJournal());
        initializeDefaultPanels();

        getCanvasUI().setDragAndDropBehavior(new JIPipeCreateCompartmentNodesFromDraggedDataDragAndDropBehavior());
        List<GraphInteractiveObjectUIContextAction> actions = Arrays.asList(
                new AddNewCompartmentUIContextAction(),
                GraphInteractiveObjectUIContextAction.SEPARATOR,
                new AddEdgeControlPointUIContextAction(),
                new RemoveEdgeControlPointUIContextAction(),
                new ClearEdgeControlsPointUIContextAction(),
                GraphInteractiveObjectUIContextAction.SEPARATOR,
                new SelectAllNodeUIContextAction(),
                new InvertSelectionNodeUIContextAction(),
                new AddBookmarkNodeUIContextAction(),
                new RemoveBookmarkNodeUIContextAction(),
                GraphInteractiveObjectUIContextAction.SEPARATOR,
                new JIPipeDesktopCompartmentsCutNodeUIContextAction(),
                new JIPipeDesktopCompartmentsCopyNodeUIContextAction(),
                new CopyEdgeUIContextAction(),
                new JIPipeDesktopCompartmentsPasteNodeUIContextAction(),
                GraphInteractiveObjectUIContextAction.SEPARATOR,
                new RunAndShowResultsNodeUIContextAction(),
                new UpdateCacheNodeUIContextAction(),
                GraphInteractiveObjectUIContextAction.SEPARATOR,
                new RunAndShowIntermediateResultsNodeUIContextAction(),
                new UpdateCacheShowIntermediateNodeUIContextAction(),
                GraphInteractiveObjectUIContextAction.SEPARATOR,
                new ClearCacheNodeUIContextAction(),
                GraphInteractiveObjectUIContextAction.SEPARATOR,
                new ExportCompartmentAsJsonNodeUIContextAction(),
                GraphInteractiveObjectUIContextAction.SEPARATOR,
                new DeleteCompartmentNodesAndEdgesContextAction(),
                GraphInteractiveObjectUIContextAction.SEPARATOR,
                new SendToForegroundUIContextAction(),
                new RaiseUIContextAction(),
                new LowerUIContextAction(),
                new SendToBackgroundUIContextAction(),
                GraphInteractiveObjectUIContextAction.SEPARATOR,
                new SelectAndMoveNodeHereNodeUIContextAction(),
                new LockNodeLocationSizeUIContextAction(),
                new UnlockNodeLocationSizeUIContextAction()
        );
        // Custom entries (from registry)
        List<GraphInteractiveObjectUIContextAction> registeredEntries = JIPipe.getCustomMenus().getRegisteredContextMenuActions().stream()
                .filter(GraphInteractiveObjectUIContextAction::showInCompartmentGraph)
                .sorted(Comparator.comparing(GraphInteractiveObjectUIContextAction::getName))
                .collect(Collectors.toList());
        if (!registeredEntries.isEmpty()) {
            actions.add(GraphInteractiveObjectUIContextAction.SEPARATOR);
            actions.addAll(registeredEntries);
        }
        getCanvasUI().setContextActions(actions);
    }

    private void initializeDefaultPanels() {

        getDockPanel().addDockPanel(JIPipeDesktopGraphEditorUI.DOCK_CALCULATOR,
                "Calculator",
                JIPipe.RESOURCES.getIcon24("actions/insert-math-expression.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopLeft,
                true,
                0, new JIPipeDesktopExpressionCalculatorUI(getDesktopWorkbench()));
        getDockPanel().addDockPanel(JIPipeDesktopGraphEditorUI.DOCK_MAP,
                "Navigator",
                JIPipe.RESOURCES.getIcon24("actions/compass.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopLeft,
                true,
                0, new JIPipeDesktopGraphEditorMinimap(this));
        getDockPanel().addDockPanel(DOCK_QUICK_GUIDE,
                "Quick guide",
                JIPipe.RESOURCES.getIcon24("actions/help-about.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                true,
                0, new JIPipeDesktopCompartmentsQuickGuidePanel(getDesktopWorkbench(), this));
        getDockPanel().addDockPanel(JIPipeDesktopGraphEditorUI.DOCK_BOOKMARKS,
                "Bookmarks",
                JIPipe.RESOURCES.getIcon24("actions/bookmarks.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                0, new JIPipeDesktopBookmarkListPanel(getDesktopWorkbench(), getGraph(), this, null));
        getDockPanel().addDockPanel(JIPipeDesktopGraphEditorUI.DOCK_HISTORY,
                "History",
                JIPipe.RESOURCES.getIcon24("actions/edit-undo-history.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                0, new JIPipeDesktopHistoryJournalUI(getHistoryJournal()));
        getDockPanel().addDockPanel(JIPipeDesktopGraphEditorUI.DOCK_LOG,
                "Log",
                JIPipe.RESOURCES.getIcon24("actions/rabbitvcs-show_log.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomBottom,
                false,
                0, new JIPipeDesktopGraphEditorLogPanel(getDesktopWorkbench()));
        getDockPanel().addDockPanel(DOCK_ADD_NODES,
                "Add nodes",
                JIPipe.RESOURCES.getIcon24("actions/node-add.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                JIPipeDesktopDockPanel.UI_ORDER_PINNED,
                new JIPipeDesktopAddNodesPanel(getDesktopWorkbench(), this));
        getDockPanel().addDockPanel(DOCK_ERRORS,
                "Errors",
                JIPipe.RESOURCES.getIcon24("actions/dialog-warning-2.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomRight,
                false,
                0, new JIPipeDesktopGraphEditorErrorPanel(getDesktopWorkbench(), this));
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
        if (getSelectionManager().getSelection().size() == 1) {
            JIPipeDesktopGraphInteractiveObjectUI interactiveObjectUI = getSelectionManager().getSelection().iterator().next();
            if (interactiveObjectUI instanceof JIPipeDesktopGraphNodeUI) {
                showSelectedNodeDocks((JIPipeDesktopGraphNodeUI) interactiveObjectUI);
            }
        }
    }

    private void showSelectedNodeDocks(JIPipeDesktopGraphNodeUI nodeUI) {
        JIPipeGraphNode node = nodeUI.getNode();

        JIPipeDesktopCompartmentsParametersPanel parametersPanel = new JIPipeDesktopCompartmentsParametersPanel(getProjectWorkbench(),
                getCanvasUI(),
                node);
        parametersPanel.getParametersUI().getContextHelpEventEmitter().subscribeLambda((source, event) -> {
            getDockPanel().activatePanel(DOCK_NODE_CONTEXT_HELP, true);
        });
        getDockPanel().addDockPanel(DOCK_NODE_CONTEXT_PARAMETERS,
                "Parameters",
                JIPipe.RESOURCES.getIcon24("actions/configure3.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopRight,
                true,
                0, parametersPanel);
        getDockPanel().addDockPanel(DOCK_NODE_CONTEXT_HELP,
                "Documentation",
                JIPipe.RESOURCES.getIcon24("actions/help-question.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomRight,
                true,
                0, parametersPanel.getParametersUI().getHelpPanel());
        getDockPanel().addDockPanel(DOCK_NODE_CONTEXT_SLOT_MANAGER,
                "Slots",
                JIPipe.RESOURCES.getIcon24("actions/labplot-editbreaklayout.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopRight,
                false,
                0, () -> new JIPipeDesktopGraphNodeSlotEditorUI(this, node));
        if (node instanceof JIPipeProjectCompartment && !((JIPipeProjectCompartment) node).getOutputNodes().isEmpty()) {
            getDockPanel().addDockPanel(DOCK_NODE_CONTEXT_RESULTS,
                    "Results",
                    JIPipe.RESOURCES.getIcon24("actions/network-server-database.png"),
                    JIPipeDesktopDockPanel.PanelLocation.TopRight,
                    false,
                    0, () -> new JIPipeDesktopCompartmentGraphEditorResultsPanel(getProjectWorkbench(), (JIPipeProjectCompartment) node, this));
        }
    }

    private JIPipeProject getProject() {
        return getDesktopWorkbench().getProject();
    }

    private JIPipeDesktopProjectWorkbench getProjectWorkbench() {
        return (JIPipeDesktopProjectWorkbench) getDesktopWorkbench();
    }

    public void addCompartment() {
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
    public void onDefaultNodeUIActionRequested(DefaultNodeUIActionRequestedEvent event) {
        if (event.getUi() != null && event.getUi().getNode() instanceof JIPipeProjectCompartment) {
            handleOpenCompartmentPipelineEditorAction(event);
        } else {
            super.onDefaultNodeUIActionRequested(event);
        }
    }

    private void handleOpenCompartmentPipelineEditorAction(DefaultNodeUIActionRequestedEvent event) {
        getProjectWorkbench().getOrOpenPipelineEditorTab((JIPipeProjectCompartment) event.getUi().getNode(), true);
    }

    /**
     * Listens to events of algorithms requesting some action
     *
     * @param event the event
     */
    @Override
    public void onNodeUIActionRequested(NodeUIActionRequestedEvent event) {
        if (event.getAction() instanceof JIPipeDesktopRunAndShowResultsAction) {
            handleRunAndShowResultsAction(event);
        } else if (event.getAction() instanceof JIPipeDesktopUpdateCacheAction) {
            handleUpdateCacheAction(event);
        } else {
            super.onNodeUIActionRequested(event);
        }
    }

    private void handleUpdateCacheAction(NodeUIActionRequestedEvent event) {
        getSelectionManager().selectOnly(event.getUi());
        JIPipeDesktopCompartmentsGraphEditorRunManager runManager = new JIPipeDesktopCompartmentsGraphEditorRunManager(getWorkbench().getProject(),
                getCanvasUI(),
                event.getUi(),
                getDockPanel(),
                ((JIPipeDesktopUpdateCacheAction) event.getAction()).isAllowChangePanels());
        runManager.run(false,
                ((JIPipeDesktopUpdateCacheAction) event.getAction()).isStoreIntermediateResults(),
                ((JIPipeDesktopUpdateCacheAction) event.getAction()).isOnlyPredecessors());
    }

    private void handleRunAndShowResultsAction(NodeUIActionRequestedEvent event) {
        getSelectionManager().selectOnly(event.getUi());
        JIPipeDesktopCompartmentsGraphEditorRunManager runManager = new JIPipeDesktopCompartmentsGraphEditorRunManager(getWorkbench().getProject(), getCanvasUI(), event.getUi(), getDockPanel(), true);
        runManager.run(true,
                ((JIPipeDesktopRunAndShowResultsAction) event.getAction()).isStoreIntermediateResults(),
                false);
    }

    @Override
    public void beforeOpenContextMenu(JPopupMenu menu) {
        Set<JIPipeDesktopGraphNodeUI> selectedNodes = getSelectionManager().getSelectionByType(JIPipeDesktopGraphNodeUI.class);
        if (getGraph().isProjectCompartmentGraph() && selectedNodes.stream().anyMatch(ui -> ui.getNode() instanceof JIPipeProjectCompartment)) {
            menu.addSeparator();
            JMenu runSetsMenu = new JMenu("Run sets ...");
            menu.add(runSetsMenu);

            Set<JIPipeGraphNode> selectedOutputs = new HashSet<>();
            for (JIPipeDesktopGraphNodeUI ui : selectedNodes) {
                if (ui.getNode() instanceof JIPipeProjectCompartment) {
                    selectedOutputs.add(ui.getNode());
                }
            }

            JIPipeDesktopRunSetsListEditor.createRunSetsManagementContextMenu(runSetsMenu,
                    selectedOutputs,
                    getProjectWorkbench());
        }
    }

    @Override
    public void onCanvasEmptyDoubleClick(MouseEvent mouseEvent) {
        addCompartment();
    }

    @Override
    protected void restoreDockStateFromSettings() {
        try {
            JIPipeDesktopDockPanel.State defaultState = new JIPipeDesktopDockPanel.State();
            defaultState.setAlwaysShowRightPanel(true);
            JIPipeDesktopDockPanel.State state = JIPipe.getSettings().getFromRegistry("graph-editor",
                    Path.of("compartments", "dock-state"),
                    JIPipeDesktopDockPanel.State.class,
                    defaultState,
                    true);
            getDockPanel().restoreState(state);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void saveDockStateToSettings() {
        if (JIPipe.isInstantiated()) {
            JIPipe.getSettings().putIntoRegistry("ui-graph-editor",
                    Path.of("compartments", "dock-state"),
                    getDockPanel().getCurrentState());
        }
    }

    @Override
    protected StringAndStringPairParameterList getDockStateTemplates() {
        if (JIPipe.isInstantiated()) {
            return JIPipe.getSettings().getFromRegistry("ui-dock", Path.of("layouts"), StringAndStringPairParameterList.class, new StringAndStringPairParameterList(), true);
        }
        return null;
    }

    @Override
    protected void restoreDefaultDockState() {
        JIPipeDesktopDockPanel.State state = new JIPipeDesktopDockPanel.State();
        state.setLeftSplitPaneRatio(0.33);
        state.setRightSplitPaneRatio(0.66);
        state.setLeftPanelWidth(350);
        state.setRightPanelWidth(500);

        state.put(DOCK_MAP, true, JIPipeDesktopDockPanel.PanelLocation.TopLeft);
        state.put(DOCK_QUICK_GUIDE, true, JIPipeDesktopDockPanel.PanelLocation.BottomLeft);
        state.put(DOCK_ADD_NODES, false, JIPipeDesktopDockPanel.PanelLocation.BottomLeft);
        state.put(DOCK_BOOKMARKS, false, JIPipeDesktopDockPanel.PanelLocation.BottomLeft);
        state.put(DOCK_HISTORY, false, JIPipeDesktopDockPanel.PanelLocation.BottomLeft);

        state.put(DOCK_NODE_CONTEXT_HELP, true, JIPipeDesktopDockPanel.PanelLocation.BottomRight);
        state.put(DOCK_LOG, false, JIPipeDesktopDockPanel.PanelLocation.BottomBottom);
        state.put(DOCK_ERRORS, false, JIPipeDesktopDockPanel.PanelLocation.BottomRight);
        state.put(DOCK_NODE_CONTEXT_PARAMETERS, true, JIPipeDesktopDockPanel.PanelLocation.TopRight);
        state.put(DOCK_NODE_CONTEXT_SLOT_MANAGER, false, JIPipeDesktopDockPanel.PanelLocation.TopRight);
        state.put(DOCK_NODE_CONTEXT_RESULTS, false, JIPipeDesktopDockPanel.PanelLocation.TopRight);

        getDockPanel().restoreState(state);
    }
}
