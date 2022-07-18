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
import org.hkijena.jipipe.api.JIPipeProjectRun;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ui.ExpressionBuilderUI;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.cache.JIPipeDataInfoCellRenderer;
import org.hkijena.jipipe.ui.components.DataPreviewControlUI;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.search.SearchTextFieldTableRowFilter;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.MenuManager;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Displays the result of multiple {@link JIPipeDataSlot}
 */
public class JIPipeMergedResultDataSlotTableUI extends JIPipeProjectWorkbenchPanel {

    private final List<JIPipeDataSlot> slots;
    private final JIPipeProjectRun run;
    private final SearchTextField searchTextField = new SearchTextField();
    private final MenuManager menuManager = new MenuManager();
    private JXTable table;
    private JIPipeMergedExportedDataTable mergedDataTable;
    private FormPanel rowUIList;
    private JScrollPane scrollPane;
    private JIPipeRowDataMergedTableCellRenderer previewRenderer;
    private JIPipeRowDataAnnotationMergedTableCellRenderer dataAnnotationPreviewRenderer;

    /**
     * @param workbenchUI The workbench
     * @param run         The algorithm run
     * @param slots       The displayed slots
     */
    public JIPipeMergedResultDataSlotTableUI(JIPipeProjectWorkbench workbenchUI, JIPipeProjectRun run, List<JIPipeDataSlot> slots) {
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
        table.setDefaultRenderer(JIPipeTextAnnotation.class, new JIPipeAnnotationTableCellRenderer());
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
                        handleSlotRowDefaultAction(selectedRows[0], table.columnAtPoint(e.getPoint()));
                }
            }
        });

        rowUIList = new FormPanel(null, ParameterPanel.WITH_SCROLLING);
        add(rowUIList, BorderLayout.SOUTH);

        // Toolbar for searching and export
        add(menuManager.getMenuBar(), BorderLayout.NORTH);

        JButton openFolderButton = new JButton("Open folder", UIUtils.getIconFromResources("actions/folder-open.png"));
        openFolderButton.addActionListener(e -> openResultsFolder());
        menuManager.add(openFolderButton);

        searchTextField.addActionListener(e -> refreshTable());
        searchTextField.addButton("Open expression editor",
                UIUtils.getIconFromResources("actions/insert-math-expression.png"),
                this::openSearchExpressionEditor);
        menuManager.add(searchTextField);

        initializeViewMenu();
        initializeExportMenu();

        DataPreviewControlUI previewControlUI = new DataPreviewControlUI();
        menuManager.add(previewControlUI);
    }

    private void initializeViewMenu() {
        JMenu viewMenu = menuManager.getOrCreateMenu("View");

        JMenuItem autoSizeFitItem = new JMenuItem("Make columns fit contents", UIUtils.getIconFromResources("actions/zoom-fit-width.png"));
        autoSizeFitItem.setToolTipText("Auto-size columns to fit their contents");
        autoSizeFitItem.addActionListener(e -> table.packAll());
        viewMenu.add(autoSizeFitItem);

        JMenuItem autoSizeSmallItem = new JMenuItem("Compact columns", UIUtils.getIconFromResources("actions/zoom-best-fit.png"));
        autoSizeSmallItem.setToolTipText("Auto-size columns to the default size");
        autoSizeSmallItem.addActionListener(e -> UIUtils.packDataTable(table));
        viewMenu.add(autoSizeSmallItem);
    }

    private void initializeExportMenu() {
        JMenu exportMenu = menuManager.getOrCreateMenu("Export");

        JMenuItem exportAsTableItem = new JMenuItem("Metadata as table", UIUtils.getIconFromResources("actions/link.png"));
        exportAsTableItem.addActionListener(e -> exportAsTable());
        exportMenu.add(exportAsTableItem);

        JMenuItem exportAsCsvItem = new JMenuItem("Metadata as *.csv", UIUtils.getIconFromResources("data-types/results-table.png"));
        exportAsCsvItem.addActionListener(e -> exportAsCSV());
        exportMenu.add(exportAsCsvItem);

        JMenuItem exportFilesByMetadataItem = new JMenuItem("Data as files", UIUtils.getIconFromResources("actions/save.png"));
        exportFilesByMetadataItem.addActionListener(e -> exportFilesByMetadata());
        exportMenu.add(exportFilesByMetadataItem);
    }

    private void openSearchExpressionEditor(SearchTextField searchTextField) {
        Set<ExpressionParameterVariable> variables = new HashSet<>();
        for (int i = 0; i < table.getModel().getColumnCount(); i++) {
            variables.add(new ExpressionParameterVariable(table.getModel().getColumnName(i), "", table.getModel().getColumnName(i)));
        }
        String result = ExpressionBuilderUI.showDialog(getWorkbench().getWindow(), searchTextField.getText(), variables);
        if (result != null) {
            searchTextField.setText(result);
        }
    }

    private void openResultsFolder() {
        if (slots.size() == 1) {
            UIUtils.openFileInNative(slots.get(0).getSlotStoragePath());
        } else if (slots.stream().map(JIPipeDataSlot::getNode).distinct().count() == 1) {
            UIUtils.openFileInNative(slots.get(0).getSlotStoragePath().getParent());
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

    private void exportAsTable() {
        AnnotationTableData tableData = new AnnotationTableData();
        for (JIPipeDataTableMetadata exportedDataTable : mergedDataTable.getAddedTables()) {
            tableData.addRows(exportedDataTable.toAnnotationTable());
        }
        TableEditor.openWindow(getWorkbench(), tableData, "Metadata");
    }

    private void exportAsCSV() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export as *.csv", UIUtils.EXTENSION_FILTER_CSV);
        if (path != null) {
            AnnotationTableData tableData = new AnnotationTableData();
            for (JIPipeDataTableMetadata exportedDataTable : mergedDataTable.getAddedTables()) {
                tableData.addRows(exportedDataTable.toAnnotationTable());
            }
            tableData.saveAsCSV(path);
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow, int selectedColumn) {
        int multiRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
        int multiDataAnnotationColumn = selectedColumn >= 0 ? mergedDataTable.toDataAnnotationColumnIndex(table.convertColumnIndexToModel(selectedColumn)) : -1;
        JIPipeDataTableMetadataRow rowInstance = mergedDataTable.getRowList().get(multiRow);
        JIPipeDataSlot slot = mergedDataTable.getSlot(multiRow);
        JIPipeResultDataSlotRowUI ui = JIPipe.getDataTypes().getUIForResultSlot(getProjectWorkbench(), slot, rowInstance);
        int dataAnnotationColumn = -1;
        if (multiDataAnnotationColumn >= 0) {
            String name = mergedDataTable.getDataAnnotationColumns().get(multiDataAnnotationColumn);
            dataAnnotationColumn = slot.getDataAnnotationColumns().indexOf(name);
        }
        ui.handleDefaultActionOrDisplayDataAnnotation(dataAnnotationColumn);
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();
        for (int viewRow : selectedRows) {
            int row = table.getRowSorter().convertRowIndexToModel(viewRow);
            JIPipeDataTableMetadataRow rowInstance = mergedDataTable.getRowList().get(row);
            JIPipeDataSlot slot = mergedDataTable.getSlot(row);
            JLabel nameLabel = new JLabel("" + rowInstance.getIndex(), JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getDataTableTooltip(slot));
            JIPipeResultDataSlotRowUI rowUI = JIPipe.getDataTypes().getUIForResultSlot(getProjectWorkbench(), slot, rowInstance);
            rowUIList.addToForm(rowUI, nameLabel, null);
        }
    }

    private void reloadTable() {
        mergedDataTable = new JIPipeMergedExportedDataTable();
        for (JIPipeDataSlot slot : this.slots) {
            JIPipeDataTableMetadata dataTable = JIPipeDataTableMetadata.loadFromJson(slot.getSlotStoragePath().resolve("data-table.json"));
            mergedDataTable.add(getProject(), slot, dataTable);
        }
        if (GeneralDataSettings.getInstance().isGenerateResultPreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        previewRenderer = new JIPipeRowDataMergedTableCellRenderer(getProjectWorkbench(), mergedDataTable, scrollPane, table);
        dataAnnotationPreviewRenderer = new JIPipeRowDataAnnotationMergedTableCellRenderer(getProjectWorkbench(), mergedDataTable, scrollPane, table);
        table.setDefaultRenderer(JIPipeDataTableMetadataRow.class, previewRenderer);
        table.setDefaultRenderer(JIPipeExportedDataAnnotation.class, dataAnnotationPreviewRenderer);
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
        UIUtils.packDataTable(table);

        if (mergedDataTable.getRowCount() == 1) {
            table.setRowSelectionInterval(0, 0);
        }

        SwingUtilities.invokeLater(previewRenderer::updateRenderedPreviews);
        SwingUtilities.invokeLater(dataAnnotationPreviewRenderer::updateRenderedPreviews);
    }

}
