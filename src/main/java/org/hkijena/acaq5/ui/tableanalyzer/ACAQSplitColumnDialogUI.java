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


import org.hkijena.acaq5.utils.BusyCursor;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UI for splitting table columns
 */
public class ACAQSplitColumnDialogUI extends JDialog {
    private DefaultTableModel tableModel;
    private DefaultTableModel resultTableModel;
    private List<JComboBox<ColumnRole>> columnOperations = new ArrayList<>();

    /**
     * @param tableModel the table
     */
    public ACAQSplitColumnDialogUI(DefaultTableModel tableModel) {
        this.tableModel = tableModel;
        initialize();
    }

    private void initialize() {
        setTitle("Split column");
        setLayout(new BorderLayout(8, 8));

        JPanel columnPanel = new JPanel(new GridBagLayout());

        for (int columnIndex = 0; columnIndex < tableModel.getColumnCount(); ++columnIndex) {

            final int row = columnIndex;
            JComboBox<ColumnRole> operationJComboBox = new JComboBox<>();
            operationJComboBox.setRenderer(new Renderer());
            columnOperations.add(operationJComboBox);

            operationJComboBox.addItem(ColumnRole.Ignore);
            operationJComboBox.addItem(ColumnRole.Category);
            operationJComboBox.addItem(ColumnRole.Value);

            columnPanel.add(operationJComboBox, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = row;
                    insets = UIUtils.UI_PADDING;
                    anchor = GridBagConstraints.NORTHWEST;
                }
            });

            JLabel label = new JLabel(tableModel.getColumnName(columnIndex),
                    UIUtils.getIconFromResources("select-column.png"), JLabel.LEFT);
            columnPanel.add(label, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = row;
                    fill = GridBagConstraints.HORIZONTAL;
                    weightx = 1;
                    insets = UIUtils.UI_PADDING;
                    anchor = GridBagConstraints.WEST;
                }
            });
        }
        UIUtils.addFillerGridBagComponent(columnPanel, tableModel.getColumnCount(), 1);

        add(new JScrollPane(columnPanel), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton calculateButton = new JButton("Calculate", UIUtils.getIconFromResources("cog.png"));
        calculateButton.addActionListener(e -> calculate());
        buttonPanel.add(calculateButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void calculate() {
        try (BusyCursor busyCursor = new BusyCursor(this)) {
            boolean categoryFound = false;
            int valueColumn = -1;
            for (int i = 0; i < columnOperations.size(); ++i) {
                if (columnOperations.get(i).getSelectedItem() == ColumnRole.Category) {
                    categoryFound = true;
                } else if (columnOperations.get(i).getSelectedItem() == ColumnRole.Value) {
                    if (valueColumn != -1) {
                        JOptionPane.showMessageDialog(this, "You can only select one value column", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    valueColumn = i;
                }
            }
            if (valueColumn == -1) {
                JOptionPane.showMessageDialog(this, "Please set one column as value column", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!categoryFound) {
                JOptionPane.showMessageDialog(this, "Please select at least one category column", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            DefaultTableModel result = new DefaultTableModel();
            Map<String, Integer> columnStateAssignment = new HashMap<>();
            Map<String, Integer> columnRowCounts = new HashMap<>();
            StringBuilder category = new StringBuilder();
            for (int row = 0; row < tableModel.getRowCount(); ++row) {
                category.setLength(0);
                for (int i = 0; i < columnOperations.size(); ++i) {
                    if (columnOperations.get(i).getSelectedItem() == ColumnRole.Category) {
                        if (category.length() > 0)
                            category.append(", ");
                        category.append(tableModel.getColumnName(i));
                        category.append("=");
                        category.append(tableModel.getValueAt(row, i));
                    }
                }

                if (!columnStateAssignment.containsKey(category.toString())) {
                    result.addColumn(tableModel.getColumnName(valueColumn) + " where " + category.toString());
                    columnStateAssignment.put(category.toString(), result.getColumnCount() - 1);
                    columnRowCounts.put(category.toString(), 0);
                }

                // Insert the value into the table
                {
                    int targetRow = columnRowCounts.get(category.toString());
                    int targetColumn = columnStateAssignment.get(category.toString());

                    if (result.getRowCount() < targetRow + 1)
                        result.setRowCount(targetRow + 1);

                    result.setValueAt(tableModel.getValueAt(row, valueColumn), targetRow, targetColumn);
                    columnRowCounts.put(category.toString(), targetRow + 1);
                }
            }

            resultTableModel = result;
        }

        setVisible(false);
    }

    public DefaultTableModel getResultTableModel() {
        return resultTableModel;
    }

    /**
     * Role for each column
     */
    enum ColumnRole {
        Ignore,
        Value,
        Category
    }

    /**
     * Renderer for {@link ColumnRole}
     */
    private static class Renderer extends JLabel implements ListCellRenderer<ColumnRole> {

        public Renderer() {
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ColumnRole> list, ColumnRole value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {

            if (value == ColumnRole.Value) {
                setText("Use as value");
                setIcon(UIUtils.getIconFromResources("number.png"));
            } else if (value == ColumnRole.Category) {
                setText("Use as category");
                setIcon(UIUtils.getIconFromResources("filter.png"));
            } else {
                setText("Ignore column");
                setIcon(UIUtils.getIconFromResources("remove.png"));
            }

            if (isSelected) {
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }

            return this;
        }
    }
}
