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

package org.hkijena.jipipe.desktop.app.grapheditor.pipeline;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.grouping.JIPipeNodeGroup;
import org.hkijena.jipipe.api.history.JIPipeDedicatedGraphHistoryJournal;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseRole;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.batchassistant.JIPipeDesktopDataBatchAssistantUI;
import org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorLogPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorMinimap;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeGraphEditorRunManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.*;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphNodeSlotEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.contextmenu.clipboard.clipboard.*;
import org.hkijena.jipipe.desktop.app.grapheditor.groups.JIPipeDesktopNodeGroupUI;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.actions.JIPipeDesktopRunAndShowResultsAction;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.actions.JIPipeDesktopUpdateCacheAction;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.addnodepanel.JIPipeDesktopAddNodePanel;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.dragdrop.JIPipeCreateNodesFromDraggedDataDragAndDropBehavior;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.properties.JIPipeDesktopPipelineParametersPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.properties.JIPipeDesktopPipelineQuickGuidePanel;
import org.hkijena.jipipe.desktop.app.history.JIPipeDesktopHistoryJournalUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplateBox;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Editor for a project graph compartment
 */
public class JIPipePipelineGraphEditorUI extends JIPipeDesktopGraphEditorUI {

    /**
     * Creates a project graph compartment editor
     *
     * @param workbenchUI    The workbench
     * @param algorithmGraph The graph
     * @param compartment    The compartment
     */
    public JIPipePipelineGraphEditorUI(JIPipeDesktopWorkbench workbenchUI, JIPipeGraph algorithmGraph, UUID compartment) {
        super(workbenchUI, algorithmGraph, compartment, algorithmGraph.getProject() != null ? algorithmGraph.getProject().getHistoryJournal() : new JIPipeDedicatedGraphHistoryJournal(algorithmGraph));
        initializeDefaultPanels();

        // Set D&D and Copy&Paste behavior
        initializeContextActions();
    }

    private void initializeContextActions() {
        getCanvasUI().setDragAndDropBehavior(new JIPipeCreateNodesFromDraggedDataDragAndDropBehavior());
        List<NodeUIContextAction> nodeSpecificContextActions = new ArrayList<>();
        if (JIPipeGeneralUIApplicationSettings.getInstance().isAddContextActionsToContextMenu()) {
            for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
                for (Method method : info.getInstanceClass().getMethods()) {
                    JIPipeContextAction actionAnnotation = method.getAnnotation(JIPipeContextAction.class);
                    if (actionAnnotation == null)
                        continue;
                    if (!actionAnnotation.showInContextMenu())
                        continue;
                    SetJIPipeDocumentation documentationAnnotation = method.getAnnotation(SetJIPipeDocumentation.class);
                    if (documentationAnnotation == null) {
                        documentationAnnotation = new JIPipeDocumentation(method.getName(), "");
                    }
                    URL iconURL;
                    if (UIUtils.DARK_THEME && !StringUtils.isNullOrEmpty(actionAnnotation.iconDarkURL())) {
                        iconURL = actionAnnotation.resourceClass().getResource(actionAnnotation.iconDarkURL());
                    } else {
                        if (!StringUtils.isNullOrEmpty(actionAnnotation.iconURL())) {
                            iconURL = actionAnnotation.resourceClass().getResource(actionAnnotation.iconURL());
                        } else {
                            iconURL = UIUtils.getIconURLFromResources("actions/configure.png");
                        }
                    }
                    if (iconURL == null) {
                        iconURL = UIUtils.getIconURLFromResources("actions/configure.png");
                    }
                    Icon icon = new ImageIcon(iconURL);

                    NodeContextActionWrapperUIContextAction action = new NodeContextActionWrapperUIContextAction(info,
                            documentationAnnotation.name(),
                            DocumentationUtils.getDocumentationDescription(documentationAnnotation),
                            icon,
                            method);
                    nodeSpecificContextActions.add(action);
                }
            }
        }

        List<NodeUIContextAction> actions = new ArrayList<>(Arrays.asList(
                new AddNewNodeUIContextAction(),
                new AddNewParameterSetNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SelectAllNodeUIContextAction(),
                new InvertSelectionNodeUIContextAction(),
                new AddBookmarkNodeUIContextAction(),
                new RemoveBookmarkNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new AlgorithmGraphCutNodeUIContextAction(),
                new AlgorithmGraphCopyNodeUIContextAction(),
                new AlgorithmGraphPasteNodeUIContextAction(),
                new AlgorithmGraphDuplicateNodeUIContextAction(),
                new AlgorithmGraphDuplicateWithInputConnectionsNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new RunAndShowResultsNodeUIContextAction(),
                new UpdateCacheNodeUIContextAction(),
                new OpenCacheBrowserInWindowUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new RunAndShowIntermediateResultsNodeUIContextAction(),
                new UpdateCacheShowIntermediateNodeUIContextAction(),
                new UpdateCacheOnlyPredecessorsNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new ClearCacheNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new IsolateNodesUIContextAction(),
                new JsonAlgorithmToGroupNodeUIContextAction(),
                new GroupNodeUIContextAction(),
                new CollapseIOInterfaceNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new EnableNodeUIContextAction(),
                new DisableNodeUIContextAction(),
                new EnablePassThroughNodeUIContextAction(),
                new DisablePassThroughNodeUIContextAction(),
                new EnableSaveOutputsNodeUIContextAction(),
                new DisableSaveOutputsNodeUIContextAction(),
                new DeleteNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SendToForegroundUIContextAction(),
                new RaiseUIContextAction(),
                new LowerUIContextAction(),
                new SendToBackgroundUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SelectAndMoveNodeHereNodeUIContextAction(),
                new LockNodeLocationSizeUIContextAction(),
                new UnlockNodeLocationSizeUIContextAction()
        ));

        // Custom entries (from registry)
        List<NodeUIContextAction> registeredEntries = JIPipe.getCustomMenus().getRegisteredContextMenuActions().stream()
                .filter(NodeUIContextAction::showInGraphCompartment)
                .sorted(Comparator.comparing(NodeUIContextAction::getName))
                .collect(Collectors.toList());
        if (!registeredEntries.isEmpty()) {
            actions.add(NodeUIContextAction.SEPARATOR);
            actions.addAll(registeredEntries);
        }

        // Node context actions
        if (!nodeSpecificContextActions.isEmpty()) {
            actions = new ArrayList<>(actions);
            actions.add(NodeUIContextAction.SEPARATOR);
            nodeSpecificContextActions.sort(Comparator.comparing(NodeUIContextAction::getName));
            actions.addAll(nodeSpecificContextActions);
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
                new JIPipeDesktopPipelineQuickGuidePanel(getDesktopWorkbench(), this));
        getDockPanel().addDockPanel("ADD_NODES",
                "Add nodes",
                UIUtils.getIcon32FromResources("actions/node-add.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                new JIPipeDesktopAddNodePanel(getDesktopWorkbench(), this));
        getDockPanel().addDockPanel("NODE_TEMPLATES", "Node templates",
                UIUtils.getIcon32FromResources("actions/star3.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                new NodeTemplateBox(getDesktopWorkbench(), true, getCanvasUI(), Collections.emptySet()));
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
//
//        bottomPanel.addTab("Templates", UIUtils.getIcon32FromResources("actions/star.png"),
//                new NodeTemplateBox(getDesktopWorkbench(), true, getCanvasUI(), null), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
    }

    @Override
    protected void updateSelection() {
        super.updateSelection();

        getDockPanel().removeDockPanelsIf(p -> p.getId().startsWith("_"));

        if(getSelection().size() == 1) {
            JIPipeDesktopGraphNodeUI nodeUI = getSelection().iterator().next();
            showSelectedNodeDocks(nodeUI);
        }

//        if (getSelection().isEmpty()) {
//            setPropertyPanel(defaultPanel, true);
//        } else if (getSelection().size() == 1) {
//            JIPipeDesktopGraphNodeUI ui = getSelection().iterator().next();
//            setPropertyPanel(new JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI(this, ui.getNode()), true);
//        } else {
//            setPropertyPanel(new JIPipeDesktopPipelineMultiAlgorithmSelectionPanelUI((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(), getCanvasUI(),
//                    getSelection().stream().map(JIPipeDesktopGraphNodeUI::getNode).collect(Collectors.toSet())), true);
//        }
    }

    private void showSelectedNodeDocks(JIPipeDesktopGraphNodeUI nodeUI) {
        JIPipeGraphNode node = nodeUI.getNode();
        JIPipeDesktopPipelineParametersPanel parametersPanel = new JIPipeDesktopPipelineParametersPanel(getDesktopWorkbench(),
                getCanvasUI(),
                node);
        parametersPanel.getParametersUI().getContextHelpEventEmitter().subscribeLambda((source, event) -> {
            getDockPanel().activatePanel("_HELP", true);
        });
        getDockPanel().addDockPanel("_PARAMETERS",
                "Parameters",
                UIUtils.getIcon32FromResources("actions/configure3.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopRight,
                true,
                parametersPanel);
        getDockPanel().addDockPanel("_HELP",
                "Documentation",
                UIUtils.getIcon32FromResources("actions/help-question.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomRight,
                true,
                parametersPanel.getParametersUI().getHelpPanel());
        getDockPanel().addDockPanel("_SLOTS",
                "Slots overview",
                UIUtils.getIcon32FromResources("actions/labplot-editbreaklayout.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopRight,
                false,
                () -> new JIPipeDesktopGraphNodeSlotEditorUI(this, node));
        if (node instanceof JIPipeIterationStepAlgorithm && getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            if(getDesktopWorkbench().getProject() != null) {
                getDockPanel().addDockPanel(
                        "_INPUTS",
                        "Inputs",
                        UIUtils.getIcon32FromResources("actions/input-management-2.png"),
                        JIPipeDesktopDockPanel.PanelLocation.TopRight,
                        false,
                        () ->  new JIPipeDesktopDataBatchAssistantUI((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(), node, () -> {
                            nodeUI.getNodeUIActionRequestedEventEmitter().emit(new JIPipeDesktopGraphNodeUI.NodeUIActionRequestedEvent(nodeUI,
                                    new JIPipeDesktopUpdateCacheAction(false, true)));
                        }));
            }
            else {
                getDockPanel().addDockPanel(
                        "_INPUTS",
                        "Inputs",
                        UIUtils.getIcon32FromResources("actions/input-management-2.png"),
                        JIPipeDesktopDockPanel.PanelLocation.TopRight,
                        false,
                        new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(), ((JIPipeIterationStepAlgorithm) node).getGenerationSettingsInterface(),
                                null, JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_SCROLLING));
            }
        }
        if(getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            getDockPanel().addDockPanel("_RESULTS",
                    "Results",
                    UIUtils.getIcon32FromResources("actions/network-server-database.png"),
                    JIPipeDesktopDockPanel.PanelLocation.TopRight,
                    false,
                    () -> new JIPipeDesktopAlgorithmCacheBrowserUI((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(),
                            node,
                            getCanvasUI()));
        }
//        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench && node instanceof JIPipeAlgorithm &&
//                !getDesktopWorkbench().getProject().getNodeExamples(node.getInfo().getId()).isEmpty()) {
//            getDockPanel().addDockPanel("_EXAMPLES",
//                    "Examples",
//                    UIUtils.getIcon32FromResources("actions/graduation-cap.png"),
//                    JIPipeDesktopDockPanel.PanelLocation.TopRight,
//                    false,
//                    () -> new JIPipeDesktopNodeExamplesUI((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(), (JIPipeAlgorithm) node, getDockPanel()));
//        }
    }


    @Override
    public void onDefaultNodeUIActionRequested(JIPipeDesktopGraphNodeUI.DefaultNodeUIActionRequestedEvent event) {
        JIPipeGraphNode node = event.getUi().getNode();
        if (node instanceof JIPipeNodeGroup) {
            if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
                JIPipeDesktopNodeGroupUI.openGroupNodeGraph(getDesktopWorkbench(), (JIPipeNodeGroup) node, true);
            }
        } else if (node instanceof JIPipeProjectCompartmentOutput) {
            // Open the compartment
            if (!Objects.equals(getCompartment(), node.getCompartmentUUIDInParentGraph()) && getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
                // This is an input
                JIPipeDesktopProjectWorkbench projectWorkbench = (JIPipeDesktopProjectWorkbench) getDesktopWorkbench();
                UUID uuid = node.getCompartmentUUIDInParentGraph();
                JIPipeProjectCompartment projectCompartment = projectWorkbench.getProject().getCompartments().get(uuid);
                projectWorkbench.getOrOpenPipelineEditorTab(projectCompartment, true);
            } else if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
                JIPipeDesktopProjectWorkbench projectWorkbench = (JIPipeDesktopProjectWorkbench) getDesktopWorkbench();
                UUID uuid = node.getCompartmentUUIDInParentGraph();
                JIPipeProjectCompartment projectCompartment = projectWorkbench.getProject().getCompartments().get(uuid);
                JIPipeOutputDataSlot outputSlot = projectCompartment.getFirstOutputSlot();
                JIPipeGraph compartmentGraph = projectWorkbench.getProject().getCompartmentGraph();
                List<JIPipeProjectCompartment> targets = new ArrayList<>();
                for (JIPipeGraphEdge edge : compartmentGraph.getGraph().edgesOf(outputSlot)) {
                    JIPipeGraphNode edgeTarget = compartmentGraph.getGraph().getEdgeTarget(edge).getNode();
                    if (edgeTarget instanceof JIPipeProjectCompartment && edgeTarget != projectCompartment) {
                        targets.add((JIPipeProjectCompartment) edgeTarget);
                    }
                }
                if (targets.size() > 1) {
                    JPopupMenu popupMenu = new JPopupMenu();
                    for (JIPipeProjectCompartment target : targets) {
                        popupMenu.add(UIUtils.createMenuItem("Go to '" + target.getName() + "'", "Open the '" + target.getName() + "' compartment", UIUtils.getIconFromResources("actions/graph-compartment.png"), () -> {
                            projectWorkbench.getOrOpenPipelineEditorTab(target, true);
                        }));
                    }
                    popupMenu.show(event.getUi().getGraphCanvasUI(),
                            event.getUi().getGraphCanvasUI().getLastMousePosition().x,
                            event.getUi().getGraphCanvasUI().getLastMousePosition().y);
                } else if (targets.size() == 1) {
                    projectWorkbench.getOrOpenPipelineEditorTab(targets.get(0), true);
                }
            }
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
            JIPipeGraphEditorRunManager runManager = new JIPipeGraphEditorRunManager(getWorkbench().getProject(), getCanvasUI(), event.getUi(), getDockPanel());
            runManager.run(true,
                    ((JIPipeDesktopRunAndShowResultsAction) event.getAction()).isStoreIntermediateResults(),
                    false);
        } else if (event.getAction() instanceof JIPipeDesktopUpdateCacheAction) {
            selectOnly(event.getUi());
            JIPipeGraphEditorRunManager runManager = new JIPipeGraphEditorRunManager(getWorkbench().getProject(), getCanvasUI(), event.getUi(), getDockPanel());
            runManager.run(false,
                    ((JIPipeDesktopUpdateCacheAction) event.getAction()).isStoreIntermediateResults(),
                    ((JIPipeDesktopUpdateCacheAction) event.getAction()).isOnlyPredecessors());
        }

//        if (event.getAction() instanceof JIPipeDesktopRunAndShowResultsAction) {
//            disableUpdateOnSelection = true;
//            selectOnly(event.getUi());
//            JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI panel = new JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI(this,
//                    event.getUi().getNode());
//            setPropertyPanel(panel, true);
//            panel.executeQuickRun(true,
//                    false,
//                    false,
//                    true,
//                    ((JIPipeDesktopRunAndShowResultsAction) event.getAction()).isStoreIntermediateResults(),
//                    false);
//            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
//        } else if (event.getAction() instanceof JIPipeDesktopUpdateCacheAction) {
//            disableUpdateOnSelection = true;
//            selectOnly(event.getUi());
//            JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI panel = new JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI(this,
//                    event.getUi().getNode());
//            setPropertyPanel(panel, true);
//            panel.executeQuickRun(false,
//                    true,
//                    false,
//                    false,
//                    ((JIPipeDesktopUpdateCacheAction) event.getAction()).isStoreIntermediateResults(),
//                    ((JIPipeDesktopUpdateCacheAction) event.getAction()).isOnlyPredecessors());
//            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
//        }
    }

    @Override
    public JIPipeNodeDatabaseRole getNodeDatabaseRole() {
        return JIPipeNodeDatabaseRole.PipelineNode;
    }
}
