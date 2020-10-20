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
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMergedDataSlotTable;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.JIPipeComponentCellRenderer;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.ui.components.SearchTextFieldTableRowFilter;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeNodeTableCellRenderer;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeProjectCompartmentTableCellRenderer;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeTraitTableCellRenderer;
import org.hkijena.jipipe.utils.CustomScrollPane;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;

/**
 * UI that displays a {@link JIPipeDataSlot} that is cached
 */
public class JIPipeCacheMultiDataSlotTableUI extends JIPipeProjectWorkbenchPanel {

    private final List<JIPipeDataSlot> slots;
    private JIPipeMergedDataSlotTable multiSlotTable;
    private JXTable table;
    private FormPanel rowUIList;
    private SearchTextField searchTextField = new SearchTextField();

    /**
     * @param workbenchUI the workbench UI
     * @param slots       The slots
     */
    public JIPipeCacheMultiDataSlotTableUI(JIPipeProjectWorkbench workbenchUI, List<JIPipeDataSlot> slots) {
        super(workbenchUI);
        this.slots = slots;
        table = new JXTable();
        this.multiSlotTable = new JIPipeMergedDataSlotTable(table);
        for (JIPipeDataSlot slot : slots) {
            multiSlotTable.add(getProject(), slot);
        }

        initialize();
        reloadTable();
        getProject().getCache().getEventBus().register(this);
        updateStatus();
    }

    private void reloadTable() {
        table.setModel(new DefaultTableModel());
        table.setModel(multiSlotTable);
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new MultiDataSlotTableColumnRenderer(multiSlotTable));
        }
        table.setAutoCreateRowSorter(true);
        table.setRowFilter(new SearchTextFieldTableRowFilter(searchTextField));
        table.packAll();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        if (GeneralUISettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(GeneralUISettings.getInstance().getPreviewHeight());
        else
            table.setRowHeight(25);
        table.setDefaultRenderer(JIPipeData.class, new JIPipeDataCellRenderer());
        table.setDefaultRenderer(Component.class, new JIPipeComponentCellRenderer());
        table.setDefaultRenderer(JIPipeGraphNode.class, new JIPipeNodeTableCellRenderer());
        table.setDefaultRenderer(JIPipeProjectCompartment.class, new JIPipeProjectCompartmentTableCellRenderer());
        table.setDefaultRenderer(JIPipeAnnotation.class, new JIPipeTraitTableCellRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane scrollPane = new CustomScrollPane(table);
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
            ResultsTableData tableData = ResultsTableData.fromTableModel(multiSlotTable);
            tableData.saveAsCSV(path);
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow) {
        int multiRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
        JIPipeDataSlot slot = multiSlotTable.getSlot(multiRow);
        int row = multiSlotTable.getRow(multiRow);
        JIPipeDataSlotRowUI rowUI = new JIPipeDataSlotRowUI(getWorkbench(), slot, row);
        rowUI.handleDefaultAction();
//        slot.getData(row, JIPipeData.class).display(slot.getNode().getName() + "/" + slot.getName() + "/" + row, getWorkbench());
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();
        for (int viewRow : selectedRows) {
            int multiRow = table.getRowSorter().convertRowIndexToModel(viewRow);
            JIPipeDataSlot slot = multiSlotTable.getSlot(multiRow);
            int row = multiSlotTable.getRow(multiRow);
            String name = slot.getNode().getName() + "/" + slot.getName() + "/" + row;
            JLabel nameLabel = new JLabel(name, JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(slot));
            JIPipeDataSlotRowUI JIPipeDataSlotRowUI = new JIPipeDataSlotRowUI(getWorkbench(), slot, row);
            rowUIList.addToForm(JIPipeDataSlotRowUI, nameLabel, null);
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
        for (JIPipeDataSlot slot : slots) {
            if (slot.getRowCount() == 0) {
                removeAll();
                setLayout(new BorderLayout());
                JLabel label = new JLabel("Data was cleared", UIUtils.getIcon64FromResources("shredder.png"), JLabel.LEFT);
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
        private final JIPipeMergedDataSlotTable dataTable;

        /**
         * Creates a new instance
         *
         * @param dataTable The table
         */
        public MultiDataSlotTableColumnRenderer(JIPipeMergedDataSlotTable dataTable) {
            this.dataTable = dataTable;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
            int modelColumn = table.convertColumnIndexToModel(column);
            if (modelColumn < 6) {
                return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else {
                String info = dataTable.getTraitColumns().get(modelColumn - 6);
                String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                        UIUtils.getIconFromResources("data-types/annotation.png"),
                        info);
                return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
            }
        }
    }
}
