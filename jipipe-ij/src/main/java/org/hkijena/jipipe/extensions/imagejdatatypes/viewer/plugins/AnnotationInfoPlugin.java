package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins;

import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.nio.file.Path;

/**
 * To be used with {@link org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedImagePlusDataViewerWindow} and other similar implementations that
 * have access to the cache
 */
public class AnnotationInfoPlugin extends ImageViewerPanelPlugin {

    private final JIPipeCacheDataViewerWindow cacheDataViewerWindow;
    private JPanel tableContainer;
    private JXTable table;
    private JToolBar toolBar;
    private ResultsTableData currentData;

    public AnnotationInfoPlugin(ImageViewerPanel viewerPanel, JIPipeCacheDataViewerWindow cacheDataViewerWindow) {
        super(viewerPanel);
        this.cacheDataViewerWindow = cacheDataViewerWindow;
        initialize();
    }

    private void initialize() {
        tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(true);
        table = new JXTable();
        tableContainer.add(new JScrollPane(table), BorderLayout.CENTER);
        tableContainer.add(table.getTableHeader(), BorderLayout.NORTH);
        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setDefaultRenderer(String.class, new Renderer());
        table.setDefaultRenderer(Double.class, new Renderer());

        toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("actions/document-export.png"));
        JPopupMenu exportMenu = UIUtils.addPopupMenuToComponent(exportButton);

        JMenuItem exportToEditorItem = new JMenuItem("Open in editor", UIUtils.getIconFromResources("actions/link.png"));
        exportToEditorItem.addActionListener(e -> exportToTableEditor());
        exportMenu.add(exportToEditorItem);

        JMenuItem exportAsCSVItem = new JMenuItem("as *.csv", UIUtils.getIconFromResources("data-types/results-table.png"));
        exportAsCSVItem.addActionListener(e -> exportAsCSV());
        exportMenu.add(exportAsCSVItem);

        toolBar.add(exportButton);
    }

    private void exportToTableEditor() {
        if (currentData == null)
            return;
        TableEditor.openWindow(getCacheDataViewerWindow().getWorkbench(), new ResultsTableData(currentData), getCacheDataViewerWindow().getTitle() + " - Annotations");
    }

    private void exportAsCSV() {
        if (currentData == null)
            return;
        Path path = FileChooserSettings.saveFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Export as *.csv", UIUtils.EXTENSION_FILTER_CSV);
        if (path != null) {
            currentData.saveAsCSV(path);
        }
    }

    @Override
    public String getCategory() {
        return "Annotations";
    }

    @Override
    public Icon getCategoryIcon() {
        return UIUtils.getIconFromResources("data-types/annotation.png");
    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
        formPanel.addWideToForm(toolBar, null);
        formPanel.addVerticalGlue(tableContainer, null);
    }

    @Override
    public void onImageChanged() {
        ResultsTableData model = new ResultsTableData();
        model.addStringColumn("Name");
        model.addStringColumn("Value");
        if (cacheDataViewerWindow != null && cacheDataViewerWindow.getDataSource() != null) {
            for (JIPipeAnnotation annotation : cacheDataViewerWindow.getDataSource().getSlot().getAnnotations(cacheDataViewerWindow.getDataSource().getRow())) {
                model.addRow();
                model.setLastValue(annotation.getName(), "Name");
                model.setLastValue(annotation.getValue(), "Value");
            }
        }
        currentData = model;
        table.setModel(model);
        UIUtils.packDataTable(table);
    }

    public JIPipeCacheDataViewerWindow getCacheDataViewerWindow() {
        return cacheDataViewerWindow;
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
