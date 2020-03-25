package org.hkijena.acaq5.api.traits;

public class ACAQDefaultDiscriminator extends ACAQDefaultTrait implements ACAQDiscriminator {

    private String value;

    public ACAQDefaultDiscriminator(ACAQTraitDeclaration declaration) {
        super(declaration);
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
