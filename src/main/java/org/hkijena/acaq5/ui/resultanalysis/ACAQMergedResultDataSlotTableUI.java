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
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.api.data.ACAQMergedExportedDataTable;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.settings.FileChooserSettings;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.components.SearchTextField;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;

/**
 * Displays the result of multiple {@link ACAQDataSlot}
 */
public class ACAQMergedResultDataSlotTableUI extends ACAQProjectWorkbenchPanel {

    private final List<ACAQDataSlot> slots;
    private ACAQRun run;
    private JXTable table;
    private ACAQMergedExportedDataTable mergedDataTable;
    private FormPanel rowUIList;
    private SearchTextField searchTextField = new SearchTextField();

    /**
     * @param workbenchUI The workbench
     * @param run         The algorithm run
     * @param slots       The displayed slots
     */
    public ACAQMergedResultDataSlotTableUI(ACAQProjectWorkbench workbenchUI, ACAQRun run, List<ACAQDataSlot> slots) {
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
        table.setDefaultRenderer(ACAQGraphNode.class, new ACAQAlgorithmTableCellRenderer());
        table.setDefaultRenderer(ACAQProjectCompartment.class, new ACAQProjectCompartmentTableCellRenderer());
        table.setDefaultRenderer(ACAQExportedDataTable.Row.class, new ACAQRowDataMergedTableCellRenderer(getProjectWorkbench()));
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

        // Toolbar for searching and export
        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);
        toolBar.setFloatable(false);

        searchTextField.addActionListener(e -> {
            table.setModel(new DefaultTableModel());
            table.setModel(mergedDataTable);
            table.setRowFilter(new MergedDataSlotRowFilter(searchTextField));
            table.packAll();
        });
        toolBar.add(searchTextField);

        JButton exportButton = new JButton(UIUtils.getIconFromResources("export.png"));
        toolBar.add(exportButton);
        exportButton.setToolTipText("Export");
        JPopupMenu exportMenu = UIUtils.addPopupMenuToComponent(exportButton);

        JMenuItem exportAsCsvItem = new JMenuItem("as *.csv", UIUtils.getIconFromResources("filetype-csv.png"));
        exportAsCsvItem.addActionListener(e -> exportAsCSV());
        exportMenu.add(exportAsCsvItem);
    }

    private void exportAsCSV() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Export as *.csv", ".csv");
        if(path != null) {
            ResultsTableData tableData = ResultsTableData.fromTableModel(mergedDataTable);
            tableData.saveAsCSV(path);
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow) {
        int row = table.getRowSorter().convertRowIndexToModel(selectedRow);
        ACAQExportedDataTable.Row rowInstance = mergedDataTable.getRowList().get(row);
        ACAQDataSlot slot = mergedDataTable.getSlot(row);
        ACAQResultDataSlotRowUI ui = ACAQUIDatatypeRegistry.getInstance().getUIForResultSlot(getProjectWorkbench(), slot, rowInstance);
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
            ACAQResultDataSlotRowUI rowUI = ACAQUIDatatypeRegistry.getInstance().getUIForResultSlot(getProjectWorkbench(), slot, rowInstance);
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

    /**
     * Filters the entries
     */
    public static class MergedDataSlotRowFilter extends RowFilter<TableModel, Integer> {
        private final SearchTextField searchTextField;

        public MergedDataSlotRowFilter(SearchTextField searchTextField) {
            this.searchTextField = searchTextField;
        }

        @Override
        public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
            for (int i = 0; i < entry.getValueCount(); i++) {
                if(searchTextField.test(StringUtils.orElse(entry.getStringValue(i), ""))) {
                    return true;
                }
            }
            return false;
        }
    }
}
