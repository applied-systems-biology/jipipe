package org.hkijena.acaq5.ui.cache;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMergedDataSlotTable;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.resultanalysis.ACAQAlgorithmTableCellRenderer;
import org.hkijena.acaq5.ui.resultanalysis.ACAQProjectCompartmentTableCellRenderer;
import org.hkijena.acaq5.ui.resultanalysis.ACAQTraitTableCellRenderer;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * UI that displays a {@link ACAQDataSlot} that is cached
 */
public class ACAQCacheMultiDataSlotTableUI extends ACAQProjectWorkbenchPanel {

    private final List<ACAQDataSlot> slots;
    private ACAQMergedDataSlotTable multiSlotTable;
    private JXTable table;
    private FormPanel rowUIList;

    /**
     * @param workbenchUI the workbench UI
     * @param slots       The slots
     */
    public ACAQCacheMultiDataSlotTableUI(ACAQProjectWorkbench workbenchUI, List<ACAQDataSlot> slots) {
        super(workbenchUI);
        this.slots = slots;
        this.multiSlotTable = new ACAQMergedDataSlotTable();
        for (ACAQDataSlot slot : slots) {
            multiSlotTable.add(getProject(), slot);
        }

        initialize();
        reloadTable();
        getProject().getCache().getEventBus().register(this);
        updateStatus();
    }

    private void reloadTable() {
        table.setModel(multiSlotTable);
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new MultiDataSlotTableColumnRenderer(multiSlotTable));
        }
        table.setAutoCreateRowSorter(true);
        table.packAll();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        table = new JXTable();
        table.setRowHeight(25);
        table.setDefaultRenderer(ACAQData.class, new ACAQDataCellRenderer());
        table.setDefaultRenderer(ACAQGraphNode.class, new ACAQAlgorithmTableCellRenderer());
        table.setDefaultRenderer(ACAQProjectCompartment.class, new ACAQProjectCompartmentTableCellRenderer());
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
        int multiRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
        ACAQDataSlot slot = multiSlotTable.getSlot(multiRow);
        int row = multiSlotTable.getRow(multiRow);
        slot.getData(row, ACAQData.class).display(slot.getAlgorithm().getName() + "/" + slot.getName() + "/" + row, getWorkbench());
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();
        for (int viewRow : selectedRows) {
            int multiRow = table.getRowSorter().convertRowIndexToModel(viewRow);
            ACAQDataSlot slot = multiSlotTable.getSlot(multiRow);
            int row = multiSlotTable.getRow(multiRow);
            String name = slot.getAlgorithm().getName() + "/" + slot.getName() + "/" + row;
            JLabel nameLabel = new JLabel(name, ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(slot));
            ACAQDataSlotRowUI ACAQDataSlotRowUI = new ACAQDataSlotRowUI(getWorkbench(), slot, row);
            rowUIList.addToForm(ACAQDataSlotRowUI, nameLabel, null);
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
        for (ACAQDataSlot slot : slots) {
            if (slot.getRowCount() == 0) {
                removeAll();
                setLayout(new BorderLayout());
                JLabel label = new JLabel("Data was cleared", UIUtils.getIconFromResources("shredder-64.png"), JLabel.LEFT);
                label.setFont(label.getFont().deriveFont(26.0f));
                add(label, BorderLayout.CENTER);

                getProject().getCache().getEventBus().unregister(this);
                multiSlotTable = null;
                return;
            }
        }
    }

    /**
     * Renders the column header
     */
    public static class MultiDataSlotTableColumnRenderer implements TableCellRenderer {
        private final ACAQMergedDataSlotTable dataTable;

        /**
         * Creates a new instance
         *
         * @param dataTable The table
         */
        public MultiDataSlotTableColumnRenderer(ACAQMergedDataSlotTable dataTable) {
            this.dataTable = dataTable;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
            int modelColumn = table.convertColumnIndexToModel(column);
            if (modelColumn < 5) {
                return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else {
                String declaration = dataTable.getTraitColumns().get(modelColumn - 5);
                String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                        UIUtils.getIconFromResources("annotation.png"),
                        declaration);
                return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
            }
        }
    }
}
