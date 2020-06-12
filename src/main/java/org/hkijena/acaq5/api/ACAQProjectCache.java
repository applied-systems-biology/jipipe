package org.hkijena.acaq5.api;

import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * A structure that manages cached data
 */
public class ACAQProjectCache {
    private final ACAQProject project;
    private final Map<ACAQAlgorithm, Map<String, Map<String, ACAQDataSlot>>> cacheEntries = new HashMap<>();

    /**
     * Creates a new instance
     * @param project the project the cache is associated to
     */
    public ACAQProjectCache(ACAQProject project) {
        this.project = project;
        project.getGraph().getEventBus().register(this);
    }

    public ACAQProject getProject() {
        return project;
    }

    /**
     * Stores data into the cache
     * @param source the generating algorithm
     * @param stateId the state id
     * @param slotName the slot that contains the data
     */
    public void store(ACAQAlgorithm source, String stateId, String slotName) {
        if(!project.getGraph().containsNode(source))
            throw new IllegalArgumentException("The cache only can hold project graph nodes!");
        ACAQDataSlot slot = source.getOutputSlot(slotName);
        Map<String, Map<String, ACAQDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if(stateMap == null) {
            stateMap = new HashMap<>();
            cacheEntries.put(source, stateMap);
        }
        Map<String, ACAQDataSlot> slotMap = stateMap.getOrDefault(stateId, null);
        if(slotMap == null) {
            slotMap = new HashMap<>();
            stateMap.put(stateId, slotMap);
        }

        ACAQDataSlot existingSlot = slotMap.getOrDefault(slotName, null);
        existingSlot.clearData();

        ACAQDataSlot slotCopy = new ACAQDataSlot(slot.getDefinition(), source);
        slotCopy.copyFrom(slot);
        slotMap.put(slotName, slotCopy);
    }

    /**
     * Tries to extract data from the cache. Returns null if no data is available.
     * @param source the generating algorithm
     * @param stateId the state id
     * @param slotName the slot that contains the data
     * @return the cache's slot. Please do not work directly on this slot. Use targetSlot.copyFrom() instead
     */
    public ACAQDataSlot extract(ACAQAlgorithm source, String stateId, String slotName) {
        Map<String, Map<String, ACAQDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if(stateMap != null) {
            Map<String, ACAQDataSlot> slotMap = stateMap.getOrDefault(stateId, null);
            if(slotMap != null) {
                return slotMap.getOrDefault(slotName, null);
            }
        }
        return null;
    }

    /**
     * Removes an algorithm from the cache
     * @param source the algorithm
     */
    public void clear(ACAQAlgorithm source) {
        Map<String, Map<String, ACAQDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if(stateMap != null) {
            for (Map.Entry<String, Map<String, ACAQDataSlot>> stateEntry : stateMap.entrySet()) {
                for (Map.Entry<String, ACAQDataSlot> slotEntry : stateEntry.getValue().entrySet()) {
                    slotEntry.getValue().clearData();
                }
            }
            cacheEntries.remove(source);
        }
    }

    /**
     * Removes an algorithm from the cache
     * @param source the algorithm
     * @param stateId state id
     */
    public void clear(ACAQAlgorithm source, String stateId) {
        Map<String, Map<String, ACAQDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if(stateMap != null) {
            Map<String, ACAQDataSlot> slotMap = stateMap.getOrDefault(stateId, null);
            if(slotMap != null) {
                for (Map.Entry<String, ACAQDataSlot> slotEntry : slotMap.entrySet()) {
                    slotEntry.getValue().clearData();
                }
                stateMap.remove(stateId);
            }
        }
    }

    /**
     * Safely removes cache entries that are not accessible anymore (e.g. an algorithm was removed from the graph; or states where the slots do not exist anymore)
     */
    public void autoClean() {
        for (ACAQAlgorithm algorithm : ImmutableList.copyOf(cacheEntries.keySet())) {
            if(project.getGraph().containsNode(algorithm)) {
                Map<String, Map<String, ACAQDataSlot>> stateMap = cacheEntries.getOrDefault(algorithm, null);
                for (Map.Entry<String, Map<String, ACAQDataSlot>> stateEntry : ImmutableList.copyOf(stateMap.entrySet())) {
                    for (String slotName : stateEntry.getValue().keySet()) {
                        if(!algorithm.getSlots().containsKey(slotName) || !algorithm.getSlots().get(slotName).isOutput())   {
                            clear(algorithm, stateEntry.getKey());
                        }
                    }
                }
            }
            else {
                clear(algorithm);
            }
        }
    }

    /**
     * Listens to changes in the algorithm graph
     * @param event the event
     */
    public void onAlgorithmRemoved(AlgorithmGraphChangedEvent event) {
        autoClean();
    }
}
