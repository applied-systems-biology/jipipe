package org.hkijena.acaq5.api.traits;

import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Helper to allow easy serialization of a collection of {@link ACAQTraitDeclaration} references
 */
public class ACAQTraitDeclarationRefCollection extends ArrayList<ACAQTraitDeclarationRef> {

    /**
     * Creates a new instance
     */
    public ACAQTraitDeclarationRefCollection() {
    }

    /**
     * @param c the collection
     */
    public ACAQTraitDeclarationRefCollection(Collection<? extends ACAQTraitDeclarationRef> c) {
        super(c);
    }
}
