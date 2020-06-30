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

package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;

/**
 * UI that displays the {@link ACAQExportedDataTable} of an {@link ACAQDataSlot}
 */
public class ACAQResultDataSlotTableUI extends ACAQProjectWorkbenchPanel {

    private ACAQRun run;
    private ACAQDataSlot slot;
    private JXTable table;
    private ACAQExportedDataTable dataTable;
    private FormPanel rowUIList;

    /**
     * @param workbenchUI the workbench UI
     * @param run         The run
     * @param slot        The slot
     */
    public ACAQResultDataSlotTableUI(ACAQProjectWorkbench workbenchUI, ACAQRun run, ACAQDataSlot slot) {
        super(workbenchUI);
        this.run = run;
        this.slot = slot;

        initialize();
        reloadTable();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        table = new JXTable();
        table.setRowHeight(25);
        table.setDefaultRenderer(Path.class, new ACAQRowLocationTableCellRenderer());
        table.setDefaultRenderer(ACAQExportedDataTable.Row.class, new ACAQRowDataTableCellRenderer(getProjectWorkbench(), slot));
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
        ACAQExportedDataTable.Row rowInstance = dataTable.getRowList().get(row);
        ACAQResultDataSlotRowUI ui = ACAQUIDatatypeRegistry.getInstance().getUIForResultSlot(getProjectWorkbench(), slot, rowInstance);
        ui.handleDefaultAction();
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();
        for (int viewRow : selectedRows) {
            int row = table.getRowSorter().convertRowIndexToModel(viewRow);
            ACAQExportedDataTable.Row rowInstance = dataTable.getRowList().get(row);
            JLabel nameLabel = new JLabel(rowInstance.getLocation().toString(), ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(slot));
            ACAQResultDataSlotRowUI rowUI = ACAQUIDatatypeRegistry.getInstance().getUIForResultSlot(getProjectWorkbench(), slot, rowInstance);
            rowUIList.addToForm(rowUI, nameLabel, null);
        }
    }

    private void reloadTable() {
        dataTable = ACAQExportedDataTable.loadFromJson(slot.getStoragePath().resolve("data-table.json"));
        table.setModel(dataTable);

        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new ACAQDataSlotTableColumnHeaderRenderer(dataTable));
        }
        table.setAutoCreateRowSorter(true);

        table.packAll();

        if (dataTable.getRowCount() == 1) {
            table.setRowSelectionInterval(0, 0);
        }
    }
}
