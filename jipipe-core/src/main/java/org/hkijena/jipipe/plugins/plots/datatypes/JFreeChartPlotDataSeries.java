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

package org.hkijena.jipipe.plugins.plots.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.io.IOException;

/**
 * A data series (table) that is rendered as plot series
 */
@JsonSerialize(using = JFreeChartPlotDataSeries.Serializer.class)
@JsonDeserialize(using = JFreeChartPlotDataSeries.Deserializer.class)
public class JFreeChartPlotDataSeries extends ResultsTableData implements JIPipeParameterCollection {
    private final ParameterChangedEventEmitter parameterChangedEventEmitter = new ParameterChangedEventEmitter();
    private final ParameterStructureChangedEventEmitter parameterStructureChangedEventEmitter = new ParameterStructureChangedEventEmitter();
    private final ParameterUIChangedEventEmitter parameterUIChangedEventEmitter = new ParameterUIChangedEventEmitter();
    private String name;

    /**
     * Creates a new instance with a null table
     */
    public JFreeChartPlotDataSeries() {
        super();
    }

    /**
     * Creates a new instance from a {@link ResultsTable}
     *
     * @param table the table
     */
    public JFreeChartPlotDataSeries(ResultsTable table) {
        super(table);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public JFreeChartPlotDataSeries(JFreeChartPlotDataSeries other) {
        super(other);
        this.name = other.name;
    }

    public static JFreeChartPlotDataSeries importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new JFreeChartPlotDataSeries(ResultsTableData.importData(storage, progressInfo).getTable());
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

    @SetJIPipeDocumentation(name = "Name", description = "Name of this data series")
    @JIPipeParameter("name")
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JIPipeParameter("name")
    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
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
    public String toString() {
        return getName() + " (" + getTable().getCounter() + " rows)";
    }

    public static class Serializer extends JsonSerializer<JFreeChartPlotDataSeries> {
        @Override
        public void serialize(JFreeChartPlotDataSeries resultsTableData, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("name", resultsTableData.name);
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<JFreeChartPlotDataSeries> {
        @Override
        public JFreeChartPlotDataSeries deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            JFreeChartPlotDataSeries resultsTableData = new JFreeChartPlotDataSeries();
            resultsTableData.name = node.get("name").asText();
            return resultsTableData;
        }
    }
}
