/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.grapheditor.compartments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.compartments.JIPipeExportedCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.extensions.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.bookmarks.BookmarkListPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.actions.RunAndShowResultsAction;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.actions.UpdateCacheAction;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.properties.JIPipePipelineMultiAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.properties.JIPipePipelineSingleAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.ui.grapheditor.compartments.contextmenu.clipboard.GraphCompartmentCopyNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.compartments.contextmenu.clipboard.GraphCompartmentCutNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.compartments.contextmenu.clipboard.GraphCompartmentPasteNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.compartments.dragdrop.JIPipeCompartmentGraphDragAndDropBehavior;
import org.hkijena.jipipe.ui.grapheditor.compartments.properties.JIPipeMultiCompartmentSelectionPanelUI;
import org.hkijena.jipipe.ui.grapheditor.compartments.properties.JIPipeSingleCompartmentSelectionPanelUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorMinimap;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.*;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.ui.history.HistoryJournalUI;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Graph editor UI for a project compartment graph
 */
public class JIPipeCompartmentsGraphEditorUI extends JIPipeGraphEditorUI {
    private JPanel defaultPanel;

    private boolean disableUpdateOnSelection = false;

    /**
     * @param workbenchUI The workbench UI
     */
    public JIPipeCompartmentsGraphEditorUI(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI, workbenchUI.getProject().getCompartmentGraph(), null, workbenchUI.getProject().getHistoryJournal());
        initializeDefaultPanel();
        setPropertyPanel(defaultPanel);

        getCanvasUI().setDragAndDropBehavior(new JIPipeCompartmentGraphDragAndDropBehavior());
        List<NodeUIContextAction> actions = Arrays.asList(
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
                new SelectAndMoveNodeHereNodeUIContextAction()
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

        JIPipeGraphEditorMinimap minimap = new JIPipeGraphEditorMinimap(this);
        splitPane.setTopComponent(minimap);

        DocumentTabPane bottomPanel = new DocumentTabPane(false);

        MarkdownReader markdownReader = new MarkdownReader(false);
        markdownReader.setDocument(MarkdownDocument.fromPluginResource("documentation/compartment-graph.md", new HashMap<>()));
        bottomPanel.addTab("Quick guide", UIUtils.getIconFromResources("actions/help.png"), markdownReader, DocumentTabPane.CloseMode.withoutCloseButton);

        bottomPanel.addTab("Bookmarks", UIUtils.getIconFromResources("actions/bookmarks.png"),
                new BookmarkListPanel(getWorkbench(), getProject().getGraph(), this), DocumentTabPane.CloseMode.withoutCloseButton);

        bottomPanel.addTab("Journal",
                UIUtils.getIconFromResources("actions/edit-undo-history.png"),
                new HistoryJournalUI(getHistoryJournal()),
                DocumentTabPane.CloseMode.withoutCloseButton);

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
            setPropertyPanel(defaultPanel);
        } else if (getSelection().size() == 1) {
            JIPipeGraphNode node = getSelection().iterator().next().getNode();
            if (node instanceof JIPipeProjectCompartment) {
                setPropertyPanel(new JIPipeSingleCompartmentSelectionPanelUI(this,
                        (JIPipeProjectCompartment) node));
            } else {
                setPropertyPanel(new JIPipePipelineSingleAlgorithmSelectionPanelUI(this, node));
            }
        } else {
            if (getSelection().stream().allMatch(ui -> ui.getNode() instanceof JIPipeProjectCompartment)) {
                setPropertyPanel(new JIPipeMultiCompartmentSelectionPanelUI((JIPipeProjectWorkbench) getWorkbench(),
                        getSelection().stream().map(ui -> (JIPipeProjectCompartment) ui.getNode()).collect(Collectors.toSet()), getCanvasUI()));
            } else {
                setPropertyPanel(new JIPipePipelineMultiAlgorithmSelectionPanelUI((JIPipeProjectWorkbench) getWorkbench(), getCanvasUI(),
                        getSelection().stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet())));
            }
        }
    }

    /**
     * Initializes the "Add nodes" area
     */
    protected void initializeAddNodesMenus() {
        JIPipeNodeInfo info = JIPipe.getNodes().getInfoById("jipipe:project-compartment");

        JButton addItem = new JButton("Add new compartment", UIUtils.getIconFromResources("actions/list-add.png"));
        UIUtils.makeFlatH25(addItem);
        addItem.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(info));
        addItem.addActionListener(e -> addCompartment());
        menuBar.add(addItem);

        JButton importItem = new JButton("Import compartment", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        UIUtils.makeFlatH25(importItem);
        importItem.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        importItem.setToolTipText("Imports a compartment from a *.jipc file");
        importItem.addActionListener(e -> importCompartment());
        menuBar.add(importItem);

        if (JIPipe.getNodes().hasNodeInfoWithId("jipipe:comment")) {
            JButton addCommentItem = new JButton("Add comment", UIUtils.getIconFromResources("actions/edit-comment.png"));
            UIUtils.makeFlatH25(addCommentItem);
            addCommentItem.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            addCommentItem.setToolTipText("Add a comment node");
            addCommentItem.addActionListener(e -> addComment());
            menuBar.add(addCommentItem);
        }
    }

    private void addComment() {
        if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(getWorkbench()))
            return;
        JIPipeCommentNode node = JIPipe.createNode(JIPipeCommentNode.class);
        if (getCanvasUI().getHistoryJournal() != null) {
            getCanvasUI().getHistoryJournal().snapshotBeforeAddNode(node, null);
        }
        getAlgorithmGraph().insertNode(node);
    }

    private void importCompartment() {
        if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(getWorkbench()))
            return;
        Path selectedPath = FileChooserSettings.openFile(this,
                FileChooserSettings.LastDirectoryKey.Projects,
                "Open JIPipe graph compartment (*.jipc)",
                UIUtils.EXTENSION_FILTER_JIPC);
        if (selectedPath != null) {
            try {
                ObjectMapper objectMapper = JsonUtils.getObjectMapper();
                JIPipeExportedCompartment exportedCompartment = objectMapper.readerFor(JIPipeExportedCompartment.class).readValue(selectedPath.toFile());

                String name = UIUtils.getUniqueStringByDialog(this, "Please enter the name of the new compartment:",
                        exportedCompartment.getSuggestedName(), s -> getProject().getCompartments().containsKey(s));
                if (name != null && !name.isEmpty()) {
                    if (getHistoryJournal() != null) {
                        getHistoryJournal().snapshotBeforeAddCompartment(name);
                    }
                    exportedCompartment.addTo(getProject(), name);
                }
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }

    private JIPipeProject getProject() {
        return ((JIPipeProjectWorkbench) getWorkbench()).getProject();
    }

    private JIPipeProjectWorkbench getProjectWorkbench() {
        return (JIPipeProjectWorkbench) getWorkbench();
    }

    private void addCompartment() {
        if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(getWorkbench()))
            return;
        String compartmentName = UIUtils.getUniqueStringByDialog(this, "Please enter the name of the compartment",
                "Compartment", s -> getProject().getCompartments().containsKey(s));
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
    @Subscribe
    public void onOpenCompartment(JIPipeGraphCanvasUI.DefaultAlgorithmUIActionRequestedEvent event) {
        if (event.getUi() != null && event.getUi().getNode() instanceof JIPipeProjectCompartment) {
            getProjectWorkbench().getOrOpenPipelineEditorTab((JIPipeProjectCompartment) event.getUi().getNode(), true);
        }
    }

    /**
     * Listens to events of algorithms requesting some action
     *
     * @param event the event
     */
    @Subscribe
    public void onAlgorithmActionRequested(JIPipeGraphCanvasUI.NodeUIActionRequestedEvent event) {
        if (event.getAction() instanceof RunAndShowResultsAction) {
            disableUpdateOnSelection = true;
            selectOnly(event.getUi());
            JIPipeSingleCompartmentSelectionPanelUI panel = new JIPipeSingleCompartmentSelectionPanelUI(this,
                    (JIPipeProjectCompartment) event.getUi().getNode());
            setPropertyPanel(panel);
            panel.executeQuickRun(true,
                    false,
                    true,
                    ((RunAndShowResultsAction) event.getAction()).isStoreIntermediateResults(),
                    false);
            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
        } else if (event.getAction() instanceof UpdateCacheAction) {
            disableUpdateOnSelection = true;
            selectOnly(event.getUi());
            JIPipeSingleCompartmentSelectionPanelUI panel = new JIPipeSingleCompartmentSelectionPanelUI(this,
                    (JIPipeProjectCompartment) event.getUi().getNode());
            setPropertyPanel(panel);
            panel.executeQuickRun(false,
                    true,
                    false,
                    ((UpdateCacheAction) event.getAction()).isStoreIntermediateResults(),
                    false);
            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
        }
    }
}
