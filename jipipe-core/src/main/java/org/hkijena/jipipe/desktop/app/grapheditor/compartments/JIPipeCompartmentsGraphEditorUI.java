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
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseRole;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorMinimap;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.*;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.contextmenu.clipboard.GraphCompartmentCopyNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.contextmenu.clipboard.GraphCompartmentCutNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.contextmenu.clipboard.GraphCompartmentPasteNodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.dragdrop.JIPipeCompartmentGraphDragAndDropBehavior;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.properties.JIPipeDesktopMultiCompartmentSelectionPanelUI;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.properties.JIPipeDesktopSingleCompartmentSelectionPanelUI;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.actions.JIPipeDesktopRunAndShowResultsAction;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.actions.JIPipeDesktopUpdateCacheAction;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.properties.JIPipeDesktopPipelineMultiAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.properties.JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.desktop.app.history.JIPipeDesktopHistoryJournalUI;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Graph editor UI for a project compartment graph
 */
public class JIPipeCompartmentsGraphEditorUI extends JIPipeDesktopGraphEditorUI {
    private JPanel defaultPanel;

    private boolean disableUpdateOnSelection = false;

    /**
     * @param workbenchUI The workbench UI
     */
    public JIPipeCompartmentsGraphEditorUI(JIPipeDesktopProjectWorkbench workbenchUI) {
        super(workbenchUI, workbenchUI.getProject().getCompartmentGraph(), null, workbenchUI.getProject().getHistoryJournal());
        initializeDefaultPanel();
        setPropertyPanel(defaultPanel, true);

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
                new ExportCompartmentToNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new DeleteCompartmentUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SetNodeHotkeyContextAction(),
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

    private void initializeDefaultPanel() {
        defaultPanel = new JPanel(new BorderLayout());

        JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, AutoResizeSplitPane.RATIO_1_TO_3);
        defaultPanel.add(splitPane, BorderLayout.CENTER);

        JIPipeDesktopGraphEditorMinimap minimap = new JIPipeDesktopGraphEditorMinimap(this);
        splitPane.setTopComponent(minimap);

        JIPipeDesktopTabPane bottomPanel = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.TabPlacement.Right);

        JIPipeDesktopMarkdownReader markdownReader = new JIPipeDesktopMarkdownReader(false);
        markdownReader.setDocument(MarkdownText.fromPluginResource("documentation/compartment-graph.md", new HashMap<>()));
        bottomPanel.addTab("Quick guide", UIUtils.getIcon32FromResources("actions/help.png"), markdownReader, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        bottomPanel.addTab("Bookmarks", UIUtils.getIcon32FromResources("actions/bookmarks.png"),
                new JIPipeDesktopBookmarkListPanel(getDesktopWorkbench(), getProject().getGraph(), this, null), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        bottomPanel.addTab("Journal",
                UIUtils.getIcon32FromResources("actions/edit-undo-history.png"),
                new JIPipeDesktopHistoryJournalUI(getHistoryJournal()),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        splitPane.setBottomComponent(bottomPanel);
    }

    @Override
    public void reloadMenuBar() {
        getMenuBar().removeAll();
        initializeAddNodesMenus();
        initializeCommonActions();
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
        if (disableUpdateOnSelection)
            return;
        if (getSelection().isEmpty()) {
            setPropertyPanel(defaultPanel, true);
        } else if (getSelection().size() == 1) {
            JIPipeGraphNode node = getSelection().iterator().next().getNode();
            if (node instanceof JIPipeProjectCompartment) {
                setPropertyPanel(new JIPipeDesktopSingleCompartmentSelectionPanelUI(this,
                        (JIPipeProjectCompartment) node), true);
            } else {
                setPropertyPanel(new JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI(this, node), true);
            }
        } else {
            if (getSelection().stream().allMatch(ui -> ui.getNode() instanceof JIPipeProjectCompartment)) {
                setPropertyPanel(new JIPipeDesktopMultiCompartmentSelectionPanelUI((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(),
                        getSelection().stream().map(ui -> (JIPipeProjectCompartment) ui.getNode()).collect(Collectors.toSet()), getCanvasUI()), true);
            } else {
                setPropertyPanel(new JIPipeDesktopPipelineMultiAlgorithmSelectionPanelUI((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(), getCanvasUI(),
                        getSelection().stream().map(JIPipeDesktopGraphNodeUI::getNode).collect(Collectors.toSet())), true);
            }
        }
    }

    /**
     * Initializes the "Add nodes" area
     */
    protected void initializeAddNodesMenus() {
        JIPipeNodeInfo info = JIPipe.getNodes().getInfoById("jipipe:project-compartment");

        JButton addItem = new JButton("Add new compartment", UIUtils.getIconFromResources("actions/list-add.png"));
        addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(info));
        addItem.addActionListener(e -> addCompartment());
        menuBar.add(addItem);
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
        if (event.getAction() instanceof JIPipeDesktopRunAndShowResultsAction) {
            disableUpdateOnSelection = true;
            selectOnly(event.getUi());
            JIPipeDesktopSingleCompartmentSelectionPanelUI panel = new JIPipeDesktopSingleCompartmentSelectionPanelUI(this,
                    (JIPipeProjectCompartment) event.getUi().getNode());
            setPropertyPanel(panel, true);
            panel.executeQuickRun(true,
                    false,
                    true,
                    ((JIPipeDesktopRunAndShowResultsAction) event.getAction()).isStoreIntermediateResults(),
                    false);
            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
        } else if (event.getAction() instanceof JIPipeDesktopUpdateCacheAction) {
            disableUpdateOnSelection = true;
            selectOnly(event.getUi());
            JIPipeDesktopSingleCompartmentSelectionPanelUI panel = new JIPipeDesktopSingleCompartmentSelectionPanelUI(this,
                    (JIPipeProjectCompartment) event.getUi().getNode());
            setPropertyPanel(panel, true);
            panel.executeQuickRun(false,
                    true,
                    false,
                    ((JIPipeDesktopUpdateCacheAction) event.getAction()).isStoreIntermediateResults(),
                    ((JIPipeDesktopUpdateCacheAction) event.getAction()).isOnlyPredecessors());
            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
        }
    }

    @Override
    public JIPipeNodeDatabaseRole getNodeDatabaseRole() {
        return JIPipeNodeDatabaseRole.CompartmentNode;
    }
}
