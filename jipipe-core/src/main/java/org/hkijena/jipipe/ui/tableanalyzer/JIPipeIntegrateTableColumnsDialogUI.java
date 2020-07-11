/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.tableanalyzer;

import org.hkijena.jipipe.api.registries.JIPipeTableRegistry;
import org.hkijena.jipipe.extensions.tables.IntegratingColumnOperation;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.BusyCursor;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UI for integrating table columns
 */
public class JIPipeIntegrateTableColumnsDialogUI extends JDialog {
    private ResultsTableData inputTableModel;
    private ResultsTableData outputTableModel;
    private Map<String, JComboBox<Object>> columnOperations = new HashMap<>();

    /**
     * @param inputTableModel The table model
     */
    public JIPipeIntegrateTableColumnsDialogUI(ResultsTableData inputTableModel) {
        this.inputTableModel = inputTableModel;
        initialize();
    }

    private void initialize() {
        setTitle("Integrate columns");
        setLayout(new BorderLayout(8, 8));

        JPanel columnPanel = new JPanel(new GridBagLayout());

        for (int columnIndex = 0; columnIndex < inputTableModel.getColumnCount(); ++columnIndex) {

            final int row = columnIndex;
            JComboBox<Object> operationJComboBox = new JComboBox<>();
            operationJComboBox.setRenderer(new Renderer());
            columnOperations.put(inputTableModel.getColumnName(columnIndex), operationJComboBox);

            operationJComboBox.addItem(null);
            operationJComboBox.addItem(new CategorizeColumnRole());

            for (JIPipeTableRegistry.ColumnOperationEntry entry :
                    JIPipeTableRegistry.getInstance().getOperationsOfType(IntegratingColumnOperation.class)
                            .values().stream().sorted(Comparator.comparing(JIPipeTableRegistry.ColumnOperationEntry::getName)).collect(Collectors.toList())) {
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
        UIUtils.addFillerGridBagComponent(columnPanel, inputTableModel.getColumnCount(), 1);

        add(new JScrollPane(columnPanel), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton calculateButton = new JButton("Calculate", UIUtils.getIconFromResources("statistics.png"));
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
                } else if (value instanceof JIPipeTableRegistry.ColumnOperationEntry) {
                    JIPipeTableRegistry.ColumnOperationEntry operationEntry = (JIPipeTableRegistry.ColumnOperationEntry) value;
                    operations.add(new ResultsTableData.IntegratingColumnOperationEntry(entry.getKey(),
                            String.format("%s(%s)", operationEntry.getShortName(), entry.getKey()),
                            (IntegratingColumnOperation) operationEntry.getOperation()));
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

            if (value instanceof JIPipeTableRegistry.ColumnOperationEntry) {
                setText(((JIPipeTableRegistry.ColumnOperationEntry) value).getName());
                setIcon(UIUtils.getIconFromResources("statistics.png"));
            } else if (value instanceof CategorizeColumnRole) {
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
