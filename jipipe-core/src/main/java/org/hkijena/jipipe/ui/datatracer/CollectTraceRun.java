/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.datatracer;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.data.sources.JIPipeCachedDataSlotDataSource;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

public class CollectTraceRun extends AbstractJIPipeRunnable {
    private final JIPipeProjectWorkbench workbench;
    private final JIPipeGraphNode targetNode;
    private final String targetSlotName;
    private final UUID targetNodeUUID;
    private final JIPipeDataTable targetTable;
    private final int targetTableRow;

    // Map from Level -> Node UUID -> Output Slot -> List of cached items
    private final Map<Integer, Map<String, Map<String, JIPipeDataTable>>> resultMap = new HashMap<>();

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

    @Override
    public void run() {
        getProgressInfo().setProgress(0, 3);
        getProgressInfo().log("Collecting available cache items ...");
        Map<String, JIPipeCachedDataSlotDataSource> availableCacheItems = getAvailableCacheItems();
        getProgressInfo().log(availableCacheItems.size() + " available items found");
        final String targetId = targetTable.getDataContext(targetTableRow).getId();
        getProgressInfo().log("Target ID: " + targetId);

        if(!availableCacheItems.containsKey(targetId)) {
            throw new RuntimeException("Unable to find " + targetId + " in cache!");
        }

        // Add the initial cached item
        insertIntoResultMap(resultMap, 0, targetNodeUUID.toString(), targetSlotName, targetTable, targetTableRow);

        // Create trace graph
        getProgressInfo().setProgress(1, 3);
        getProgressInfo().log("Generating trace graph ...");
        DefaultDirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (Map.Entry<String, JIPipeCachedDataSlotDataSource> entry : availableCacheItems.entrySet()) {
            JIPipeCachedDataSlotDataSource dataSource = entry.getValue();
            JIPipeDataContext dataContext = dataSource.getDataTable().getDataContext(dataSource.getRow());
            graph.addVertex(dataContext.getId());
            for (String predecessor : dataContext.getPredecessors()) {
                if(availableCacheItems.containsKey(predecessor)) {
                    graph.addVertex(predecessor);
                    graph.addEdge(predecessor, entry.getKey());
                }
                else {
                    getProgressInfo().log("Unable to find " + predecessor + "! Skipped.");
                }
            }
        }

        // Extract results from trace graph
        getProgressInfo().setProgress(2, 3);
        getProgressInfo().log("Extracting paths from/to target data");
        DijkstraShortestPath<String, DefaultEdge> shortestPath = new DijkstraShortestPath<>(graph);
        for (Map.Entry<String, JIPipeCachedDataSlotDataSource> entry : availableCacheItems.entrySet()) {
            GraphPath<String, DefaultEdge> predecessorPath = shortestPath.getPath(entry.getKey(), targetId);
            GraphPath<String, DefaultEdge> successorPath = predecessorPath == null ? shortestPath.getPath(targetId, entry.getKey()) : null;

            if(predecessorPath != null && predecessorPath.getLength() != 0) {
                JIPipeCachedDataSlotDataSource dataSource = availableCacheItems.get(entry.getKey());
                insertIntoResultMap(resultMap, -predecessorPath.getLength(), dataSource.getNodeUUID().toString(), dataSource.getSlotName(), dataSource.getDataTable(), dataSource.getRow());
            }
            else if(successorPath != null && successorPath.getLength() != 0) {
                JIPipeCachedDataSlotDataSource dataSource = availableCacheItems.get(entry.getKey());
                insertIntoResultMap(resultMap, successorPath.getLength(), dataSource.getNodeUUID().toString(), dataSource.getSlotName(), dataSource.getDataTable(), dataSource.getRow());
            }
        }

        getProgressInfo().setProgress(3, 3);
    }

    public Map<Integer, Map<String, Map<String, JIPipeDataTable>>> getResultMap() {
        return resultMap;
    }
}
