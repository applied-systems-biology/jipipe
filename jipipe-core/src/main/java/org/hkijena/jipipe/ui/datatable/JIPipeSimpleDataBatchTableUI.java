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
 *
 */

package org.hkijena.jipipe.ui.datatable;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.components.DataPreviewControlUI;
import org.hkijena.jipipe.ui.components.renderers.JIPipeComponentCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.search.SearchTextFieldTableRowFilter;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;

/**
 * Panel that displays a data batch table
 */
public class JIPipeSimpleDataBatchTableUI extends JPanel {
    private final List<JIPipeMergingDataBatch> dataBatchList;
    private JXTable table;
    private JScrollPane scrollPane;
    private SearchTextField searchTextField;
    private JIPipeSimpleDataBatchTableModel dataTableModel;

    public JIPipeSimpleDataBatchTableUI(List<JIPipeMergingDataBatch> dataBatchList) {
        this.dataBatchList = dataBatchList;
        initialize();
        reloadTable();
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public JXTable getTable() {
        return table;
    }

    public List<JIPipeMergingDataBatch> getDataBatchList() {
        return dataBatchList;
    }

    public JIPipeSimpleDataBatchTableModel getDataTable() {
        return dataTableModel;
    }

    private void initialize() {
        setLayout(new BorderLayout());

        table = new JXTable();
        if (GeneralDataSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setDefaultRenderer(Component.class, new JIPipeComponentCellRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(UIManager.getColor("TextArea.background"));
        add(scrollPane, BorderLayout.CENTER);
        add(table.getTableHeader(), BorderLayout.NORTH);

        // Toolbar for searching and export
        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);
        toolBar.setFloatable(false);

        searchTextField = new SearchTextField();
        searchTextField.addActionListener(e -> reloadTable());
        toolBar.add(searchTextField);

        DataPreviewControlUI previewControlUI = new DataPreviewControlUI();
        toolBar.add(previewControlUI);

        GeneralDataSettings.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void onPreviewSizeChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
                if (isDisplayable() && "preview-size".equals(event.getKey())) {
                    reloadTable();
                }
            }
        });
    }

    private void reloadTable() {
        dataTableModel = new JIPipeSimpleDataBatchTableModel(table, dataBatchList);
        table.setModel(dataTableModel);
        dataTableModel.setScrollPane(scrollPane);
        if (GeneralDataSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setRowFilter(new SearchTextFieldTableRowFilter(searchTextField));
        TableColumnModel columnModel = table.getColumnModel();
        table.setAutoCreateRowSorter(true);
        UIUtils.packDataTable(table);
        columnModel.getColumn(1).setPreferredWidth(GeneralDataSettings.getInstance().getPreviewSize());
        SwingUtilities.invokeLater(dataTableModel::updateRenderedPreviews);
    }

    public void resetSearch() {
        if (!StringUtils.isNullOrEmpty(searchTextField.getText())) {
            searchTextField.setText("");
        }
    }
}
