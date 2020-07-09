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

package org.hkijena.jipipe.extensions.tables.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.tables.parameters.enums.TableColumnGeneratorParameter;

/**
 * Parameter that acts as source (via matching a column) or a generator
 */
public class TableColumnSourceParameter implements JIPipeParameterCollection, JIPipeValidatable {

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

    @JIPipeDocumentation(name = "Mode", description = "Which source is used")
    @JIPipeParameter("mode")
    @JsonGetter("mode")
    public Mode getMode() {
        return mode;
    }

    @JIPipeParameter("mode")
    @JsonSetter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @JIPipeDocumentation(name = "Column source", description = "Source that picks a column")
    @JIPipeParameter("column-source")
    @JsonGetter("column-source")
    public StringPredicate getColumnSource() {
        return columnSource;
    }

    @JIPipeParameter("column-source")
    @JsonSetter("column-source")
    public void setColumnSource(StringPredicate columnSource) {
        this.columnSource = columnSource;
    }

    @JIPipeDocumentation(name = "Generator source", description = "Source that generates a column")
    @JIPipeParameter("generator-source")
    @JsonGetter("generator-source")
    public TableColumnGeneratorParameter getGeneratorSource() {
        return generatorSource;
    }

    @JIPipeParameter("generator-source")
    @JsonSetter("generator-source")
    public void setGeneratorSource(TableColumnGeneratorParameter generatorSource) {
        this.generatorSource = generatorSource;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
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
