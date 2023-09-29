package org.hkijena.jipipe.ui.batchassistant;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.ui.cache.JIPipeDataTableRowUI;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeDefaultResultDataSlotRowUI;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.Store;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.border.DropShadowBorder;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class DataBatchAssistantInputPreviewPanelTable extends JPanel {

    private static final int LIMITED_DATA_LIMIT = 2;
    public static final Color COLOR_HIGHLIGHT = new Color(0xBAE8BA);
    public static final Color COLOR_HIGHLIGHT_CELL = new Color(0xF9FEF9);
    private final DataBatchAssistantInputPreviewPanel previewPanel;
    private final JIPipeInputDataSlot inputSlot;

    private final boolean shouldLimitData;
    private boolean hasLimitedData;
    private final JXTable table = new JXTable();
    private final DefaultTableModel model = new DefaultTableModel();
    private JIPipeDataBatchGenerationResult dataBatchGenerationResult;
    private final Multimap<Integer, Integer> dataBatchMapping = HashMultimap.create();

    public DataBatchAssistantInputPreviewPanelTable(DataBatchAssistantInputPreviewPanel previewPanel, JIPipeInputDataSlot inputSlot, boolean shouldLimitData) {
        this.previewPanel = previewPanel;
        this.inputSlot = inputSlot;
        this.shouldLimitData = shouldLimitData;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(new DropShadowBorder(Color.BLACK,
                5,
                0.2f,
                12,
                true,
                true,
                true,
                true), BorderFactory.createLineBorder(Color.LIGHT_GRAY)));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(UIManager.getColor("Panel.background"));

        add(toolBar, BorderLayout.NORTH);

        JLabel slotLabel = new JLabel(inputSlot.getName(), JIPipe.getDataTypes().getIconFor(inputSlot.getAcceptedDataType()), JLabel.LEFT);
        slotLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        toolBar.add(slotLabel);

        Collection<Store<JIPipeDataTable>> stores = previewPanel.getDataBatchAssistantUI().getCurrentCache().get(inputSlot.getName());

        if (stores.isEmpty()) {
//            add(UIUtils.createInfoLabel("No cached data", "Please run 'Update predecessor cache'", UIUtils.getIcon32FromResources("actions/update-cache.png")), BorderLayout.CENTER);
            JButton updateCacheButton = new JButton("<html><strong>No cached data</strong><br/>Click here to update the predecessor cache</html>", UIUtils.getIcon32FromResources("actions/cache-predecessors.png"));
            updateCacheButton.setHorizontalAlignment(SwingConstants.LEFT);
            updateCacheButton.addActionListener(e -> previewPanel.getDataBatchAssistantUI().updatePredecessorCache());
            updateCacheButton.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
            add(updateCacheButton, BorderLayout.CENTER);
        } else {
            createModel(stores);
            initializeTable();
        }
    }

    private void createModel(Collection<Store<JIPipeDataTable>> stores) {
        // Collect data
        List<JIPipeWeakDataTableDataSource> sourceColumn = new ArrayList<>();
        Map<String, List<JIPipeTextAnnotation>> annotationColumns = new HashMap<>();

        for (Store<JIPipeDataTable> store : stores) {
            JIPipeDataTable dataTable = store.get();
            if (dataTable != null) {
                AnnotationTableData annotationTable = dataTable.toAnnotationTable(false);

                // Must copy manually due to limit
                for (int i = 0; i < annotationTable.getRowCount(); i++) {
                    if (shouldLimitData && sourceColumn.size() >= LIMITED_DATA_LIMIT) {
                        hasLimitedData = true;
                        break;
                    }

                    // Track the source
                    sourceColumn.add(new JIPipeWeakDataTableDataSource(dataTable, i));

                    // Add the annotations
                    for (JIPipeTextAnnotation textAnnotation : dataTable.getTextAnnotations(i)) {
                        List<JIPipeTextAnnotation> target = annotationColumns.getOrDefault(textAnnotation.getName(), null);
                        if (target == null) {
                            target = new ArrayList<>();
                            for (int j = 0; j < sourceColumn.size() - 1; j++) {
                                target.add(null);
                            }
                            annotationColumns.put(textAnnotation.getName(), target);
                        }
                        target.add(textAnnotation);
                    }
                }
            }
        }

        // Create columns
        model.addColumn("{data}", sourceColumn.toArray());
        model.addColumn("{id}", sourceColumn.toArray());
        for (Map.Entry<String, List<JIPipeTextAnnotation>> entry : annotationColumns.entrySet()) {
            model.addColumn(entry.getKey(), entry.getValue().toArray(new JIPipeTextAnnotation[0]));
        }

        if (hasLimitedData) {
            String[] arr = new String[model.getColumnCount()];
            Arrays.fill(arr, "...");
            model.addRow(arr);
        }
    }

    private void initializeTable() {

        table.setModel(model);
        table.setEditable(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new HeaderCellRenderer(this));
        }
        table.setDefaultRenderer(Object.class, new ContentCellRenderer(this));

        // Popup menu for copying
        initializeTableContextMenu();

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
        tablePanel.add(table, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);

        if (model.getColumnCount() > 0) {
            table.getColumnExt(0).setMinWidth(100);
            table.getColumnExt(0).setMaxWidth(100);
            table.getColumnExt(0).setPreferredWidth(100);
        }
        if (model.getColumnCount() > 1) {
            table.getColumnExt(1).setMinWidth(100);
            table.getColumnExt(1).setMaxWidth(100);
            table.getColumnExt(1).setPreferredWidth(100);
        }
    }

    private void initializeTableContextMenu() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        table.changeSelection(row, col, false, false);
                    }
                }
                else if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    displaySelectedData();
                }
            }
        });
        JPopupMenu popupMenu = UIUtils.addRightClickPopupMenuToComponent(table);
        popupMenu.add(UIUtils.createMenuItem("Copy cell value", "Copies the value of the highlighted cell", UIUtils.getIconFromResources("actions/edit-copy.png"), () -> {
            int viewRow = table.getSelectedRow();
            int viewColumn = table.getSelectedColumn();
            if (viewRow >= 0 && viewColumn >= 0) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                int modelColumn = table.convertColumnIndexToModel(viewColumn);
                Object value = model.getValueAt(modelRow, modelColumn);
                if (value instanceof JIPipeTextAnnotation) {
                    UIUtils.copyToClipboard(((JIPipeTextAnnotation) value).getValue());
                }
            }
        }));
        popupMenu.add(UIUtils.createMenuItem("Copy annotation name", "Copies the column name of the highlighted column", UIUtils.getIconFromResources("actions/edit-copy.png"), () -> {
            int viewColumn = table.getSelectedColumn();
            if (viewColumn >= 0) {
                int modelColumn = table.convertColumnIndexToModel(viewColumn);
                UIUtils.copyToClipboard(model.getColumnName(modelColumn));
            }
        }));
        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Display data", "Displays the selected data", UIUtils.getIconFromResources("actions/search.png"), this::displaySelectedData));
    }

    private void displaySelectedData() {
        int viewRow = table.getSelectedRow();
        int viewColumn = table.getSelectedColumn();
        if (viewRow >= 0 && viewColumn >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            Object value = model.getValueAt(modelRow, 0);
            if(value instanceof JIPipeWeakDataTableDataSource) {
                JIPipeWeakDataTableDataSource dataSource = (JIPipeWeakDataTableDataSource) value;
                JIPipeDataTable dataTable = dataSource.getDataTable();
                if(dataTable != null) {
                    JIPipeData data = dataTable.getData(dataSource.getRow(), JIPipeData.class, new JIPipeProgressInfo());
                    JIPipeDataDisplayOperation mainOperation = JIPipeDataTableRowUI.getMainOperation(data.getClass());
                    if(mainOperation != null) {
                        mainOperation.display(dataTable, dataSource.getRow(), previewPanel.getWorkbench(), false);
                    }
                    else {
                        data.display(data.toString(), previewPanel.getWorkbench(), dataSource);
                    }
                }
            }
        }
    }

    public void highlightResults(JIPipeDataBatchGenerationResult dataBatchGenerationResult) {

        this.dataBatchGenerationResult = dataBatchGenerationResult;

        // Generate data batch mapping
        this.dataBatchMapping.clear();
        List<JIPipeMergingDataBatch> dataBatches = dataBatchGenerationResult.getDataBatches();
        for (int dataBatchIndex = 0; dataBatchIndex < dataBatches.size(); dataBatchIndex++) {
            JIPipeMergingDataBatch dataBatch = dataBatches.get(dataBatchIndex);
            for (Integer inputRow : dataBatch.getInputRows(inputSlot.getName())) {
                dataBatchMapping.put(inputRow, dataBatchIndex);
            }
        }


        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint(50);
        });
    }

    private static class ContentCellRenderer extends JLabel implements TableCellRenderer {

        private final DataBatchAssistantInputPreviewPanelTable previewPanelTable;

        private final Color defaultForeground = UIManager.getColor("Table.foreground");
        private final Color defaultBackground = UIManager.getColor("Table.background");

        private ContentCellRenderer(DataBatchAssistantInputPreviewPanelTable previewPanelTable) {
            this.previewPanelTable = previewPanelTable;
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            setForeground(defaultForeground);
            setBackground(defaultBackground);
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            setIcon(null);

            int dataBatchIndex = -1;
            Collection<Integer> indices = null;
            if (previewPanelTable.dataBatchMapping != null && table.getModel().getColumnCount() > 0) {
                Object obj = table.getModel().getValueAt(table.convertRowIndexToView(row), 0);
                if(obj instanceof JIPipeWeakDataTableDataSource) {
                    JIPipeWeakDataTableDataSource dataSource = (JIPipeWeakDataTableDataSource) obj;
                    indices = previewPanelTable.dataBatchMapping.get(dataSource.getRow());
                    if (indices.size() > 1) {
                        dataBatchIndex = Integer.MAX_VALUE;
                    } else if (indices.size() == 1) {
                        dataBatchIndex = indices.iterator().next();
                    }
                }
            }


            if (value instanceof JIPipeTextAnnotation) {
                JIPipeTextAnnotation textAnnotation = (JIPipeTextAnnotation) value;

                String columnName = table.getColumnName(column);
                if (previewPanelTable.dataBatchGenerationResult != null && previewPanelTable.dataBatchGenerationResult.getReferenceTextAnnotationColumns().contains(columnName)) {
                    setBackground(COLOR_HIGHLIGHT_CELL);
                    setFont(new Font(Font.DIALOG, Font.BOLD, 12));
                }
                setText(HtmlEscapers.htmlEscaper().escape(StringUtils.nullToEmpty(textAnnotation.getValue())));

                if(dataBatchIndex >= 0 && dataBatchIndex != Integer.MAX_VALUE) {
                    setForeground(Color.getHSBColor(1.0f * dataBatchIndex / previewPanelTable.dataBatchGenerationResult.getDataBatches().size(), 0.6f, 0.5f));
                }

            } else if (value instanceof JIPipeWeakDataTableDataSource) {

                if(table.convertColumnIndexToModel(column) == 0) {
                    // Display the data
                    JIPipeWeakDataTableDataSource dataSource = (JIPipeWeakDataTableDataSource) value;
                    JIPipeDataTable dataTable = dataSource.getDataTable();
                    if(dataTable != null && dataSource.getRow() < dataTable.getRowCount()) {
                        JIPipeDataItemStore dataItemStore = dataTable.getDataItemStore(dataSource.getRow());
                        setText(dataItemStore.getStringRepresentation());
                        setIcon(JIPipe.getDataTypes().getIconFor(dataItemStore.getDataClass()));
                    }
                    else {
                        setText("NA");
                        setIcon(UIUtils.getIconFromResources("actions/edit-clear-all.png"));
                    }
                }
                else {
                    if (dataBatchIndex == Integer.MAX_VALUE) {
                        setIcon(UIUtils.getIconInvertedFromResources("actions/go-right.png"));
                        setForeground(Color.BLUE);
                        setText(indices.stream().sorted().map(Object::toString).collect(Collectors.joining(", ")));
                    } else if (dataBatchIndex >= 0) {
                        setIcon(UIUtils.getIconInvertedFromResources("actions/go-right.png"));
                        setText(String.valueOf(dataBatchIndex));
                        setBackground(Color.getHSBColor(1.0f * dataBatchIndex / previewPanelTable.dataBatchGenerationResult.getDataBatches().size(), 0.3f, 0.7f));
                        setForeground(Color.WHITE);
                    } else {
                        setForeground(Color.LIGHT_GRAY);
                        setText("NA");
                    }
                }

            } else {
                String columnName = table.getColumnName(column);
                if (previewPanelTable.dataBatchGenerationResult != null && previewPanelTable.dataBatchGenerationResult.getReferenceTextAnnotationColumns().contains(columnName)) {
                    setBackground(COLOR_HIGHLIGHT_CELL);
                    setFont(new Font(Font.DIALOG, Font.BOLD, 12));
                }
                setText(StringUtils.nullToEmpty(value));
            }

            if (isSelected) {
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.DARK_GRAY),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)));
            } else {
                setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            }

            return this;
        }
    }

    private static class HeaderCellRenderer implements TableCellRenderer {

        private final DataBatchAssistantInputPreviewPanelTable previewPanelTable;

        private HeaderCellRenderer(DataBatchAssistantInputPreviewPanelTable previewPanelTable) {
            this.previewPanelTable = previewPanelTable;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
            int modelColumn = table.convertColumnIndexToModel(column);
            String columnName = table.getModel().getColumnName(modelColumn);
            String html;
            if (modelColumn == 0) {
                html = "Data";
            }
            else if(modelColumn == 1) {
                html = "Iteration step";
            }
            else {
                html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                        UIUtils.getIconFromResources("data-types/annotation.png"),
                        StringUtils.nullToEmpty(value));
            }

            Component component = defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);

            if (previewPanelTable.dataBatchGenerationResult != null &&
                    previewPanelTable.dataBatchGenerationResult.getReferenceTextAnnotationColumns().contains(columnName)) {
                component.setBackground(COLOR_HIGHLIGHT);
                component.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
            } else {
                component.setBackground(UIManager.getColor("TableHeader.background"));
                component.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            }

            return component;
        }
    }

}
