package org.hkijena.acaq5.api.traits;

import java.util.Objects;

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

    @Override
    public int compareTo(ACAQDiscriminator o) {
        return Objects.compare(getValue(), o.getValue(), String::compareTo);
    }
}
