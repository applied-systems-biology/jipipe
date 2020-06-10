package org.hkijena.acaq5.extensions.imagejalgorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQDefaultTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

/**
 * Discriminator that describes an image index position
 */
@ACAQDocumentation(name = "ROI index", description = "References an index of a ROI within a ROI list.")
public class ROIIndexTrait extends ACAQDefaultTrait {
    /**
     * Creates a new discriminator
     *
     * @param declaration The declaration
     * @param value       The value
     */
    public ROIIndexTrait(ACAQTraitDeclaration declaration, String value) {
        super(declaration, value);
    }
}
