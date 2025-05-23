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

package org.hkijena.jipipe.plugins.parameters.library.table;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

/**
 * Table of other parameters
 */
@JsonSerialize(using = ParameterTable.Serializer.class)
@JsonDeserialize(using = ParameterTable.Deserializer.class)
public class ParameterTable implements TableModel {
    private final ModelChangedEventEmitter modelChangedEventEmitter = new ModelChangedEventEmitter();
    private List<ParameterColumn> columns = new ArrayList<>();
    private List<List<Object>> rows = new ArrayList<>();
    private List<TableModelListener> listeners = new ArrayList<>();
    private Supplier<List<Object>> rowGenerator;

    /**
     * Creates a new table
     */
    public ParameterTable() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ParameterTable(ParameterTable other) {
        for (ParameterColumn column : other.columns) {
            this.columns.add(new ParameterColumn(column));
        }
        for (List<Object> row : other.rows) {
            int columnIndex = 0;
            List<Object> thisRow = new ArrayList<>();
            for (Object o : row) {
                JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().
                        getInfoByFieldClass(columns.get(columnIndex).fieldClass);
                thisRow.add(info.duplicate(o));
                ++columnIndex;
            }
            this.rows.add(thisRow);
        }
    }

    public ModelChangedEventEmitter getModelChangedEventEmitter() {
        return modelChangedEventEmitter;
    }

    /**
     * Adds a new column to the table
     *
     * @param column       the column
     * @param initialValue the initial value
     */
    public void addColumn(ParameterColumn column, Object initialValue) {
        columns.add(column);
        JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(column.fieldClass);
        for (List<Object> row : rows) {
            row.add(info.duplicate(initialValue)); // Deep-copy!
        }
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columns.get(columnIndex).getName();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columns.get(columnIndex).getFieldClass();
    }

    public ParameterColumn getColumnInfo(int columnIndex) {
        return columns.get(columnIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex).get(columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        rows.get(rowIndex).set(columnIndex, aValue);
//        postTableModelChangedEvent();
        for (TableModelListener listener : listeners) {
            try {
                listener.tableChanged(new TableModelEvent(this, rowIndex));
            } catch (IndexOutOfBoundsException e) {
            }
        }
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    private void postTableModelChangedEvent() {
        // Breaks JXTable for some reason. Use workaround via event bus
//        for (TableModelListener listener : listeners) {
//            listener.tableChanged(new TableModelEvent(this));
//        }
        modelChangedEventEmitter.emit(new ModelChangedEvent(this, new TableModelEvent(this)));
    }

    /**
     * Adds a row with raw values.
     *
     * @param row the row
     */
    public void addRow(List<Object> row) {
        if (row.size() != columns.size())
            throw new IndexOutOfBoundsException("The row list must have as many elements as there are columns!");
//        for(int  i = 0; i < row.size(); ++i) {
//            if(row.get(i) != null && !columns.get(i).fieldClass.isAssignableFrom(row.get(i).getClass())) {
//                throw new ClassCastException("Cannot fit column " + i + " value! Type does not match!");
//            }
//        }
        rows.add(row);
        postTableModelChangedEvent();
    }

    /**
     * Creates a new empty row
     */
    public void addRow() {
        if (rowGenerator != null) {
            rows.add(rowGenerator.get());
        } else {
            List<Object> row = new ArrayList<>();

            for (ParameterColumn column : columns) {
                JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(column.fieldClass);
                row.add(info.newInstance());
            }
            rows.add(row);
        }
        postTableModelChangedEvent();
    }

    /**
     * Removes the row with the specified index
     *
     * @param index the index
     */
    public void removeRow(int index) {
        rows.remove(index);
        postTableModelChangedEvent();
    }

    public Supplier<List<Object>> getRowGenerator() {
        return rowGenerator;
    }

    public void setRowGenerator(Supplier<List<Object>> rowGenerator) {
        this.rowGenerator = rowGenerator;
    }

    /**
     * Returns the column definition of given index
     *
     * @param col column index
     * @return column definition
     */
    public ParameterColumn getColumn(int col) {
        return columns.get(col);
    }

    /**
     * Removes the specified column
     *
     * @param col the column index
     */
    public void removeColumn(int col) {
        columns.remove(col);
        for (List<Object> row : rows) {
            row.remove(col);
        }
        postTableModelChangedEvent();
    }

    public boolean containsColumn(String column) {
        return columns.stream().anyMatch(c -> Objects.equals(column, c.getKey()));
    }

    public int getColumnIndex(String column) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getKey().equals(column))
                return i;
        }
        return -1;
    }

    /**
     * Clears the table
     */
    public void clear() {
        rows.clear();
        columns.clear();
        postTableModelChangedEvent();
    }

    public interface ModelChangedEventListener {
        void onParameterTableModelChangedEvent(ModelChangedEvent event);
    }

    /**
     * Column in the parameter table
     */
    public static class ParameterColumn {
        private String name;
        private String description;
        private String key;
        private Class<?> fieldClass;

        /**
         * Creates a new instance.
         * Values are null
         */
        public ParameterColumn() {
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public ParameterColumn(ParameterColumn other) {
            this.name = other.name;
            this.description = other.description;
            this.key = other.key;
            this.fieldClass = other.fieldClass;
        }

        /**
         * Creates a new instance
         *
         * @param name       The parameter name
         * @param key        The parameter key
         * @param fieldClass The parameter data class
         */
        public ParameterColumn(String name, String key, Class<?> fieldClass) {
            this.name = name;
            this.key = key;
            this.fieldClass = fieldClass;
        }

        @JsonGetter("name")
        public String getName() {
            return name;
        }

        @JsonGetter("name")
        public void setName(String name) {
            this.name = name;
        }

        public Class<?> getFieldClass() {
            return fieldClass;
        }

        public void setFieldClass(Class<?> fieldClass) {
            this.fieldClass = fieldClass;
        }

        @JsonGetter("field-class-id")
        public String getFieldClassInfoId() {
            return JIPipe.getParameterTypes().getInfoByFieldClass(fieldClass).getId();
        }

        @JsonSetter("field-class-id")
        public void setFieldClassInfoId(String id) {
            fieldClass = JIPipe.getParameterTypes().getInfoById(id).getFieldClass();
        }

        @JsonGetter("key")
        public String getKey() {
            return key;
        }

        @JsonSetter("key")
        public void setKey(String key) {
            this.key = key;
        }

        @JsonGetter("description")
        public String getDescription() {
            return description;
        }

        @JsonSetter("description")
        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * Serializes a {@link ParameterTable}
     */
    public static class Serializer extends JsonSerializer<ParameterTable> {
        @Override
        public void serialize(ParameterTable value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeStartObject();
            gen.writeObjectField("columns", value.columns);
            gen.writeFieldName("rows");
            gen.writeStartArray();
            for (List<Object> row : value.rows) {
                gen.writeStartObject();
                for (int col = 0; col < value.columns.size(); ++col) {
                    gen.writeObjectField(value.columns.get(col).key, row.get(col));
                }
                gen.writeEndObject();
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    /**
     * Deserializes a {@link ParameterTable}
     */
    public static class Deserializer extends JsonDeserializer<ParameterTable> {
        @Override
        public ParameterTable deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ParameterTable table = new ParameterTable();
            JsonNode treeNode = p.readValueAsTree();

            table.columns = JsonUtils.getObjectMapper().readerFor(new TypeReference<List<ParameterColumn>>() {
            }).readValue(treeNode.get("columns"));
            Map<String, Integer> columnKeyToIndexMap = new HashMap<>();
            for (int col = 0; col < table.columns.size(); ++col) {
                columnKeyToIndexMap.put(table.columns.get(col).key, col);
            }

            for (JsonNode rowNode : ImmutableList.copyOf(treeNode.get("rows").elements())) {
                ImmutableList<Map.Entry<String, JsonNode>> fields = ImmutableList.copyOf(rowNode.fields());
                List<Object> row = new ArrayList<>();
                for (int i = 0; i < fields.size(); ++i) {
                    row.add(null);
                }
                for (Map.Entry<String, JsonNode> entry : fields) {
                    int col = columnKeyToIndexMap.get(entry.getKey());
                    Class<?> columnClass = table.columns.get(col).fieldClass;
                    row.set(col, JsonUtils.getObjectMapper().readerFor(columnClass).readValue(entry.getValue()));
                }
                table.rows.add(row);
            }

            return table;
        }
    }

    public static class ModelChangedEvent extends AbstractJIPipeEvent {
        private final ParameterTable parameterTable;
        private final TableModelEvent tableModelEvent;

        public ModelChangedEvent(ParameterTable parameterTable, TableModelEvent tableModelEvent) {
            super(parameterTable);
            this.parameterTable = parameterTable;
            this.tableModelEvent = tableModelEvent;
        }

        public ParameterTable getParameterTable() {
            return parameterTable;
        }

        public TableModelEvent getTableModelEvent() {
            return tableModelEvent;
        }
    }

    public static class ModelChangedEventEmitter extends JIPipeEventEmitter<ModelChangedEvent, ModelChangedEventListener> {

        @Override
        protected void call(ModelChangedEventListener modelChangedEventListener, ModelChangedEvent event) {
            modelChangedEventListener.onParameterTableModelChangedEvent(event);
        }
    }
}
