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

package org.hkijena.jipipe.desktop.app.ploteditor;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotDataSeries;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
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
public class JIPipeDesktopPlotAvailableDataManagerUI extends JIPipeDesktopWorkbenchPanel implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final JFreeChartPlotEditor plotEditor;
    private JList<TableColumnData> dataSourceJList;
    private JPopupMenu importPopupMenu;

    /**
     * @param workbench  the workbench
     * @param plotEditor the plot builder
     */
    public JIPipeDesktopPlotAvailableDataManagerUI(JIPipeDesktopWorkbench workbench, JFreeChartPlotEditor plotEditor) {
        super(workbench);
        this.plotEditor = plotEditor;
        initialize();
        reloadList();

        plotEditor.getParameterChangedEventEmitter().subscribe(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();

        JButton importDataButton = new JButton("Import", UIUtils.getIconFromResources("actions/document-import.png"));
        this.importPopupMenu = new JPopupMenu();
        UIUtils.addReloadablePopupMenuToButton(importDataButton, importPopupMenu, this::reloadImportPopupMenu);
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
        dataSourceJList.setCellRenderer(new JIPipeDesktopPlotDataSeriesColumnListCellRenderer());
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
        importCSVItem.addActionListener(e -> importDataCSV());
        importPopupMenu.add(importCSVItem);

        List<JIPipeDesktopTabPane.DocumentTab> tableAnalyzers = getDesktopWorkbench().getDocumentTabPane().getTabsContaining(JIPipeDesktopTableEditor.class);
        if (!tableAnalyzers.isEmpty()) {
            importPopupMenu.addSeparator();
            for (JIPipeDesktopTabPane.DocumentTab tab : tableAnalyzers) {
                JMenuItem importItem = new JMenuItem("Import from '" + tab.getTitle() + "'", UIUtils.getIconFromResources("data-types/results-table.png"));
                JIPipeDesktopTableEditor tableAnalyzerUI = (JIPipeDesktopTableEditor) tab.getContent();
                importItem.addActionListener(e -> importDataTableAnalyzer(tableAnalyzerUI, tab.getTitle()));
                importPopupMenu.add(importItem);
            }
        }
    }

    private void importDataTableAnalyzer(JIPipeDesktopTableEditor tableAnalyzerUI, String title) {
        ResultsTableData tableModel = tableAnalyzerUI.getTableModel();
        plotEditor.importData(tableModel, title);
    }

    private void importDataCSV() {
        Path selectedPath = JIPipeDesktop.openFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Import CSV table (*.csv)", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_CSV);
        if (selectedPath != null) {
            String fileName = selectedPath.getFileName().toString();
            try {
                ResultsTable table = ResultsTable.open(selectedPath.toString());
                JFreeChartPlotDataSeries series = new JFreeChartPlotDataSeries(table);
                series.setName(fileName);
                plotEditor.importData(series);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void showSelectedData() {
        int rows = 0;
        for (TableColumnData dataSource : dataSourceJList.getSelectedValuesList()) {
            rows = Math.max(dataSource.getRows(), rows);
        }

        ResultsTableData tableModel = new ResultsTableData();
        tableModel.addRows(rows);
        for (int sourceColumn = 0; sourceColumn < dataSourceJList.getSelectedValuesList().size(); sourceColumn++) {
            TableColumnData dataSource = dataSourceJList.getSelectedValuesList().get(sourceColumn);
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
        JIPipeDesktopTableEditor.openWindow(getDesktopWorkbench(), tableModel, name);
    }

    /**
     * Reloads the list of data sources
     */
    public void reloadList() {
        DefaultListModel<TableColumnData> model = (DefaultListModel<TableColumnData>) dataSourceJList.getModel();
        model.clear();
        for (TableColumnData dataSource : plotEditor.getAvailableData().values().stream()
                .sorted(Comparator.comparing(TableColumnData::getLabel)).collect(Collectors.toList())) {
            model.addElement(dataSource);
        }
    }

    /**
     * Triggered when a plot builder parameter is changed
     *
     * @param event generated event
     */
    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (event.getKey().equals("available-data")) {
            reloadList();
        }
    }
}
