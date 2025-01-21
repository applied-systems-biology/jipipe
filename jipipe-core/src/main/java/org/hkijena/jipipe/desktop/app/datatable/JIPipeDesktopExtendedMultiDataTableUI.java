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

package org.hkijena.jipipe.desktop.app.datatable;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
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
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopDataInfoCellRenderer;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopDataTableRowDisplayUtil;
import org.hkijena.jipipe.desktop.app.cache.exporters.JIPipeDesktopDataExporterRun;
import org.hkijena.jipipe.desktop.app.cache.exporters.JIPipeDesktopDataTableToFilesByMetadataExporterRun;
import org.hkijena.jipipe.desktop.app.cache.exporters.JIPipeDesktopDataTableToOutputExporterRun;
import org.hkijena.jipipe.desktop.app.cache.exporters.JIPipeDesktopDataTableToZIPExporterRun;
import org.hkijena.jipipe.desktop.app.datatracer.JIPipeDesktopDataTracerUI;
import org.hkijena.jipipe.desktop.app.resultanalysis.renderers.JIPipeDesktopAnnotationTableCellRenderer;
import org.hkijena.jipipe.desktop.app.resultanalysis.renderers.JIPipeDesktopNodeTableCellRenderer;
import org.hkijena.jipipe.desktop.app.resultanalysis.renderers.JIPipeDesktopProjectCompartmentTableCellRenderer;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDataPreviewControlUI;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopComponentCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallToggleButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopExtendedDataTableSearchTextFieldTableRowFilter;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.ui.ExpressionBuilderUI;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralDataApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.OwningStore;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.scripting.MacroUtils;
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
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Displays multiple {@link JIPipeDataTable} in one table.
 */
public class JIPipeDesktopExtendedMultiDataTableUI extends JIPipeDesktopWorkbenchPanel implements JIPipeParameterCollection.ParameterChangedEventListener, JIPipeCache.ModifiedEventListener {

    private final List<Store<JIPipeDataTable>> dataTableStores;
    private final boolean withCompartmentAndAlgorithm;
    private final JXTable table;
    private final JIPipeDesktopSearchTextField searchTextField = new JIPipeDesktopSearchTextField();
    private final JIPipeDesktopRibbon ribbon = new JIPipeDesktopRibbon(2);
    private final JIPipeDesktopExtendedMultiDataTableModel multiSlotTable;
    private final JLabel infoLabel = new JLabel();

    /**
     * @param workbenchUI                 the workbench UI
     * @param dataTableStores             The slots
     * @param withCompartmentAndAlgorithm if the compartment and algorithm are included as columns
     */
    public JIPipeDesktopExtendedMultiDataTableUI(JIPipeDesktopWorkbench workbenchUI, List<Store<JIPipeDataTable>> dataTableStores, boolean withCompartmentAndAlgorithm) {
        super(workbenchUI);
        this.dataTableStores = new ArrayList<>();
        this.dataTableStores.addAll(dataTableStores);
        this.withCompartmentAndAlgorithm = withCompartmentAndAlgorithm;
        table = new JXTable();
        this.multiSlotTable = new JIPipeDesktopExtendedMultiDataTableModel(table, withCompartmentAndAlgorithm);
        JIPipeProject project = null;
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            project = getDesktopWorkbench().getProject();
        }
        for (Store<JIPipeDataTable> dataTable : dataTableStores) {
            multiSlotTable.add(project, dataTable);
        }

        initialize();
        reloadTable();
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            getDesktopWorkbench().getProject().getCache().getModifiedEventEmitter().subscribeWeak(this);
        }
        updateStatus();
        JIPipeGeneralDataApplicationSettings.getInstance().getParameterChangedEventEmitter().subscribeWeak(this);
        showDataRows(new int[0]);
    }

    private void reloadTable() {
        if (JIPipeGeneralDataApplicationSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(JIPipeGeneralDataApplicationSettings.getInstance().getPreviewSize());
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
        table.setRowFilter(new JIPipeDesktopExtendedDataTableSearchTextFieldTableRowFilter(searchTextField));
        UIUtils.packDataTable(table);

        int previewColumn = withCompartmentAndAlgorithm ? 4 : 2;
        columnModel.getColumn(previewColumn).setPreferredWidth(JIPipeGeneralDataApplicationSettings.getInstance().getPreviewSize());
        SwingUtilities.invokeLater(multiSlotTable::updateRenderedPreviews);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        if (JIPipeGeneralDataApplicationSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(JIPipeGeneralDataApplicationSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setDefaultRenderer(JIPipeDataInfo.class, new JIPipeDesktopDataInfoCellRenderer());
        table.setDefaultRenderer(Component.class, new JIPipeDesktopComponentCellRenderer());
        table.setDefaultRenderer(JIPipeGraphNode.class, new JIPipeDesktopNodeTableCellRenderer());
        table.setDefaultRenderer(JIPipeProjectCompartment.class, new JIPipeDesktopProjectCompartmentTableCellRenderer());
        table.setDefaultRenderer(JIPipeTextAnnotation.class, new JIPipeDesktopAnnotationTableCellRenderer());
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
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int[] selectedRows = table.getSelectedRows();
                    if (selectedRows.length > 0)
                        handleSlotRowDefaultAction(selectedRows[0], table.columnAtPoint(e.getPoint()));
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    showContextMenu(e);
                }
            }
        });

        // Menu/Toolbar
        JPanel menuContainerPanel = new JPanel();
        menuContainerPanel.setLayout(new BoxLayout(menuContainerPanel, BoxLayout.Y_AXIS));
        add(menuContainerPanel, BorderLayout.NORTH);

        // Ribbon
        initializeRibbon(menuContainerPanel);

        // Search toolbar
        initializeToolbar(menuContainerPanel);

        // Add info label
        infoLabel.setBorder(UIUtils.createEmptyBorder(4));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void showContextMenu(MouseEvent e) {
        int viewRow = table.rowAtPoint(e.getPoint());
        int viewCol = table.columnAtPoint(e.getPoint());
        if (viewRow >= 0) {
            int rawModelRow = table.convertRowIndexToModel(viewRow);
            table.setRowSelectionInterval(viewRow, viewRow);
            Object objectAtColumn;
            JIPipeDataTable dataTable;
            int dataAnnotationColumn = -1;
            int multiRow = table.getRowSorter().convertRowIndexToModel(viewRow);
            Store<JIPipeDataTable> slotStore = multiSlotTable.getSlotStore(multiRow);
            if (slotStore.isPresent()) {
                dataTable = slotStore.get();
            } else {
                dataTable = null;
            }
            if (viewCol >= 0) {
                int modelColumn = table.convertColumnIndexToModel(viewCol);
                objectAtColumn = table.getModel().getValueAt(rawModelRow,
                        modelColumn);
                int multiDataAnnotationColumn = multiSlotTable.toDataAnnotationColumnIndex(table.convertColumnIndexToModel(viewCol));
                if (dataTable != null) {
                    if (multiDataAnnotationColumn >= 0) {
                        String name = multiSlotTable.getDataAnnotationColumns().get(multiDataAnnotationColumn);
                        dataAnnotationColumn = dataTable.getDataAnnotationColumnNames().indexOf(name);
                    }
                }
            } else {
                objectAtColumn = null;
            }

            // Conversion to internal row
            int modelRow = multiSlotTable.getRow(multiRow);

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
                for (JIPipeDesktopDataDisplayOperation displayOperation : JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(datatypeId)) {
                    openWithMenu.add(UIUtils.createMenuItem(displayOperation.getName(), displayOperation.getDescription(), displayOperation.getIcon(),
                            () -> displayOperation.display(dataTable, modelRow, getDesktopWorkbench(), false)));
                }
                popupMenu.add(openWithMenu);
            }

            // Trace
            if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
                popupMenu.add(UIUtils.createMenuItem("Trace ...",
                        "Allows to trace how the selected data was generated",
                        UIUtils.getIconFromResources("actions/footsteps.png"),
                        () -> traceData(dataTable.getDataContext(modelRow).getId())));
            }

            if (dataAnnotationColumn >= 0) {
                JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(modelRow, dataAnnotationColumn);
                JMenu openWithMenu = new JMenu();
                openWithMenu.setText("Open " + dataAnnotation.getName() + " with ...");

                Class<? extends JIPipeData> dataClass = dataAnnotation.getDataClass();
                String datatypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(dataClass);
                for (JIPipeDesktopDataDisplayOperation displayOperation : JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(datatypeId)) {
                    openWithMenu.add(UIUtils.createMenuItem(displayOperation.getName(), displayOperation.getDescription(), displayOperation.getIcon(),
                            () -> displayOperation.displayDataAnnotation(dataTable, modelRow, dataAnnotation, getDesktopWorkbench())));
                }
                popupMenu.add(openWithMenu);
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
                        Path path = JIPipeFileChooserApplicationSettings.saveFile(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export row " + modelRow);
                        if (path != null) {
                            Path directory = path.getParent();
                            String name = path.getFileName().toString();
                            JIPipeDesktopDataExporterRun run = new JIPipeDesktopDataExporterRun(dataTable.getData(modelRow, JIPipeData.class, new JIPipeProgressInfo()),
                                    directory, name);
                            JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), SwingUtilities.getWindowAncestor(this), run, new JIPipeRunnableQueue("Export"));
                        }
                    }));

            if (dataAnnotationColumn >= 0) {
                JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(modelRow, dataAnnotationColumn);
                popupMenu.add(UIUtils.createMenuItem("Export " + dataAnnotation.getName(), "Exports the data annotation '" + dataAnnotation.getName() + "'", UIUtils.getIconFromResources("actions/document-export.png"),
                        () -> {
                            Path path = JIPipeFileChooserApplicationSettings.saveFile(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export row " + modelRow);
                            if (path != null) {
                                Path directory = path.getParent();
                                String name = path.getFileName().toString();
                                JIPipeDesktopDataExporterRun run = new JIPipeDesktopDataExporterRun(dataAnnotation.getData(JIPipeData.class, new JIPipeProgressInfo()),
                                        directory, name);
                                JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), SwingUtilities.getWindowAncestor(this), run, new JIPipeRunnableQueue("Export"));
                            }
                        }));
            }

            popupMenu.show(table, e.getX(), e.getY());

        }
    }

    private void traceData(String id) {
        JIPipeDesktopDataTracerUI.openWindow((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(), id);
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

    public JIPipeDesktopRibbon getRibbon() {
        return ribbon;
    }

    private void initializeTableRibbon() {
        JIPipeDesktopRibbon.Task viewTask = ribbon.addTask("Table");
        JIPipeDesktopRibbon.Band tableBand = viewTask.addBand("General");
        JIPipeDesktopRibbon.Band previewBand = viewTask.addBand("Previews");

        // Table band
        tableBand.add(new JIPipeDesktopLargeButtonRibbonAction("Open as tab", "Opens the current table in a new tab", UIUtils.getIcon32FromResources("actions/open-in-new-window.png"), this::openTableInNewTab));
        tableBand.add(new JIPipeDesktopLargeButtonRibbonAction("Filter", "Opens the current filtered table in a new tab", UIUtils.getIcon32FromResources("actions/view-filter.png"), this::openFilteredTableInNewTab));
        tableBand.add(new JIPipeDesktopSmallButtonRibbonAction("Fit columns", "Fits the table columns to their contents", UIUtils.getIconFromResources("actions/zoom-fit-width.png"), table::packAll));
        tableBand.add(new JIPipeDesktopSmallButtonRibbonAction("Compact columns", "Auto-size columns to the default size", UIUtils.getIconFromResources("actions/zoom-fit-width.png"), () -> UIUtils.packDataTable(table)));

        // Preview band
        previewBand.add(new JIPipeDesktopSmallToggleButtonRibbonAction("Enable previews", "Allows to toggle previews on and off", UIUtils.getIconFromResources("actions/zoom.png"), JIPipeGeneralDataApplicationSettings.getInstance().isGenerateCachePreviews(), (toggle) -> {
            JIPipeGeneralDataApplicationSettings.getInstance().setGenerateCachePreviews(toggle.isSelected());
            reloadTable();
        }));
        previewBand.add(new JIPipeDesktopRibbon.Action(UIUtils.boxHorizontal(new JLabel("Size"), new JIPipeDesktopDataPreviewControlUI()), 1, new Insets(2, 2, 2, 2)));
    }

    private void initializeExportRibbon() {
        JIPipeDesktopRibbon.Task exportTask = ribbon.addTask("Export");
        JIPipeDesktopRibbon.Band dataBand = exportTask.addBand("Data");
        JIPipeDesktopRibbon.Band metadataBand = exportTask.addBand("Metadata");
        JIPipeDesktopRibbon.Band tableBand = exportTask.addBand("Table");

        // Data band
        dataBand.add(new JIPipeDesktopLargeButtonRibbonAction("As files", "Exports all data as files named according to annotations", UIUtils.getIcon32FromResources("actions/document-export.png"), this::exportByMetadataExporter));

        // Metadata band
        metadataBand.add(new JIPipeDesktopSmallButtonRibbonAction("To CSV/Excel", "Exports the text annotations as table", UIUtils.getIcon16FromResources("actions/table.png"), this::exportMetadataAsFiles));
        metadataBand.add(new JIPipeDesktopSmallButtonRibbonAction("Open as table", "Opens the text annotations as table", UIUtils.getIcon16FromResources("actions/open-in-new-window.png"), this::exportMetadataAsTableEditor));

        // Table band
        tableBand.add(new JIPipeDesktopSmallButtonRibbonAction("As ZIP", "Exports the whole table as ZIP file", UIUtils.getIcon16FromResources("actions/package.png"), this::exportAsJIPipeSlotZIP));
        tableBand.add(new JIPipeDesktopSmallButtonRibbonAction("As directory", "Exports the whole table as directory", UIUtils.getIcon16FromResources("actions/folder-open.png"), this::exportAsJIPipeSlotDirectory));
    }

    private void openFilteredTableInNewTab() {
        String name = dataTableStores.stream().map(slotReference -> {
            JIPipeDataTable slot = slotReference.get();
            if (slot != null)
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
                if (slotStore.isPresent()) {
                    JIPipeDataTable slot = slotStore.get();
                    int row = multiSlotTable.getRow(modelRow);
                    copy.addData(slot.getDataItemStore(row),
                            slot.getTextAnnotations(row),
                            JIPipeTextAnnotationMergeMode.OverwriteExisting,
                            slot.getDataAnnotations(row),
                            JIPipeDataAnnotationMergeMode.OverwriteExisting,
                            slot.getDataContext(row),
                            new JIPipeProgressInfo());
                } else {
                    throw new RuntimeException("Data table was already cleared!");
                }
            }
        }
        getDesktopWorkbench().getDocumentTabPane().addTab(name,
                UIUtils.getIconFromResources("data-types/data-table.png"),
                new JIPipeDesktopExtendedDataTableUI(getDesktopWorkbench(), new OwningStore<>(copy), true, false),
                JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                true);
        getDesktopWorkbench().getDocumentTabPane().switchToLastTab();
    }

    private void openTableInNewTab() {
        String name = "Cache: " + dataTableStores.stream().map(slotReference -> {
            JIPipeDataTable slot = slotReference.get();
            if (slot != null)
                return slot.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "");
            else
                return "[NA]";
        }).distinct().collect(Collectors.joining(", "));
        getDesktopWorkbench().getDocumentTabPane().addTab(name,
                UIUtils.getIconFromResources("actions/database.png"),
                new JIPipeDesktopExtendedMultiDataTableUI(getDesktopWorkbench(), dataTableStores, withCompartmentAndAlgorithm),
                JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                true);
        getDesktopWorkbench().getDocumentTabPane().switchToLastTab();
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

    private void exportByMetadataExporter() {
        List<JIPipeDataTable> dataTables = dereferenceDataTables();
        JIPipeDesktopDataTableToFilesByMetadataExporterRun run = new JIPipeDesktopDataTableToFilesByMetadataExporterRun(getDesktopWorkbench(), dataTables, false);
        if (run.setup()) {
            JIPipeRunnableQueue.getInstance().enqueue(run);
        }
    }

    private void exportAsJIPipeSlotDirectory() {
        List<JIPipeDataTable> dataTables = dereferenceDataTables();

        Path directory = JIPipeFileChooserApplicationSettings.openDirectory(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export as JIPipe data table");
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

            // Merge the data tables
            JIPipeDataTable mergedTable = new JIPipeDataTable(JIPipeData.class);
            for (JIPipeDataTable dataTable : dataTables) {
                mergedTable.addDataFromTable(dataTable, new JIPipeProgressInfo());
            }

            JIPipeDesktopDataTableToOutputExporterRun run = new JIPipeDesktopDataTableToOutputExporterRun(getDesktopWorkbench(),
                    directory,
                    Collections.singletonList(mergedTable),
                    true,
                    true);
            JIPipeRunnableQueue.getInstance().enqueue(run);
        }
    }

    private List<JIPipeDataTable> dereferenceDataTables() {
        List<JIPipeDataTable> dataTables = new ArrayList<>();
        for (Store<JIPipeDataTable> dataTableReference : dataTableStores) {
            JIPipeDataTable dataTable = dataTableReference.get();
            if (dataTable == null) {
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
            if (dataTable == null) {
                throw new RuntimeException("Data table has been cleared!");
            }
            mergedTable.addDataFromTable(dataTable, new JIPipeProgressInfo());
        }

        Path outputZipFile = JIPipeFileChooserApplicationSettings.saveFile(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export as JIPipe data table (*.zip)", UIUtils.EXTENSION_FILTER_ZIP);
        if (outputZipFile != null) {
            if (Files.isRegularFile(outputZipFile)) {
                if (JOptionPane.showConfirmDialog(getDesktopWorkbench().getWindow(),
                        "The file '" + outputZipFile + "' already exists. Do you want to overwrite the file?",
                        "Export *.zip",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
                    return;
            }
            JIPipeDesktopDataTableToZIPExporterRun run = new JIPipeDesktopDataTableToZIPExporterRun(getDesktopWorkbench(),
                    outputZipFile,
                    mergedTable);
            JIPipeRunnableQueue.getInstance().enqueue(run);
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
        JIPipeDesktopTableEditor.openWindow(getDesktopWorkbench(), tableData, nodes.stream().sorted().collect(Collectors.joining(", ")));
    }

    private void exportMetadataAsFiles() {
        Path path = JIPipeFileChooserApplicationSettings.saveFile(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Export as file", UIUtils.EXTENSION_FILTER_CSV, UIUtils.EXTENSION_FILTER_XLSX);
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
        if (slotStore.isPresent()) {
            JIPipeDataTable slot = slotStore.get();

            int row = multiSlotTable.getRow(multiRow);
            int dataAnnotationColumn = -1;
            if (multiDataAnnotationColumn >= 0) {
                String name = multiSlotTable.getDataAnnotationColumns().get(multiDataAnnotationColumn);
                dataAnnotationColumn = slot.getDataAnnotationColumnNames().indexOf(name);
            }
            JIPipeDesktopDataTableRowDisplayUtil rowUI = new JIPipeDesktopDataTableRowDisplayUtil(getDesktopWorkbench(), slotStore, row);
            rowUI.handleDefaultActionOrDisplayDataAnnotation(dataAnnotationColumn);
//        slot.getData(row, JIPipeData.class).display(slot.getNode().getName() + "/" + slot.getName() + "/" + row, getWorkbench());
        } else {
            throw new RuntimeException("Data was already cleared!");
        }
    }

    private void showDataRows(int[] selectedRows) {

        List<JIPipeDataTable> dataTables = new ArrayList<>();
        for (Store<JIPipeDataTable> dataTableReference : dataTableStores) {
            JIPipeDataTable dataTable = dataTableReference.get();
            if (dataTable == null) {
                return;
            }
            dataTables.add(dataTable);
        }


        int rowCount = dataTables.stream().mapToInt(JIPipeDataTable::getRowCount).sum();
        infoLabel.setText(rowCount + " rows" + (dataTables.size() > 1 ? " across " + dataTables.size() + " tables" : "") + (selectedRows.length > 0 ? ", " + selectedRows.length + " selected" : ""));
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
        List<JIPipeDataTable> dataTables = new ArrayList<>();
        for (Store<JIPipeDataTable> dataTableReference : dataTableStores) {
            JIPipeDataTable dataTable = dataTableReference.get();
            if (dataTable == null) {
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
            if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
                ((JIPipeDesktopProjectWorkbench) getDesktopWorkbench()).getProject().getCache().getModifiedEventEmitter().unsubscribe(this);
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
     * Renders the column header
     */
    public static class MultiDataSlotTableColumnRenderer implements TableCellRenderer {
        private final JIPipeDesktopExtendedMultiDataTableModel multiDataTableModel;

        /**
         * Creates a new instance
         *
         * @param multiDataTableModel The table
         */
        public MultiDataSlotTableColumnRenderer(JIPipeDesktopExtendedMultiDataTableModel multiDataTableModel) {
            this.multiDataTableModel = multiDataTableModel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JIPipeDesktopExtendedMultiDataTableModel model = (JIPipeDesktopExtendedMultiDataTableModel) table.getModel();
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
