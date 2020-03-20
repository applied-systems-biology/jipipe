package org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Biological object")
@ACAQHidden
public class BioObjects implements ACAQTrait {
    private ACAQTraitDeclaration declaration;

    public BioObjects(ACAQTraitDeclaration declaration) {
        this.declaration = declaration;
    }

    @Override
    public ACAQTraitDeclaration getDeclaration() {
        return declaration;
    }
}
