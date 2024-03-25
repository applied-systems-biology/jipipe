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

import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabListCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import java.awt.*;

/**
 * UI that merges tables
 */
public class JIPipeDesktopJIPipeMergeTablesDialogUI extends JDialog {
    private JIPipeDesktopTableEditor tableAnalyzerUI;
    private JComboBox<JIPipeDesktopTabPane.DocumentTab> tableSelection;
    private JXTable jxTable;

    /**
     * Creates new instance
     *
     * @param tableAnalyzerUI The table analyzer
     */
    public JIPipeDesktopJIPipeMergeTablesDialogUI(JIPipeDesktopTableEditor tableAnalyzerUI) {
        this.tableAnalyzerUI = tableAnalyzerUI;
        initialize();

        for (JIPipeDesktopTabPane.DocumentTab tab : tableAnalyzerUI.getDesktopWorkbench().getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof JIPipeDesktopTableEditor) {
                tableSelection.addItem(tab);
            }
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        tableSelection = new JComboBox<>();
        tableSelection.setRenderer(new JIPipeDesktopTabListCellRenderer());
        tableSelection.addItemListener(e -> {
            if (e.getItem() instanceof JIPipeDesktopTabPane.DocumentTab) {
                jxTable.setModel(((JIPipeDesktopTableEditor) ((JIPipeDesktopTabPane.DocumentTab) e.getItem()).getContent()).getTableModel());
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
            ResultsTableData sourceModel = ((JIPipeDesktopTableEditor) ((JIPipeDesktopTabPane.DocumentTab) tableSelection.getSelectedItem()).getContent()).getTableModel();
            ResultsTableData targetModel = tableAnalyzerUI.getTableModel();

            targetModel.addRows(sourceModel);

            tableAnalyzerUI.autoSizeColumns();
            setVisible(false);
        }

    }
}
