package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.collections.StringRenamingList;
import org.hkijena.acaq5.extensions.parameters.filters.StringRenaming;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that removes columns
 */
@ACAQDocumentation(name = "Rename table column", description = "Renames columns")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class RenameColumnAlgorithm extends ACAQIteratingAlgorithm {

    private StringRenamingList renamingEntries = new StringRenamingList();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public RenameColumnAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public RenameColumnAlgorithm(RenameColumnAlgorithm other) {
        super(other);
        this.renamingEntries = new StringRenamingList(renamingEntries);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData table = (ResultsTableData) dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class).duplicate();
        for (StringRenaming renamingEntry : renamingEntries) {
            for (int col = 0; col < table.getColumnCount(); col++) {
                String oldName = table.getColumnName(col);
                String newName = renamingEntry.apply(oldName);
                if (!Objects.equals(oldName, newName)) {
                    table.renameColumn(oldName, newName);
                }
            }
        }
        dataInterface.addOutputData(getFirstOutputSlot(), table);
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
