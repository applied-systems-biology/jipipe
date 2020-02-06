package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.api.ACAQAlgorithm;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ACAQAlgorithmRegistry {
    private Set<Class<? extends ACAQAlgorithm>> registeredAlgorithms = new HashSet<>();

    public ACAQAlgorithmRegistry() {

    }

    public void register(Class<? extends ACAQAlgorithm> klass) {
        registeredAlgorithms.add(klass);
    }

    public Set<Class<? extends ACAQAlgorithm>> getRegisteredAlgorithms() {
        return Collections.unmodifiableSet(registeredAlgorithms);
    }
}
