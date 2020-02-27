package org.hkijena.acaq5.extension.api.traits.bioobject.count;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Single object", description = "There is only exact one object")
public class SingleBioObject extends BioObjectsCount {
    public SingleBioObject(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
