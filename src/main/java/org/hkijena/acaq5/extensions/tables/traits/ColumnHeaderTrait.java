package org.hkijena.acaq5.extensions.tables.traits;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQDefaultTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

/**
 * Trait that contains column headers
 */
@ACAQDocumentation(name = "Column header", description = "References a column header.")
public class ColumnHeaderTrait extends ACAQDefaultTrait {
    /**
     * Creates a new discriminator
     *
     * @param declaration The declaration
     * @param value       The value
     */
    public ColumnHeaderTrait(ACAQTraitDeclaration declaration, String value) {
        super(declaration, value);
    }
}
