package org.hkijena.jipipe.api.cache;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.concurrent.locks.StampedLock;

public class JIPipeLocalProjectMemoryCache implements JIPipeCache {

    private final JIPipeProject project;

    /**
     * The cached data
     */
    private final Map<UUID, Map<String, JIPipeDataTable>> cachedOutputSlots = new HashMap<>();

    /**
     * A copy of all nodes
     */
    private final Map<UUID, JIPipeGraphNode> currentNodeStates = new HashMap<>();

    /**
     * For each node UUID the set of expected predecessor UUIDs
     */
    private final Map<UUID, Set<UUID>> expectedNodePredecessors = new HashMap<>();

    /**
     * For each node the set of expected parents
     */
    private final Map<UUID, Set<UUID>> currentNodeStateInputs = new HashMap<>();
    private final StoredEventEmitter storedEventEmitter = new StoredEventEmitter();
    private final ClearedEventEmitter clearedEventEmitter = new ClearedEventEmitter();
    private final ModifiedEventEmitter modifiedEventEmitter = new ModifiedEventEmitter();
    private DefaultDirectedGraph<UUID, DefaultEdge> currentNodeStatePredecessorGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
    private int currentSize = 0;
    private boolean ignoreNodeFunctionalEquals = false;
    private final StampedLock stampedLock = new StampedLock();

    public JIPipeLocalProjectMemoryCache(JIPipeProject project) {
        this.project = project;
    }

    @Override
    public StoredEventEmitter getStoredEventEmitter() {
        return storedEventEmitter;
    }

    @Override
    public ClearedEventEmitter getClearedEventEmitter() {
        return clearedEventEmitter;
    }

    @Override
    public ModifiedEventEmitter getModifiedEventEmitter() {
        return modifiedEventEmitter;
    }

    @Override
    public void store(JIPipeGraphNode graphNode, UUID nodeUUID, JIPipeDataTable data, String outputName, JIPipeProgressInfo progressInfo) {
        long stamp = stampedLock.writeLock();
        try {
            JIPipeGraphNode projectNode = project.getGraph().getNodeByUUID(nodeUUID);
            if (projectNode == null) {
                progressInfo.log("Refusing to cache node " + nodeUUID + " (" + graphNode.getDisplayName() + ") --> Not in project anymore!");
                return;
            }

            // Store the current state for comparison and update the predecessor graph
            JIPipeGraphNode graphNodeDuplicate = graphNode.duplicate();
            currentNodeStates.put(nodeUUID, graphNodeDuplicate);
            putNodeIntoGraph_(projectNode, progressInfo);

            // Store the data
            Map<String, JIPipeDataTable> slotMap = cachedOutputSlots.getOrDefault(nodeUUID, null);
            if (slotMap == null) {
                slotMap = new HashMap<>();
                cachedOutputSlots.put(nodeUUID, slotMap);
            }
            JIPipeDataTable dataTableCopy = new JIPipeOutputDataSlot(new JIPipeDataSlotInfo(data.getAcceptedDataType(), JIPipeSlotType.Output, outputName, ""), projectNode);
            dataTableCopy.addDataFromTable(data, progressInfo);
            slotMap.put(outputName, dataTableCopy);
            progressInfo.log("Stored " + data.getRowCount() + " into " + nodeUUID + "/" + outputName);
        }
        finally {
            stampedLock.unlock(stamp);
        }

        updateSize_();
        storedEventEmitter.emit(new StoredEvent(this, nodeUUID, data, outputName));
        modifiedEventEmitter.emit(new ModifiedEvent(this));
    }

    private Set<UUID> getDirectParentNodeUUIDs_(JIPipeGraphNode graphNode) {
        Set<UUID> inputUUIDs = new HashSet<>();
        for (JIPipeInputDataSlot inputSlot : graphNode.getInputSlots()) {
            Set<JIPipeDataSlot> inputIncomingSourceSlots = graphNode.getParentGraph().getInputIncomingSourceSlots(inputSlot);
            if (inputIncomingSourceSlots != null) {
                for (JIPipeDataSlot sourceSlot : inputIncomingSourceSlots) {
                    inputUUIDs.add(sourceSlot.getNode().getUUIDInParentGraph());
                }
            }
        }
        return inputUUIDs;
    }

    private void putNodeIntoGraph_(JIPipeGraphNode projectNode, JIPipeProgressInfo progressInfo) {
        UUID uuid = projectNode.getUUIDInParentGraph();
        currentNodeStatePredecessorGraph.addVertex(uuid);

        // Store the direct parent UUIDs for later comparison
        Set<UUID> inputUUIDs = getDirectParentNodeUUIDs_(projectNode);
        currentNodeStateInputs.put(uuid, inputUUIDs);

        // Store all existing predecessors
        Set<UUID> existingPredecessors = new HashSet<>();
        Set<UUID> expectedPredecessors = new HashSet<>();
        for (DefaultEdge defaultEdge : currentNodeStatePredecessorGraph.incomingEdgesOf(uuid)) {
            UUID predecessorUUID = currentNodeStatePredecessorGraph.getEdgeSource(defaultEdge);
            existingPredecessors.add(predecessorUUID);
        }

        // Register predecessors
        List<JIPipeGraphNode> predecessorAlgorithms = project.getGraph().getAllPredecessorNodes(projectNode, project.getGraph().traverse());
        for (JIPipeGraphNode predecessorAlgorithm : predecessorAlgorithms) {
            UUID predecessorUUID = predecessorAlgorithm.getUUIDInParentGraph();

            // Add if necessary
            if (!currentNodeStatePredecessorGraph.containsVertex(predecessorUUID)) {
                progressInfo.log("Register predecessor " + predecessorUUID + " of " + uuid);
                currentNodeStatePredecessorGraph.addVertex(predecessorUUID);
                putNodeIntoGraph_(project.getGraph().getNodeByUUID(predecessorUUID), progressInfo);
            }

            currentNodeStates.put(predecessorUUID, predecessorAlgorithm.duplicate());
            expectedPredecessors.add(predecessorUUID);
            currentNodeStatePredecessorGraph.addEdge(predecessorUUID, uuid);
        }
        expectedNodePredecessors.put(uuid, expectedPredecessors); // needed for pruning

        // Remove old predecessors
        for (UUID existingPredecessor : existingPredecessors) {
            if (!expectedPredecessors.contains(existingPredecessor)) {
                currentNodeStatePredecessorGraph.removeEdge(existingPredecessor, uuid);
            }
        }

        progressInfo.log("Tracked " + expectedPredecessors.size() + " predecessors");
    }

    /**
     * Iterates through all cached node states and remove all that are not existing within the project graph or where the project graph was changed
     *
     * @return if nodes were removed
     */
    private boolean removeInvalidNodeStates_(JIPipeProgressInfo progressInfo) {
        boolean updated = false;
        if (ignoreNodeFunctionalEquals) {
            progressInfo.log("Node functional states (functionallyEquals) will be ignored for the removal of invalid node states");
        }
        for (UUID uuid : ImmutableList.copyOf(currentNodeStates.keySet())) {
            JIPipeGraphNode currentNode = project.getGraph().getNodeByUUID(uuid);
            JIPipeGraphNode cachedNode = currentNodeStates.get(uuid);

            // Remove deleted node
            if (currentNode == null) {
                updated = true;
                removeAndInvalidateNodeCache_(uuid, progressInfo);
                progressInfo.log("Removed invalid node state for " + uuid + " [node deleted]");
                continue;
            }

            // Check inputs
            Set<UUID> directParentNodeUUIDs = getDirectParentNodeUUIDs_(currentNode);
            if (!Objects.equals(directParentNodeUUIDs, currentNodeStateInputs.get(uuid))) {
                updated = true;
                removeAndInvalidateNodeCache_(uuid, progressInfo);
                progressInfo.log("Removed invalid node state for " + uuid + " [inputs changed]");
                continue;
            }

            // Check output slots
            Map<String, JIPipeDataTable> slotMap = cachedOutputSlots.getOrDefault(uuid, null);
            if (slotMap != null) {
                if (slotMap.isEmpty()) {
                    updated = true;
                    removeAndInvalidateNodeCache_(uuid, progressInfo);
                    progressInfo.log("Removed invalid node state for " + uuid + " [empty slot map]");
                    continue;
                }
            }

            if (!ignoreNodeFunctionalEquals) {
                if (currentNode == null || !currentNode.functionallyEquals(cachedNode)) {
                    updated = true;
                    removeAndInvalidateNodeCache_(uuid, progressInfo);
                    progressInfo.log("Removed invalid node state for " + uuid);
                }
            }
        }
        return updated;
    }

    public boolean isIgnoreNodeFunctionalEquals() {
        return ignoreNodeFunctionalEquals;
    }

    public void setIgnoreNodeFunctionalEquals(boolean ignoreNodeFunctionalEquals) {
        this.ignoreNodeFunctionalEquals = ignoreNodeFunctionalEquals;
    }

    /**
     * Removes all vertices that have missing inputs
     *
     * @return if nodes were removed
     */
    private boolean pruneGraph_(JIPipeProgressInfo progressInfo) {
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
                if (!expectedPredecessors.equals(availablePredecessors)) {
                    removeAndInvalidateNodeCache_(uuid, progressInfo);

                    modified = true;
                    updated = true;

                    iterationProgress.log("Deleting cache of " + uuid + " --> predecessors are missing");
                }
            }
        }
        while (modified);

        return updated;
    }

    private void removeAndInvalidateNodeCache_(UUID uuid, JIPipeProgressInfo progressInfo) {
        Map<String, JIPipeDataTable> dataTableMap = cachedOutputSlots.getOrDefault(uuid, null);
        if (dataTableMap != null && !dataTableMap.isEmpty()) {
            int items = 0;
            for (Map.Entry<String, JIPipeDataTable> entry : dataTableMap.entrySet()) {
                items += entry.getValue().getRowCount();
            }
            progressInfo.log("Freed node " + uuid + " with " + items + " cached rows");
        }
        cachedOutputSlots.remove(uuid);
        currentNodeStates.remove(uuid);
        currentNodeStateInputs.remove(uuid);
        expectedNodePredecessors.remove(uuid);
        currentNodeStatePredecessorGraph.removeVertex(uuid);
    }

    @Override
    public Map<String, JIPipeDataTable> query(JIPipeGraphNode graphNode, UUID nodeUUID, JIPipeProgressInfo progressInfo) {
        long stamp = stampedLock.readLock();
        try {
            return cachedOutputSlots.getOrDefault(nodeUUID, Collections.emptyMap());
        }
        finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public void clearOutdated(JIPipeProgressInfo progressInfo) {
        long stamp = stampedLock.writeLock();
        boolean updated;
        try {
            updated = removeInvalidNodeStates_(progressInfo);
            updated |= pruneGraph_(progressInfo);
        } finally {
            stampedLock.unlock(stamp);
        }
        if (updated) {
            updateSize_();
            clearedEventEmitter.emit(new ClearedEvent(this, null));
            modifiedEventEmitter.emit(new ModifiedEvent(this));
        }
    }

    @Override
    public void clearAll(JIPipeProgressInfo progressInfo) {
        long stamp = stampedLock.writeLock();
        try {
            cachedOutputSlots.clear();
            currentNodeStates.clear();
            expectedNodePredecessors.clear();
            currentNodeStateInputs.clear();
            currentNodeStatePredecessorGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        } finally {
            stampedLock.unlock(stamp);
        }

        updateSize_();
        clearedEventEmitter.emit(new ClearedEvent(this, null));
        modifiedEventEmitter.emit(new ModifiedEvent(this));
    }

    @Override
    public void clearAll(UUID nodeUUID, boolean invalidateChildren, JIPipeProgressInfo progressInfo) {
        long stamp = stampedLock.writeLock();
        try {
            if (invalidateChildren) {
                removeAndInvalidateNodeCache_(nodeUUID, progressInfo);
            } else {
                // Only remove the data
                cachedOutputSlots.remove(nodeUUID);
            }
        } finally {
            stampedLock.unlock(stamp);
        }

        updateSize_();
        clearedEventEmitter.emit(new ClearedEvent(this, nodeUUID));
        modifiedEventEmitter.emit(new ModifiedEvent(this));
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

    private void updateSize_() {
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
     *
     * @param uuid         the node UUID
     * @param progressInfo the progress
     */
    public void softClear(UUID uuid, JIPipeProgressInfo progressInfo) {
        long stamp = stampedLock.writeLock();
        try {
            Map<String, JIPipeDataTable> slotMap = cachedOutputSlots.getOrDefault(uuid, null);
            if (slotMap != null) {
                progressInfo.log("Soft-clear node " + uuid);
                slotMap.clear();
            }
        } finally {
            stampedLock.unlock(stamp);
        }
    }
}
