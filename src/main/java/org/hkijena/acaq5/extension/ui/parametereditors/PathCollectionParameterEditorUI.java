package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.api.parameters.CollectionParameter;
import org.hkijena.acaq5.api.parameters.PathCollectionParameter;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.FileSelection;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParametertypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.Arrays;

public class PathCollectionParameterEditorUI extends ACAQParameterEditorUI {

    private JList<String> listPanel;
    private FileSelection.IOMode ioMode = FileSelection.IOMode.Open;
    private FileSelection.PathMode pathMode = FileSelection.PathMode.FilesOnly;
    private JFileChooser fileChooser = new JFileChooser();

    public PathCollectionParameterEditorUI(ACAQWorkbenchUI workbenchUI, ACAQParameterAccess parameterAccess) {
        super(workbenchUI, parameterAccess);
        initializeFileSelection();
        initialize();
        refreshList();
    }

    private void initializeFileSelection() {
        FilePathParameterSettings settings = getParameterAccess().getAnnotationOfType(FilePathParameterSettings.class);
        if (settings != null) {
            ioMode = settings.ioMode();
            pathMode = settings.pathMode();
        }
        fileChooser.setMultiSelectionEnabled(true);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        listPanel = new JList<>();
        listPanel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        add(listPanel, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        JButton addButton = new JButton("Add", UIUtils.getIconFromResources("add.png"));
        addButton.addActionListener(e -> addEntry());
        toolBar.add(addButton);

        JButton removeButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        removeButton.addActionListener(e -> removeSelectedEntries());
        toolBar.add(removeButton);
    }

    private void removeSelectedEntries() {
        PathCollectionParameter parameter = getParameterAccess().get();
        int[] indicies = listPanel.getSelectedIndices();
        Arrays.sort(indicies);

        for(int i = indicies.length - 1; i >= 0; --i) {
            int index = indicies[i];
            parameter.remove(index);
        }
        refreshList();
    }

    private void addEntry() {
        switch (pathMode) {
            case FilesOnly:
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                break;
            case DirectoriesOnly:
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                break;
            case FilesAndDirectories:
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                break;
        }
        PathCollectionParameter parameter = getParameterAccess().get();
        if (ioMode == FileSelection.IOMode.Open) {
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                for (File selectedFile : fileChooser.getSelectedFiles()) {
                    parameter.add(selectedFile.toPath());
                }
                refreshList();
            }
        } else {
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                for (File selectedFile : fileChooser.getSelectedFiles()) {
                    parameter.add(selectedFile.toPath());
                }
                refreshList();
            }
        }
    }


    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    private void refreshList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        PathCollectionParameter parameter = getParameterAccess().get();
        for (Path path : parameter) {
            listModel.addElement(path.toString());
        }
        listPanel.setModel(listModel);
    }
}
