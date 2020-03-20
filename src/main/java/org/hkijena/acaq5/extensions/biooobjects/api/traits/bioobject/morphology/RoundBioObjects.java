package org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.morphology;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Round objects", description = "Circular objects")
public class RoundBioObjects extends FilamentousBioObjects {
    public RoundBioObjects(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
