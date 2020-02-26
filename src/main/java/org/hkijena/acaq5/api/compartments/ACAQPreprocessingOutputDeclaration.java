package org.hkijena.acaq5.api.compartments;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQDefaultAlgorithmDeclaration;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQPreprocessingOutput;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTraitConfiguration;

public class ACAQPreprocessingOutputDeclaration extends ACAQDefaultAlgorithmDeclaration {
    public ACAQPreprocessingOutputDeclaration() {
        super(ACAQPreprocessingOutput.class);
    }

    @Override
    public ACAQAlgorithm newInstance() {
        throw new UnsupportedOperationException();
    }

    public static ACAQPreprocessingOutput createInstance(ACAQSlotConfiguration slotConfiguration,
                                                    ACAQTraitConfiguration traitConfiguration) {
        return new ACAQPreprocessingOutput(new ACAQPreprocessingOutputDeclaration(), slotConfiguration, traitConfiguration);
    }
}
