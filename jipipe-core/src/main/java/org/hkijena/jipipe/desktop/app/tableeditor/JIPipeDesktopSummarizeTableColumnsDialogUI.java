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

package org.hkijena.jipipe.desktop.app.tableeditor;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.plugins.tables.SummarizingColumnOperation;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UI for summarizing table columns
 */
public class JIPipeDesktopSummarizeTableColumnsDialogUI extends JDialog {
    private ResultsTableData inputTableModel;
    private ResultsTableData outputTableModel;
    private Map<String, JComboBox<Object>> columnOperations = new HashMap<>();

    /**
     * @param inputTableModel The table model
     */
    public JIPipeDesktopSummarizeTableColumnsDialogUI(ResultsTableData inputTableModel) {
        this.inputTableModel = inputTableModel;
        initialize();
    }

    private void initialize() {
        setTitle("Summarize columns");
        setLayout(new BorderLayout(8, 8));

        JPanel columnPanel = new JPanel(new GridBagLayout());

        for (int columnIndex = 0; columnIndex < inputTableModel.getColumnCount(); ++columnIndex) {

            final int row = columnIndex;
            JComboBox<Object> operationJComboBox = new JComboBox<>();
            operationJComboBox.setRenderer(new Renderer());
            columnOperations.put(inputTableModel.getColumnName(columnIndex), operationJComboBox);

            operationJComboBox.addItem(null);
            operationJComboBox.addItem(new CategorizeColumnRole());

            for (JIPipeExpressionRegistry.ColumnOperationEntry entry :
                    JIPipe.getTableOperations().getTableColumnOperationsOfType(SummarizingColumnOperation.class)
                            .values().stream().sorted(Comparator.comparing(JIPipeExpressionRegistry.ColumnOperationEntry::getName)).collect(Collectors.toList())) {
                operationJComboBox.addItem(entry);
            }

            columnPanel.add(operationJComboBox, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = row;
                    insets = UIUtils.UI_PADDING;
                    anchor = GridBagConstraints.NORTHWEST;
                }
            });

            JLabel label = new JLabel(inputTableModel.getColumnName(columnIndex),
                    UIUtils.getIconFromResources("actions/stock_select-column.png"), JLabel.LEFT);
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
        UIUtils.addFillerGridBagComponent(columnPanel, inputTableModel.getColumnCount(), 1);
        JScrollPane scrollPane = new JScrollPane(columnPanel);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton calculateButton = new JButton("Calculate", UIUtils.getIconFromResources("actions/statistics.png"));
        calculateButton.addActionListener(e -> calculate());
        buttonPanel.add(calculateButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void calculate() {
        try (BusyCursor busyCursor = new BusyCursor(this)) {
            Set<String> categoryColumns = new HashSet<>();
            List<ResultsTableData.IntegratingColumnOperationEntry> operations = new ArrayList<>();
            for (Map.Entry<String, JComboBox<Object>> entry : columnOperations.entrySet()) {
                Object value = entry.getValue().getSelectedItem();
                if (value instanceof CategorizeColumnRole) {
                    categoryColumns.add(entry.getKey());
                } else if (value instanceof JIPipeExpressionRegistry.ColumnOperationEntry) {
                    JIPipeExpressionRegistry.ColumnOperationEntry operationEntry = (JIPipeExpressionRegistry.ColumnOperationEntry) value;
                    operations.add(new ResultsTableData.IntegratingColumnOperationEntry(entry.getKey(),
                            String.format("%s(%s)", operationEntry.getShortName(), entry.getKey()),
                            (SummarizingColumnOperation) operationEntry.getOperation()));
                }
            }

            outputTableModel = inputTableModel.getStatistics(operations, categoryColumns);
        }

        setVisible(false);
    }

    /**
     * @return The resulting table model
     */
    public ResultsTableData getOutputTableModel() {
        return outputTableModel;
    }

    /**
     * Marks the column as categorization source
     */
    private static class CategorizeColumnRole {

    }

    /**
     * Renders a list entry
     */
    private static class Renderer extends JLabel implements ListCellRenderer<Object> {

        public Renderer() {
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {

            if (value instanceof JIPipeExpressionRegistry.ColumnOperationEntry) {
                setText(((JIPipeExpressionRegistry.ColumnOperationEntry) value).getName());
                setIcon(UIUtils.getIconFromResources("actions/statistics.png"));
            } else if (value instanceof CategorizeColumnRole) {
                setText("Use as category");
                setIcon(UIUtils.getIconFromResources("actions/filter.png"));
            } else {
                setText("Ignore column");
                setIcon(UIUtils.getIconFromResources("actions/cancel.png"));
            }

            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }

            return this;
        }
    }
}
