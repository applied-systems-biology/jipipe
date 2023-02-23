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
 *
 */

package org.hkijena.jipipe.ui.datatable;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ui.ExpressionBuilderUI;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.cache.JIPipeDataInfoCellRenderer;
import org.hkijena.jipipe.ui.cache.JIPipeDataTableRowUI;
import org.hkijena.jipipe.ui.cache.exporters.JIPipeDataTableToFilesByMetadataExporterRun;
import org.hkijena.jipipe.ui.cache.exporters.JIPipeDataTableToOutputExporterRun;
import org.hkijena.jipipe.ui.cache.exporters.JIPipeDataTableToZIPExporterRun;
import org.hkijena.jipipe.ui.components.DataPreviewControlUI;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.renderers.JIPipeComponentCellRenderer;
import org.hkijena.jipipe.ui.components.ribbon.LargeButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.Ribbon;
import org.hkijena.jipipe.ui.components.ribbon.SmallButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.SmallToggleButtonAction;
import org.hkijena.jipipe.ui.components.search.ExtendedDataTableSearchTextFieldTableRowFilter;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.resultanalysis.renderers.JIPipeAnnotationTableCellRenderer;
import org.hkijena.jipipe.ui.resultanalysis.renderers.JIPipeNodeTableCellRenderer;
import org.hkijena.jipipe.ui.resultanalysis.renderers.JIPipeProjectCompartmentTableCellRenderer;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.OwningStore;
import org.hkijena.jipipe.utils.data.Store;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Displays multiple {@link JIPipeDataTable} in one table.
 */
public class JIPipeExtendedMultiDataTableUI extends JIPipeWorkbenchPanel {

    private final List<Store<JIPipeDataTable>> dataTableStores;
    private final boolean withCompartmentAndAlgorithm;
    private final JXTable table;
    private final SearchTextField searchTextField = new SearchTextField();

    private final Ribbon ribbon = new Ribbon(2);
    private final JIPipeExtendedMultiDataTableModel multiSlotTable;
    private FormPanel rowUIList;

    /**
     * @param workbenchUI                 the workbench UI
     * @param dataTableStores                  The slots
     * @param withCompartmentAndAlgorithm if the compartment and algorithm are included as columns
     */
    public JIPipeExtendedMultiDataTableUI(JIPipeWorkbench workbenchUI, List<Store<JIPipeDataTable>> dataTableStores, boolean withCompartmentAndAlgorithm) {
        super(workbenchUI);
        this.dataTableStores = new ArrayList<>();
        this.dataTableStores.addAll(dataTableStores);
        this.withCompartmentAndAlgorithm = withCompartmentAndAlgorithm;
        table = new JXTable();
        this.multiSlotTable = new JIPipeExtendedMultiDataTableModel(table, withCompartmentAndAlgorithm);
        JIPipeProject project = null;
        if (getWorkbench() instanceof JIPipeProjectWorkbench) {
            project = ((JIPipeProjectWorkbench) getWorkbench()).getProject();
        }
        for (Store<JIPipeDataTable> dataTable : dataTableStores) {
            multiSlotTable.add(project, dataTable);
        }

        initialize();
        reloadTable();
        if (getWorkbench() instanceof JIPipeProjectWorkbench) {
            ((JIPipeProjectWorkbench) getWorkbench()).getProject().getCache().getEventBus().register(this);
        }
        updateStatus();
        GeneralDataSettings.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void onPreviewSizeChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
                if (isDisplayable() && "preview-size".equals(event.getKey())) {
                    reloadTable();
                }
            }
        });
        showDataRows(new int[0]);
    }

    private void reloadTable() {
        if (GeneralDataSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setModel(new DefaultTableModel());
        multiSlotTable.clearPreviewCache();
        table.setModel(multiSlotTable);
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new MultiDataSlotTableColumnRenderer(multiSlotTable));
        }
        table.setAutoCreateRowSorter(true);
        table.setRowFilter(new ExtendedDataTableSearchTextFieldTableRowFilter(searchTextField));
        UIUtils.packDataTable(table);

        int previewColumn = withCompartmentAndAlgorithm ? 4 : 2;
        columnModel.getColumn(previewColumn).setPreferredWidth(GeneralDataSettings.getInstance().getPreviewSize());
        SwingUtilities.invokeLater(multiSlotTable::updateRenderedPreviews);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        if (GeneralDataSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setDefaultRenderer(JIPipeDataInfo.class, new JIPipeDataInfoCellRenderer());
        table.setDefaultRenderer(Component.class, new JIPipeComponentCellRenderer());
        table.setDefaultRenderer(JIPipeGraphNode.class, new JIPipeNodeTableCellRenderer());
        table.setDefaultRenderer(JIPipeProjectCompartment.class, new JIPipeProjectCompartmentTableCellRenderer());
        table.setDefaultRenderer(JIPipeTextAnnotation.class, new JIPipeAnnotationTableCellRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane scrollPane = new JScrollPane(table);
        multiSlotTable.setScrollPane(scrollPane);
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

    public Ribbon getRibbon() {
        return ribbon;
    }

    private void initializeTableRibbon() {
        Ribbon.Task viewTask = ribbon.addTask("Table");
        Ribbon.Band tableBand = viewTask.addBand("General");
        Ribbon.Band previewBand = viewTask.addBand("Previews");

        // Table band
        tableBand.add(new LargeButtonAction("Open as tab", "Opens the current table in a new tab", UIUtils.getIcon32FromResources("actions/link.png"), this::openTableInNewTab));
        tableBand.add(new LargeButtonAction("Filter", "Opens the current filtered table in a new tab", UIUtils.getIcon32FromResources("actions/view-filter.png"), this::openFilteredTableInNewTab));
        tableBand.add(new SmallButtonAction("Fit columns", "Fits the table columns to their contents", UIUtils.getIconFromResources("actions/zoom-fit-width.png"), table::packAll));
        tableBand.add(new SmallButtonAction("Compact columns", "Auto-size columns to the default size", UIUtils.getIconFromResources("actions/zoom-fit-width.png"), () -> UIUtils.packDataTable(table)));

        // Preview band
        previewBand.add(new SmallToggleButtonAction("Enable previews", "Allows to toggle previews on and off", UIUtils.getIconFromResources("actions/zoom.png"), GeneralDataSettings.getInstance().isGenerateCachePreviews(), (toggle) -> {
            GeneralDataSettings.getInstance().setGenerateCachePreviews(toggle.isSelected());
            reloadTable();
        }));
        previewBand.add(new Ribbon.Action(UIUtils.boxHorizontal(new JLabel("Size"), new DataPreviewControlUI()), 1, new Insets(2, 2, 2, 2)));
    }

    private void initializeExportRibbon() {
        Ribbon.Task exportTask = ribbon.addTask("Export");
        Ribbon.Band dataBand = exportTask.addBand("Data");
        Ribbon.Band metadataBand = exportTask.addBand("Metadata");
        Ribbon.Band tableBand = exportTask.addBand("Table");

        // Data band
        dataBand.add(new LargeButtonAction("As files", "Exports all data as files named according to annotations", UIUtils.getIcon32FromResources("actions/document-export.png"), this::exportByMetadataExporter));

        // Metadata band
        metadataBand.add(new SmallButtonAction("To CSV/Excel", "Exports the text annotations as table", UIUtils.getIcon16FromResources("actions/table.png"), this::exportMetadataAsFiles));
        metadataBand.add(new SmallButtonAction("Open as table", "Opens the text annotations as table", UIUtils.getIcon16FromResources("actions/link.png"), this::exportMetadataAsTableEditor));

        // Table band
        tableBand.add(new SmallButtonAction("As ZIP", "Exports the whole table as ZIP file", UIUtils.getIcon16FromResources("actions/package.png"), this::exportAsJIPipeSlotZIP));
        tableBand.add(new SmallButtonAction("As directory", "Exports the whole table as directory", UIUtils.getIcon16FromResources("actions/folder-open.png"), this::exportAsJIPipeSlotDirectory));
    }

    private void openFilteredTableInNewTab() {
        String name = dataTableStores.stream().map(slotReference -> {
            JIPipeDataTable slot = slotReference.get();
            if(slot != null)
                return slot.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "");
            else
                return "[NA]";
        }).distinct().collect(Collectors.joining(", "));
        if (searchTextField.getSearchStrings().length > 0) {
            name = "[Filtered] " + name;
        } else {
            name = "Copy of " + name;
        }
        JIPipeDataTable copy = new JIPipeDataTable(JIPipeData.class);
        if (table.getRowFilter() != null) {
            for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                Store<JIPipeDataTable> slotStore = multiSlotTable.getSlotStore(modelRow);
                if(slotStore.isPresent()) {
                    JIPipeDataTable slot = slotStore.get();
                    int row = multiSlotTable.getRow(modelRow);
                    copy.addData(slot.getDataItemStore(row),
                            slot.getTextAnnotations(row),
                            JIPipeTextAnnotationMergeMode.OverwriteExisting,
                            slot.getDataAnnotations(row),
                            JIPipeDataAnnotationMergeMode.OverwriteExisting);
                }
                else {
                    throw new RuntimeException("Data table was already cleared!");
                }
            }
        }
        getWorkbench().getDocumentTabPane().addTab(name,
                UIUtils.getIconFromResources("data-types/data-table.png"),
                new JIPipeExtendedDataTableUI(getWorkbench(), new OwningStore<>(copy), true),
                DocumentTabPane.CloseMode.withSilentCloseButton,
                true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }

    private void openTableInNewTab() {
        String name = "Cache: " + dataTableStores.stream().map(slotReference -> {
            JIPipeDataTable slot = slotReference.get();
            if(slot != null)
                return slot.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "");
            else
                return "[NA]";
        }).distinct().collect(Collectors.joining(", "));
        getWorkbench().getDocumentTabPane().addTab(name,
                UIUtils.getIconFromResources("actions/database.png"),
                new JIPipeExtendedMultiDataTableUI(getWorkbench(), dataTableStores, withCompartmentAndAlgorithm),
                DocumentTabPane.CloseMode.withSilentCloseButton,
                true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
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

    private void exportByMetadataExporter() {
        List<JIPipeDataTable> dataTables = dereferenceDataTables();
        JIPipeDataTableToFilesByMetadataExporterRun run = new JIPipeDataTableToFilesByMetadataExporterRun(getWorkbench(), dataTables, false);
        if (run.setup()) {
            JIPipeRunnerQueue.getInstance().enqueue(run);
        }
    }

    private void exportAsJIPipeSlotDirectory() {
        List<JIPipeDataTable> dataTables = dereferenceDataTables();

        Path directory = FileChooserSettings.openDirectory(this, FileChooserSettings.LastDirectoryKey.Data, "Export as JIPipe data table");
        if (directory != null) {
            try {
                if (Files.isDirectory(directory) && Files.list(directory).findAny().isPresent()) {
                    if (JOptionPane.showConfirmDialog(this, "The selected directory " + directory + " is not empty. The contents will be deleted before writing the outputs. " +
                            "Continue anyways?", "Export as JIPipe data table", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                        return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Merge the data tables
            JIPipeDataTable mergedTable = new JIPipeDataTable(JIPipeData.class);
            for (JIPipeDataTable dataTable : dataTables) {
                mergedTable.addDataFromTable(dataTable, new JIPipeProgressInfo());
            }

            JIPipeDataTableToOutputExporterRun run = new JIPipeDataTableToOutputExporterRun(getWorkbench(),
                    directory,
                    Collections.singletonList(mergedTable),
                    true,
                    true);
            JIPipeRunnerQueue.getInstance().enqueue(run);
        }
    }

    private List<JIPipeDataTable> dereferenceDataTables() {
        List<JIPipeDataTable> dataTables = new ArrayList<>();
        for (Store<JIPipeDataTable> dataTableReference : dataTableStores) {
            JIPipeDataTable dataTable = dataTableReference.get();
            if(dataTable == null) {
                throw new RuntimeException("Data table has been cleared!");
            }
            dataTables.add(dataTable);
        }
        return dataTables;
    }

    private void exportAsJIPipeSlotZIP() {
        // Merge the data tables
        JIPipeDataTable mergedTable = new JIPipeDataTable(JIPipeData.class);
        for (Store<JIPipeDataTable> dataTableReference : dataTableStores) {
            JIPipeDataTable dataTable = dataTableReference.get();
            if(dataTable == null) {
                throw new RuntimeException("Data table has been cleared!");
            }
            mergedTable.addDataFromTable(dataTable, new JIPipeProgressInfo());
        }

        Path outputZipFile = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Export as JIPipe data table (*.zip)", UIUtils.EXTENSION_FILTER_ZIP);
        if (outputZipFile != null) {
            if (Files.isRegularFile(outputZipFile)) {
                if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(),
                        "The file '" + outputZipFile + "' already exists. Do you want to overwrite the file?",
                        "Export *.zip",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
                    return;
            }
            JIPipeDataTableToZIPExporterRun run = new JIPipeDataTableToZIPExporterRun(getWorkbench(),
                    outputZipFile,
                    mergedTable);
            JIPipeRunnerQueue.getInstance().enqueue(run);
        }
    }


    private void exportMetadataAsTableEditor() {
        AnnotationTableData tableData = new AnnotationTableData();
        for (JIPipeDataTable slot : multiSlotTable.getSlotList()) {
            tableData.addRows(slot.toAnnotationTable(true));
        }
        Set<String> nodes = new HashSet<>();
        for (JIPipeDataTable dataSlot : multiSlotTable.getSlotList()) {
            nodes.add(dataSlot.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_DISPLAY_NAME, ""));
        }
        TableEditor.openWindow(getWorkbench(), tableData, nodes.stream().sorted().collect(Collectors.joining(", ")));
    }

    private void exportMetadataAsFiles() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export as file", UIUtils.EXTENSION_FILTER_CSV, UIUtils.EXTENSION_FILTER_XLSX);
        if (path != null) {
            AnnotationTableData tableData = new AnnotationTableData();
            for (JIPipeDataTable slot : multiSlotTable.getSlotList()) {
                tableData.addRows(slot.toAnnotationTable(true));
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
        int multiDataAnnotationColumn = selectedColumn >= 0 ? multiSlotTable.toDataAnnotationColumnIndex(table.convertColumnIndexToModel(selectedColumn)) : -1;
        Store<JIPipeDataTable> slotStore = multiSlotTable.getSlotStore(multiRow);
        if(slotStore.isPresent()) {
            JIPipeDataTable slot = slotStore.get();

            int row = multiSlotTable.getRow(multiRow);
            int dataAnnotationColumn = -1;
            if (multiDataAnnotationColumn >= 0) {
                String name = multiSlotTable.getDataAnnotationColumns().get(multiDataAnnotationColumn);
                dataAnnotationColumn = slot.getDataAnnotationColumns().indexOf(name);
            }
            JIPipeDataTableRowUI rowUI = new JIPipeDataTableRowUI(getWorkbench(), slotStore, row);
            rowUI.handleDefaultActionOrDisplayDataAnnotation(dataAnnotationColumn);
//        slot.getData(row, JIPipeData.class).display(slot.getNode().getName() + "/" + slot.getName() + "/" + row, getWorkbench());
        }
        else {
            throw new RuntimeException("Data was already cleared!");
        }
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();

        List<JIPipeDataTable> dataTables = new ArrayList<>();
        for (Store<JIPipeDataTable> dataTableReference : dataTableStores) {
            JIPipeDataTable dataTable = dataTableReference.get();
            if(dataTable == null) {
                return;
            }
            dataTables.add(dataTable);
        }

        JLabel infoLabel = new JLabel();
        int rowCount = dataTables.stream().mapToInt(JIPipeDataTable::getRowCount).sum();
        infoLabel.setText(rowCount + " rows" + (dataTables.size() > 1 ? " across " + dataTables.size() + " tables" : "") + (selectedRows.length > 0 ? ", " + selectedRows.length + " selected" : ""));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        rowUIList.addWideToForm(infoLabel, null);

        for (int viewRow : selectedRows) {
            int multiRow = table.getRowSorter().convertRowIndexToModel(viewRow);
            Store<JIPipeDataTable> slotStore = multiSlotTable.getSlotStore(multiRow);
            if(slotStore.isPresent()) {
                JIPipeDataTable slot = slotStore.get();
                int row = multiSlotTable.getRow(multiRow);
                Class<? extends JIPipeData> dataClass = slot.getDataClass(row);
                String name = slot.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "") + "/" +
                        slot.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "") + "/" + row;
                JLabel nameLabel = new JLabel(name, JIPipe.getDataTypes().getIconFor(dataClass), JLabel.LEFT);
                nameLabel.setToolTipText(TooltipUtils.getDataTableTooltip(slot));
                JIPipeDataTableRowUI JIPipeDataTableRowUI = new JIPipeDataTableRowUI(getWorkbench(), slotStore, row);
                rowUIList.addToForm(JIPipeDataTableRowUI, nameLabel, null);
            }
            else {
                throw new RuntimeException("Data was already cleared!");
            }
        }
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(JIPipeCache.ModifiedEvent event) {
        updateStatus();
    }

    private void updateStatus() {
        List<JIPipeDataTable> dataTables = new ArrayList<>();
        for (Store<JIPipeDataTable> dataTableReference : dataTableStores) {
            JIPipeDataTable dataTable = dataTableReference.get();
            if(dataTable == null) {
                return;
            }
            dataTables.add(dataTable);
        }

        boolean hasData = false;
        for (JIPipeDataTable slot : dataTables) {
            if (slot.getRowCount() > 0) {
                hasData = true;
                break;
            }
        }
        if (!hasData) {
            if (getWorkbench() instanceof JIPipeProjectWorkbench) {
                ((JIPipeProjectWorkbench) getWorkbench()).getProject().getCache().getEventBus().unregister(this);
            }
        }
    }

    /**
     * Renders the column header
     */
    public static class MultiDataSlotTableColumnRenderer implements TableCellRenderer {
        private final JIPipeExtendedMultiDataTableModel multiDataTableModel;

        /**
         * Creates a new instance
         *
         * @param multiDataTableModel The table
         */
        public MultiDataSlotTableColumnRenderer(JIPipeExtendedMultiDataTableModel multiDataTableModel) {
            this.multiDataTableModel = multiDataTableModel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JIPipeExtendedMultiDataTableModel model = (JIPipeExtendedMultiDataTableModel) table.getModel();
            TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
            int modelColumn = table.convertColumnIndexToModel(column);
            int spacer = model.isWithCompartmentAndAlgorithm() ? 7 : 5;
            if (modelColumn < spacer) {
                return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else if (multiDataTableModel.toDataAnnotationColumnIndex(modelColumn) != -1) {
                String info = multiDataTableModel.getDataAnnotationColumns().get(multiDataTableModel.toDataAnnotationColumnIndex(modelColumn));
                String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                        UIUtils.getIconFromResources("data-types/data-annotation.png"),
                        info);
                return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
            } else {
                String info = multiDataTableModel.getTextAnnotationColumns().get(multiDataTableModel.toAnnotationColumnIndex(modelColumn));
                String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                        UIUtils.getIconFromResources("data-types/annotation.png"),
                        info);
                return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
            }
        }
    }
}
