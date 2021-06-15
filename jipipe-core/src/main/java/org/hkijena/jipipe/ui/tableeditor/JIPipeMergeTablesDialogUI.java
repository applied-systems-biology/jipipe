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

package org.hkijena.jipipe.ui.tableeditor;

import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.components.DocumentTabListCellRenderer;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import java.awt.*;

/**
 * UI that merges tables
 */
public class JIPipeMergeTablesDialogUI extends JDialog {
    private TableEditor tableAnalyzerUI;
    private JComboBox<DocumentTabPane.DocumentTab> tableSelection;
    private JXTable jxTable;

    /**
     * Creates new instance
     *
     * @param tableAnalyzerUI The table analyzer
     */
    public JIPipeMergeTablesDialogUI(TableEditor tableAnalyzerUI) {
        this.tableAnalyzerUI = tableAnalyzerUI;
        initialize();

        for (DocumentTabPane.DocumentTab tab : tableAnalyzerUI.getWorkbench().getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof TableEditor) {
                tableSelection.addItem(tab);
            }
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        tableSelection = new JComboBox<>();
        tableSelection.setRenderer(new DocumentTabListCellRenderer());
        tableSelection.addItemListener(e -> {
            if (e.getItem() instanceof DocumentTabPane.DocumentTab) {
                jxTable.setModel(((TableEditor) ((DocumentTabPane.DocumentTab) e.getItem()).getContent()).getTableModel());
                jxTable.packAll();
            }
        });
        toolBar.add(tableSelection);
        toolBar.add(Box.createHorizontalGlue());
        add(toolBar, BorderLayout.NORTH);

        jxTable = new JXTable();
        jxTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jxTable.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(jxTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton calculateButton = new JButton("Merge", UIUtils.getIconFromResources("actions/document-import.png"));
        calculateButton.addActionListener(e -> calculate());
        buttonPanel.add(calculateButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void calculate() {
        if (tableSelection.getSelectedItem() != null) {
            tableAnalyzerUI.createUndoSnapshot();
            ResultsTableData sourceModel = ((TableEditor) ((DocumentTabPane.DocumentTab) tableSelection.getSelectedItem()).getContent()).getTableModel();
            ResultsTableData targetModel = tableAnalyzerUI.getTableModel();

            targetModel.addRows(sourceModel);

            tableAnalyzerUI.autoSizeColumns();
            setVisible(false);
        }

    }
}
