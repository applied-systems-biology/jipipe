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

import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wraps a set of input and output slots that belong together.
 * This is a less restricted variant of {@link JIPipeDataBatch} used by {@link JIPipeMergingAlgorithm}
 */
public class JIPipeMergingDataBatch {
    private JIPipeGraphNode node;
    private Map<JIPipeDataSlot, Set<Integer>> inputSlotRows;
    private Map<String, JIPipeAnnotation> annotations = new HashMap<>();

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
        this.annotations = new HashMap<>(other.annotations);
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
    public void addData(JIPipeDataSlot slot, int row) {
        Set<Integer> rows = inputSlotRows.getOrDefault(slot, null);
        if (rows == null) {
            rows = new HashSet<>();
            inputSlotRows.put(slot, rows);
        }
        rows.add(row);
    }

    /**
     * Sets the input slot to only one row
     *
     * @param slot the slot
     * @param row  the row
     */
    public void setData(JIPipeDataSlot slot, int row) {
        Set<Integer> rows = inputSlotRows.getOrDefault(slot, null);
        if (rows == null) {
            rows = new HashSet<>();
            inputSlotRows.put(slot, rows);
        }
        rows.clear();
        rows.add(row);
    }

    public void setAnnotations(Map<String, JIPipeAnnotation> annotations) {
        this.annotations = annotations;
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
     * @param <T>       Data type
     * @param slotName  The slot name
     * @param dataClass The data type that should be returned
     * @return Input data with provided name
     */
    public <T extends JIPipeData> List<T> getInputData(String slotName, Class<T> dataClass) {
        return getInputData(node.getInputSlot(slotName), dataClass);
    }

    /**
     * Gets stored data from an input slot
     *
     * @param <T>       Data type
     * @param slot      The slot
     * @param dataClass The data type that should be returned
     * @return Input data with provided name
     */
    public <T extends JIPipeData> List<T> getInputData(JIPipeDataSlot slot, Class<T> dataClass) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isInput())
            throw new IllegalArgumentException("Slot is not an input slot!");
        List<T> result = new ArrayList<>();
        for (Integer row : inputSlotRows.getOrDefault(slot, Collections.emptySet())) {
            result.add(slot.getData(row, dataClass));
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
        return getInputRows(node.getInputSlot(slot));
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
        if (!slot.isInput())
            throw new IllegalArgumentException("Slot is not an input slot!");
        return inputSlotRows.getOrDefault(slot, Collections.emptySet());
    }

    /**
     * Gets the list of annotations
     *
     * @return list of annotations
     */
    public Map<String, JIPipeAnnotation> getAnnotations() {
        return annotations;
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
     * @param slotName Slot name
     * @param data     Added data
     */
    public void addOutputData(String slotName, JIPipeData data) {
        addOutputData(node.getOutputSlot(slotName), data);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations should be set up till this point
     *
     * @param slotName              Slot name
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the global ones
     */
    public void addOutputData(String slotName, JIPipeData data, List<JIPipeAnnotation> additionalAnnotations) {
        addOutputData(node.getOutputSlot(slotName), data, additionalAnnotations);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations that are added to all traits should be set up till this point
     *
     * @param slot Slot instance
     * @param data Added data
     */
    public void addOutputData(JIPipeDataSlot slot, JIPipeData data) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        slot.addData(data, new ArrayList<>(annotations.values()));
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations that are added to all traits should be set up till this point
     *
     * @param slot                  Slot instance
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the global ones
     */
    public void addOutputData(JIPipeDataSlot slot, JIPipeData data, List<JIPipeAnnotation> additionalAnnotations) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        List<JIPipeAnnotation> finalAnnotations = new ArrayList<>(annotations.values());
        finalAnnotations.addAll(additionalAnnotations);
        slot.addData(data, finalAnnotations);
    }


    /**
     * Returns true if there is at least one slot that has no rows attached to it
     * @return if the batch is incomplete
     */
    public boolean isIncomplete() {
        for (Set<Integer> rows : inputSlotRows.values()) {
            if(rows.isEmpty())
                return true;
        }
        return false;
    }

    /**
     * Returns true if each slot only has one row
     * @return if the batch is single
     */
    public boolean isSingle() {
        for (Set<Integer> rows : inputSlotRows.values()) {
            if(rows.size() != 1)
                return false;
        }
        return true;
    }
}
