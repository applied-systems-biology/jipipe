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

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.cache.JIPipeDataInfoCellRenderer;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.PreviewControlUI;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.ui.components.SearchTextFieldTableRowFilter;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
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
    private JScrollPane scrollPane;
    private JIPipeRowDataMergedTableCellRenderer previewRenderer;

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
        GeneralDataSettings.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void onPreviewSizeChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
                if (isDisplayable() && "preview-size".equals(event.getKey())) {
                    reloadTable();
                }
            }
        });
    }

    private void initialize() {
        setLayout(new BorderLayout());
        table = new JXTable();
        if (GeneralDataSettings.getInstance().isGenerateResultPreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setDefaultRenderer(Path.class, new JIPipeRowIndexTableCellRenderer());
        table.setDefaultRenderer(JIPipeDataInfo.class, new JIPipeDataInfoCellRenderer());
        table.setDefaultRenderer(JIPipeGraphNode.class, new JIPipeNodeTableCellRenderer());
        table.setDefaultRenderer(JIPipeProjectCompartment.class, new JIPipeProjectCompartmentTableCellRenderer());
        table.setDefaultRenderer(JIPipeAnnotation.class, new JIPipeTraitTableCellRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        scrollPane = new JScrollPane(table);
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

        searchTextField.addActionListener(e -> refreshTable());
        toolBar.add(searchTextField);

        JButton openFolderButton = new JButton("Open folder", UIUtils.getIconFromResources("actions/folder-open.png"));
        openFolderButton.addActionListener(e -> openResultsFolder());
        toolBar.add(openFolderButton);

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("actions/document-export.png"));
        toolBar.add(exportButton);
        JPopupMenu exportMenu = UIUtils.addPopupMenuToComponent(exportButton);

        JMenuItem exportAsCsvItem = new JMenuItem("Metadata as *.csv", UIUtils.getIconFromResources("data-types/results-table.png"));
        exportAsCsvItem.addActionListener(e -> exportAsCSV());
        exportMenu.add(exportAsCsvItem);

        JMenuItem exportFilesByMetadataItem = new JMenuItem("Data as files", UIUtils.getIconFromResources("actions/save.png"));
        exportFilesByMetadataItem.addActionListener(e -> exportFilesByMetadata());
        exportMenu.add(exportFilesByMetadataItem);

        PreviewControlUI previewControlUI = new PreviewControlUI();
        toolBar.add(previewControlUI);
    }

    private void openResultsFolder() {
        if (slots.size() == 1) {
            UIUtils.openFileInNative(slots.get(0).getStoragePath());
        } else if (slots.stream().map(JIPipeDataSlot::getNode).distinct().count() == 1) {
            UIUtils.openFileInNative(slots.get(0).getStoragePath().getParent());
        } else {
            UIUtils.openFileInNative(run.getConfiguration().getOutputPath());
        }
    }

    private void exportFilesByMetadata() {
        JIPipeResultCopyFilesByMetadataExporterRun run = new JIPipeResultCopyFilesByMetadataExporterRun(getWorkbench(), slots, true);
        if (run.setup()) {
            JIPipeRunnerQueue.getInstance().enqueue(run);
        }
    }

    private void exportAsCSV() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Export as *.csv", UIUtils.EXTENSION_FILTER_CSV);
        if (path != null) {
            ResultsTableData tableData = ResultsTableData.fromTableModel(mergedDataTable);
            tableData.saveAsCSV(path);
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow) {
        int row = table.getRowSorter().convertRowIndexToModel(selectedRow);
        JIPipeExportedDataTable.Row rowInstance = mergedDataTable.getRowList().get(row);
        JIPipeDataSlot slot = mergedDataTable.getSlot(row);
        JIPipeResultDataSlotRowUI ui = JIPipe.getDataTypes().getUIForResultSlot(getProjectWorkbench(), slot, rowInstance);
        ui.handleDefaultAction();
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();
        for (int viewRow : selectedRows) {
            int row = table.getRowSorter().convertRowIndexToModel(viewRow);
            JIPipeExportedDataTable.Row rowInstance = mergedDataTable.getRowList().get(row);
            JIPipeDataSlot slot = mergedDataTable.getSlot(row);
            JLabel nameLabel = new JLabel("" + rowInstance.getIndex(), JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(slot));
            JIPipeResultDataSlotRowUI rowUI = JIPipe.getDataTypes().getUIForResultSlot(getProjectWorkbench(), slot, rowInstance);
            rowUIList.addToForm(rowUI, nameLabel, null);
        }
    }

    private void reloadTable() {
        mergedDataTable = new JIPipeMergedExportedDataTable();
        for (JIPipeDataSlot slot : this.slots) {
            JIPipeExportedDataTable dataTable = JIPipeExportedDataTable.loadFromJson(slot.getStoragePath().resolve("data-table.json"));
            mergedDataTable.add(getProject(), slot, dataTable);
        }
        if (GeneralDataSettings.getInstance().isGenerateResultPreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        previewRenderer = new JIPipeRowDataMergedTableCellRenderer(getProjectWorkbench(), mergedDataTable, scrollPane, table);
        table.setDefaultRenderer(JIPipeExportedDataTable.Row.class, previewRenderer);
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

        SwingUtilities.invokeLater(previewRenderer::updateRenderedPreviews);
    }

}
