package org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.count;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.BioObjects;

@ACAQHidden
@ACAQDocumentation(name = "Object count")
public class BioObjectsCount extends BioObjects {
    public BioObjectsCount(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
