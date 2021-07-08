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
import org.hkijena.jipipe.api.data.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
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
    private JIPipeDataAnnotationMergeStrategy dataAnnotationMergeStrategy = JIPipeDataAnnotationMergeStrategy.MergeTables;
    private boolean applyMerging = true;
    private JIPipeAnnotationMatchingMethod annotationMatchingMethod = JIPipeAnnotationMatchingMethod.ExactMatch;
    private DefaultExpressionParameter customAnnotationMatching = new DefaultExpressionParameter("exact_match_results");

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

    public void setReferenceColumns(JIPipeColumMatching columnGrouping, StringQueryExpression customColumns) {
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
            case None:
                referenceColumns = Collections.emptySet();
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
        return new HashSet<>(expression.queryAll(result, new ExpressionParameters()));
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

    public List<JIPipeMergingDataBatch> build(JIPipeProgressInfo progressInfo) {

        // Special case: Merge all
        if (getReferenceColumns() == REFERENCE_COLUMN_MERGE_ALL) {
            return applyMergeAllSolver(progressInfo.resolveAndLog("Merge into one batch"));
        }
        // Special case: Split all
        if (getReferenceColumns() == REFERENCE_COLUMN_SPLIT_ALL) {
            return applySplitAllSolver(progressInfo.resolveAndLog("Split into batches"));
        }

        if (referenceColumns.size() == 1 && annotationMatchingMethod == JIPipeAnnotationMatchingMethod.ExactMatch) {
            return applyDictionarySolver(progressInfo.resolveAndLog("Dictionary solver"));
        }

        // No easy solution: Use flow graph solver
        return applyFlowGraphSolver(progressInfo.resolveAndLog("Flow graph solver"));
    }

    /**
     * The dictionary solver expects exactly one reference column and uses a dictionary for managing this structure
     * It cannot handle custom equality
     *
     * @param progressInfo the progress info
     * @return data batches
     */
    private List<JIPipeMergingDataBatch> applyDictionarySolver(JIPipeProgressInfo progressInfo) {
        final String annotationKey = referenceColumns.iterator().next();
        Set<String> allKeys = new HashSet<>();
        Map<JIPipeDataSlot, Multimap<String, Integer>> matchedRows = new HashMap<>();

        // Find groups of rows
        progressInfo.log("Grouping rows");
        for (JIPipeDataSlot slot : slotList) {
            progressInfo.resolve("Grouping rows").log("Slot " + slot.getName());
            Multimap<String, Integer> slotMap = HashMultimap.create();
            for (int row = 0; row < slot.getRowCount(); row++) {
                JIPipeAnnotation annotation = slot.getAnnotationOr(row, annotationKey, null);
                String value = annotation != null ? annotation.getValue() : "";
                slotMap.put(value, row);
                allKeys.add(value);
            }
            matchedRows.put(slot, slotMap);
        }
        if (progressInfo.isCancelled().get())
            return null;

        // Apply "empty" to all other keys if we have any other ones
        if (allKeys.contains("") && allKeys.size() > 1) {
            for (Map.Entry<JIPipeDataSlot, Multimap<String, Integer>> entry : matchedRows.entrySet()) {

                // Add empty into all other ones
                Collection<Integer> emptyRows = new HashSet<>(entry.getValue().get(""));

                // Remove the "empty" key
                entry.getValue().removeAll("");

                // Copy
                for (String key : entry.getValue().keySet()) {
                    entry.getValue().putAll(key, emptyRows);
                }
            }
            allKeys.remove("");
        }

        List<JIPipeMergingDataBatch> dataBatches = new ArrayList<>();
        if (applyMerging) {
            // We are done. Directly convert to data batches
            for (String key : allKeys) {
                JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(node);
                for (JIPipeDataSlot slot : slotList) {
                    Multimap<String, Integer> slotMap = matchedRows.get(slot);
                    dataBatch.addInputData(slot, slotMap.get(key));
                    dataBatch.addGlobalAnnotations(slot.getAnnotations(slotMap.get(key)), annotationMergeStrategy);
                    dataBatch.addGlobalDataAnnotations(slot.getDataAnnotations(slotMap.get(key)), getDataAnnotationMergeStrategy());
                }
                dataBatches.add(dataBatch);
            }
        } else {
            // Split apart into single batches
            progressInfo.log("Splitting batches");
            for (String key : allKeys) {
                List<JIPipeMergingDataBatch> keyBatches = new ArrayList<>();
                List<JIPipeMergingDataBatch> tempKeyBatches = new ArrayList<>();
                for (JIPipeDataSlot slot : slotList) {
                    Collection<Integer> rows = matchedRows.get(slot).get(key);
                    if (keyBatches.isEmpty()) {
                        // No data batches -> Create initial batch set
                        for (Integer row : rows) {
                            JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(node);
                            dataBatch.addInputData(slot, row);
                            tempKeyBatches.add(dataBatch);
                        }
                    } else {
                        // Add the row into copies of all key batches
                        if (!rows.isEmpty()) {
                            for (Integer row : rows) {
                                for (JIPipeMergingDataBatch dataBatch : keyBatches) {
                                    JIPipeMergingDataBatch copy = new JIPipeMergingDataBatch(dataBatch);
                                    copy.addInputData(slot, row);
                                    tempKeyBatches.add(copy);
                                }
                            }
                        } else {
                            tempKeyBatches.addAll(keyBatches);
                        }
                    }
                    keyBatches.clear();
                    keyBatches.addAll(tempKeyBatches);
                    tempKeyBatches.clear();
                }
                dataBatches.addAll(keyBatches);
            }
            // Resolve annotations
            progressInfo.log("Resolving annotations");
            for (JIPipeMergingDataBatch dataBatch : dataBatches) {
                List<JIPipeAnnotation> annotations = new ArrayList<>();
                List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();
                for (JIPipeDataSlot slot : slotList) {
                    annotations.addAll(slot.getAnnotations(dataBatch.getInputRows(slot)));
                    dataAnnotations.addAll(slot.getDataAnnotations(dataBatch.getInputRows(slot)));
                }
                dataBatch.addGlobalAnnotations(annotations, getAnnotationMergeStrategy());
                dataBatch.addGlobalDataAnnotations(dataAnnotations, getDataAnnotationMergeStrategy());
            }
        }
        // Ensure that all slots are known to the data batch builder
        for (JIPipeMergingDataBatch dataBatch : dataBatches) {
            for (JIPipeDataSlot slot : slotList) {
                dataBatch.addEmptySlot(slot);
            }
        }
        return dataBatches;
    }

    /**
     * Special case solver that splits all data batches
     *
     * @param progressInfo the progress info
     * @return data batches
     */
    private List<JIPipeMergingDataBatch> applySplitAllSolver(JIPipeProgressInfo progressInfo) {
        List<JIPipeMergingDataBatch> split = new ArrayList<>();
        for (JIPipeDataSlot slot : slotList) {
            for (int row = 0; row < slot.getRowCount(); row++) {
                if (row % 1000 == 0) {
                    progressInfo.resolveAndLog("Row", row, slot.getRowCount());
                    if (progressInfo.isCancelled().get())
                        return null;
                }
                JIPipeMergingDataBatch batch = new JIPipeMergingDataBatch(this.node);
                for (JIPipeDataSlot slot2 : slotList) {
                    batch.addEmptySlot(slot2);
                }
                batch.addInputData(slot, row);
                batch.addGlobalAnnotations(slot.getAnnotations(row), getAnnotationMergeStrategy());
                batch.addGlobalDataAnnotations(slot.getDataAnnotations(row), getDataAnnotationMergeStrategy());
                split.add(batch);
            }
        }
        return split;
    }

    /**
     * Special case solver that merges all data into one batch
     *
     * @param progressInfo the progress info
     * @return data batches
     */
    private List<JIPipeMergingDataBatch> applyMergeAllSolver(JIPipeProgressInfo progressInfo) {
        JIPipeMergingDataBatch batch = new JIPipeMergingDataBatch(this.node);
        for (JIPipeDataSlot slot : slotList) {
            batch.addEmptySlot(slot);
            List<JIPipeAnnotation> annotations = new ArrayList<>();
            List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();
            for (int row = 0; row < slot.getRowCount(); row++) {
                if (row % 1000 == 0) {
                    progressInfo.resolveAndLog("Row", row, slot.getRowCount());
                    if (progressInfo.isCancelled().get())
                        return null;
                }
                batch.addInputData(slot, row);
                annotations.addAll(slot.getAnnotations(row));
                dataAnnotations.addAll(slot.getDataAnnotations(row));
            }
            progressInfo.log("Merging " + annotations.size() + " annotations");
            batch.addGlobalAnnotations(annotations, getAnnotationMergeStrategy());
            batch.addGlobalDataAnnotations(dataAnnotations, getDataAnnotationMergeStrategy());
        }
        return Arrays.asList(batch);
    }

    private List<JIPipeMergingDataBatch> applyFlowGraphSolver(JIPipeProgressInfo progressInfo) {
        DefaultDirectedGraph<RowNode, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Create one node per row
        Multimap<JIPipeDataSlot, RowNode> rowNodesBySlot = HashMultimap.create();
        for (JIPipeDataSlot slot : slotList) {

            progressInfo.resolve("Creating nodes").log("Slot " + slot.getName());

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

            // Special case: Empty optional slot. Here we must create some dummy data
            if (slot.getRowCount() == 0 && slot.getInfo().isOptional()) {
                Map<String, String> annotations = new HashMap<>();
                if (referenceColumns == null)
                    annotations.put("\nuid", slot.getName() + "/-1");
                RowNode rowNode = new RowNode(slot, -1, annotations);
                if (!applyMerging)
                    graph.addVertex(rowNode);
                rowNodesBySlot.put(slot, rowNode);
            }

            if (progressInfo.isCancelled().get())
                return null;
            if (applyMerging) {
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
        if (progressInfo.isCancelled().get())
            return null;
        progressInfo.log("Connecting compatible layers");
        for (int layer = 1; layer < slotList.size(); layer++) {
            JIPipeDataSlot previousSlot = slotList.get(layer - 1);
            JIPipeDataSlot currentSlot = slotList.get(layer);
            for (RowNode previousNode : rowNodesBySlot.get(previousSlot)) {
                for (RowNode currentNode : rowNodesBySlot.get(currentSlot)) {
                    if (previousNode.isCompatibleTo(currentNode, getAnnotationMatchingMethod(), getCustomAnnotationMatching())) {
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
        if (progressInfo.isCancelled().get())
            return null;
        progressInfo.log("Inserting trivial connections");
        for (RowNode rowNode : rowNodesBySlot.get(slotList.get(0))) {
            graph.addEdge(source, rowNode);
        }
        for (RowNode rowNode : rowNodesBySlot.get(slotList.get(slotList.size() - 1))) {
            graph.addEdge(rowNode, sink);
        }

        // Add orphaned connections to source/sink
        if (progressInfo.isCancelled().get())
            return null;
        progressInfo.log("Connecting orphaned nodes");
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


        if (progressInfo.isCancelled().get())
            return null;
        progressInfo.log("Getting all paths");
        List<JIPipeMergingDataBatch> result = new ArrayList<>();
        AllDirectedPaths<RowNode, DefaultEdge> directedPaths = new AllDirectedPaths<>(graph);
        List<GraphPath<RowNode, DefaultEdge>> allPaths = directedPaths.getAllPaths(source, sink, false, Integer.MAX_VALUE);
        progressInfo.log("Found " + allPaths.size() + " paths");

        if (progressInfo.isCancelled().get())
            return null;

        progressInfo.log("Generating data batches");
        for (GraphPath<RowNode, DefaultEdge> path : allPaths) {
            JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this.node);
            for (RowNode rowNode : path.getVertexList()) {
                if (rowNode == source || rowNode == sink)
                    continue;
                if (rowNode.rows.contains(-1))
                    continue;
                dataBatch.addInputData(rowNode.slot, rowNode.rows);
                for (Integer row : rowNode.rows) {
                    dataBatch.addGlobalAnnotations(rowNode.slot.getAnnotations(row), annotationMergeStrategy);
                    dataBatch.addGlobalDataAnnotations(rowNode.slot.getDataAnnotations(row), dataAnnotationMergeStrategy);
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

    public JIPipeDataAnnotationMergeStrategy getDataAnnotationMergeStrategy() {
        return dataAnnotationMergeStrategy;
    }

    public void setDataAnnotationMergeStrategy(JIPipeDataAnnotationMergeStrategy dataAnnotationMergeStrategy) {
        this.dataAnnotationMergeStrategy = dataAnnotationMergeStrategy;
    }

    public JIPipeAnnotationMatchingMethod getAnnotationMatchingMethod() {
        return annotationMatchingMethod;
    }

    public void setAnnotationMatchingMethod(JIPipeAnnotationMatchingMethod annotationMatchingMethod) {
        this.annotationMatchingMethod = annotationMatchingMethod;
    }

    public DefaultExpressionParameter getCustomAnnotationMatching() {
        return customAnnotationMatching;
    }

    public void setCustomAnnotationMatching(DefaultExpressionParameter customAnnotationMatching) {
        this.customAnnotationMatching = customAnnotationMatching;
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
        List<JIPipeMergingDataBatch> batches = builder.build(new JIPipeProgressInfo());

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
                singleBatch.setInputData(entry.getKey(), targetRow);
                singleBatch.setGlobalAnnotations(batch.getGlobalAnnotations());
                singleBatch.setGlobalDataAnnotations(batch.getGlobalDataAnnotations());
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

        public boolean isCompatibleTo(RowNode otherNode, JIPipeAnnotationMatchingMethod annotationMatchingMethod, DefaultExpressionParameter customAnnotationMatching) {
            boolean exactMatchResults;
            {
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
            if (annotationMatchingMethod == JIPipeAnnotationMatchingMethod.ExactMatch) {
                return exactMatchResults;
            } else {
                ExpressionParameters expressionParameters = new ExpressionParameters();
                expressionParameters.put("annotations", annotations);
                expressionParameters.put("other_annotations", otherNode.annotations);
                expressionParameters.put("exact_match_results", exactMatchResults);
                return customAnnotationMatching.test(expressionParameters);
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
