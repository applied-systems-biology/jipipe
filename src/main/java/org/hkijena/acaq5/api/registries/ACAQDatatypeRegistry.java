package org.hkijena.acaq5.api.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains known {@link ACAQData} types, and associates them to their respective {@link ACAQDataSlot}.
 */
public class ACAQDatatypeRegistry {
    private Set<Class<? extends ACAQData>> registeredDataTypes = new HashSet<>();
    private Set<Class<? extends ACAQData>> hiddenDataTypes = new HashSet<>();

    public ACAQDatatypeRegistry() {

    }

    public static ACAQDatatypeRegistry getInstance() {
        return ACAQRegistryService.getInstance().getDatatypeRegistry();
    }

    public void register(Class<? extends ACAQData> klass) {
        registeredDataTypes.add(klass);
        if(klass.getAnnotationsByType(ACAQHidden.class).length > 0)
            hiddenDataTypes.add(klass);
    }

    public Set<Class<? extends ACAQData>> getRegisteredDataTypes() {
        return Collections.unmodifiableSet(registeredDataTypes);
    }

    public Set<Class<? extends ACAQData>> getUnhiddenRegisteredDataTypes() {
        return registeredDataTypes.stream().filter(d -> !isHidden(d)).collect(Collectors.toSet());
    }

    public Class<? extends ACAQData> findDataClass(String canonicalName) {
        return registeredDataTypes.stream().filter(c -> c.getCanonicalName().equals(canonicalName)).findFirst().get();
    }

    public boolean isHidden(Class<? extends ACAQData> klass) {
        return hiddenDataTypes.contains(klass);
    }
}
