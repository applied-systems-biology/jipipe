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

package org.hkijena.jipipe.ui.cache;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.renderers.JIPipeComponentCellRenderer;
import org.hkijena.jipipe.ui.components.PreviewControlUI;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.search.SearchTextFieldTableRowFilter;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeAnnotationTableCellRenderer;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeNodeTableCellRenderer;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeProjectCompartmentTableCellRenderer;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UI that displays a {@link JIPipeDataSlot} that is cached
 */
public class JIPipeCacheMultiDataSlotTableUI extends JIPipeWorkbenchPanel {

    private final List<JIPipeDataSlot> slots;
    private final boolean withCompartmentAndAlgorithm;
    private JIPipeMergedDataSlotTableModel multiSlotTable;
    private JXTable table;
    private FormPanel rowUIList;
    private SearchTextField searchTextField = new SearchTextField();

    /**
     * @param workbenchUI                 the workbench UI
     * @param slots                       The slots
     * @param withCompartmentAndAlgorithm
     */
    public JIPipeCacheMultiDataSlotTableUI(JIPipeWorkbench workbenchUI, List<JIPipeDataSlot> slots, boolean withCompartmentAndAlgorithm) {
        super(workbenchUI);
        this.slots = slots;
        this.withCompartmentAndAlgorithm = withCompartmentAndAlgorithm;
        table = new JXTable();
        this.multiSlotTable = new JIPipeMergedDataSlotTableModel(table, withCompartmentAndAlgorithm);
        JIPipeProject project = null;
        if (getWorkbench() instanceof JIPipeProjectWorkbench) {
            project = ((JIPipeProjectWorkbench) getWorkbench()).getProject();
        }
        for (JIPipeDataSlot slot : slots) {
            multiSlotTable.add(project, slot);
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
        table.setRowFilter(new SearchTextFieldTableRowFilter(searchTextField));
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
        table.setDefaultRenderer(JIPipeAnnotation.class, new JIPipeAnnotationTableCellRenderer());
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
                        handleSlotRowDefaultAction(selectedRows[0]);
                }
            }
        });

        rowUIList = new FormPanel(null, ParameterPanel.WITH_SCROLLING);
        add(rowUIList, BorderLayout.SOUTH);

        // Toolbar for searching and export
        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);
        toolBar.setFloatable(false);

        searchTextField.addActionListener(e -> reloadTable());
        toolBar.add(searchTextField);

        JButton exportButton = new JButton("Export table", UIUtils.getIconFromResources("actions/document-export.png"));
        toolBar.add(exportButton);
        JPopupMenu exportMenu = UIUtils.addPopupMenuToComponent(exportButton);

        JMenuItem exportAsTableItem = new JMenuItem("Metadata as table", UIUtils.getIconFromResources("actions/link.png"));
        exportAsTableItem.addActionListener(e -> exportAsTable());
        exportMenu.add(exportAsTableItem);

        JMenuItem exportAsCsvItem = new JMenuItem("Metadata as *.csv", UIUtils.getIconFromResources("data-types/results-table.png"));
        exportAsCsvItem.addActionListener(e -> exportAsCSV());
        exportMenu.add(exportAsCsvItem);

        JMenuItem exportStandardizedSlotItem = new JMenuItem("Data as JIPipe slot output", UIUtils.getIconFromResources("apps/jipipe.png"));
        exportStandardizedSlotItem.addActionListener(e -> exportAsJIPipeSlot());
        exportMenu.add(exportStandardizedSlotItem);

        JMenuItem exportByMetadataExporterItem = new JMenuItem("Data as files", UIUtils.getIconFromResources("actions/save.png"));
        exportByMetadataExporterItem.addActionListener(e -> exportByMetadataExporter());
        exportMenu.add(exportByMetadataExporterItem);

        JButton autoSizeButton = new JButton(UIUtils.getIconFromResources("actions/zoom-fit-width.png"));
        autoSizeButton.setToolTipText("Auto-size columns to fit their contents");
        autoSizeButton.addActionListener(e -> table.packAll());
        UIUtils.makeFlat25x25(autoSizeButton);
        toolBar.add(autoSizeButton);

        JButton smallSizeButton = new JButton(UIUtils.getIconFromResources("actions/zoom-best-fit.png"));
        smallSizeButton.setToolTipText("Auto-size columns to the default size");
        smallSizeButton.addActionListener(e -> UIUtils.packDataTable(table));
        UIUtils.makeFlat25x25(smallSizeButton);
        toolBar.add(smallSizeButton);

        toolBar.addSeparator();

        PreviewControlUI previewControlUI = new PreviewControlUI();
        toolBar.add(previewControlUI);
    }

    private void exportByMetadataExporter() {
        JIPipeCachedSlotToFilesByMetadataExporterRun run = new JIPipeCachedSlotToFilesByMetadataExporterRun(getWorkbench(), slots, true);
        if (run.setup()) {
            JIPipeRunnerQueue.getInstance().enqueue(run);
        }
    }

    private void exportAsJIPipeSlot() {
        Path path = FileChooserSettings.openDirectory(this, FileChooserSettings.LastDirectoryKey.Data, "Export data as JIPipe output slot");
        if (path != null) {
            JIPipeCachedSlotToOutputExporterRun run = new JIPipeCachedSlotToOutputExporterRun(getWorkbench(), path, slots, true);
            JIPipeRunnerQueue.getInstance().enqueue(run);
        }
    }


    private void exportAsTable() {
        AnnotationTableData tableData = new AnnotationTableData();
        for (JIPipeDataSlot slot : multiSlotTable.getSlotList()) {
            tableData.addRows(slot.toAnnotationTable(true));
        }
        Set<String> nodes = new HashSet<>();
        for (JIPipeDataSlot dataSlot : multiSlotTable.getSlotList()) {
            nodes.add(dataSlot.getNode().getDisplayName());
        }
        TableEditor.openWindow(getWorkbench(), tableData, nodes.stream().sorted().collect(Collectors.joining(", ")));
    }

    private void exportAsCSV() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export as *.csv", UIUtils.EXTENSION_FILTER_CSV);
        if (path != null) {
            AnnotationTableData tableData = new AnnotationTableData();
            for (JIPipeDataSlot slot : multiSlotTable.getSlotList()) {
                tableData.addRows(slot.toAnnotationTable(true));
            }
            tableData.saveAsCSV(path);
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow) {
        int multiRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
        JIPipeDataSlot slot = multiSlotTable.getSlot(multiRow);
        int row = multiSlotTable.getRow(multiRow);
        JIPipeDataSlotRowUI rowUI = new JIPipeDataSlotRowUI(getWorkbench(), slot, row);
        rowUI.handleDefaultAction();
//        slot.getData(row, JIPipeData.class).display(slot.getNode().getName() + "/" + slot.getName() + "/" + row, getWorkbench());
    }

    private void showDataRows(int[] selectedRows) {
        rowUIList.clear();

        JLabel infoLabel = new JLabel();
        int rowCount = slots.stream().mapToInt(JIPipeDataSlot::getRowCount).sum();
        infoLabel.setText(rowCount + " rows" + (slots.size() > 1 ? " across " + slots.size() + " tables" : "") + (selectedRows.length > 0 ? ", " + selectedRows.length + " selected" : ""));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        rowUIList.addWideToForm(infoLabel, null);

        for (int viewRow : selectedRows) {
            int multiRow = table.getRowSorter().convertRowIndexToModel(viewRow);
            JIPipeDataSlot slot = multiSlotTable.getSlot(multiRow);
            int row = multiSlotTable.getRow(multiRow);
            Class<? extends JIPipeData> dataClass = slot.getDataClass(row);
            String name = slot.getNode().getName() + "/" + slot.getName() + "/" + row;
            JLabel nameLabel = new JLabel(name, JIPipe.getDataTypes().getIconFor(dataClass), JLabel.LEFT);
            nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(slot));
            JIPipeDataSlotRowUI JIPipeDataSlotRowUI = new JIPipeDataSlotRowUI(getWorkbench(), slot, row);
            rowUIList.addToForm(JIPipeDataSlotRowUI, nameLabel, null);
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
        for (JIPipeDataSlot slot : slots) {
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
        private final JIPipeMergedDataSlotTableModel dataTable;

        /**
         * Creates a new instance
         *
         * @param dataTable The table
         */
        public MultiDataSlotTableColumnRenderer(JIPipeMergedDataSlotTableModel dataTable) {
            this.dataTable = dataTable;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JIPipeMergedDataSlotTableModel model = (JIPipeMergedDataSlotTableModel) table.getModel();
            TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
            int modelColumn = table.convertColumnIndexToModel(column);
            int spacer = model.isWithCompartmentAndAlgorithm() ? 6 : 4;
            if (modelColumn < spacer) {
                return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else if (dataTable.toDataAnnotationColumnIndex(modelColumn) != -1) {
                String info = dataTable.getDataAnnotationColumns().get(dataTable.toDataAnnotationColumnIndex(modelColumn));
                String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                        UIUtils.getIconFromResources("data-types/data-annotation.png"),
                        info);
                return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
            } else {
                String info = dataTable.getAnnotationColumns().get(dataTable.toAnnotationColumnIndex(modelColumn));
                String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                        UIUtils.getIconFromResources("data-types/annotation.png"),
                        info);
                return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
            }
        }
    }
}
