package org.hkijena.acaq5.api.traits;

import java.util.Objects;

/**
 * Standard implementation of {@link ACAQTrait}
 */
public class ACAQDefaultTrait implements ACAQTrait {

    private final ACAQTraitDeclaration declaration;
    private String value = "";

    /**
     * Creates a new instance
     *
     * @param declaration The declaration
     * @param value       The value
     */
    public ACAQDefaultTrait(ACAQTraitDeclaration declaration, String value) {
        this.declaration = declaration;
        this.value = value;
    }

    /**
     * Creates a new instance with an empty string value
     * @param declaration the declaration
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
        return getDeclaration().newInstance(value);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ACAQTrait))
            return false;
        if (getDeclaration() != ((ACAQTrait) o).getDeclaration())
            return false;
        ACAQTrait that = (ACAQTrait) o;
        return Objects.equals(value, that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public int compareTo(ACAQTrait o) {
        return Objects.compare(getValue(), o.getValue(), String::compareTo);
    }
}
