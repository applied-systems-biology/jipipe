package org.hkijena.acaq5.ui.compartments;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.compartments.ACAQExportedCompartment;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQSlotEditorUI;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.ui.settings.ACAQGraphWrapperAlgorithmExporter;
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
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW);
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
        for (Map.Entry<String, ACAQAlgorithm> entry : getProject().getGraph().getAlgorithmNodes().entrySet()) {
            if (Objects.equals(entry.getValue().getCompartment(), compartmentId)) {
                report.forCategory(entry.getKey()).report(entry.getValue());
            }
        }
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report, false);
            return;
        }

        ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph();
        for (Map.Entry<String, ACAQAlgorithm> entry : getProject().getGraph().getAlgorithmNodes().entrySet()) {
            if (Objects.equals(entry.getValue().getCompartment(), compartmentId)) {
                ACAQAlgorithm algorithm = entry.getValue().duplicate();
                algorithm.setCompartment(ACAQAlgorithmGraph.COMPARTMENT_DEFAULT);
                graph.insertNode(entry.getKey(), algorithm, compartmentId);
            }
        }
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : getProject().getGraph().getSlotEdges()) {
            if (Objects.equals(edge.getKey().getAlgorithm().getCompartment(), compartmentId) &&
                    Objects.equals(edge.getValue().getAlgorithm().getCompartment(), compartmentId)) {
                ACAQDataSlot source = graph.getEquivalentSlot(edge.getKey());
                ACAQDataSlot target = graph.getEquivalentSlot(edge.getValue());
                graph.connect(source, target);
            }
        }

        ACAQGraphWrapperAlgorithmExporter exporter = new ACAQGraphWrapperAlgorithmExporter(getProjectWorkbench(), graph);
        exporter.getAlgorithmDeclaration().getMetadata().setName(compartment.getName());
        exporter.getAlgorithmDeclaration().getMetadata().setDescription(compartment.getCustomDescription());
        getProjectWorkbench().getDocumentTabPane().addTab("Export algorithm '" + compartment.getName() + "'",
                UIUtils.getIconFromResources("export.png"),
                exporter,
                DocumentTabPane.CloseMode.withAskOnCloseButton);
        getProjectWorkbench().getDocumentTabPane().switchToLastTab();
    }

    private void exportCompartmentToJSON() {
        ACAQExportedCompartment exportedCompartment = new ACAQExportedCompartment(compartment);
        exportedCompartment.getMetadata().setName(compartment.getName());
        exportedCompartment.getMetadata().setDescription("An exported ACAQ5 compartment");
        ParameterPanel metadataEditor = new ParameterPanel(getProjectWorkbench(), exportedCompartment.getMetadata(),
                null,
                ParameterPanel.NONE);

        if (JOptionPane.showConfirmDialog(this, metadataEditor, "Export compartment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle("Save compartment (*.json)");
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                Path savePath = fileChooser.getSelectedFile().toPath();
                try {
                    exportedCompartment.saveToJson(savePath);
                    getProjectWorkbench().sendStatusBarText("Exported compartment '" + compartment.getName() + "' to " + savePath);
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
    public ACAQAlgorithm getCompartment() {
        return compartment;
    }
}
