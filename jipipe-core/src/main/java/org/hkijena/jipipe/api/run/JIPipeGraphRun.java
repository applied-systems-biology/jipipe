package org.hkijena.jipipe.api.run;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import ij.IJ;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeFixedThreadPool;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.runtimepartitioning.RuntimePartitionReferenceParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.jar.Attributes;

public class JIPipeGraphRun extends AbstractJIPipeRunnable implements JIPipeGraphRunGCGraph.GCEventListener {

    private final JIPipeProject project;
    private final JIPipeGraph graph;
    private final JIPipeGraphRunSettings configuration;
    private JIPipeGraphNodeRunContext runContext;
    private final List<JIPipeRuntimePartition> runtimePartitions;

    public JIPipeGraphRun(JIPipeGraph graph, JIPipeGraphRunSettings configuration) {
        this.configuration = configuration;
        this.project = null;
        this.graph = new JIPipeGraph(graph);
        this.runtimePartitions = new ArrayList<>();
        this.runtimePartitions.add(new JIPipeRuntimePartition());
    }

    public JIPipeGraphRun(JIPipeProject project, JIPipeGraphRunSettings configuration) {
        this.project = project;
        this.graph = new JIPipeGraph(project.getGraph());
        this.configuration = configuration;
        this.runtimePartitions = new ArrayList<>();
        for (JIPipeRuntimePartition runtimePartition : project.getRuntimePartitions().toList()) {
            this.runtimePartitions.add(new JIPipeRuntimePartition(runtimePartition));
        }
    }

    public static JIPipeGraphRun loadFromFolder(Path path, JIPipeValidationReport report, JIPipeNotificationInbox notifications) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getTaskLabel() {
        return "Pipeline run";
    }

    public JIPipeGraphRunSettings getConfiguration() {
        return configuration;
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        progressInfo.log("\n" +
                "\n" +
                "     _________  _            _____              __     ___              _  __        __ \n" +
                " __ / /  _/ _ \\(_)__  ___   / ___/______ ____  / /    / _ \\__ _____    / |/ /____ __/ /_\n" +
                "/ // // // ___/ / _ \\/ -_) / (_ / __/ _ `/ _ \\/ _ \\  / , _/ // / _ \\  /    / -_) \\ / __/\n" +
                "\\___/___/_/  /_/ .__/\\__/  \\___/_/  \\_,_/ .__/_//_/ /_/|_|\\_,_/_//_/ /_/|_/\\__/_\\_\\\\__/ \n" +
                "              /_/                      /_/                                              \n" +
                "\n");
        progressInfo.log("JIPipe version: " + StringUtils.orElse(getClass().getPackage().getImplementationVersion(), "Development"));
        Attributes manifestAttributes = ReflectionUtils.getManifestAttributes();
        if (manifestAttributes != null) {
            String implementationDateString = manifestAttributes.getValue("Implementation-Date");
            progressInfo.log("JIPipe build date: " + StringUtils.orElse(implementationDateString, "N/A"));
        }
        progressInfo.log(StringUtils.orElse("ImageJ version: " + IJ.getVersion(), "N/A"));
        progressInfo.log("Java version: " + StringUtils.orElse(System.getProperty("java.version"), "N/A"));
        progressInfo.log("Registered nodes types: " + JIPipe.getNodes().getRegisteredNodeInfos().size() + " nodes");
        progressInfo.log("Registered data types: " + JIPipe.getDataTypes().getRegisteredDataTypes().size() + " types");
        progressInfo.log("Enabled extensions: " + String.join(", ", JIPipe.getInstance().getExtensionRegistry().getActivatedExtensions()));
        progressInfo.log("Operating system: " + SystemUtils.OS_NAME + " " + SystemUtils.OS_VERSION + " [" + SystemUtils.OS_ARCH + "]");
        progressInfo.log("");
        progressInfo.log("Erasing skipped/disabled algorithms (except direct predecessors) ...");
        cleanGraph(graph, progressInfo.resolve("Preprocessing/Cleanup"));

        long startTime = System.currentTimeMillis();
        progressInfo.log("JIPipe run starting at " + StringUtils.formatDateTime(LocalDateTime.now()));
        progressInfo.log("Preparing ...");
        initializeInternalStoragePaths();
        initializeScratchDirectory();
        initializeRelativeDirectories();
        assignDataStoragePaths();
        progressInfo.log("Outputs will be written to " + configuration.getOutputPath());

        try (JIPipeFixedThreadPool threadPool = new JIPipeFixedThreadPool(configuration.getNumThreads())) {

            // Setup context
            runContext = new JIPipeGraphNodeRunContext();
            runContext.setGraphRun(this);
            runContext.setThreadPool(threadPool);

            // Start iteration on initial graph
            progressInfo.log("--> Starting first iteration ...");
            runGraph(graph, progressInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            runContext = null;

            // Clear all slots
            for (JIPipeGraphNode node : graph.getGraphNodes()) {
                node.clearSlotData();
            }
        }

        // Postprocessing
        progressInfo.log("Postprocessing steps ...");
        try {
            if (configuration.getOutputPath() != null && configuration.isSaveToDisk())
                project.saveProject(configuration.getOutputPath().resolve("project.jip"));
        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(e,
                    "Could not save project to '" + configuration.getOutputPath().resolve("project.jip") + "'!",
                    "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                    "Check if you can write to the output directory.");
        }

        progressInfo.log("Run ending at " + StringUtils.formatDateTime(LocalDateTime.now()));
        progressInfo.log("\n--> Required " + StringUtils.formatDuration(System.currentTimeMillis() - startTime) + " to execute.\n");

        try {
            if (configuration.getOutputPath() != null)
                Files.write(configuration.getOutputPath().resolve("log.txt"), progressInfo.getLog().toString().getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(e,
                    "Could not write log '" + configuration.getOutputPath().resolve("log.txt") + "'!",
                    "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                    "Check if you can write to the output directory.");
        }
    }

    private void cleanGraph(JIPipeGraph graph, JIPipeProgressInfo progressInfo) {
        outer: for (JIPipeGraphNode graphNode : ImmutableList.copyOf(graph.getGraphNodes())) {
            if (graphNode instanceof JIPipeAlgorithm) {
                if (!((JIPipeAlgorithm) graphNode).isEnabled() || ((JIPipeAlgorithm) graphNode).isSkipped()) {

                    // Check if we need the algorithm in some way (cache access)
                    if(configuration.isLoadFromCache()) {
                        for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
                            for (JIPipeDataSlot inputSlot : graph.getOutputOutgoingTargetSlots(outputSlot)) {
                                JIPipeGraphNode targetNode = inputSlot.getNode();
                                if(targetNode instanceof JIPipeAlgorithm && (((JIPipeAlgorithm) targetNode).isEnabled() && !((JIPipeAlgorithm) targetNode).isSkipped())) {
                                    progressInfo.log("Keeping " + graphNode.getDisplayName() + " [is skipped or disabled, but cache predecessor]");
                                    continue outer;
                                }
                            }
                        }

                    }

                    progressInfo.log("Deleting " + graphNode.getDisplayName() + " [is skipped or disabled]");
                    graph.removeNode(graphNode, false);
                }
            } else {
                progressInfo.log("Deleting " + graphNode.getDisplayName() + " [not an algorithm]");
                graph.removeNode(graphNode, false);
            }
        }
    }

    /**
     * Iterates through output slots and assigns the data storage paths
     */
    private void assignDataStoragePaths() {
        if (configuration.getOutputPath() != null) {
            if (!Files.exists(configuration.getOutputPath())) {
                try {
                    Files.createDirectories(configuration.getOutputPath());
                } catch (IOException e) {
                    throw new JIPipeValidationRuntimeException(e,
                            "Could not create necessary directory '" + configuration.getOutputPath() + "'!",
                            "Either the path is invalid, or you do not have permissions to create the directory",
                            "Check if the path is valid and the parent directories are writeable by your current user");
                }
            }

            // Apply output path to the data slots
            for (JIPipeDataSlot slot : graph.getSlotNodes()) {
                if (slot.isOutput()) {
                    slot.setSlotStoragePath(configuration.getOutputPath().resolve(slot.getNode().getInternalStoragePath().resolve(slot.getName())));
                    try {
                        Files.createDirectories(slot.getSlotStoragePath());
                    } catch (IOException e) {
                        throw new JIPipeValidationRuntimeException(e,
                                "Could not create necessary directory '" + slot.getSlotStoragePath() + "'!",
                                "Either the path is invalid, or you do not have permissions to create the directory",
                                "Check if the path is valid and the parent directories are writeable by your current user");
                    }
                }
            }
        }
    }

    private void initializeScratchDirectory() {
        try {
            Files.createDirectories(configuration.getOutputPath().resolve("_scratch"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            algorithm.setScratchBaseDirectory(configuration.getOutputPath().resolve("_scratch"));
        }
    }

    private void initializeRelativeDirectories() {
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            algorithm.setBaseDirectory(null);
        }
    }

    private void initializeInternalStoragePaths() {
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            JIPipeProjectCompartment compartment = project.getCompartments().get(node.getCompartmentUUIDInParentGraph());
            node.setInternalStoragePath(Paths.get(StringUtils.safeJsonify(compartment.getAliasIdInParentGraph()))
                    .resolve(StringUtils.safeJsonify(graph.getAliasIdOf(node))));
            node.setProjectDirectory(project.getWorkDirectory());
            node.setRuntimeProject(project);
        }
    }

    public void runGraph(JIPipeGraph graph, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Iterating on graph " + graph + " ...");
        JIPipeGraphRunPartitionGraph partitionGraph = new JIPipeGraphRunPartitionGraph(graph);
        progressInfo.log("-> Created partition graph " + partitionGraph);

        JIPipeGraphRunGCGraph gcGraph = new JIPipeGraphRunGCGraph(graph, graph.getGraphNodes());
        gcGraph.getGcEventEmitter().subscribe(this);
        progressInfo.log("-> Created GC graph " + gcGraph);

        if (partitionGraph.vertexSet().size() == 1) {
            progressInfo.log("--> Single partition. Running graph.");
            runPartitionNodeSet(graph, partitionGraph.vertexSet().iterator().next(), gcGraph, progressInfo);
        } else if (partitionGraph.vertexSet().size() > 1) {
            progressInfo.log("--> Multiple partitions. Executing partitions in topological order.");
            TopologicalOrderIterator<Set<JIPipeGraphNode>, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(partitionGraph);
            ImmutableList<Set<JIPipeGraphNode>> partitionNodeSets = ImmutableList.copyOf(orderIterator);
            for (int i = 0; i < partitionNodeSets.size(); i++) {
                Set<JIPipeGraphNode> partitionNodeSet = partitionNodeSets.get(i);
                if (partitionNodeSet.isEmpty())
                    continue;
                runPartitionNodeSet(graph, partitionNodeSet, gcGraph, progressInfo);
            }
        } else {
            progressInfo.log("--> Nothing to do");
        }
    }

    private void runPartitionNodeSet(JIPipeGraph graph, Set<JIPipeGraphNode> partitionNodeSet, JIPipeGraphRunGCGraph gcGraph, JIPipeProgressInfo progressInfo) {
        int partitionId = 0;
        for (JIPipeGraphNode graphNode : partitionNodeSet) {
            if (graphNode instanceof JIPipeAlgorithm) {
                partitionId = ((JIPipeAlgorithm) graphNode).getRuntimePartition().getIndex();
            }
        }
        JIPipeRuntimePartition runtimePartition = getRuntimePartition(partitionId);
        progressInfo.log("--> Selected runtime partition id=" + partitionId + " as name=" + runtimePartition.getName());

        // Find the interfacing outputs (outputs that should not be cleared away by the GC)
        Set<JIPipeDataSlot> interfacingSlots = new HashSet<>();
        for (JIPipeGraphEdge graphEdge : graph.getGraph().edgeSet()) {
            JIPipeDataSlot edgeSource = graph.getGraph().getEdgeSource(graphEdge);
            JIPipeDataSlot edgeTarget = graph.getGraph().getEdgeTarget(graphEdge);
            if (edgeSource.getNode() instanceof JIPipeAlgorithm && edgeTarget.getNode() instanceof JIPipeAlgorithm) {
                if (((JIPipeAlgorithm) edgeSource.getNode()).getRuntimePartition().getIndex() == partitionId && ((JIPipeAlgorithm) edgeTarget.getNode()).getRuntimePartition().getIndex() != partitionId) {
                    interfacingSlots.add(edgeSource);
                    progressInfo.log("--> Marked output " + edgeSource.getDisplayName() + " as interfacing");
                }
            }
        }

        JIPipeProgressInfo partitionProgress = progressInfo.resolve("Partition " + partitionId);
        runGraph(graph, partitionNodeSet, gcGraph, partitionProgress);
    }

    private void runGraph(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter, JIPipeGraphRunGCGraph gcGraph, JIPipeProgressInfo progressInfo) {

        // A copy of the graph that is destroyed
        JIPipeGraphRunDataFlowGraph dataFlowGraph = new JIPipeGraphRunDataFlowGraph(graph, nodeFilter);
        progressInfo.log("Built data flow graph " + dataFlowGraph);

        int progress = 0;
        final int maxProgress = dataFlowGraph.vertexSet().size();
        progressInfo.setProgress(progress, maxProgress);

        try {
//            progressInfo.setWithSpinner(true);

            while (!dataFlowGraph.vertexSet().isEmpty()) {

                if (progressInfo.isCancelled()) {
                    throw new JIPipeValidationRuntimeException(new InterruptedException(),
                            "Execution was cancelled",
                            "You cancelled the execution of the pipeline.",
                            null);
                }

                // Increment progress
                ++progress;
                progressInfo.setProgress(progress, maxProgress);
                progressInfo.log("");

                // Find the best candidate (shortest path)
                Object nextVertex = dataFlowGraph.getBestNextVertex();

                if (nextVertex == null) {
                    throw new NullPointerException("No candidate found! Are there cycles in the graph?");
                }

                // Execute operation & cleanup
                runFlowGraphNode(nextVertex, dataFlowGraph, gcGraph, progressInfo);
            }

        } finally {
//            progressInfo.setWithSpinner(false);
        }
    }

    private void runFlowGraphNode(Object flowGraphNode, JIPipeGraphRunDataFlowGraph dataFlowGraph, JIPipeGraphRunGCGraph gcGraph, JIPipeProgressInfo progressInfo) {
        if (flowGraphNode instanceof JIPipeInputDataSlot) {
            progressInfo.log("+I " + ((JIPipeInputDataSlot) flowGraphNode).getDisplayName());

            // Add data from incoming edges within graph
            JIPipeInputDataSlot inputSlot = (JIPipeInputDataSlot) flowGraphNode;
            Set<JIPipeGraphEdge> incomingEdges = graph.getGraph().incomingEdgesOf(inputSlot);
            for (JIPipeGraphEdge graphEdge : incomingEdges) {
                JIPipeDataSlot outputSlot = graph.getGraph().getEdgeSource(graphEdge);
                inputSlot.addDataFromTable(outputSlot, progressInfo.resolve(outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName()));

                if (gcGraph != null) {
                    gcGraph.removeOutputToInputEdge(outputSlot, inputSlot, progressInfo.resolve("GC"));
                }
            }

            // Remove vertex
            dataFlowGraph.removeVertex(flowGraphNode);
        } else if (flowGraphNode instanceof JIPipeOutputDataSlot) {
            JIPipeOutputDataSlot outputSlot = (JIPipeOutputDataSlot) flowGraphNode;
            progressInfo.log("+O " + outputSlot.getDisplayName());
            progressInfo.resolve(outputSlot.getNode().getDisplayName()).log("Contents of data slot " + outputSlot);

            JIPipeOutputDataSlot outputDataSlot = (JIPipeOutputDataSlot) flowGraphNode;

            if (gcGraph != null) {
                gcGraph.removeNodeToOutputEdge(outputDataSlot, progressInfo.resolve("GC"));
            }

            // Remove vertex
            dataFlowGraph.removeVertex(flowGraphNode);
        } else if (flowGraphNode instanceof JIPipeAlgorithm) {
            JIPipeAlgorithm algorithm = (JIPipeAlgorithm) flowGraphNode;
            JIPipeProgressInfo algorithmProgress = progressInfo.resolve(algorithm.getDisplayName());
            progressInfo.log("+N " + algorithm.getDisplayName());
            algorithmProgress.log("Executing " + algorithm.getUUIDInParentGraph());


            if(!tryLoadFromCache(algorithm, algorithmProgress)) {
                try {
                    if (!algorithm.isSkipped() && algorithm.isEnabled() && algorithm.getInfo().isRunnable()) {
                        algorithm.run(runContext, algorithmProgress);
                    }
                    else {
                        algorithmProgress.log("Not runnable (skipped/disabled/node info not marked as runnable). Workload will not be executed!");
                    }
                } catch (Exception e) {
                    throw new JIPipeValidationRuntimeException(
                            new GraphNodeValidationReportContext(algorithm),
                            e,
                            "An error occurred during processing",
                            "On running the algorithm '" + algorithm.getDisplayName(),
                            "Please follow the instructions for the other error messages.");
                }
            }

            if (gcGraph != null) {
                gcGraph.removeInputToNodeEdge(algorithm, progressInfo.resolve("GC"));
            }

            // Remove vertex
            dataFlowGraph.removeVertex(flowGraphNode);

        } else {
            progressInfo.log("[!] Unsupported flow graph node type: " + flowGraphNode);

            // Remove vertex
            dataFlowGraph.removeVertex(flowGraphNode);
        }
    }

    /**
     * Attempts to load data from cache
     *
     * @param runAlgorithm the target node (inside copy graph)
     * @return if successful. This means all output slots were restored.
     */
    private boolean tryLoadFromCache(JIPipeGraphNode runAlgorithm, JIPipeProgressInfo progressInfo) {
        if (!configuration.isLoadFromCache())
            return false;
        if (!(runAlgorithm instanceof JIPipeAlgorithm))
            return false;
        Map<String, JIPipeDataTable> cachedData = project.getCache().query(runAlgorithm, runAlgorithm.getUUIDInParentGraph(), progressInfo.resolve("Query cache"));
        if (!cachedData.isEmpty()) {
            progressInfo.log("Accessing cache of node " + runAlgorithm.getUUIDInParentGraph() + " (" + runAlgorithm.getDisplayName() + ")");
            for (JIPipeDataSlot outputSlot : runAlgorithm.getOutputSlots()) {
                if (!cachedData.containsKey(outputSlot.getName())) {
                    progressInfo.log(String.format("Cache access failed. Missing output slot %s", outputSlot.getName()));
                    return false;
                }
            }
            for (JIPipeDataSlot outputSlot : runAlgorithm.getOutputSlots()) {
                if (cachedData.get(outputSlot.getName()).isEmpty()) {
                    // If it's empty, we don't know
                    progressInfo.log(String.format("Cache for slot %s is empty!", outputSlot.getName()));
                    return false;
                }
                outputSlot.clearData();
                outputSlot.addDataFromTable(cachedData.get(outputSlot.getName()), progressInfo);
            }
            progressInfo.log("Cache data access successful.");
            return true;
        }
        return false;
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }

    public JIPipeRuntimePartition getRuntimePartition(RuntimePartitionReferenceParameter reference) {
        return getRuntimePartition(reference.getIndex());
    }

    public JIPipeRuntimePartition getRuntimePartition(int index) {
        if (index >= 0 && index < runtimePartitions.size()) {
            return runtimePartitions.get(index);
        } else {
            JIPipeRuntimePartition dummy = new JIPipeRuntimePartition();
            dummy.setName("Unnamed " + index + " (fallback)");
            return dummy;
        }
    }

    @Override
    public void onGCEvent(JIPipeGraphRunGCGraph.GCEvent event) {
        storeOutput(event.getSlot());
    }

    private void storeOutput(JIPipeOutputDataSlot outputDataSlot) {
        JIPipeProgressInfo storageProgress = getProgressInfo().resolve("Storage");
        if(project != null && configuration.isStoreToCache()) {
            if(!configuration.getDisableStoreToCacheNodes().contains(outputDataSlot.getNode().getUUIDInParentGraph())) {
                storageProgress.log("Storing " + outputDataSlot.getDisplayName() + " into cache");
                project.getCache().store(outputDataSlot.getNode(), outputDataSlot.getNode().getUUIDInParentGraph(), outputDataSlot, outputDataSlot.getName(), storageProgress);
            }
            else {
                storageProgress.log("NOT storing " + outputDataSlot.getDisplayName() + " [deactivated in config]");
            }
        }
        if (configuration.isSaveToDisk() && !configuration.getDisableSaveToDiskNodes().contains(outputDataSlot.getNode().getUUIDInParentGraph())) {
            JIPipeProgressInfo saveProgress = storageProgress.resolveAndLog(String.format("Saving data in slot '%s' (data type %s)", outputDataSlot.getDisplayName(), JIPipeDataInfo.getInstance(outputDataSlot.getAcceptedDataType()).getName()));
            storageProgress.log("Flushing output slot " + outputDataSlot.getDisplayName());
            outputDataSlot.flush(saveProgress);
        }
    }
}
