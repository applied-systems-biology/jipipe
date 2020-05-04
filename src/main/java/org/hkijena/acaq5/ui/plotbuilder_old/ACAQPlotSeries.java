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

package org.hkijena.acaq5.ui.plotbuilder_old;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A whole data set for a plot.
 * This is equivalent to a table.
 */
public class ACAQPlotSeries {
    private Map<String, ACAQPlotSeriesColumn> columns = new HashMap<>();
    private Map<String, Object> parameters = new HashMap<>();
    private Map<String, Class> parameterTypes = new HashMap<>();
    private EventBus eventBus = new EventBus();
    private boolean enabled = true;

    /**
     * Creates new instance
     */
    public ACAQPlotSeries() {

    }

    /**
     * Adds a column
     *
     * @param name   Name
     * @param column Column
     */
    public void addColumn(String name, ACAQPlotSeriesColumn column) {
        columns.put(name, column);
        column.getEventBus().register(this);
    }

    /**
     * Triggered when column data was changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void handleColumnDataChangedEvent(ACAQPlotSeriesColumn.DataChangedEvent event) {
        eventBus.post(new DataChangedEvent(this));
    }

    /**
     * @return Columns
     */
    public Map<String, ACAQPlotSeriesColumn> getColumns() {
        return Collections.unmodifiableMap(columns);
    }

    /**
     * @return Event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Adds a parameter
     *
     * @param key          Unique parameter key
     * @param defaultValue Default value
     */
    public void addParameter(String key, Object defaultValue) {
        parameters.put(key, Objects.requireNonNull(defaultValue));
        parameterTypes.put(key, defaultValue.getClass());
    }

    /**
     * Gets the type of the parameter
     *
     * @param key Unique parameter key
     * @return Parameter field tye
     */
    public Class getParameterType(String key) {
        return parameterTypes.get(key);
    }

    /**
     * Gets parameter value
     *
     * @param key Unique parameter key
     * @return Parameter value
     */
    public Object getParameterValue(String key) {
        return parameters.get(key);
    }

    /**
     * Sets a parameter value
     *
     * @param key   Unique parameter key
     * @param value Parameter value
     */
    public void setParameterValue(String key, Object value) {
        parameters.put(key, Objects.requireNonNull(value));
        getEventBus().post(new DataChangedEvent(this));
    }

    /**
     * @return Sorted list of parameter names
     */
    public List<String> getParameterNames() {
        return parameters.keySet().stream().sorted().collect(Collectors.toList());
    }

    /**
     * Converts a column into a numeric column
     *
     * @param name Column name
     * @return Numeric column
     */
    public ACAQNumericPlotSeriesColumn getAsNumericColumn(String name) {
        return (ACAQNumericPlotSeriesColumn) columns.get(name);
    }

    /**
     * Converts a column into a string column
     *
     * @param name Column name
     * @return String column
     */
    public ACAQStringPlotSeriesColumn getAsStringColumn(String name) {
        return (ACAQStringPlotSeriesColumn) columns.get(name);
    }

    /**
     * @return The effective number of rows
     */
    public int getMaximumRequiredRowCount() {
        int count = 0;
        for (ACAQPlotSeriesColumn column : columns.values()) {
            count = Math.max(count, column.getRequiredRowCount());
        }
        return count;
    }

    /**
     * @return True if the series should be displayed
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables/Disables the series.
     * A disabled series will not be displayed.
     *
     * @param enabled Enables/Disables the series
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        getEventBus().post(new DataChangedEvent(this));
    }

    /**
     * Event when data is changed
     */
    public static class DataChangedEvent {
        private ACAQPlotSeries series;

        /**
         * @param series Event source
         */
        public DataChangedEvent(ACAQPlotSeries series) {
            this.series = series;
        }

        /**
         * @return Event source
         */
        public ACAQPlotSeries getSeries() {
            return series;
        }
    }
}
