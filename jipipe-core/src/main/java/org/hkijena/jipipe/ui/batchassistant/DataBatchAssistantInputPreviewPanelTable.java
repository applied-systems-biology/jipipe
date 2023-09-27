package org.hkijena.jipipe.ui.batchassistant;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.Store;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DataBatchAssistantInputPreviewPanelTable extends JPanel {

    private static final int LIMITED_DATA_LIMIT = 2;
    public static final Color COLOR_HIGHLIGHT = new Color(0xBAE8BA);
    public static final Color COLOR_HIGHLIGHT_CELL = new Color(0xF9FEF9);
    private final DataBatchAssistantInputPreviewPanel previewPanel;
    private final JIPipeInputDataSlot inputSlot;

    private final boolean shouldLimitData;
    private boolean hasLimitedData;
    private final JXTable table = new JXTable();
    private Highlighter tableHighlighter;

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
            add(UIUtils.createInfoLabel("No cached data", "Please click 'Update predecessor cache'", UIUtils.getIcon32FromResources("actions/update-cache.png")), BorderLayout.CENTER);
        } else {
            ResultsTableData model = new ResultsTableData();
            for (Store<JIPipeDataTable> store : stores) {
                JIPipeDataTable dataTable = store.get();
                if (dataTable != null) {
                    AnnotationTableData annotationTable = dataTable.toAnnotationTable(false);

                    // Must copy manually due to limit
                    for (int i = 0; i < annotationTable.getRowCount(); i++) {
                        if(shouldLimitData && model.getRowCount() >= LIMITED_DATA_LIMIT) {
                            hasLimitedData = true;
                            break;
                        }

                        int targetRow = model.addRow();
                        for (int j = 0; j < annotationTable.getColumnCount(); j++) {
                            model.setValueAt(annotationTable.getValueAsString(i, j), targetRow, annotationTable.getColumnName(j));
                        }
                    }
                }

            }

            if (model.getColumnCount() == 0) {
                add(UIUtils.createInfoLabel("No columns", "This input contains no text annotation columns", UIUtils.getIcon32FromResources("actions/tag.png")), BorderLayout.CENTER);
            } else {

                if(hasLimitedData) {
                    int row = model.addRow();
                    for (int i = 0; i < model.getColumnCount(); i++) {
                        model.setValueAt("...", row, i);
                    }
                }

                initializeTable(model);
            }
        }

    }

    private void initializeTable(ResultsTableData model) {

        table.setModel(model);
        table.setEditable(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new HeaderCellRenderer());
        }

        // Popup menu for copying
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
            }
        });
        JPopupMenu popupMenu = UIUtils.addRightClickPopupMenuToComponent(table);
        popupMenu.add(UIUtils.createMenuItem("Copy cell value", "Copies the value of the highlighted cell", UIUtils.getIconFromResources("actions/edit-copy.png"), () -> {
            int viewRow = table.getSelectedRow();
            int viewColumn = table.getSelectedColumn();
            if (viewRow >= 0 && viewColumn >= 0) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                int modelColumn = table.convertColumnIndexToModel(viewColumn);
                UIUtils.copyToClipboard(model.getValueAsString(modelRow, modelColumn));
            }
        }));
        popupMenu.add(UIUtils.createMenuItem("Copy annotation name", "Copies the column name of the highlighted column", UIUtils.getIconFromResources("actions/edit-copy.png"), () -> {
            int viewColumn = table.getSelectedColumn();
            if (viewColumn >= 0) {
                int modelColumn = table.convertColumnIndexToModel(viewColumn);
                UIUtils.copyToClipboard(model.getColumnName(modelColumn));
            }
        }));

        add(UIUtils.boxVertical(table.getTableHeader(), table), BorderLayout.CENTER);
    }

    public void highlightResults(JIPipeDataBatchGenerationResult dataBatchGenerationResult) {

        // Header highlight
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            ((HeaderCellRenderer)column.getHeaderRenderer()).setHighlightedColumns(dataBatchGenerationResult.getReferenceTextAnnotationColumns());
        }

        // Cell highlighters
        if(tableHighlighter != null) {
            table.removeHighlighter(tableHighlighter);
        }

        final HighlightPredicate predicate = (renderer, adapter) -> dataBatchGenerationResult.getReferenceTextAnnotationColumns()
                .contains(adapter.getColumnName(adapter.convertColumnIndexToModel(adapter.column)));

        ColorHighlighter highlighter = new ColorHighlighter(
                predicate,
                COLOR_HIGHLIGHT_CELL,   // background color
                null);       // no change in foreground color

        tableHighlighter = highlighter;
        table.addHighlighter(highlighter);

        SwingUtilities.invokeLater(() -> {
          revalidate();
          repaint(50);
        });
    }

    private static class HeaderCellRenderer implements TableCellRenderer {

        private Set<String > highlightedColumns = new HashSet<>();

        public Set<String> getHighlightedColumns() {
            return highlightedColumns;
        }

        public void setHighlightedColumns(Set<String> highlightedColumns) {
            this.highlightedColumns = highlightedColumns;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
            int modelColumn = table.convertColumnIndexToModel(column);
            String columnName = table.getModel().getColumnName(modelColumn);
            String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                    UIUtils.getIconFromResources("data-types/annotation.png"),
                    StringUtils.nullToEmpty(value));

            Component component = defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);

            if(highlightedColumns.contains(columnName)) {
                component.setBackground(COLOR_HIGHLIGHT);
                component.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
            }
            else {
                component.setBackground(UIManager.getColor("TableHeader.background"));
                component.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            }

            return component;
        }
    }
}
