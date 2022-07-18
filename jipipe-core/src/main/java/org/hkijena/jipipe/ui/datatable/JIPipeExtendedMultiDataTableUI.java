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
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
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
import org.hkijena.jipipe.ui.cache.JIPipeDataTableToFilesByMetadataExporterRun;
import org.hkijena.jipipe.ui.cache.JIPipeDataTableToOutputExporterRun;
import org.hkijena.jipipe.ui.cache.JIPipeDataTableToZIPExporterRun;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.PreviewControlUI;
import org.hkijena.jipipe.ui.components.renderers.JIPipeComponentCellRenderer;
import org.hkijena.jipipe.ui.components.search.ExtendedDataTableSearchTextFieldTableRowFilter;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeAnnotationTableCellRenderer;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeNodeTableCellRenderer;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeProjectCompartmentTableCellRenderer;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.MenuManager;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Displays multiple {@link JIPipeDataTable} in one table.
 */
public class JIPipeExtendedMultiDataTableUI extends JIPipeWorkbenchPanel {

    private final List<? extends JIPipeDataTable> dataTables;
    private final boolean withCompartmentAndAlgorithm;
    private JIPipeExtendedMultiDataTableModel multiSlotTable;
    private final JXTable table;
    private FormPanel rowUIList;
    private final SearchTextField searchTextField = new SearchTextField();

    private final MenuManager menuManager = new MenuManager();

    /**
     * @param workbenchUI                 the workbench UI
     * @param dataTables                  The slots
     * @param withCompartmentAndAlgorithm if the compartment and algorithm are included as columns
     */
    public JIPipeExtendedMultiDataTableUI(JIPipeWorkbench workbenchUI, List<? extends JIPipeDataTable> dataTables, boolean withCompartmentAndAlgorithm) {
        super(workbenchUI);
        this.dataTables = dataTables;
        this.withCompartmentAndAlgorithm = withCompartmentAndAlgorithm;
        table = new JXTable();
        this.multiSlotTable = new JIPipeExtendedMultiDataTableModel(table, withCompartmentAndAlgorithm);
        JIPipeProject project = null;
        if (getWorkbench() instanceof JIPipeProjectWorkbench) {
            project = ((JIPipeProjectWorkbench) getWorkbench()).getProject();
        }
        for (JIPipeDataTable dataTable : dataTables) {
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

        // Toolbar for searching and export
        add(menuManager.getMenuBar(), BorderLayout.NORTH);

        searchTextField.addActionListener(e -> reloadTable());
        searchTextField.addButton("Open expression editor",
                UIUtils.getIconFromResources("actions/insert-math-expression.png"),
                this::openSearchExpressionEditor);
        menuManager.add(searchTextField);

        initializeViewMenu();
        initializeExportMenu();

        PreviewControlUI previewControlUI = new PreviewControlUI();
        menuManager.add(previewControlUI);
    }

    public MenuManager getMenuManager() {
        return menuManager;
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

        viewMenu.addSeparator();

        JMenuItem openReferenceWindowItem = new JMenuItem("Open in new tab", UIUtils.getIconFromResources("actions/tab.png"));
        openReferenceWindowItem.addActionListener(e -> {
            openTableInNewTab();
        });
        viewMenu.add(openReferenceWindowItem);

        JMenuItem openFilteredWindowItem = new JMenuItem("Apply filter", UIUtils.getIconFromResources("actions/filter.png"));
        openFilteredWindowItem.addActionListener(e -> {
            openFilteredTableInNewTab();
        });
        viewMenu.add(openFilteredWindowItem);
    }

    private void initializeExportMenu() {
        JMenu exportMenu = menuManager.getOrCreateMenu("Export");

        JMenuItem exportAsTableItem = new JMenuItem("Metadata as table", UIUtils.getIconFromResources("actions/link.png"));
        exportAsTableItem.addActionListener(e -> exportAsTable());
        exportMenu.add(exportAsTableItem);

        JMenuItem exportAsCsvItem = new JMenuItem("Metadata as *.csv", UIUtils.getIconFromResources("data-types/results-table.png"));
        exportAsCsvItem.addActionListener(e -> exportAsCSV());
        exportMenu.add(exportAsCsvItem);

        JMenuItem exportStandardizedSlotDirectoryItem = new JMenuItem("Data as JIPipe data table (directory)", UIUtils.getIconFromResources("apps/jipipe.png"));
        exportStandardizedSlotDirectoryItem.addActionListener(e -> exportAsJIPipeSlotDirectory());
        exportMenu.add(exportStandardizedSlotDirectoryItem);

        JMenuItem exportStandardizedSlotZIPItem = new JMenuItem("Data as JIPipe data table (*.zip)", UIUtils.getIconFromResources("apps/jipipe.png"));
        exportStandardizedSlotZIPItem.addActionListener(e -> exportAsJIPipeSlotZIP());
        exportMenu.add(exportStandardizedSlotZIPItem);

        JMenuItem exportByMetadataExporterItem = new JMenuItem("Data as files", UIUtils.getIconFromResources("actions/save.png"));
        exportByMetadataExporterItem.addActionListener(e -> exportByMetadataExporter());
        exportMenu.add(exportByMetadataExporterItem);
    }

    private void openFilteredTableInNewTab() {
        String name = dataTables.stream().map(slot -> slot.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "")).distinct().collect(Collectors.joining(", "));
        if (searchTextField.getSearchStrings().length > 0) {
            name = "[Filtered] " + name;
        } else {
            name = "Copy of " + name;
        }
        JIPipeDataTable copy = new JIPipeDataTable(JIPipeData.class);
        if (table.getRowFilter() != null) {
            for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                JIPipeDataTable slot = multiSlotTable.getSlot(modelRow);
                int row = multiSlotTable.getRow(modelRow);
                copy.addData(slot.getVirtualData(row),
                        slot.getTextAnnotations(row),
                        JIPipeTextAnnotationMergeMode.OverwriteExisting,
                        slot.getDataAnnotations(row),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting);
            }
        }
        getWorkbench().getDocumentTabPane().addTab(name,
                UIUtils.getIconFromResources("data-types/data-table.png"),
                new JIPipeExtendedDataTableUI(getWorkbench(), copy, true),
                DocumentTabPane.CloseMode.withSilentCloseButton,
                true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }

    private void openTableInNewTab() {
        String name = "Cache: " + dataTables.stream().map(slot -> slot.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "")).distinct().collect(Collectors.joining(", "));
        getWorkbench().getDocumentTabPane().addTab(name,
                UIUtils.getIconFromResources("actions/database.png"),
                new JIPipeExtendedMultiDataTableUI(getWorkbench(), dataTables, withCompartmentAndAlgorithm),
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
        JIPipeDataTableToFilesByMetadataExporterRun run = new JIPipeDataTableToFilesByMetadataExporterRun(getWorkbench(), dataTables, true);
        if (run.setup()) {
            JIPipeRunnerQueue.getInstance().enqueue(run);
        }
    }

    private void exportAsJIPipeSlotDirectory() {
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
            JIPipeDataTableToOutputExporterRun run = new JIPipeDataTableToOutputExporterRun(getWorkbench(),
                    directory,
                    dataTables,
                    true,
                    true);
            JIPipeRunnerQueue.getInstance().enqueue(run);
        }
    }

    private void exportAsJIPipeSlotZIP() {
        for (JIPipeDataTable dataTable : dataTables) {
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
                        dataTable);
                JIPipeRunnerQueue.getInstance().enqueue(run);
            }
        }
    }


    private void exportAsTable() {
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

    private void exportAsCSV() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export as *.csv", UIUtils.EXTENSION_FILTER_CSV);
        if (path != null) {
            AnnotationTableData tableData = new AnnotationTableData();
            for (JIPipeDataTable slot : multiSlotTable.getSlotList()) {
                tableData.addRows(slot.toAnnotationTable(true));
            }
            tableData.saveAsCSV(path);
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow, int selectedColumn) {
        int multiRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
        int multiDataAnnotationColumn = selectedColumn >= 0 ? multiSlotTable.toDataAnnotationColumnIndex(table.convertColumnIndexToModel(selectedColumn)) : -1;
        JIPipeDataTable slot = multiSlotTable.getSlot(multiRow);
        int row = multiSlotTable.getRow(multiRow);
        int dataAnnotationColumn = -1;
        if (multiDataAnnotationColumn >= 0) {
            String name = multiSlotTable.getDataAnnotationColumns().get(multiDataAnnotationColumn);
            dataAnnotationColumn = slot.getDataAnnotationColumns().indexOf(name);
        }
        JIPipeDataTableRowUI rowUI = new JIPipeDataTableRowUI(getWorkbench(), slot, row);
        rowUI.handleDefaultActionOrDisplayDataAnnotation(dataAnnotationColumn);
//        slot.getData(row, JIPipeData.class).display(slot.getNode().getName() + "/" + slot.getName() + "/" + row, getWorkbench());
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();

        JLabel infoLabel = new JLabel();
        int rowCount = dataTables.stream().mapToInt(JIPipeDataTable::getRowCount).sum();
        infoLabel.setText(rowCount + " rows" + (dataTables.size() > 1 ? " across " + dataTables.size() + " tables" : "") + (selectedRows.length > 0 ? ", " + selectedRows.length + " selected" : ""));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        rowUIList.addWideToForm(infoLabel, null);

        for (int viewRow : selectedRows) {
            int multiRow = table.getRowSorter().convertRowIndexToModel(viewRow);
            JIPipeDataTable slot = multiSlotTable.getSlot(multiRow);
            int row = multiSlotTable.getRow(multiRow);
            Class<? extends JIPipeData> dataClass = slot.getDataClass(row);
            String name = slot.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "") + "/" +
                    slot.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "") + "/" + row;
            JLabel nameLabel = new JLabel(name, JIPipe.getDataTypes().getIconFor(dataClass), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getDataTableTooltip(slot));
            JIPipeDataTableRowUI JIPipeDataTableRowUI = new JIPipeDataTableRowUI(getWorkbench(), slot, row);
            rowUIList.addToForm(JIPipeDataTableRowUI, nameLabel, null);
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
        boolean hasData = false;
        for (JIPipeDataTable slot : dataTables) {
            if (slot.getRowCount() > 0) {
                hasData = true;
                break;
            }
        }
        if (!hasData) {
            removeAll();
            setLayout(new BorderLayout());
            JLabel label = new JLabel("No data available", UIUtils.getIcon64FromResources("no-data.png"), JLabel.LEFT);
            label.setFont(label.getFont().deriveFont(26.0f));
            add(label, BorderLayout.CENTER);

            if (getWorkbench() instanceof JIPipeProjectWorkbench) {
                ((JIPipeProjectWorkbench) getWorkbench()).getProject().getCache().getEventBus().unregister(this);
            }
            multiSlotTable = null;
        }
    }

    /**
     * Renders the column header
     */
    public static class MultiDataSlotTableColumnRenderer implements TableCellRenderer {
        private final JIPipeExtendedMultiDataTableModel dataTable;

        /**
         * Creates a new instance
         *
         * @param dataTable The table
         */
        public MultiDataSlotTableColumnRenderer(JIPipeExtendedMultiDataTableModel dataTable) {
            this.dataTable = dataTable;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JIPipeExtendedMultiDataTableModel model = (JIPipeExtendedMultiDataTableModel) table.getModel();
            TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
            int modelColumn = table.convertColumnIndexToModel(column);
            int spacer = model.isWithCompartmentAndAlgorithm() ? 7 : 5;
            if (modelColumn < spacer) {
                return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else if (dataTable.toDataAnnotationColumnIndex(modelColumn) != -1) {
                String info = dataTable.getDataAnnotationColumns().get(dataTable.toDataAnnotationColumnIndex(modelColumn));
                String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                        UIUtils.getIconFromResources("data-types/data-annotation.png"),
                        info);
                return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
            } else {
                String info = dataTable.getTextAnnotationColumns().get(dataTable.toAnnotationColumnIndex(modelColumn));
                String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                        UIUtils.getIconFromResources("data-types/annotation.png"),
                        info);
                return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
            }
        }
    }
}
