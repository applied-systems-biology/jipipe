package org.hkijena.acaq5.api.compartments.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that passes the input to the output
 */
@ACAQDocumentation(name = "IO Interface", description = "Passes its input to its output without changes.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Miscellaneous)
public class IOInterfaceAlgorithm extends ACAQAlgorithm {

    /**
     * Creates a new instance.
     * Please do not use this constructor manually, but instead use {@link ACAQGraphNode}'s newInstance() method
     *
     * @param declaration The algorithm declaration
     */
    public IOInterfaceAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQIOSlotConfiguration());
    }

    /**
     * Creates a copy of the other algorithm
     *
     * @param other The original
     */
    public IOInterfaceAlgorithm(IOInterfaceAlgorithm other) {
        super(other);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            ACAQDataSlot outputSlot = getSlots().get("Output " + inputSlot.getName());
            outputSlot.copyFrom(inputSlot);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
