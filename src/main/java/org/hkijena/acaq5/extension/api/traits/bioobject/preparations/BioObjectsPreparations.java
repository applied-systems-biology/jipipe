package org.hkijena.acaq5.extension.api.traits.bioobject.preparations;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.extension.api.traits.bioobject.BioObjects;

/**
 * Base interface for all sample preparations
 */
@ACAQHidden
@ACAQDocumentation(name = "Sample preparation")
public class BioObjectsPreparations extends BioObjects {
    public BioObjectsPreparations(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
