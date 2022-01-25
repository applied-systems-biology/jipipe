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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;

import java.util.*;

/**
 * Wraps a set of input and output slots that belong together
 * This can be used for convenience
 */
public class JIPipeDataBatch implements Comparable<JIPipeDataBatch> {
    private JIPipeGraphNode node;
    private Map<JIPipeDataSlot, Integer> inputSlotRows;
    private Map<String, JIPipeTextAnnotation> mergedAnnotations = new HashMap<>();
    private Map<String, JIPipeDataAnnotation> mergedDataAnnotations = new HashMap<>();

    /**
     * Creates a new interface
     *
     * @param node The algorithm
     */
    public JIPipeDataBatch(JIPipeGraphNode node) {
        this.node = node;
        this.inputSlotRows = new HashMap<>();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public JIPipeDataBatch(JIPipeDataBatch other) {
        this.node = other.node;
        this.inputSlotRows = new HashMap<>(other.inputSlotRows);
        this.mergedAnnotations = new HashMap<>(other.mergedAnnotations);
        this.mergedDataAnnotations = new HashMap<>(other.mergedDataAnnotations);
    }

    public JIPipeGraphNode getNode() {
        return node;
    }

    /**
     * Gets the original annotations of given slot
     *
     * @param slot the slot
     * @return the annotations
     */
    public List<JIPipeTextAnnotation> getOriginalAnnotations(JIPipeDataSlot slot) {
        return slot.getAnnotations(inputSlotRows.get(slot));
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
     * Raw access to all data stored in the  batch
     *
     * @return map from data slot to row index
     */
    public Map<JIPipeDataSlot, Integer> getInputSlotRows() {
        return inputSlotRows;
    }

    /**
     * Sets the data row of a given slot. This should not be called after the interface was generated
     *
     * @param slot the data slot
     * @param row  the row
     */
    public void setInputData(JIPipeDataSlot slot, int row) {
        inputSlotRows.put(slot, row);
    }

    /**
     * Adds annotations to the merged annotation storage of this interface.
     * Merged annotations are passed to all output slots.
     *
     * @param annotations the annotations
     * @param strategy    strategy to apply on merging existing values
     */
    public void addMergedAnnotations(Collection<JIPipeTextAnnotation> annotations, JIPipeTextAnnotationMergeMode strategy) {
        for (JIPipeTextAnnotation annotation : annotations) {
            if (annotation != null) {
                JIPipeTextAnnotation existing = this.mergedAnnotations.getOrDefault(annotation.getName(), null);
                if (existing == null) {
                    this.mergedAnnotations.put(annotation.getName(), annotation);
                } else {
                    String newValue = strategy.merge(existing.getValue(), annotation.getValue());
                    this.mergedAnnotations.put(annotation.getName(), new JIPipeTextAnnotation(annotation.getName(), newValue));
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
     * @param progressInfo storage progress
     * @return Input data with provided name
     */
    public <T extends JIPipeData> T getInputData(String slotName, Class<T> dataClass, JIPipeProgressInfo progressInfo) {
        return getInputData(node.getInputSlot(slotName), dataClass, progressInfo);
    }

    /**
     * Gets stored data from an input slot
     *
     * @param <T>          Data type
     * @param slot         The slot
     * @param dataClass    The data type that should be returned
     * @param progressInfo storage progress
     * @return Input data with provided name
     */
    public <T extends JIPipeData> T getInputData(JIPipeDataSlot slot, Class<T> dataClass, JIPipeProgressInfo progressInfo) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isInput())
            throw new IllegalArgumentException("Slot is not an input slot!");
        return slot.getData(inputSlotRows.get(slot), dataClass, progressInfo);
    }

    /**
     * Gets the list of annotations.
     * The map is mutable.
     *
     * @return map from annotation name to annotation value
     */
    public Map<String, JIPipeTextAnnotation> getMergedAnnotations() {
        return mergedAnnotations;
    }

    public void setMergedAnnotations(Map<String, JIPipeTextAnnotation> annotations) {
        this.mergedAnnotations = annotations;
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
    public void addMergedAnnotation(JIPipeTextAnnotation annotation, JIPipeTextAnnotationMergeMode strategy) {
        JIPipeTextAnnotation existing = this.mergedAnnotations.getOrDefault(annotation.getName(), null);
        if (existing == null) {
            this.mergedAnnotations.put(annotation.getName(), annotation);
        } else {
            String newValue = strategy.merge(existing.getValue(), annotation.getValue());
            this.mergedAnnotations.put(annotation.getName(), new JIPipeTextAnnotation(annotation.getName(), newValue));
        }
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
     * Removes an annotation of provided type
     *
     * @param name removed annotation
     */
    public void removeMergedAnnotation(String name) {
        mergedAnnotations.remove(name);
    }

    /**
     * Removes a data annotation
     *
     * @param name the name
     */
    public void removeMergedDataAnnotation(String name) {
        mergedDataAnnotations.remove(name);
    }

    /**
     * Returns a merged annotation
     *
     * @param name name of the annotation
     * @return the annotation instance or null if there is no such annotation
     */
    public JIPipeTextAnnotation getMergedAnnotation(String name) {
        return mergedAnnotations.getOrDefault(name, null);
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
     * @param progressInfo storage progress
     */
    public void addOutputData(String slotName, JIPipeData data, JIPipeProgressInfo progressInfo) {
        addOutputData(node.getOutputSlot(slotName), data, progressInfo);
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
    public void addOutputData(String slotName, JIPipeData data, List<JIPipeTextAnnotation> additionalAnnotations, JIPipeTextAnnotationMergeMode mergeStrategy, JIPipeProgressInfo progressInfo) {
        addOutputData(node.getOutputSlot(slotName), data, additionalAnnotations, mergeStrategy, progressInfo);
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
     * @param slot         Slot instance
     * @param data         Added data
     * @param progressInfo storage progress
     */
    public void addOutputData(JIPipeDataSlot slot, JIPipeData data, JIPipeProgressInfo progressInfo) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        slot.addData(data, new ArrayList<>(mergedAnnotations.values()), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        for (Map.Entry<String, JIPipeDataAnnotation> entry : mergedDataAnnotations.entrySet()) {
            slot.setVirtualDataAnnotation(slot.getRowCount() - 1, entry.getKey(), entry.getValue().getVirtualData());
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
     * @param progressInfo          storage progress
     */
    public void addOutputData(JIPipeDataSlot slot, JIPipeData data, List<JIPipeTextAnnotation> additionalAnnotations, JIPipeTextAnnotationMergeMode mergeStrategy, JIPipeProgressInfo progressInfo) {
        addOutputData(slot, data, additionalAnnotations, mergeStrategy, Collections.emptyList(), JIPipeDataAnnotationMergeMode.OverwriteExisting, progressInfo);
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
        List<JIPipeTextAnnotation> finalAnnotations = new ArrayList<>(mergedAnnotations.values());
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
            slot.setVirtualDataAnnotation(slot.getRowCount() - 1, name, dataAnnotationMergeStrategy.merge(localDataAnnotations.get(name)).iterator().next().getVirtualData());
        }
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
        JIPipeDataSlot dummy = new JIPipeDataSlot(info, node);
        ArrayList<JIPipeTextAnnotation> annotations = new ArrayList<>(getMergedAnnotations().values());
        dummy.addData(sourceSlot.getVirtualData(getInputRow(sourceSlot.getName())), annotations, JIPipeTextAnnotationMergeMode.Merge);
        for (Map.Entry<String, JIPipeDataAnnotation> entry : mergedDataAnnotations.entrySet()) {
            dummy.setVirtualDataAnnotation(dummy.getRowCount() - 1, entry.getKey(), entry.getValue().getVirtualData());
        }
        return dummy;
    }

    /**
     * Returns the row of the data in the original data slot
     *
     * @param slot the slot name
     * @return the row or -1 if no data is present
     */
    public int getInputRow(String slot) {
        for (Map.Entry<JIPipeDataSlot, Integer> entry : getInputSlotRows().entrySet()) {
            if (slot.equals(entry.getKey().getName())) {
                return entry.getValue();
            }
        }
        return -1;
    }

    /**
     * Returns the row of the data in the original data slot
     *
     * @param slot the slot
     * @return the row or -1 if no data is present
     */
    public int getInputRow(JIPipeDataSlot slot) {
        return getInputSlotRows().getOrDefault(slot, -1);
    }

    @Override
    public int compareTo(JIPipeDataBatch o) {
        Set<String> annotationKeySet = new HashSet<>(mergedAnnotations.keySet());
        annotationKeySet.addAll(o.mergedAnnotations.keySet());
        List<String> annotationKeys = new ArrayList<>(annotationKeySet);
        annotationKeys.sort(Comparator.naturalOrder());
        for (String key : annotationKeys) {
            JIPipeTextAnnotation here = mergedAnnotations.getOrDefault(key, null);
            JIPipeTextAnnotation there = o.mergedAnnotations.getOrDefault(key, null);
            String hereValue = here != null ? here.getValue() : "";
            String thereValue = there != null ? there.getValue() : "";
            int c = hereValue.compareTo(thereValue);
            if (c != 0)
                return c;
        }
        return 0;
    }
}
