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

package org.hkijena.acaq5.extensions.parameters.matrix;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.utils.JsonUtils;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A 2D matrix of floats
 */
public abstract class Matrix2D<T> implements TableModel {

    private List<List<T>> rows = new ArrayList<>();
    private List<TableModelListener> listeners = new ArrayList<>();
    private Class<T> entryClass;

    /**
     * Creates a new instance
     *
     * @param entryClass the type of the entries
     */
    public Matrix2D(Class<T> entryClass) {
        this.entryClass = entryClass;
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Matrix2D(Matrix2D<T> other) {
        for (List<T> row : other.rows) {
            rows.add(new ArrayList<>(row));
        }
        this.entryClass = other.entryClass;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return rows.isEmpty() ? 1 : rows.get(0).size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return "" + columnIndex;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Float.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex).get(columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        rows.get(rowIndex).set(columnIndex, (T) aValue);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(new TableModelEvent(this, rowIndex, rowIndex, columnIndex));
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

    /**
     * Creates a new entry
     *
     * @return entry
     */
    protected abstract T createNewEntry();

    /**
     * Adds a new row
     */
    public void addRow() {
        List<T> row = new ArrayList<>();
        for (int i = 0; i < getColumnCount(); i++) {
            row.add(createNewEntry());
        }
        rows.add(row);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(new TableModelEvent(this, rows.size() - 1));
        }
    }

    /**
     * Removes a row
     *
     * @param rowIndex the row index
     */
    public void removeRow(int rowIndex) {
        rows.remove(rowIndex);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(new TableModelEvent(this));
        }
    }

    /**
     * Adds a new column
     */
    public void addColumn() {
        for (List<T> row : rows) {
            row.add(createNewEntry());
        }
    }

    /**
     * Removes a column
     *
     * @param columnIndex the column index
     */
    public void removeColumn(int columnIndex) {
        for (List<T> row : rows) {
            row.remove(columnIndex);
        }
        for (TableModelListener listener : listeners) {
            listener.tableChanged(new TableModelEvent(this));
        }
    }

    public Class<T> getEntryClass() {
        return entryClass;
    }

    /**
     * Serializes the parameter
     */
    public static class Serializer extends JsonSerializer<Matrix2D<?>> {
        @Override
        public void serialize(Matrix2D<?> objects, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("rows", objects.rows);
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes the parameter
     */
    public static class Deserializer<T> extends JsonDeserializer<Matrix2D<T>> implements ContextualDeserializer {

        private JavaType deserializedType;

        @Override
        public Matrix2D<T> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode root = jsonParser.readValueAsTree();

            Matrix2D<T> listParameter;
            try {
                listParameter = (Matrix2D<T>) deserializedType.getRawClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            ObjectReader objectReader = JsonUtils.getObjectMapper().readerFor(listParameter.getEntryClass());
            for (JsonNode rowElement : ImmutableList.copyOf(root.get("rows").elements())) {
                List rowList = new ArrayList<>();
                for (JsonNode columnElement : ImmutableList.copyOf(rowElement.elements())) {
                    Object item = objectReader.readValue(columnElement);
                    rowList.add(item);
                }
                listParameter.rows.add(rowList);
            }

            return listParameter;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            //beanProperty is null when the type to deserialize is the top-level type or a generic type, not a type of a bean property
            JavaType type = ctxt.getContextualType() != null
                    ? ctxt.getContextualType()
                    : property.getMember().getType();
            Matrix2D.Deserializer<?> deserializer = new Matrix2D.Deserializer<>();
            deserializer.deserializedType = type;
            return deserializer;
        }
    }
}
