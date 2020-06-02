package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.collections.PathListParameter;
import org.hkijena.acaq5.ui.components.FileSelection;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.BorderLayout;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Editor for {@link PathListParameter}
 */
public class PathCollectionParameterEditorUI extends ACAQParameterEditorUI {

    private JList<String> listPanel;
    private FileSelection.IOMode ioMode = FileSelection.IOMode.Open;
    private FileSelection.PathMode pathMode = FileSelection.PathMode.FilesOnly;
    private JFileChooser fileChooser = new JFileChooser();

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public PathCollectionParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initializeFileSelection();
        initialize();
        reload();
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

        toolBar.add(new JLabel(getParameterAccess().getName()));

        toolBar.add(Box.createHorizontalGlue());

        JButton addButton = new JButton("Add", UIUtils.getIconFromResources("add.png"));
        addButton.addActionListener(e -> addEntry());
        toolBar.add(addButton);

        JButton removeButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        removeButton.addActionListener(e -> removeSelectedEntries());
        toolBar.add(removeButton);
    }

    private void removeSelectedEntries() {
        PathListParameter parameter = getParameterAccess().get(PathListParameter.class);
        if (parameter == null) {
            parameter = new PathListParameter();
        }
        int[] indicies = listPanel.getSelectedIndices();
        Arrays.sort(indicies);

        for (int i = indicies.length - 1; i >= 0; --i) {
            int index = indicies[i];
            parameter.remove(index);
        }
        reload();
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
        PathListParameter parameter = getParameterAccess().get(PathListParameter.class);
        if (parameter == null) {
            parameter = new PathListParameter();
            getParameterAccess().set(parameter);
        }
        if (ioMode == FileSelection.IOMode.Open) {
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                for (File selectedFile : fileChooser.getSelectedFiles()) {
                    parameter.add(selectedFile.toPath());
                }
                reload();
            }
        } else {
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                for (File selectedFile : fileChooser.getSelectedFiles()) {
                    parameter.add(selectedFile.toPath());
                }
                reload();
            }
        }
    }


    @Override
    public boolean isUILabelEnabled() {
        return false;
    }


    @Override
    public void reload() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        PathListParameter parameter = getParameterAccess().get(PathListParameter.class);
        if (parameter == null) {
            parameter = new PathListParameter();
            getParameterAccess().set(parameter);
        }
        for (Path path : parameter) {
            listModel.addElement(path.toString());
        }
        listPanel.setModel(listModel);
    }
}
