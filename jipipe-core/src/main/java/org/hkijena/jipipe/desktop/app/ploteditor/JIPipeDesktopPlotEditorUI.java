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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import ij.macro.Variable;
import ij.measure.ResultsTable;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPWriteDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopPlotDisplayComponent;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopUserFriendlyErrorUI;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.plugins.plots.JIPipePlotDataClassFilter;
import org.hkijena.jipipe.plugins.plots.datatypes.PlotData;
import org.hkijena.jipipe.plugins.plots.datatypes.PlotDataSeries;
import org.hkijena.jipipe.plugins.plots.datatypes.PlotMetadata;
import org.hkijena.jipipe.plugins.settings.FileChooserSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Priority;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

/**
 * User interface for displaying and creating plots
 */
public class JIPipeDesktopPlotEditorUI extends JIPipeDesktopWorkbenchPanel implements JIPipeParameterCollection, JIPipeValidatable, JIPipeParameterCollection.ParameterChangedEventListener {
    private final BiMap<String, TableColumn> availableData = HashBiMap.create();
    private final List<JIPipeDesktopPlotSeriesEditor> seriesBuilders = new ArrayList<>();
    private final JIPipeDesktopTabPane sideBar = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.TabPlacement.Right);
    private final ParameterChangedEventEmitter parameterChangedEventEmitter = new ParameterChangedEventEmitter();
    private final ParameterStructureChangedEventEmitter parameterStructureChangedEventEmitter = new ParameterStructureChangedEventEmitter();
    private final ParameterUIChangedEventEmitter parameterUIChangedEventEmitter = new ParameterUIChangedEventEmitter();
    private JIPipeDataInfoRef plotType = new JIPipeDataInfoRef();
    private PlotData currentPlot;
    private JSplitPane splitPane;
    private boolean isRebuilding = false;
    private JIPipeDesktopPlotDisplayComponent plotReader;

    /**
     * @param workbench the workbench
     */
    public JIPipeDesktopPlotEditorUI(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        initialize();
        rebuildPlot();
        installDefaultDataSources();
        parameterChangedEventEmitter.subscribe(this);
    }

    /**
     * Creates a new plot editor in a new window
     *
     * @param workbench the workbench
     * @param title     the title
     * @return the table editor component
     */
    public static JIPipeDesktopPlotEditorUI openWindow(JIPipeDesktopWorkbench workbench, String title) {
        JFrame window = new JFrame(title);
        window.getContentPane().setLayout(new BorderLayout());
        window.setIconImage(UIUtils.getJIPipeIcon128());
        JIPipeDesktopPlotEditorUI editor = new JIPipeDesktopPlotEditorUI(workbench);
        window.getContentPane().add(editor, BorderLayout.CENTER);
        window.setSize(1024, 768);
        window.setLocationRelativeTo(workbench.getWindow());
        window.setVisible(true);
        return editor;
    }

    public JToolBar getToolBar() {
        return plotReader.getToolBar();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Create settings panel
        sideBar.addTab("Settings", UIUtils.getIconFromResources("actions/configure.png"),
                new JIPipeDesktopParameterPanel(getDesktopWorkbench(),
                        this,
                        null,
                        JIPipeDesktopParameterPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterPanel.DOCUMENTATION_BELOW | JIPipeDesktopParameterPanel.WITH_SCROLLING),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                false);
        sideBar.addTab("Series", UIUtils.getIconFromResources("actions/stock_select-column.png"),
                new JIPipePlotSeriesListEditorUI(getDesktopWorkbench(), this),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        sideBar.addTab("Data", UIUtils.getIconFromResources("data-types/results-table.png"),
                new JIPipeDesktopPlotAvailableDataManagerUI(getDesktopWorkbench(), this),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        // Create plot reader
        plotReader = new JIPipeDesktopPlotDisplayComponent(this);

        JButton saveButton = new JButton("Save", UIUtils.getIconFromResources("actions/save.png"));
        saveButton.addActionListener(e -> savePlot());
        plotReader.getToolBar().add(saveButton, 0);

        JButton openButton = new JButton("Open", UIUtils.getIconFromResources("actions/fileopen.png"));
        openButton.addActionListener(e -> openPlot());
        plotReader.getToolBar().add(openButton, 0);

        splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, plotReader, sideBar, new AutoResizeSplitPane.DynamicSidebarRatio());
        add(splitPane, BorderLayout.CENTER);
    }

    public JIPipeDesktopTabPane getSideBar() {
        return sideBar;
    }

    private void savePlot() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Save plot", UIUtils.EXTENSION_FILTER_ZIP);
        if (UIUtils.checkAndAskIfFileExists(this, path, "Save plot")) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try (JIPipeZIPWriteDataStorage storage = new JIPipeZIPWriteDataStorage(progressInfo, path)) {
                getCurrentPlot().exportData(storage, path.getFileName().toString(), false, progressInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void openPlot() {
        Path path = FileChooserSettings.openFile(this, FileChooserSettings.LastDirectoryKey.Data, "Open plot", UIUtils.EXTENSION_FILTER_ZIP);
        if (path != null) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try (JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(progressInfo, path)) {
                PlotData plotData = PlotData.importData(storage, progressInfo);
                importExistingPlot(plotData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void installDefaultDataSources() {
        for (Class<? extends JIPipeData> klass : JIPipe.getDataTypes().getRegisteredDataTypes().values()) {
            if (TableColumn.isGeneratingTableColumn(klass)) {
                TableColumn dataSource = (TableColumn) ReflectionUtils.newInstance(klass);
                availableData.put(dataSource.getLabel(), dataSource);
            }
        }
        parameterChangedEventEmitter.emit(new ParameterChangedEvent(this, "available-data"));
    }

    @Override
    public ParameterChangedEventEmitter getParameterChangedEventEmitter() {
        return parameterChangedEventEmitter;
    }

    @Override
    public ParameterStructureChangedEventEmitter getParameterStructureChangedEventEmitter() {
        return parameterStructureChangedEventEmitter;
    }

    @Override
    public ParameterUIChangedEventEmitter getParameterUIChangedEventEmitter() {
        return parameterUIChangedEventEmitter;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (getPlotType().getInfo() == null) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    reportContext,
                    "Plot type not selected!",
                    "Please select a plot type!"));
        }
        if (currentPlot != null) {
            report.report(new CustomValidationReportContext("Plot parameters"), currentPlot);
        }
        for (int i = 0; i < seriesBuilders.size(); ++i) {
            report.report(new CustomValidationReportContext("Series #" + (i + 1)), seriesBuilders.get(i));
        }

    }

    @SetJIPipeDocumentation(name = "Plot type", description = "The type of plot to be generated.")
    @JIPipeParameter(value = "plot-type", priority = Priority.HIGH)
    @JIPipeDataParameterSettings(dataBaseClass = PlotData.class, dataClassFilter = JIPipePlotDataClassFilter.class)
    public JIPipeDataInfoRef getPlotType() {
        if (plotType == null) {
            plotType = new JIPipeDataInfoRef();
        }
        return plotType;
    }

    @JIPipeParameter("plot-type")
    public void setPlotType(JIPipeDataInfoRef plotType) {
        this.plotType = plotType;

        updatePlotTypeParameters();
    }

    private void updatePlotTypeParameters() {
        boolean changedPlot = false;
        if (currentPlot == null || (plotType.getInfo() != null &&
                !Objects.equals(plotType.getInfo().getDataClass(), currentPlot.getClass()))) {
            if (plotType.getInfo() != null) {
                currentPlot = (PlotData) JIPipe.createData(plotType.getInfo().getDataClass());
                changedPlot = true;
                currentPlot.getParameterChangedEventEmitter().subscribe(this);
            }
        } else if (plotType.getInfo() == null) {
            currentPlot = null;
            changedPlot = true;
        }
        if (changedPlot) {
            seriesBuilders.clear();

            parameterStructureChangedEventEmitter.emit(new ParameterStructureChangedEvent(this));
            if (currentPlot != null) {
                PlotMetadata metadata = currentPlot.getClass().getAnnotation(PlotMetadata.class);
                for (int i = 0; i < metadata.minSeriesCount(); i++) {
                    addSeries();
                }
            }
        }
    }

    @SetJIPipeDocumentation(name = "Plot parameters")
    @JIPipeParameter("plot")
    public PlotData getCurrentPlot() {
        return currentPlot;
    }

    /**
     * Imports an existing plot data into this builder.
     * This replaces the current settings
     *
     * @param data the existing plot
     */
    public void importExistingPlot(PlotData data) {
        this.plotType.setInfo(JIPipeDataInfo.getInstance(data.getClass()));
        this.currentPlot = data;
        seriesBuilders.clear();
        List<PlotDataSeries> seriesList = new ArrayList<>(data.getSeries());
        for (PlotDataSeries series : seriesList) {
            JIPipeDesktopPlotSeriesEditor builder = new JIPipeDesktopPlotSeriesEditor(this, JIPipeDataInfo.getInstance(data.getClass()));
            builder.setName(series.getName());
            Map<String, String> columnMap = importData(series);
            for (Map.Entry<String, String> entry : columnMap.entrySet()) {
                builder.assignData(entry.getKey(), availableData.get(entry.getValue()));
            }
            seriesBuilders.add(builder);

            // Register events
            builder.getColumnAssignments().getParameterChangedEventEmitter().subscribe(this);
            builder.getParameterChangedEventEmitter().subscribe(this);
        }


        parameterChangedEventEmitter.emit(new ParameterChangedEvent(this, "series"));
        parameterChangedEventEmitter.emit(new ParameterChangedEvent(this, "available-data"));
        currentPlot.getParameterChangedEventEmitter().subscribe(this);
        emitParameterStructureChangedEvent();
        rebuildPlot();
    }

    /**
     * Triggered when a parameter was changed
     *
     * @param event generated event
     */
    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        if (event.getSource() == currentPlot && "series".equals(event.getKey()))
            return;
        rebuildPlot();
    }

    /**
     * Attempts to rebuild the current plot
     */
    public void rebuildPlot() {
        if (currentPlot == null)
            return;
        if (isRebuilding)
            return;
        try {
            isRebuilding = true;

            JIPipeValidationReport report = new JIPipeValidationReport();
            this.reportValidity(new UnspecifiedValidationReportContext(), report);
            if (!report.isValid()) {
                JIPipeDesktopUserFriendlyErrorUI errorUI = new JIPipeDesktopUserFriendlyErrorUI(getDesktopWorkbench(), null, JIPipeDesktopUserFriendlyErrorUI.WITH_SCROLLING);
                errorUI.displayErrors(report);
                errorUI.addVerticalGlue();
                splitPane.setLeftComponent(errorUI);
                return;
            }

            // Generate
            currentPlot.clearSeries();
            for (JIPipeDesktopPlotSeriesEditor seriesBuilder : seriesBuilders) {
                if (!seriesBuilder.isEnabled())
                    continue;
                PlotDataSeries dataSeries = seriesBuilder.buildSeries();
                currentPlot.addSeries(dataSeries);
            }

            plotReader.getChartPanel().setChart(currentPlot.getChart());
            splitPane.setLeftComponent(plotReader);
        } finally {
            isRebuilding = false;
        }
    }

    /**
     * Adds data into the data storage
     *
     * @param series the data
     * @return how the columns of this series are mapped to the columns in the list of available columns
     */
    public Map<String, String> importData(PlotDataSeries series) {
        Map<String, String> columnMapping = new HashMap<>();

        String seriesName = series.getName();
        if (StringUtils.isNullOrEmpty(seriesName))
            seriesName = "Series";

        ResultsTable table = series.getTable();
        for (int col = 0; col <= table.getLastColumn(); ++col) {
            String name = seriesName + "/" + table.getColumnHeading(col);
            name = StringUtils.makeUniqueString(name, " ", s -> availableData.containsKey(s));

            Variable[] data = table.getColumnAsVariables(table.getColumnHeading(col));
            if (data.length == 0)
                continue;

            TableColumn dataSource;
            int dataType = (int) ReflectionUtils.invokeMethod(data[0], "getType");
            if (dataType == 0) {
                double[] convertedData = new double[data.length];
                for (int i = 0; i < data.length; ++i) {
                    convertedData[i] = data[i].getValue();
                }
                dataSource = new DoubleArrayTableColumn(convertedData, name);
            } else {
                String[] convertedData = new String[data.length];
                for (int i = 0; i < data.length; ++i) {
                    convertedData[i] = data[i].getString();
                }
                dataSource = new StringArrayTableColumn(convertedData, name);
            }
            availableData.put(name, dataSource);
            columnMapping.put(table.getColumnHeading(col), name);
        }

        parameterChangedEventEmitter.emit(new ParameterChangedEvent(this, "available-data"));

        return columnMapping;
    }

    /**
     * Imports data from a table model
     *
     * @param model      the table model
     * @param seriesName name for this table model
     * @return how the columns of this series are mapped to the columns in the list of available columns
     */
    public Map<String, String> importData(TableModel model, String seriesName) {
        Map<String, String> columnMapping = new HashMap<>();

        for (int col = 0; col < model.getColumnCount(); ++col) {
            String name = seriesName + "/" + model.getColumnName(col);
            name = StringUtils.makeUniqueString(name, " ", s -> availableData.containsKey(s));

            TableColumn dataSource;
            if (Number.class.isAssignableFrom(model.getColumnClass(col))) {
                double[] convertedData = new double[model.getRowCount()];
                for (int i = 0; i < model.getRowCount(); ++i) {
                    convertedData[i] = (double) model.getValueAt(i, col);
                }
                dataSource = new DoubleArrayTableColumn(convertedData, name);
            } else {
                String[] convertedData = new String[model.getRowCount()];
                for (int i = 0; i < model.getRowCount(); ++i) {
                    convertedData[i] = "" + model.getValueAt(i, col);
                }
                dataSource = new StringArrayTableColumn(convertedData, name);
            }
            availableData.put(name, dataSource);
            columnMapping.put(model.getColumnName(col), name);
        }

        parameterChangedEventEmitter.emit(new ParameterChangedEvent(this, "available-data"));

        return columnMapping;
    }

    /**
     * Contains single-column series that will be later spliced together depending on the user's settings
     *
     * @return the map of available data
     */
    public Map<String, TableColumn> getAvailableData() {
        return Collections.unmodifiableMap(availableData);
    }

    public List<JIPipeDesktopPlotSeriesEditor> getSeriesBuilders() {
        return Collections.unmodifiableList(seriesBuilders);
    }

    /**
     * Moves the series down in order
     *
     * @param seriesBuilder the series
     */
    public void moveSeriesDown(JIPipeDesktopPlotSeriesEditor seriesBuilder) {
        int index = this.seriesBuilders.indexOf(seriesBuilder);
        if (index >= 0 && index < this.seriesBuilders.size() - 1) {
            this.seriesBuilders.set(index, this.seriesBuilders.get(index + 1));
            this.seriesBuilders.set(index + 1, seriesBuilder);
            parameterChangedEventEmitter.emit(new ParameterChangedEvent(this, "series"));
        }
    }

    /**
     * Moves the series up in order
     *
     * @param seriesBuilder the series
     */
    public void moveSeriesUp(JIPipeDesktopPlotSeriesEditor seriesBuilder) {
        int index = this.seriesBuilders.indexOf(seriesBuilder);
        if (index > 0) {
            this.seriesBuilders.set(index, this.seriesBuilders.get(index - 1));
            this.seriesBuilders.set(index - 1, seriesBuilder);
            parameterChangedEventEmitter.emit(new ParameterChangedEvent(this, "series"));
        }
    }

    /**
     * Adds a new series
     */
    public void addSeries() {
        JIPipeDesktopPlotSeriesEditor seriesBuilder = new JIPipeDesktopPlotSeriesEditor(this, plotType.getInfo());
        seriesBuilders.add(seriesBuilder);
        parameterChangedEventEmitter.emit(new ParameterChangedEvent(this, "series"));

        // Register events
        seriesBuilder.getColumnAssignments().getParameterChangedEventEmitter().subscribe(this);
        seriesBuilder.getParameterChangedEventEmitter().subscribe(this);
    }

    /**
     * Removes a series from the series builder
     *
     * @param seriesBuilder the series
     */
    public void removeSeries(JIPipeDesktopPlotSeriesEditor seriesBuilder) {
        this.seriesBuilders.remove(seriesBuilder);
        parameterChangedEventEmitter.emit(new ParameterChangedEvent(this, "series"));
    }


    /**
     * Removes the data
     *
     * @param data data
     */
    public void removeData(List<TableColumn> data) {
        for (TableColumn dataSource : data) {
            if (dataSource.isUserRemovable()) {
                availableData.inverse().remove(dataSource);
            }
        }
        parameterChangedEventEmitter.emit(new ParameterChangedEvent(this, "available-data"));
    }
}