package org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Membrane-labeled objects", description = "Object membranes are labeled")
public class MembraneLabeledBioObjects extends BioObjectsLabeling {
    public MembraneLabeledBioObjects(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
