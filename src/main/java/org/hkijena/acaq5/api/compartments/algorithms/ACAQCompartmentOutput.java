package org.hkijena.acaq5.api.compartments.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQIOSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A graph compartment output
 * Transfers data 1:1 from input to output
 */
@ACAQDocumentation(name = "Compartment output", description = "Output of a compartment")
public class ACAQCompartmentOutput extends ACAQAlgorithm {

    /**
     * Creates a new instance.
     * Please do not use this constructor manually, but instead use {@link ACAQAlgorithm}'s newInstance() method
     *
     * @param declaration The algorithm declaration
     */
    public ACAQCompartmentOutput(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQIOSlotConfiguration());
    }

    /**
     * Creates a copy of the other algorithm
     *
     * @param other The original
     */
    public ACAQCompartmentOutput(ACAQCompartmentOutput other) {
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
