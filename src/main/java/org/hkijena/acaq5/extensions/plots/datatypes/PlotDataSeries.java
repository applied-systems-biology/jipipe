package org.hkijena.acaq5.extensions.plots.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import ij.macro.Variable;
import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;

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
        super((ResultsTable) null);
    }

    /**
     * Creates a new instance from a {@link ResultsTable}
     * @param table the table
     */
    public PlotDataSeries(ResultsTable table) {
        super(table);
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public PlotDataSeries(PlotDataSeries other) {
        super(other);
        this.name = other.name;
    }

    /**
     * Gets a copy of a column by name
     * @param name column name
     * @return copy of the column data
     */
    public double[] getColumnAsDouble(String name) {
        int index = getTable().getColumnIndex(name);
        return getTable().getColumnAsDoubles(index);
    }

    /**
     * Gets a copy of a column by name
     * @param name column name
     * @return copy of the column data
     */
    public String[] getColumnAsString(String name) {
        int index = getTable().getColumnIndex(name);
        String[] column = new String[getTable().getCounter()];
        for(int i = 0; i < column.length; ++i) {
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
}
