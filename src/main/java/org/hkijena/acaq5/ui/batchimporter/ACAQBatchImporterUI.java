package org.hkijena.acaq5.ui.batchimporter;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.batchimporter.ACAQBatchImporter;
import org.hkijena.acaq5.api.batchimporter.algorithms.ACAQSubfoldersAsSamples;
import org.hkijena.acaq5.api.batchimporter.datasources.ACAQFolderDataSource;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

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
        ACAQAlgorithmRegistry algorithmRegistry = ACAQRegistryService.getInstance().getAlgorithmRegistry();
        ACAQFolderDataSource source = (ACAQFolderDataSource) algorithmRegistry.getDefaultDeclarationFor(ACAQFolderDataSource.class).newInstance();
        ACAQSubfoldersAsSamples sampleGenerator = (ACAQSubfoldersAsSamples) algorithmRegistry.getDefaultDeclarationFor(ACAQSubfoldersAsSamples.class).newInstance();

        batchImporter.getGraph().insertNode(source);
        batchImporter.getGraph().insertNode(sampleGenerator);

        batchImporter.getGraph().connect(source.getOutputSlot(), sampleGenerator.getInputSlots().get(0));
    }

    private void initialize() {
        setLayout(new BorderLayout());
        graphUI = new ACAQAlgorithmGraphUI(getWorkbenchUI(), batchImporter.getGraph());
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

        JButton runButton = new JButton("Import", UIUtils.getIconFromResources("import.png"));
        runButton.addActionListener(e -> runImport());
        toolBar.add(runButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void runImport() {

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
                batchImporter.loadFrom(fileChooser.getSelectedFile().toPath());
                getWorkbenchUI().sendStatusBarText("Loaded importer from " + fileChooser.getSelectedFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
