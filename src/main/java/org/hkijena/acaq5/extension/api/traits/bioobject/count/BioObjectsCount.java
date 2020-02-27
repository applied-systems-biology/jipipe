package org.hkijena.acaq5.extension.api.traits.bioobject.count;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.traits.HiddenTrait;
import org.hkijena.acaq5.extension.api.traits.bioobject.BioObjects;

@HiddenTrait
@ACAQDocumentation(name = "Object count")
public class BioObjectsCount extends BioObjects {
    public BioObjectsCount(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
