package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.api.ACAQTrait;

import java.util.HashSet;
import java.util.Set;

public class ACAQTraitRegistry {
    private Set<Class<? extends ACAQTrait>> registeredTraits = new HashSet<>();

    public ACAQTraitRegistry() {

    }

    public void register(Class<? extends ACAQTrait> klass) {
        registeredTraits.add(klass);
    }
}
