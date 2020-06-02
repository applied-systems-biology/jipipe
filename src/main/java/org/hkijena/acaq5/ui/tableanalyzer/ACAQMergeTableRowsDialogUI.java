/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.tableanalyzer;

import org.hkijena.acaq5.ui.components.DocumentTabListCellRenderer;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * UI that merges table rows
 */
public class ACAQMergeTableRowsDialogUI extends JDialog {
    private ACAQTableAnalyzerUI tableAnalyzerUI;
    private JComboBox<DocumentTabPane.DocumentTab> tableSelection;
    private JCheckBox addMissingColumnsCheckBox;
    private JXTable jxTable;

    /**
     * Creates new instance
     *
     * @param tableAnalyzerUI The table analyzer
     */
    public ACAQMergeTableRowsDialogUI(ACAQTableAnalyzerUI tableAnalyzerUI) {
        this.tableAnalyzerUI = tableAnalyzerUI;
        initialize();

        for (DocumentTabPane.DocumentTab tab : tableAnalyzerUI.getProjectWorkbench().getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof ACAQTableAnalyzerUI && tab.getContent() != tableAnalyzerUI) {
                tableSelection.addItem(tab);
            }
        }
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
            }
        });
        toolBar.add(tableSelection);
        toolBar.add(Box.createHorizontalGlue());
        add(toolBar, BorderLayout.NORTH);

        jxTable = new JXTable();
        jxTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jxTable.setEditable(false);
        add(new JScrollPane(jxTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        addMissingColumnsCheckBox = new JCheckBox("Add missing columns", true);
        buttonPanel.add(addMissingColumnsCheckBox);

        buttonPanel.add(Box.createHorizontalGlue());

        JButton calculateButton = new JButton("Merge", UIUtils.getIconFromResources("import.png"));
        calculateButton.addActionListener(e -> calculate());
        buttonPanel.add(calculateButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void calculate() {
        if (tableSelection.getSelectedItem() != null) {
            tableAnalyzerUI.createUndoSnapshot();
            DefaultTableModel sourceModel = ((ACAQTableAnalyzerUI) ((DocumentTabPane.DocumentTab) tableSelection.getSelectedItem()).getContent()).getTableModel();
            DefaultTableModel targetModel = tableAnalyzerUI.getTableModel();

            Set<Integer> assignedTargetColumns = new HashSet<>();
            int[] sourceToTargetColumnMapping = new int[sourceModel.getColumnCount()];
            Arrays.fill(sourceToTargetColumnMapping, -1);

            final int initialTargetColumnCount = targetModel.getColumnCount();
            for (int i = 0; i < sourceModel.getColumnCount(); ++i) {
                boolean found = false;
                for (int j = 0; j < initialTargetColumnCount; ++j) {
                    if (Objects.equals(targetModel.getColumnName(j), sourceModel.getColumnName(i)) &&
                            !assignedTargetColumns.contains(j)) {
                        sourceToTargetColumnMapping[i] = j;
                        assignedTargetColumns.add(j);
                        found = true;
                        break;
                    }
                }
                if (!found && addMissingColumnsCheckBox.isSelected()) {
                    targetModel.addColumn(sourceModel.getColumnName(i));
                    sourceToTargetColumnMapping[i] = targetModel.getColumnCount() - 1;
                    assignedTargetColumns.add(targetModel.getColumnCount() - 1);
                }
            }

            Object[] rowBuffer = new Object[targetModel.getColumnCount()];
            for (int row = 0; row < sourceModel.getRowCount(); ++row) {
                Arrays.fill(rowBuffer, null);
                for (int i = 0; i < sourceModel.getColumnCount(); ++i) {
                    int target = sourceToTargetColumnMapping[i];
                    if (target != -1) {
                        rowBuffer[target] = sourceModel.getValueAt(row, i);
                    }
                }
                targetModel.addRow(rowBuffer);
            }

            tableAnalyzerUI.autoSizeColumns();
            setVisible(false);
        }

    }
}
