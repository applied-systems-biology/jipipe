package org.hkijena.acaq5.ui.cache;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.resultanalysis.ACAQTraitTableCellRenderer;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
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

/**
 * UI that displays a {@link ACAQDataSlot} that is cached
 */
public class ACAQCacheDataSlotTableUI extends ACAQProjectWorkbenchPanel {

    private final ACAQDataSlot slot;
    private JXTable table;
    private FormPanel rowUIList;

    /**
     * @param workbenchUI the workbench UI
     * @param slot        The slot
     */
    public ACAQCacheDataSlotTableUI(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot) {
        super(workbenchUI);
        this.slot = slot;

        initialize();
        reloadTable();
        getProject().getCache().getEventBus().register(this);
        updateStatus();
    }

    private void reloadTable() {
        table.setModel(new WrapperTableModel(slot));
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
        table.setRowHeight(25);
        table.setDefaultRenderer(ACAQData.class, new ACAQDataCellRenderer());
        table.setDefaultRenderer(ACAQAnnotation.class, new ACAQTraitTableCellRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);
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
    }

    private void handleSlotRowDefaultAction(int selectedRow) {
        int row = table.getRowSorter().convertRowIndexToModel(selectedRow);
        slot.getData(row, ACAQData.class).display(slot.getAlgorithm().getName() + "/" + slot.getName() + "/" + row, getWorkbench());
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();
        for (int viewRow : selectedRows) {
            int row = table.getRowSorter().convertRowIndexToModel(viewRow);
            String name = slot.getAlgorithm().getName() + "/" + slot.getName() + "/" + row;
            JLabel nameLabel = new JLabel(name, ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(slot));
            ACAQDataSlotRowUI rowUI = new ACAQDataSlotRowUI(getWorkbench(), slot, row);
            rowUIList.addToForm(rowUI, nameLabel, null);
        }
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(ACAQProjectCache.ModifiedEvent event) {
        updateStatus();
    }

    private void updateStatus() {
        if (slot.getRowCount() == 0) {
            removeAll();
            setLayout(new BorderLayout());
            JLabel label = new JLabel("Data was cleared", UIUtils.getIconFromResources("shredder-64.png"), JLabel.LEFT);
            label.setFont(label.getFont().deriveFont(26.0f));
            add(label, BorderLayout.CENTER);

            getProject().getCache().getEventBus().unregister(this);
        }
    }


    /**
     * Wraps around a {@link ACAQDataSlot} to display a "toString" column
     */
    public static class WrapperTableModel implements TableModel {

        private final ACAQDataSlot slot;

        /**
         * Creates a new instance
         *
         * @param slot the wrapped slot
         */
        public WrapperTableModel(ACAQDataSlot slot) {
            this.slot = slot;
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
                return "String representation";
            else {
                return slot.getColumnName(columnIndex - 1);
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return slot.getColumnClass(0);
            else if (columnIndex == 1)
                return String.class;
            else {
                return slot.getColumnClass(columnIndex - 1);
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
            else if (columnIndex == 1)
                return "" + slot.getData(rowIndex, ACAQData.class);
            else {
                return slot.getValueAt(rowIndex, columnIndex - 1);
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
        private final ACAQDataSlot dataTable;

        /**
         * Creates a new instance
         *
         * @param dataTable The table
         */
        public WrapperColumnHeaderRenderer(ACAQDataSlot dataTable) {
            this.dataTable = dataTable;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
            int modelColumn = table.convertColumnIndexToModel(column);
            if (modelColumn < 2) {
                return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else {
                String declaration = dataTable.getAnnotationColumns().get(modelColumn - 2);
                String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                        UIUtils.getIconFromResources("annotation.png"),
                        declaration);
                return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
            }
        }
    }
}
