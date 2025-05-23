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

import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopResultsTableDataListCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * UI that merges tables
 */
public class JIPipeDesktopOpenTableFromImageJDialogUI extends JDialog {
    private final JIPipeDesktopWorkbench workbench;
    private JComboBox<ResultsTableData> tableSelection;
    private JXTable jxTable;

    public JIPipeDesktopOpenTableFromImageJDialogUI(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        initialize();
        refreshList();
    }

    private void refreshList() {
        DefaultComboBoxModel<ResultsTableData> model = new DefaultComboBoxModel<>();
        for (Window window : WindowManager.getAllNonImageWindows()) {
            if (window instanceof TextWindow) {
                ResultsTable resultsTable = ((TextWindow) window).getResultsTable();
                if (resultsTable != null) {
                    ResultsTableData tableData = new ResultsTableData(resultsTable);
                    model.addElement(tableData);
                }
            }
        }
        tableSelection.setModel(model);
        if (model.getSize() > 0) {
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
        tableSelection.setRenderer(new JIPipeDesktopResultsTableDataListCellRenderer());
        tableSelection.addItemListener(e -> {
            refreshPreview();
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

        JButton refreshButton = new JButton("Refresh", UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.addActionListener(e -> refreshList());
        buttonPanel.add(refreshButton);

        buttonPanel.add(Box.createHorizontalGlue());

        JButton calculateButton = new JButton("Open in JIPipe", UIUtils.getIconFromResources("apps/jipipe.png"));
        calculateButton.addActionListener(e -> open());
        buttonPanel.add(calculateButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void refreshPreview() {
        if (tableSelection.getSelectedItem() instanceof ResultsTableData) {
            jxTable.setModel((TableModel) tableSelection.getSelectedItem());
            jxTable.packAll();
        }
    }

    private void open() {
        if (tableSelection.getSelectedItem() instanceof ResultsTableData) {
            ResultsTableData tableData = (ResultsTableData) tableSelection.getSelectedItem();
            JIPipeDesktopTableEditor tableAnalyzerUI = new JIPipeDesktopTableEditor((JIPipeDesktopProjectWorkbench) workbench, tableData);
            workbench.getDocumentTabPane().addTab("Table", UIUtils.getIconFromResources("data-types/results-table.png"),
                    tableAnalyzerUI, JIPipeDesktopTabPane.CloseMode.withAskOnCloseButton, true);
            workbench.getDocumentTabPane().switchToLastTab();
            setVisible(false);
        }
    }
}
