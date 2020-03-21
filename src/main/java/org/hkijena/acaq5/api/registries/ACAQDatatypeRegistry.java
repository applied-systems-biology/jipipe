package org.hkijena.acaq5.api.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.DatatypeRegisteredEvent;

import java.util.*;

/**
 * Contains known {@link ACAQData} types, and associates them to their respective {@link ACAQDataSlot}.
 */
public class ACAQDatatypeRegistry {
    private BiMap<String, Class<? extends ACAQData>> registeredDataTypes = HashBiMap.create();
    private Set<String> hiddenDataTypeIds = new HashSet<>();
    private EventBus eventBus = new EventBus();

    public ACAQDatatypeRegistry() {

    }

    public void register(String id, Class<? extends ACAQData> klass) {
        registeredDataTypes.put(id, klass);
        if (klass.getAnnotationsByType(ACAQHidden.class).length > 0)
            hiddenDataTypeIds.add(id);
        eventBus.post(new DatatypeRegisteredEvent(id));
    }

    public Map<String, Class<? extends ACAQData>> getRegisteredDataTypes() {
        return Collections.unmodifiableMap(registeredDataTypes);
    }

    public Map<String, Class<? extends ACAQData>> getUnhiddenRegisteredDataTypes() {
        Map<String, Class<? extends ACAQData>> result = new HashMap<>();
        for (Map.Entry<String, Class<? extends ACAQData>> entry : registeredDataTypes.entrySet()) {
            if (!hiddenDataTypeIds.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public String getIdOf(Class<? extends ACAQData> dataClass) {
        return registeredDataTypes.inverse().get(dataClass);
    }

    public boolean hasDatatypeWithId(String id) {
        return registeredDataTypes.containsKey(id);
    }

    public boolean isHidden(String id) {
        return hiddenDataTypeIds.contains(id);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public Class<? extends ACAQData> getById(String id) {
        return registeredDataTypes.get(id);
    }

    public Map<String, Set<Class<? extends ACAQData>>> getDataTypesByMenuPaths() {
        return ACAQData.groupByMenuPath(registeredDataTypes.values());
    }

    public static ACAQDatatypeRegistry getInstance() {
        return ACAQRegistryService.getInstance().getDatatypeRegistry();
    }
}
