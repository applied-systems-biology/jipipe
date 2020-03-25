package org.hkijena.acaq5.api.traits;

public class ACAQDefaultTrait implements ACAQTrait {

    private ACAQTraitDeclaration declaration;

    public ACAQDefaultTrait(ACAQTraitDeclaration declaration) {
        this.declaration = declaration;
    }

    @Override
    public ACAQTraitDeclaration getDeclaration() {
        return declaration;
    }
}
