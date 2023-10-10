package org.hkijena.jipipe.ui.datatracer;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.sources.JIPipeCachedDataSlotDataSource;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;

import java.util.*;

public class CollectTraceRun extends AbstractJIPipeRunnable {
    private final JIPipeProjectWorkbench workbench;
    private final JIPipeGraphNode targetNode;
    private final String targetSlotName;
    private final UUID targetNodeUUID;
    private final JIPipeDataTable targetTable;
    private final int targetTableRow;

    public CollectTraceRun(JIPipeProjectWorkbench workbench, JIPipeGraphNode targetNode, String targetSlotName, UUID targetNodeUUID, JIPipeDataTable targetTable, int targetTableRow) {
        this.workbench = workbench;
        this.targetNode = targetNode;
        this.targetSlotName = targetSlotName;
        this.targetNodeUUID = targetNodeUUID;
        this.targetTable = targetTable;
        this.targetTableRow = targetTableRow;
    }

    @Override
    public String getTaskLabel() {
        return "Calculating data trace";
    }

    private Map<String, JIPipeCachedDataSlotDataSource> getAvailableCacheItems() {
        Map<String, JIPipeCachedDataSlotDataSource> result = new HashMap<>();
        for (JIPipeGraphNode graphNode : workbench.getProject().getGraph().getGraphNodes()) {
            UUID uuid = graphNode.getUUIDInParentGraph();
            Map<String, JIPipeDataTable> cache = workbench.getProject().getCache().query(graphNode, uuid, getProgressInfo());
            for (Map.Entry<String, JIPipeDataTable> entry : cache.entrySet()) {
                JIPipeDataTable dataTable = entry.getValue();
                for (int row = 0; row < dataTable.getRowCount(); row++) {
                    result.put(dataTable.getDataContext(row).getId(), new JIPipeCachedDataSlotDataSource(dataTable, row, uuid, entry.getKey()));
                }
            }
        }
        return result;
    }

    private void insertIntoResultMap(Map<Integer, Map<String, Map<String, JIPipeDataTable>>> resultMap, int level, String nodeUUID, String slotName, JIPipeDataTable sourceTable, int sourceRow) {
        Map<String, Map<String, JIPipeDataTable>> levelMap = resultMap.getOrDefault(level, null);
        if(levelMap == null) {
            levelMap = new HashMap<>();
            resultMap.put(level, levelMap);
        }
        Map<String, JIPipeDataTable> nodeMap = levelMap.getOrDefault(nodeUUID, null);
        if(nodeMap == null) {
            nodeMap = new HashMap<>();
            levelMap.put(nodeUUID, nodeMap);
        }
        JIPipeDataTable targetTable = nodeMap.getOrDefault(slotName, null);
        if(targetTable == null) {
            targetTable = new JIPipeDataTable();
            nodeMap.put(slotName, targetTable);
        }

        targetTable.addDataFromTable(sourceTable.slice(Collections.singleton(sourceRow)), getProgressInfo());
    }

    private Set<String> getPredecessors(JIPipeCachedDataSlotDataSource dataSource) {
        return dataSource.getDataTable().getDataContext(dataSource.getRow()).getPredecessors();
    }

    @Override
    public void run() {
        getProgressInfo().log("Collecting available cache items ...");
        Map<String, JIPipeCachedDataSlotDataSource> availableCacheItems = getAvailableCacheItems();
        getProgressInfo().log(availableCacheItems.size() + " available items found");
        final String targetId = targetTable.getDataContext(targetTableRow).getId();
        getProgressInfo().log("Target ID: " + targetId);

        if(!availableCacheItems.containsKey(targetId)) {
            throw new RuntimeException("Unable to find " + targetId + " in cache!");
        }

        // Map from Level -> Node UUID -> Output Slot -> List of cached items
        Map<Integer, Map<String, Map<String, JIPipeDataTable>>> resultMap = new HashMap<>();

        // Add the initial cached item
        insertIntoResultMap(resultMap, 0, targetNodeUUID.toString(), targetSlotName, targetTable, targetTableRow);

        getProgressInfo().log("Backward iteration ...");
        backwardIteration(resultMap, availableCacheItems);

        getProgressInfo().log("Forward iteration ...");

    }

    private void backwardIteration(Map<Integer, Map<String, Map<String, JIPipeDataTable>>> resultMap, Map<String, JIPipeCachedDataSlotDataSource> availableCacheItems) {
        Queue<JIPipeCachedDataSlotDataSource> queue = new ArrayDeque<>();
        for (String predecessorId : targetTable.getDataContext(targetTableRow).getPredecessors()) {
            JIPipeCachedDataSlotDataSource predecessorSource = availableCacheItems.getOrDefault(predecessorId, null);
            if(predecessorSource != null) {
                queue.add(predecessorSource);
            }
            else {
                getProgressInfo().log("WARNING: Unable to find data " + predecessorId);
            }
        }
        while (!queue.isEmpty()) {
            JIPipeCachedDataSlotDataSource current = queue.remove();
        }
    }


}
