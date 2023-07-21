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

package org.hkijena.jipipe.api.nodes;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;

import java.util.*;

/**
 * Wraps a set of input and output slots that belong together.
 * This is a less restricted variant of {@link JIPipeDataBatch} used by {@link JIPipeMergingAlgorithm}
 */
public class JIPipeMergingDataBatch implements Comparable<JIPipeMergingDataBatch> {
    private final JIPipeGraphNode node;
    private final Map<JIPipeDataSlot, Set<Integer>> inputSlotRows;
    private Map<String, JIPipeTextAnnotation> mergedTextAnnotations = new HashMap<>();
    private Map<String, JIPipeDataAnnotation> mergedDataAnnotations = new HashMap<>();

    /**
     * Creates a new interface
     *
     * @param node The algorithm
     */
    public JIPipeMergingDataBatch(JIPipeGraphNode node) {
        this.node = node;
        this.inputSlotRows = new HashMap<>();
//        initialize(inputSlots, row);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public JIPipeMergingDataBatch(JIPipeMergingDataBatch other) {
        this.node = other.node;
        this.inputSlotRows = new HashMap<>(other.inputSlotRows);
        this.mergedTextAnnotations = new HashMap<>(other.mergedTextAnnotations);
        this.mergedDataAnnotations = new HashMap<>(other.mergedDataAnnotations);
    }

    public JIPipeGraphNode getNode() {
        return node;
    }

    /**
     * Raw access to all data stored in the  batch
     *
     * @return map from data slot to set of row indices
     */
    public Map<JIPipeDataSlot, Set<Integer>> getInputSlotRows() {
        return inputSlotRows;
    }

    /**
     * Adds the data row of a given slot. This should not be called after the interface was generated
     *
     * @param slot the data slot
     * @param row  the row
     */
    public void addInputData(JIPipeDataSlot slot, int row) {
        Set<Integer> rows = inputSlotRows.getOrDefault(slot, null);
        if (rows == null) {
            rows = new HashSet<>();
            inputSlotRows.put(slot, rows);
        }
        rows.add(row);
    }

    /**
     * Adds the data row of a given slot. This should not be called after the interface was generated
     *
     * @param slot      the data slot
     * @param rowsToAdd the rows to add
     */
    public void addInputData(JIPipeDataSlot slot, Collection<Integer> rowsToAdd) {
        Set<Integer> rows = inputSlotRows.getOrDefault(slot, null);
        if (rows == null) {
            rows = new HashSet<>();
            inputSlotRows.put(slot, rows);
        }
        rows.addAll(rowsToAdd);
    }

    /**
     * Sets the input slot to only one row
     *
     * @param slot the slot
     * @param row  the row
     */
    public void setInputData(JIPipeDataSlot slot, int row) {
        Set<Integer> rows = inputSlotRows.getOrDefault(slot, null);
        if (rows == null) {
            rows = new HashSet<>();
            inputSlotRows.put(slot, rows);
        }
        rows.clear();
        rows.add(row);
    }

    /**
     * Adds an annotation to the annotation list
     *
     * @param annotation added annotation. Cannot be null.
     */
    public void addMergedDataAnnotation(JIPipeDataAnnotation annotation, JIPipeDataAnnotationMergeMode strategy) {
        JIPipeDataAnnotation existing = this.mergedDataAnnotations.getOrDefault(annotation.getName(), null);
        if (existing == null) {
            this.mergedDataAnnotations.put(annotation.getName(), annotation);
        } else {
            annotation = strategy.merge(Arrays.asList(existing, annotation)).get(0);
            this.mergedDataAnnotations.put(annotation.getName(), annotation);
        }
    }

    /**
     * Adds annotations to the annotation list
     *
     * @param annotations added annotations
     */
    public void addMergedDataAnnotations(Collection<JIPipeDataAnnotation> annotations, JIPipeDataAnnotationMergeMode strategy) {
        for (JIPipeDataAnnotation annotation : annotations) {
            addMergedDataAnnotation(annotation, strategy);
        }
    }

    /**
     * Adds annotations to the merged annotation storage of this interface.
     * Merged annotations are passed to all output slots.
     *
     * @param annotations the annotations
     * @param strategy    strategy to apply on merging existing values
     */
    public void addMergedTextAnnotations(Map<String, String> annotations, JIPipeTextAnnotationMergeMode strategy) {
        for (Map.Entry<String, String> entry : annotations.entrySet()) {
            JIPipeTextAnnotation existing = this.mergedTextAnnotations.getOrDefault(entry.getKey(), null);
            if (existing == null) {
                this.mergedTextAnnotations.put(entry.getKey(), new JIPipeTextAnnotation(entry.getKey(), entry.getValue()));
            } else {
                String newValue = strategy.merge(existing.getValue(), entry.getValue());
                this.mergedTextAnnotations.put(entry.getKey(), new JIPipeTextAnnotation(entry.getKey(), newValue));
            }
        }
    }

    /**
     * Adds annotations to the merged annotation storage of this interface.
     * Merged annotations are passed to all output slots.
     *
     * @param annotations the annotations
     * @param strategy    strategy to apply on merging existing values
     */
    public void addMergedTextAnnotations(List<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode strategy) {
        for (JIPipeTextAnnotation annotation : annotations) {
            if (annotation != null) {
                JIPipeTextAnnotation existing = this.mergedTextAnnotations.getOrDefault(annotation.getName(), null);
                if (existing == null) {
                    this.mergedTextAnnotations.put(annotation.getName(), annotation);
                } else {
                    String newValue = strategy.merge(existing.getValue(), annotation.getValue());
                    this.mergedTextAnnotations.put(annotation.getName(), new JIPipeTextAnnotation(annotation.getName(), newValue));
                }
            }
        }
    }

    /**
     * Gets stored data from an input slot
     *
     * @param <T>          Data type
     * @param slotName     The slot name
     * @param dataClass    The data type that should be returned
     * @param progressInfo data access progress
     * @return Input data with provided name
     */
    public <T extends JIPipeData> List<T> getInputData(String slotName, Class<T> dataClass, JIPipeProgressInfo progressInfo) {
        return getInputData(node.getInputSlot(slotName), dataClass, progressInfo);
    }

    /**
     * Gets stored data from an input slot
     *
     * @param slotName The slot name
     * @return Input data with provided name
     */
    public List<JIPipeDataItemStore> getVirtualInputData(String slotName) {
        return getVirtualInputData(node.getInputSlot(slotName));
    }

    /**
     * Gets stored data from an input slot
     *
     * @param <T>          Data type
     * @param slot         The slot
     * @param dataClass    The data type that should be returned
     * @param progressInfo data access progress
     * @return Input data with provided name
     */
    public <T extends JIPipeData> List<T> getInputData(JIPipeDataSlot slot, Class<T> dataClass, JIPipeProgressInfo progressInfo) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isInput())
            throw new IllegalArgumentException("Slot is not an input slot!");
        List<T> result = new ArrayList<>();
        for (Integer row : inputSlotRows.getOrDefault(slot, Collections.emptySet())) {
            result.add(slot.getData(row, dataClass, progressInfo));
        }
        return result;
    }

    /**
     * Gets stored data from an input slot
     *
     * @param slot The slot
     * @return Input data with provided name
     */
    public List<JIPipeDataItemStore> getVirtualInputData(JIPipeDataSlot slot) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isInput())
            throw new IllegalArgumentException("Slot is not an input slot!");
        List<JIPipeDataItemStore> result = new ArrayList<>();
        for (Integer row : inputSlotRows.getOrDefault(slot, Collections.emptySet())) {
            result.add(slot.getDataItemStore(row));
        }
        return result;
    }

    /**
     * Returns the row indices that belong to this data interface
     *
     * @param slot slot name
     * @return the row indices that belong to this data interface
     */
    public Set<Integer> getInputRows(String slot) {
        for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : getInputSlotRows().entrySet()) {
            if (slot.equals(entry.getKey().getName())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Returns the row indices that belong to this data interface
     *
     * @param slot slot
     * @return the row indices that belong to this data interface
     */
    public Set<Integer> getInputRows(JIPipeDataSlot slot) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        return inputSlotRows.getOrDefault(slot, Collections.emptySet());
    }

    /**
     * Gets the list of annotations.
     * The map is mutable.
     *
     * @return map from annotation name to annotation value
     */
    public Map<String, JIPipeTextAnnotation> getMergedTextAnnotations() {
        return mergedTextAnnotations;
    }

    public void setMergedTextAnnotations(Map<String, JIPipeTextAnnotation> annotations) {
        this.mergedTextAnnotations = annotations;
    }

    /**
     * Gets the list of annotations.
     * The map is mutable.
     *
     * @return map from annotation name to annotation value
     */
    public Map<String, JIPipeDataAnnotation> getMergedDataAnnotations() {
        return mergedDataAnnotations;
    }

    public void setMergedDataAnnotations(Map<String, JIPipeDataAnnotation> dataAnnotations) {
        this.mergedDataAnnotations = dataAnnotations;
    }

    /**
     * Adds an annotation to the annotation list
     *
     * @param annotation added annotation. Cannot be null.
     */
    public void addMergedTextAnnotation(JIPipeTextAnnotation annotation, JIPipeTextAnnotationMergeMode strategy) {
        JIPipeTextAnnotation existing = this.mergedTextAnnotations.getOrDefault(annotation.getName(), null);
        if (existing == null) {
            this.mergedTextAnnotations.put(annotation.getName(), annotation);
        } else {
            String newValue = strategy.merge(existing.getValue(), annotation.getValue());
            this.mergedTextAnnotations.put(annotation.getName(), new JIPipeTextAnnotation(annotation.getName(), newValue));
        }
    }


    /**
     * Removes an annotation of provided type
     *
     * @param info removed annotation
     */
    public void removeMergedTextAnnotation(String info) {
        mergedTextAnnotations.remove(info);
    }

    /**
     * Returns a merged annotation
     *
     * @param name name of the annotation
     * @return the annotation instance or null if there is no such annotation
     */
    public JIPipeTextAnnotation getMergedTextAnnotation(String name) {
        return mergedTextAnnotations.getOrDefault(name, null);
    }

    /**
     * Returns a merged annotation
     *
     * @param name name of the annotation
     * @return the annotation instance or null if there is no such annotation
     */
    public JIPipeDataAnnotation getMergedDataAnnotation(String name) {
        return mergedDataAnnotations.getOrDefault(name, null);
    }


    /**
     * Writes output data into the provided slot
     * Please note that annotations should be set up till this point
     *
     * @param slotName     Slot name
     * @param data         Added data
     * @param progressInfo data storage progress
     */
    public void addOutputData(String slotName, JIPipeData data, JIPipeProgressInfo progressInfo) {
        addOutputData(node.getOutputSlot(slotName), data, progressInfo);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations should be set up till this point
     *
     * @param slotName Slot name
     * @param data     Added data
     */
    public void addOutputData(String slotName, JIPipeDataItemStore data) {
        addOutputData(node.getOutputSlot(slotName), data);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations should be set up till this point
     *
     * @param slotName              Slot name
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the merged ones
     * @param progressInfo          data storage progress
     */
    public void addOutputData(String slotName, JIPipeData data, List<JIPipeTextAnnotation> additionalAnnotations, JIPipeProgressInfo progressInfo) {
        addOutputData(node.getOutputSlot(slotName), data, additionalAnnotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations should be set up till this point
     *
     * @param slotName              Slot name
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the merged ones
     */
    public void addOutputData(String slotName, JIPipeDataItemStore data, List<JIPipeTextAnnotation> additionalAnnotations) {
        addOutputData(node.getOutputSlot(slotName), data, additionalAnnotations, JIPipeTextAnnotationMergeMode.Merge);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations should be set up till this point
     *
     * @param slotName              Slot name
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the merged ones
     * @param progressInfo          data storage progress
     */
    public void addOutputData(String slotName, JIPipeData data, List<JIPipeTextAnnotation> additionalAnnotations, JIPipeTextAnnotationMergeMode annotationMergeStrategy, JIPipeProgressInfo progressInfo) {
        addOutputData(node.getOutputSlot(slotName), data, additionalAnnotations, annotationMergeStrategy, progressInfo);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations should be set up till this point
     *
     * @param slotName              Slot name
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the merged ones
     */
    public void addOutputData(String slotName, JIPipeDataItemStore data, JIPipeTextAnnotationMergeMode annotationMergeStrategy, List<JIPipeTextAnnotation> additionalAnnotations) {
        addOutputData(node.getOutputSlot(slotName), data, additionalAnnotations, annotationMergeStrategy);
    }

    public void addOutputData(JIPipeDataSlot slot, JIPipeDataItemStore data, List<JIPipeTextAnnotation> additionalAnnotations, JIPipeTextAnnotationMergeMode annotationMergeStrategy, List<JIPipeDataAnnotation> additionalDataAnnotations, JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy) {
        addOutputData(slot, data, additionalAnnotations, annotationMergeStrategy);
        Multimap<String, JIPipeDataAnnotation> localDataAnnotations = HashMultimap.create();
        for (Map.Entry<String, JIPipeDataAnnotation> entry : mergedDataAnnotations.entrySet()) {
            localDataAnnotations.put(entry.getKey(), entry.getValue());
        }
        for (JIPipeDataAnnotation dataAnnotation : additionalDataAnnotations) {
            localDataAnnotations.put(dataAnnotation.getName(), dataAnnotation);
        }
        for (String name : localDataAnnotations.keySet()) {
            slot.setDataAnnotationItemStore(slot.getRowCount() - 1, name, dataAnnotationMergeStrategy.merge(localDataAnnotations.get(name)).iterator().next().getVirtualData());
        }
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations that are added to all annotations should be set up till this point
     *
     * @param slot         Slot instance
     * @param data         Added data
     * @param progressInfo data storage progress
     */
    public void addOutputData(JIPipeDataSlot slot, JIPipeData data, JIPipeProgressInfo progressInfo) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        slot.addData(data, new ArrayList<>(mergedTextAnnotations.values()), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        for (Map.Entry<String, JIPipeDataAnnotation> entry : mergedDataAnnotations.entrySet()) {
            slot.setDataAnnotationItemStore(slot.getRowCount() - 1, entry.getKey(), entry.getValue().getVirtualData());
        }
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations that are added to all annotations should be set up till this point
     *
     * @param slot Slot instance
     * @param data Added data
     */
    public void addOutputData(JIPipeDataSlot slot, JIPipeDataItemStore data) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        slot.addData(data, new ArrayList<>(mergedTextAnnotations.values()), JIPipeTextAnnotationMergeMode.Merge);
        for (Map.Entry<String, JIPipeDataAnnotation> entry : mergedDataAnnotations.entrySet()) {
            slot.setDataAnnotationItemStore(slot.getRowCount() - 1, entry.getKey(), entry.getValue().getVirtualData());
        }
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations should be set up till this point
     *
     * @param slotName              Slot name
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the merged ones
     * @param mergeStrategy         how annotations should be merged
     * @param progressInfo          storage progress
     */
    public void addOutputData(String slotName, JIPipeData data, List<JIPipeTextAnnotation> additionalAnnotations, JIPipeTextAnnotationMergeMode mergeStrategy, List<JIPipeDataAnnotation> additionalDataAnnotations, JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy, JIPipeProgressInfo progressInfo) {
        addOutputData(node.getOutputSlot(slotName), data, additionalAnnotations, mergeStrategy, additionalDataAnnotations, dataAnnotationMergeStrategy, progressInfo);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations that are added to all annotations should be set up till this point
     *
     * @param slot                  Slot instance
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the merged ones
     * @param mergeStrategy         how annotations should be merged
     * @param progressInfo          storage progress
     */
    public void addOutputData(JIPipeDataSlot slot, JIPipeData data, List<JIPipeTextAnnotation> additionalAnnotations, JIPipeTextAnnotationMergeMode mergeStrategy, List<JIPipeDataAnnotation> additionalDataAnnotations, JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy, JIPipeProgressInfo progressInfo) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        List<JIPipeTextAnnotation> finalAnnotations = new ArrayList<>(mergedTextAnnotations.values());
        finalAnnotations.addAll(additionalAnnotations);
        slot.addData(data, finalAnnotations, mergeStrategy, progressInfo);
        Multimap<String, JIPipeDataAnnotation> localDataAnnotations = HashMultimap.create();
        for (Map.Entry<String, JIPipeDataAnnotation> entry : mergedDataAnnotations.entrySet()) {
            localDataAnnotations.put(entry.getKey(), entry.getValue());
        }
        for (JIPipeDataAnnotation dataAnnotation : additionalDataAnnotations) {
            localDataAnnotations.put(dataAnnotation.getName(), dataAnnotation);
        }
        for (String name : localDataAnnotations.keySet()) {
            slot.setDataAnnotationItemStore(slot.getRowCount() - 1, name, dataAnnotationMergeStrategy.merge(localDataAnnotations.get(name)).iterator().next().getVirtualData());
        }
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations that are added to all annotations should be set up till this point
     *
     * @param slot                  Slot instance
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the merged ones
     * @param mergeStrategy         how annotations should be merged
     * @param progressInfo          data storage progress
     */
    public void addOutputData(JIPipeDataSlot slot, JIPipeData data, List<JIPipeTextAnnotation> additionalAnnotations, JIPipeTextAnnotationMergeMode mergeStrategy, JIPipeProgressInfo progressInfo) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        List<JIPipeTextAnnotation> finalAnnotations = new ArrayList<>(mergedTextAnnotations.values());
        finalAnnotations.addAll(additionalAnnotations);
        slot.addData(data, finalAnnotations, mergeStrategy, progressInfo);
        for (Map.Entry<String, JIPipeDataAnnotation> entry : mergedDataAnnotations.entrySet()) {
            slot.setDataAnnotationItemStore(slot.getRowCount() - 1, entry.getKey(), entry.getValue().getVirtualData());
        }
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations that are added to all annotations should be set up till this point
     *
     * @param slot                  Slot instance
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the merged ones
     * @param mergeStrategy         how annotations should be merged
     */
    public void addOutputData(JIPipeDataSlot slot, JIPipeDataItemStore data, List<JIPipeTextAnnotation> additionalAnnotations, JIPipeTextAnnotationMergeMode mergeStrategy) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        List<JIPipeTextAnnotation> finalAnnotations = new ArrayList<>(mergedTextAnnotations.values());
        finalAnnotations.addAll(additionalAnnotations);
        slot.addData(data, finalAnnotations, mergeStrategy);
        for (Map.Entry<String, JIPipeDataAnnotation> entry : mergedDataAnnotations.entrySet()) {
            slot.setDataAnnotationItemStore(slot.getRowCount() - 1, entry.getKey(), entry.getValue().getVirtualData());
        }
    }

    /**
     * Gets the original annotations of given slot
     *
     * @param slot the slot
     * @return the annotations
     */
    public List<JIPipeTextAnnotation> getOriginalAnnotations(JIPipeDataSlot slot) {
        return slot.getTextAnnotations(inputSlotRows.get(slot));
    }

    /**
     * Gets the original annotations of given slot
     *
     * @param slotName the slot
     * @return the annotations
     */
    public List<JIPipeTextAnnotation> getOriginalAnnotations(String slotName) {
        return getOriginalAnnotations(node.getInputSlot(slotName));
    }

    /**
     * Gets the original annotations of given slot
     *
     * @param slot the slot
     * @return the annotations
     */
    public List<JIPipeDataAnnotation> getOriginalDataAnnotations(JIPipeDataSlot slot) {
        return slot.getDataAnnotations(inputSlotRows.get(slot));
    }

    /**
     * Gets the original annotations of given slot
     *
     * @param slotName the slot
     * @return the annotations
     */
    public List<JIPipeDataAnnotation> getOriginalDataAnnotations(String slotName) {
        return getOriginalDataAnnotations(node.getInputSlot(slotName));
    }

    /**
     * Returns true if no data is referenced
     *
     * @return if no data is referenced
     */
    public boolean isEmpty() {
        if (inputSlotRows.isEmpty())
            return true;
        for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : inputSlotRows.entrySet()) {
            if (!entry.getValue().isEmpty())
                return false;
        }
        return true;
    }

    /**
     * Returns true if there is at least one slot that has no rows attached to it
     *
     * @return if the batch is incomplete
     */
    public boolean isIncomplete() {
        for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : inputSlotRows.entrySet()) {
            if (entry.getValue().isEmpty() && !entry.getKey().getInfo().isOptional())
                return true;
        }
        return false;
    }

    /**
     * Returns true if each slot only has one row
     *
     * @return if the batch is single
     */
    public boolean isSingle() {
        for (Set<Integer> rows : inputSlotRows.values()) {
            if (rows.size() != 1)
                return false;
        }
        return true;
    }

    /**
     * Creates a new dummy slot that contains the data of one input slot and the annotations of this batch
     *
     * @param info       info of the new slot
     * @param node       the node that will own the new slot
     * @param sourceSlot the source slot
     * @return a new dummy slot
     */
    public JIPipeDataSlot toDummySlot(JIPipeDataSlotInfo info, JIPipeGraphNode node, JIPipeDataSlot sourceSlot) {
        JIPipeDataSlot dummy = info.createInstance(node);
        ArrayList<JIPipeTextAnnotation> annotations = new ArrayList<>(getMergedTextAnnotations().values());
        for (int row : getInputRows(sourceSlot.getName())) {
            dummy.addData(sourceSlot.getDataItemStore(row), annotations, JIPipeTextAnnotationMergeMode.Merge);
            for (Map.Entry<String, JIPipeDataAnnotation> entry : mergedDataAnnotations.entrySet()) {
                dummy.setDataAnnotationItemStore(dummy.getRowCount() - 1, entry.getKey(), entry.getValue().getVirtualData());
            }
        }
        return dummy;
    }

    @Override
    public int compareTo(JIPipeMergingDataBatch o) {
        Set<String> annotationKeySet = new HashSet<>(mergedTextAnnotations.keySet());
        annotationKeySet.addAll(o.mergedTextAnnotations.keySet());
        List<String> annotationKeys = new ArrayList<>(annotationKeySet);
        annotationKeys.sort(Comparator.naturalOrder());
        for (String key : annotationKeys) {
            JIPipeTextAnnotation here = mergedTextAnnotations.getOrDefault(key, null);
            JIPipeTextAnnotation there = o.mergedTextAnnotations.getOrDefault(key, null);
            String hereValue = here != null ? here.getValue() : "";
            String thereValue = there != null ? there.getValue() : "";
            int c = hereValue.compareTo(thereValue);
            if (c != 0)
                return c;
        }
        return 0;
    }

    /**
     * Ensures that the specified slot is registered to the data batch
     *
     * @param slot the slot
     */
    public void addEmptySlot(JIPipeDataSlot slot) {
        inputSlotRows.putIfAbsent(slot, new HashSet<>());
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getName()).append(": { ");
        for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : inputSlotRows.entrySet()) {
            stringBuilder.append(entry.getKey().getName()).append(" -> [").append(Ints.join(", ", Ints.toArray(entry.getValue()))).append("], ");
        }
        stringBuilder.append(" }");
        return stringBuilder.toString();
    }
}
