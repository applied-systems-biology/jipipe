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

package org.hkijena.jipipe.desktop.app.cache;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.nio.file.Path;


public class JIPipeDesktopCachedDataViewerAnnotationInfoPanel extends JIPipeDesktopWorkbenchPanel {

    private JXTable table;
    private ResultsTableData currentData;

    public JIPipeDesktopCachedDataViewerAnnotationInfoPanel(JIPipeDesktopWorkbench workbench) {
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
        JIPipeDesktopTableEditor.openWindow(getDesktopWorkbench(), new ResultsTableData(currentData),
                UIUtils.getAWTWindowTitle(SwingUtilities.getWindowAncestor(this)) + " - Annotations");
    }

    private void exportAsCSV() {
        if (currentData == null)
            return;
        Path path = JIPipeDesktop.saveFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export as *.csv", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_CSV);
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
