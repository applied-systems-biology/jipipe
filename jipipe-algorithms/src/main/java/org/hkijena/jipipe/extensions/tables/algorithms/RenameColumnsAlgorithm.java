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

package org.hkijena.jipipe.extensions.tables.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.StringFilterAndStringPair;
import org.hkijena.jipipe.extensions.tables.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.StringArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.TableColumn;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that removes columns
 */
@JIPipeDocumentation(name = "Rename single columns", description = "Renames columns")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = TableColumn.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = TableColumn.class, slotName = "Output", autoCreate = true)
public class RenameColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringFilterAndStringPair.List renamingEntries = new StringFilterAndStringPair.List();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public RenameColumnsAlgorithm(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public RenameColumnsAlgorithm(RenameColumnsAlgorithm other) {
        super(other);
        this.renamingEntries = new StringFilterAndStringPair.List(other.renamingEntries);
    }

    @Override
    protected void runIteration(JIPipeDataInterface dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        TableColumn input = dataInterface.getInputData(getFirstInputSlot(), TableColumn.class);
        String name = input.getLabel();
        for (StringFilterAndStringPair renamingEntry : renamingEntries) {
            if (renamingEntry.getKey().test(name)) {
                name = renamingEntry.getValue();
                break;
            }
        }

        if (input.isNumeric()) {
            dataInterface.addOutputData(getFirstOutputSlot(), new DoubleArrayTableColumn(input.getDataAsDouble(input.getRows()), name));
        } else {
            dataInterface.addOutputData(getFirstOutputSlot(), new StringArrayTableColumn(input.getDataAsString(input.getRows()), name));
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
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

    @JIPipeDocumentation(name = "Renaming entries", description = "You can rename one or multiple columns.")
    @JIPipeParameter("renaming-entries")
    public StringFilterAndStringPair.List getRenamingEntries() {
        return renamingEntries;
    }

    @JIPipeParameter("renaming-entries")
    public void setRenamingEntries(StringFilterAndStringPair.List renamingEntries) {
        this.renamingEntries = renamingEntries;
    }
}
