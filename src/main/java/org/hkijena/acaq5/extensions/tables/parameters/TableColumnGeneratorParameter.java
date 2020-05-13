package org.hkijena.acaq5.extensions.tables.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.parameters.references.ACAQDataDeclarationRef;
import org.hkijena.acaq5.extensions.tables.ColumnContentType;

/**
 * A parameter that allows the user to setup a table column generator
 */
public class TableColumnGeneratorParameter implements ACAQValidatable {
    private ACAQDataDeclarationRef generatorType;
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
        this.generatorType = new ACAQDataDeclarationRef(other.generatorType);
        this.generatedType = other.generatedType;
    }

    @ACAQDocumentation(name = "Generator", description = "Which generator is responsible for generating the column")
    @JsonGetter("generator-type")
    @ACAQParameter("generator-type")
    public ACAQDataDeclarationRef getGeneratorType() {
        if (generatorType == null)
            generatorType = new ACAQDataDeclarationRef();
        return generatorType;
    }

    @JsonSetter("generator-type")
    @ACAQParameter("generator-type")
    public void setGeneratorType(ACAQDataDeclarationRef generatorType) {
        this.generatorType = generatorType;
    }

    @ACAQDocumentation(name = "Generated type", description = "The column type to be generated")
    @JsonGetter("generated-type")
    @ACAQParameter("generated-type")
    public ColumnContentType getGeneratedType() {
        return generatedType;
    }

    @JsonSetter("generated-type")
    @ACAQParameter("generated-type")
    public void setGeneratedType(ColumnContentType generatedType) {
        this.generatedType = generatedType;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Generator").checkNonNull(getGeneratorType().getDeclaration(), this);
    }
}
