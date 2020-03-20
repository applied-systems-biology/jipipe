package org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.morphology;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Filamentous objects", description = "Filamentous objects, e.g. worms")
public class FilamentousBioObjects extends BioObjectsMorphology {
    public FilamentousBioObjects(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
