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
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseRole;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorLogPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorMinimap;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.*;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphEditorErrorPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphNodeSlotEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.actions.JIPipeDesktopCompartmentsGraphEditorRunManager;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.contextmenu.JIPipeDesktopCompartmentsCopyNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.contextmenu.JIPipeDesktopCompartmentsCutNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.contextmenu.JIPipeDesktopCompartmentsPasteNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.dragdrop.JIPipeDesktopCompartmentsGraphDragAndDropBehavior;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.properties.JIPipeDesktopCompartmentsAddCompartmentsPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.properties.JIPipeDesktopCompartmentsParametersPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.properties.JIPipeDesktopCompartmentsQuickGuidePanel;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.actions.JIPipeDesktopPipelineGraphEditorRunManager;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.actions.JIPipeDesktopRunAndShowResultsAction;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.actions.JIPipeDesktopUpdateCacheAction;
import org.hkijena.jipipe.desktop.app.history.JIPipeDesktopHistoryJournalUI;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.settings.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Graph editor UI for a project compartment graph
 */
public class JIPipeDesktopCompartmentsGraphEditorUI extends AbstractJIPipeDesktopGraphEditorUI {

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

        getCanvasUI().setDragAndDropBehavior(new JIPipeDesktopCompartmentsGraphDragAndDropBehavior());
        List<NodeUIContextAction> actions = Arrays.asList(
                new AddNewCompartmentUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SelectAllNodeUIContextAction(),
                new InvertSelectionNodeUIContextAction(),
                new AddBookmarkNodeUIContextAction(),
                new RemoveBookmarkNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new JIPipeDesktopCompartmentsCutNodeUIContextAction(),
                new JIPipeDesktopCompartmentsCopyNodeUIContextAction(),
                new JIPipeDesktopCompartmentsPasteNodeUIContextAction(),
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

        getDockPanel().addDockPanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_MAP,
                "Overview map",
                UIUtils.getIcon32FromResources("actions/zoom.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopLeft,
                true,
                new JIPipeDesktopGraphEditorMinimap(this));
        getDockPanel().addDockPanel(DOCK_QUICK_GUIDE,
                "Quick guide",
                UIUtils.getIcon32FromResources("actions/help-about.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                true,
                new JIPipeDesktopCompartmentsQuickGuidePanel(getDesktopWorkbench(), this));
        getDockPanel().addDockPanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_BOOKMARKS,
                "Bookmarks",
                UIUtils.getIcon32FromResources("actions/bookmarks.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                new JIPipeDesktopBookmarkListPanel(getDesktopWorkbench(), getGraph(), this, null));
        getDockPanel().addDockPanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_HISTORY,
                "History",
                UIUtils.getIcon32FromResources("actions/edit-undo-history.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                new JIPipeDesktopHistoryJournalUI(getHistoryJournal()));
        getDockPanel().addDockPanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_LOG,
                "Log",
                UIUtils.getIcon32FromResources("actions/rabbitvcs-show_log.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomRight,
                false,
                new JIPipeDesktopGraphEditorLogPanel(getDesktopWorkbench()));
        getDockPanel().addDockPanel(DOCK_ADD_NODES,
                "Add nodes",
                UIUtils.getIcon32FromResources("actions/node-add.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                new JIPipeDesktopCompartmentsAddCompartmentsPanel(getDesktopWorkbench(), this));
        getDockPanel().addDockPanel(DOCK_ERRORS,
                "Errors",
                UIUtils.getIcon32FromResources("actions/dialog-warning-2.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomRight,
                false,
                new JIPipeDesktopGraphEditorErrorPanel(getDesktopWorkbench(), this));
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
        if(getSelection().size() == 1) {
            JIPipeDesktopGraphNodeUI nodeUI = getSelection().iterator().next();
            showSelectedNodeDocks(nodeUI);
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
                UIUtils.getIcon32FromResources("actions/configure3.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopRight,
                true,
                parametersPanel);
        getDockPanel().addDockPanel(DOCK_NODE_CONTEXT_HELP,
                "Documentation",
                UIUtils.getIcon32FromResources("actions/help-question.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomRight,
                true,
                parametersPanel.getParametersUI().getHelpPanel());
        getDockPanel().addDockPanel(DOCK_NODE_CONTEXT_SLOT_MANAGER,
                "Connections overview",
                UIUtils.getIcon32FromResources("actions/labplot-editbreaklayout.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopRight,
                false,
                () -> new JIPipeDesktopGraphNodeSlotEditorUI(this, node));
        if(node instanceof JIPipeProjectCompartment && !((JIPipeProjectCompartment) node).getOutputNodes().isEmpty()) {
            getDockPanel().addDockPanel(DOCK_NODE_CONTEXT_RESULTS,
                    "Results",
                    UIUtils.getIcon32FromResources("actions/network-server-database.png"),
                    JIPipeDesktopDockPanel.PanelLocation.TopRight,
                    false,
                    () -> createResultsDock((JIPipeProjectCompartment) node));
        }
    }

    private JComponent createResultsDock(JIPipeProjectCompartment compartment) {
        JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Bottom);
        for (JIPipeOutputDataSlot outputSlot : compartment.getOutputSlots()) {
            JIPipeProjectCompartmentOutput outputNode = compartment.getOutputNode(outputSlot.getName());
            tabPane.addTab(outputNode.getOutputSlotName(),
                    UIUtils.getIconFromResources("actions/graph-compartment.png"),
                    new JIPipeDesktopAlgorithmCacheBrowserUI((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(), outputNode, getCanvasUI()),
                    JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        }
        return tabPane;
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
        if (event.getAction() instanceof JIPipeDesktopRunAndShowResultsAction) {
            selectOnly(event.getUi());
            JIPipeDesktopCompartmentsGraphEditorRunManager runManager = new JIPipeDesktopCompartmentsGraphEditorRunManager(getWorkbench().getProject(), getCanvasUI(), event.getUi(), getDockPanel());
            runManager.run(true,
                    ((JIPipeDesktopRunAndShowResultsAction) event.getAction()).isStoreIntermediateResults(),
                    false);
        } else if (event.getAction() instanceof JIPipeDesktopUpdateCacheAction) {
            selectOnly(event.getUi());
            JIPipeDesktopCompartmentsGraphEditorRunManager runManager = new JIPipeDesktopCompartmentsGraphEditorRunManager(getWorkbench().getProject(), getCanvasUI(), event.getUi(), getDockPanel());
            runManager.run(false,
                    ((JIPipeDesktopUpdateCacheAction) event.getAction()).isStoreIntermediateResults(),
                    ((JIPipeDesktopUpdateCacheAction) event.getAction()).isOnlyPredecessors());
        }
    }

    @Override
    public JIPipeNodeDatabaseRole getNodeDatabaseRole() {
        return JIPipeNodeDatabaseRole.CompartmentNode;
    }

    @Override
    protected void restoreDockStateFromSettings() {
        try {
            JIPipeGraphEditorUIApplicationSettings.DockLayoutSettings settings = JIPipeGraphEditorUIApplicationSettings.getInstance().getDockLayoutSettings();
            JIPipeDesktopDockPanel.State state = JsonUtils.readFromString(settings.getCompartmentsEditorDockLayout(), JIPipeDesktopDockPanel.State.class);
            getDockPanel().restoreState(state);
        }
        catch (Throwable ignored) {
        }
    }

    @Override
    protected void saveDockStateToSettings() {
        if(JIPipe.isInstantiated()) {
            JIPipeGraphEditorUIApplicationSettings.DockLayoutSettings settings = JIPipeGraphEditorUIApplicationSettings.getInstance().getDockLayoutSettings();
            settings.setCompartmentsEditorDockLayout(JsonUtils.toJsonString(getDockPanel().getCurrentState()));
            JIPipe.getSettings().saveLater();
        }
    }

    @Override
    protected StringAndStringPairParameter.List getDockStateTemplates() {
        if(JIPipe.isInstantiated()) {
            JIPipeGraphEditorUIApplicationSettings.DockLayoutSettings settings = JIPipeGraphEditorUIApplicationSettings.getInstance().getDockLayoutSettings();
            return settings.getCompartmentsEditorDockLayoutTemplates();
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
        state.put(DOCK_LOG, false, JIPipeDesktopDockPanel.PanelLocation.BottomRight);
        state.put(DOCK_ERRORS, false, JIPipeDesktopDockPanel.PanelLocation.BottomRight);
        state.put(DOCK_NODE_CONTEXT_PARAMETERS, true, JIPipeDesktopDockPanel.PanelLocation.TopRight);
        state.put(DOCK_NODE_CONTEXT_SLOT_MANAGER, false, JIPipeDesktopDockPanel.PanelLocation.TopRight);
        state.put(DOCK_NODE_CONTEXT_RESULTS, false, JIPipeDesktopDockPanel.PanelLocation.TopRight);

        getDockPanel().restoreState(state);
    }
}
