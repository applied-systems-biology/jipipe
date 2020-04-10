package org.hkijena.acaq5.api.traits;

/**
 * Standard implementation of {@link ACAQDiscriminator}
 */
public class ACAQDefaultDiscriminator extends ACAQDefaultTrait implements ACAQDiscriminator {

    private String value;

    /**
     * Creates a new discriminator
     *
     * @param declaration The declaration
     * @param value       The value
     */
    public ACAQDefaultDiscriminator(ACAQTraitDeclaration declaration, String value) {
        super(declaration);
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Sets the value
     *
     * @param value The value
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return getDeclaration() + "=" + value;
    }
}
