package org.hkijena.acaq5.extension.api.traits.bioobject;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.traits.HiddenTrait;

@ACAQDocumentation(name = "Biological object")
@HiddenTrait
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
