package org.hkijena.acaq5.extensions.tables.traits;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQDefaultDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Row filter", description = "References a way how rows were filtered.")
public class RowFilterDiscriminator extends ACAQDefaultDiscriminator {
    /**
     * Creates a new discriminator
     *
     * @param declaration The declaration
     * @param value       The value
     */
    public RowFilterDiscriminator(ACAQTraitDeclaration declaration, String value) {
        super(declaration, value);
    }
}
