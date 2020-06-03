package org.hkijena.acaq5.ui.plotbuilder;

import com.google.common.eventbus.Subscribe;
import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableAnalyzerUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages available data sets of a plot builder
 */
public class ACAQPlotAvailableDataManagerUI extends ACAQWorkbenchPanel {

    private ACAQPlotBuilderUI plotBuilderUI;
    private JList<TableColumn> dataSourceJList;
    private JPopupMenu importPopupMenu;

    /**
     * @param workbench     the workbench
     * @param plotBuilderUI the plot builder
     */
    public ACAQPlotAvailableDataManagerUI(ACAQWorkbench workbench, ACAQPlotBuilderUI plotBuilderUI) {
        super(workbench);
        this.plotBuilderUI = plotBuilderUI;
        initialize();
        reloadList();

        plotBuilderUI.getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();

        JButton importDataButton = new JButton("Import", UIUtils.getIconFromResources("import.png"));
        this.importPopupMenu = new JPopupMenu();
        UIUtils.addReloadablePopupMenuToComponent(importDataButton, importPopupMenu, this::reloadImportPopupMenu);
        toolBar.add(importDataButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton removeSelectedDataButton = new JButton("Remove", UIUtils.getIconFromResources("delete.png"));
        removeSelectedDataButton.addActionListener(e -> removeSelectedData());
        toolBar.add(removeSelectedDataButton);

        JButton openSelectedDataButton = new JButton("Show", UIUtils.getIconFromResources("search.png"));
        openSelectedDataButton.addActionListener(e -> showSelectedData());
        toolBar.add(openSelectedDataButton);

        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        dataSourceJList = new JList<>(new DefaultListModel<>());
        dataSourceJList.setCellRenderer(new PlotDataSeriesColumnListCellRenderer());
        add(dataSourceJList, BorderLayout.CENTER);

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
        plotBuilderUI.removeData(dataSourceJList.getSelectedValuesList());
    }

    private void reloadImportPopupMenu() {
        importPopupMenu.removeAll();

        JMenuItem importCSVItem = new JMenuItem("From *.csv", UIUtils.getIconFromResources("open.png"));
        importCSVItem.addActionListener(e -> importFromCSV());
        importPopupMenu.add(importCSVItem);

        List<DocumentTabPane.DocumentTab> tableAnalyzers = getWorkbench().getDocumentTabPane().getTabsContaining(ACAQTableAnalyzerUI.class);
        if (!tableAnalyzers.isEmpty()) {
            importPopupMenu.addSeparator();
            for (DocumentTabPane.DocumentTab tab : tableAnalyzers) {
                JMenuItem importItem = new JMenuItem("Import from '" + tab.getTitle() + "'", UIUtils.getIconFromResources("table.png"));
                ACAQTableAnalyzerUI tableAnalyzerUI = (ACAQTableAnalyzerUI) tab.getContent();
                importItem.addActionListener(e -> importFromTableAnalyzer(tableAnalyzerUI, tab.getTitle()));
                importPopupMenu.add(importItem);
            }
        }
    }

    private void importFromTableAnalyzer(ACAQTableAnalyzerUI tableAnalyzerUI, String title) {
        DefaultTableModel tableModel = tableAnalyzerUI.getTableModel();
        plotBuilderUI.importData(tableModel, title);
    }

    private void importFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import *.csv");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String fileName = fileChooser.getSelectedFile().getName();
            try {
                ResultsTable table = ResultsTable.open(fileChooser.getSelectedFile().toString());
                PlotDataSeries series = new PlotDataSeries(table);
                series.setName(fileName);
                plotBuilderUI.importData(series);
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

        DefaultTableModel tableModel = new DefaultTableModel(rows, dataSourceJList.getSelectedValuesList().size());
        tableModel.setColumnIdentifiers(dataSourceJList.getSelectedValuesList().stream().map(TableColumn::getLabel).toArray());
        for (int column = 0; column < dataSourceJList.getSelectedValuesList().size(); column++) {
            TableColumn dataSource = dataSourceJList.getSelectedValuesList().get(column);
            if (dataSource.isNumeric()) {
                double[] data = dataSource.getDataAsDouble(rows);
                for (int row = 0; row < rows; row++) {
                    tableModel.setValueAt(data[row], row, column);
                }
            } else {
                String[] data = dataSource.getDataAsString(rows);
                for (int row = 0; row < rows; row++) {
                    tableModel.setValueAt(data[row], row, column);
                }
            }
        }

        String name = dataSourceJList.getSelectedValuesList().size() == 1 ? dataSourceJList.getSelectedValuesList().get(0).getLabel() : "Table";
        ACAQTableAnalyzerUI tableAnalyzerUI = new ACAQTableAnalyzerUI((ACAQProjectWorkbench) getWorkbench(), tableModel);
        getWorkbench().getDocumentTabPane().addTab(name, UIUtils.getIconFromResources("table.png"),
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
        for (TableColumn dataSource : plotBuilderUI.getAvailableData().values().stream()
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
    public void onParameterChanged(ParameterChangedEvent event) {
        if (event.getKey().equals("available-data")) {
            reloadList();
        }
    }
}
