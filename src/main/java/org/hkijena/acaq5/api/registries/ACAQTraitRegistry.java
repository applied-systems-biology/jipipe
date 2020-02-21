package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.api.traits.ACAQTrait;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ACAQTraitRegistry {
    private Set<Class<? extends ACAQTrait>> registeredTraits = new HashSet<>();

    public ACAQTraitRegistry() {

    }

    public void register(Class<? extends ACAQTrait> klass) {
        if(!ACAQTrait.isHidden(klass))
            registeredTraits.add(klass);
    }

    public Set<Class<? extends ACAQTrait>> getTraits() {
        return Collections.unmodifiableSet(registeredTraits);
    }

    public Class<? extends ACAQTrait> findTraitClass(String canonicalName) {
        return registeredTraits.stream().filter(c -> c.getCanonicalName().equals(canonicalName)).findFirst().get();
    }
}
