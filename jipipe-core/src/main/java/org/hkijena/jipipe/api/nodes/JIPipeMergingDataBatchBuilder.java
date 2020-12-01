package org.hkijena.jipipe.api.nodes;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeRegistryIssues;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.extensions.parameters.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.settings.ExtensionSettings;
import org.hkijena.jipipe.extensions.strings.StringData;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that generates a {@link JIPipeMergingDataBatch} or {@link JIPipeDataBatch} instance.
 */
public class JIPipeMergingDataBatchBuilder {

    private static final Set<String> REFERENCE_COLUMN_MERGE_ALL = Sets.newHashSet("{{}}MERGE_ALL");
    private static final Set<String> REFERENCE_COLUMN_SPLIT_ALL = Sets.newHashSet("{{}}SPLIT_ALL");

    private JIPipeGraphNode node;
    private List<JIPipeDataSlot> slotList = new ArrayList<>();
    private Map<String, JIPipeDataSlot> slots = new HashMap<>();
    private Set<String> referenceColumns = new HashSet<>();
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.Merge;
    private boolean applyMerging = true;

    public JIPipeMergingDataBatchBuilder() {

    }

    public List<JIPipeDataSlot> getSlots() {
        return Collections.unmodifiableList(new ArrayList<>(slots.values()));
    }

    public void setSlots(List<JIPipeDataSlot> slots) {
        this.slots.clear();
        this.slotList.clear();
        this.slotList.addAll(slots);
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
     *
     * @param referenceColumns the reference columns
     */
    public void setReferenceColumns(Set<String> referenceColumns) {
        this.referenceColumns = referenceColumns;
    }

    public void setReferenceColumns(JIPipeColumnGrouping columnGrouping, StringQueryExpression customColumns) {
        if (slots.isEmpty())
            System.err.println("Warning: Trying to calculate reference columns with empty slot list!");
        switch (columnGrouping) {
            case Custom:
                referenceColumns = getInputAnnotationByFilter(customColumns);
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
                referenceColumns = REFERENCE_COLUMN_MERGE_ALL;
                break;
            case SplitAll:
                referenceColumns = REFERENCE_COLUMN_SPLIT_ALL;
                break;
            default:
                throw new UnsupportedOperationException("Unknown column matching strategy: " + columnGrouping);
        }
    }

    public Set<String> getInputAnnotationByFilter(StringQueryExpression expression) {
        Set<String> result = new HashSet<>();
        for (JIPipeDataSlot slot : slots.values()) {
            result.addAll(slot.getAnnotationColumns());
        }
        return new HashSet<>(expression.queryAll(result));
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

        // Special case: Merge all
        if (getReferenceColumns() == REFERENCE_COLUMN_MERGE_ALL) {
            JIPipeMergingDataBatch batch = new JIPipeMergingDataBatch(this.node);
            for (JIPipeDataSlot slot : slotList) {
                for (int row = 0; row < slot.getRowCount(); row++) {
                    batch.addData(slot, row);
                    batch.addGlobalAnnotations(slot.getAnnotations(row), getAnnotationMergeStrategy());
                }
            }
            return Arrays.asList(batch);
        }
        if (getReferenceColumns() == REFERENCE_COLUMN_SPLIT_ALL) {
            List<JIPipeMergingDataBatch> split = new ArrayList<>();
            for (JIPipeDataSlot slot : slotList) {
                for (int row = 0; row < slot.getRowCount(); row++) {
                    JIPipeMergingDataBatch batch = new JIPipeMergingDataBatch(this.node);
                    batch.addData(slot, row);
                    batch.addGlobalAnnotations(slot.getAnnotations(row), getAnnotationMergeStrategy());
                    split.add(batch);
                }
            }
            return split;
        }

        DefaultDirectedGraph<RowNode, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Create one node per row
        Multimap<JIPipeDataSlot, RowNode> rowNodesBySlot = HashMultimap.create();
        for (JIPipeDataSlot slot : slotList) {
            for (int row = 0; row < slot.getRowCount(); row++) {
                Map<String, String> annotations = new HashMap<>();
                if (referenceColumns != null) {
                    for (String column : referenceColumns) {
                        JIPipeAnnotation annotation = slot.getAnnotationOr(row, column, null);
                        if (annotation != null) {
                            annotations.put(annotation.getName(), annotation.getValue());
                        }
                    }
                } else {
                    annotations.put("uid", slot.getName() + "/" + row);
                }
                RowNode rowNode = new RowNode(slot, row, annotations);
                if (!applyMerging)
                    graph.addVertex(rowNode);
                rowNodesBySlot.put(slot, rowNode);
            }
            if (applyMerging) {
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
        for (int layer = 1; layer < slotList.size(); layer++) {
            JIPipeDataSlot previousSlot = slotList.get(layer - 1);
            JIPipeDataSlot currentSlot = slotList.get(layer);
            for (RowNode previousNode : rowNodesBySlot.get(previousSlot)) {
                for (RowNode currentNode : rowNodesBySlot.get(currentSlot)) {
                    if (previousNode.isCompatibleTo(currentNode)) {
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
        for (RowNode rowNode : rowNodesBySlot.get(slotList.get(0))) {
            graph.addEdge(source, rowNode);
        }
        for (RowNode rowNode : rowNodesBySlot.get(slotList.get(slotList.size() - 1))) {
            graph.addEdge(rowNode, sink);
        }

        // Add orphaned connections to source/sink
        for (int i = 0; i < slotList.size(); i++) {

            // Check source orphans
            if (i != 0) {
                ShortestPathAlgorithm<RowNode, DefaultEdge> shortestPathAlgorithm = new DijkstraShortestPath<>(graph);
                Set<RowNode> orphans = new HashSet<>();
                for (RowNode rowNode : rowNodesBySlot.get(slotList.get(i))) {
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


        List<JIPipeMergingDataBatch> result = new ArrayList<>();
        AllDirectedPaths<RowNode, DefaultEdge> directedPaths = new AllDirectedPaths<>(graph);
        for (GraphPath<RowNode, DefaultEdge> path : directedPaths.getAllPaths(source, sink, false, Integer.MAX_VALUE)) {
            JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this.node);
            for (RowNode rowNode : path.getVertexList()) {
                if (rowNode == source || rowNode == sink)
                    continue;
                dataBatch.addData(rowNode.slot, rowNode.rows);
                for (Integer row : rowNode.rows) {
                    dataBatch.addGlobalAnnotations(rowNode.slot.getAnnotations(row), annotationMergeStrategy);
                }

//                dataBatch.addGlobalAnnotations(rowNode.annotations, annotationMergeStrategy);
            }
            result.add(dataBatch);
        }

        // Ensure that all slots are covered
        for (JIPipeMergingDataBatch dataBatch : result) {
            for (JIPipeDataSlot slot : slotList) {
                dataBatch.getInputSlotRows().putIfAbsent(slot, Collections.emptySet());
            }
        }


        return result;
    }

    public boolean isApplyMerging() {
        return applyMerging;
    }

    public void setApplyMerging(boolean applyMerging) {
        this.applyMerging = applyMerging;
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

    public static void main(String[] args) {
        ImageJ imageJ = new ImageJ();
        JIPipe jiPipe = JIPipe.createInstance(imageJ.context());
        ExtensionSettings settings = new ExtensionSettings();
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        jiPipe.initialize(settings, issues);
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        JIPipeDataSlot slot1 = new JIPipeDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot1", null), null);
        slot1.addData(new StringData("A"), Arrays.asList(new JIPipeAnnotation("C1", "A"), new JIPipeAnnotation("C2", "X")), JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        slot1.addData(new StringData("B"), Arrays.asList(new JIPipeAnnotation("C1", "B"), new JIPipeAnnotation("C2", "Y")), JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        slot1.addData(new StringData("C"), Arrays.asList(new JIPipeAnnotation("C1", "C"), new JIPipeAnnotation("C3", "Z")), JIPipeAnnotationMergeStrategy.Merge, progressInfo);

        JIPipeDataSlot slot2 = new JIPipeDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot2", null), null);
        slot2.addData(new StringData("A"), Arrays.asList(new JIPipeAnnotation("C1", "A"), new JIPipeAnnotation("C2", "X")), JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        slot2.addData(new StringData("B"), Arrays.asList(new JIPipeAnnotation("C1", "B"), new JIPipeAnnotation("C2", "Y")), JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        slot2.addData(new StringData("C"), Arrays.asList(new JIPipeAnnotation("C1", "C"), new JIPipeAnnotation("C3", "Z")), JIPipeAnnotationMergeStrategy.Merge, progressInfo);

        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy.Merge);
        builder.setReferenceColumns(new HashSet<>(Arrays.asList("C1", "C2")));
        builder.setSlots(Arrays.asList(slot1, slot2));
        List<JIPipeMergingDataBatch> batches = builder.build();

        System.out.println(batches.size());
    }

    /**
     * Builds a single data batch where each slot only can have one row
     *
     * @return the list of batched or null if none can be generated
     */
    public static List<JIPipeDataBatch> convertMergingToSingleDataBatches(List<JIPipeMergingDataBatch> mergingDataBatches) {
        List<JIPipeDataBatch> result = new ArrayList<>();
        for (JIPipeMergingDataBatch batch : mergingDataBatches) {
            JIPipeDataBatch singleBatch = new JIPipeDataBatch(batch.getNode());
            for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : batch.getInputSlotRows().entrySet()) {
                if (entry.getValue().size() != 1)
                    return null;
                int targetRow = entry.getValue().iterator().next();
                singleBatch.setData(entry.getKey(), targetRow);
                singleBatch.setAnnotations(batch.getAnnotations());
            }
            result.add(singleBatch);
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

        public boolean isCompatibleTo(RowNode otherNode) {
            Set<String> annotationsToTest = new HashSet<>(annotations.keySet());
            annotationsToTest.retainAll(otherNode.annotations.keySet());
            for (String key : annotationsToTest) {
                if (!Objects.equals(annotations.get(key), otherNode.annotations.get(key)))
                    return false;
            }
            return true;
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
