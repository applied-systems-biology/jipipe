package org.hkijena.jipipe.api.cache;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

public class JIPipeLocalProjectMemoryCache implements JIPipeCache {

    private final JIPipeProject project;
    private final EventBus eventBus = new EventBus();
    private final Map<UUID, Map<String, JIPipeDataTable>> cachedOutputSlots = new HashMap<>();
    private final Map<UUID, JIPipeGraphNode> currentNodeStates = new HashMap<>();
    private final Map<UUID, Set<UUID>> expectedNodePredecessors = new HashMap<>();
    private DefaultDirectedGraph<UUID, DefaultEdge> currentNodeStatePredecessorGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

    private int currentSize = 0;

    public JIPipeLocalProjectMemoryCache(JIPipeProject project) {
        this.project = project;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void store(JIPipeGraphNode graphNode, UUID nodeUUID, JIPipeDataTable data, String outputName, JIPipeProgressInfo progressInfo) {
        JIPipeGraphNode projectNode = project.getGraph().getNodeByUUID(nodeUUID);
        if(projectNode == null) {
            progressInfo.log("Refusing to cache node " + nodeUUID + " (" + graphNode.getDisplayName() + ") --> Not in project anymore!");
            return;
        }

        // Store the current state for comparison and update the predecessor graph
        JIPipeGraphNode graphNodeDuplicate = graphNode.duplicate();
        currentNodeStates.put(nodeUUID, graphNodeDuplicate);
        putNodeIntoGraph(projectNode, progressInfo);

        // Store the data
        Map<String, JIPipeDataTable> slotMap = cachedOutputSlots.getOrDefault(nodeUUID, null);
        if(slotMap == null) {
            slotMap = new HashMap<>();
            cachedOutputSlots.put(nodeUUID, slotMap);
        }
        JIPipeDataTable dataTableCopy = new JIPipeOutputDataSlot(new JIPipeDataSlotInfo(data.getAcceptedDataType(), JIPipeSlotType.Output, outputName, ""), projectNode);
        dataTableCopy.addDataFromTable(data, progressInfo);
        slotMap.put(outputName, dataTableCopy);
        progressInfo.log("Stored " + data.getRowCount() + " into " + nodeUUID + "/" + outputName);

        updateSize();
        getEventBus().post(new StoredEvent(this, nodeUUID, data, outputName));
        getEventBus().post(new ModifiedEvent(this));
    }

    private void putNodeIntoGraph(JIPipeGraphNode projectNode, JIPipeProgressInfo progressInfo) {
        UUID uuid = projectNode.getUUIDInParentGraph();
        currentNodeStatePredecessorGraph.addVertex(uuid);

        // Store all existing predecessors
        Set<UUID> existingPredecessors = new HashSet<>();
        Set<UUID> expectedPredecessors = new HashSet<>();
        for (DefaultEdge defaultEdge : currentNodeStatePredecessorGraph.incomingEdgesOf(uuid)) {
            UUID predecessorUUID = currentNodeStatePredecessorGraph.getEdgeSource(defaultEdge);
            existingPredecessors.add(predecessorUUID);
        }

        // Register predecessors
        List<JIPipeGraphNode> predecessorAlgorithms = project.getGraph().getPredecessorAlgorithms(projectNode, project.getGraph().traverse());
        for (JIPipeGraphNode predecessorAlgorithm : predecessorAlgorithms) {
            UUID predecessorUUID = predecessorAlgorithm.getUUIDInParentGraph();

            // Add if necessary
            if(!currentNodeStatePredecessorGraph.containsVertex(predecessorUUID)) {
                progressInfo.log("Register predecessor " + predecessorUUID + " of " + uuid);
                currentNodeStatePredecessorGraph.addVertex(predecessorUUID);
                putNodeIntoGraph(project.getGraph().getNodeByUUID(predecessorUUID), progressInfo);
            }

            currentNodeStates.put(predecessorUUID, predecessorAlgorithm.duplicate());
            expectedPredecessors.add(predecessorUUID);
            currentNodeStatePredecessorGraph.addEdge(predecessorUUID, uuid);
        }
        expectedNodePredecessors.put(uuid, expectedPredecessors); // needed for pruning

        // Remove old predecessors
        for (UUID existingPredecessor : existingPredecessors) {
            if(!expectedPredecessors.contains(existingPredecessor)) {
                currentNodeStatePredecessorGraph.removeEdge(existingPredecessor, uuid);
            }
        }

        progressInfo.log("Tracked " + expectedPredecessors.size() + " predecessors");
    }

    /**
     * Iterates through all cached node states and remove all that are not existing within the project graph or where the project graph was changed
     */
    private void removeInvalidNodeStates(JIPipeProgressInfo progressInfo) {
        for (UUID uuid : ImmutableList.copyOf(currentNodeStates.keySet())) {
            JIPipeGraphNode currentNode = project.getGraph().getNodeByUUID(uuid);
            JIPipeGraphNode cachedNode = currentNodeStates.get(uuid);

            // Check output slots
            Map<String, JIPipeDataTable> slotMap = cachedOutputSlots.getOrDefault(uuid, null);
            if(slotMap != null) {
                if(slotMap.isEmpty()) {
                    removeNodeCache(uuid);
                    progressInfo.log("Removed invalid node state for " + uuid + " [empty slot map]");
                    continue;
                }
            }

            if(currentNode == null || !currentNode.functionallyEquals(cachedNode)) {
                removeNodeCache(uuid);
                progressInfo.log("Removed invalid node state for " + uuid);
            }
        }
    }

    /**
     * Removes all vertices that have missing inputs
     */
    private void pruneGraph(JIPipeProgressInfo progressInfo) {
        boolean modified;
        boolean updated = false;
        Set<UUID> availablePredecessors = new HashSet<>();
        int iteration = 0;
        do {
            iteration++;
            JIPipeProgressInfo iterationProgress = progressInfo.resolveAndLog("Iteration " + iteration);
            modified = false;
            for (UUID uuid : ImmutableList.copyOf(currentNodeStates.keySet())) {
                availablePredecessors.clear();
                for (DefaultEdge defaultEdge : currentNodeStatePredecessorGraph.incomingEdgesOf(uuid)) {
                    availablePredecessors.add(currentNodeStatePredecessorGraph.getEdgeSource(defaultEdge));
                }
                Set<UUID> expectedPredecessors = expectedNodePredecessors.getOrDefault(uuid, null);
                if(!expectedPredecessors.equals(availablePredecessors)) {
                    removeNodeCache(uuid);

                    modified = true;
                    updated = true;

                    iterationProgress.log("Deleting cache of " + uuid + " --> predecessors are missing");
                }
            }
        }
        while (modified);

        if(updated) {
            updateSize();
            getEventBus().post(new ClearedEvent(this, null));
            getEventBus().post(new ModifiedEvent(this));
        }
    }

    private void removeNodeCache(UUID uuid) {
        cachedOutputSlots.remove(uuid);
        currentNodeStates.remove(uuid);
        expectedNodePredecessors.remove(uuid);
        currentNodeStatePredecessorGraph.removeVertex(uuid);
    }

    @Override
    public Map<String, JIPipeDataTable> query(JIPipeGraphNode graphNode, UUID nodeUUID, JIPipeProgressInfo progressInfo) {
        return cachedOutputSlots.getOrDefault(nodeUUID, Collections.emptyMap());
    }

    @Override
    public void clearOutdated(JIPipeProgressInfo progressInfo) {
       removeInvalidNodeStates(progressInfo);
       pruneGraph(progressInfo);
    }

    @Override
    public void clearAll(JIPipeProgressInfo progressInfo) {
        cachedOutputSlots.clear();
        currentNodeStates.clear();
        expectedNodePredecessors.clear();
        currentNodeStatePredecessorGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

        updateSize();
        getEventBus().post(new ClearedEvent(this, null));
        getEventBus().post(new ModifiedEvent(this));
    }

    @Override
    public void clearAll(UUID nodeUUID, JIPipeProgressInfo progressInfo) {
        removeNodeCache(nodeUUID);

        updateSize();
        getEventBus().post(new ClearedEvent(this, nodeUUID));
        getEventBus().post(new ModifiedEvent(this));
    }

    @Override
    public boolean isEmpty() {
        return currentSize <= 0;
    }

    @Override
    public int size() {
        return currentSize;
    }

    @Override
    public String toString() {
        return String.format("Local memory cache [%d items] { %d vertices, %d edges }", size(), currentNodeStatePredecessorGraph.vertexSet().size(), currentNodeStatePredecessorGraph.edgeSet().size());
    }

    public void makeNonVirtual(JIPipeProgressInfo progressInfo) {
        for (Map.Entry<UUID, Map<String, JIPipeDataTable>> nodeEntry : cachedOutputSlots.entrySet()) {
            for (Map.Entry<String, JIPipeDataTable> slotMap : nodeEntry.getValue().entrySet()) {
                slotMap.getValue().makeDataNonVirtual(progressInfo.resolveAndLog(nodeEntry.getKey() + "/" + slotMap.getKey()), true);
            }
        }
    }

    public void makeVirtual(JIPipeProgressInfo progressInfo) {
        for (Map.Entry<UUID, Map<String, JIPipeDataTable>> nodeEntry : cachedOutputSlots.entrySet()) {
            for (Map.Entry<String, JIPipeDataTable> slotMap : nodeEntry.getValue().entrySet()) {
                slotMap.getValue().makeDataVirtual(progressInfo.resolveAndLog(nodeEntry.getKey() + "/" + slotMap.getKey()));
            }
        }
    }

    private void updateSize() {
        currentSize = 0;
        for (Map.Entry<UUID, Map<String, JIPipeDataTable>> nodeEntry : cachedOutputSlots.entrySet()) {
            for (JIPipeDataTable dataTable : nodeEntry.getValue().values()) {
                currentSize += dataTable.getRowCount();
            }
        }
    }

    /**
     * Removes the cached data of a node without modifying the internal structure
     * Remove outdated will be able to remove these
     * @param uuid the node UUID
     * @param progressInfo the progress
     */
    public void softClear(UUID uuid, JIPipeProgressInfo progressInfo) {
        Map<String, JIPipeDataTable> slotMap = cachedOutputSlots.getOrDefault(uuid, null);
        if(slotMap != null) {
            progressInfo.log("Soft-clear node " + uuid);
            slotMap.clear();
        }
    }
}
