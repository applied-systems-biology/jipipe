package org.hkijena.jipipe.api.nodes;

import com.google.common.collect.ImmutableList;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;

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
        // Old algorithm
        Map<JIPipeDataBatchKey, Map<String, TIntSet>> dataSets = new HashMap<>();
        for (JIPipeDataSlot slot : slots.values()) {
            for (int row = 0; row < slot.getRowCount(); row++) {
                JIPipeDataBatchKey key = new JIPipeDataBatchKey();
                if(referenceColumns != null) {
                    for (String referenceTraitColumn : referenceColumns) {
                        key.getEntries().put(referenceTraitColumn, null);
                    }
                    for (JIPipeAnnotation annotation : slot.getAnnotations(row)) {
                        if (annotation != null && referenceColumns.contains(annotation.getName())) {
                            key.getEntries().put(annotation.getName(), annotation);
                        }
                    }
                }
                else {
                    key.getEntries().put("#uid", new JIPipeAnnotation("#uid", slot.getName() + "[" + row + "]"));
                }
                Map<String, TIntSet> dataSet = dataSets.getOrDefault(key, null);
                if (dataSet == null) {
                    dataSet = new HashMap<>();
                    dataSets.put(key, dataSet);
                }
                TIntSet rows = dataSet.getOrDefault(slot.getName(), null);
                if (rows == null) {
                    rows = new TIntHashSet();
                    dataSet.put(slot.getName(), rows);
                }
                rows.add(row);
            }
        }
        List<JIPipeMergingDataBatch> result = new ArrayList<>();
        for (Map.Entry<JIPipeDataBatchKey, Map<String, TIntSet>> dataSetEntry : ImmutableList.copyOf(dataSets.entrySet())) {

            JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(node);
            for (Map.Entry<String, TIntSet> dataSlotEntry : dataSetEntry.getValue().entrySet()) {
                JIPipeDataSlot inputSlot = slots.get(dataSlotEntry.getKey());
                TIntSet rows = dataSetEntry.getValue().get(inputSlot.getName());
                for (TIntIterator it = rows.iterator(); it.hasNext(); ) {
                    int row = it.next();
                    dataBatch.addData(inputSlot, row);
                    dataBatch.addGlobalAnnotations(inputSlot.getAnnotations(row), annotationMergeStrategy);
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
}
