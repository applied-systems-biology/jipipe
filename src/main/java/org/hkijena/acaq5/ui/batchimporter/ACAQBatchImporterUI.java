package org.hkijena.acaq5.ui.batchimporter;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.batchimporter.ACAQBatchImporter;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.ConfirmingButton;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Set;

public class ACAQBatchImporterUI extends ACAQUIPanel {

    private ACAQBatchImporter batchImporter;
    private ACAQAlgorithmGraphUI graphUI;

    public ACAQBatchImporterUI(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI);
        batchImporter = new ACAQBatchImporter(getProject());
        initialize();
        initializeGraphWithDefaults();
    }

    private void initializeGraphWithDefaults() {
//        ACAQAlgorithmRegistry algorithmRegistry = ACAQRegistryService.getInstance().getAlgorithmRegistry();
//        ACAQFolderDataSource source = (ACAQFolderDataSource) algorithmRegistry.getDefaultDeclarationFor(ACAQFolderDataSource.class).newInstance();
//        ACAQSubfoldersAsSamples sampleGenerator = (ACAQSubfoldersAsSamples) algorithmRegistry.getDefaultDeclarationFor(ACAQSubfoldersAsSamples.class).newInstance();
//
//        batchImporter.getGraph().insertNode(source);
//        batchImporter.getGraph().insertNode(sampleGenerator);
//
//        batchImporter.getGraph().connect(source.getOutputSlot(), sampleGenerator.getInputSlots().get(0));
    }

    private void initialize() {
        setLayout(new BorderLayout());
        graphUI = new ACAQAlgorithmGraphUI(getWorkbenchUI(), batchImporter.getGraph(), ACAQAlgorithmGraph.COMPARTMENT_BATCHIMPORTER);
        add(graphUI, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton openButton = new JButton("Open importer", UIUtils.getIconFromResources("open.png"));
        openButton.addActionListener(e -> open());
        toolBar.add(openButton);

        JButton saveButton = new JButton("Save importer", UIUtils.getIconFromResources("save.png"));
        saveButton.addActionListener(e -> save());
        toolBar.add(saveButton);

        toolBar.add(Box.createHorizontalGlue());

        ConfirmingButton runButton = new ConfirmingButton("Import", UIUtils.getIconFromResources("import.png"));
        runButton.addActionListener(e -> runImport());
        toolBar.add(runButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void runImport() {
        ACAQValidityReport report = new ACAQValidityReport();
        batchImporter.reportValidity(report);
        if(!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report);
            return;
        }

        Set<String> conflictingSamples = batchImporter.getConflictingSamples();

        if(!conflictingSamples.isEmpty()) {

            StringBuilder builder = new StringBuilder();
            builder.append("<html>");
            builder.append("The batch importer detected following samples that already exist:<br/><br/>");
            for (String conflictingSample : conflictingSamples) {
                builder.append("<i>").append(conflictingSample).append("</i><br/>");
            }
            builder.append("<br/><br/>");
            builder.append("You can either skip already existing samples, overwrite them, or rename new samples and leave existing ones untouched.");
            builder.append("<br>What should be done?");
            builder.append("</html>");

            int option = JOptionPane.showOptionDialog(this,
                    builder.toString(),
                    "Batch-import",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Skip", "Overwrite", "Rename", "Cancel"},
                    "Cancel");
            switch(option) {
                case 0:
                    batchImporter.setConflictResolution(ACAQBatchImporter.ConflictResolution.Skip);
                    break;
                case 1:
                    batchImporter.setConflictResolution(ACAQBatchImporter.ConflictResolution.Overwrite);
                    break;
                case 2:
                    batchImporter.setConflictResolution(ACAQBatchImporter.ConflictResolution.Rename);
                    break;
                default:
                    return;
            }
        }

        try {
            batchImporter.run(s -> {}, () -> false);
            getWorkbenchUI().sendStatusBarText("Imported " + batchImporter.getLastImportedSamples().size() + " samples via batch-importer");
        }
        catch (Exception e) {
            UIUtils.openErrorDialog(this, e);
        }
    }

    private void save() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Save importer configuration (*.json");
        if(fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                batchImporter.saveAs(fileChooser.getSelectedFile().toPath());
                getWorkbenchUI().sendStatusBarText("Saved importer to " + fileChooser.getSelectedFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void open() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Open importer configuration (*.json");
        if(fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                batchImporter = ACAQBatchImporter.loadFromFile(fileChooser.getSelectedFile().toPath(), getProject());
                remove(graphUI);
                graphUI = new ACAQAlgorithmGraphUI(getWorkbenchUI(), batchImporter.getGraph(), ACAQAlgorithmGraph.COMPARTMENT_BATCHIMPORTER);
                add(graphUI, BorderLayout.CENTER);
                revalidate();
                repaint();
                getWorkbenchUI().sendStatusBarText("Loaded importer from " + fileChooser.getSelectedFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
