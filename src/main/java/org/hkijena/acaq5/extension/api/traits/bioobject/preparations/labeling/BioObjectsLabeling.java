package org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.BioObjectsPreparations;

@ACAQDocumentation(name = "Object labeling")
@ACAQHidden
public class BioObjectsLabeling extends BioObjectsPreparations {
    public BioObjectsLabeling(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
