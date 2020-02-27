package org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Unlabeled objects", description = "Objects are unlabeled")
public class UnlabeledBioObjects extends BioObjectsLabeling {
    public UnlabeledBioObjects(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
