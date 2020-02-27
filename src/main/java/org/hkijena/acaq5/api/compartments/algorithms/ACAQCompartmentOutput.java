package org.hkijena.acaq5.api.compartments.algorithms;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTraitConfiguration;

public class ACAQCompartmentOutput extends ACAQAlgorithm {

    public ACAQCompartmentOutput(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration, ACAQTraitConfiguration traitConfiguration) {
        super(declaration, slotConfiguration, traitConfiguration);
    }

    @Override
    public void run() {

    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
