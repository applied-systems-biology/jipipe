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
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ui.ExpressionBuilderUI;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.cache.JIPipeDataInfoCellRenderer;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.PreviewControlUI;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.search.SearchTextFieldTableRowFilter;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * UI that displays the {@link JIPipeDataTableMetadata} of an {@link JIPipeDataSlot}
 */
public class JIPipeResultDataSlotTableUI extends JIPipeProjectWorkbenchPanel {

    private JIPipeRun run;
    private JIPipeDataSlot slot;
    private JXTable table;
    private JIPipeDataTableMetadata dataTable;
    private FormPanel rowUIList;
    private SearchTextField searchTextField = new SearchTextField();
    private JIPipeRowDataTableCellRenderer previewRenderer;
    private JIPipeRowDataAnnotationTableCellRenderer dataAnnotationPreviewRenderer;

    /**
     * @param workbenchUI the workbench UI
     * @param run         The run
     * @param slot        The slot
     */
    public JIPipeResultDataSlotTableUI(JIPipeProjectWorkbench workbenchUI, JIPipeRun run, JIPipeDataSlot slot) {
        super(workbenchUI);
        this.run = run;
        this.slot = slot;

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
        JScrollPane scrollPane = new JScrollPane(table);

        table.setDefaultRenderer(Path.class, new JIPipeRowIndexTableCellRenderer());
        table.setDefaultRenderer(JIPipeDataInfo.class, new JIPipeDataInfoCellRenderer());
        previewRenderer = new JIPipeRowDataTableCellRenderer(getProjectWorkbench(), slot, table, scrollPane);
        dataAnnotationPreviewRenderer = new JIPipeRowDataAnnotationTableCellRenderer(getProjectWorkbench(), slot, table, scrollPane);
        table.setDefaultRenderer(JIPipeDataTableMetadataRow.class, previewRenderer);
        table.setDefaultRenderer(JIPipeExportedDataAnnotation.class, dataAnnotationPreviewRenderer);
        table.setDefaultRenderer(JIPipeTextAnnotation.class, new JIPipeAnnotationTableCellRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

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
        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);
        toolBar.setFloatable(false);

        searchTextField.addActionListener(e -> refreshTable());
        searchTextField.addButton("Open expression editor",
                UIUtils.getIconFromResources("actions/insert-math-expression.png"),
                this::openSearchExpressionEditor);
        toolBar.add(searchTextField);

        JButton openFolderButton = new JButton("Open folder", UIUtils.getIconFromResources("actions/folder-open.png"));
        openFolderButton.addActionListener(e -> openResultsFolder());
        toolBar.add(openFolderButton);

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("actions/document-export.png"));
        toolBar.add(exportButton);
        JPopupMenu exportMenu = UIUtils.addPopupMenuToComponent(exportButton);

        JMenuItem exportAsTableItem = new JMenuItem("Metadata as table", UIUtils.getIconFromResources("actions/link.png"));
        exportAsTableItem.addActionListener(e -> exportAsTable());
        exportMenu.add(exportAsTableItem);

        JMenuItem exportAsCsvItem = new JMenuItem("Metadata as *.csv", UIUtils.getIconFromResources("data-types/results-table.png"));
        exportAsCsvItem.addActionListener(e -> exportAsCSV());
        exportMenu.add(exportAsCsvItem);

        JMenuItem exportFilesByMetadataItem = new JMenuItem("Data as files", UIUtils.getIconFromResources("actions/save.png"));
        exportFilesByMetadataItem.addActionListener(e -> exportFilesByMetadata());
        exportMenu.add(exportFilesByMetadataItem);

        JButton autoSizeButton = new JButton(UIUtils.getIconFromResources("actions/zoom-fit-width.png"));
        autoSizeButton.setToolTipText("Auto-size columns to fit their contents");
        autoSizeButton.addActionListener(e -> table.packAll());
        toolBar.add(autoSizeButton);

        JButton smallSizeButton = new JButton(UIUtils.getIconFromResources("actions/zoom-best-fit.png"));
        smallSizeButton.setToolTipText("Auto-size columns to the default size");
        smallSizeButton.addActionListener(e -> UIUtils.packDataTable(table));
        toolBar.add(smallSizeButton);

        toolBar.addSeparator();

        PreviewControlUI previewControlUI = new PreviewControlUI();
        toolBar.add(previewControlUI);
    }

    private void openSearchExpressionEditor(SearchTextField searchTextField) {
        Set<ExpressionParameterVariable> variables = new HashSet<>();
        for (int i = 0; i < table.getModel().getColumnCount(); i++) {
            variables.add(new ExpressionParameterVariable(table.getModel().getColumnName(i), "", table.getModel().getColumnName(i)));
        }
        String result = ExpressionBuilderUI.showDialog(getWorkbench().getWindow(), searchTextField.getText(), variables);
        if(result != null) {
            searchTextField.setText(result);
        }
    }

    private void openResultsFolder() {
        UIUtils.openFileInNative(slot.getStoragePath());
    }

    private void exportFilesByMetadata() {
        JIPipeResultCopyFilesByMetadataExporterRun run = new JIPipeResultCopyFilesByMetadataExporterRun(getWorkbench(), Collections.singletonList(slot), false);
        if (run.setup()) {
            JIPipeRunnerQueue.getInstance().enqueue(run);
        }
    }

    private void exportAsTable() {
        ResultsTableData tableData = dataTable.toAnnotationTable();
        TableEditor.openWindow(getWorkbench(), tableData, "Metadata");
    }

    private void exportAsCSV() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export as *.csv", UIUtils.EXTENSION_FILTER_CSV);
        if (path != null) {
            ResultsTableData tableData = dataTable.toAnnotationTable();
            tableData.saveAsCSV(path);
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow, int selectedColumn) {
        int row = table.getRowSorter().convertRowIndexToModel(selectedRow);
        int dataAnnotationColumn = selectedColumn >= 0 ? dataTable.toDataAnnotationColumnIndex(table.convertColumnIndexToModel(selectedColumn)) : -1;
        JIPipeDataTableMetadataRow rowInstance = dataTable.getRowList().get(row);
        JIPipeResultDataSlotRowUI ui = JIPipe.getDataTypes().getUIForResultSlot(getProjectWorkbench(), slot, rowInstance);
        ui.handleDefaultActionOrDisplayDataAnnotation(dataAnnotationColumn);
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();
        for (int viewRow : selectedRows) {
            int row = table.getRowSorter().convertRowIndexToModel(viewRow);
            JIPipeDataTableMetadataRow rowInstance = dataTable.getRowList().get(row);
            JLabel nameLabel = new JLabel("" + rowInstance.getIndex(), JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getDataTableTooltip(slot));
            JIPipeResultDataSlotRowUI rowUI = JIPipe.getDataTypes().getUIForResultSlot(getProjectWorkbench(), slot, rowInstance);
            rowUIList.addToForm(rowUI, nameLabel, null);
        }
    }

    private void reloadTable() {
        dataTable = JIPipeDataTableMetadata.loadFromJson(slot.getStoragePath().resolve("data-table.json"));
        if (GeneralDataSettings.getInstance().isGenerateResultPreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setModel(dataTable);
        refreshTable();
    }

    private void refreshTable() {
        table.setModel(new DefaultTableModel());
        table.setModel(dataTable);
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new JIPipeDataSlotTableColumnHeaderRenderer(dataTable));
        }
        table.setAutoCreateRowSorter(true);
        table.setRowFilter(new SearchTextFieldTableRowFilter(searchTextField));
        UIUtils.packDataTable(table);

        if (dataTable.getRowCount() == 1) {
            table.setRowSelectionInterval(0, 0);
        }

        SwingUtilities.invokeLater(previewRenderer::updateRenderedPreviews);
        SwingUtilities.invokeLater(dataAnnotationPreviewRenderer::updateRenderedPreviews);
    }
}
