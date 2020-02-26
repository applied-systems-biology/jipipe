package org.hkijena.acaq5.api.compartments.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.compartments.dataslots.ACAQPreprocessingOutputDataSlot;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTraitConfiguration;


@ACAQDocumentation(name="Preprocessing output")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Internal)
@AlgorithmOutputSlot(value = ACAQPreprocessingOutputDataSlot.class, slotName = "Data", autoCreate = true)

public class ACAQPreprocessingOutput extends ACAQAlgorithm {

    public ACAQPreprocessingOutput(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration, ACAQTraitConfiguration traitConfiguration) {
        super(declaration, slotConfiguration, traitConfiguration);
    }

    @Override
    protected ACAQSlotConfiguration copySlotConfiguration(ACAQAlgorithm other) {
        // The slot configuration is global
        return other.getSlotConfiguration();
    }

    @Override
    public void run() {

    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
