package org.hkijena.acaq5.extensions.tables.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;

/**
 * Parameter that acts as source (via matching a column) or a generator
 */
public class TableColumnSourceParameter implements ACAQParameterCollection {

    private EventBus eventBus = new EventBus();
    private Mode mode = Mode.PickColumn;
    private StringFilter columnSource = new StringFilter();
    private TableColumnGeneratorParameter generatorSource = new TableColumnGeneratorParameter();

    /**
     * Creates a new instance
     */
    public TableColumnSourceParameter() {
    }

    /**
     * Copies the object
     *
     * @param other the original
     */
    public TableColumnSourceParameter(TableColumnSourceParameter other) {
        this.mode = other.mode;
        this.columnSource = new StringFilter(other.columnSource);
        this.generatorSource = new TableColumnGeneratorParameter(other.generatorSource);
    }

    @ACAQDocumentation(name = "Mode", description = "Which source is used")
    @ACAQParameter("mode")
    @JsonGetter("mode")
    public Mode getMode() {
        return mode;
    }

    @ACAQParameter("mode")
    @JsonSetter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @ACAQDocumentation(name = "Column source", description = "Source that picks a column")
    @ACAQParameter("column-source")
    @JsonGetter("column-source")
    public StringFilter getColumnSource() {
        return columnSource;
    }

    @ACAQParameter("column-source")
    @JsonSetter("column-source")
    public void setColumnSource(StringFilter columnSource) {
        this.columnSource = columnSource;
    }

    @ACAQDocumentation(name = "Generator source", description = "Source that generates a column")
    @ACAQParameter("generator-source")
    @JsonGetter("generator-source")
    public TableColumnGeneratorParameter getGeneratorSource() {
        return generatorSource;
    }

    @ACAQParameter("generator-source")
    @JsonSetter("generator-source")
    public void setGeneratorSource(TableColumnGeneratorParameter generatorSource) {
        this.generatorSource = generatorSource;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Modes are that a column is picked or one is generated
     */
    public enum Mode {
        PickColumn,
        GenerateColumn
    }
}
