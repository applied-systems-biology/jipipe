package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Merges the input slot tables into one data slot
 */
@ACAQDocumentation(name = "Merge data slots", description = "Merges the input slots into one data slot")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor)
public class MergeDataSlots extends ACAQAlgorithm {

    /**
     * @param declaration the algorithm declaration
     */
    public MergeDataSlots(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder()
                .restrictOutputSlotCount(1)
                .build());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public MergeDataSlots(MergeDataSlots other) {
        super(other);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ACAQDataSlot outputSlot = getFirstOutputSlot();
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            outputSlot.copyFrom(inputSlot);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (getOutputSlots().isEmpty()) {
            report.reportIsInvalid("No output slot!",
                    "The result is put into the output slot.",
                    "Please add an output slot that is compatible to the input data.", this);
        } else {
            ACAQDataSlot outputSlot = getFirstOutputSlot();
            for (ACAQDataSlot inputSlot : getInputSlots()) {
                if (!outputSlot.getAcceptedDataType().isAssignableFrom(inputSlot.getAcceptedDataType())) {
                    report.forCategory("Slots").forCategory(inputSlot.getName())
                            .reportIsInvalid("Input slot is incompatible!",
                                    "Output data must fit to the input data.",
                                    "Please add an output slot that is compatible to the input data.", this);
                }
            }
        }
    }
}
