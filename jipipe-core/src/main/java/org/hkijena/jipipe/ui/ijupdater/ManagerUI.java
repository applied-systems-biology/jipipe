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
import net.imagej.updater.FilesCollection;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.ui.components.SearchTextFieldTableRowFilter;
import org.hkijena.jipipe.utils.CustomScrollPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class ManagerUI extends JIPipeWorkbenchPanel {

    private final JIPipeImageJPluginManager pluginManager;
    private FilesCollection filesCollection;
    private JXTable table;
    private ViewOptions.Option currentViewOption = ViewOptions.Option.UPDATEABLE;
    private SearchTextField searchTextField;
    private JPanel optionPanel;

    /**
     * @param workbench the workbench
     * @param pluginManager the plugin manager
     */
    public ManagerUI(JIPipeWorkbench workbench, JIPipeImageJPluginManager pluginManager) {
        super(workbench);
        this.pluginManager = pluginManager;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        initializeToolbar(toolBar);
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout());
        table = new JXTable(new DefaultTableModel());
        table.setRowFilter(new SearchTextFieldTableRowFilter(searchTextField));
        table.setRowHeight(25);
        CustomScrollPane scrollPane = new CustomScrollPane(table);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);

        optionPanel = new JPanel(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                tablePanel,
                optionPanel);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.66);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });
        add(splitPane, BorderLayout.CENTER);
    }

    private void initializeToolbar(JToolBar toolBar) {
        searchTextField = new SearchTextField();
        searchTextField.addActionListener(e -> refreshTable());
        toolBar.add(searchTextField);

        ButtonGroup viewOptionGroup = new ButtonGroup();
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.ALL, UIUtils.getIconFromResources("glob.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.MANAGED, UIUtils.getIconFromResources("cloud.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.OTHERS, UIUtils.getIconFromResources("cloud-crossed.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.LOCALLY_MODIFIED, UIUtils.getIconFromResources("edit.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.INSTALLED, UIUtils.getIconFromResources("checkmark.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.UNINSTALLED, UIUtils.getIconFromResources("remove.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.CHANGES, UIUtils.getIconFromResources("wrench.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.UPTODATE, UIUtils.getIconFromResources("checkbox-green.png"));
        addViewOptionButton(toolBar, viewOptionGroup, ViewOptions.Option.UPDATEABLE, UIUtils.getIconFromResources("checkbox-orange.png"));
    }

    private void addViewOptionButton(JToolBar toolBar, ButtonGroup viewOptionGroup, ViewOptions.Option option, Icon icon) {
        JToggleButton button = new JToggleButton(icon, option == currentViewOption);
        button.addActionListener(e -> {
            System.out.println(button);
            if(button.isSelected()) {
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
        if(filesCollection != null) {
            table.setModel(new FileTableModel(filesCollection));
            table.setRowFilter(new FileTableModelFilter(searchTextField, currentViewOption));
        }
        else {
            table.setModel(new DefaultTableModel());
        }
    }

    public void setOptionPanelContent(Component content) {
        optionPanel.removeAll();
        optionPanel.add(content, BorderLayout.CENTER);
        optionPanel.revalidate();
        optionPanel.repaint();
    }
}
