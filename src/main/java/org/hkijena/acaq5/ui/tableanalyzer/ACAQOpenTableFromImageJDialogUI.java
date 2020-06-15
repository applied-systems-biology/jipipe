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

import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.DocumentTabListCellRenderer;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.ResultsTableDataListCellRenderer;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * UI that merges tables
 */
public class ACAQOpenTableFromImageJDialogUI extends JDialog {
    private final ACAQWorkbench workbench;
    private JComboBox<ResultsTableData> tableSelection;
    private JXTable jxTable;

    /**
     * Creates new instance
     *
     * @param workbench the workbench
     */
    public ACAQOpenTableFromImageJDialogUI(ACAQWorkbench workbench) {
        this.workbench = workbench;
        initialize();
        refreshList();
    }

    private void refreshList() {
        DefaultComboBoxModel<ResultsTableData> model = new DefaultComboBoxModel<>();
        for (Window window : WindowManager.getAllNonImageWindows()) {
            if(window instanceof TextWindow) {
                ResultsTable resultsTable = ((TextWindow) window).getResultsTable();
                if(resultsTable != null) {
                    ResultsTableData tableData = new ResultsTableData(resultsTable);
                    model.addElement(tableData);
                }
            }
        }
        tableSelection.setModel(model);
        if(model.getSize() > 0) {
            tableSelection.setSelectedItem(model.getElementAt(0));
            refreshPreview();
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setTitle("Import table from ImageJ");
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        tableSelection = new JComboBox<>(new DefaultComboBoxModel<>());
        tableSelection.setRenderer(new ResultsTableDataListCellRenderer());
        tableSelection.addItemListener(e -> {
            refreshPreview();
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

        JButton refreshButton = new JButton("Refresh", UIUtils.getIconFromResources("refresh.png"));
        refreshButton.addActionListener(e -> refreshList());
        buttonPanel.add(refreshButton);

        buttonPanel.add(Box.createHorizontalGlue());

        JButton calculateButton = new JButton("Open in ACAQ5", UIUtils.getIconFromResources("acaq5.png"));
        calculateButton.addActionListener(e -> open());
        buttonPanel.add(calculateButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void refreshPreview() {
        if (tableSelection.getSelectedItem() instanceof ResultsTableData) {
            System.out.println("" + tableSelection.getSelectedItem());
            jxTable.setModel((TableModel) tableSelection.getSelectedItem());
            jxTable.packAll();
        }
    }

    private void open() {
        if(tableSelection.getSelectedItem() instanceof ResultsTableData) {
            ResultsTableData tableData = (ResultsTableData) tableSelection.getSelectedItem();
            ACAQTableEditor tableAnalyzerUI = new ACAQTableEditor((ACAQProjectWorkbench) workbench, tableData);
            workbench.getDocumentTabPane().addTab("Table", UIUtils.getIconFromResources("data-types/results-table.png"),
                    tableAnalyzerUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
            workbench.getDocumentTabPane().switchToLastTab();
            setVisible(false);
        }
    }
}
