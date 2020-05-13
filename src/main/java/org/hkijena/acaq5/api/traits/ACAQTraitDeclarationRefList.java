package org.hkijena.acaq5.api.traits;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.Collection;

/**
 * Helper to allow easy serialization of a collection of {@link ACAQTraitDeclaration} references
 */
public class ACAQTraitDeclarationRefList extends ListParameter<ACAQTraitDeclarationRef> {

    /**
     * Creates a new instance
     */
    public ACAQTraitDeclarationRefList() {
        super(ACAQTraitDeclarationRef.class);
    }

    /**
     * @param c the collection
     */
    public ACAQTraitDeclarationRefList(Collection<? extends ACAQTraitDeclarationRef> c) {
        super(ACAQTraitDeclarationRef.class);
        for (ACAQTraitDeclarationRef declarationRef : c) {
            add(new ACAQTraitDeclarationRef(declarationRef));
        }
    }
}
