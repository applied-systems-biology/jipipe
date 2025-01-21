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

package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.nio.file.Path;


public class JIPipeDesktopDataViewerTextAnnotationsPanel extends JIPipeDesktopWorkbenchPanel {

    private JXTable table;
    private ResultsTableData currentData;

    public JIPipeDesktopDataViewerTextAnnotationsPanel(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        initialize();
    }

    public ResultsTableData getCurrentData() {
        return currentData;
    }

    public void setCurrentData(ResultsTableData currentData) {
        this.currentData = currentData;
        table.setModel(currentData);
        UIUtils.packDataTable(table);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(true);
        tableContainer.setBackground(UIManager.getColor("Table.background"));
        table = new JXTable();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(UIManager.getColor("Table.background"));
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        tableContainer.add(table.getTableHeader(), BorderLayout.NORTH);
        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setDefaultRenderer(String.class, new Renderer());
        table.setDefaultRenderer(Double.class, new Renderer());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton menuButton = new JButton(UIUtils.getIconFromResources("actions/hamburger-menu.png"));
        menuButton.setToolTipText("Menu");
        UIUtils.makeButtonFlat25x25(menuButton);
        JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(menuButton);

        popupMenu.add(UIUtils.createMenuItem("Open in editor",
                "Opens the table in a dedicated editor window",
                UIUtils.getIconFromResources("actions/open-in-new-window.png"),
                this::exportToTableEditor));
        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Export as *.csv",
                "Exports the table as CSV file",
                UIUtils.getIconFromResources("actions/document-export.png"),
                this::exportAsCSV));
        popupMenu.add(UIUtils.createMenuItem("Export as *.xlsx",
                "Exports the table as Excel file",
                UIUtils.getIconFromResources("actions/document-export.png"),
                this::exportAsXLSX));

        toolBar.add(menuButton);

        add(toolBar, BorderLayout.NORTH);
        add(tableContainer, BorderLayout.CENTER);
    }

    private void exportAsXLSX() {
        if (currentData == null)
            return;
        Path path = JIPipeFileChooserApplicationSettings.saveFile(this,
                JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data,
                "Export as *.xlsx",
                UIUtils.EXTENSION_FILTER_XLSX);
        if (path != null) {
            currentData.saveAsXLSX(path);
        }
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
        Path path = JIPipeFileChooserApplicationSettings.saveFile(this,
                JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data,
                "Export as *.csv",
                UIUtils.EXTENSION_FILTER_CSV);
        if (path != null) {
            currentData.saveAsCSV(path);
        }
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
