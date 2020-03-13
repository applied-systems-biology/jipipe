package org.hkijena.acaq5.api.traits;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Helper to allow easy serialization of a collection of {@link ACAQTraitDeclaration} references
 */
public class ACAQTraitDeclarationRefCollection extends ArrayList<ACAQTraitDeclarationRef> {

    public ACAQTraitDeclarationRefCollection() {
    }

    public ACAQTraitDeclarationRefCollection(Collection<? extends ACAQTraitDeclarationRef> c) {
        super(c);
    }
}
