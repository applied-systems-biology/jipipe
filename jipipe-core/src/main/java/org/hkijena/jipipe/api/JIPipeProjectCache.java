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
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.extensions.settings.VirtualDataSettings;

import java.util.*;

/**
 * A structure that manages cached data
 */
public class JIPipeProjectCache {
    private final EventBus eventBus = new EventBus();
    private final JIPipeProject project;
    private final Map<UUID, Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>>> cacheEntries = new HashMap<>();
    private final Map<JIPipeDataInfo, Integer> cachedDataTypes = new HashMap<>();
    private int cachedRowNumber = 0;
    private boolean disableTriggerEvent = false;

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
     *
     * @param source       the generating algorithm
     * @param stateId      the state id
     * @param slot         the slot that contains the data
     * @param progressInfo data storage progress
     */
    public void store(JIPipeGraphNode source, JIPipeProjectCacheState stateId, JIPipeDataSlot slot, JIPipeProgressInfo progressInfo) {
        if (!RuntimeSettings.getInstance().isAllowCache())
            return;
        if (!project.getGraph().containsNode(source))
            throw new IllegalArgumentException("The cache only can hold project graph nodes!");
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = cacheEntries.getOrDefault(source.getUUIDInParentGraph(), null);
        if (stateMap == null) {
            stateMap = new HashMap<>();
            cacheEntries.put(source.getUUIDInParentGraph(), stateMap);
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
        slotCopy.addData(slot, progressInfo);
        slotCopy.applyVirtualState(progressInfo);
        slotMap.put(slot.getName(), slotCopy);
        addToStatistics(slotCopy);

        if (!disableTriggerEvent)
            eventBus.post(new ModifiedEvent(this));
    }

    /**
     * Tries to extract data from the cache
     *
     * @param source the node UUID
     * @return map from state ID to map of slot name to slot. Null if not found
     */
    public Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> extract(UUID source) {
        return cacheEntries.getOrDefault(source, null);
    }

    /**
     * Tries to extract data from the cache. Returns null if no data is available.
     *
     * @param source  the node UUID
     * @param stateId the state id
     * @return all cached slots. Please do not work directly on those. Use targetSlot.copyFrom() instead. Never null.
     */
    public Map<String, JIPipeDataSlot> extract(UUID source, JIPipeProjectCacheState stateId) {
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if (stateMap != null) {
            return stateMap.getOrDefault(stateId, Collections.emptyMap());
        }
        return Collections.emptyMap();
    }

    /**
     * Tries to extract data from the cache. Returns null if no data is available.
     *
     * @param source   the node UUID
     * @param stateId  the state id
     * @param slotName the slot that contains the data
     * @return the cache's slot. Please do not work directly on this slot. Use targetSlot.copyFrom() instead. Null if not found
     */
    public JIPipeDataSlot extract(UUID source, JIPipeProjectCacheState stateId, String slotName) {
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
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
     * @param source the node UUID
     */
    public void clear(UUID source) {
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if (stateMap != null) {
            for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateEntry : stateMap.entrySet()) {
                for (Map.Entry<String, JIPipeDataSlot> slotEntry : stateEntry.getValue().entrySet()) {
                    removeFromStatistics(slotEntry.getValue());
                    slotEntry.getValue().clearData();
                }
            }
            cacheEntries.remove(source);
            if (!disableTriggerEvent)
                eventBus.post(new ModifiedEvent(this));
        }
    }

    /**
     * Removes an algorithm from the cache
     *
     * @param source  the node UUID
     * @param stateId state id
     */
    public void clear(UUID source, JIPipeProjectCacheState stateId) {
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = cacheEntries.getOrDefault(source, null);
        if (stateMap != null) {
            Map<String, JIPipeDataSlot> slotMap = stateMap.getOrDefault(stateId, null);
            if (slotMap != null) {
                for (Map.Entry<String, JIPipeDataSlot> slotEntry : slotMap.entrySet()) {
                    removeFromStatistics(slotEntry.getValue());
                    slotEntry.getValue().clearData();
                }
            }
            stateMap.remove(stateId);
            if (!disableTriggerEvent)
                eventBus.post(new ModifiedEvent(this));
        }
    }

    /**
     * Removes everything from the cache
     */
    public void clear() {
        for (Map.Entry<UUID, Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>>> algorithmEntry : cacheEntries.entrySet()) {
            for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateEntry : algorithmEntry.getValue().entrySet()) {
                for (Map.Entry<String, JIPipeDataSlot> slotEntry : stateEntry.getValue().entrySet()) {
                    slotEntry.getValue().clearData();
                }
            }
        }
        cacheEntries.clear();
        cachedRowNumber = 0;
        cachedDataTypes.clear();
        if (!disableTriggerEvent)
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
     * @param progressInfo         the progress info
     */
    public void autoClean(boolean compareSlots, boolean compareProjectStates, JIPipeProgressInfo progressInfo) {
        try {
            disableTriggerEvent = true;
            JIPipeProjectCacheQuery cacheQuery = new JIPipeProjectCacheQuery(project);
            List<JIPipeGraphNode> traversedAlgorithms = null;
            for (UUID nodeUUID : ImmutableList.copyOf(cacheEntries.keySet())) {
                if (project.getGraph().containsNode(nodeUUID)) {
                    JIPipeGraphNode node = project.getGraph().getNodeByUUID(nodeUUID);
                    if (compareSlots || compareProjectStates) {
                        if (traversedAlgorithms == null) {
                            traversedAlgorithms = project.getGraph().traverse();
                        }
                        JIPipeProjectCacheState stateId = cacheQuery.getCachedId(nodeUUID);

                        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = cacheEntries.getOrDefault(nodeUUID, null);
                        for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateEntry : ImmutableList.copyOf(stateMap.entrySet())) {
                            if (compareProjectStates) {
                                if (!Objects.equals(stateEntry.getKey(), stateId)) {
                                    progressInfo.log("Clearing " + node.getDisplayName() + " state " + stateEntry.getKey());
                                    clear(nodeUUID, stateEntry.getKey());
                                }
                            } else {
                                for (String slotName : stateEntry.getValue().keySet()) {
                                    if (!node.getOutputSlotMap().containsKey(slotName) || !node.getOutputSlotMap().get(slotName).isOutput()) {
                                        progressInfo.log("Clearing " + node.getDisplayName() + " state " + stateEntry.getKey());
                                        clear(nodeUUID, stateEntry.getKey());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    clear(nodeUUID);
                }
            }
        } finally {
            disableTriggerEvent = false;
        }
        eventBus.post(new ModifiedEvent(this));
    }

    /**
     * Listens to changes in the algorithm graph.
     * Triggers autoClean with only removing invalid algorithms
     *
     * @param event the event
     */
    public void onAlgorithmRemoved(JIPipeGraph.GraphChangedEvent event) {
        autoClean(false, false, new JIPipeProgressInfo());
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

    public int size() {
        return cachedRowNumber;
    }

    /**
     * Makes the whole cache virtual
     *
     * @param progress the progress
     */
    public void makeVirtual(JIPipeProgressInfo progress) {
        progress.addMaxProgress(cachedRowNumber);
        for (Map.Entry<UUID, Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>>> nodeMap : cacheEntries.entrySet()) {
            JIPipeGraphNode node = getProject().getGraph().getNodeByUUID(nodeMap.getKey());
            if(node == null)
                continue;
            JIPipeProgressInfo nodeProgress = progress.resolve(node.getName());
            for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMapEntry : nodeMap.getValue().entrySet()) {
                JIPipeProgressInfo stateProgress = nodeProgress.resolve(stateMapEntry.getKey().renderGenerationTime());
                for (Map.Entry<String, JIPipeDataSlot> dataSlotEntry : stateMapEntry.getValue().entrySet()) {
                    JIPipeProgressInfo slotProgress = stateProgress.resolve(dataSlotEntry.getKey());
                    slotProgress.addProgress(dataSlotEntry.getValue().getRowCount());
                    slotProgress.log("Moving " + dataSlotEntry.getValue().getRowCount() + " rows to HDD");
                    dataSlotEntry.getValue().getInfo().setVirtual(true);
                    dataSlotEntry.getValue().makeDataVirtual(slotProgress);
                }
            }
        }
    }

    /**
     * Makes the whole cache virtual
     *
     * @param progress the progress
     */
    public void makeNonVirtual(JIPipeProgressInfo progress) {
        progress.addMaxProgress(cachedRowNumber);
        for (Map.Entry<UUID, Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>>> nodeMap : cacheEntries.entrySet()) {
            JIPipeGraphNode node = getProject().getGraph().getNodeByUUID(nodeMap.getKey());
            if(node == null)
                continue;
            JIPipeProgressInfo nodeProgress = progress.resolve(node.getName());
            for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMapEntry : nodeMap.getValue().entrySet()) {
                JIPipeProgressInfo stateProgress = nodeProgress.resolve(stateMapEntry.getKey().renderGenerationTime());
                for (Map.Entry<String, JIPipeDataSlot> dataSlotEntry : stateMapEntry.getValue().entrySet()) {
                    JIPipeProgressInfo slotProgress = stateProgress.resolve(dataSlotEntry.getKey());
                    slotProgress.addProgress(dataSlotEntry.getValue().getRowCount());
                    slotProgress.log("Moving " + dataSlotEntry.getValue().getRowCount() + " rows from HDD");
                    dataSlotEntry.getValue().getInfo().setVirtual(false);
                    dataSlotEntry.getValue().makeDataNonVirtual(slotProgress, true);
                }
            }
        }
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

}
