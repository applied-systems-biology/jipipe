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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wraps a set of input and output slots that belong together
 * This can be used for convenience
 */
public class JIPipeDataBatch implements Comparable<JIPipeDataBatch> {
    private JIPipeGraphNode node;
    private Map<JIPipeDataSlot, Integer> inputSlotRows;
    private Map<String, JIPipeAnnotation> annotations = new HashMap<>();

    /**
     * Creates a new interface
     *
     * @param node The algorithm
     */
    public JIPipeDataBatch(JIPipeGraphNode node) {
        this.node = node;
        this.inputSlotRows = new HashMap<>();
//        initialize(inputSlots, row);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public JIPipeDataBatch(JIPipeDataBatch other) {
        this.node = other.node;
        this.inputSlotRows = new HashMap<>(other.inputSlotRows);
        this.annotations = new HashMap<>(other.annotations);
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
    public List<JIPipeAnnotation> getOriginalAnnotations(JIPipeDataSlot slot) {
        return slot.getAnnotations(inputSlotRows.get(slot));
    }

    /**
     * Gets the original annotations of given slot
     *
     * @param slotName the slot
     * @return the annotations
     */
    public List<JIPipeAnnotation> getOriginalAnnotations(String slotName) {
        return getOriginalAnnotations(node.getInputSlot(slotName));
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
     * Adds annotations to the global annotation storage of this interface.
     * Global annotations are passed to all output slots.
     *
     * @param annotations the annotations
     * @param strategy    strategy to apply on merging existing values
     */
    public void addGlobalAnnotations(List<JIPipeAnnotation> annotations, JIPipeAnnotationMergeStrategy strategy) {
        for (JIPipeAnnotation annotation : annotations) {
            if (annotation != null) {
                JIPipeAnnotation existing = this.annotations.getOrDefault(annotation.getName(), null);
                if (existing == null) {
                    this.annotations.put(annotation.getName(), annotation);
                } else {
                    String newValue = strategy.merge(existing.getValue(), annotation.getValue());
                    this.annotations.put(annotation.getName(), new JIPipeAnnotation(annotation.getName(), newValue));
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
     * @return list of annotations
     */
    public Map<String, JIPipeAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, JIPipeAnnotation> annotations) {
        this.annotations = annotations;
    }

    /**
     * Adds an annotation to the annotation list
     *
     * @param annotation added annotation. Cannot be null.
     */
    public void addGlobalAnnotation(JIPipeAnnotation annotation, JIPipeAnnotationMergeStrategy strategy) {
        JIPipeAnnotation existing = this.annotations.getOrDefault(annotation.getName(), null);
        if (existing == null) {
            this.annotations.put(annotation.getName(), annotation);
        } else {
            String newValue = strategy.merge(existing.getValue(), annotation.getValue());
            this.annotations.put(annotation.getName(), new JIPipeAnnotation(annotation.getName(), newValue));
        }
    }


    /**
     * Removes an annotation of provided type
     *
     * @param info removed annotation
     */
    public void removeGlobalAnnotation(String info) {
        annotations.remove(info);
    }

    /**
     * Returns an existing annotation
     * Only the exact type is used for searching
     *
     * @param info annotation type
     * @return null if it does not exist
     */
    public JIPipeAnnotation getAnnotationOfType(String info) {
        return annotations.getOrDefault(info, null);
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
     * @param additionalAnnotations Annotations that are added additionally to the global ones
     * @param mergeStrategy         how annotations should be merged
     * @param progressInfo          storage progress
     */
    public void addOutputData(String slotName, JIPipeData data, List<JIPipeAnnotation> additionalAnnotations, JIPipeAnnotationMergeStrategy mergeStrategy, JIPipeProgressInfo progressInfo) {
        addOutputData(node.getOutputSlot(slotName), data, additionalAnnotations, mergeStrategy, progressInfo);
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
        slot.addData(data, new ArrayList<>(annotations.values()), JIPipeAnnotationMergeStrategy.Merge, progressInfo);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations that are added to all annotations should be set up till this point
     *
     * @param slot                  Slot instance
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the global ones
     * @param mergeStrategy         how annotations should be merged
     * @param progressInfo          storage progress
     */
    public void addOutputData(JIPipeDataSlot slot, JIPipeData data, List<JIPipeAnnotation> additionalAnnotations, JIPipeAnnotationMergeStrategy mergeStrategy, JIPipeProgressInfo progressInfo) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        List<JIPipeAnnotation> finalAnnotations = new ArrayList<>(annotations.values());
        finalAnnotations.addAll(additionalAnnotations);
        slot.addData(data, finalAnnotations, mergeStrategy, progressInfo);
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
        ArrayList<JIPipeAnnotation> annotations = new ArrayList<>(getAnnotations().values());
        for (int row = 0; row < sourceSlot.getRowCount(); row++) {
            dummy.addData(sourceSlot.getVirtualData(row), annotations, JIPipeAnnotationMergeStrategy.Merge);
        }
        return dummy;
    }

    @Override
    public int compareTo(JIPipeDataBatch o) {
        Set<String> annotationKeySet = new HashSet<>(annotations.keySet());
        annotationKeySet.addAll(o.annotations.keySet());
        List<String> annotationKeys = new ArrayList<>(annotationKeySet);
        annotationKeys.sort(Comparator.naturalOrder());
        for (String key : annotationKeys) {
            JIPipeAnnotation here = annotations.getOrDefault(key, null);
            JIPipeAnnotation there = o.annotations.getOrDefault(key, null);
            String hereValue = here != null ? here.getValue() : "";
            String thereValue = there != null ? there.getValue() : "";
            int c = hereValue.compareTo(thereValue);
            if (c != 0)
                return c;
        }
        return 0;
    }
}
