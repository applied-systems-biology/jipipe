package org.hkijena.jipipe.api.run;

import com.google.common.base.Charsets;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
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
import org.hkijena.jipipe.api.grouping.JIPipeGraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithmIterationStepGenerationSettings;
import org.hkijena.jipipe.api.nodes.infos.JIPipeEmptyNodeInfo;
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
    private final JIPipeGraphRun parent;
    private final JIPipeGraphRunSettings configuration;
    private JIPipeGraphNodeRunContext runContext;
    private final List<JIPipeRuntimePartition> runtimePartitions;

    public JIPipeGraphRun(JIPipeGraphRun parent, JIPipeGraph graph, JIPipeGraphRunSettings configuration) {
        this.parent = parent;
        this.configuration = configuration;
        this.project = parent.getProject();
        this.graph = new JIPipeGraph(graph);
        this.runtimePartitions = new ArrayList<>();
        this.runtimePartitions.add(new JIPipeRuntimePartition());
    }

    public JIPipeGraphRun(JIPipeProject project, JIPipeGraphRunSettings configuration) {
        this.project = project;
        this.graph = new JIPipeGraph(project.getGraph());
        this.parent = null;
        this.configuration = configuration;
        this.runtimePartitions = new ArrayList<>();
        for (JIPipeRuntimePartition runtimePartition : project.getRuntimePartitions().toList()) {
            this.runtimePartitions.add(new JIPipeRuntimePartition(runtimePartition));
        }
    }

    public static JIPipeGraphRun loadFromFolder(Path path, JIPipeValidationReport report, JIPipeNotificationInbox notifications) {
        throw new UnsupportedOperationException("not implemented");
    }

    public JIPipeGraphRun getParent() {
        return parent;
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

        if (parent == null) {
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
        }

        progressInfo.log("Erasing skipped/disabled algorithms (except direct predecessors) ...");
        cleanGraph(graph, progressInfo.resolve("Preprocessing/Cleanup"));

        long startTime = System.currentTimeMillis();
        if (parent == null) {
            progressInfo.log("JIPipe run starting at " + StringUtils.formatDateTime(LocalDateTime.now()));
        }

        progressInfo.log("Preparing ...");
        initializeInternalStoragePaths();
        initializeScratchDirectory();
        initializeRelativeDirectories();
        assignDataStoragePaths();
        if (parent == null) {
            progressInfo.log("Outputs will be written to " + configuration.getOutputPath());
        }

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
                for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                    if (inputSlot.getRowCount() > 0) {
                        progressInfo.log("[!] Slot " + inputSlot.getDisplayName() + " still contains " + inputSlot.getRowCount() + " items! Clearing in post-processing!");
                    }
                    inputSlot.clear();
                }
                for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
                    if (!outputSlot.isSkipGC()) {
                        if (outputSlot.getRowCount() > 0) {
                            progressInfo.log("[!] Slot " + outputSlot.getDisplayName() + " still contains " + outputSlot.getRowCount() + " items! Clearing in post-processing!");
                        }
                        outputSlot.clear();
                    }
                }
            }
        }

        // Postprocessing
        if (parent == null) {
            progressInfo.log("Postprocessing steps ...");
            try {
                if (configuration.getOutputPath() != null && configuration.isStoreToDisk())
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
    }

    public void cleanGraph(JIPipeGraph graph, JIPipeProgressInfo progressInfo) {
        outer:
        for (JIPipeGraphNode graphNode : ImmutableList.copyOf(graph.getGraphNodes())) {
            if (graphNode instanceof JIPipeAlgorithm) {
                if (!((JIPipeAlgorithm) graphNode).isEnabled() || ((JIPipeAlgorithm) graphNode).isSkipped()) {

                    // Check if we need the algorithm in some way (cache access)
                    if (configuration.isLoadFromCache()) {
                        for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
                            for (JIPipeDataSlot inputSlot : graph.getOutputOutgoingTargetSlots(outputSlot)) {
                                JIPipeGraphNode targetNode = inputSlot.getNode();
                                if (targetNode instanceof JIPipeAlgorithm && (((JIPipeAlgorithm) targetNode).isEnabled() && !((JIPipeAlgorithm) targetNode).isSkipped())) {
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
            if (compartment != null) {
                node.setInternalStoragePath(Paths.get(StringUtils.safeJsonify(compartment.getAliasIdInParentGraph()))
                        .resolve(StringUtils.safeJsonify(graph.getAliasIdOf(node))));
            } else {
                node.setInternalStoragePath(Paths.get("_sub")
                        .resolve(UUID.randomUUID().toString())
                        .resolve(StringUtils.safeJsonify(graph.getAliasIdOf(node))));
            }
            node.setProjectDirectory(project.getWorkDirectory());
            node.setRuntimeProject(project);
        }
    }

    private void runGraph(JIPipeGraph graph, JIPipeProgressInfo progressInfo) {
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

        if (runtimePartition.getIterationMode() == JIPipeGraphWrapperAlgorithm.IterationMode.PassThrough) {
            // Non-iterating mode
            JIPipeProgressInfo partitionProgress = progressInfo.resolve("Partition " + partitionId);
            runGraph(graph, partitionNodeSet, gcGraph, runtimePartition, partitionProgress);
        } else {
            runIteratingPartitionNodeSet(graph, partitionNodeSet, gcGraph, runtimePartition, progressInfo.resolve("Looped partition " + partitionId));
        }
    }

    private void runIteratingPartitionNodeSet(JIPipeGraph graph, Set<JIPipeGraphNode> partitionNodeSet, JIPipeGraphRunGCGraph gcGraph, JIPipeRuntimePartition runtimePartition, JIPipeProgressInfo progressInfo) {
        JIPipeGraph extracted = graph.extractShallow(partitionNodeSet, true, false);
        JIPipeGraphWrapperAlgorithm graphWrapperAlgorithm = new JIPipeGraphWrapperAlgorithm(new JIPipeEmptyNodeInfo(), extracted);

        // Configure iterations
        graphWrapperAlgorithm.setIterationMode(runtimePartition.getIterationMode());
        if (runtimePartition.getIterationMode() == JIPipeGraphWrapperAlgorithm.IterationMode.IteratingDataBatch) {
            graphWrapperAlgorithm.setBatchGenerationSettings(new JIPipeMergingAlgorithmIterationStepGenerationSettings(runtimePartition.getLoopIterationIteratingSettings()));
        } else {
            graphWrapperAlgorithm.setBatchGenerationSettings(new JIPipeMergingAlgorithmIterationStepGenerationSettings(runtimePartition.getLoopIterationMergingSettings()));
        }

        // Create a group i/o slot for all interfacing inputs and all outputs
        BiMap<JIPipeDataSlot, String> inputMap = HashBiMap.create();
        BiMap<JIPipeDataSlot, String> outputMap = HashBiMap.create();
        for (JIPipeGraphNode node : partitionNodeSet) {
            if (node instanceof JIPipeAlgorithm) {

                // Register interfacing inputs (all others MUST be left alone!)
                outer:
                for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                    for (JIPipeGraphEdge graphEdge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                        JIPipeDataSlot edgeSource = graph.getGraph().getEdgeSource(graphEdge);
                        if (edgeSource.getNode() instanceof JIPipeAlgorithm) {
                            if (!Objects.equals(((JIPipeAlgorithm) edgeSource.getNode()).getRuntimePartition(), ((JIPipeAlgorithm) node).getRuntimePartition())) {
                                String uuid = UUID.randomUUID().toString();
                                progressInfo.log("--> Detecting interfacing input " + inputSlot.getDisplayName() + " (registered as " + uuid + ")");
                                inputMap.put(inputSlot, uuid);
                                continue outer;
                            }
                        }
                    }
                }

                // Register all interfacing or endpoint outputs (important due to caching) or intermediate outputs
                outer:
                for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
                    if (graph.getGraph().outDegreeOf(outputSlot) == 0) {
                        String uuid = UUID.randomUUID().toString();
                        progressInfo.log("--> Detecting endpoint output " + outputSlot.getDisplayName() + " (registered as " + uuid + ")");
                        outputMap.put(outputSlot, uuid);
                    } else {
                        for (JIPipeGraphEdge graphEdge : graph.getGraph().outgoingEdgesOf(outputSlot)) {
                            JIPipeDataSlot edgeTarget = graph.getGraph().getEdgeTarget(graphEdge);
                            if (edgeTarget.getNode() instanceof JIPipeAlgorithm) {
                                if (!Objects.equals(((JIPipeAlgorithm) edgeTarget.getNode()).getRuntimePartition(), ((JIPipeAlgorithm) node).getRuntimePartition())) {
                                    String uuid = UUID.randomUUID().toString();
                                    progressInfo.log("--> Detecting interfacing output " + outputSlot.getDisplayName() + " (registered as " + uuid + ")");
                                    outputMap.put(outputSlot, uuid);
                                    continue outer;
                                } else if (!outputSlot.isSkipExport() && !outputSlot.isSkipCache() && !configuration.getDisableStoreToCacheNodes().contains(outputSlot.getNode().getUUIDInParentGraph())) {
                                    String uuid = UUID.randomUUID().toString();
                                    progressInfo.log("--> Detecting intermediate output " + outputSlot.getDisplayName() + " (registered as " + uuid + ")");
                                    outputMap.put(outputSlot, uuid);
                                    continue outer;
                                }
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<JIPipeDataSlot, String> entry : inputMap.entrySet()) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) graphWrapperAlgorithm.getGroupInput().getSlotConfiguration();
            JIPipeDataSlotInfo source = entry.getKey().getInfo();
            slotConfiguration.addSlot(new JIPipeDataSlotInfo(source.getDataClass(), JIPipeSlotType.Input, entry.getValue(), "", source.isOptional()), false);
        }
        for (Map.Entry<JIPipeDataSlot, String> entry : outputMap.entrySet()) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) graphWrapperAlgorithm.getGroupOutput().getSlotConfiguration();
            JIPipeDataSlotInfo source = entry.getKey().getInfo();
            slotConfiguration.addSlot(new JIPipeDataSlotInfo(source.getDataClass(), JIPipeSlotType.Output, entry.getValue(), "", source.isOptional()), false);
        }

        graphWrapperAlgorithm.updateGroupSlots();

        // Connected graph wrapper algorithm internally
        for (Map.Entry<JIPipeDataSlot, String> entry : inputMap.entrySet()) {
            // [Group input] out --> input slot
            JIPipeOutputDataSlot outputSlot = graphWrapperAlgorithm.getGroupInput().getOutputSlot(entry.getValue());
            JIPipeInputDataSlot inputSlot = (JIPipeInputDataSlot) entry.getKey();
            progressInfo.log("Wrapped graph connect " + outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName());
            graphWrapperAlgorithm.getWrappedGraph().connect(outputSlot, inputSlot);
        }
        for (Map.Entry<JIPipeDataSlot, String> entry : outputMap.entrySet()) {
            // output slot --> [Group output] in
            JIPipeInputDataSlot inputSlot = graphWrapperAlgorithm.getGroupOutput().getInputSlot(entry.getValue());
            JIPipeOutputDataSlot outputSlot = (JIPipeOutputDataSlot) entry.getKey();
            progressInfo.log("Wrapped graph connect " + outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName());
            graphWrapperAlgorithm.getWrappedGraph().connect(outputSlot, inputSlot);
        }

        // Pull data into the original input slots
        // Trigger GC accordingly
        progressInfo.log("Gathering inputs ...");
        for (Map.Entry<JIPipeDataSlot, String> entry : inputMap.entrySet()) {
            JIPipeInputDataSlot inputSlot = (JIPipeInputDataSlot) entry.getKey();
            progressInfo.log("+I " + inputSlot.getDisplayName());

            // Add data from incoming edges within graph
            Set<JIPipeGraphEdge> incomingEdges = graph.getGraph().incomingEdgesOf(inputSlot);
            for (JIPipeGraphEdge graphEdge : incomingEdges) {
                JIPipeDataSlot outputSlot = graph.getGraph().getEdgeSource(graphEdge);

                if (!inputSlot.isSkipDataGathering()) {
                    inputSlot.addDataFromTable(outputSlot, progressInfo.resolve(outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName()));
                } else {
                    progressInfo.log("NOT copying " + outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName() + " [data gathering disabled]");
                }

                if (gcGraph != null) {
                    gcGraph.removeOutputToInputEdge(outputSlot, inputSlot, progressInfo.resolve("GC"));
                }
            }
        }

        // Move data from original input slots to wrapper input slots
        progressInfo.log("Moving inputs to wrapped nodes ...");
        for (Map.Entry<JIPipeDataSlot, String> entry : inputMap.entrySet()) {
            JIPipeInputDataSlot originalInputSlot = (JIPipeInputDataSlot) entry.getKey();
            JIPipeInputDataSlot targetInputSlot = graphWrapperAlgorithm.getInputSlot(entry.getValue());
            targetInputSlot.addDataFromTable(originalInputSlot, progressInfo.resolve("I -> W-I"));

            // GC the original
            originalInputSlot.clear();
        }

        // Execute the graph wrapper
        try {
            graphWrapperAlgorithm.run(runContext, progressInfo);
        }
        catch (Throwable e) {
            if (progressInfo.isCancelled()) {
                throw e;
            }
            if(runtimePartition.isContinueOnFailure()) {
                progressInfo.log("\n\n------------------------\n" +
                        "Partition iteration execution FAILED!\n" +
                        "Message: " + e.getMessage() + "\n" +
                        "\n" +
                        "CONTINUING AS REQUESTED!\n" +
                        "------------------------\n\n");

                // Clean to prevent moving corrupted data out
                graphWrapperAlgorithm.clearSlotData();
            }
            else {
                throw e;
            }
        }

        // Copy outputs into nodes
        progressInfo.log("Moving outputs to parent graph nodes ...");
        for (Map.Entry<JIPipeDataSlot, String> entry : outputMap.entrySet()) {
            JIPipeOutputDataSlot targetOutputSlot = (JIPipeOutputDataSlot) entry.getKey();
            JIPipeOutputDataSlot sourceOutputSlot = graphWrapperAlgorithm.getOutputSlot(entry.getValue());
            targetOutputSlot.addDataFromTable(sourceOutputSlot, progressInfo.resolve("W-O -> O [" + sourceOutputSlot.getDisplayName() + " >>> " + targetOutputSlot.getDisplayName() + "]"));

            // GC the wrapped
            sourceOutputSlot.clear();
        }

        // Cleanup to be sure
        graphWrapperAlgorithm.clearSlotData();

        // Trigger GC
        if (gcGraph != null) {
            // Destroy all edges
            progressInfo.log("Informing GC ...");
            for (JIPipeGraphNode node : partitionNodeSet) {
                if (node instanceof JIPipeAlgorithm) {
                    // Not needed (done earlier)
                    for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                        for (JIPipeGraphEdge graphEdge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                            gcGraph.removeOutputToInputEdge(graph.getGraph().getEdgeSource(graphEdge), inputSlot, progressInfo.resolve("GC"));
                        }
                    }
                    gcGraph.removeInputToNodeEdge((JIPipeAlgorithm) node, progressInfo.resolve("GC"));
                    for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
                        gcGraph.removeNodeToOutputEdge(outputSlot, progressInfo.resolve("GC"));
                    }
                }
            }
        }
    }

    private void runGraph(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter, JIPipeGraphRunGCGraph gcGraph, JIPipeRuntimePartition runtimePartition, JIPipeProgressInfo progressInfo) {

        // A copy of the graph that is destroyed
        JIPipeGraphRunDataFlowGraph dataFlowGraph = new JIPipeGraphRunDataFlowGraph(graph, nodeFilter);
        progressInfo.log("Built data flow graph " + dataFlowGraph);

        int progress = 0;
        final int maxProgress = dataFlowGraph.vertexSet().size();
        progressInfo.setProgress(progress, maxProgress);

        //            progressInfo.setWithSpinner(true);

        try {
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
                runFlowGraphNode(nextVertex, dataFlowGraph, gcGraph, false, progressInfo);
            }
        } catch (Throwable e) {
            if (progressInfo.isCancelled()) {
                throw e;
            }
            if (runtimePartition.isContinueOnFailure()) {
                progressInfo.log("\n\n------------------------\n" +
                        "Partition execution FAILED!\n" +
                        "Message: " + e.getMessage() + "\n" +
                        "\n" +
                        "CONTINUING AS REQUESTED!\n" +
                        "------------------------\n\n");

                // Cleanup
                while (!dataFlowGraph.vertexSet().isEmpty()) {
                    // Find the best candidate (shortest path)
                    Object nextVertex = dataFlowGraph.getBestNextVertex();

                    if (nextVertex == null) {
                        throw new NullPointerException("No candidate found! Are there cycles in the graph?");
                    }

                    // Execute operation & cleanup
                    runFlowGraphNode(nextVertex, dataFlowGraph, gcGraph, true, progressInfo.resolve("Cleanup"));
                }
            } else {
                throw e;
            }
        }

    }

    private void runFlowGraphNode(Object flowGraphNode, JIPipeGraphRunDataFlowGraph dataFlowGraph, JIPipeGraphRunGCGraph gcGraph, boolean skipWorkload, JIPipeProgressInfo progressInfo) {
        if (flowGraphNode instanceof JIPipeInputDataSlot) {
            progressInfo.log("+I " + ((JIPipeInputDataSlot) flowGraphNode).getDisplayName());

            // Add data from incoming edges within graph
            JIPipeInputDataSlot inputSlot = (JIPipeInputDataSlot) flowGraphNode;
            Set<JIPipeGraphEdge> incomingEdges = graph.getGraph().incomingEdgesOf(inputSlot);
            for (JIPipeGraphEdge graphEdge : incomingEdges) {

                JIPipeDataSlot outputSlot = graph.getGraph().getEdgeSource(graphEdge);

                if(!skipWorkload) {
                    if (!inputSlot.isSkipDataGathering()) {
                        inputSlot.addDataFromTable(outputSlot, progressInfo.resolve(outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName()));
                    } else {
                        progressInfo.log("NOT copying " + outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName() + " [data gathering disabled]");
                    }
                }

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


            if(!skipWorkload) {
                if (!tryLoadFromCache(algorithm, algorithmProgress)) {
                    try {
                        if (!algorithm.isSkipped() && algorithm.isEnabled() && algorithm.getInfo().isRunnable()) {
                            algorithm.run(runContext, algorithmProgress);
                        } else {
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
        if (project != null && configuration.isStoreToCache()) {
            if (!configuration.getDisableStoreToCacheNodes().contains(outputDataSlot.getNode().getUUIDInParentGraph()) && !outputDataSlot.isSkipCache()) {
                storageProgress.log("Storing " + outputDataSlot.getDisplayName() + " into cache");
                project.getCache().store(outputDataSlot.getNode(), outputDataSlot.getNode().getUUIDInParentGraph(), outputDataSlot, outputDataSlot.getName(), storageProgress);
            } else {
                storageProgress.log("NOT storing " + outputDataSlot.getDisplayName() + " [deactivated in config]");
            }
        }
        if (configuration.isStoreToDisk() && !configuration.getDisableSaveToDiskNodes().contains(outputDataSlot.getNode().getUUIDInParentGraph()) && !outputDataSlot.isSkipExport()) {
            JIPipeProgressInfo saveProgress = storageProgress.resolveAndLog(String.format("Saving data in slot '%s' (data type %s)", outputDataSlot.getDisplayName(), JIPipeDataInfo.getInstance(outputDataSlot.getAcceptedDataType()).getName()));
            storageProgress.log("Flushing output slot " + outputDataSlot.getDisplayName());
            outputDataSlot.flush(saveProgress);
        }
    }
}
