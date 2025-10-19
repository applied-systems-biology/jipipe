package org.hkijena.jipipe.api.nodes.iterationstep.solvers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeTextAnnotationMatchingMethod;
import org.hkijena.jipipe.api.nodes.iterationstep.IterationStepSolver;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGenerator;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;

public class FlowGraphSolver implements IterationStepSolver {
    @Override
    public List<JIPipeMultiIterationStep> solve(JIPipeMultiIterationStepGenerator generator, JIPipeProgressInfo progressInfo) {
        DefaultDirectedGraph<RowNode, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        List<JIPipeDataSlot> slotList = generator.getSlots();
        Set<String> referenceColumns = generator.getReferenceColumns();

        // Create one node per row
        Multimap<JIPipeDataSlot, RowNode> rowNodesBySlot = HashMultimap.create();
        for (JIPipeDataSlot slot : slotList) {

            progressInfo.resolve("Creating nodes").log("Slot " + slot.getName());

            for (int row = 0; row < slot.getRowCount(); row++) {
                Map<String, String> annotations = new HashMap<>();
                if (referenceColumns != null) {
                    for (String column : referenceColumns) {
                        JIPipeTextAnnotation annotation = slot.getTextAnnotationOr(row, column, null);
                        if (annotation != null) {
                            annotations.put(annotation.getName(), annotation.getValue());
                        }
                    }
                } else {
                    annotations.put("uid", slot.getName() + "/" + row);
                }
                RowNode rowNode = new RowNode(slot, row, annotations);
                if (!generator.isApplyMerging())
                    graph.addVertex(rowNode);
                rowNodesBySlot.put(slot, rowNode);
            }

            // Special case: Empty optional slot. Here we must create some dummy data
            if (slot.getRowCount() == 0 && slot.getInfo().isOptional()) {
                Map<String, String> annotations = new HashMap<>();
                if (referenceColumns == null)
                    annotations.put("\nuid", slot.getName() + "/-1");
                RowNode rowNode = new RowNode(slot, -1, annotations);
                if (!generator.isApplyMerging())
                    graph.addVertex(rowNode);
                rowNodesBySlot.put(slot, rowNode);
            }

            if (progressInfo.isCancelled())
                return null;
            if (generator.isApplyMerging()) {
                progressInfo.log("Partitioning");
                Map<Map<String, String>, List<RowNode>> partitions = rowNodesBySlot.get(slot).stream().collect(Collectors.groupingBy(RowNode::getAnnotations));
                rowNodesBySlot.removeAll(slot);
                for (Map.Entry<Map<String, String>, List<RowNode>> entry : partitions.entrySet()) {
                    Set<Integer> rows = new HashSet<>();
                    for (RowNode rowNode : entry.getValue()) {
                        rows.addAll(rowNode.rows);
                    }
                    RowNode merged = new RowNode(slot, rows, entry.getKey());
                    graph.addVertex(merged);
                    rowNodesBySlot.put(slot, merged);
                }
            }
        }

        // Connect compatible rows
        if (progressInfo.isCancelled())
            return null;
        progressInfo.log("Connecting compatible layers");
        for (int layer = 1; layer < slotList.size(); layer++) {
            JIPipeDataSlot previousSlot = slotList.get(layer - 1);
            JIPipeDataSlot currentSlot = slotList.get(layer);
            for (RowNode previousNode : rowNodesBySlot.get(previousSlot)) {
                for (RowNode currentNode : rowNodesBySlot.get(currentSlot)) {
                    if (previousNode.isCompatibleTo(currentNode, generator.getAnnotationMatchingMethod(), generator.getCustomAnnotationMatching(), generator.isForceNAIsAny())) {
                        graph.addEdge(previousNode, currentNode);
                    }
                }
            }
        }

        // Create source and sink
        RowNode source = new RowNode(null, 0, Collections.singletonMap("", "source"));
        RowNode sink = new RowNode(null, 0, Collections.singletonMap("", "sink"));
        graph.addVertex(source);
        graph.addVertex(sink);

        // Trivial connections (first and last layer)
        if (progressInfo.isCancelled())
            return null;
        progressInfo.log("Inserting trivial connections");
        for (RowNode rowNode : rowNodesBySlot.get(slotList.getFirst())) {
            graph.addEdge(source, rowNode);
        }
        for (RowNode rowNode : rowNodesBySlot.get(slotList.getLast())) {
            graph.addEdge(rowNode, sink);
        }

        // Add orphaned connections to source/sink
        if (progressInfo.isCancelled())
            return null;
        progressInfo.log("Connecting orphaned nodes");
        for (int i = 0; i < slotList.size(); i++) {
            if (progressInfo.isCancelled())
                return null;
            // Check source orphans
            if (i != 0) {
                ShortestPathAlgorithm<RowNode, DefaultEdge> shortestPathAlgorithm = new DijkstraShortestPath<>(graph);
                Set<RowNode> orphans = new HashSet<>();
                for (RowNode rowNode : rowNodesBySlot.get(slotList.get(i))) {
                    if (progressInfo.isCancelled())
                        return null;
                    if (shortestPathAlgorithm.getPath(source, rowNode) == null) {
                        orphans.add(rowNode);
                    }
                }
                shortestPathAlgorithm = null;
                for (RowNode orphan : orphans) {
                    graph.addEdge(source, orphan);
                }
            }
            // Check sink orphans
            if (i != slotList.size() - 1) {
                ShortestPathAlgorithm<RowNode, DefaultEdge> shortestPathAlgorithm = new DijkstraShortestPath<>(graph);
                Set<RowNode> orphans = new HashSet<>();
                for (RowNode rowNode : rowNodesBySlot.get(slotList.get(i))) {
                    if (progressInfo.isCancelled())
                        return null;
                    if (shortestPathAlgorithm.getPath(rowNode, sink) == null) {
                        orphans.add(rowNode);
                    }
                }
                shortestPathAlgorithm = null;
                for (RowNode orphan : orphans) {
                    graph.addEdge(orphan, sink);
                }
            }
        }


//        DOTExporter<RowNode, DefaultEdge> dotExporter = new DOTExporter<>();
//        dotExporter.setVertexAttributeProvider(node -> {
//            Map<String, Attribute> attributeMap = new HashMap<>();
//            attributeMap.put("label", new DefaultAttribute<>(node.toString(), AttributeType.STRING));
//            return attributeMap;
//        });
//        dotExporter.exportGraph(graph, new File("flowgraph.dot"));


        if (progressInfo.isCancelled())
            return null;
        progressInfo.log("Getting all paths");
        List<JIPipeMultiIterationStep> result = new ArrayList<>();
        AllDirectedPaths<RowNode, DefaultEdge> directedPaths = new AllDirectedPaths<>(graph);
        List<GraphPath<RowNode, DefaultEdge>> allPaths = directedPaths.getAllPaths(source, sink, false, Integer.MAX_VALUE);
        progressInfo.log("Found " + allPaths.size() + " paths");

        if (progressInfo.isCancelled())
            return null;

        progressInfo.log("Generating iteration steps");
        for (GraphPath<RowNode, DefaultEdge> path : allPaths) {
            JIPipeMultiIterationStep iterationStep = new JIPipeMultiIterationStep(generator.getNode());
            for (RowNode rowNode : path.getVertexList()) {
                if (rowNode == source || rowNode == sink)
                    continue;
                if (rowNode.rows.contains(-1))
                    continue;
                iterationStep.addInputData(rowNode.slot, rowNode.rows);
                for (Integer row : rowNode.rows) {
                    iterationStep.addMergedTextAnnotations(rowNode.slot.getTextAnnotations(row), generator.getAnnotationMergeStrategy());
                    iterationStep.addMergedDataAnnotations(rowNode.slot.getDataAnnotations(row), generator.getDataAnnotationMergeStrategy());
                }

//                iterationStep.addGlobalAnnotations(rowNode.annotations, annotationMergeStrategy);
            }
            result.add(iterationStep);
        }

        // Ensure that all slots are covered
        for (JIPipeMultiIterationStep iterationStep : result) {
            for (JIPipeDataSlot slot : slotList) {
                iterationStep.getInputSlotRows().putIfAbsent(slot, Collections.emptySet());
            }
        }

        return result;
    }

    /**
     * A node that represents one row in a {@link JIPipeDataSlot}
     */
    private static class RowNode {
        private final JIPipeDataSlot slot;
        private final Set<Integer> rows;
        private final Map<String, String> annotations;

        public RowNode(JIPipeDataSlot slot, int row, Map<String, String> annotations) {
            this.slot = slot;
            this.rows = new HashSet<>(Collections.singleton(row));
            this.annotations = annotations;
        }

        public RowNode(JIPipeDataSlot slot, Set<Integer> rows, Map<String, String> annotations) {
            this.slot = slot;
            this.rows = rows;
            this.annotations = annotations;
        }

        public boolean isCompatibleTo(RowNode otherNode, JIPipeTextAnnotationMatchingMethod annotationMatchingMethod, JIPipeExpressionParameter customAnnotationMatching, boolean forceNAIsAny) {
            boolean exactMatchResults;
            if (forceNAIsAny && annotations.containsKey("")) {
                exactMatchResults = true;
            } else {
                Set<String> annotationsToTest = new HashSet<>(annotations.keySet());
                annotationsToTest.retainAll(otherNode.annotations.keySet());
                exactMatchResults = true;
                for (String key : annotationsToTest) {
                    if (!Objects.equals(annotations.get(key), otherNode.annotations.get(key))) {
                        exactMatchResults = false;
                        break;
                    }
                }
            }
            if (annotationMatchingMethod == JIPipeTextAnnotationMatchingMethod.ExactMatch) {
                return exactMatchResults;
            } else {
                JIPipeExpressionVariablesMap expressionVariables = new JIPipeExpressionVariablesMap();
                expressionVariables.put("annotations", annotations);
                expressionVariables.put("other_annotations", otherNode.annotations);
                expressionVariables.put("exact_match_results", exactMatchResults);
                return customAnnotationMatching.test(expressionVariables);
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (slot != null)
                builder.append(slot.getName()).append(" / ").append(rows.stream().map(s -> "" + s).collect(Collectors.joining(","))).append("\n");
            for (Map.Entry<String, String> entry : annotations.entrySet()) {
                builder.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            return builder.toString();
        }

        public Map<String, String> getAnnotations() {
            return annotations;
        }
    }
}
