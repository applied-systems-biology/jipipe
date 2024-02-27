package org.hkijena.jipipe.ui.cache;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.nio.file.Path;


public class JIPipeCachedDataViewerAnnotationInfoPanel extends JIPipeWorkbenchPanel {

    private JXTable table;
    private ResultsTableData currentData;

    public JIPipeCachedDataViewerAnnotationInfoPanel(JIPipeWorkbench workbench) {
        super(workbench);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(true);
        tableContainer.setBackground(UIManager.getColor("Table.background"));
        table = new JXTable();
        tableContainer.add(new JScrollPane(table), BorderLayout.CENTER);
        tableContainer.add(table.getTableHeader(), BorderLayout.NORTH);
        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setDefaultRenderer(String.class, new Renderer());
        table.setDefaultRenderer(Double.class, new Renderer());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("actions/document-export.png"));
        JPopupMenu exportMenu = UIUtils.addPopupMenuToButton(exportButton);

        JMenuItem exportToEditorItem = new JMenuItem("Open in editor", UIUtils.getIconFromResources("actions/open-in-new-window.png"));
        exportToEditorItem.addActionListener(e -> exportToTableEditor());
        exportMenu.add(exportToEditorItem);

        JMenuItem exportAsCSVItem = new JMenuItem("as *.csv", UIUtils.getIconFromResources("data-types/results-table.png"));
        exportAsCSVItem.addActionListener(e -> exportAsCSV());
        exportMenu.add(exportAsCSVItem);

        toolBar.add(exportButton);

        add(toolBar, BorderLayout.NORTH);
        add(tableContainer, BorderLayout.CENTER);
    }

    private void exportToTableEditor() {
        if (currentData == null)
            return;
        TableEditor.openWindow(getWorkbench(), new ResultsTableData(currentData),
                UIUtils.getAWTWindowTitle(SwingUtilities.getWindowAncestor(this)) + " - Annotations");
    }

    private void exportAsCSV() {
        if (currentData == null)
            return;
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Export as *.csv", UIUtils.EXTENSION_FILTER_CSV);
        if (path != null) {
            currentData.saveAsCSV(path);
        }
    }

    public void displayAnnotations(JIPipeDataTableDataSource dataSource) {
        ResultsTableData model = new ResultsTableData();
        model.addStringColumn("Name");
        model.addStringColumn("Value");
        if (dataSource != null) {
            for (JIPipeTextAnnotation annotation : dataSource.getDataTable().getTextAnnotations(dataSource.getRow())) {
                model.addRow();
                model.setLastValue(annotation.getName(), "Name");
                model.setLastValue(annotation.getValue(), "Value");
            }
        }
        currentData = model;
        table.setModel(model);
        UIUtils.packDataTable(table);
    }

    public static class Renderer extends JLabel implements TableCellRenderer {

        /**
         * Creates a new renderer
         */
        public Renderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            setText("<html><i><span style=\"color:gray;\">\"</span>" + value + "<span style=\"color:gray;\">\"</span></i></html>");

            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }

            return this;
        }
    }
}
