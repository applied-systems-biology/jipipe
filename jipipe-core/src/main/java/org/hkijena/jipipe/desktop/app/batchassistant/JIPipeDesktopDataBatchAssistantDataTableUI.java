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

package org.hkijena.jipipe.desktop.app.batchassistant;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopDataInfoCellRenderer;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopDataTableRowDisplayUtil;
import org.hkijena.jipipe.desktop.app.cache.exporters.JIPipeDesktopDataExporterRun;
import org.hkijena.jipipe.desktop.app.cache.exporters.JIPipeDesktopDataTableToFilesByMetadataExporterRun;
import org.hkijena.jipipe.desktop.app.cache.exporters.JIPipeDesktopDataTableToOutputExporterRun;
import org.hkijena.jipipe.desktop.app.cache.exporters.JIPipeDesktopDataTableToZIPExporterRun;
import org.hkijena.jipipe.desktop.app.resultanalysis.renderers.JIPipeDesktopAnnotationTableCellRenderer;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDataPreviewControlUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopComponentCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextFieldTableRowFilter;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.batchassistant.DataBatchStatusData;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.ui.ExpressionBuilderUI;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralDataApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.MenuManager;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.data.WeakStore;
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
public class JIPipeDesktopDataBatchAssistantDataTableUI extends JIPipeDesktopWorkbenchPanel implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final JIPipeDesktopSearchTextField searchTextField = new JIPipeDesktopSearchTextField();
    private final MenuManager menuManager = new MenuManager();
    private JIPipeDataTable dataTable;
    private JXTable table;
    private JIPipeDesktopFormPanel rowUIList;
    private JIPipeDesktopDataBatchAssistantTableModel dataTableModel;
    private JScrollPane scrollPane;
    private int numRowsWithIncompleteRequired = 0;
    private int numRowsWithIncompleteOptional = 0;
    private int numRowsWithMerging = 0;

    /**
     * @param workbenchUI the workbench UI
     * @param dataTable   data table containing the visualization of the data batches
     */
    public JIPipeDesktopDataBatchAssistantDataTableUI(JIPipeDesktopWorkbench workbenchUI, JIPipeDataTable dataTable) {
        super(workbenchUI);
        this.dataTable = dataTable;

        initialize();
        reloadTable();
        JIPipeGeneralDataApplicationSettings.getInstance().getParameterChangedEventEmitter().subscribeWeak(this);
        showDataRows(new int[0]);
    }

    public JIPipeDataTable getDataTable() {
        return dataTable;
    }

    public void setDataTable(JIPipeDataTable dataTable) {
        this.dataTable = dataTable;
        this.numRowsWithIncompleteRequired = 0;
        this.numRowsWithIncompleteOptional = 0;
        this.numRowsWithMerging = 0;
        for (int row = 0; row < dataTable.getRowCount(); row++) {
            DataBatchStatusData data = dataTable.getData(row, DataBatchStatusData.class, new JIPipeProgressInfo());
            if (data.getNumIncompleteRequired() > 0) {
                ++numRowsWithIncompleteRequired;
            }
            if (data.getNumIncompleteOptional() > 0) {
                ++numRowsWithIncompleteOptional;
            }
            if (data.getNumMerging() > 0) {
                ++numRowsWithMerging;
            }
        }
        reloadTable();
    }

    private void reloadTable() {
        dataTableModel = new JIPipeDesktopDataBatchAssistantTableModel(table, dataTable);
        table.setModel(dataTableModel);
        dataTableModel.setScrollPane(scrollPane);
        if (JIPipeGeneralDataApplicationSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(JIPipeGeneralDataApplicationSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setRowFilter(new JIPipeDesktopSearchTextFieldTableRowFilter(searchTextField));
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new WrapperColumnHeaderRenderer(new WeakStore<>(dataTable)));
        }
        table.setAutoCreateRowSorter(true);
        if (columnModel.getColumnCount() > 1) {
            columnModel.getColumn(1).setPreferredWidth(JIPipeGeneralDataApplicationSettings.getInstance().getPreviewSize());
        }
        SwingUtilities.invokeLater(() -> {
            dataTableModel.updateRenderedPreviews();
            UIUtils.packDataTable(table);
            showDataRows(new int[0]);
        });
    }

    private void initialize() {
        setLayout(new BorderLayout());
        table = new JXTable();
        if (JIPipeGeneralDataApplicationSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(JIPipeGeneralDataApplicationSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setDefaultRenderer(Integer.class, new BatchIndexCellRenderer());
        table.setDefaultRenderer(JIPipeDataInfo.class, new JIPipeDesktopDataInfoCellRenderer());
        table.setDefaultRenderer(Component.class, new JIPipeDesktopComponentCellRenderer());
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
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int[] selectedRows = table.getSelectedRows();
                    if (selectedRows.length > 0) {
                        handleSlotRowDefaultAction(selectedRows[0], table.columnAtPoint(e.getPoint()));
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    showContextMenu(e);
                }
            }
        });

        rowUIList = new JIPipeDesktopFormPanel(null, JIPipeDesktopParameterPanel.WITH_SCROLLING);
        add(rowUIList, BorderLayout.SOUTH);

        // Toolbar for searching and export
        add(menuManager.getMenuBar(), BorderLayout.NORTH);

        searchTextField.addActionListener(e -> reloadTable());
        searchTextField.addButton("Open expression editor",
                UIUtils.getIconFromResources("actions/insert-math-expression.png"),
                this::openSearchExpressionEditor);
        menuManager.add(searchTextField);

        // Window menu
        initializeViewMenu();

        // Export menu
        initializeExportMenu();

        JIPipeDesktopDataPreviewControlUI previewControlUI = new JIPipeDesktopDataPreviewControlUI();
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

        viewMenu.addSeparator();

        JMenuItem openReferenceWindowItem = new JMenuItem("Open in new tab", UIUtils.getIconFromResources("actions/tab.png"));
        openReferenceWindowItem.addActionListener(e -> {
            String name = "Iteration steps: " + getDataTable().getDisplayName();
            getDesktopWorkbench().getDocumentTabPane().addTab(name,
                    UIUtils.getIconFromResources("actions/database.png"),
                    new JIPipeDesktopDataBatchAssistantDataTableUI(getDesktopWorkbench(), getDataTable()),
                    JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                    true);
            getDesktopWorkbench().getDocumentTabPane().switchToLastTab();
        });
        viewMenu.add(openReferenceWindowItem);
    }

    private void initializeExportMenu() {
        JMenu exportMenu = menuManager.getOrCreateMenu("Export");

        JMenuItem exportAsTableItem = new JMenuItem("Metadata as table", UIUtils.getIconFromResources("actions/open-in-new-window.png"));
        exportAsTableItem.addActionListener(e -> exportAsTable());
        exportMenu.add(exportAsTableItem);

        JMenuItem exportAsCsvItem = new JMenuItem("Metadata as *.csv", UIUtils.getIconFromResources("data-types/results-table.png"));
        exportAsCsvItem.addActionListener(e -> exportAsCSV());
        exportMenu.add(exportAsCsvItem);

        JMenuItem exportStandardizedDirectorySlotItem = new JMenuItem("Data as JIPipe data table (directory)", UIUtils.getIconFromResources("apps/jipipe.png"));
        exportStandardizedDirectorySlotItem.addActionListener(e -> exportAsJIPipeSlotDirectory());
        exportMenu.add(exportStandardizedDirectorySlotItem);

        JMenuItem exportStandardizedZIPSlotItem = new JMenuItem("Data as JIPipe data table (directory)", UIUtils.getIconFromResources("apps/jipipe.png"));
        exportStandardizedZIPSlotItem.addActionListener(e -> exportAsJIPipeSlotZIP());
        exportMenu.add(exportStandardizedZIPSlotItem);

        JMenuItem exportByMetadataExporterItem = new JMenuItem("Data as files", UIUtils.getIconFromResources("actions/filesave.png"));
        exportByMetadataExporterItem.addActionListener(e -> exportByMetadataExporter());
        exportMenu.add(exportByMetadataExporterItem);
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
                            () -> displayOperation.display(dataTable, modelRow, getDesktopWorkbench(), false)));
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

    private void exportAsTable() {
        ResultsTableData tableData = dataTableModel.getDataTable().toAnnotationTable(true);
        JIPipeDesktopTableEditor.openWindow(getDesktopWorkbench(), tableData, dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_DISPLAY_NAME, "")
                + "/" + dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, ""));
    }

    private void exportByMetadataExporter() {
        JIPipeDesktopDataTableToFilesByMetadataExporterRun run = new JIPipeDesktopDataTableToFilesByMetadataExporterRun(getDesktopWorkbench(), Collections.singletonList(dataTable), false);
        if (run.setup()) {
            JIPipeRunnableQueue.getInstance().enqueue(run);
        }
    }

    private void exportAsJIPipeSlotDirectory() {
        Path directory = JIPipeFileChooserApplicationSettings.saveDirectory(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export as JIPipe data table");
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
            JIPipeDesktopDataTableToOutputExporterRun run = new JIPipeDesktopDataTableToOutputExporterRun(getDesktopWorkbench(),
                    directory,
                    Collections.singletonList(dataTable),
                    false,
                    true);
            JIPipeRunnableQueue.getInstance().enqueue(run);
        }
    }

    private void exportAsJIPipeSlotZIP() {
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
                    dataTable);
            JIPipeRunnableQueue.getInstance().enqueue(run);
        }
    }

    private void exportAsCSV() {
        Path path = JIPipeFileChooserApplicationSettings.saveFile(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Export as *.csv", UIUtils.EXTENSION_FILTER_CSV);
        if (path != null) {
            ResultsTableData tableData = dataTableModel.getDataTable().toAnnotationTable(true);
            tableData.saveAsCSV(path);
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow, int selectedColumn) {
        int row = table.getRowSorter().convertRowIndexToModel(selectedRow);
        int dataAnnotationColumn = selectedColumn >= 0 ? dataTableModel.toDataAnnotationColumnIndex(table.convertColumnIndexToModel(selectedColumn)) : -1;
        JIPipeDesktopDataTableRowDisplayUtil rowUI = new JIPipeDesktopDataTableRowDisplayUtil(getDesktopWorkbench(), new WeakStore<>(dataTable), row);
        rowUI.handleDefaultActionOrDisplayDataAnnotation(dataAnnotationColumn);
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();

        {
            JLabel infoLabel = new JLabel(UIUtils.getIconFromResources("actions/format-list-ordered.png"), JLabel.LEFT);
            infoLabel.setText(StringUtils.formatPluralS(dataTable.getRowCount(), "iteration step") + (selectedRows.length > 0 ? ", " + selectedRows.length + " selected" : ""));
            infoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            rowUIList.addWideToForm(infoLabel, null);
        }

        if (numRowsWithIncompleteRequired > 0) {
            JLabel warningLabel = new JLabel(StringUtils.formatPluralS(numRowsWithIncompleteRequired, "step") + " with missing data",
                    UIUtils.getIconFromResources("emblems/warning.png"),
                    JLabel.LEFT);
            warningLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(warningLabel, BorderLayout.CENTER);
            panel.add(UIUtils.createPopupHelpButton("There are iteration steps where slots are not assigned a data item. " +
                    "Please check the annotations in the 'Input data' section to find out why this is the case."), BorderLayout.EAST);
            rowUIList.addWideToForm(panel);
        }

        if (numRowsWithIncompleteOptional > 0) {
            JLabel warningLabel = new JLabel(StringUtils.formatPluralS(numRowsWithIncompleteOptional, "step") + " with missing optional data",
                    UIUtils.getIconFromResources("emblems/emblem-important.png"),
                    JLabel.LEFT);
            warningLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(warningLabel, BorderLayout.CENTER);
            panel.add(UIUtils.createPopupHelpButton("There are iteration steps where optional slots are not assigned a data item. " +
                    "Optional slots do not necessarily need an input, but if this message is unexpected, please " +
                    "check the annotations in the 'Input data' section to find out why this is the case."), BorderLayout.EAST);
            rowUIList.addWideToForm(panel);
        }

        if (numRowsWithMerging > 0) {
            JLabel infoLabel = new JLabel(StringUtils.formatPluralS(numRowsWithMerging, "step") + " with multiple data per slot",
                    UIUtils.getIconFromResources("emblems/emblem-information.png"),
                    JLabel.LEFT);
            infoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(infoLabel, BorderLayout.CENTER);
            panel.add(UIUtils.createPopupHelpButton("There are iteration steps multiple data items are assigned to one slot. " +
                    "If this is unexpected, please check the annotations in the 'Input data' section to find out why this is the case."), BorderLayout.EAST);
            rowUIList.addWideToForm(panel);
        }
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (isDisplayable() && "preview-size".equals(event.getKey())) {
            reloadTable();
        }
    }

    /**
     * Renders the column header of {@link JIPipeDesktopDataBatchAssistantTableModel}
     */
    public static class WrapperColumnHeaderRenderer implements TableCellRenderer {
        private final Store<JIPipeDataTable> dataTableStore;

        /**
         * Creates a new instance
         *
         * @param dataTable The table
         */
        public WrapperColumnHeaderRenderer(Store<JIPipeDataTable> dataTable) {
            this.dataTableStore = dataTable;
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
                if (columnIndex >= dataTable.getDataAnnotationColumnNames().size() + 2)
                    return columnIndex - dataTable.getDataAnnotationColumnNames().size() - 2;
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
                if (columnIndex < dataTable.getDataAnnotationColumnNames().size() + 4 && (columnIndex - 2) < dataTable.getDataAnnotationColumnNames().size()) {
                    return columnIndex - 2;
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
            int modelColumn = table.convertColumnIndexToModel(column);
            if (modelColumn < 2) {
                return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else if (toDataAnnotationColumnIndex(modelColumn) != -1) {
                JIPipeDataTable dataTable = dataTableStore.get();
                if (dataTable != null) {
                    String info = dataTable.getDataAnnotationColumnNames().get(toDataAnnotationColumnIndex(modelColumn));
                    String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                            UIUtils.getIconFromResources("data-types/slot.png"),
                            info);
                    return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
                } else {
                    return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
            } else {
                JIPipeDataTable dataTable = dataTableStore.get();
                if (dataTable != null) {
                    String info = dataTable.getTextAnnotationColumnNames().get(toAnnotationColumnIndex(modelColumn));
                    String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                            UIUtils.getIconFromResources("data-types/annotation.png"),
                            info);
                    return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
                } else {
                    return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
            }
        }
    }

    public static class BatchIndexCellRenderer extends JLabel implements TableCellRenderer {

        public BatchIndexCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Integer) {
                setText(value.toString());
                setBackground(Color.getHSBColor(1.0f * (Integer) value / table.getRowCount(), 0.3f, 0.7f));
                setForeground(Color.WHITE);
            }
            if (isSelected) {
                setBorder(BorderFactory.createCompoundBorder(UIUtils.createControlBorder(),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)));
            } else {
                setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            }
            return this;
        }
    }
}
