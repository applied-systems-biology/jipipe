/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.plotbuilder;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ACAQPlotSeries {
    private Map<String, ACAQPlotSeriesColumn> columns = new HashMap<>();
    private Map<String, Object> parameters = new HashMap<>();
    private Map<String, Class> parameterTypes = new HashMap<>();
    private EventBus eventBus = new EventBus();
    private boolean enabled = true;

    public ACAQPlotSeries() {

    }

    public void addColumn(String name, ACAQPlotSeriesColumn column) {
        columns.put(name, column);
        column.getEventBus().register(this);
    }

    @Subscribe
    public void handleColumnDataChangedEvent(ACAQPlotSeriesColumn.DataChangedEvent event) {
        eventBus.post(new DataChangedEvent(this));
    }

    public Map<String, ACAQPlotSeriesColumn> getColumns() {
        return Collections.unmodifiableMap(columns);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public void addParameter(String key, Object defaultValue) {
        parameters.put(key, Objects.requireNonNull(defaultValue));
        parameterTypes.put(key, defaultValue.getClass());
    }

    public Class getParameterType(String key) {
        return parameterTypes.get(key);
    }

    public Object getParameterValue(String key) {
        return parameters.get(key);
    }

    public void setParameterValue(String key, Object value) {
        parameters.put(key, Objects.requireNonNull(value));
        getEventBus().post(new DataChangedEvent(this));
    }

    public List<String> getParameterNames() {
        return parameters.keySet().stream().sorted().collect(Collectors.toList());
    }

    public ACAQNumericPlotSeriesColumn getAsNumericColumn(String name) {
        return (ACAQNumericPlotSeriesColumn) columns.get(name);
    }

    public ACAQStringPlotSeriesColumn getAsStringColumn(String name) {
        return (ACAQStringPlotSeriesColumn) columns.get(name);
    }

    public int getMaximumRequiredRowCount() {
        int count = 0;
        for (ACAQPlotSeriesColumn column : columns.values()) {
            count = Math.max(count, column.getRequiredRowCount());
        }
        return count;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        getEventBus().post(new DataChangedEvent(this));
    }

    public static class DataChangedEvent {
        private ACAQPlotSeries series;

        public DataChangedEvent(ACAQPlotSeries series) {
            this.series = series;
        }

        public ACAQPlotSeries getSeries() {
            return series;
        }
    }
}
