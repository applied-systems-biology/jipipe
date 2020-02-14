package org.hkijena.acaq5.api.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import org.hkijena.acaq5.api.ACAQData;
import org.hkijena.acaq5.api.ACAQDataSlot;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ACAQDatatypeRegistry {
    private Set<Class<? extends ACAQData>> registeredDataTypes = new HashSet<>();
    private BiMap<Class<? extends ACAQData>, Class<? extends ACAQDataSlot<?>>> registeredSlotDataTypes = HashBiMap.create();

    public ACAQDatatypeRegistry() {

    }

    public void register(Class<? extends ACAQData> klass, Class<? extends ACAQDataSlot<?>> slotClass) {
        registeredDataTypes.add(klass);
        registeredSlotDataTypes.put(klass, slotClass);
    }

    public Set<Class<? extends ACAQData>> getRegisteredDataTypes() {
        return Collections.unmodifiableSet(registeredDataTypes);
    }

    public  BiMap<Class<? extends ACAQData>, Class<? extends ACAQDataSlot<?>>> getRegisteredSlotDataTypes() {
        return ImmutableBiMap.copyOf(registeredSlotDataTypes);
    }

    public Class<? extends ACAQDataSlot<?>> findDataSlotClass(String canonicalName) {
        return registeredSlotDataTypes.values().stream().filter(c -> c.getCanonicalName().equals(canonicalName)).findFirst().get();
    }
}
