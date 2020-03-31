/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.tableanalyzer;

import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.components.DocumentTabListCellRenderer;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.utils.TableUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Vector;

/**
 * UI that merges table columns
 */
public class ACAQMergeTableColumnsDialogUI extends JDialog {
    private ACAQTableAnalyzerUI tableAnalyzerUI;
    private DefaultTableModel tableModel;
    private JComboBox<DocumentTabPane.DocumentTab> tableSelection;
    private JXTable jxTable;
    private JTable columnSelection;

    /**
     * @param tableAnalyzerUI The table analyzer
     */
    public ACAQMergeTableColumnsDialogUI(ACAQTableAnalyzerUI tableAnalyzerUI) {
        this.tableAnalyzerUI = tableAnalyzerUI;
        this.tableModel = tableAnalyzerUI.getTableModel();

        for (DocumentTabPane.DocumentTab tab : tableAnalyzerUI.getWorkbenchUI().getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof ACAQTableAnalyzerUI && tab.getContent() != tableAnalyzerUI) {
                tableSelection.addItem(tab);
            }
        }
    }

    /**
     * @param workbenchUI The workbench
     * @param tableModel The table
     */
    public ACAQMergeTableColumnsDialogUI(ACAQProjectUI workbenchUI, DefaultTableModel tableModel) {
        this.tableModel = tableModel;
        initialize();

        for (DocumentTabPane.DocumentTab tab : workbenchUI.getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof ACAQTableAnalyzerUI) {
                tableSelection.addItem(tab);
            }
        }
    }

    /**
     * Finds the tab that contains the selected table
     * @return The tab
     */
    public DocumentTabPane.DocumentTab getMergedTab() {
        if (tableSelection.getSelectedItem() instanceof DocumentTabPane.DocumentTab)
            return (DocumentTabPane.DocumentTab) tableSelection.getSelectedItem();
        else
            return null;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();

        tableSelection = new JComboBox<>();
        tableSelection.setRenderer(new DocumentTabListCellRenderer());
        tableSelection.addItemListener(e -> {
            if (e.getItem() instanceof DocumentTabPane.DocumentTab) {
                jxTable.setModel(((ACAQTableAnalyzerUI) ((DocumentTabPane.DocumentTab) e.getItem()).getContent()).getTableModel());
                jxTable.packAll();
                updateColumnSelection();
            }
        });
        toolBar.add(tableSelection);
        toolBar.add(Box.createHorizontalGlue());
        add(toolBar, BorderLayout.NORTH);

        jxTable = new JXTable();
        jxTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jxTable.setEditable(false);

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.PAGE_AXIS));

        settingsPanel.add(new JLabel("Columns to be copied") {
            {
                setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
            }
        });
        columnSelection = createColumnSelectionTable();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(jxTable), new JScrollPane(columnSelection));
        splitPane.setResizeWeight(0.8);
        add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton calculateButton = new JButton("Merge", UIUtils.getIconFromResources("import.png"));
        calculateButton.addActionListener(e -> calculate());
        buttonPanel.add(calculateButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void updateColumnSelection() {
        DefaultTableModel model = (DefaultTableModel) columnSelection.getModel();
        model.setNumRows(0);

        if (tableSelection.getSelectedItem() != null) {
            DefaultTableModel sourceModel = ((ACAQTableAnalyzerUI) ((DocumentTabPane.DocumentTab) tableSelection.getSelectedItem()).getContent()).getTableModel();
            for (int column = 0; column < sourceModel.getColumnCount(); ++column) {
                model.addRow(new Object[]{true, sourceModel.getColumnName(column)});
            }
        }
    }

    private void calculate() {
        if (tableSelection.getSelectedItem() != null) {
            if (tableAnalyzerUI != null)
                tableAnalyzerUI.createUndoSnapshot();
            DefaultTableModel sourceModel = ((ACAQTableAnalyzerUI) ((DocumentTabPane.DocumentTab) tableSelection.getSelectedItem()).getContent()).getTableModel();
            DefaultTableModel targetModel = tableModel;

            final int targetColumnStartIndex = targetModel.getColumnCount();

            for (int i = 0; i < sourceModel.getColumnCount(); ++i) {
                boolean isSelected = (boolean) columnSelection.getModel().getValueAt(i, 0);
                if (isSelected) {
                    targetModel.addColumn(sourceModel.getColumnName(i));
                }
            }

            if (targetModel.getRowCount() < sourceModel.getRowCount())
                targetModel.setRowCount(sourceModel.getRowCount());

            Vector data = targetModel.getDataVector();
            for (int row = 0; row < sourceModel.getRowCount(); ++row) {
                Vector rowVector = (Vector) data.get(row);
                int targetColumnIndex = 0;
                for (int i = 0; i < sourceModel.getColumnCount(); ++i) {
                    boolean isSelected = (boolean) columnSelection.getModel().getValueAt(i, 0);
                    if (isSelected) {
                        rowVector.set(targetColumnIndex + targetColumnStartIndex, sourceModel.getValueAt(row, i));
                        ++targetColumnIndex;
                    }
                }
            }
            targetModel.setDataVector(data, TableUtils.getColumnIdentifiers(targetModel));
            if (tableAnalyzerUI != null)
                tableAnalyzerUI.autoSizeColumns();

            setVisible(false);
        }
    }

    private static JTable createColumnSelectionTable() {
        JTable columnSelection = new JTable();
        DefaultTableModel columnSelectionmodel = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0)
                    return Boolean.class;
                else if (columnIndex == 1)
                    return String.class;
                return super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
        columnSelectionmodel.setColumnCount(2);
        columnSelection.setModel(columnSelectionmodel);
        columnSelection.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        columnSelection.getColumnModel().getColumn(0).setMaxWidth(20);
        columnSelection.setShowGrid(false);
        columnSelection.setOpaque(false);
        columnSelection.setTableHeader(null);
        columnSelection.setDefaultRenderer(String.class, new ColumnNameCellRenderer());
        return columnSelection;
    }

    /**
     * Renders a tab name
     */
    private static class ColumnNameCellRenderer extends JLabel implements TableCellRenderer {

        public ColumnNameCellRenderer() {
            this.setIcon(UIUtils.getIconFromResources("table.png"));
            this.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.setText("" + value);
            return this;
        }
    }
}
