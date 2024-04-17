/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.resultanalysis;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadata;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadataRow;
import org.hkijena.jipipe.api.data.serialization.JIPipeMergedDataTableMetadata;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.app.resultanalysis.renderers.*;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.ui.ExpressionBuilderUI;
import org.hkijena.jipipe.plugins.settings.FileChooserSettings;
import org.hkijena.jipipe.plugins.settings.GeneralDataSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopDataInfoCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDataPreviewControlUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallToggleButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextFieldTableRowFilter;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
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
public class JIPipeDesktopMergedResultDataSlotTableUI extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final JIPipeProject project;
    private final Path storagePath;
    private final List<JIPipeDataSlot> slots;
    private final JIPipeDesktopSearchTextField searchTextField = new JIPipeDesktopSearchTextField();
    private final JIPipeDesktopRibbon ribbon = new JIPipeDesktopRibbon();
    private JXTable table;
    private JIPipeMergedDataTableMetadata mergedDataTable;
    private JIPipeDesktopFormPanel rowUIList;
    private JScrollPane scrollPane;
    private JIPipeDesktopRowDataMergedTableCellRenderer previewRenderer;
    private JIPipeDesktopRowDataAnnotationMergedTableCellRenderer dataAnnotationPreviewRenderer;

    public JIPipeDesktopMergedResultDataSlotTableUI(JIPipeDesktopProjectWorkbench projectWorkbench, JIPipeProject project, Path storagePath, List<JIPipeDataSlot> slots) {
        super(projectWorkbench);
        this.project = project;
        this.storagePath = storagePath;
        this.slots = slots;

        initialize();
        reloadTable();
        GeneralDataSettings.getInstance().getParameterChangedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        table = new JXTable();
        if (GeneralDataSettings.getInstance().isGenerateResultPreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setDefaultRenderer(Path.class, new JIPipeDesktopRowIndexTableCellRenderer());
        table.setDefaultRenderer(JIPipeDataInfo.class, new JIPipeDesktopDataInfoCellRenderer());
        table.setDefaultRenderer(JIPipeGraphNode.class, new JIPipeDesktopNodeTableCellRenderer());
        table.setDefaultRenderer(JIPipeProjectCompartment.class, new JIPipeDesktopProjectCompartmentTableCellRenderer());
        table.setDefaultRenderer(JIPipeTextAnnotation.class, new JIPipeDesktopAnnotationTableCellRenderer());
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

        rowUIList = new JIPipeDesktopFormPanel(null, JIPipeDesktopParameterPanel.WITH_SCROLLING);
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

    private void initializeTableRibbon() {
        JIPipeDesktopRibbon.Task viewTask = ribbon.addTask("Table");
        JIPipeDesktopRibbon.Band tableBand = viewTask.addBand("General");
        JIPipeDesktopRibbon.Band previewBand = viewTask.addBand("Previews");
        JIPipeDesktopRibbon.Band dataBand = viewTask.addBand("Data");

        // Table band
        tableBand.add(new JIPipeDesktopSmallButtonRibbonAction("Fit columns", "Fits the table columns to their contents", UIUtils.getIconFromResources("actions/zoom-fit-width.png"), table::packAll));
        tableBand.add(new JIPipeDesktopSmallButtonRibbonAction("Compact columns", "Auto-size columns to the default size", UIUtils.getIconFromResources("actions/zoom-fit-width.png"), () -> UIUtils.packDataTable(table)));

        // Preview band
        previewBand.add(new JIPipeDesktopSmallToggleButtonRibbonAction("Enable previews", "Allows to toggle previews on and off", UIUtils.getIconFromResources("actions/zoom.png"), GeneralDataSettings.getInstance().isGenerateResultPreviews(), (toggle) -> {
            GeneralDataSettings.getInstance().setGenerateResultPreviews(toggle.isSelected());
            reloadTable();
        }));
        previewBand.add(new JIPipeDesktopRibbon.Action(UIUtils.boxHorizontal(new JLabel("Size"), new JIPipeDesktopDataPreviewControlUI()), 1, new Insets(2, 2, 2, 2)));

        // Data band
        dataBand.add(new JIPipeDesktopLargeButtonRibbonAction("Open directory", "Opens the directory that contains the displayed results", UIUtils.getIcon32FromResources("actions/folder-open.png"), this::openResultsFolder));
    }

    private void initializeExportRibbon() {
        JIPipeDesktopRibbon.Task exportTask = ribbon.addTask("Export");
        JIPipeDesktopRibbon.Band dataBand = exportTask.addBand("Data");
        JIPipeDesktopRibbon.Band metadataBand = exportTask.addBand("Metadata");

        // Data band
        dataBand.add(new JIPipeDesktopLargeButtonRibbonAction("As files", "Exports all data as files named according to annotations", UIUtils.getIcon32FromResources("actions/document-export.png"), this::exportFilesByMetadata));

        // Metadata band
        metadataBand.add(new JIPipeDesktopSmallButtonRibbonAction("To CSV/Excel", "Exports the text annotations as table", UIUtils.getIcon16FromResources("actions/table.png"), this::exportMetadataAsFiles));
        metadataBand.add(new JIPipeDesktopSmallButtonRibbonAction("Open as table", "Opens the text annotations as table", UIUtils.getIcon16FromResources("actions/open-in-new-window.png"), this::exportMetadataAsTableEditor));
    }

    private void openSearchExpressionEditor(JIPipeDesktopSearchTextField searchTextField) {
        Set<JIPipeExpressionParameterVariableInfo> variables = new HashSet<>();
        for (int i = 0; i < table.getModel().getColumnCount(); i++) {
            variables.add(new JIPipeExpressionParameterVariableInfo(table.getModel().getColumnName(i), table.getModel().getColumnName(i), ""));
        }
        String result = ExpressionBuilderUI.showDialog(getDesktopWorkbench().getWindow(), searchTextField.getText(), variables);
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
            UIUtils.openFileInNative(storagePath);
        }
    }

    private void exportFilesByMetadata() {
        JIPipeResultCopyFilesByMetadataExporterRun run = new JIPipeResultCopyFilesByMetadataExporterRun(getDesktopWorkbench(), slots, true);
        if (run.setup()) {
            JIPipeRunnableQueue.getInstance().enqueue(run);
        }
    }

    private void exportMetadataAsTableEditor() {
        AnnotationTableData tableData = new AnnotationTableData();
        for (JIPipeDataTableMetadata exportedDataTable : mergedDataTable.getAddedTables()) {
            tableData.addRows(exportedDataTable.toAnnotationTable());
        }
        JIPipeDesktopTableEditor.openWindow(getDesktopWorkbench(), tableData, "Metadata");
    }

    private void exportMetadataAsFiles() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export as file", UIUtils.EXTENSION_FILTER_CSV, UIUtils.EXTENSION_FILTER_XLSX);
        if (path != null) {
            AnnotationTableData tableData = new AnnotationTableData();
            for (JIPipeDataTableMetadata exportedDataTable : mergedDataTable.getAddedTables()) {
                tableData.addRows(exportedDataTable.toAnnotationTable());
            }
            if (UIUtils.EXTENSION_FILTER_XLSX.accept(path.toFile())) {
                tableData.saveAsXLSX(path);
            } else {
                tableData.saveAsCSV(path);
            }
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow, int selectedColumn) {
        int multiRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
        int multiDataAnnotationColumn = selectedColumn >= 0 ? mergedDataTable.toDataAnnotationColumnIndex(table.convertColumnIndexToModel(selectedColumn)) : -1;
        JIPipeDataTableMetadataRow rowInstance = mergedDataTable.getRowList().get(multiRow);
        JIPipeDataSlot slot = mergedDataTable.getSlot(multiRow);
        JIPipeDesktopResultDataSlotRowUI ui = JIPipe.getDataTypes().getUIForResultSlot(getDesktopProjectWorkbench(), slot, rowInstance);
        int dataAnnotationColumn = -1;
        if (multiDataAnnotationColumn >= 0) {
            String name = mergedDataTable.getDataAnnotationColumns().get(multiDataAnnotationColumn);
            dataAnnotationColumn = slot.getDataAnnotationColumnNames().indexOf(name);
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
            JIPipeDesktopResultDataSlotRowUI rowUI = JIPipe.getDataTypes().getUIForResultSlot(getDesktopProjectWorkbench(), slot, rowInstance);
            rowUIList.addToForm(rowUI, nameLabel, null);
        }
    }

    private void reloadTable() {
        mergedDataTable = new JIPipeMergedDataTableMetadata();
        for (JIPipeDataSlot slot : this.slots) {
            JIPipeDataTableMetadata dataTable = JIPipeDataTableMetadata.loadFromJson(slot.getSlotStoragePath().resolve("data-table.json"));
            mergedDataTable.add(getProject(), slot, dataTable);
        }
        if (GeneralDataSettings.getInstance().isGenerateResultPreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        previewRenderer = new JIPipeDesktopRowDataMergedTableCellRenderer(getDesktopProjectWorkbench(), mergedDataTable, scrollPane, table);
        dataAnnotationPreviewRenderer = new JIPipeDesktopRowDataAnnotationMergedTableCellRenderer(getDesktopProjectWorkbench(), mergedDataTable, scrollPane, table);
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
            column.setHeaderRenderer(new JIPipeDesktopMergedDataSlotTableColumnHeaderRenderer(mergedDataTable));
        }
        table.setAutoCreateRowSorter(true);
        table.setRowFilter(new JIPipeDesktopSearchTextFieldTableRowFilter(searchTextField));
        UIUtils.packDataTable(table);

        if (mergedDataTable.getRowCount() == 1) {
            table.setRowSelectionInterval(0, 0);
        }

        SwingUtilities.invokeLater(previewRenderer::updateRenderedPreviews);
        SwingUtilities.invokeLater(dataAnnotationPreviewRenderer::updateRenderedPreviews);
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (isDisplayable() && "preview-size".equals(event.getKey())) {
            reloadTable();
        }
    }
}