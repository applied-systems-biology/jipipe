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

package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.ui.swing.updater.ViewOptions;
import net.imagej.updater.FileObject;
import net.imagej.updater.FilesCollection;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.search.SearchTextFieldTableRowFilter;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class ManagerUI extends JIPipeWorkbenchPanel {

    private final JIPipeImageJPluginManager pluginManager;
    JToolBar toolBar = new JToolBar();
    private FilesCollection filesCollection;
    private JXTable table;
    private ViewOptions.Option currentViewOption = ViewOptions.Option.UPDATEABLE;
    private SearchTextField searchTextField;
    private JPanel tablePanel = new JPanel(new BorderLayout());
    private JPanel optionPanel;

    /**
     * @param workbench     the workbench
     * @param pluginManager the plugin manager
     */
    public ManagerUI(JIPipeWorkbench workbench, JIPipeImageJPluginManager pluginManager) {
        super(workbench);
        this.pluginManager = pluginManager;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        initializeToolbar(toolBar);
        toolBar.setFloatable(false);

        table = new JXTable(new DefaultTableModel());
        table.setRowFilter(new SearchTextFieldTableRowFilter(searchTextField));
        table.setRowHeight(25);
        table.getSelectionModel().addListSelectionListener(e -> showSelectedRows(table.getSelectedRows()));
        JScrollPane scrollPane = new JScrollPane(table);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
        optionPanel = new JPanel(new BorderLayout());

        setOptionPanelContent(null);
    }

    private void showSelectedRows(int[] viewRows) {
        if (pluginManager.isCurrentlyRunning())
            return;
        if (table.getModel() instanceof FileTableModel) {
            int[] modelRows = new int[viewRows.length];
            for (int i = 0; i < viewRows.length; i++) {
                modelRows[i] = table.convertRowIndexToModel(viewRows[i]);
            }

            if (modelRows.length == 1) {
                FileObject selected = ((FileTableModel) table.getModel()).rowToFile.get(modelRows[0]);
                SingleFileSelectionPanel panel = new SingleFileSelectionPanel(getWorkbench(), this, selected);
                setOptionPanelContent(panel);
            } else if (modelRows.length > 1) {
                Set<FileObject> selected = new HashSet<>();
                for (int row : modelRows) {
                    selected.add(((FileTableModel) table.getModel()).rowToFile.get(row));
                }
                MultiFileSelectionPanel panel = new MultiFileSelectionPanel(getWorkbench(), this, selected);
                setOptionPanelContent(panel);
            }
        }
    }

    private void initializeToolbar(JToolBar toolBar) {
        searchTextField = new SearchTextField();
        searchTextField.addActionListener(e -> refreshTable());
        toolBar.add(searchTextField);

        ButtonGroup viewOptionGroup = new ButtonGroup();
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.ALL, UIUtils.getIconFromResources("status/package-all.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.MANAGED, UIUtils.getIconFromResources("status/package-supported-2.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.OTHERS, UIUtils.getIconFromResources("status/package-unknown.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.LOCALLY_MODIFIED, UIUtils.getIconFromResources("status/package-unknown-changed.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.INSTALLED, UIUtils.getIconFromResources("status/package-install.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.UNINSTALLED, UIUtils.getIconFromResources("status/package-purge.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.CHANGES, UIUtils.getIconFromResources("status/package-new.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.UPTODATE, UIUtils.getIconFromResources("status/package-installed-updated.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.UPDATEABLE, UIUtils.getIconFromResources("status/package-installed-outdated.png"));
    }

    public void fireFileChanged(final FileObject file) {
        if (table.getModel() instanceof FileTableModel) {
            ((FileTableModel) table.getModel()).fireFileChanged(file);
        }
    }

    private void addViewOptionButton(JToolBar toolBar, ButtonGroup viewOptionGroup, ViewOptions.Option option, Icon icon) {
        JToggleButton button = new JToggleButton(icon, option == currentViewOption);
        button.addActionListener(e -> {
            if (button.isSelected()) {
                currentViewOption = option;
                refreshTable();
            }
        });
        UIUtils.makeFlat25x25(button);
        button.setToolTipText(option.toString());
        viewOptionGroup.add(button);
        toolBar.add(button);
    }

    public FilesCollection getFilesCollection() {
        return filesCollection;
    }

    public void setFilesCollection(FilesCollection filesCollection) {
        this.filesCollection = filesCollection;
        refreshTable();
    }

    public void refreshTable() {
        if (filesCollection != null) {
            table.setModel(new FileTableModel(filesCollection));
            table.setRowFilter(new FileTableModelFilter(searchTextField, currentViewOption));
        } else {
            table.setModel(new DefaultTableModel());
        }
    }

    public void setMainPanelContent(Component content) {
        removeAll();
        if (content != null) {
            add(toolBar, BorderLayout.NORTH);
            add(content, BorderLayout.CENTER);
            revalidate();
            repaint();
        } else {
            setOptionPanelContent(null);
        }
    }

    public void setOptionPanelContent(Component content) {
        removeAll();
        if (content == null) {
            add(toolBar, BorderLayout.NORTH);
            add(tablePanel, BorderLayout.CENTER);
        } else {
            add(toolBar, BorderLayout.NORTH);
            optionPanel.removeAll();
            optionPanel.add(content, BorderLayout.CENTER);
            JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    tablePanel,
                    optionPanel, AutoResizeSplitPane.RATIO_3_TO_1);
            add(splitPane, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }
}
