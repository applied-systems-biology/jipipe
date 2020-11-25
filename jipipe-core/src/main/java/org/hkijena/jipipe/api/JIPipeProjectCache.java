/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.events.GraphChangedEvent;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.extensions.settings.VirtualDataSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A structure that manages cached data
 */
public class JIPipeProjectCache {
    private final EventBus eventBus = new EventBus();
    private final JIPipeProject project;
    private final Map<JIPipeGraphNode, Map<State, Map<String, JIPipeDataSlot>>> cacheEntries = new HashMap<>();
    private final Map<JIPipeDataInfo, Integer> cachedDataTypes = new HashMap<>();
    private int cachedRowNumber = 0;

    /**
     * Creates a new instance
     *
     * @param project the project the cache is associated to
     */
    public JIPipeProjectCache(JIPipeProject project) {
        this.project = project;
        project.getGraph().getEventBus().register(this);
    }

    public JIPipeProject getProject() {
        return project;
    }

    /**
     * Stores data into the cache
     *  @param source  the generating algorithm
     * @param stateId the state id
     * @param slot    the slot that contains the data
     * @param progressInfo data storage progress
     */
    public void store(JIPipeGraphNode source, State stateId, JIPipeDataSlot slot, JIPipeProgressInfo progressInfo) {
        if (!RuntimeSettings.getInstance().isAllowCache())
            return;
        if (!project.getGraph().containsNode(source))
            throw new IllegalArgumentException("The cache only can hold project graph nodes!");
        Map<State, Map<String, JIPipeDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if (stateMap == null) {
            stateMap = new HashMap<>();
            cacheEntries.put(source, stateMap);
        }

        Map<String, JIPipeDataSlot> slotMap = stateMap.getOrDefault(stateId, null);
        if (slotMap == null) {
            slotMap = new HashMap<>();
        }
        stateMap.put(stateId, slotMap); // Update generation time

        JIPipeDataSlot existingSlot = slotMap.getOrDefault(slot.getName(), null);
        if (existingSlot != null) {
            removeFromStatistics(existingSlot);
            existingSlot.clearData();
        }

        JIPipeDataSlot slotCopy = new JIPipeDataSlot(slot.getInfo(), source);
        slotCopy.getInfo().setVirtual(VirtualDataSettings.getInstance().isVirtualCache());
        slotCopy.addData(slot);
        slotCopy.applyVirtualState(progressInfo);
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
    public Map<State, Map<String, JIPipeDataSlot>> extract(JIPipeGraphNode source) {
        return cacheEntries.getOrDefault(source, null);
    }

    /**
     * Tries to extract data from the cache. Returns null if no data is available.
     *
     * @param source  the generating algorithm
     * @param stateId the state id
     * @return all cached slots. Please do not work directly on those. Use targetSlot.copyFrom() instead. Never null.
     */
    public Map<String, JIPipeDataSlot> extract(JIPipeGraphNode source, State stateId) {
        Map<State, Map<String, JIPipeDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
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
    public JIPipeDataSlot extract(JIPipeGraphNode source, String stateId, String slotName) {
        Map<State, Map<String, JIPipeDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if (stateMap != null) {
            Map<String, JIPipeDataSlot> slotMap = stateMap.getOrDefault(stateId, null);
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
    public void clear(JIPipeGraphNode source) {
        Map<State, Map<String, JIPipeDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if (stateMap != null) {
            for (Map.Entry<State, Map<String, JIPipeDataSlot>> stateEntry : stateMap.entrySet()) {
                for (Map.Entry<String, JIPipeDataSlot> slotEntry : stateEntry.getValue().entrySet()) {
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
    public void clear(JIPipeGraphNode source, State stateId) {
        Map<State, Map<String, JIPipeDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if (stateMap != null) {
            Map<String, JIPipeDataSlot> slotMap = stateMap.getOrDefault(stateId, null);
            if (slotMap != null) {
                for (Map.Entry<String, JIPipeDataSlot> slotEntry : slotMap.entrySet()) {
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
        for (Map.Entry<JIPipeGraphNode, Map<State, Map<String, JIPipeDataSlot>>> algorithmEntry : cacheEntries.entrySet()) {
            for (Map.Entry<State, Map<String, JIPipeDataSlot>> stateEntry : algorithmEntry.getValue().entrySet()) {
                for (Map.Entry<String, JIPipeDataSlot> slotEntry : stateEntry.getValue().entrySet()) {
                    slotEntry.getValue().clearData();
                }
            }
        }
        cacheEntries.clear();
        cachedRowNumber = 0;
        cachedDataTypes.clear();
        eventBus.post(new ModifiedEvent(this));
    }

    private void addToStatistics(JIPipeDataSlot slot) {
        cachedRowNumber += slot.getRowCount();
        JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(slot.getAcceptedDataType());
        int perDataType = cachedDataTypes.getOrDefault(dataInfo, 0);
        perDataType += slot.getRowCount();
        cachedDataTypes.put(dataInfo, perDataType);
    }

    private void removeFromStatistics(JIPipeDataSlot slot) {
        cachedRowNumber -= slot.getRowCount();
        JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(slot.getAcceptedDataType());
        int perDataType = cachedDataTypes.getOrDefault(dataInfo, 0);
        perDataType -= slot.getRowCount();
        cachedDataTypes.put(dataInfo, perDataType);
    }

    /**
     * Safely removes cache entries that are not accessible anymore (e.g. an algorithm was removed from the graph; or states where the slots do not exist anymore)
     *
     * @param compareSlots         if true, states are removed if the output slots don't align with the current configuration anymore
     * @param compareProjectStates if true, states that are not within the project anymore are also removed
     */
    public void autoClean(boolean compareSlots, boolean compareProjectStates) {
        JIPipeProjectCacheQuery cacheQuery = new JIPipeProjectCacheQuery(project);
        List<JIPipeGraphNode> traversedAlgorithms = null;
        for (JIPipeGraphNode algorithm : ImmutableList.copyOf(cacheEntries.keySet())) {
            if (project.getGraph().containsNode(algorithm)) {
                if (compareSlots || compareProjectStates) {
                    if (traversedAlgorithms == null) {
                        traversedAlgorithms = project.getGraph().traverseAlgorithms();
                    }
                    State stateId = cacheQuery.getCachedId(algorithm);

                    Map<State, Map<String, JIPipeDataSlot>> stateMap = cacheEntries.getOrDefault(algorithm, null);
                    for (Map.Entry<State, Map<String, JIPipeDataSlot>> stateEntry : ImmutableList.copyOf(stateMap.entrySet())) {
                        if (compareProjectStates) {
                            if (!Objects.equals(stateEntry.getKey(), stateId)) {
                                clear(algorithm, stateEntry.getKey());
                            }
                        } else {
                            for (String slotName : stateEntry.getValue().keySet()) {
                                if (!algorithm.getOutputSlotMap().containsKey(slotName) || !algorithm.getOutputSlotMap().get(slotName).isOutput()) {
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
    public void onAlgorithmRemoved(GraphChangedEvent event) {
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
    public Map<JIPipeDataInfo, Integer> getCachedDataTypes() {
        return Collections.unmodifiableMap(cachedDataTypes);
    }

    public boolean isEmpty() {
        return cachedRowNumber <= 0;
    }

    /**
     * Event that is triggered when the cache was modified (something stored or removed)
     */
    public static class ModifiedEvent {
        private final JIPipeProjectCache cache;

        /**
         * Creates a new event
         *
         * @param cache source
         */
        public ModifiedEvent(JIPipeProjectCache cache) {
            this.cache = cache;
        }

        public JIPipeProjectCache getCache() {
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
            return StringUtils.formatDateTime(getGenerationTime());
        }
    }
}
