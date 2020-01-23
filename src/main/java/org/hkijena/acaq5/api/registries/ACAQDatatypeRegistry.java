package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.api.ACAQData;

import java.util.*;

public class ACAQDatatypeRegistry {
    private Set<Class<? extends ACAQData>> registeredDataTypes = new HashSet<>();

    public ACAQDatatypeRegistry() {

    }

    public void register(Class<? extends ACAQData> klass) {
        registeredDataTypes.add(klass);
    }

    public Set<Class<? extends ACAQData>> getRegisteredDataTypes() {
        return Collections.unmodifiableSet(registeredDataTypes);
    }
}
