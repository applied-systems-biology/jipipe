package org.hkijena.acaq5.extensions.imagejalgorithms.ij1;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitConfiguration;

/**
 * Provides some methods to simplify integration of ImageJ1 plugins into ACAQ5
 */
public abstract class ImageJ1Algorithm extends ACAQIteratingAlgorithm {
    /**
     * Creates a new instance
     *
     * @param declaration        Algorithm declaration
     * @param slotConfiguration  Slot configuration override
     * @param traitConfiguration Trait configuration override
     */
    public ImageJ1Algorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration, ACAQTraitConfiguration traitConfiguration) {
        super(declaration, slotConfiguration, traitConfiguration);
    }

    /**
     * Creates a new instance
     *
     * @param declaration       Algorithm declaration
     * @param slotConfiguration Slot configuration override
     */
    public ImageJ1Algorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration) {
        super(declaration, slotConfiguration);
    }

    /**
     * Creates a new instance
     *
     * @param declaration Algorithm declaration
     */
    public ImageJ1Algorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ImageJ1Algorithm(ImageJ1Algorithm other) {
        super(other);
    }
}
