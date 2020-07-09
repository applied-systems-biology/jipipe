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

package org.hkijena.pipelinej.extensions.tables.algorithms;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.tables.ResultsTableData;
import org.hkijena.pipelinej.extensions.parameters.pairs.PairParameterSettings;
import org.hkijena.pipelinej.extensions.parameters.pairs.StringFilterAndStringPair;
import org.hkijena.pipelinej.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.pipelinej.utils.StringUtils;

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
public class RenameTableColumnsAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private StringFilterAndStringPair.List renamingEntries = new StringFilterAndStringPair.List();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public RenameTableColumnsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public RenameTableColumnsAlgorithm(RenameTableColumnsAlgorithm other) {
        super(other);
        this.renamingEntries = new StringFilterAndStringPair.List(other.renamingEntries);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData table = (ResultsTableData) dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class).duplicate();
        for (StringFilterAndStringPair renamingEntry : renamingEntries) {
            for (int col = 0; col < table.getColumnCount(); col++) {
                String oldName = table.getColumnName(col);
                String newName = renamingEntry.getKey().test(oldName) ? renamingEntry.getValue() : oldName;
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
    @StringParameterSettings(monospace = true)
    @PairParameterSettings(singleRow = false, keyLabel = "From", valueLabel = "To")
    @ACAQParameter("renaming-entries")
    public StringFilterAndStringPair.List getRenamingEntries() {
        return renamingEntries;
    }

    @ACAQParameter("renaming-entries")
    public void setRenamingEntries(StringFilterAndStringPair.List renamingEntries) {
        this.renamingEntries = renamingEntries;
    }
}
