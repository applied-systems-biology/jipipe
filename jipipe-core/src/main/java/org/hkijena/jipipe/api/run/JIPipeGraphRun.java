package org.hkijena.jipipe.api.run;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class JIPipeGraphRun extends AbstractJIPipeRunnable {
    private static final int DATA_FLOW_WEIGHT_INTERNAL = 0;
    private static final int DATA_FLOW_WEIGHT_LIGHT = 4;
    private static final int DATA_FLOW_WEIGHT_HEAVY = 1; // Algorithm should go through heavy data asap

    private final JIPipeProject project;
    private final JIPipeGraph graph;
    private final JIPipeGraphRunSettings configuration;
    private JIPipeGraphGCHelper graphGCHelper;
    private JIPipeGraphNodeRunContext runContext;

    public JIPipeGraphRun(JIPipeGraph graph, JIPipeGraphRunSettings configuration) {
        this.configuration = configuration;
        this.project = null;
        this.graph = new JIPipeGraph(graph);
    }

    public JIPipeGraphRun(JIPipeProject project, JIPipeGraphRunSettings configuration) {
        this.project = project;
        this.graph = new JIPipeGraph(project.getGraph());
        this.configuration = configuration;
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
        graphGCHelper = new JIPipeGraphGCHelper(graph);

        try (JIPipeFixedThreadPool threadPool = new JIPipeFixedThreadPool(configuration.getNumThreads())) {

            // Setup context
            runContext = new JIPipeGraphNodeRunContext();
            runContext.setGraphRun(this);
            runContext.setThreadPool(threadPool);

            // Start iteration on initial graph
            progressInfo.log("Starting first iteration ...");
            runOrPartitionGraph(graph, progressInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            graphGCHelper = null;
            runContext = null;
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

    public void runOrPartitionGraph(JIPipeGraph graph, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Iterating on graph " + graph + " ...");
        DirectedAcyclicGraph<Set<JIPipeGraphNode>, DefaultEdge> partitionGraph = calculatePartitionGraph(graph);
        progressInfo.log("-> Partition graph has " + partitionGraph.vertexSet().size() + " vertices, " + partitionGraph.edgeSet().size() + " edges");
        if (partitionGraph.vertexSet().size() == 1) {
            progressInfo.log("--> Single partition. Running graph.");
            runGraph(graph, partitionGraph.vertexSet().iterator().next(), progressInfo);
        } else if (partitionGraph.vertexSet().size() > 1) {
            progressInfo.log("--> Multiple partitions. Executing partitions in topological order.");
            TopologicalOrderIterator<Set<JIPipeGraphNode>, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(partitionGraph);
            ImmutableList<Set<JIPipeGraphNode>> partitionNodeSets = ImmutableList.copyOf(orderIterator);
            for (int i = 0; i < partitionNodeSets.size(); i++) {
                Set<JIPipeGraphNode> partitionNodeSet = partitionNodeSets.get(i);
                if (partitionNodeSet.isEmpty())
                    continue;
                JIPipeProgressInfo partitionProgress = progressInfo.resolve("Partition " + ((JIPipeAlgorithm) partitionNodeSet.iterator().next()).getRuntimePartition().getIndex());
                runGraph(graph, partitionNodeSet, partitionProgress);
            }
        } else {
            progressInfo.log("--> Nothing to do");
        }
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

    private void runGraph(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter, JIPipeProgressInfo progressInfo) {

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
                dataFlowGraph.removeVertex(bestCandidate);
                endpoints.remove(bestCandidate);
            }
        }
        finally {
            progressInfo.setWithSpinner(false);
        }
    }

    private void runFlowGraphNode(Object flowGraphNode, JIPipeProgressInfo progressInfo) {
        if(flowGraphNode instanceof JIPipeInputDataSlot) {
            // TODO: standard copy operation will clash with loops
            // Store results in local cache?
            // Maybe have Map<JIPipeDataSlot, JIPipeDataSlot>? for overrides?
            JIPipeOutputDataSlot outputSlot = (JIPipeOutputDataSlot) flowGraphNode;
            progressInfo.resolve(outputSlot.getNode().getDisplayName())
        }
        else if(flowGraphNode instanceof JIPipeOutputDataSlot) {
            JIPipeOutputDataSlot outputSlot = (JIPipeOutputDataSlot) flowGraphNode;
            progressInfo.resolve(outputSlot.getNode().getDisplayName()).resolve(outputSlot.getDisplayName()).log("Status is " + outputSlot);
        }
        else if(flowGraphNode instanceof JIPipeAlgorithm) {
            JIPipeAlgorithm algorithm = (JIPipeAlgorithm) flowGraphNode;
            JIPipeProgressInfo algorithmProgress = progressInfo.resolveAndLog(algorithm.getDisplayName());
            if(algorithm.isSkipped() || !algorithm.isEnabled() || !algorithm.getInfo().isRunnable()) {
                algorithmProgress.log("Not runnable (skipped/disabled/node info not marked as runnable). Workload will not be executed!");
                return;
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
