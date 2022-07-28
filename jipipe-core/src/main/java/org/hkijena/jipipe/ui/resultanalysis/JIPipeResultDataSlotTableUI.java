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
import org.hkijena.jipipe.ui.components.DataPreviewControlUI;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.ribbon.LargeButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.Ribbon;
import org.hkijena.jipipe.ui.components.ribbon.SmallButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.SmallToggleButtonAction;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.search.SearchTextFieldTableRowFilter;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.resultanalysis.renderers.*;
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

    private final JIPipeProjectRun run;
    private final JIPipeDataSlot slot;
    private final SearchTextField searchTextField = new SearchTextField();
    private final Ribbon ribbon = new Ribbon();
    private JXTable table;
    private JIPipeDataTableMetadata dataTable;
    private FormPanel rowUIList;
    private JIPipeRowDataTableCellRenderer previewRenderer;
    private JIPipeRowDataAnnotationTableCellRenderer dataAnnotationPreviewRenderer;

    /**
     * @param workbenchUI the workbench UI
     * @param run         The run
     * @param slot        The slot
     */
    public JIPipeResultDataSlotTableUI(JIPipeProjectWorkbench workbenchUI, JIPipeProjectRun run, JIPipeDataSlot slot) {
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


        // Menu/Toolbar
        JPanel menuContainerPanel = new JPanel();
        menuContainerPanel.setLayout(new BoxLayout(menuContainerPanel, BoxLayout.Y_AXIS));
        add(menuContainerPanel, BorderLayout.NORTH);

        // Ribbon
        initializeRibbon(menuContainerPanel);

        // Search toolbar
        initializeToolbar(menuContainerPanel);
    }

    private void initializeRibbon(JPanel menuContainerPanel) {
        menuContainerPanel.add(ribbon);
        initializeTableRibbon();
        initializeExportRibbon();
        ribbon.rebuildRibbon();
    }

    private void initializeTableRibbon() {
        Ribbon.Task viewTask = ribbon.addTask("Table");
        Ribbon.Band tableBand = viewTask.addBand("General");
        Ribbon.Band previewBand = viewTask.addBand("Previews");
        Ribbon.Band dataBand = viewTask.addBand("Data");

        // Table band
        tableBand.add(new SmallButtonAction("Fit columns", "Fits the table columns to their contents", UIUtils.getIconFromResources("actions/zoom-fit-width.png"), table::packAll));
        tableBand.add(new SmallButtonAction("Compact columns", "Auto-size columns to the default size", UIUtils.getIconFromResources("actions/zoom-fit-width.png"), () ->  UIUtils.packDataTable(table)));

        // Preview band
        previewBand.add(new SmallToggleButtonAction("Enable previews", "Allows to toggle previews on and off", UIUtils.getIconFromResources("actions/zoom.png"), GeneralDataSettings.getInstance().isGenerateResultPreviews(), (toggle) -> {
            GeneralDataSettings.getInstance().setGenerateResultPreviews(toggle.isSelected());
            reloadTable();
        }));
        previewBand.add(new Ribbon.Action(UIUtils.boxHorizontal(new JLabel("Size"), new DataPreviewControlUI()), 1, new Insets(2,2,2,2)));

        // Data band
        dataBand.add(new LargeButtonAction("Open directory", "Opens the directory that contains the displayed results", UIUtils.getIcon32FromResources("actions/folder-open.png"), this::openResultsFolder));
    }

    private void initializeExportRibbon() {
        Ribbon.Task exportTask = ribbon.addTask("Export");
        Ribbon.Band dataBand = exportTask.addBand("Data");
        Ribbon.Band metadataBand = exportTask.addBand("Metadata");

        // Data band
        dataBand.add(new LargeButtonAction("As files", "Exports all data as files named according to annotations", UIUtils.getIcon32FromResources("actions/document-export.png"), this::exportFilesByMetadata));

        // Metadata band
        metadataBand.add(new SmallButtonAction("To CSV/Excel", "Exports the text annotations as table", UIUtils.getIcon16FromResources("actions/table.png"), this::exportMetadataAsFiles));
        metadataBand.add(new SmallButtonAction("Open as table", "Opens the text annotations as table", UIUtils.getIcon16FromResources("actions/link.png"), this::exportMetadataAsTableEditor));
    }

    private void initializeToolbar(JPanel menuContainerPanel) {
        JToolBar searchToolbar = new JToolBar();
        searchToolbar.setFloatable(false);
        menuContainerPanel.add(Box.createVerticalStrut(8));
        menuContainerPanel.add(searchToolbar);

        searchTextField.addActionListener(e -> reloadTable());
        searchTextField.addButton("Open expression editor",
                UIUtils.getIconFromResources("actions/insert-math-expression.png"),
                this::openSearchExpressionEditor);
        searchToolbar.add(searchTextField);
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
        UIUtils.openFileInNative(slot.getSlotStoragePath());
    }

    private void exportFilesByMetadata() {
        JIPipeResultCopyFilesByMetadataExporterRun run = new JIPipeResultCopyFilesByMetadataExporterRun(getWorkbench(), Collections.singletonList(slot), false);
        if (run.setup()) {
            JIPipeRunnerQueue.getInstance().enqueue(run);
        }
    }

    private void exportMetadataAsTableEditor() {
        ResultsTableData tableData = dataTable.toAnnotationTable();
        TableEditor.openWindow(getWorkbench(), tableData, "Metadata");
    }

    private void exportMetadataAsFiles() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export as file", UIUtils.EXTENSION_FILTER_CSV, UIUtils.EXTENSION_FILTER_XLSX);
        if (path != null) {
            ResultsTableData tableData = dataTable.toAnnotationTable();
            if(UIUtils.EXTENSION_FILTER_XLSX.accept(path.toFile())) {
                tableData.saveAsXLSX(path);
            }
            else {
                tableData.saveAsCSV(path);
            }
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
        dataTable = JIPipeDataTableMetadata.loadFromJson(slot.getSlotStoragePath().resolve("data-table.json"));
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
