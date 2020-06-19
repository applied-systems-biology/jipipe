package org.hkijena.acaq5.ui.compartments;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.ACAQExportedCompartment;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.grouping.NodeGroup;
import org.hkijena.acaq5.extensions.settings.FileChooserSettings;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.extensionbuilder.ACAQJsonAlgorithmExporter;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQSlotEditorUI;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * UI for a single {@link ACAQProjectCompartment}
 */
public class ACAQSingleCompartmentSelectionPanelUI extends ACAQProjectWorkbenchPanel {
    private ACAQProjectCompartment compartment;
    private ACAQAlgorithmGraphCanvasUI canvas;

    /**
     * @param workbenchUI the workbench
     * @param compartment the compartment
     * @param canvas      the graph canvas
     */
    public ACAQSingleCompartmentSelectionPanelUI(ACAQProjectWorkbench workbenchUI, ACAQProjectCompartment compartment, ACAQAlgorithmGraphCanvasUI canvas) {
        super(workbenchUI);
        this.compartment = compartment;
        this.canvas = canvas;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabbedPane = new DocumentTabPane();

        ParameterPanel parametersUI = new ParameterPanel(getProjectWorkbench(),
                compartment,
                MarkdownDocument.fromPluginResource("documentation/compartment-graph.md"),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SCROLLING);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("cog.png"),
                parametersUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQSlotEditorUI compartmentSlotEditorUI = new ACAQSlotEditorUI(compartment);
        tabbedPane.addTab("Connections", UIUtils.getIconFromResources("graph-compartment.png"),
                compartmentSlotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQSlotEditorUI slotEditorUI = new ACAQSlotEditorUI(compartment.getOutputNode());
        tabbedPane.addTab("Output data", UIUtils.getIconFromResources("database.png"),
                slotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        add(tabbedPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(compartment.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(compartment.getDeclaration())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getProjectCompartmentTooltip(compartment, getProject().getGraph()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        if (canvas.getCopyPasteBehavior() != null) {
            JButton cutButton = new JButton(UIUtils.getIconFromResources("cut.png"));
            cutButton.setToolTipText("Cut");
            cutButton.addActionListener(e -> canvas.getCopyPasteBehavior().cut(Collections.singleton(compartment)));
            toolBar.add(cutButton);

            JButton copyButton = new JButton(UIUtils.getIconFromResources("copy.png"));
            copyButton.setToolTipText("Copy");
            copyButton.addActionListener(e -> canvas.getCopyPasteBehavior().copy(Collections.singleton(compartment)));
            toolBar.add(copyButton);
        }

        JButton openButton = new JButton("Open in editor", UIUtils.getIconFromResources("edit.png"));
        openButton.addActionListener(e -> openInEditor());
        toolBar.add(openButton);

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("export.png"));

        JPopupMenu exportMenu = UIUtils.addPopupMenuToComponent(exportButton);

        JMenuItem exportToAlgorithmButton = new JMenuItem("As custom algorithm", UIUtils.getIconFromResources("cog.png"));
        exportToAlgorithmButton.addActionListener(e -> exportCompartmentToAlgorithm());
        exportMenu.add(exportToAlgorithmButton);

        JMenuItem exportToFileButton = new JMenuItem("As JSON file", UIUtils.getIconFromResources("filetype-text.png"));
        exportToFileButton.addActionListener(e -> exportCompartmentToJSON());
        exportMenu.add(exportToFileButton);

        toolBar.add(exportButton);

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete compartment");
        deleteButton.addActionListener(e -> deleteCompartment());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void exportCompartmentToAlgorithm() {
        final String compartmentId = compartment.getProjectCompartmentId();
        ACAQValidityReport report = new ACAQValidityReport();
        for (Map.Entry<String, ACAQGraphNode> entry : getProject().getGraph().getAlgorithmNodes().entrySet()) {
            if (Objects.equals(entry.getValue().getCompartment(), compartmentId)) {
                report.forCategory(entry.getKey()).report(entry.getValue());
            }
        }
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report, false);
            return;
        }

        ACAQAlgorithmGraph extractedGraph = getProject().getGraph().extract(getProject().getGraph().getAlgorithmsWithCompartment(compartmentId), true);
        NodeGroup nodeGroup = new NodeGroup(extractedGraph, true);
        ACAQJsonAlgorithmExporter.createExporter(getProjectWorkbench(), nodeGroup, compartment.getName(), compartment.getCustomDescription());
    }

    private void exportCompartmentToJSON() {
        ACAQExportedCompartment exportedCompartment = new ACAQExportedCompartment(compartment);
        exportedCompartment.getMetadata().setName(compartment.getName());
        exportedCompartment.getMetadata().setDescription("An exported ACAQ5 compartment");
        ParameterPanel metadataEditor = new ParameterPanel(getProjectWorkbench(), exportedCompartment.getMetadata(),
                null,
                ParameterPanel.WITH_SCROLLING);

        if (JOptionPane.showConfirmDialog(this, metadataEditor, "Export compartment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Save ACAQ5 graph compartment (*.json)");
            if (selectedPath != null) {
                try {
                    exportedCompartment.saveToJson(selectedPath);
                    getProjectWorkbench().sendStatusBarText("Exported compartment '" + compartment.getName() + "' to " + selectedPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void openInEditor() {
        getProjectWorkbench().openCompartmentGraph(compartment, true);
    }

    private void deleteCompartment() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to delete the compartment '" + compartment.getName() + "'?\n" +
                "You will lose all nodes stored in this compartment.", "Delete compartment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            compartment.getProject().removeCompartment(compartment);
        }
    }

    /**
     * @return the compartment
     */
    public ACAQGraphNode getCompartment() {
        return compartment;
    }
}
