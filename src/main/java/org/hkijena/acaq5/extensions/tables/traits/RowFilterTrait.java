package org.hkijena.acaq5.extensions.tables.traits;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQDefaultTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

/**
 * Contains information how rows are filtered
 */
@ACAQDocumentation(name = "Row filter", description = "References a way how rows were filtered.")
public class RowFilterTrait extends ACAQDefaultTrait {
    /**
     * Creates a new discriminator
     *
     * @param declaration The declaration
     * @param value       The value
     */
    public RowFilterTrait(ACAQTraitDeclaration declaration, String value) {
        super(declaration, value);
    }
}
