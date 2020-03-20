package org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Uniformly labeled objects", description = "Objects are labeled as whole")
public class UniformlyLabeledBioObjects extends BioObjectsLabeling {
    public UniformlyLabeledBioObjects(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
