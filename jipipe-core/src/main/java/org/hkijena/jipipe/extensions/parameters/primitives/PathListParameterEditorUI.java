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

package org.hkijena.jipipe.extensions.parameters.primitives;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.renderers.PathListCellRenderer;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Editor for {@link PathList}
 */
public class PathListParameterEditorUI extends JIPipeParameterEditorUI {

    private final JLabel emptyLabel = new JLabel("<html><strong>This list is empty</strong><br/>Click 'Add' to add items.</html>",
            UIUtils.getIcon32FromResources("info.png"), JLabel.LEFT);
    private JList<Path> listPanel;
    private PathIOMode ioMode = PathIOMode.Open;
    private PathType pathMode = PathType.FilesOnly;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public PathListParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initializeFileSelection();
        initialize();
        reload();
    }

    private void initializeFileSelection() {
        PathParameterSettings settings = getParameterAccess().getAnnotationOfType(PathParameterSettings.class);
        if (settings != null) {
            ioMode = settings.ioMode();
            pathMode = settings.pathMode();
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2));
        listPanel = new JList<>();
        listPanel.setCellRenderer(new PathListCellRenderer());
        listPanel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        add(listPanel, BorderLayout.CENTER);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(emptyLabel, BorderLayout.SOUTH);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        toolBar.add(Box.createHorizontalStrut(4));
        toolBar.add(new JLabel(getParameterAccess().getName()));
        toolBar.add(Box.createHorizontalGlue());

        JButton addButton = new JButton("Add", UIUtils.getIconFromResources("actions/list-add.png"));
        addButton.addActionListener(e -> addEntry());
        toolBar.add(addButton);

        JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/delete.png"));
        removeButton.setToolTipText("Remove selected elements");
        removeButton.addActionListener(e -> removeSelectedEntries());
        toolBar.add(removeButton);

        JButton menuButton = new JButton(UIUtils.getIconFromResources("actions/open-menu.png"));
        menuButton.setToolTipText("Show additional options");
        JPopupMenu menu = UIUtils.addPopupMenuToComponent(menuButton);
        toolBar.add(menuButton);

        JMenuItem clearItem = new JMenuItem("Clear", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearItem.setToolTipText("Removes all items");
        clearItem.addActionListener(e -> clearList());
        menu.add(clearItem);

        JMenuItem sortItem = new JMenuItem("Sort", UIUtils.getIconFromResources("actions/view-sort.png"));
        sortItem.setToolTipText("Sorts the items");
        sortItem.addActionListener(e -> sortList());
        menu.add(sortItem);
    }

    private void sortList() {
        Object order = JOptionPane.showInputDialog(this, "Please select the sort order", "Sort items",
                JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Ascending", "Descending"}, "Ascending");
        if (Objects.equals("Ascending", order)) {
            PathList parameter = getParameter(PathList.class);
            parameter.sort(Comparator.naturalOrder());
            setParameter(parameter, true);
        } else if (Objects.equals("Descending", order)) {
            PathList parameter = getParameter(PathList.class);
            parameter.sort(Comparator.reverseOrder());
            setParameter(parameter, true);
        }
    }

    private void clearList() {
        PathList parameter = getParameter(PathList.class);
        parameter.clear();
        setParameter(parameter, true);
    }

    private void removeSelectedEntries() {
        PathList parameter = getParameter(PathList.class);
        int[] indicies = listPanel.getSelectedIndices();
        Arrays.sort(indicies);

        for (int i = indicies.length - 1; i >= 0; --i) {
            int index = indicies[i];
            parameter.remove(index);
        }

        setParameter(parameter, true);
    }

    private void addEntry() {
        PathList parameter = getParameter(PathList.class);
        FileChooserSettings.LastDirectoryKey key = FileChooserSettings.LastDirectoryKey.Parameters;
        PathParameterSettings annotation = getParameterAccess().getAnnotationOfType(PathParameterSettings.class);
        if (annotation != null) {
            key = annotation.key();
        }
        List<Path> paths = FileChooserSettings.selectMulti(this, key, "Add path", ioMode, pathMode);
        parameter.addAll(paths);
        setParameter(parameter, true);
    }


    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        DefaultListModel<Path> listModel = new DefaultListModel<>();
        PathList parameter = getParameter(PathList.class);
        for (Path path : parameter) {
            listModel.addElement(path);
        }
        listPanel.setModel(listModel);
        emptyLabel.setVisible(parameter.isEmpty());
    }
}
