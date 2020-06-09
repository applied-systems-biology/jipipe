package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.collections.StringRenamingList;
import org.hkijena.acaq5.extensions.parameters.filters.StringRenaming;
import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that removes columns
 */
@ACAQDocumentation(name = "Rename single columns", description = "Renames columns")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = TableColumn.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = TableColumn.class, slotName = "Output", autoCreate = true)
public class RenameColumnsAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private StringRenamingList renamingEntries = new StringRenamingList();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public RenameColumnsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public RenameColumnsAlgorithm(RenameColumnsAlgorithm other) {
        super(other);
        this.renamingEntries = new StringRenamingList(renamingEntries);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        TableColumn input = dataInterface.getInputData(getFirstInputSlot(), TableColumn.class);
        String name = input.getLabel();
        for (StringRenaming renamingEntry : renamingEntries) {
            if(renamingEntry.test(name)) {
                name = renamingEntry.apply(name);
                break;
            }
        }

        if(input.isNumeric()) {
            dataInterface.addOutputData(getFirstOutputSlot(), new DoubleArrayTableColumn(input.getDataAsDouble(input.getRows()), name));
        }
        else {
            dataInterface.addOutputData(getFirstOutputSlot(), new StringArrayTableColumn(input.getDataAsString(input.getRows()), name));
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Renaming entries").report(renamingEntries);
        for (int i = 0; i < renamingEntries.size(); i++) {
            if (StringUtils.isNullOrEmpty(renamingEntries.get(i).getValue())) {
                report.forCategory("Item #" + (i + 1)).reportIsInvalid("Target cannot be empty!",
                        "You cannot rename a column to an empty name!",
                        "Please change the target to a unique non-empty name.",
                        this);
            }
        }
    }

    @ACAQDocumentation(name = "Renaming entries", description = "You can rename one or multiple columns.")
    @ACAQParameter("renaming-entries")
    public StringRenamingList getRenamingEntries() {
        return renamingEntries;
    }

    @ACAQParameter("renaming-entries")
    public void setRenamingEntries(StringRenamingList renamingEntries) {
        this.renamingEntries = renamingEntries;
    }
}
