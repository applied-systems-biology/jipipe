package org.hkijena.acaq5.ui.compartments;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.compartments.ACAQExportedCompartment;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQSlotEditorUI;
import org.hkijena.acaq5.ui.parameters.ACAQParameterAccessUI;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

/**
 * UI for a single {@link ACAQProjectCompartment}
 */
public class ACAQSingleCompartmentSelectionPanelUI extends ACAQProjectWorkbenchPanel {
    private ACAQProjectCompartment compartment;

    /**
     * @param workbenchUI the workbench
     * @param compartment the compartment
     */
    public ACAQSingleCompartmentSelectionPanelUI(ACAQProjectWorkbench workbenchUI, ACAQProjectCompartment compartment) {
        super(workbenchUI);
        this.compartment = compartment;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabbedPane = new DocumentTabPane();

        ACAQParameterAccessUI parametersUI = new ACAQParameterAccessUI(getProjectWorkbench(),
                compartment,
                MarkdownDocument.fromPluginResource("documentation/compartment-graph.md"),
                true,
                true);
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

        JButton openButton = new JButton("Open in editor", UIUtils.getIconFromResources("edit.png"));
        openButton.addActionListener(e -> openInEditor());
        toolBar.add(openButton);

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("export.png"));
        exportButton.addActionListener(e -> exportCompartment());
        toolBar.add(exportButton);

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete compartment");
        deleteButton.addActionListener(e -> deleteCompartment());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void exportCompartment() {
        ACAQExportedCompartment exportedCompartment = new ACAQExportedCompartment(compartment);
        exportedCompartment.getMetadata().setName(compartment.getName());
        exportedCompartment.getMetadata().setDescription("An exported ACAQ5 compartment");
        ACAQParameterAccessUI metadataEditor = new ACAQParameterAccessUI(getProjectWorkbench(), exportedCompartment.getMetadata(),
                null,
                false,
                false);

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
