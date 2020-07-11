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

package org.hkijena.jipipe.ui.resultanalysis;

import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.api.data.JIPipeMergedExportedDataTable;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.ui.components.SearchTextFieldTableRowFilter;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;

/**
 * Displays the result of multiple {@link JIPipeDataSlot}
 */
public class JIPipeMergedResultDataSlotTableUI extends JIPipeProjectWorkbenchPanel {

    private final List<JIPipeDataSlot> slots;
    private JIPipeRun run;
    private JXTable table;
    private JIPipeMergedExportedDataTable mergedDataTable;
    private FormPanel rowUIList;
    private SearchTextField searchTextField = new SearchTextField();

    /**
     * @param workbenchUI The workbench
     * @param run         The algorithm run
     * @param slots       The displayed slots
     */
    public JIPipeMergedResultDataSlotTableUI(JIPipeProjectWorkbench workbenchUI, JIPipeRun run, List<JIPipeDataSlot> slots) {
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
        table.setDefaultRenderer(Path.class, new JIPipeRowLocationTableCellRenderer());
        table.setDefaultRenderer(JIPipeGraphNode.class, new JIPipeAlgorithmTableCellRenderer());
        table.setDefaultRenderer(JIPipeProjectCompartment.class, new JIPipeProjectCompartmentTableCellRenderer());
        table.setDefaultRenderer(JIPipeExportedDataTable.Row.class, new JIPipeRowDataMergedTableCellRenderer(getProjectWorkbench()));
        table.setDefaultRenderer(JIPipeAnnotation.class, new JIPipeTraitTableCellRenderer());
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

        searchTextField.addActionListener(e -> refreshTable());
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
        if (path != null) {
            ResultsTableData tableData = ResultsTableData.fromTableModel(mergedDataTable);
            tableData.saveAsCSV(path);
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow) {
        int row = table.getRowSorter().convertRowIndexToModel(selectedRow);
        JIPipeExportedDataTable.Row rowInstance = mergedDataTable.getRowList().get(row);
        JIPipeDataSlot slot = mergedDataTable.getSlot(row);
        JIPipeResultDataSlotRowUI ui = JIPipeUIDatatypeRegistry.getInstance().getUIForResultSlot(getProjectWorkbench(), slot, rowInstance);
        ui.handleDefaultAction();
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();
        for (int viewRow : selectedRows) {
            int row = table.getRowSorter().convertRowIndexToModel(viewRow);
            JIPipeExportedDataTable.Row rowInstance = mergedDataTable.getRowList().get(row);
            JIPipeDataSlot slot = mergedDataTable.getSlot(row);
            JLabel nameLabel = new JLabel(rowInstance.getLocation().toString(), JIPipeUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(slot));
            JIPipeResultDataSlotRowUI rowUI = JIPipeUIDatatypeRegistry.getInstance().getUIForResultSlot(getProjectWorkbench(), slot, rowInstance);
            rowUIList.addToForm(rowUI, nameLabel, null);
        }
    }

    private void reloadTable() {
        mergedDataTable = new JIPipeMergedExportedDataTable();
        for (JIPipeDataSlot slot : this.slots) {
            JIPipeExportedDataTable dataTable = JIPipeExportedDataTable.loadFromJson(slot.getStoragePath().resolve("data-table.json"));
            mergedDataTable.add(getProject(), slot, dataTable);
        }
        table.setModel(mergedDataTable);
        refreshTable();
    }

    private void refreshTable() {
        table.setModel(new DefaultTableModel());
        table.setModel(mergedDataTable);
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new JIPipeMergedDataSlotTableColumnHeaderRenderer(mergedDataTable));
        }
        table.setAutoCreateRowSorter(true);
        table.setRowFilter(new SearchTextFieldTableRowFilter(searchTextField));
        table.packAll();

        if (mergedDataTable.getRowCount() == 1) {
            table.setRowSelectionInterval(0, 0);
        }
    }

}
