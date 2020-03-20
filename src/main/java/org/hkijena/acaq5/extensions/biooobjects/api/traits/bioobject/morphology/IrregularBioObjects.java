package org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.morphology;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Irregular objects", description = "Objects without a defined form")
public class IrregularBioObjects extends BioObjectsMorphology {
    public IrregularBioObjects(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
