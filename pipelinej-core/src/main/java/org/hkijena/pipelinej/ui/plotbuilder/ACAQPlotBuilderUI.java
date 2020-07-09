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

package org.hkijena.pipelinej.ui.plotbuilder;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import ij.macro.Variable;
import ij.measure.ResultsTable;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQValidatable;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.data.ACAQData;
import org.hkijena.pipelinej.api.data.ACAQDataDeclaration;
import org.hkijena.pipelinej.api.events.ParameterChangedEvent;
import org.hkijena.pipelinej.api.events.ParameterStructureChangedEvent;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;
import org.hkijena.pipelinej.api.registries.ACAQDatatypeRegistry;
import org.hkijena.pipelinej.extensions.tables.DoubleArrayTableColumn;
import org.hkijena.pipelinej.extensions.tables.StringArrayTableColumn;
import org.hkijena.pipelinej.extensions.tables.TableColumn;
import org.hkijena.pipelinej.extensions.parameters.editors.ACAQDataParameterSettings;
import org.hkijena.pipelinej.extensions.parameters.references.ACAQDataDeclarationRef;
import org.hkijena.pipelinej.extensions.plots.datatypes.PlotData;
import org.hkijena.pipelinej.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.pipelinej.extensions.plots.datatypes.PlotMetadata;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.ACAQWorkbenchPanel;
import org.hkijena.pipelinej.ui.components.DocumentTabPane;
import org.hkijena.pipelinej.ui.components.PlotReader;
import org.hkijena.pipelinej.ui.components.UserFriendlyErrorUI;
import org.hkijena.pipelinej.ui.parameters.ParameterPanel;
import org.hkijena.pipelinej.utils.ReflectionUtils;
import org.hkijena.pipelinej.utils.StringUtils;
import org.hkijena.pipelinej.utils.UIUtils;
import org.scijava.Priority;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * User interface for displaying and creating plots
 */
public class ACAQPlotBuilderUI extends ACAQWorkbenchPanel implements ACAQParameterCollection, ACAQValidatable {

    private EventBus eventBus = new EventBus();
    private ACAQDataDeclarationRef plotType = new ACAQDataDeclarationRef();
    private PlotData currentPlot;
    private JSplitPane splitPane;
    private BiMap<String, TableColumn> availableData = HashBiMap.create();
    private List<ACAQPlotSeriesBuilder> seriesBuilders = new ArrayList<>();
    private boolean isRebuilding = false;
    private PlotReader plotReader;

    /**
     * @param workbench the workbench
     */
    public ACAQPlotBuilderUI(ACAQWorkbench workbench) {
        super(workbench);
        initialize();
        rebuildPlot();
        installDefaultDataSources();
        this.eventBus.register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Create settings panel
        DocumentTabPane tabbedPane = new DocumentTabPane();
        tabbedPane.addTab("Settings", UIUtils.getIconFromResources("cog.png"),
                new ParameterPanel(getWorkbench(),
                        this,
                        null,
                        ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SCROLLING),
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);
        tabbedPane.addTab("Series", UIUtils.getIconFromResources("select-column.png"),
                new ACAQPlotSeriesListEditorUI(getWorkbench(), this),
                DocumentTabPane.CloseMode.withoutCloseButton);
        tabbedPane.addTab("Data", UIUtils.getIconFromResources("table.png"),
                new ACAQPlotAvailableDataManagerUI(getWorkbench(), this),
                DocumentTabPane.CloseMode.withoutCloseButton);

        // Create plot reader
        plotReader = new PlotReader();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, plotReader, tabbedPane);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.66);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });

        add(splitPane, BorderLayout.CENTER);
    }

    private void installDefaultDataSources() {
        for (Class<? extends ACAQData> klass : ACAQDatatypeRegistry.getInstance().getRegisteredDataTypes().values()) {
            if (TableColumn.isGeneratingTableColumn(klass)) {
                TableColumn dataSource = (TableColumn) ReflectionUtils.newInstance(klass);
                availableData.put(dataSource.getLabel(), dataSource);
            }
        }

    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Plot type").checkNonNull(getPlotType().getDeclaration(), this);
        if (currentPlot != null) {
            report.forCategory("Plot parameters").report(currentPlot);
        }
        for (int i = 0; i < seriesBuilders.size(); ++i) {
            report.forCategory("Series").forCategory("Series #" + (i + 1)).report(seriesBuilders.get(i));
        }

    }

    @ACAQDocumentation(name = "Plot type", description = "The type of plot to be generated.")
    @ACAQParameter(value = "plot-type", priority = Priority.HIGH)
    @ACAQDataParameterSettings(dataBaseClass = PlotData.class)
    public ACAQDataDeclarationRef getPlotType() {
        if (plotType == null) {
            plotType = new ACAQDataDeclarationRef();
        }
        return plotType;
    }

    @ACAQParameter("plot-type")
    public void setPlotType(ACAQDataDeclarationRef plotType) {
        this.plotType = plotType;

        updatePlotTypeParameters();
    }

    private void updatePlotTypeParameters() {
        boolean changedPlot = false;
        if (currentPlot == null || (plotType.getDeclaration() != null &&
                !Objects.equals(plotType.getDeclaration().getDataClass(), currentPlot.getClass()))) {
            if (plotType.getDeclaration() != null) {
                currentPlot = (PlotData) ACAQData.createInstance(plotType.getDeclaration().getDataClass());
                changedPlot = true;
                currentPlot.getEventBus().register(this);
            }
        } else if (plotType.getDeclaration() == null) {
            currentPlot = null;
            changedPlot = true;
        }
        if (changedPlot) {
            seriesBuilders.clear();

            getEventBus().post(new ParameterStructureChangedEvent(this));
            if (currentPlot != null) {
                PlotMetadata metadata = currentPlot.getClass().getAnnotation(PlotMetadata.class);
                for (int i = 0; i < metadata.minSeriesCount(); i++) {
                    addSeries();
                }
            }
        }
    }

    @ACAQDocumentation(name = "Plot parameters")
    @ACAQParameter("plot")
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
        this.plotType.setDeclaration(ACAQDataDeclaration.getInstance(data.getClass()));
        this.currentPlot = data;
        seriesBuilders.clear();
        List<PlotDataSeries> seriesList = new ArrayList<>(data.getSeries());
        for (PlotDataSeries series : seriesList) {
            ACAQPlotSeriesBuilder builder = new ACAQPlotSeriesBuilder(this, ACAQDataDeclaration.getInstance(data.getClass()));
            Map<String, String> columnMap = importData(series);
            for (Map.Entry<String, String> entry : columnMap.entrySet()) {
                builder.assignData(entry.getKey(), availableData.get(entry.getValue()));
            }
            seriesBuilders.add(builder);

            // Register events
            builder.getColumnAssignments().getEventBus().register(this);
            builder.getEventBus().register(this);
        }


        getEventBus().post(new ParameterStructureChangedEvent(this));
        currentPlot.getEventBus().register(this);
    }

    /**
     * Triggered when a parameter was changed
     *
     * @param event generated event
     */
    @Subscribe
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

            ACAQValidityReport report = new ACAQValidityReport();
            this.reportValidity(report);
            if (!report.isValid()) {
                UserFriendlyErrorUI errorUI = new UserFriendlyErrorUI(null, UserFriendlyErrorUI.WITH_SCROLLING);
                errorUI.displayErrors(report);
                errorUI.addVerticalGlue();
                splitPane.setLeftComponent(errorUI);
                return;
            }

            // Generate
            currentPlot.clearSeries();
            for (ACAQPlotSeriesBuilder seriesBuilder : seriesBuilders) {
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

        return columnMapping;
    }

    /**
     * Contains single-column series that will be later spliced together depending on the user's settings
     *
     * @return the list of avilable data
     */
    public Map<String, TableColumn> getAvailableData() {
        return Collections.unmodifiableMap(availableData);
    }

    public List<ACAQPlotSeriesBuilder> getSeriesBuilders() {
        return Collections.unmodifiableList(seriesBuilders);
    }

    /**
     * Moves the series down in order
     *
     * @param seriesBuilder the series
     */
    public void moveSeriesDown(ACAQPlotSeriesBuilder seriesBuilder) {
        int index = this.seriesBuilders.indexOf(seriesBuilder);
        if (index >= 0 && index < this.seriesBuilders.size() - 1) {
            this.seriesBuilders.set(index, this.seriesBuilders.get(index + 1));
            this.seriesBuilders.set(index + 1, seriesBuilder);
            eventBus.post(new ParameterChangedEvent(this, "series"));
        }
    }

    /**
     * Moves the series up in order
     *
     * @param seriesBuilder the series
     */
    public void moveSeriesUp(ACAQPlotSeriesBuilder seriesBuilder) {
        int index = this.seriesBuilders.indexOf(seriesBuilder);
        if (index > 0) {
            this.seriesBuilders.set(index, this.seriesBuilders.get(index - 1));
            this.seriesBuilders.set(index - 1, seriesBuilder);
            eventBus.post(new ParameterChangedEvent(this, "series"));
        }
    }

    /**
     * Adds a new series
     */
    public void addSeries() {
        ACAQPlotSeriesBuilder seriesBuilder = new ACAQPlotSeriesBuilder(this, plotType.getDeclaration());
        seriesBuilders.add(seriesBuilder);
        eventBus.post(new ParameterChangedEvent(this, "series"));

        // Register events
        seriesBuilder.getColumnAssignments().getEventBus().register(this);
        seriesBuilder.getEventBus().register(this);
    }

    /**
     * Removes a series from the series builder
     *
     * @param seriesBuilder the series
     */
    public void removeSeries(ACAQPlotSeriesBuilder seriesBuilder) {
        this.seriesBuilders.remove(seriesBuilder);
        eventBus.post(new ParameterChangedEvent(this, "series"));
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
        eventBus.post(new ParameterChangedEvent(this, "available-data"));
    }
}
