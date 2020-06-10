package org.hkijena.acaq5.extensions.imagejalgorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQDefaultTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

/**
 * Discriminator that describes an image title
 */
@ACAQDocumentation(name = "Image title", description = "Contains the title of an image. This can be useful when importing an image.")
public class ImageTitleTrait extends ACAQDefaultTrait {
    /**
     * Creates a new discriminator
     *
     * @param declaration The declaration
     * @param value       The value
     */
    public ImageTitleTrait(ACAQTraitDeclaration declaration, String value) {
        super(declaration, value);
    }
}
