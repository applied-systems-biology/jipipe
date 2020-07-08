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

package org.hkijena.acaq5.ui.grapheditor.layout;

import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQNodeUI;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * MST-based auto-layout method
 */
public class MSTGraphAutoLayoutMethod implements GraphAutoLayoutMethod {

    @Override
    public void accept(ACAQGraphCanvasUI canvasUI) {
        if(canvasUI.getNodeUIs().size() <= 1)
            return;

        DefaultDirectedGraph<Node, Edge> graph = generateGraph(canvasUI, canvasUI.getNodeUIs().values());

        // Extract the sorted list of roots
        // Sorted from highest to lowest depth
        int track = generateTracks(graph);

        // Squash tracks (make them consecutive)
        squashTracks(graph, track);

//        try {
//            DOTExporter<Node, Edge> exporter = new DOTExporter<>(new IntegerComponentNameProvider<>(),
//                    new StringComponentNameProvider<>(),
//                    new StringComponentNameProvider<>());
//            exporter.exportGraph(graph, new File("tracks.dot"));
//        } catch (ExportException e) {
//            e.printStackTrace();
//        }

        if(canvasUI.getCurrentViewMode() == ACAQGraphCanvasUI.ViewMode.Vertical) {
            autoLayoutVertical(graph);
        }
        else {
            autoLayoutHorizontal(graph);
        }
    }

    private int generateTracks(DefaultDirectedGraph<Node, Edge> graph) {
        List<Node> roots = new ArrayList<>();
        for (Node node : graph.vertexSet()) {
            if(graph.incomingEdgesOf(node).isEmpty()) {
                roots.add(node);
            }
        }
        roots.sort(Comparator.comparing(Node::getDepth).reversed());

        int track = -1;
        for (Node root : roots) {
            Stack<Node> stack = new Stack<>();
            stack.push(root);

            while(!stack.isEmpty()) {
                Node top = stack.pop();
                ++track;
                top.track = track;
                Set<Edge> edges = graph.outgoingEdgesOf(top);
                while(edges.size() > 0) {
                    if (edges.size() == 1) {
                        // Iterate down until we find a branching section
                        do {
                            top = graph.getEdgeTarget(edges.iterator().next());
                            top.track = track;
                            edges = graph.outgoingEdgesOf(top);
                        }
                        while (edges.size() == 1);
                    }
                    if (edges.size() > 1) {
                        List<Node> branches = edges.stream().map(graph::getEdgeTarget).sorted(Comparator.comparing(Node::getDepth)).collect(Collectors.toList());
                        top = branches.get(branches.size() - 1);
                        top.track = track;
                        edges = graph.outgoingEdgesOf(top);
                        for (int i = 0; i < branches.size() - 1; i++) {
                            stack.push(branches.get(i));
                        }
                    }
                }
            }
        }
        return track;
    }

    private void squashTracks(DefaultDirectedGraph<Node, Edge> graph, int maxTrack) {
        Set<Integer> nonEmptyTracks = new HashSet<>();
        for (Node node : graph.vertexSet()) {
            nonEmptyTracks.add(node.track);
        }
        Map<Integer, Integer> trackRenaming = new HashMap<>();
        int trackModifier = 0;
        for (int i = 0; i <= maxTrack; i++) {
            if(nonEmptyTracks.contains(i)) {
                trackRenaming.put(i, i - trackModifier);
            }
            else {
                ++trackModifier;
                trackRenaming.put(i, i - trackModifier);
            }
        }
        for (Node node : graph.vertexSet()) {
            node.track = trackRenaming.get(node.track);
        }
    }

    private void autoLayoutHorizontal(DefaultDirectedGraph<Node, Edge> graph) {
        Map<Integer, Integer> trackHeights = new HashMap<>();
        Map<Integer, Integer> depthWidths = new HashMap<>();
        Map<Integer, Integer> cumulativeDepthWidths = new HashMap<>();
        int minTrack = Integer.MAX_VALUE;
        int maxTrack = Integer.MIN_VALUE;
        int maxDepth = 0;
        for (Node node : graph.vertexSet()) {
            minTrack = Math.min(minTrack, node.track);
            maxTrack = Math.max(maxTrack, node.track);
            maxDepth = Math.max(maxDepth, node.depth);
            trackHeights.put(node.track, Math.max(trackHeights.getOrDefault(node.track, 0), node.ui.getHeight()));
            depthWidths.put(node.depth, Math.max(depthWidths.getOrDefault(node.depth, 0), node.ui.getWidth()));
        }
        cumulativeDepthWidths.put(0, depthWidths.getOrDefault(0, 0));
        for (int depth = 1; depth <= maxDepth; depth++) {
            cumulativeDepthWidths.put(depth, cumulativeDepthWidths.get(depth - 1) + depthWidths.get(depth) + ACAQNodeUI.SLOT_UI_WIDTH * 4);
        }
        int maxCumulativeDepthWidth = 0;
        for (Integer value : cumulativeDepthWidths.values()) {
            maxCumulativeDepthWidth = Math.max(value, maxCumulativeDepthWidth);
        }


        int y = ACAQNodeUI.SLOT_UI_HEIGHT;
        for (int track = minTrack; track <= maxTrack; track++) {
            int finalTrack = track;
            for (Node node : graph.vertexSet().stream().filter(node -> node.track == finalTrack).collect(Collectors.toList())) {
                int x = maxCumulativeDepthWidth - cumulativeDepthWidths.get(node.depth);
                node.getUi().setLocation(x, y);
            }
            y += trackHeights.get(track) + ACAQNodeUI.SLOT_UI_HEIGHT;
        }
    }

    private void autoLayoutVertical(DefaultDirectedGraph<Node, Edge> graph) {
        Map<Integer, Integer> trackWidths = new HashMap<>();
        int minTrack = Integer.MAX_VALUE;
        int maxTrack = Integer.MIN_VALUE;
        int maxDepth = 0;
        for (Node node : graph.vertexSet()) {
            minTrack = Math.min(minTrack, node.track);
            maxTrack = Math.max(maxTrack, node.track);
            maxDepth = Math.max(maxDepth, node.depth);
            trackWidths.put(node.track, Math.max(trackWidths.getOrDefault(node.track, 0), node.ui.getWidth()));
        }
        int x = ACAQNodeUI.SLOT_UI_WIDTH * 4;
        for (int track = minTrack; track <= maxTrack; track++) {
            int finalTrack = track;
            for (Node node : graph.vertexSet().stream().filter(node -> node.track == finalTrack).collect(Collectors.toList())) {
                int y = (maxDepth - node.depth) * 4 * ACAQNodeUI.SLOT_UI_HEIGHT + ACAQNodeUI.SLOT_UI_HEIGHT;
                node.getUi().setLocation(x, y);
            }
            x += trackWidths.get(track) + ACAQNodeUI.SLOT_UI_WIDTH * 4;
        }
    }

    private DefaultDirectedGraph<Node, Edge> generateGraph(ACAQGraphCanvasUI canvasUI, Set<ACAQNodeUI> uis) {
        DefaultDirectedGraph<Node, Edge> graph = new DefaultDirectedGraph<>(Edge.class);
        DefaultDirectedGraph<Node, Edge> helperGraph = new DefaultDirectedGraph<>(Edge.class);
        Map<ACAQNodeUI, Node> nodeMap = new HashMap<>();
        for (ACAQNodeUI ui : uis) {
            Node node = new Node(ui);
            graph.addVertex(node);
            helperGraph.addVertex(node);
            nodeMap.put(ui, node);
        }
        for (ACAQNodeUI ui : uis) {
            for (ACAQDataSlot outputSlot : ui.getNode().getOutputSlots()) {
                for (ACAQDataSlot targetSlot : ui.getGraphUI().getGraph().getTargetSlots(outputSlot)) {
                    ACAQNodeUI targetUI =    canvasUI.getNodeUIs().getOrDefault(targetSlot.getNode(), null);
                    if(targetUI != null) {
                        Node targetNode = nodeMap.get(targetUI);
                        helperGraph.addEdge(nodeMap.get(ui), targetNode);
                        graph.addEdge(nodeMap.get(ui), targetNode);
                    }
                }
            }
        }

        // Find the node depths via the helper graph
        calculateMaximumNodeDepths(helperGraph);
        helperGraph = null;

        // Find the edge depths as min of all involved node depths
        calculateEdgeWeights(graph);
//        DOTExporter<Node, Edge> exporter = new DOTExporter<>(new IntegerComponentNameProvider<>(),
//                new StringComponentNameProvider<>(),
//                new StringComponentNameProvider<>());
//        try {
//            exporter.exportGraph(graph, new File("graph.dot"));
//        } catch (ExportException e) {
//            e.printStackTrace();
//        }

        // Find the mst
        KruskalMinimumSpanningTree<Node, Edge> minimumSpanningTree = new KruskalMinimumSpanningTree<>(graph);
        for (Edge edge : ImmutableList.copyOf(graph.edgeSet())) {
            if(!minimumSpanningTree.getSpanningTree().getEdges().contains(edge)) {
                graph.removeEdge(edge);
            }
        }

//        try {
//            exporter.exportGraph(graph, new File("mst.dot"));
//        } catch (ExportException e) {
//            e.printStackTrace();
//        }


        return graph;
    }

    private void calculateEdgeWeights(DefaultDirectedGraph<Node, Edge> graph) {
        for (Edge edge : graph.edgeSet()) {
            Node edgeSource = graph.getEdgeSource(edge);
            Node edgeTarget = graph.getEdgeTarget(edge);
            edge.depth = Math.min(edgeSource.depth, edgeTarget.depth);
        }
    }

    private void calculateMaximumNodeDepths(DefaultDirectedGraph<Node, Edge> helperGraph) {
        while(!helperGraph.vertexSet().isEmpty()) {
            Set<Node> leaves = new HashSet<>();
            for (Node node : helperGraph.vertexSet()) {
                if(helperGraph.outgoingEdgesOf(node).isEmpty()) {
                    leaves.add(node);
                }
            }
            for (Node leaf : leaves) {
                for (Edge edge : helperGraph.incomingEdgesOf(leaf)) {
                    Node edgeSource = helperGraph.getEdgeSource(edge);
                    edgeSource.depth = Math.max(edgeSource.depth, leaf.depth + 1);
                }
            }
            helperGraph.removeAllVertices(leaves);
        }
    }

    public static class Node {
        private final ACAQNodeUI ui;
        private int depth = 0;
        private int track = -1;

        public Node(ACAQNodeUI ui) {
            this.ui = ui;
        }

        public ACAQNodeUI getUi() {
            return ui;
        }

        public int getDepth() {
            return depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }

        @Override
        public String toString() {
            return String.format("%s d=%d, t=%d", ui.getNode().getName(), depth, track);
        }

        public int getTrack() {
            return track;
        }

        public void setTrack(int track) {
            this.track = track;
        }
    }

    public static class Edge extends DefaultWeightedEdge {
        private int depth = Integer.MAX_VALUE;

        public int getDepth() {
            return depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }

        @Override
        protected double getWeight() {
            return -depth;
        }

        @Override
        public String toString() {
            return "d=" + depth;
        }
    }
}
