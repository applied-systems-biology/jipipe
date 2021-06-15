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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotColumn;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotMetadata;
import org.hkijena.jipipe.extensions.plots.parameters.UIPlotDataSeriesColumnEnum;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Constructs a series from a set of columns
 */
public class JIPipePlotSeriesBuilder implements JIPipeParameterCollection, JIPipeValidatable {
    private PlotEditor plotBuilderUI;
    private JIPipeDataInfo plotType;
    private EventBus eventBus = new EventBus();
    private JIPipeDynamicParameterCollection columnAssignments = new JIPipeDynamicParameterCollection(false);
    private String name = "Series";
    private boolean enabled = true;

    /**
     * Creates a new instance
     *
     * @param plotBuilderUI the plot builder
     * @param plotType      the plot type this series builder is defined for
     */
    public JIPipePlotSeriesBuilder(PlotEditor plotBuilderUI, JIPipeDataInfo plotType) {
        this.plotBuilderUI = plotBuilderUI;
        this.plotType = plotType;

        PlotMetadata metadata = plotType.getDataClass().getAnnotation(PlotMetadata.class);
        for (PlotColumn column : metadata.columns()) {
            UIPlotDataSeriesColumnEnum parameter = new UIPlotDataSeriesColumnEnum();
            JIPipeMutableParameterAccess parameterAccess = new JIPipeMutableParameterAccess();
            parameterAccess.set(parameter);
            parameterAccess.setFieldClass(UIPlotDataSeriesColumnEnum.class);
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
    public Map<String, TableColumn> getSelectedSources() {
        Map<String, TableColumn> result = new HashMap<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
            UIPlotDataSeriesColumnEnum parameter = entry.getValue().get(UIPlotDataSeriesColumnEnum.class);
            result.put(entry.getKey(), (TableColumn) parameter.getValue());
        }
        return result;
    }

    /**
     * Updates the column parameters
     */
    public void updateSeriesList() {
        List<Object> allowedItems = new ArrayList<>(plotBuilderUI.getAvailableData().values());

        for (JIPipeParameterAccess value : columnAssignments.getParameters().values()) {
            JIPipeMutableParameterAccess parameterAccess = (JIPipeMutableParameterAccess) value;
            UIPlotDataSeriesColumnEnum parameter = parameterAccess.get(UIPlotDataSeriesColumnEnum.class);
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
    public void assignData(String column, TableColumn columnData) {
        JIPipeMutableParameterAccess parameterAccess = columnAssignments.getParameter(column);
        UIPlotDataSeriesColumnEnum parameter = parameterAccess.get(UIPlotDataSeriesColumnEnum.class);
        parameter.setValue(columnData);
        parameterAccess.set(parameter);
    }

    @JIPipeDocumentation(name = "Data assignments", description = "Please select which data should be assigned to which plot input.")
    @JIPipeParameter("column-assignments")
    public JIPipeDynamicParameterCollection getColumnAssignments() {
        return columnAssignments;
    }

    public JIPipeDataInfo getPlotType() {
        return plotType;
    }

    @JIPipeDocumentation(name = "Name", description = "Name shown in the plot")
    @JIPipeParameter("name")
    public String getName() {
        return name;
    }

    @JIPipeParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    public PlotEditor getPlotBuilderUI() {
        return plotBuilderUI;
    }

    @JIPipeDocumentation(name = "Enabled", description = "If the series is shown")
    @JIPipeParameter(value = "enabled", hidden = true)
    public boolean isEnabled() {
        return enabled;
    }

    @JIPipeParameter("enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        for (Map.Entry<String, JIPipeParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
            JIPipeMutableParameterAccess parameterAccess = (JIPipeMutableParameterAccess) entry.getValue();
            UIPlotDataSeriesColumnEnum parameter = parameterAccess.get(UIPlotDataSeriesColumnEnum.class);
            if (parameter.getValue() == null) {
                report.forCategory("Data assignments").forCategory(entry.getKey()).reportIsInvalid("No data selected!",
                        "The plot requires that you select a data source.",
                        "Please select a data source.",
                        this);
            }
        }

        int rows = 0;
        Map<String, TableColumn> selectedSources = getSelectedSources();
        for (TableColumn source : selectedSources.values()) {
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
        Map<String, TableColumn> selectedSources = getSelectedSources();
        for (TableColumn source : selectedSources.values()) {
            rows = Math.max(rows, source.getRows());
        }
        ResultsTable table = new ResultsTable(rows);
        PlotMetadata metadata = plotType.getDataClass().getAnnotation(PlotMetadata.class);

        Map<String, Boolean> columnIsNumeric = new HashMap<>();
        Map<String, Integer> columnIndices = new HashMap<>();
        for (PlotColumn column : metadata.columns()) {
            columnIsNumeric.put(column.name(), column.isNumeric());
            int columnIndex = table.getFreeColumn(column.name());
            columnIndices.put(column.name(), columnIndex);
        }

        for (Map.Entry<String, TableColumn> entry : selectedSources.entrySet()) {
            int columnIndex = columnIndices.get(entry.getKey());
            boolean isNumeric = columnIsNumeric.get(entry.getKey());
            if (!isNumeric) {
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
