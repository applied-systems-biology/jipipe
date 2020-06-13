package org.hkijena.acaq5.api;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.extensions.settings.RuntimeSettings;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * A structure that manages cached data
 */
public class ACAQProjectCache {
    private final EventBus eventBus = new EventBus();
    private final ACAQProject project;
    private final Map<ACAQAlgorithm, Map<State, Map<String, ACAQDataSlot>>> cacheEntries = new HashMap<>();
    private final Map<ACAQDataDeclaration, Integer> cachedDataTypes = new HashMap<>();
    private int cachedRowNumber = 0;

    /**
     * Creates a new instance
     *
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
     *
     * @param source  the generating algorithm
     * @param stateId the state id
     * @param slot    the slot that contains the data
     */
    public void store(ACAQAlgorithm source, State stateId, ACAQDataSlot slot) {
        if (!RuntimeSettings.getInstance().isAllowCache())
            return;
        if (!project.getGraph().containsNode(source))
            throw new IllegalArgumentException("The cache only can hold project graph nodes!");
        Map<State, Map<String, ACAQDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if (stateMap == null) {
            stateMap = new HashMap<>();
            cacheEntries.put(source, stateMap);
        }

        Map<String, ACAQDataSlot> slotMap = stateMap.getOrDefault(stateId, null);
        if (slotMap == null) {
            slotMap = new HashMap<>();
        }
        stateMap.put(stateId, slotMap); // Update generation time

        ACAQDataSlot existingSlot = slotMap.getOrDefault(slot.getName(), null);
        if (existingSlot != null) {
            removeFromStatistics(existingSlot);
            existingSlot.clearData();
        }

        ACAQDataSlot slotCopy = new ACAQDataSlot(slot.getDefinition(), source);
        slotCopy.copyFrom(slot);
        slotMap.put(slot.getName(), slotCopy);
        addToStatistics(slotCopy);

        eventBus.post(new ModifiedEvent(this));
    }

    /**
     * Tries to extract data from the cache
     *
     * @param source the generating algorithm
     * @return map from state ID to map of slot name to slot. Null if not found
     */
    public Map<State, Map<String, ACAQDataSlot>> extract(ACAQAlgorithm source) {
        return cacheEntries.getOrDefault(source, null);
    }

    /**
     * Tries to extract data from the cache. Returns null if no data is available.
     *
     * @param source  the generating algorithm
     * @param stateId the state id
     * @return all cached slots. Please do not work directly on those. Use targetSlot.copyFrom() instead. Never null.
     */
    public Map<String, ACAQDataSlot> extract(ACAQAlgorithm source, State stateId) {
        Map<State, Map<String, ACAQDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if (stateMap != null) {
            return stateMap.getOrDefault(stateId, Collections.emptyMap());
        }
        return Collections.emptyMap();
    }

    /**
     * Tries to extract data from the cache. Returns null if no data is available.
     *
     * @param source   the generating algorithm
     * @param stateId  the state id
     * @param slotName the slot that contains the data
     * @return the cache's slot. Please do not work directly on this slot. Use targetSlot.copyFrom() instead. Null if not found
     */
    public ACAQDataSlot extract(ACAQAlgorithm source, String stateId, String slotName) {
        Map<State, Map<String, ACAQDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if (stateMap != null) {
            Map<String, ACAQDataSlot> slotMap = stateMap.getOrDefault(stateId, null);
            if (slotMap != null) {
                return slotMap.getOrDefault(slotName, null);
            }
        }
        return null;
    }

    /**
     * Removes an algorithm from the cache
     *
     * @param source the algorithm
     */
    public void clear(ACAQAlgorithm source) {
        Map<State, Map<String, ACAQDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if (stateMap != null) {
            for (Map.Entry<State, Map<String, ACAQDataSlot>> stateEntry : stateMap.entrySet()) {
                for (Map.Entry<String, ACAQDataSlot> slotEntry : stateEntry.getValue().entrySet()) {
                    removeFromStatistics(slotEntry.getValue());
                    slotEntry.getValue().clearData();
                }
            }
            cacheEntries.remove(source);
            eventBus.post(new ModifiedEvent(this));
        }
    }

    /**
     * Removes an algorithm from the cache
     *
     * @param source  the algorithm
     * @param stateId state id
     */
    public void clear(ACAQAlgorithm source, State stateId) {
        Map<State, Map<String, ACAQDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if (stateMap != null) {
            Map<String, ACAQDataSlot> slotMap = stateMap.getOrDefault(stateId, null);
            if (slotMap != null) {
                for (Map.Entry<String, ACAQDataSlot> slotEntry : slotMap.entrySet()) {
                    removeFromStatistics(slotEntry.getValue());
                    slotEntry.getValue().clearData();
                }
            }
            stateMap.remove(stateId);
            eventBus.post(new ModifiedEvent(this));
        }
    }

    /**
     * Removes everything from the cache
     */
    public void clear() {
        for (Map.Entry<ACAQAlgorithm, Map<State, Map<String, ACAQDataSlot>>> algorithmEntry : cacheEntries.entrySet()) {
            for (Map.Entry<State, Map<String, ACAQDataSlot>> stateEntry : algorithmEntry.getValue().entrySet()) {
                for (Map.Entry<String, ACAQDataSlot> slotEntry : stateEntry.getValue().entrySet()) {
                    slotEntry.getValue().clearData();
                }
            }
        }
        cacheEntries.clear();
        cachedRowNumber = 0;
        cachedDataTypes.clear();
        eventBus.post(new ModifiedEvent(this));
    }

    private void addToStatistics(ACAQDataSlot slot) {
        cachedRowNumber += slot.getRowCount();
        ACAQDataDeclaration dataDeclaration = ACAQDataDeclaration.getInstance(slot.getAcceptedDataType());
        int perDataType = cachedDataTypes.getOrDefault(dataDeclaration, 0);
        perDataType += slot.getRowCount();
        cachedDataTypes.put(dataDeclaration, perDataType);
    }

    private void removeFromStatistics(ACAQDataSlot slot) {
        cachedRowNumber -= slot.getRowCount();
        ACAQDataDeclaration dataDeclaration = ACAQDataDeclaration.getInstance(slot.getAcceptedDataType());
        int perDataType = cachedDataTypes.getOrDefault(dataDeclaration, 0);
        perDataType -= slot.getRowCount();
        cachedDataTypes.put(dataDeclaration, perDataType);
    }

    /**
     * Safely removes cache entries that are not accessible anymore (e.g. an algorithm was removed from the graph; or states where the slots do not exist anymore)
     *
     * @param compareSlots         if true, states are removed if the output slots don't align with the current configuration anymore
     * @param compareProjectStates if true, states that are not within the project anymore are also removed
     */
    public void autoClean(boolean compareSlots, boolean compareProjectStates) {
        List<ACAQGraphNode> traversedAlgorithms = null;
        for (ACAQAlgorithm algorithm : ImmutableList.copyOf(cacheEntries.keySet())) {
            if (project.getGraph().containsNode(algorithm)) {
                if (compareSlots || compareProjectStates) {
                    if (traversedAlgorithms == null) {
                        traversedAlgorithms = project.getGraph().traverseAlgorithms();
                    }
                    State stateId = project.getStateIdOf(algorithm, traversedAlgorithms);

                    Map<State, Map<String, ACAQDataSlot>> stateMap = cacheEntries.getOrDefault(algorithm, null);
                    for (Map.Entry<State, Map<String, ACAQDataSlot>> stateEntry : ImmutableList.copyOf(stateMap.entrySet())) {
                        if (compareProjectStates) {
                            if (!Objects.equals(stateEntry.getKey(), stateId)) {
                                clear(algorithm, stateEntry.getKey());
                            }
                        } else {
                            for (String slotName : stateEntry.getValue().keySet()) {
                                if (!algorithm.getSlots().containsKey(slotName) || !algorithm.getSlots().get(slotName).isOutput()) {
                                    clear(algorithm, stateEntry.getKey());
                                    break;
                                }
                            }
                        }
                    }
                }
            } else {
                clear(algorithm);
            }
        }
    }

    /**
     * Listens to changes in the algorithm graph.
     * Triggers autoClean with only removing invalid algorithms
     *
     * @param event the event
     */
    public void onAlgorithmRemoved(AlgorithmGraphChangedEvent event) {
        autoClean(false, false);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns the number of cached data rows
     *
     * @return the number of cached data rows
     */
    public int getCachedRowNumber() {
        return cachedRowNumber;
    }

    /**
     * Returns statistics on how many rows of given data type have been cached
     *
     * @return statistics on how many rows of given data type have been cached
     */
    public Map<ACAQDataDeclaration, Integer> getCachedDataTypes() {
        return Collections.unmodifiableMap(cachedDataTypes);
    }

    public boolean isEmpty() {
        return cachedRowNumber <= 0;
    }

    /**
     * Event that is triggered when the cache was modified (something stored or removed)
     */
    public static class ModifiedEvent {
        private final ACAQProjectCache cache;

        /**
         * Creates a new event
         *
         * @param cache source
         */
        public ModifiedEvent(ACAQProjectCache cache) {
            this.cache = cache;
        }

        public ACAQProjectCache getCache() {
            return cache;
        }
    }

    /**
     * Encapsulates a state
     */
    public static class State implements Comparable<State> {
        private final LocalDateTime generationTime;
        private final String stateId;

        /**
         * Creates a new instance
         *
         * @param generationTime the generation time. It is ignored during comparison.
         * @param stateId        the state ID that uniquely identifies the state
         */
        public State(LocalDateTime generationTime, String stateId) {
            this.generationTime = generationTime;
            this.stateId = stateId;
        }

        public LocalDateTime getGenerationTime() {
            return generationTime;
        }

        public String getStateId() {
            return stateId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return stateId.equals(state.stateId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stateId);
        }

        @Override
        public int compareTo(State o) {
            return generationTime.compareTo(o.generationTime);
        }

        @Override
        public String toString() {
            return stateId;
        }

        /**
         * Formats the generation time
         *
         * @return formatted string
         */
        public String renderGenerationTime() {
            return getGenerationTime().format(DateTimeFormatter.ISO_LOCAL_DATE) + " " +
                    getGenerationTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }
}
