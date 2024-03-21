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

package org.hkijena.jipipe.extensions.tables.parameters.enums;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.tables.ColumnContentType;

/**
 * A parameter that allows the user to setup a table column generator
 */
public class TableColumnGeneratorParameter implements JIPipeValidatable {
    private JIPipeDataInfoRef generatorType;
    private ColumnContentType generatedType;

    /**
     * Creates a new instance
     */
    public TableColumnGeneratorParameter() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public TableColumnGeneratorParameter(TableColumnGeneratorParameter other) {
        this.generatorType = new JIPipeDataInfoRef(other.generatorType);
        this.generatedType = other.generatedType;
    }

    @SetJIPipeDocumentation(name = "Generator", description = "Which generator is responsible for generating the column")
    @JsonGetter("generator-type")
    @JIPipeParameter("generator-type")
    public JIPipeDataInfoRef getGeneratorType() {
        if (generatorType == null)
            generatorType = new JIPipeDataInfoRef();
        return generatorType;
    }

    @JsonSetter("generator-type")
    @JIPipeParameter("generator-type")
    public void setGeneratorType(JIPipeDataInfoRef generatorType) {
        this.generatorType = generatorType;
    }

    @SetJIPipeDocumentation(name = "Generated type", description = "The column type to be generated")
    @JsonGetter("generated-type")
    @JIPipeParameter("generated-type")
    public ColumnContentType getGeneratedType() {
        return generatedType;
    }

    @JsonSetter("generated-type")
    @JIPipeParameter("generated-type")
    public void setGeneratedType(ColumnContentType generatedType) {
        this.generatedType = generatedType;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (getGeneratorType().getInfo() == null) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    reportContext,
                    "Generator not selected!",
                    "Please select a generator"));
        }
    }
}
