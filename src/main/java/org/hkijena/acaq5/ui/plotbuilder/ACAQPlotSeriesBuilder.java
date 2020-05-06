package org.hkijena.acaq5.ui.plotbuilder;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotColumn;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotMetadata;
import org.hkijena.acaq5.extensions.plots.parameters.UIPlotDataSourceEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Constructs a series from a set of columns
 */
public class ACAQPlotSeriesBuilder implements ACAQParameterCollection, ACAQValidatable {
    private ACAQPlotBuilderUI plotBuilderUI;
    private ACAQDataDeclaration plotType;
    private EventBus eventBus = new EventBus();
    private ACAQDynamicParameterCollection columnAssignments = new ACAQDynamicParameterCollection(false);
    private String name = "Series";
    private boolean enabled = true;

    /**
     * Creates a new instance
     *
     * @param plotBuilderUI the plot builder
     * @param plotType      the plot type this series builder is defined for
     */
    public ACAQPlotSeriesBuilder(ACAQPlotBuilderUI plotBuilderUI, ACAQDataDeclaration plotType) {
        this.plotBuilderUI = plotBuilderUI;
        this.plotType = plotType;

        PlotMetadata metadata = plotType.getDataClass().getAnnotation(PlotMetadata.class);
        for (PlotColumn column : metadata.columns()) {
            UIPlotDataSourceEnum parameter = new UIPlotDataSourceEnum();
            ACAQMutableParameterAccess parameterAccess = new ACAQMutableParameterAccess();
            parameterAccess.set(parameter);
            parameterAccess.setFieldClass(UIPlotDataSourceEnum.class);
            parameterAccess.setKey(column.name());
            parameterAccess.setName(column.name());
            columnAssignments.addParameter(parameterAccess);
        }
        updateSeriesList();

        plotBuilderUI.getEventBus().register(this);
    }

    /**
     * Returns the currently selected sources.
     *
     * @return map of series column ID to source
     */
    public Map<String, PlotDataSource> getSelectedSources() {
        Map<String, PlotDataSource> result = new HashMap<>();
        for (Map.Entry<String, ACAQParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
            UIPlotDataSourceEnum parameter = entry.getValue().get();
            result.put(entry.getKey(), (PlotDataSource) parameter.getValue());
        }
        return result;
    }

    /**
     * Updates the column parameters
     */
    public void updateSeriesList() {
        List<Object> allowedItems = new ArrayList<>(plotBuilderUI.getAvailableData().values());

        for (ACAQParameterAccess value : columnAssignments.getParameters().values()) {
            ACAQMutableParameterAccess parameterAccess = (ACAQMutableParameterAccess) value;
            UIPlotDataSourceEnum parameter = parameterAccess.get();
            parameter.setAllowedValues(allowedItems);
        }

        getEventBus().post(new ParameterStructureChangedEvent(this));
    }

    /**
     * Triggered when the plot builder's parameters are changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onParametersChanged(ParameterChangedEvent event) {
        if (event.getKey().equals("available-data")) {
            updateSeriesList();
        }
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Assigns data to a column
     *
     * @param column     the series column
     * @param columnData the assigned data
     */
    public void assignData(String column, PlotDataSource columnData) {
        ACAQMutableParameterAccess parameterAccess = columnAssignments.getParameter(column);
        UIPlotDataSourceEnum parameter = parameterAccess.get();
        parameter.setValue(columnData);
        parameterAccess.set(parameter);
    }

    @ACAQDocumentation(name = "Data assignments", description = "Please select which data should be assigned to which plot input.")
    @ACAQParameter("column-assignments")
    public ACAQDynamicParameterCollection getColumnAssignments() {
        return columnAssignments;
    }

    public ACAQDataDeclaration getPlotType() {
        return plotType;
    }

    @ACAQDocumentation(name = "Name", description = "Name shown in the plot")
    @ACAQParameter("name")
    public String getName() {
        return name;
    }

    @ACAQParameter("name")
    public void setName(String name) {
        this.name = name;
        getEventBus().post(new ParameterChangedEvent(this, "name"));
    }

    public ACAQPlotBuilderUI getPlotBuilderUI() {
        return plotBuilderUI;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        getEventBus().post(new ParameterChangedEvent(this, "enabled"));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (Map.Entry<String, ACAQParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
            ACAQMutableParameterAccess parameterAccess = (ACAQMutableParameterAccess) entry.getValue();
            UIPlotDataSourceEnum parameter = parameterAccess.get();
            if (parameter.getValue() == null) {
                report.forCategory("Data assignments").forCategory(entry.getKey()).reportIsInvalid("No data selected!",
                        "The plot requires that you select a data source.",
                        "Please select a data source.",
                        this);
            }
        }

        int rows = 0;
        Map<String, PlotDataSource> selectedSources = getSelectedSources();
        for (PlotDataSource source : selectedSources.values()) {
            if (source != null)
                rows = Math.max(rows, source.getRows());
        }
        if (rows == 0) {
            report.forCategory("Data assignments").forCategory("Data integrity").reportIsInvalid("Selected data is empty!",
                    "The plot requires that you select a data source.",
                    "Please select at least one data source with a known row count.",
                    this);
        }
    }

    /**
     * Generates a series
     *
     * @return Generated series
     */
    public PlotDataSeries buildSeries() {
        int rows = 0;
        Map<String, PlotDataSource> selectedSources = getSelectedSources();
        for (PlotDataSource source : selectedSources.values()) {
            rows = Math.max(rows, source.getRows());
        }
        ResultsTable table = new ResultsTable(rows);
        PlotMetadata metadata = plotType.getDataClass().getAnnotation(PlotMetadata.class);

        Map<String, Class<?>> columnClasses = new HashMap<>();
        Map<String, Integer> columnIndices = new HashMap<>();
        for (PlotColumn column : metadata.columns()) {
            columnClasses.put(column.name(), column.dataType());
            int columnIndex = table.getFreeColumn(column.name());
            columnIndices.put(column.name(), columnIndex);
        }

        for (Map.Entry<String, PlotDataSource> entry : selectedSources.entrySet()) {
            int columnIndex = columnIndices.get(entry.getKey());
            Class<?> columnClass = columnClasses.get(entry.getKey());
            if (columnClass == String.class) {
                String[] data = entry.getValue().getDataAsString(rows);
                for (int i = 0; i < data.length; i++) {
                    table.setValue(columnIndex, i, data[i]);
                }
            } else {
                double[] data = entry.getValue().getDataAsDouble(rows);
                for (int i = 0; i < data.length; i++) {
                    table.setValue(columnIndex, i, data[i]);
                }
            }
        }

        PlotDataSeries dataSeries = new PlotDataSeries(table);
        dataSeries.setName(getName());
        return dataSeries;
    }
}
