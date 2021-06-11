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

package org.hkijena.jipipe.ui.compartments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.compartments.JIPipeExportedCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.history.AddCompartmentGraphHistorySnapshot;
import org.hkijena.jipipe.api.history.AddNodeGraphHistorySnapshot;
import org.hkijena.jipipe.api.history.ImportCompartmentGraphHistorySnapshot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.extensions.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.grapheditor.*;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.*;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.GraphCompartmentCopyNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.GraphCompartmentCutNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.GraphCompartmentPasteNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.settings.JIPipeMultiAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.ui.grapheditor.settings.JIPipeSingleAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Graph editor UI for a project compartment graph
 */
public class JIPipeCompartmentGraphUI extends JIPipeGraphEditorUI {
    private JPanel defaultPanel;

    /**
     * @param workbenchUI The workbench UI
     */
    public JIPipeCompartmentGraphUI(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI, workbenchUI.getProject().getCompartmentGraph(), null);
        initializeDefaultPanel();
        setPropertyPanel(defaultPanel);

        getCanvasUI().setDragAndDropBehavior(new JIPipeCompartmentGraphDragAndDropBehavior());
        List<NodeUIContextAction> actions = Arrays.asList(
                new SelectAllNodeUIContextAction(),
                new InvertSelectionNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new GraphCompartmentCutNodeUIContextAction(),
                new GraphCompartmentCopyNodeUIContextAction(),
                new GraphCompartmentPasteNodeUIContextAction(),
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

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
        defaultPanel.add(splitPane, BorderLayout.CENTER);

        JIPipeGraphEditorMinimap minimap = new JIPipeGraphEditorMinimap(this);
        splitPane.setTopComponent(minimap);

        MarkdownReader markdownReader = new MarkdownReader(false);
        markdownReader.setDocument(MarkdownDocument.fromPluginResource("documentation/compartment-graph.md", new HashMap<>()));
        splitPane.setBottomComponent(markdownReader);
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
        if (getSelection().isEmpty()) {
            setPropertyPanel(defaultPanel);
        } else if (getSelection().size() == 1) {
            JIPipeGraphNode node = getSelection().iterator().next().getNode();
            if (node instanceof JIPipeProjectCompartment) {
                setPropertyPanel(new JIPipeSingleCompartmentSelectionPanelUI(this,
                        (JIPipeProjectCompartment) node));
            } else {
                setPropertyPanel(new JIPipeSingleAlgorithmSelectionPanelUI(this, node));
            }
        } else {
            if (getSelection().stream().allMatch(ui -> ui.getNode() instanceof JIPipeProjectCompartment)) {
                setPropertyPanel(new JIPipeMultiCompartmentSelectionPanelUI((JIPipeProjectWorkbench) getWorkbench(),
                        getSelection().stream().map(ui -> (JIPipeProjectCompartment) ui.getNode()).collect(Collectors.toSet()), getCanvasUI()));
            } else {
                setPropertyPanel(new JIPipeMultiAlgorithmSelectionPanelUI((JIPipeProjectWorkbench) getWorkbench(), getCanvasUI(),
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
        AddNodeGraphHistorySnapshot snapshot = new AddNodeGraphHistorySnapshot(getAlgorithmGraph(), Collections.singleton(node));
        getCanvasUI().getGraphHistory().addSnapshotBefore(snapshot);
        getAlgorithmGraph().insertNode(node);
    }

    private void importCompartment() {
        if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(getWorkbench()))
            return;
        Path selectedPath = FileChooserSettings.openFile(this,
                FileChooserSettings.KEY_PROJECT,
                "Open JIPipe graph compartment (*.jipc)",
                UIUtils.EXTENSION_FILTER_JIPC);
        if (selectedPath != null) {
            try {
                ObjectMapper objectMapper = JsonUtils.getObjectMapper();
                JIPipeExportedCompartment exportedCompartment = objectMapper.readerFor(JIPipeExportedCompartment.class).readValue(selectedPath.toFile());

                String name = UIUtils.getUniqueStringByDialog(this, "Please enter the name of the new compartment:",
                        exportedCompartment.getSuggestedName(), s -> getProject().getCompartments().containsKey(s));
                if (name != null && !name.isEmpty()) {
                    JIPipeProjectCompartment compartment = exportedCompartment.addTo(getProject(), name);
                    getCanvasUI().getGraphHistory().addSnapshotBefore(new ImportCompartmentGraphHistorySnapshot(getProject(), compartment));
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
            AddCompartmentGraphHistorySnapshot snapshot = new AddCompartmentGraphHistorySnapshot(getProject(), compartmentName);
            getCanvasUI().getGraphHistory().addSnapshotBefore(snapshot);
            JIPipeProjectCompartment compartment = getProject().addCompartment(compartmentName);
            snapshot.setCompartmentInstance(compartment);
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
            getProjectWorkbench().openCompartmentGraph((JIPipeProjectCompartment) event.getUi().getNode(), true);
        }
    }
}
