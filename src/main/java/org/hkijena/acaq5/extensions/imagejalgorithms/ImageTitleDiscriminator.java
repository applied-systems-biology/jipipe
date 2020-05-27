package org.hkijena.acaq5.extensions.imagejalgorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQDefaultDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

/**
 * Discriminator that describes an image title
 */
@ACAQDocumentation(name = "Image title", description = "Contains the title of an image. This can be useful when importing an image.")
public class ImageTitleDiscriminator extends ACAQDefaultDiscriminator {
    /**
     * Creates a new discriminator
     *
     * @param declaration The declaration
     * @param value       The value
     */
    public ImageTitleDiscriminator(ACAQTraitDeclaration declaration, String value) {
        super(declaration, value);
    }
}
