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

package org.hkijena.pipelinej.extensions.plots.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import ij.measure.ResultsTable;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;
import org.hkijena.pipelinej.extensions.tables.ResultsTableData;

/**
 * A data series (table) that is rendered as plot series
 */
public class PlotDataSeries extends ResultsTableData implements ACAQParameterCollection {

    private EventBus eventBus = new EventBus();
    private String name;

    /**
     * Creates a new instance with a null table
     */
    public PlotDataSeries() {
        super();
    }

    /**
     * Creates a new instance from a {@link ResultsTable}
     *
     * @param table the table
     */
    public PlotDataSeries(ResultsTable table) {
        super(table);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public PlotDataSeries(PlotDataSeries other) {
        super(other);
        this.name = other.name;
    }

    /**
     * Gets a copy of a column by name
     *
     * @param name column name
     * @return copy of the column data
     */
    public double[] getColumnAsDouble(String name) {
        int index = getTable().getColumnIndex(name);
        return getTable().getColumnAsDoubles(index);
    }

    /**
     * Gets a copy of a column by name
     *
     * @param name column name
     * @return copy of the column data
     */
    public String[] getColumnAsString(String name) {
        int index = getTable().getColumnIndex(name);
        String[] column = new String[getTable().getCounter()];
        for (int i = 0; i < column.length; ++i) {
            column[i] = getTable().getStringValue(index, i);
        }
        return column;
    }

    @ACAQDocumentation(name = "Name", description = "Name of this data series")
    @ACAQParameter("name")
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @ACAQParameter("name")
    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public String toString() {
        return getName() + " (" + getTable().getCounter() + " rows)";
    }
}
