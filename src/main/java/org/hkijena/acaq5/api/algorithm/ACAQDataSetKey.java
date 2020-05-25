package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Structure that can hold multiple {@link org.hkijena.acaq5.api.traits.ACAQTrait} instances to make a set of traits a key
 */
public class ACAQDataSetKey {
    private Map<ACAQTraitDeclaration, ACAQTrait> entries = new HashMap<>();

    public Map<ACAQTraitDeclaration, ACAQTrait> getEntries() {
        return entries;
    }

    public void setEntries(Map<ACAQTraitDeclaration, ACAQTrait> entries) {
        this.entries = entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ACAQDataSetKey that = (ACAQDataSetKey) o;
        return entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }

    @Override
    public String toString() {
        return entries.values().stream().map(acaqTrait -> "" + acaqTrait).collect(Collectors.joining(", "));
    }
}
