package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class ACAQResultDataSlotUI extends ACAQUIPanel {

    private ACAQRun run;
    private ACAQDataSlot slot;
    private JXTable table;
    private ACAQExportedDataTable dataTable;
    private FormPanel rowUIList;

    public ACAQResultDataSlotUI(ACAQWorkbenchUI workbenchUI, ACAQRun run, ACAQDataSlot slot) {
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

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(table.getTableHeader(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            showDataRows(table.getSelectedRows());
        });

        rowUIList = new FormPanel(null, false, false, true);
        add(rowUIList, BorderLayout.SOUTH);
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();
        for (int row : selectedRows) {
            ACAQExportedDataTable.Row rowInstance = dataTable.getRowList().get(row);
            JLabel nameLabel = new JLabel(rowInstance.getLocation().toString(), ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(slot));
            ACAQResultDataSlotRowUI rowUI = ACAQUIDatatypeRegistry.getInstance().getUIForResultSlot(getWorkbenchUI(), slot, rowInstance);
            rowUIList.addToForm(rowUI, nameLabel, null);
        }
    }

    private void reloadTable() {
        dataTable = ACAQExportedDataTable.loadFromJson(slot.getStoragePath().resolve("data-table.json"));
        table.setModel(dataTable);
    }
}
