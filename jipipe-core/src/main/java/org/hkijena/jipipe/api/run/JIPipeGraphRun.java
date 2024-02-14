package org.hkijena.jipipe.api.run;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class JIPipeGraphRun extends AbstractJIPipeRunnable {

    private final JIPipeProject project;
    private final JIPipeGraph graph;
    private final JIPipeGraphRunSettings configuration;

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
//        setProgressInfo(getProgressInfo().resolve("〈⚙〉"));
        getProgressInfo().log("+++ JIPipe Graph Runner next +++");
        getProgressInfo().log("Starting first iteration ...");

        JIPipeGraphNodeRunContext runContext = new JIPipeGraphNodeRunContext();

        runOrPartitionGraph(graph, runContext, getProgressInfo());
    }

    private void runOrPartitionGraph(JIPipeGraph graph, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Iterating on graph " + graph + " ...");
        DirectedAcyclicGraph<Set<JIPipeGraphNode>, DefaultEdge> partitionGraph = calculatePartitionGraph(graph);
        progressInfo.log("-> Partition graph has " + partitionGraph.vertexSet().size() + " vertices, " + partitionGraph.edgeSet().size() + " edges");
        if(partitionGraph.vertexSet().size() == 1) {
            progressInfo.log("--> Single partition. Running graph.");

        }
        else if(partitionGraph.vertexSet().size() > 1) {
            progressInfo.log("--> Multiple partitions. Executing partitions in topological order.");
        }
        else {
            progressInfo.log("--> Nothing to do");
        }
    }

    private DirectedAcyclicGraph<Set<JIPipeGraphNode>, DefaultEdge> calculatePartitionGraph(JIPipeGraph graph) {
        JIPipeGraph copy = new JIPipeGraph(graph);

        // Delete all inter-partition edges
        for (JIPipeGraphEdge edge : ImmutableList.copyOf(copy.getGraph().edgeSet())) {
            JIPipeGraphNode source = copy.getGraph().getEdgeSource(edge).getNode();
            JIPipeGraphNode target = copy.getGraph().getEdgeTarget(edge).getNode();
            if(source instanceof JIPipeAlgorithm && target instanceof JIPipeAlgorithm) {
                if(!Objects.equals(((JIPipeAlgorithm) source).getRuntimePartition(), ((JIPipeAlgorithm) target).getRuntimePartition())) {
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
                if(node instanceof JIPipeAlgorithm) {
                    for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                        for (JIPipeGraphEdge edge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                            JIPipeDataSlot source = graph.getGraph().getEdgeSource(edge);
                            if(source.getNode() instanceof JIPipeAlgorithm) {
                                if(!Objects.equals(((JIPipeAlgorithm) source.getNode()).getRuntimePartition(),
                                        ((JIPipeAlgorithm) node).getRuntimePartition())) {
                                    result.addEdge(nodes, components.get(source.getNode()));
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
