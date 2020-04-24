package org.hkijena.acaq5.extensions.imagejalgorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQDefaultDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

/**
 * Discriminator that describes an image index position
 */
@ACAQDocumentation(name = "Image index", description = "References a slice or position in a multi-dimensional image.")
public class ImageIndexDiscriminator extends ACAQDefaultDiscriminator {
    /**
     * Creates a new discriminator
     *
     * @param declaration The declaration
     * @param value       The value
     */
    public ImageIndexDiscriminator(ACAQTraitDeclaration declaration, String value) {
        super(declaration, value);
    }
}
