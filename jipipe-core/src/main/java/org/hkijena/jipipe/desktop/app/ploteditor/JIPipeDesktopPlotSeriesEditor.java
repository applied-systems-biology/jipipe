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
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotColumn;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotDataSeries;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotMetadata;
import org.hkijena.jipipe.plugins.plots.parameters.UIPlotDataSeriesColumnEnum;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Constructs a series from a set of columns
 */
public class JIPipeDesktopPlotSeriesEditor extends AbstractJIPipeParameterCollection implements JIPipeValidatable, JIPipeParameterCollection.ParameterChangedEventListener {
    private final JFreeChartPlotEditor plotBuilderUI;
    private final JIPipeDataInfo plotType;
    private final JIPipeDynamicParameterCollection columnAssignments = new JIPipeDynamicParameterCollection(false);
    private String name = "Series";
    private boolean enabled = true;

    /**
     * Creates a new instance
     *
     * @param plotBuilderUI the plot builder
     * @param plotType      the plot type this series builder is defined for
     */
    public JIPipeDesktopPlotSeriesEditor(JFreeChartPlotEditor plotBuilderUI, JIPipeDataInfo plotType) {
        this.plotBuilderUI = plotBuilderUI;
        this.plotType = plotType;

        JFreeChartPlotMetadata metadata = plotType.getDataClass().getAnnotation(JFreeChartPlotMetadata.class);
        for (JFreeChartPlotColumn column : metadata.columns()) {
            UIPlotDataSeriesColumnEnum parameter = new UIPlotDataSeriesColumnEnum();
            JIPipeMutableParameterAccess parameterAccess = new JIPipeMutableParameterAccess();
            parameterAccess.set(parameter);
            parameterAccess.setFieldClass(UIPlotDataSeriesColumnEnum.class);
            parameterAccess.setKey(column.name());
            parameterAccess.setName(column.name());
            columnAssignments.addParameter(parameterAccess);
        }
        updateSeriesList();

        plotBuilderUI.getParameterChangedEventEmitter().subscribe(this);
    }

    /**
     * Returns the currently selected sources.
     *
     * @return map of series column ID to source
     */
    public Map<String, TableColumnData> getSelectedSources() {
        Map<String, TableColumnData> result = new HashMap<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
            UIPlotDataSeriesColumnEnum parameter = entry.getValue().get(UIPlotDataSeriesColumnEnum.class);
            result.put(entry.getKey(), (TableColumnData) parameter.getValue());
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

        getParameterStructureChangedEventEmitter().emit(new ParameterStructureChangedEvent(this));
    }

    /**
     * Assigns data to a column
     *
     * @param column     the series column
     * @param columnData the assigned data
     */
    public void assignData(String column, TableColumnData columnData) {
        JIPipeMutableParameterAccess parameterAccess = columnAssignments.getParameter(column);
        UIPlotDataSeriesColumnEnum parameter = parameterAccess.get(UIPlotDataSeriesColumnEnum.class);
        parameter.setValue(columnData);
        parameterAccess.set(parameter);
    }

    @SetJIPipeDocumentation(name = "Data assignments", description = "Please select which data should be assigned to which plot input.")
    @JIPipeParameter("column-assignments")
    public JIPipeDynamicParameterCollection getColumnAssignments() {
        return columnAssignments;
    }

    public JIPipeDataInfo getPlotType() {
        return plotType;
    }

    @SetJIPipeDocumentation(name = "Name", description = "Name shown in the plot")
    @JIPipeParameter("name")
    public String getName() {
        return name;
    }

    @JIPipeParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    public JFreeChartPlotEditor getPlotBuilderUI() {
        return plotBuilderUI;
    }

    @SetJIPipeDocumentation(name = "Enabled", description = "If the series is shown")
    @JIPipeParameter(value = "enabled", hidden = true)
    public boolean isEnabled() {
        return enabled;
    }

    @JIPipeParameter("enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        for (Map.Entry<String, JIPipeParameterAccess> entry : columnAssignments.getParameters().entrySet()) {
            JIPipeMutableParameterAccess parameterAccess = (JIPipeMutableParameterAccess) entry.getValue();
            UIPlotDataSeriesColumnEnum parameter = parameterAccess.get(UIPlotDataSeriesColumnEnum.class);
            if (parameter.getValue() == null) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new CustomValidationReportContext("Data assignments: " + entry.getKey()),
                        "No data selected!",
                        "The plot requires that you select a data source.",
                        "Please select a data source."));
            }
        }

        int rows = 0;
        Map<String, TableColumnData> selectedSources = getSelectedSources();
        for (TableColumnData source : selectedSources.values()) {
            if (source != null)
                rows = Math.max(rows, source.getRows());
        }
        if (rows == 0) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new CustomValidationReportContext("Data integrity"),
                    "Selected data is empty!",
                    "The plot requires that you select a data source.",
                    "Please select at least one data source with a known row count."));
        }
    }

    /**
     * Generates a series
     *
     * @return Generated series
     */
    public JFreeChartPlotDataSeries buildSeries() {
        int rows = 0;
        Map<String, TableColumnData> selectedSources = getSelectedSources();
        for (TableColumnData source : selectedSources.values()) {
            rows = Math.max(rows, source.getRows());
        }
        ResultsTable table = new ResultsTable(rows);
        JFreeChartPlotMetadata metadata = plotType.getDataClass().getAnnotation(JFreeChartPlotMetadata.class);

        Map<String, Boolean> columnIsNumeric = new HashMap<>();
        Map<String, Integer> columnIndices = new HashMap<>();
        for (JFreeChartPlotColumn column : metadata.columns()) {
            columnIsNumeric.put(column.name(), column.isNumeric());
            int columnIndex = table.getFreeColumn(column.name());
            columnIndices.put(column.name(), columnIndex);
        }

        for (Map.Entry<String, TableColumnData> entry : selectedSources.entrySet()) {
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

        JFreeChartPlotDataSeries dataSeries = new JFreeChartPlotDataSeries(table);
        dataSeries.setName(getName());
        return dataSeries;
    }

    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        if (event.getKey().equals("available-data")) {
            updateSeriesList();
        }
    }
}
