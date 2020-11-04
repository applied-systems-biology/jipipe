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

package org.hkijena.jipipe.ui.cache;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.JIPipeCachedDataPreview;
import org.hkijena.jipipe.ui.components.JIPipeComponentCellRenderer;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.ui.components.SearchTextFieldTableRowFilter;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeTraitTableCellRenderer;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * UI that displays a {@link JIPipeDataSlot} that is cached
 */
public class JIPipeCacheDataSlotTableUI extends JIPipeProjectWorkbenchPanel {

    private final JIPipeDataSlot slot;
    private JXTable table;
    private FormPanel rowUIList;
    private SearchTextField searchTextField = new SearchTextField();
    private WrapperTableModel dataTable;

    /**
     * @param workbenchUI the workbench UI
     * @param slot        The slot
     */
    public JIPipeCacheDataSlotTableUI(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot) {
        super(workbenchUI);
        this.slot = slot;

        initialize();
        reloadTable();
        getProject().getCache().getEventBus().register(this);
        updateStatus();
    }

    private void reloadTable() {
        dataTable = new WrapperTableModel(table, slot);
        table.setModel(dataTable);
        table.setRowFilter(new SearchTextFieldTableRowFilter(searchTextField));
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new WrapperColumnHeaderRenderer(slot));
        }
        table.setAutoCreateRowSorter(true);
        table.packAll();

    }

    private void initialize() {
        setLayout(new BorderLayout());
        table = new JXTable();
        if (GeneralDataSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setDefaultRenderer(JIPipeData.class, new JIPipeDataCellRenderer());
        table.setDefaultRenderer(Component.class, new JIPipeComponentCellRenderer());
        table.setDefaultRenderer(JIPipeAnnotation.class, new JIPipeTraitTableCellRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(UIManager.getColor("TextArea.background"));
        add(scrollPane, BorderLayout.CENTER);
        add(table.getTableHeader(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            showDataRows(table.getSelectedRows());
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int[] selectedRows = table.getSelectedRows();
                    if (selectedRows.length > 0)
                        handleSlotRowDefaultAction(selectedRows[0]);
                }
            }
        });

        rowUIList = new FormPanel(null, ParameterPanel.WITH_SCROLLING);
        add(rowUIList, BorderLayout.SOUTH);

        // Toolbar for searching and export
        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);
        toolBar.setFloatable(false);

        searchTextField.addActionListener(e -> reloadTable());
        toolBar.add(searchTextField);

        JButton exportButton = new JButton(UIUtils.getIconFromResources("actions/document-export.png"));
        toolBar.add(exportButton);
        exportButton.setToolTipText("Export");
        JPopupMenu exportMenu = UIUtils.addPopupMenuToComponent(exportButton);

        JMenuItem exportAsCsvItem = new JMenuItem("as *.csv", UIUtils.getIconFromResources("data-types/results-table.png"));
        exportAsCsvItem.addActionListener(e -> exportAsCSV());
        exportMenu.add(exportAsCsvItem);
    }

    private void exportAsCSV() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Export as *.csv", UIUtils.EXTENSION_FILTER_CSV);
        if (path != null) {
            ResultsTableData tableData = ResultsTableData.fromTableModel(dataTable);
            tableData.saveAsCSV(path);
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow) {
        int row = table.getRowSorter().convertRowIndexToModel(selectedRow);
//        slot.getData(row, JIPipeData.class).display(slot.getNode().getName() + "/" + slot.getName() + "/" + row, getWorkbench());
        JIPipeDataSlotRowUI rowUI = new JIPipeDataSlotRowUI(getWorkbench(), slot, row);
        rowUI.handleDefaultAction();
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();
        for (int viewRow : selectedRows) {
            int row = table.getRowSorter().convertRowIndexToModel(viewRow);
            String name = slot.getNode().getName() + "/" + slot.getName() + "/" + row;
            JLabel nameLabel = new JLabel(name, JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(slot));
            JIPipeDataSlotRowUI rowUI = new JIPipeDataSlotRowUI(getWorkbench(), slot, row);
            rowUIList.addToForm(rowUI, nameLabel, null);
        }
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(JIPipeProjectCache.ModifiedEvent event) {
        updateStatus();
    }

    private void updateStatus() {
        if (slot.getRowCount() == 0) {
            removeAll();
            setLayout(new BorderLayout());
            JLabel label = new JLabel("No data available", UIUtils.getIcon64FromResources("no-data.png"), JLabel.LEFT);
            label.setFont(label.getFont().deriveFont(26.0f));
            add(label, BorderLayout.CENTER);

            getProject().getCache().getEventBus().unregister(this);
        }
    }


    /**
     * Wraps around a {@link JIPipeDataSlot} to display a "toString" column
     */
    public static class WrapperTableModel implements TableModel {

        private final JTable table;
        private final JIPipeDataSlot slot;
        private List<Component> previewCache = new ArrayList<>();

        /**
         * Creates a new instance
         *
         * @param table the table
         * @param slot  the wrapped slot
         */
        public WrapperTableModel(JTable table, JIPipeDataSlot slot) {
            this.table = table;
            this.slot = slot;
            for (int i = 0; i < slot.getRowCount(); i++) {
                previewCache.add(null);
            }
        }

        @Override
        public int getRowCount() {
            return slot.getRowCount();
        }

        @Override
        public int getColumnCount() {
            return slot.getColumnCount() + 1;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0)
                return slot.getColumnName(0);
            else if (columnIndex == 1)
                return "Preview";
            else if (columnIndex == 2)
                return "String representation";
            else {
                return slot.getColumnName(columnIndex - 2);
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return slot.getColumnClass(0);
            else if (columnIndex == 1)
                return Component.class;
            else if (columnIndex == 2)
                return String.class;
            else {
                return slot.getColumnClass(columnIndex - 2);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0)
                return slot.getValueAt(rowIndex, 0);
            else if (columnIndex == 1) {
                Component preview = previewCache.get(rowIndex);
                if (preview == null) {
                    if (GeneralDataSettings.getInstance().isGenerateCachePreviews()) {
                        JIPipeData data = slot.getData(rowIndex, JIPipeData.class);
                        preview = new JIPipeCachedDataPreview(table, data);
                        previewCache.set(rowIndex, preview);
                    } else {
                        preview = new JLabel("N/A");
                        previewCache.set(rowIndex, preview);
                    }
                }
                return preview;
            } else if (columnIndex == 2)
                return "" + slot.getData(rowIndex, JIPipeData.class);
            else {
                return slot.getValueAt(rowIndex, columnIndex - 2);
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        }

        @Override
        public void addTableModelListener(TableModelListener l) {

        }

        @Override
        public void removeTableModelListener(TableModelListener l) {

        }
    }

    /**
     * Renders the column header of {@link WrapperTableModel}
     */
    public static class WrapperColumnHeaderRenderer implements TableCellRenderer {
        private final JIPipeDataSlot dataTable;

        /**
         * Creates a new instance
         *
         * @param dataTable The table
         */
        public WrapperColumnHeaderRenderer(JIPipeDataSlot dataTable) {
            this.dataTable = dataTable;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
            int modelColumn = table.convertColumnIndexToModel(column);
            if (modelColumn < 3) {
                return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else {
                String info = dataTable.getAnnotationColumns().get(modelColumn - 3);
                String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                        UIUtils.getIconFromResources("data-types/annotation.png"),
                        info);
                return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
            }
        }
    }
}
