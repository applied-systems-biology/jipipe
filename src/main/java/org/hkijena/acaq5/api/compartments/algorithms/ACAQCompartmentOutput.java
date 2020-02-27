package org.hkijena.acaq5.api.compartments.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.compartments.ACAQCompartmentSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.traits.ACAQMutableTraitModifier;
import org.hkijena.acaq5.api.traits.ACAQTraitConfiguration;

@ACAQDocumentation(name = "Compartment output", description = "Output of a compartment")
public class ACAQCompartmentOutput extends ACAQAlgorithm {

    public ACAQCompartmentOutput(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQCompartmentSlotConfiguration(), null);
    }

    public ACAQCompartmentOutput(ACAQCompartmentOutput other) {
        super(other);
    }

    @Override
    public void run() {
        for (ACAQDataSlot<?> inputSlot : getInputSlots()) {
            ACAQDataSlot<?> outputSlot = getSlots().get("Output " + inputSlot.getName());
            outputSlot.setData(inputSlot.getData());
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
