package org.hkijena.acaq5.api.traits;

/**
 * Default implementation of {@link ACAQTrait}
 */
public class ACAQDefaultTrait implements ACAQTrait {

    private ACAQTraitDeclaration declaration;

    /**
     * @param declaration The trait declaration
     */
    public ACAQDefaultTrait(ACAQTraitDeclaration declaration) {
        this.declaration = declaration;
    }

    @Override
    public ACAQTraitDeclaration getDeclaration() {
        return declaration;
    }

    @Override
    public ACAQTrait duplicate() {
        return declaration.newInstance();
    }

    @Override
    public String toString() {
        return "" + getDeclaration();
    }
}
