package org.hkijena.acaq5.api.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains known {@link ACAQData} types, and associates them to their respective {@link ACAQDataSlot}.
 */
public class ACAQDatatypeRegistry {
    private Set<Class<? extends ACAQData>> registeredDataTypes = new HashSet<>();

    public ACAQDatatypeRegistry() {

    }

    public static ACAQDatatypeRegistry getInstance() {
        return ACAQRegistryService.getInstance().getDatatypeRegistry();
    }

    public void register(Class<? extends ACAQData> klass) {
        registeredDataTypes.add(klass);
    }

    public Set<Class<? extends ACAQData>> getRegisteredDataTypes() {
        return Collections.unmodifiableSet(registeredDataTypes);
    }

    public Class<? extends ACAQData> findDataClass(String canonicalName) {
        return registeredDataTypes.stream().filter(c -> c.getCanonicalName().equals(canonicalName)).findFirst().get();
    }
}
