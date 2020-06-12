package org.hkijena.acaq5.ui.cache;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.hkijena.acaq5.ui.resultanalysis.*;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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
        table.setDefaultRenderer(ACAQData.class, new DataCellRenderer());
        table.setDefaultRenderer(ACAQTrait.class, new ACAQTraitTableCellRenderer());
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
            RowUI rowUI =new RowUI(getWorkbench(), slot, row);
            rowUIList.addToForm(rowUI, nameLabel, null);
        }
    }

    /**
     * Triggered when the cache was updated
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(ACAQProjectCache.ModifiedEvent event) {
        updateStatus();
    }

    private void updateStatus() {
        if(slot.getRowCount() == 0) {
            removeAll();
            setLayout(new BorderLayout());
            JLabel label = new JLabel("Data was cleared", UIUtils.getIconFromResources("shredder-64.png"), JLabel.LEFT);
            label.setFont(label.getFont().deriveFont(26.0f));
            add(label, BorderLayout.CENTER);

            getProject().getCache().getEventBus().unregister(this);
        }
    }

    /**
     * UI for a row
     */
    public static class RowUI extends ACAQWorkbenchPanel {
        private final ACAQDataSlot slot;
        private final int row;

        /**
         * Creates a new instance
         * @param workbench the workbench
         * @param slot the slot
         * @param row the row
         */
        public RowUI(ACAQWorkbench workbench, ACAQDataSlot slot, int row) {
            super(workbench);
            this.slot = slot;
            this.row = row;

            this.initialize();
        }

        private void initialize() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(Box.createHorizontalGlue());

            JButton copyButton = new JButton("Copy string", UIUtils.getIconFromResources("copy.png"));
            copyButton.setToolTipText("Copies the string representation");
            copyButton.addActionListener(e -> copyString());
            add(copyButton);

            JButton displayButton = new JButton("Show", UIUtils.getIconFromResources("search.png"));
            displayButton.setToolTipText("Shows the item");
            displayButton.addActionListener(e -> slot.getData(row, ACAQData.class).display(slot.getAlgorithm().getName() + "/" + slot.getName() + "/" + row,
                    getWorkbench()));
            add(displayButton);
        }

        private void copyString() {
            String string = "" + slot.getData(row, ACAQData.class);
            StringSelection selection = new StringSelection(string);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        }
    }

    /**
     * Wraps around a {@link ACAQDataSlot} to display a "toString" column
     */
    public static class WrapperTableModel implements TableModel {

        private final ACAQDataSlot slot;

        /**
         * Creates a new instance
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
          if(columnIndex == 0)
              return slot.getColumnName(0);
          else if(columnIndex == 1)
              return "String representation";
          else {
              return slot.getColumnName(columnIndex - 1);
          }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if(columnIndex == 0)
                return slot.getColumnClass(0);
            else if(columnIndex == 1)
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
            if(columnIndex == 0)
                return slot.getValueAt(rowIndex, 0);
            else if(columnIndex == 1)
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
     * Renders {@link ACAQData}
     */
    public static class DataCellRenderer extends JLabel implements TableCellRenderer {

        /**
         * Create new instance
         */
        public DataCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if(value instanceof ACAQData) {
                ACAQData data = (ACAQData) value;
                setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(data.getClass()));
                setText(ACAQData.getNameOf(data.getClass()));
            }
            if (isSelected) {
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }
            return this;
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
                ACAQTraitDeclaration declaration = dataTable.getAnnotationColumns().get(modelColumn - 2);
                String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                        ACAQUITraitRegistry.getInstance().getIconURLFor(declaration).toString(),
                        declaration.getName());
                return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
            }
        }
    }
}
