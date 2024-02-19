package org.hkijena.jipipe.api.run;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.utils.StringUtils;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class JIPipeGraphRun extends AbstractJIPipeRunnable {
    public static final int DATA_FLOW_WEIGHT_INTERNAL = 0;
    public static final int DATA_FLOW_WEIGHT_LIGHT = 4;
    public static final int DATA_FLOW_WEIGHT_HEAVY = 1; // Algorithm should go through heavy data asap

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
        progressInfo.log("Erasing skipped/disabled algorithms ...");
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
            runOrPartitionGraph(graph, progressInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
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
        for (JIPipeGraphNode graphNode : ImmutableList.copyOf(graph.getGraphNodes())) {
            if (graphNode instanceof JIPipeAlgorithm) {
                if (!((JIPipeAlgorithm) graphNode).isEnabled() || ((JIPipeAlgorithm) graphNode).isSkipped()) {
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

    public void runOrPartitionGraph(JIPipeGraph graph, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Iterating on graph " + graph + " ...");
        DirectedAcyclicGraph<Set<JIPipeGraphNode>, DefaultEdge> partitionGraph = calculatePartitionGraph(graph);
        progressInfo.log("-> Partition graph has " + partitionGraph.vertexSet().size() + " vertices, " + partitionGraph.edgeSet().size() + " edges");

        DefaultDirectedWeightedGraph<Object, DefaultWeightedEdge> fullDataFlowGraph = buildDataFlowGraph(graph, graph.getGraphNodes());
        progressInfo.log("-> Full data flow graph has " + fullDataFlowGraph.vertexSet().size() + " vertices, " + fullDataFlowGraph.edgeSet().size() + " edges");

        if (partitionGraph.vertexSet().size() == 1) {
            progressInfo.log("--> Single partition. Running graph.");
            runPartitionNodeSet(graph, partitionGraph.vertexSet().iterator().next(), fullDataFlowGraph, progressInfo);
        } else if (partitionGraph.vertexSet().size() > 1) {
            progressInfo.log("--> Multiple partitions. Executing partitions in topological order.");
            TopologicalOrderIterator<Set<JIPipeGraphNode>, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(partitionGraph);
            ImmutableList<Set<JIPipeGraphNode>> partitionNodeSets = ImmutableList.copyOf(orderIterator);
            for (int i = 0; i < partitionNodeSets.size(); i++) {
                Set<JIPipeGraphNode> partitionNodeSet = partitionNodeSets.get(i);
                if (partitionNodeSet.isEmpty())
                    continue;
                runPartitionNodeSet(graph, partitionNodeSet, fullDataFlowGraph, progressInfo);
            }
        } else {
            progressInfo.log("--> Nothing to do");
        }
    }

    private void runPartitionNodeSet(JIPipeGraph graph, Set<JIPipeGraphNode> partitionNodeSet, DefaultDirectedWeightedGraph<Object, DefaultWeightedEdge> fullDataFlowGraph, JIPipeProgressInfo progressInfo) {
        int partitionId = 0;
        for (JIPipeGraphNode graphNode : partitionNodeSet) {
            if(graphNode instanceof JIPipeAlgorithm) {
                partitionId = ((JIPipeAlgorithm) graphNode).getRuntimePartition().getIndex();
            }
        }
        partitionId = Math.max(0, Math.min(partitionId, runtimePartitions.size() - 1));
        JIPipeRuntimePartition runtimePartition = runtimePartitions.get(partitionId);
        progressInfo.log("--> Selected runtime partition id=" + partitionId + " name=" + runtimePartition.getName());

        // Find the interfacing outputs (outputs that should not be cleared away by the GC)
        Set<JIPipeDataSlot> interfacingSlots = new HashSet<>();
        for (JIPipeGraphEdge graphEdge : graph.getGraph().edgeSet()) {
            JIPipeDataSlot edgeSource = graph.getGraph().getEdgeSource(graphEdge);
            JIPipeDataSlot edgeTarget = graph.getGraph().getEdgeTarget(graphEdge);
            if(edgeSource.getNode() instanceof JIPipeAlgorithm && edgeTarget.getNode() instanceof JIPipeAlgorithm) {
                if(((JIPipeAlgorithm) edgeSource.getNode()).getRuntimePartition().getIndex() == partitionId && ((JIPipeAlgorithm) edgeTarget.getNode()).getRuntimePartition().getIndex() != partitionId) {
                    interfacingSlots.add(edgeSource);
                    progressInfo.log("--> Marked output " + edgeSource.getDisplayName() + " as interfacing");
                }
            }
        }

        JIPipeProgressInfo partitionProgress = progressInfo.resolve("Partition " + partitionId);
        runGraph(graph, partitionNodeSet, fullDataFlowGraph, interfacingSlots, partitionProgress);
    }

    private DefaultDirectedWeightedGraph<Object, DefaultWeightedEdge> buildDataFlowGraph(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter) {
        DefaultDirectedWeightedGraph<Object, DefaultWeightedEdge> flowGraph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for (JIPipeGraphNode graphNode : nodeFilter) {
            flowGraph.addVertex(graphNode);
            for (JIPipeInputDataSlot inputSlot : graphNode.getInputSlots()) {
                flowGraph.addVertex(inputSlot);
            }
            for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
                flowGraph.addVertex(outputSlot);
            }
        }
        for (JIPipeGraphNode graphNode : nodeFilter) {
            for (JIPipeInputDataSlot inputSlot : graphNode.getInputSlots()) {
                // Edge to the node (weight 0)
                flowGraph.setEdgeWeight(flowGraph.addEdge(inputSlot, graphNode), DATA_FLOW_WEIGHT_INTERNAL);

                // Inputs
                for (JIPipeGraphEdge graphEdge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                    int weight;
                    if(JIPipeDataInfo.getInstance(inputSlot.getAcceptedDataType()).isHeavy()) {
                        weight = DATA_FLOW_WEIGHT_HEAVY;
                    }
                    else {
                        weight = DATA_FLOW_WEIGHT_LIGHT;
                    }
                    JIPipeDataSlot edgeSource = graph.getGraph().getEdgeSource(graphEdge);
                    if(flowGraph.containsVertex(edgeSource)) {
                        flowGraph.setEdgeWeight(flowGraph.addEdge(edgeSource, inputSlot), weight);
                    }
                }
            }
            for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
                // Edge from the node (weight by data)
                int weight;
                if(JIPipeDataInfo.getInstance(outputSlot.getAcceptedDataType()).isHeavy()) {
                    weight = DATA_FLOW_WEIGHT_HEAVY;
                }
                else {
                    weight = DATA_FLOW_WEIGHT_LIGHT;
                }
                flowGraph.setEdgeWeight(flowGraph.addEdge(graphNode, outputSlot), weight);
            }
        }
        return flowGraph;
    }

    private void runGraph(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter, DefaultDirectedWeightedGraph<Object, DefaultWeightedEdge> fullDataFlowGraph, Set<JIPipeDataSlot> interfacingSlots, JIPipeProgressInfo progressInfo) {

        // A copy of the graph that is destroyed
        DefaultDirectedWeightedGraph<Object, DefaultWeightedEdge> dataFlowGraph = buildDataFlowGraph(graph, nodeFilter);
        progressInfo.log("Built data flow graph with " + dataFlowGraph.vertexSet().size() + " vertices, " + dataFlowGraph.edgeSet().size() + " edges");

        // Find endpoints
        Set<Object> endpoints = dataFlowGraph.vertexSet().stream().filter(vertex -> dataFlowGraph.outDegreeOf(vertex) == 0).collect(Collectors.toSet());

        int progress = 0;
        final int maxProgress = dataFlowGraph.vertexSet().size();
        progressInfo.setProgress(progress, maxProgress);

        try {
            progressInfo.setWithSpinner(true);

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

                // Find the candidates
                List<Object> candidates = new ArrayList<>();
                for (Object vertex : dataFlowGraph.vertexSet()) {
                    if (dataFlowGraph.inDegreeOf(vertex) == 0) {
                        candidates.add(vertex);
                    }
                }

                // Find the best candidate (shortest path)
                DijkstraShortestPath<Object, DefaultWeightedEdge> dijkstraShortestPath = new DijkstraShortestPath<>(dataFlowGraph);
                Object bestCandidate = null;
                double bestWeight = Double.POSITIVE_INFINITY;

                for (Object candidate : candidates) {
                    for (Object endpoint : endpoints) {
                        GraphPath<Object, DefaultWeightedEdge> path = dijkstraShortestPath.getPath(candidate, endpoint);
                        double weight = path.getWeight();
                        if (weight < bestWeight) {
                            bestCandidate = candidate;
                        }
                    }
                }

                if (bestCandidate == null) {
                    throw new NullPointerException("No candidate found! Are there cycles in the graph?");
                }

                // Execute operation
                runFlowGraphNode(bestCandidate, progressInfo);

                // Delete
                cleanupFlowGraphNode(bestCandidate, dataFlowGraph, fullDataFlowGraph, interfacingSlots, endpoints, progressInfo);
            }
        }
        finally {
            progressInfo.setWithSpinner(false);
        }
    }

    private void cleanupFlowGraphNode(Object flowGraphNode, DefaultDirectedWeightedGraph<Object, DefaultWeightedEdge> dataFlowGraph, DefaultDirectedWeightedGraph<Object, DefaultWeightedEdge> fullDataFlowGraph, Set<JIPipeDataSlot> interfacingSlots, Set<Object> endpoints, JIPipeProgressInfo progressInfo) {

        if(flowGraphNode instanceof JIPipeAlgorithm) {
            // Clear inputs
            for (JIPipeInputDataSlot inputSlot : ((JIPipeAlgorithm) flowGraphNode).getInputSlots()) {
                progressInfo.resolve("GC").log("Clear input '" + inputSlot.getName() + "' of node '" + inputSlot.getNode().getDisplayName() + "'");
                inputSlot.clear();
            }

            // Remove from flow graph
            dataFlowGraph.removeVertex(flowGraphNode);
            endpoints.remove(flowGraphNode);
        }
        else if(flowGraphNode instanceof JIPipeInputDataSlot) {
            // Remove from flow graph
            dataFlowGraph.removeVertex(flowGraphNode);
            endpoints.remove(flowGraphNode);
        }

        // Remove from full flow graph & GC (handling of orphan outputs)
        if(fullDataFlowGraph != null) {
            if(flowGraphNode instanceof JIPipeDataSlot && !interfacingSlots.contains(flowGraphNode)) {
                // Apply GC here
                runGC(fullDataFlowGraph, interfacingSlots, false, progressInfo);
            }
            fullDataFlowGraph.removeVertex(flowGraphNode);
        }
        else {
            // Local GC only
            runGC(dataFlowGraph, interfacingSlots, true, progressInfo);
        }


    }

    private void runGC(DefaultDirectedWeightedGraph<Object, DefaultWeightedEdge> dataFlowGraph, Set<JIPipeDataSlot> interfacingSlots, boolean discard, JIPipeProgressInfo progressInfo) {
        for (Object vertex : dataFlowGraph.vertexSet()) {
            if(vertex instanceof JIPipeOutputDataSlot) {
                if(dataFlowGraph.degreeOf(vertex) == 0) {
                    progressInfo.log("Marked as unused: " + ((JIPipeOutputDataSlot) vertex).getDisplayName());
                }
            }
        }

    }

    private void runFlowGraphNode(Object flowGraphNode, JIPipeProgressInfo progressInfo) {
        if(flowGraphNode instanceof JIPipeInputDataSlot) {
            // Add data from incoming edges within graph
            JIPipeInputDataSlot inputSlot = (JIPipeInputDataSlot) flowGraphNode;
            for (JIPipeGraphEdge graphEdge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                JIPipeDataSlot outputSlot = graph.getGraph().getEdgeSource(graphEdge);
                inputSlot.addDataFromTable(outputSlot, progressInfo);
            }
        }
        else if(flowGraphNode instanceof JIPipeOutputDataSlot) {
            JIPipeOutputDataSlot outputSlot = (JIPipeOutputDataSlot) flowGraphNode;
            progressInfo.resolve(outputSlot.getNode().getDisplayName()).log("Contents of data slot " + outputSlot);
        }
        else if(flowGraphNode instanceof JIPipeAlgorithm) {
            JIPipeAlgorithm algorithm = (JIPipeAlgorithm) flowGraphNode;
            JIPipeProgressInfo algorithmProgress = progressInfo.resolve(algorithm.getDisplayName());
            algorithmProgress.log("Executing " + algorithm.getUUIDInParentGraph());
            if(algorithm.isSkipped() || !algorithm.isEnabled() || !algorithm.getInfo().isRunnable()) {
                algorithmProgress.log("Not runnable (skipped/disabled/node info not marked as runnable). Workload will not be executed!");
                return;
            }

            try {
                algorithm.run(runContext, algorithmProgress);
            } catch (Exception e) {
                throw new JIPipeValidationRuntimeException(
                        new GraphNodeValidationReportContext(algorithm),
                        e,
                        "An error occurred during processing",
                        "On running the algorithm '" + algorithm.getDisplayName(),
                        "Please follow the instructions for the other error messages.");
            }
        }
        else {
            progressInfo.log("[!] Unsupported flow graph node type: " + flowGraphNode);
        }
    }


    private DirectedAcyclicGraph<Set<JIPipeGraphNode>, DefaultEdge> calculatePartitionGraph(JIPipeGraph graph) {
        JIPipeGraph copy = new JIPipeGraph(graph);

        // Delete all inter-partition edges
        for (JIPipeGraphEdge edge : ImmutableList.copyOf(copy.getGraph().edgeSet())) {
            JIPipeGraphNode source = copy.getGraph().getEdgeSource(edge).getNode();
            JIPipeGraphNode target = copy.getGraph().getEdgeTarget(edge).getNode();
            if (source instanceof JIPipeAlgorithm && target instanceof JIPipeAlgorithm) {
                if (!Objects.equals(((JIPipeAlgorithm) source).getRuntimePartition(), ((JIPipeAlgorithm) target).getRuntimePartition())) {
                    copy.getGraph().removeEdge(edge);
                }
            }
        }

        // Find components
        DirectedAcyclicGraph<Set<JIPipeGraphNode>, DefaultEdge> result = new DirectedAcyclicGraph<>(DefaultEdge.class);
        ConnectivityInspector<JIPipeDataSlot, JIPipeGraphEdge> inspector = new ConnectivityInspector<>(copy.getGraph());

        // Create vertices (contains nodes from the original graph)
        Map<JIPipeGraphNode, Set<JIPipeGraphNode>> components = new HashMap<>();
        for (Set<JIPipeDataSlot> slots : inspector.connectedSets()) {
            Set<JIPipeGraphNode> nodes = slots.stream().map(JIPipeDataSlot::getNode).map(graph::getEquivalentNode).collect(Collectors.toSet());
            result.addVertex(nodes);
            for (JIPipeGraphNode node : nodes) {
                components.put(graph.getEquivalentNode(node), nodes);
            }
        }

        // Create edges
        for (Set<JIPipeGraphNode> nodes : result.vertexSet()) {
            for (JIPipeGraphNode node : nodes) {
                if (node instanceof JIPipeAlgorithm) {
                    for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                        for (JIPipeGraphEdge edge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                            JIPipeDataSlot source = graph.getGraph().getEdgeSource(edge);
                            if (source.getNode() instanceof JIPipeAlgorithm) {
                                if (!Objects.equals(((JIPipeAlgorithm) source.getNode()).getRuntimePartition(),
                                        ((JIPipeAlgorithm) node).getRuntimePartition())) {
                                    result.addEdge(components.get(source.getNode()), nodes);
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private void initializeGraph(JIPipeGraph graph) {
        graph.setAttachments(graph.getAttachments());
        graph.attach(this);
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }
}
