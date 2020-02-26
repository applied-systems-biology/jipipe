package org.hkijena.acaq5.api.compartments;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDefaultAlgorithmDeclaration;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQAlgorithmInput;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTraitConfiguration;

public class ACAQAlgorithmInputDeclaration extends ACAQDefaultAlgorithmDeclaration {
    public ACAQAlgorithmInputDeclaration() {
        super(ACAQAlgorithmInput.class);
    }

    @Override
    public ACAQAlgorithm newInstance() {
        throw new UnsupportedOperationException();
    }

    public static ACAQAlgorithmInput createInstance(ACAQSlotConfiguration slotConfiguration,
                                                    ACAQTraitConfiguration traitConfiguration) {
        return new ACAQAlgorithmInput(new ACAQAlgorithmInputDeclaration(), slotConfiguration, traitConfiguration);
    }
}
