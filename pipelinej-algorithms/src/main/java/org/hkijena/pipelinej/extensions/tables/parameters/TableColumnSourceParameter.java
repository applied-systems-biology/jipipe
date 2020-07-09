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

package org.hkijena.pipelinej.extensions.tables.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQValidatable;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;
import org.hkijena.pipelinej.extensions.parameters.predicates.StringPredicate;
import org.hkijena.pipelinej.extensions.tables.parameters.enums.TableColumnGeneratorParameter;

/**
 * Parameter that acts as source (via matching a column) or a generator
 */
public class TableColumnSourceParameter implements ACAQParameterCollection, ACAQValidatable {

    private EventBus eventBus = new EventBus();
    private Mode mode = Mode.PickColumn;
    private StringPredicate columnSource = new StringPredicate();
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
        this.columnSource = new StringPredicate(other.columnSource);
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
    public StringPredicate getColumnSource() {
        return columnSource;
    }

    @ACAQParameter("column-source")
    @JsonSetter("column-source")
    public void setColumnSource(StringPredicate columnSource) {
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

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (mode == Mode.PickColumn) {
            report.report(columnSource);
        } else if (mode == Mode.GenerateColumn) {
            report.report(generatorSource);
        }
    }

    /**
     * Modes are that a column is picked or one is generated
     */
    public enum Mode {
        PickColumn,
        GenerateColumn
    }
}
