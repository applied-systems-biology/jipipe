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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps a set of input and output slots that belong together
 * This can be used for convenience
 */
public class JIPipeDataBatch {
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
    public void setData(JIPipeDataSlot slot, int row) {
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
     * @param <T>       Data type
     * @param slotName  The slot name
     * @param dataClass The data type that should be returned
     * @return Input data with provided name
     */
    public <T extends JIPipeData> T getInputData(String slotName, Class<T> dataClass) {
        return getInputData(node.getInputSlot(slotName), dataClass);
    }

    //    private void initialize(List<JIPipeDataSlot> inputSlots, int referenceInputSlotRow) {
//        JIPipeDataSlot referenceInputSlot = inputSlots.get(0);
//        inputSlotRows.put(referenceInputSlot, referenceInputSlotRow);
//        annotations = referenceInputSlot.getAnnotations(referenceInputSlotRow);
//        for (JIPipeDataSlot inputSlot : inputSlots) {
//            if (inputSlot != referenceInputSlot) {
//                int row = inputSlot.findRowWithTraits(annotations);
//                if (row == -1)
//                    throw new UserFriendlyRuntimeException("Could not find matching input slot for provided annotations!",
//                            "Unable to group input data!",
//                            "Algorithm '" + algorithm.getName() + "'", "The algorithm '" + algorithm.getName() + "' has multiple input data slots. Tho process it, JIPipe must find input data " +
//                            "that belongs to the same data set. It uses annotations for this purpose. If you have duplicate annotations, or missing columns, then " +
//                            "JIPipe is not able to find matching data.",
//                            "Run the quick run on each input data set and check that annotation columns with unique values are created. You can have " +
//                                    "multiple columns - all of them will be taken into consideration. If you do not have annotations, use nodes in the 'Annotation' " +
//                                    "category to add them early on during file processing.");
//                inputSlotRows.put(inputSlot, row);
//            }
//        }
//    }

    /**
     * Gets stored data from an input slot
     *
     * @param <T>       Data type
     * @param slot      The slot
     * @param dataClass The data type that should be returned
     * @return Input data with provided name
     */
    public <T extends JIPipeData> T getInputData(JIPipeDataSlot slot, Class<T> dataClass) {
        if (slot.getNode() != node)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isInput())
            throw new IllegalArgumentException("Slot is not an input slot!");
        return slot.getData(inputSlotRows.get(slot), dataClass);
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
}
