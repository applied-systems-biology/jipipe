/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.datatable;

import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.components.DataPreviewControlUI;
import org.hkijena.jipipe.ui.components.renderers.JIPipeComponentCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.search.SearchTextFieldTableRowFilter;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.WeakStore;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel that displays a data batch table
 */
public class JIPipeSimpleDataBatchTableUI extends JPanel implements JIPipeParameterCollection.ParameterChangedEventListener {
    private List<JIPipeMultiIterationStep> iterationStepList;
    private JXTable table;
    private JScrollPane scrollPane;
    private SearchTextField searchTextField;
    private JIPipeSimpleDataBatchTableModel dataTableModel;

    public JIPipeSimpleDataBatchTableUI(List<JIPipeMultiIterationStep> iterationStepList) {
        this.iterationStepList = iterationStepList;
        initialize();
        reloadTable();
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public JXTable getTable() {
        return table;
    }

    public List<JIPipeMultiIterationStep> getDataBatchList() {
        return iterationStepList;
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

        GeneralDataSettings.getInstance().getParameterChangedEventEmitter().subscribeWeak(this);
    }

    private void reloadTable() {
        dataTableModel = new JIPipeSimpleDataBatchTableModel(table, iterationStepList, WeakStore.class);
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

    public void dispose() {
        try {
            iterationStepList = new ArrayList<>();
            dataTableModel = null;
            table.setModel(new DefaultTableModel());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (isDisplayable() && "preview-size".equals(event.getKey())) {
            reloadTable();
        }
    }
}
