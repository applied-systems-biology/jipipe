package org.hkijena.jipipe.api.nodes;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that generates a {@link JIPipeMergingDataBatch} or {@link JIPipeDataBatch} instance.
 */
public class JIPipeMergingDataBatchBuilder {
    private JIPipeGraphNode node;
    private Map<String, JIPipeDataSlot> slots = new HashMap<>();
    private Set<String> referenceColumns = new HashSet<>();
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.Merge;

    public JIPipeMergingDataBatchBuilder() {

    }

    public List<JIPipeDataSlot> getSlots() {
        return Collections.unmodifiableList(new ArrayList<>(slots.values()));
    }

    public void setSlots(List<JIPipeDataSlot> slots) {
        this.slots.clear();
        for (JIPipeDataSlot slot : slots) {
            this.slots.put(slot.getName(), slot);   
        }
    }

    public Set<String> getReferenceColumns() {
        return referenceColumns;
    }

    /**
     * Sets the reference columns
     * An empty list merges all data into one batch
     * Setting it to null splits all data into a separate batch
     * @param referenceColumns the reference columns
     */
    public void setReferenceColumns(Set<String> referenceColumns) {
        this.referenceColumns = referenceColumns;
    }
    
    public void setReferenceColumns(JIPipeColumnGrouping columnGrouping, StringPredicate.List customColumns, boolean invertCustomColumns) {
        if(slots.isEmpty())
            System.err.println("Warning: Trying to calculate reference columns with empty slot list!");
        switch (columnGrouping) {
            case Custom:
                referenceColumns = getInputAnnotationByFilter(customColumns, invertCustomColumns);
                break;
            case Union:
                referenceColumns = getInputAnnotationColumnUnion("");
                break;
            case Intersection:
                referenceColumns = getInputAnnotationColumnIntersection("");
                break;
            case PrefixHashUnion:
                referenceColumns = getInputAnnotationColumnUnion("#");
                break;
            case PrefixHashIntersection:
                referenceColumns = getInputAnnotationColumnIntersection("#");
                break;
            case MergeAll:
                referenceColumns = Collections.emptySet();
                break;
            case SplitAll:
                referenceColumns = null;
                break;
            default:
                throw new UnsupportedOperationException("Unknown column matching strategy: " + columnGrouping);
        }
    }

    public Set<String> getInputAnnotationByFilter(StringPredicate.List predicates, boolean invertCustomColumns) {
        Set<String> result = new HashSet<>();
        for (JIPipeDataSlot slot : slots.values()) {
            result.addAll(slot.getAnnotationColumns());
        }
        if (invertCustomColumns) {
            result.removeIf(s -> predicates.stream().anyMatch(p -> p.test(s)));
        } else {
            result.removeIf(s -> predicates.stream().noneMatch(p -> p.test(s)));
        }
        return result;
    }
    
    public Set<String> getInputAnnotationColumnIntersection(String prefix) {
        Set<String> result = new HashSet<>();
        for (JIPipeDataSlot inputSlot : slots.values()) {
            Set<String> filtered = inputSlot.getAnnotationColumns().stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toSet());
            if (result.isEmpty()) {
                result.addAll(filtered);
            } else {
                result.retainAll(filtered);
            }
        }
        return result;
    }

    public Set<String> getInputAnnotationColumnUnion(String prefix) {
        Set<String> result = new HashSet<>();
        for (JIPipeDataSlot inputSlot : slots.values()) {
            Set<String> filtered = inputSlot.getAnnotationColumns().stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toSet());
            result.addAll(filtered);
        }
        return result;
    }

    public List<JIPipeMergingDataBatch> build() {
        DefaultDirectedGraph<AnnotationNode, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Pin slots into an order
        ArrayList<JIPipeDataSlot> slotList = new ArrayList<>(slots.values());

        /*
        Phase 1: Populate the graph with all known annotation values
         */
        Set<String> annotationKeys = new HashSet<>();
        Multimap<String, AnnotationNode> nodeByKey = HashMultimap.create();
        Map<String, Map<String, AnnotationNode>> nodeByKeyValue = new HashMap<>();
        for (JIPipeDataSlot slot : slotList) {
            for (int row = 0; row < slot.getRowCount(); row++) {
                if (referenceColumns != null) {
                    for (JIPipeAnnotation annotation : slot.getAnnotations(row)) {
                        if (!referenceColumns.contains(annotation.getName()))
                            continue;
                        AnnotationNode node = new AnnotationNode(annotation);
                        annotationKeys.add(annotation.getName());
                        if (!graph.vertexSet().contains(node)) {
                            graph.addVertex(node);
                            nodeByKey.put(annotation.getName(), node);
                            Map<String, AnnotationNode> map = nodeByKeyValue.getOrDefault(annotation.getName(), null);
                            if(map == null) {
                                map = new HashMap<>();
                                nodeByKeyValue.put(annotation.getName(), map);
                            }
                            map.put(annotation.getValue(), node);
                        }
                    }
                }
                else {
                    // Special case: Assign unique Id to the row
                    String uid = slot.getName() + "/" + row;
                    JIPipeAnnotation annotation = new JIPipeAnnotation("uid", uid);
                    AnnotationNode node = new AnnotationNode(annotation);
                    annotationKeys.add(annotation.getName());
                    if (!graph.vertexSet().contains(node)) {
                        graph.addVertex(node);
                        nodeByKey.put(annotation.getName(), node);
                        Map<String, AnnotationNode> map = nodeByKeyValue.getOrDefault(annotation.getName(), null);
                        if(map == null) {
                            map = new HashMap<>();
                            nodeByKeyValue.put(annotation.getName(), map);
                        }
                        map.put(annotation.getValue(), node);
                    }
                }
            }
        }

        if(annotationKeys.isEmpty()) {
            // No annotations available -> Merge everything into one batch
            JIPipeMergingDataBatch batch = new JIPipeMergingDataBatch(node);
            for (JIPipeDataSlot slot : slotList) {
                for (int row = 0; row < slot.getRowCount(); row++) {
                    batch.addData(slot, row);
                    batch.addGlobalAnnotations(slot.getAnnotations(row), annotationMergeStrategy);
                }
            }
            return Arrays.asList(batch);
        }

        // Pin annotation keys into an order
        List<String> annotationKeyList = new ArrayList<>(annotationKeys);

        /*
        Phase 2: Create source & sink, connect layers
         */
        AnnotationNode source = new AnnotationNode(new JIPipeAnnotation("", "source"));
        AnnotationNode sink = new AnnotationNode(new JIPipeAnnotation("", "sink"));
        graph.addVertex(source);
        graph.addVertex(sink);

        for (int layer = 0; layer < annotationKeyList.size(); layer++) {
            String key = annotationKeyList.get(layer);
            // Connect to source/sink
            if (layer == 0) {
                for (AnnotationNode annotationNode : nodeByKey.get(key)) {
                    graph.addEdge(source, annotationNode);
                }
            }
            if (layer == annotationKeyList.size() - 1) {
                for (AnnotationNode annotationNode : nodeByKey.get(key)) {
                    graph.addEdge(annotationNode, sink);
                }
            }
            // Connect to previous layer
            if(layer != 0) {
                String previousKey = annotationKeyList.get(layer - 1);
                for (AnnotationNode current : nodeByKey.get(key)) {
                    for (AnnotationNode previous : nodeByKey.get(previousKey)) {
                        graph.addEdge(previous, current);
                    }
                }
            }
        }

        /*
        Phase 3: Assign slots & rows to the nodes
         */
        for (JIPipeDataSlot slot : slotList) {
            for (int row = 0; row < slot.getRowCount(); row++) {
                for (String key : annotationKeyList) {
                    JIPipeAnnotation annotation = slot.getAnnotationOr(row, key, null);
                    if(annotation != null) {
                        AnnotationNode node = nodeByKeyValue.get(key).get(annotation.getValue());
                        node.addSlotRow(slot, row);
                    }
                    else {
                        // Applies to all entries of "key"
                        for (AnnotationNode node : nodeByKey.get(key)) {
                            node.addSlotRow(slot, row);
                        }
                    }
                }
            }
        }

//        DOTExporter<AnnotationNode, DefaultEdge> dotExporter = new DOTExporter<>();
//        dotExporter.setVertexAttributeProvider(node -> {
//            Map<String, Attribute> attributeMap = new HashMap<>();
//            attributeMap.put("label", new DefaultAttribute<>(node.toString(), AttributeType.STRING));
//            return attributeMap;
//        });
//        dotExporter.exportGraph(graph, new File("flowgraph.dot"));

        /*
        Phase 4: Iterate through all paths from source to sink and build final data sets
        Each path corresponds to a data batch
         */
        List<JIPipeMergingDataBatch> result = new ArrayList<>();

        AllDirectedPaths<AnnotationNode, DefaultEdge> paths = new AllDirectedPaths<>(graph);
        for (GraphPath<AnnotationNode, DefaultEdge> path : paths.getAllPaths(source, sink, false, Integer.MAX_VALUE)) {
            JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this.node);
            for (AnnotationNode annotationNode : path.getVertexList()) {
                if(annotationNode == source || annotationNode == sink)
                    continue;
                for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : annotationNode.dataBatch.getInputSlotRows().entrySet()) {
                    dataBatch.addData(entry.getKey(), entry.getValue());
                    dataBatch.addGlobalAnnotations(entry.getKey().getAnnotations(entry.getValue()), annotationMergeStrategy);
                }
            }
            result.add(dataBatch);
        }

        return result;
    }

    /**
     * Builds a single data batch where each slot only can have one row
     * @return the list of batched or null if none can be generated
     */
    public static List<JIPipeDataBatch> convertMergingToSingleDataBatches(List<JIPipeMergingDataBatch> mergingDataBatches) {
        List<JIPipeDataBatch> result = new ArrayList<>();
        for (JIPipeMergingDataBatch batch : mergingDataBatches) {
            JIPipeDataBatch singleBatch = new JIPipeDataBatch(batch.getNode());
            for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : batch.getInputSlotRows().entrySet()) {
                if(entry.getValue().size() != 1)
                    return null;
                int targetRow = entry.getValue().iterator().next();
                singleBatch.setData(entry.getKey(), targetRow);
                singleBatch.setAnnotations(batch.getAnnotations());
            }
            result.add(singleBatch);
        }
        return result;
    }

    public JIPipeGraphNode getNode() {
        return node;
    }

    public void setNode(JIPipeGraphNode node) {
        this.node = node;
    }

    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    private static class AnnotationNode {
        private final JIPipeAnnotation annotation;
        private final JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch((JIPipeGraphNode)null);

        private AnnotationNode(JIPipeAnnotation annotation) {
            this.annotation = annotation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnnotationNode that = (AnnotationNode) o;
            return annotation.equals(that.annotation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(annotation);
        }

        public void addSlotRow(JIPipeDataSlot slot, int row) {
            dataBatch.addData(slot, row);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(annotation.getName()).append("=").append(annotation.getValue()).append("\n");
            for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : dataBatch.getInputSlotRows().entrySet()) {
                builder.append(entry.getKey().getName()).append(": ").append(entry.getValue().stream().map(r -> "" + r).collect(Collectors.joining(",")));
                builder.append("\n");
            }
            return builder.toString();
        }
    }
}
