package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.api.data.ACAQMergedExportedDataTable;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.ACAQProjectUIPanel;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;

/**
 * Displays the result of multiple {@link ACAQDataSlot}
 */
public class ACAQMultipleResultDataSlotTableUI extends ACAQProjectUIPanel {

    private final List<ACAQDataSlot> slots;
    private ACAQRun run;
    private JXTable table;
    private ACAQMergedExportedDataTable mergedDataTable;
    private FormPanel rowUIList;

    /**
     * @param workbenchUI The workbench
     * @param run The algorithm run
     * @param slots The displayed slots
     */
    public ACAQMultipleResultDataSlotTableUI(ACAQProjectUI workbenchUI, ACAQRun run, List<ACAQDataSlot> slots) {
        super(workbenchUI);
        this.run = run;
        this.slots = slots;

        initialize();
        reloadTable();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        table = new JXTable();
        table.setRowHeight(25);
        table.setDefaultRenderer(Path.class, new ACAQRowLocationTableCellRenderer());
        table.setDefaultRenderer(ACAQAlgorithm.class, new ACAQAlgorithmTableCellRenderer());
        table.setDefaultRenderer(ACAQProjectCompartment.class, new ACAQProjectCompartmentTableCellRenderer());
        table.setDefaultRenderer(ACAQExportedDataTable.Row.class, new ACAQRowDataMergedTableCellRenderer(getWorkbenchUI()));
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

        rowUIList = new FormPanel(null, false, false, true);
        add(rowUIList, BorderLayout.SOUTH);
    }

    private void handleSlotRowDefaultAction(int selectedRow) {
        int row = table.getRowSorter().convertRowIndexToModel(selectedRow);
        ACAQExportedDataTable.Row rowInstance = mergedDataTable.getRowList().get(row);
        ACAQDataSlot slot = mergedDataTable.getSlot(row);
        ACAQResultDataSlotRowUI ui = ACAQUIDatatypeRegistry.getInstance().getUIForResultSlot(getWorkbenchUI(), slot, rowInstance);
        ui.handleDefaultAction();
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();
        for (int viewRow : selectedRows) {
            int row = table.getRowSorter().convertRowIndexToModel(viewRow);
            ACAQExportedDataTable.Row rowInstance = mergedDataTable.getRowList().get(row);
            ACAQDataSlot slot = mergedDataTable.getSlot(row);
            JLabel nameLabel = new JLabel(rowInstance.getLocation().toString(), ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(slot));
            ACAQResultDataSlotRowUI rowUI = ACAQUIDatatypeRegistry.getInstance().getUIForResultSlot(getWorkbenchUI(), slot, rowInstance);
            rowUIList.addToForm(rowUI, nameLabel, null);
        }
    }

    private void reloadTable() {
        mergedDataTable = new ACAQMergedExportedDataTable();
        for (ACAQDataSlot slot : this.slots) {
            ACAQExportedDataTable dataTable = ACAQExportedDataTable.loadFromJson(slot.getStoragePath().resolve("data-table.json"));
            mergedDataTable.add(getProject(), slot, dataTable);
        }
        table.setModel(mergedDataTable);

        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new ACAQMergedDataSlotTableColumnHeaderRenderer(mergedDataTable));
        }
        table.setAutoCreateRowSorter(true);

        table.packAll();

        if (mergedDataTable.getRowCount() == 1) {
            table.setRowSelectionInterval(0, 0);
        }
    }
}
