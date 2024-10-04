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
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataAnnotationInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopDataInfoCellRenderer;
import org.hkijena.jipipe.desktop.app.resultanalysis.renderers.*;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDataPreviewControlUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallToggleButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextFieldTableRowFilter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.ui.ExpressionBuilderUI;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralDataApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
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
 * UI that displays the {@link JIPipeDataTableInfo} of an {@link JIPipeDataSlot}
 */
public class JIPipeDesktopResultDataSlotTableUI extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final JIPipeDataSlot slot;
    private final JIPipeDesktopSearchTextField searchTextField = new JIPipeDesktopSearchTextField();
    private final JIPipeDesktopRibbon ribbon = new JIPipeDesktopRibbon();
    private final JIPipeProject project;
    private final Path storagePath;
    private JXTable table;
    private JIPipeDataTableInfo dataTable;
    private JIPipeDesktopFormPanel rowUIList;
    private JIPipeDesktopRowDataTableCellRenderer previewRenderer;
    private JIPipeDesktopRowDataAnnotationTableCellRenderer dataAnnotationPreviewRenderer;


    public JIPipeDesktopResultDataSlotTableUI(JIPipeDesktopProjectWorkbench projectWorkbench, JIPipeProject project, Path storagePath, JIPipeDataSlot slot) {
        super(projectWorkbench);
        this.project = project;
        this.storagePath = storagePath;
        this.slot = slot;

        initialize();
        reloadTable();
        JIPipeGeneralDataApplicationSettings.getInstance().getParameterChangedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        table = new JXTable();
        if (JIPipeGeneralDataApplicationSettings.getInstance().isGenerateResultPreviews())
            table.setRowHeight(JIPipeGeneralDataApplicationSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(table);

        table.setDefaultRenderer(Path.class, new JIPipeDesktopRowIndexTableCellRenderer());
        table.setDefaultRenderer(JIPipeDataInfo.class, new JIPipeDesktopDataInfoCellRenderer());
        previewRenderer = new JIPipeDesktopRowDataTableCellRenderer(getDesktopProjectWorkbench(), slot, table, scrollPane);
        dataAnnotationPreviewRenderer = new JIPipeDesktopRowDataAnnotationTableCellRenderer(getDesktopProjectWorkbench(), slot, table, scrollPane);
        table.setDefaultRenderer(JIPipeDataTableRowInfo.class, previewRenderer);
        table.setDefaultRenderer(JIPipeDataAnnotationInfo.class, dataAnnotationPreviewRenderer);
        table.setDefaultRenderer(JIPipeTextAnnotation.class, new JIPipeDesktopAnnotationTableCellRenderer());
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

        rowUIList = new JIPipeDesktopFormPanel(null, JIPipeDesktopParameterFormPanel.WITH_SCROLLING);
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
        JIPipeDesktopRibbon.Task viewTask = ribbon.addTask("Table");
        JIPipeDesktopRibbon.Band tableBand = viewTask.addBand("General");
        JIPipeDesktopRibbon.Band previewBand = viewTask.addBand("Previews");
        JIPipeDesktopRibbon.Band dataBand = viewTask.addBand("Data");

        // Table band
        tableBand.add(new JIPipeDesktopSmallButtonRibbonAction("Fit columns", "Fits the table columns to their contents", UIUtils.getIconFromResources("actions/zoom-fit-width.png"), table::packAll));
        tableBand.add(new JIPipeDesktopSmallButtonRibbonAction("Compact columns", "Auto-size columns to the default size", UIUtils.getIconFromResources("actions/zoom-fit-width.png"), () -> UIUtils.packDataTable(table)));

        // Preview band
        previewBand.add(new JIPipeDesktopSmallToggleButtonRibbonAction("Enable previews", "Allows to toggle previews on and off", UIUtils.getIconFromResources("actions/zoom.png"), JIPipeGeneralDataApplicationSettings.getInstance().isGenerateResultPreviews(), (toggle) -> {
            JIPipeGeneralDataApplicationSettings.getInstance().setGenerateResultPreviews(toggle.isSelected());
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
        UIUtils.openFileInNative(slot.getSlotStoragePath());
    }

    private void exportFilesByMetadata() {
        JIPipeResultCopyFilesByMetadataExporterRun run = new JIPipeResultCopyFilesByMetadataExporterRun(getDesktopWorkbench(), Collections.singletonList(slot), false);
        if (run.setup()) {
            JIPipeRunnableQueue.getInstance().enqueue(run);
        }
    }

    private void exportMetadataAsTableEditor() {
        ResultsTableData tableData = dataTable.toAnnotationTable();
        JIPipeDesktopTableEditor.openWindow(getDesktopWorkbench(), tableData, "Metadata");
    }

    private void exportMetadataAsFiles() {
        Path path = JIPipeFileChooserApplicationSettings.saveFile(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Export as file", UIUtils.EXTENSION_FILTER_CSV, UIUtils.EXTENSION_FILTER_XLSX);
        if (path != null) {
            ResultsTableData tableData = dataTable.toAnnotationTable();
            if (UIUtils.EXTENSION_FILTER_XLSX.accept(path.toFile())) {
                tableData.saveAsXLSX(path);
            } else {
                tableData.saveAsCSV(path);
            }
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow, int selectedColumn) {
        int row = table.getRowSorter().convertRowIndexToModel(selectedRow);
        int dataAnnotationColumn = selectedColumn >= 0 ? dataTable.toDataAnnotationColumnIndex(table.convertColumnIndexToModel(selectedColumn)) : -1;
        JIPipeDataTableRowInfo rowInstance = dataTable.getRowList().get(row);
        JIPipeDesktopResultDataSlotRowUI ui = JIPipe.getDataTypes().getUIForResultSlot(getDesktopProjectWorkbench(), slot, rowInstance);
        ui.handleDefaultActionOrDisplayDataAnnotation(dataAnnotationColumn);
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();
        for (int viewRow : selectedRows) {
            int row = table.getRowSorter().convertRowIndexToModel(viewRow);
            JIPipeDataTableRowInfo rowInstance = dataTable.getRowList().get(row);
            JLabel nameLabel = new JLabel("" + rowInstance.getIndex(), JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getDataTableTooltip(slot));
            JIPipeDesktopResultDataSlotRowUI rowUI = JIPipe.getDataTypes().getUIForResultSlot(getDesktopProjectWorkbench(), slot, rowInstance);
            rowUIList.addToForm(rowUI, nameLabel, null);
        }
    }

    private void reloadTable() {
        dataTable = JIPipeDataTableInfo.loadFromJson(slot.getSlotStoragePath().resolve("data-table.json"));
        if (JIPipeGeneralDataApplicationSettings.getInstance().isGenerateResultPreviews())
            table.setRowHeight(JIPipeGeneralDataApplicationSettings.getInstance().getPreviewSize());
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
            column.setHeaderRenderer(new JIPipeDesktopDataSlotTableColumnHeaderRenderer(dataTable));
        }
        table.setAutoCreateRowSorter(true);
        table.setRowFilter(new JIPipeDesktopSearchTextFieldTableRowFilter(searchTextField));
        UIUtils.packDataTable(table);

        if (dataTable.getRowCount() == 1) {
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
