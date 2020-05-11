package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that adds or replaces a column by a generated value
 */
@ACAQDocumentation(name = "Add or re-generate table column", description = "Adds a new column or replaces an existing table column")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class AddColumnAlgorithm extends ACAQIteratingAlgorithm {

    private String columnName = "";
    private boolean replaceIfExists = false;


    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public AddColumnAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public AddColumnAlgorithm(AddColumnAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {

    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if(StringUtils.isNullOrEmpty(columnName))
            report.forCategory("Column name").reportIsInvalid("Column name is empty!",
                    "The target column name cannot be empty.",
                    "Please provide a non-empty name.",
                    this);
    }

    @ACAQDocumentation(name = "Column name", description = "Name of the target column")
    @ACAQParameter("column-name")
    public String getColumnName() {
        return columnName;
    }

    @ACAQParameter("column-name")
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    @ACAQDocumentation(name = "Replace existing data", description = "If the target column exists, replace its content")
    @ACAQParameter("replace-existing")
    public boolean isReplaceIfExists() {
        return replaceIfExists;
    }

    @ACAQParameter("replace-existing")
    public void setReplaceIfExists(boolean replaceIfExists) {
        this.replaceIfExists = replaceIfExists;
    }
}
