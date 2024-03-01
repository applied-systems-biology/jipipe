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

package org.hkijena.jipipe.ui.datatable;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ui.ExpressionBuilderUI;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.cache.JIPipeDataInfoCellRenderer;
import org.hkijena.jipipe.ui.cache.JIPipeDataTableRowUI;
import org.hkijena.jipipe.ui.cache.exporters.JIPipeDataExporterRun;
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
import org.hkijena.jipipe.ui.datatracer.DataTracerUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.resultanalysis.renderers.JIPipeAnnotationTableCellRenderer;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.OwningStore;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.scripting.MacroUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * UI that displays a {@link JIPipeDataTable} that is cached
 */
public class JIPipeExtendedDataTableUI extends JIPipeWorkbenchPanel implements JIPipeCache.ModifiedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {

    private final boolean updateWithCache;
    private final SearchTextField searchTextField = new SearchTextField();
    private final Ribbon ribbon = new Ribbon(2);
    private Store<JIPipeDataTable> dataTableStore;
    private JXTable table;
    private FormPanel rowUIList;
    private JIPipeExtendedDataTableModel dataTableModel;
    private JScrollPane scrollPane;

    /**
     * @param workbenchUI     the workbench UI
     * @param dataTableStore  The slot
     * @param updateWithCache if the table should refresh on project cache changes
     */
    public JIPipeExtendedDataTableUI(JIPipeWorkbench workbenchUI, Store<JIPipeDataTable> dataTableStore, boolean updateWithCache) {
        super(workbenchUI);
        this.dataTableStore = dataTableStore;
        this.updateWithCache = updateWithCache;

        initialize();
        reloadTable();
        if (updateWithCache && getWorkbench() instanceof JIPipeProjectWorkbench) {
            ((JIPipeProjectWorkbench) getWorkbench()).getProject().getCache().getModifiedEventEmitter().subscribeWeak(this);
        }
        updateStatus();
        GeneralDataSettings.getInstance().getParameterChangedEventEmitter().subscribeWeak(this);
        showDataRows(new int[0]);
    }

    public JIPipeDataTable getDataTable() {
        return dataTableStore.get();
    }

    public void setDataTable(Store<JIPipeDataTable> dataTableStore) {
        this.dataTableStore = dataTableStore;
        reloadTable();
    }

    private void reloadTable() {
        dataTableModel = new JIPipeExtendedDataTableModel(table, dataTableStore);
        table.setModel(dataTableModel);
        dataTableModel.setScrollPane(scrollPane);
        if (GeneralDataSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setRowFilter(new ExtendedDataTableSearchTextFieldTableRowFilter(searchTextField));
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new WrapperColumnHeaderRenderer(dataTableStore));
        }
        table.setAutoCreateRowSorter(true);
        UIUtils.packDataTable(table);
        columnModel.getColumn(1).setPreferredWidth(GeneralDataSettings.getInstance().getPreviewSize());
        SwingUtilities.invokeLater(dataTableModel::updateRenderedPreviews);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        table = new JXTable();
        if (GeneralDataSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setDefaultRenderer(JIPipeDataInfo.class, new JIPipeDataInfoCellRenderer());
        table.setDefaultRenderer(Component.class, new JIPipeComponentCellRenderer());
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
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int[] selectedRows = table.getSelectedRows();
                    if (selectedRows.length > 0)
                        handleSlotRowDefaultAction(selectedRows[0], table.columnAtPoint(e.getPoint()));
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    showContextMenu(e);
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

    private void showContextMenu(MouseEvent e) {
        int viewRow = table.rowAtPoint(e.getPoint());
        int viewCol = table.columnAtPoint(e.getPoint());
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            table.setRowSelectionInterval(viewRow, viewRow);
            Object objectAtColumn;
            JIPipeDataTable dataTable = dataTableModel.getDataTable();
            int dataAnnotationColumn = -1;
            if (viewCol >= 0) {
                int modelColumn = table.convertColumnIndexToModel(viewCol);
                objectAtColumn = table.getModel().getValueAt(modelRow,
                        modelColumn);
                dataAnnotationColumn = modelColumn >= 0 ? dataTableModel.toDataAnnotationColumnIndex(table.convertColumnIndexToModel(modelColumn)) : -1;
            } else {
                objectAtColumn = null;
            }

            JPopupMenu popupMenu = new JPopupMenu();

            // Show/open with for data
            if (dataAnnotationColumn >= 0) {
                JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(modelRow, dataAnnotationColumn);
                popupMenu.add(UIUtils.createMenuItem("Show data annotation", "Shows the data annotation '" + dataAnnotation.getName() + "'",
                        UIUtils.getIconFromResources("actions/search.png"), () -> handleSlotRowDefaultAction(viewRow, viewCol)));
            }

            // Show/open with controls
            popupMenu.add(UIUtils.createMenuItem("Show", "Shows the data", UIUtils.getIconFromResources("actions/search.png"), () -> handleSlotRowDefaultAction(viewRow, 0)));

            {
                JMenu openWithMenu = new JMenu();
                openWithMenu.setText("Open with ...");

                Class<? extends JIPipeData> dataClass = dataTable.getDataClass(modelRow);
                String datatypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(dataClass);
                for (JIPipeDataDisplayOperation displayOperation : JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(datatypeId)) {
                    openWithMenu.add(UIUtils.createMenuItem(displayOperation.getName(), displayOperation.getDescription(), displayOperation.getIcon(),
                            () -> displayOperation.display(dataTable, modelRow, getWorkbench(), false)));
                }
                popupMenu.add(openWithMenu);
            }

            if (dataAnnotationColumn >= 0) {
                JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(modelRow, dataAnnotationColumn);
                JMenu openWithMenu = new JMenu();
                openWithMenu.setText("Open " + dataAnnotation.getName() + " with ...");

                Class<? extends JIPipeData> dataClass = dataAnnotation.getDataClass();
                String datatypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(dataClass);
                for (JIPipeDataDisplayOperation displayOperation : JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(datatypeId)) {
                    openWithMenu.add(UIUtils.createMenuItem(displayOperation.getName(), displayOperation.getDescription(), displayOperation.getIcon(),
                            () -> displayOperation.displayDataAnnotation(dataTable, modelRow, dataAnnotation, getWorkbench())));
                }
                popupMenu.add(openWithMenu);
            }

            // Trace
            if(getWorkbench() instanceof JIPipeProjectWorkbench) {
                popupMenu.add(UIUtils.createMenuItem("Trace ...",
                        "Allows to trace how the selected data was generated",
                        UIUtils.getIconFromResources("actions/footsteps.png"),
                        () -> traceData(dataTable.getDataContext(modelRow).getId())));
            }

            // String (preview)
            if (objectAtColumn instanceof String) {
                popupMenu.addSeparator();
                popupMenu.add(UIUtils.createMenuItem("Copy string representation", "Copies the string '" + objectAtColumn + "' into the clipboard",
                        UIUtils.getIconFromResources("actions/edit-copy.png"), () -> UIUtils.copyToClipboard(StringUtils.nullToEmpty(objectAtColumn))));
            }

            // Annotations
            if (objectAtColumn instanceof JIPipeTextAnnotation) {
                popupMenu.addSeparator();
                String annotationName = ((JIPipeTextAnnotation) objectAtColumn).getName();
                String annotationValue = ((JIPipeTextAnnotation) objectAtColumn).getValue();
                String annotationNameAndValue = annotationName + "=" + annotationValue;
                String filterExpression = annotationName + " == " + "\"" + MacroUtils.escapeString(annotationValue) + "\"";
                popupMenu.add(UIUtils.createMenuItem("Copy " + annotationName + " name", "Copies the string '" + annotationName + "' into the clipboard",
                        UIUtils.getIconFromResources("actions/edit-copy.png"), () -> UIUtils.copyToClipboard(StringUtils.nullToEmpty(annotationName))));
                popupMenu.add(UIUtils.createMenuItem("Copy " + annotationName + " value", "Copies the string '" + annotationValue + "' into the clipboard",
                        UIUtils.getIconFromResources("actions/edit-copy.png"), () -> UIUtils.copyToClipboard(StringUtils.nullToEmpty(annotationValue))));
                popupMenu.add(UIUtils.createMenuItem("Copy " + annotationName + " name and value", "Copies the string '" + annotationNameAndValue + "' into the clipboard",
                        UIUtils.getIconFromResources("actions/edit-copy.png"), () -> UIUtils.copyToClipboard(StringUtils.nullToEmpty(annotationNameAndValue))));
                popupMenu.add(UIUtils.createMenuItem("Copy " + annotationName + " as filter", "Copies the string '" + filterExpression + "' into the clipboard",
                        UIUtils.getIconFromResources("actions/filter.png"), () -> UIUtils.copyToClipboard(StringUtils.nullToEmpty(filterExpression))));
            }

            popupMenu.addSeparator();

            popupMenu.add(UIUtils.createMenuItem("Export", "Exports the data", UIUtils.getIconFromResources("actions/document-export.png"),
                    () -> {
                        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Export row " + modelRow);
                        if (path != null) {
                            Path directory = path.getParent();
                            String name = path.getFileName().toString();
                            JIPipeDataExporterRun run = new JIPipeDataExporterRun(dataTable.getData(modelRow, JIPipeData.class, new JIPipeProgressInfo()),
                                    directory, name);
                            JIPipeRunExecuterUI.runInDialog(getWorkbench(), SwingUtilities.getWindowAncestor(this), run, new JIPipeRunnerQueue("Export"));
                        }
                    }));

            if (dataAnnotationColumn >= 0) {
                JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(modelRow, dataAnnotationColumn);
                popupMenu.add(UIUtils.createMenuItem("Export " + dataAnnotation.getName(), "Exports the data annotation '" + dataAnnotation.getName() + "'", UIUtils.getIconFromResources("actions/document-export.png"),
                        () -> {
                            Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Export row " + modelRow);
                            if (path != null) {
                                Path directory = path.getParent();
                                String name = path.getFileName().toString();
                                JIPipeDataExporterRun run = new JIPipeDataExporterRun(dataAnnotation.getData(JIPipeData.class, new JIPipeProgressInfo()),
                                        directory, name);
                                JIPipeRunExecuterUI.runInDialog(getWorkbench(), SwingUtilities.getWindowAncestor(this), run, new JIPipeRunnerQueue("Export"));
                            }
                        }));
            }

            popupMenu.show(table, e.getX(), e.getY());
        }
    }

    private void traceData(String id) {
        DataTracerUI.openWindow((JIPipeProjectWorkbench) getWorkbench(), id);
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
        tableBand.add(new LargeButtonAction("Open as tab", "Opens the current table in a new tab", UIUtils.getIcon32FromResources("actions/open-in-new-window.png"), this::openTableInNewTab));
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
        metadataBand.add(new SmallButtonAction("Open as table", "Opens the text annotations as table", UIUtils.getIcon16FromResources("actions/open-in-new-window.png"), this::exportMetadataAsTableEditor));

        // Table band
        tableBand.add(new SmallButtonAction("As ZIP", "Exports the whole table as ZIP file", UIUtils.getIcon16FromResources("actions/package.png"), this::exportAsJIPipeSlotZIP));
        tableBand.add(new SmallButtonAction("As directory", "Exports the whole table as directory", UIUtils.getIcon16FromResources("actions/folder-open.png"), this::exportAsJIPipeSlotDirectory));
    }

    private void openFilteredTableInNewTab() {
        String name = getDataTable().getDisplayName();
        if (searchTextField.getSearchStrings().length > 0) {
            name = "[Filtered] " + name;
        } else {
            name = "Copy of " + name;
        }
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            JIPipeDataTable copy = new JIPipeDataTable(dataTable.getAcceptedDataType());
            if (table.getRowFilter() != null) {
                for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    copy.addData(dataTable.getDataItemStore(modelRow),
                            dataTable.getTextAnnotations(modelRow),
                            JIPipeTextAnnotationMergeMode.OverwriteExisting,
                            dataTable.getDataAnnotations(modelRow),
                            JIPipeDataAnnotationMergeMode.OverwriteExisting,
                            dataTable.getDataContext(modelRow),
                            new JIPipeProgressInfo());
                }
            }
            getWorkbench().getDocumentTabPane().addTab(name,
                    UIUtils.getIconFromResources("data-types/data-table.png"),
                    new JIPipeExtendedDataTableUI(getWorkbench(), new OwningStore<>(copy), true),
                    DocumentTabPane.CloseMode.withSilentCloseButton,
                    true);
            getWorkbench().getDocumentTabPane().switchToLastTab();
        }
    }

    private void openTableInNewTab() {
        String name = "Cache: " + getDataTable().getDisplayName();
        getWorkbench().getDocumentTabPane().addTab(name,
                UIUtils.getIconFromResources("actions/database.png"),
                new JIPipeExtendedDataTableUI(getWorkbench(), dataTableStore, true),
                DocumentTabPane.CloseMode.withSilentCloseButton,
                true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }

    private void openSearchExpressionEditor(SearchTextField searchTextField) {
        Set<JIPipeExpressionParameterVariableInfo> variables = new HashSet<>();
        for (int i = 0; i < table.getModel().getColumnCount(); i++) {
            variables.add(new JIPipeExpressionParameterVariableInfo(table.getModel().getColumnName(i), table.getModel().getColumnName(i), ""));
        }
        String result = ExpressionBuilderUI.showDialog(getWorkbench().getWindow(), searchTextField.getText(), variables);
        if (result != null) {
            searchTextField.setText(result);
        }
    }

    private void exportMetadataAsTableEditor() {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            ResultsTableData tableData = dataTableModel.getDataTable().toAnnotationTable(true);
            TableEditor.openWindow(getWorkbench(), tableData, dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_DISPLAY_NAME, "")
                    + "/" + dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, ""));
        }

    }

    private void exportByMetadataExporter() {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            JIPipeDataTableToFilesByMetadataExporterRun run = new JIPipeDataTableToFilesByMetadataExporterRun(getWorkbench(), Collections.singletonList(dataTable), false);
            if (run.setup()) {
                JIPipeRunnerQueue.getInstance().enqueue(run);
            }
        }
    }

    private void exportAsJIPipeSlotDirectory() {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            Path directory = FileChooserSettings.openDirectory(this, FileChooserSettings.LastDirectoryKey.Data, "Export as JIPipe data table");
            if (directory != null) {
                try {
                    if (Files.isDirectory(directory) && Files.list(directory).findAny().isPresent()) {
                        if (JOptionPane.showConfirmDialog(this, "The selected directory " + directory + " is not empty. The contents will be deleted before writing the outputs. " +
                                "Continue anyway?", "Export as JIPipe data table", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                            return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                JIPipeDataTableToOutputExporterRun run = new JIPipeDataTableToOutputExporterRun(getWorkbench(),
                        directory,
                        Collections.singletonList(dataTable),
                        false,
                        true);
                JIPipeRunnerQueue.getInstance().enqueue(run);
            }
        }
    }

    private void exportAsJIPipeSlotZIP() {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            Path outputZipFile = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Export as JIPipe data table (*.zip)", UIUtils.EXTENSION_FILTER_ZIP);
            if (outputZipFile != null) {
                outputZipFile = PathUtils.ensureExtension(outputZipFile, ".zip");
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

    private void exportMetadataAsFiles() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export as file", UIUtils.EXTENSION_FILTER_CSV, UIUtils.EXTENSION_FILTER_XLSX);
        if (path != null) {
            ResultsTableData tableData = dataTableModel.getDataTable().toAnnotationTable(true);
            if (UIUtils.EXTENSION_FILTER_XLSX.accept(path.toFile())) {
                tableData.saveAsXLSX(path);
            } else {
                tableData.saveAsCSV(path);
            }
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow, int selectedColumn) {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            int row = table.getRowSorter().convertRowIndexToModel(selectedRow);
            int dataAnnotationColumn = selectedColumn >= 0 ? dataTableModel.toDataAnnotationColumnIndex(table.convertColumnIndexToModel(selectedColumn)) : -1;
            JIPipeDataTableRowUI rowUI = new JIPipeDataTableRowUI(getWorkbench(), dataTableStore, row);
            rowUI.handleDefaultActionOrDisplayDataAnnotation(dataAnnotationColumn);
        }
    }

    private void showDataRows(int[] selectedRows) {

        rowUIList.clear();

        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {

            JLabel infoLabel = new JLabel();
            infoLabel.setText(dataTable.getRowCount() + " rows" + (selectedRows.length > 0 ? ", " + selectedRows.length + " selected" : ""));
            infoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            rowUIList.addWideToForm(infoLabel, null);

            for (int viewRow : selectedRows) {
                int row = table.getRowSorter().convertRowIndexToModel(viewRow);
                String name;
                String nodeName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "");
                if (!StringUtils.isNullOrEmpty(nodeName))
                    name = nodeName + "/" + dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "") + "/" + row;
                else
                    name = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "") + "/" + row;
                JLabel nameLabel = new JLabel(name, JIPipe.getDataTypes().getIconFor(dataTable.getAcceptedDataType()), JLabel.LEFT);
                nameLabel.setToolTipText(TooltipUtils.getDataTableTooltip(dataTable));
                JIPipeDataTableRowUI rowUI = new JIPipeDataTableRowUI(getWorkbench(), dataTableStore, row);
                rowUIList.addToForm(rowUI, nameLabel, null);
            }

        }
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Override
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {
        updateStatus();
    }

    private void updateStatus() {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            if (updateWithCache && dataTable.getRowCount() == 0) {
                if (getWorkbench() instanceof JIPipeProjectWorkbench) {
                    ((JIPipeProjectWorkbench) getWorkbench()).getProject().getCache().getModifiedEventEmitter().unsubscribe(this);
                }
            }
        }
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (isDisplayable() && "preview-size".equals(event.getKey())) {
            reloadTable();
        }
    }

    /**
     * Renders the column header of {@link JIPipeExtendedDataTableModel}
     */
    public static class WrapperColumnHeaderRenderer implements TableCellRenderer {
        private final Store<JIPipeDataTable> dataTableStore;

        /**
         * Creates a new instance
         *
         * @param dataTableStore The table reference
         */
        public WrapperColumnHeaderRenderer(Store<JIPipeDataTable> dataTableStore) {
            this.dataTableStore = dataTableStore;
        }

        /**
         * Converts the column index to an annotation column index, or returns -1 if the column is not one
         *
         * @param columnIndex absolute column index
         * @return relative annotation column index, or -1
         */
        public int toAnnotationColumnIndex(int columnIndex) {
            JIPipeDataTable dataTable = dataTableStore.get();
            if (dataTable != null) {
                if (columnIndex >= dataTable.getDataAnnotationColumnNames().size() + 4)
                    return columnIndex - dataTable.getDataAnnotationColumnNames().size() - 4;
                else
                    return -1;
            } else {
                return -1;
            }
        }

        /**
         * Converts the column index to a data annotation column index, or returns -1 if the column is not one
         *
         * @param columnIndex absolute column index
         * @return relative data annotation column index, or -1
         */
        public int toDataAnnotationColumnIndex(int columnIndex) {
            JIPipeDataTable dataTable = dataTableStore.get();
            if (dataTable != null) {
                if (columnIndex < dataTable.getDataAnnotationColumnNames().size() + 4 && (columnIndex - 4) < dataTable.getDataAnnotationColumnNames().size()) {
                    return columnIndex - 4;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
            JIPipeDataTable dataTable = dataTableStore.get();
            int modelColumn = table.convertColumnIndexToModel(column);
            if (modelColumn < 4) {
                return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else if (toDataAnnotationColumnIndex(modelColumn) != -1) {
                if (dataTable != null) {
                    String info = dataTable.getDataAnnotationColumnNames().get(toDataAnnotationColumnIndex(modelColumn));
                    String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                            UIUtils.getIconFromResources("data-types/data-annotation.png"),
                            info);
                    return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
                } else {
                    return new JLabel("NA");
                }
            } else {
                if (dataTable != null) {
                    int annotationColumnIndex = toAnnotationColumnIndex(modelColumn);
                    if (annotationColumnIndex < dataTable.getTextAnnotationColumnNames().size()) {
                        String info = dataTable.getTextAnnotationColumnNames().get(annotationColumnIndex);
                        String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                                UIUtils.getIconFromResources("data-types/annotation.png"),
                                info);
                        return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
                    } else {
                        return defaultRenderer.getTableCellRendererComponent(table, "Annotation", isSelected, hasFocus, row, column);
                    }
                } else {
                    return new JLabel("NA");
                }
            }
        }
    }
}
