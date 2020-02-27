package org.hkijena.acaq5.extension.api.traits.bioobject.morphology;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.traits.HiddenTrait;
import org.hkijena.acaq5.extension.api.traits.bioobject.BioObjects;

@HiddenTrait
@ACAQDocumentation(name = "Object morphology")
public class BioObjectsMorphology extends BioObjects {
    public BioObjectsMorphology(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
