package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.settings.FileChooserSettings;
import org.hkijena.acaq5.ui.components.PathEditor;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Editor for {@link PathList}
 */
public class PathListParameterEditorUI extends ACAQParameterEditorUI {

    private JList<String> listPanel;
    private PathEditor.IOMode ioMode = PathEditor.IOMode.Open;
    private PathEditor.PathMode pathMode = PathEditor.PathMode.FilesOnly;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public PathListParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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
        PathList parameter = getParameter(PathList.class);
        int[] indicies = listPanel.getSelectedIndices();
        Arrays.sort(indicies);

        for (int i = indicies.length - 1; i >= 0; --i) {
            int index = indicies[i];
            parameter.remove(index);
        }
        reload();
    }

    private void addEntry() {
        PathList parameter = getParameter(PathList.class);
        List<Path> paths = FileChooserSettings.selectMulti(this, FileChooserSettings.KEY_PARAMETER, "Add path", ioMode, pathMode);
        parameter.addAll(paths);
        getParameterAccess().set(parameter);
    }


    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        PathList parameter = getParameter(PathList.class);
        for (Path path : parameter) {
            listModel.addElement(path.toString());
        }
        listPanel.setModel(listModel);
    }
}
