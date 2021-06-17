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

package org.hkijena.jipipe.ui.plotbuilder;

import com.google.common.eventbus.Subscribe;
import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages available data sets of a plot builder
 */
public class JIPipePlotAvailableDataManagerUI extends JIPipeWorkbenchPanel {

    private PlotEditor plotEditor;
    private JList<TableColumn> dataSourceJList;
    private JPopupMenu importPopupMenu;

    /**
     * @param workbench     the workbench
     * @param plotEditor the plot builder
     */
    public JIPipePlotAvailableDataManagerUI(JIPipeWorkbench workbench, PlotEditor plotEditor) {
        super(workbench);
        this.plotEditor = plotEditor;
        initialize();
        reloadList();

        plotEditor.getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();

        JButton importDataButton = new JButton("Import", UIUtils.getIconFromResources("actions/document-import.png"));
        this.importPopupMenu = new JPopupMenu();
        UIUtils.addReloadablePopupMenuToComponent(importDataButton, importPopupMenu, this::reloadImportPopupMenu);
        toolBar.add(importDataButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton removeSelectedDataButton = new JButton("Remove", UIUtils.getIconFromResources("actions/delete.png"));
        removeSelectedDataButton.addActionListener(e -> removeSelectedData());
        toolBar.add(removeSelectedDataButton);

        JButton openSelectedDataButton = new JButton("Show", UIUtils.getIconFromResources("actions/find.png"));
        openSelectedDataButton.addActionListener(e -> showSelectedData());
        toolBar.add(openSelectedDataButton);

        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        dataSourceJList = new JList<>(new DefaultListModel<>());
        dataSourceJList.setCellRenderer(new PlotDataSeriesColumnListCellRenderer());
        add(new JScrollPane(dataSourceJList), BorderLayout.CENTER);

        dataSourceJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedData();
                }
            }
        });
    }

    private void removeSelectedData() {
        plotEditor.removeData(dataSourceJList.getSelectedValuesList());
    }

    private void reloadImportPopupMenu() {
        importPopupMenu.removeAll();

        JMenuItem importCSVItem = new JMenuItem("From *.csv", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        importCSVItem.addActionListener(e -> importFromCSV());
        importPopupMenu.add(importCSVItem);

        List<DocumentTabPane.DocumentTab> tableAnalyzers = getWorkbench().getDocumentTabPane().getTabsContaining(TableEditor.class);
        if (!tableAnalyzers.isEmpty()) {
            importPopupMenu.addSeparator();
            for (DocumentTabPane.DocumentTab tab : tableAnalyzers) {
                JMenuItem importItem = new JMenuItem("Import from '" + tab.getTitle() + "'", UIUtils.getIconFromResources("data-types/results-table.png"));
                TableEditor tableAnalyzerUI = (TableEditor) tab.getContent();
                importItem.addActionListener(e -> importFromTableAnalyzer(tableAnalyzerUI, tab.getTitle()));
                importPopupMenu.add(importItem);
            }
        }
    }

    private void importFromTableAnalyzer(TableEditor tableAnalyzerUI, String title) {
        ResultsTableData tableModel = tableAnalyzerUI.getTableModel();
        plotEditor.importData(tableModel, title);
    }

    private void importFromCSV() {
        Path selectedPath = FileChooserSettings.openFile(this, FileChooserSettings.KEY_PROJECT, "Import CSV table (*.csv)", UIUtils.EXTENSION_FILTER_CSV);
        if (selectedPath != null) {
            String fileName = selectedPath.getFileName().toString();
            try {
                ResultsTable table = ResultsTable.open(selectedPath.toString());
                PlotDataSeries series = new PlotDataSeries(table);
                series.setName(fileName);
                plotEditor.importData(series);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void showSelectedData() {
        int rows = 0;
        for (TableColumn dataSource : dataSourceJList.getSelectedValuesList()) {
            rows = Math.max(dataSource.getRows(), rows);
        }

        ResultsTableData tableModel = new ResultsTableData();
        tableModel.addRows(rows);
        for (int sourceColumn = 0; sourceColumn < dataSourceJList.getSelectedValuesList().size(); sourceColumn++) {
            TableColumn dataSource = dataSourceJList.getSelectedValuesList().get(sourceColumn);
            int targetColumn = tableModel.addColumn(dataSource.getLabel(), !dataSource.isNumeric());
            if (dataSource.isNumeric()) {
                double[] data = dataSource.getDataAsDouble(rows);
                for (int row = 0; row < rows; row++) {
                    tableModel.setValueAt(data[row], row, targetColumn);
                }
            } else {
                String[] data = dataSource.getDataAsString(rows);
                for (int row = 0; row < rows; row++) {
                    tableModel.setValueAt(data[row], row, targetColumn);
                }
            }
        }

        String name = dataSourceJList.getSelectedValuesList().size() == 1 ? dataSourceJList.getSelectedValuesList().get(0).getLabel() : "Table";
        TableEditor tableAnalyzerUI = new TableEditor((JIPipeProjectWorkbench) getWorkbench(), tableModel);
        getWorkbench().getDocumentTabPane().addTab(name, UIUtils.getIconFromResources("data-types/results-table.png"),
                tableAnalyzerUI,
                DocumentTabPane.CloseMode.withAskOnCloseButton,
                true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }

    /**
     * Reloads the list of data sources
     */
    public void reloadList() {
        DefaultListModel<TableColumn> model = (DefaultListModel<TableColumn>) dataSourceJList.getModel();
        model.clear();
        for (TableColumn dataSource : plotEditor.getAvailableData().values().stream()
                .sorted(Comparator.comparing(TableColumn::getLabel)).collect(Collectors.toList())) {
            model.addElement(dataSource);
        }
    }

    /**
     * Triggered when a plot builder parameter is changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (event.getKey().equals("available-data")) {
            reloadList();
        }
    }
}
