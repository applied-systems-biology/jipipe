package org.hkijena.acaq5.api.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQDependency;
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
    private Map<String, ACAQDependency> registeredDatatypeSources = new HashMap<>();
    private EventBus eventBus = new EventBus();

    public ACAQDatatypeRegistry() {

    }

    /**
     * Registers a data type
     *
     * @param id
     * @param klass
     * @param source
     */
    public void register(String id, Class<? extends ACAQData> klass, ACAQDependency source) {
        registeredDataTypes.put(id, klass);
        registeredDatatypeSources.put(id, source);
        if (klass.getAnnotationsByType(ACAQHidden.class).length > 0)
            hiddenDataTypeIds.add(id);
        eventBus.post(new DatatypeRegisteredEvent(id));
    }

    /**
     * Gets all registered data types
     *
     * @return
     */
    public Map<String, Class<? extends ACAQData>> getRegisteredDataTypes() {
        return Collections.unmodifiableMap(registeredDataTypes);
    }

    /**
     * Gets all data types that are not hidden
     *
     * @return
     */
    public Map<String, Class<? extends ACAQData>> getUnhiddenRegisteredDataTypes() {
        Map<String, Class<? extends ACAQData>> result = new HashMap<>();
        for (Map.Entry<String, Class<? extends ACAQData>> entry : registeredDataTypes.entrySet()) {
            if (!hiddenDataTypeIds.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Gets the ID of the data type
     *
     * @param dataClass
     * @return
     */
    public String getIdOf(Class<? extends ACAQData> dataClass) {
        return registeredDataTypes.inverse().get(dataClass);
    }

    /**
     * Returns true if there is a data type with given ID
     *
     * @param id
     * @return
     */
    public boolean hasDatatypeWithId(String id) {
        return registeredDataTypes.containsKey(id);
    }

    /**
     * Returns true if the data type with given ID is hidden
     *
     * @param id
     * @return
     */
    public boolean isHidden(String id) {
        return hiddenDataTypeIds.contains(id);
    }

    /**
     * Gets the event bus that post registration events
     *
     * @return
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns the data class with specified ID
     *
     * @param id
     * @return
     */
    public Class<? extends ACAQData> getById(String id) {
        return registeredDataTypes.get(id);
    }

    /**
     * Gets the registered data types, grouped by their menu paths
     *
     * @return
     */
    public Map<String, Set<Class<? extends ACAQData>>> getDataTypesByMenuPaths() {
        return ACAQData.groupByMenuPath(registeredDataTypes.values());
    }

    /**
     * Returns the source that registered that data type
     *
     * @param id
     * @return
     */
    public ACAQDependency getSourceOf(String id) {
        return registeredDatatypeSources.getOrDefault(id, null);
    }

    /**
     * Returns the source that registered that data type
     *
     * @param dataClass
     * @return
     */
    public ACAQDependency getSourceOf(Class<? extends ACAQData> dataClass) {
        return getSourceOf(getIdOf(dataClass));
    }

    /**
     * Returns true if the specified data type class is registered
     *
     * @param dataClass
     * @return
     */
    public boolean hasDataType(Class<? extends ACAQData> dataClass) {
        return registeredDataTypes.containsValue(dataClass);
    }

    public static ACAQDatatypeRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getDatatypeRegistry();
    }
}
