package org.hkijena.acaq5.extensions.tables.traits;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQDefaultDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Column header", description = "References a column header.")
public class ColumnHeaderDiscriminator extends ACAQDefaultDiscriminator {
    /**
     * Creates a new discriminator
     *
     * @param declaration The declaration
     * @param value       The value
     */
    public ColumnHeaderDiscriminator(ACAQTraitDeclaration declaration, String value) {
        super(declaration, value);
    }
}
