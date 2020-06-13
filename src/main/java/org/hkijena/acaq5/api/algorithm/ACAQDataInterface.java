package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps a set of input and output slots that belong together
 * This can be used for convenience
 */
public class ACAQDataInterface {
    private ACAQGraphNode algorithm;
    private Map<ACAQDataSlot, Integer> inputSlotRows;
    private Map<String, ACAQAnnotation> annotations = new HashMap<>();

    /**
     * Creates a new interface
     *
     * @param algorithm The algorithm
     */
    public ACAQDataInterface(ACAQGraphNode algorithm) {
        this.algorithm = algorithm;
        this.inputSlotRows = new HashMap<>();
//        initialize(inputSlots, row);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ACAQDataInterface(ACAQDataInterface other) {
        this.algorithm = other.algorithm;
        this.inputSlotRows = new HashMap<>(other.inputSlotRows);
        this.annotations = new HashMap<>(other.annotations);
    }

    /**
     * Sets the data row of a given slot. This should not be called after the interface was generated
     *
     * @param slot the data slot
     * @param row  the row
     */
    public void setData(ACAQDataSlot slot, int row) {
        inputSlotRows.put(slot, row);
    }

    /**
     * Adds annotations to the global annotation storage of this interface.
     * Global annotations are passed to all output slots.
     *
     * @param annotations the annotations
     * @param overwrite   if true, annotations of the same type are overwritten
     */
    public void addGlobalAnnotations(List<ACAQAnnotation> annotations, boolean overwrite) {
        for (ACAQAnnotation annotation : annotations) {
            if (annotation != null) {
                if (overwrite)
                    this.annotations.put(annotation.getName(), annotation);
                else
                    this.annotations.putIfAbsent(annotation.getName(), annotation);
            }
        }
    }

//    private void initialize(List<ACAQDataSlot> inputSlots, int referenceInputSlotRow) {
//        ACAQDataSlot referenceInputSlot = inputSlots.get(0);
//        inputSlotRows.put(referenceInputSlot, referenceInputSlotRow);
//        annotations = referenceInputSlot.getAnnotations(referenceInputSlotRow);
//        for (ACAQDataSlot inputSlot : inputSlots) {
//            if (inputSlot != referenceInputSlot) {
//                int row = inputSlot.findRowWithTraits(annotations);
//                if (row == -1)
//                    throw new UserFriendlyRuntimeException("Could not find matching input slot for provided annotations!",
//                            "Unable to group input data!",
//                            "Algorithm '" + algorithm.getName() + "'", "The algorithm '" + algorithm.getName() + "' has multiple input data slots. Tho process it, ACAQ must find input data " +
//                            "that belongs to the same data set. It uses annotations for this purpose. If you have duplicate annotations, or missing columns, then " +
//                            "ACAQ is not able to find matching data.",
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
     * @param slotName  The slot name
     * @param dataClass The data type that should be returned
     * @return Input data with provided name
     */
    public <T extends ACAQData> T getInputData(String slotName, Class<T> dataClass) {
        return getInputData(algorithm.getInputSlot(slotName), dataClass);
    }

    /**
     * Gets stored data from an input slot
     *
     * @param <T>       Data type
     * @param slot      The slot
     * @param dataClass The data type that should be returned
     * @return Input data with provided name
     */
    public <T extends ACAQData> T getInputData(ACAQDataSlot slot, Class<T> dataClass) {
        if (slot.getAlgorithm() != algorithm)
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
    public Map<String, ACAQAnnotation> getAnnotations() {
        return annotations;
    }

    /**
     * Adds an annotation to the annotation list
     *
     * @param trait added annotation. Cannot be null.
     */
    public void addGlobalAnnotation(ACAQAnnotation trait) {
        annotations.put(trait.getName(), trait);
    }

    /**
     * Adds an annotation to the annotation list
     *
     * @param trait     added annotation. Cannot be null.
     * @param overwrite if existing annotation types can be overwritten
     */
    public void addGlobalAnnotation(ACAQAnnotation trait, boolean overwrite) {
        if (overwrite && annotations.containsKey(trait.getName()))
            return;
        annotations.put(trait.getName(), trait);
    }


    /**
     * Removes an annotation of provided type
     *
     * @param declaration removed annotation
     */
    public void removeGlobalAnnotation(String declaration) {
        annotations.remove(declaration);
    }

    /**
     * Returns an existing annotation
     * Only the exact type is used for searching
     *
     * @param declaration annotation type
     * @return null if it does not exist
     */
    public ACAQAnnotation getAnnotationOfType(String declaration) {
        return annotations.getOrDefault(declaration, null);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations should be set up till this point
     *
     * @param slotName Slot name
     * @param data     Added data
     */
    public void addOutputData(String slotName, ACAQData data) {
        addOutputData(algorithm.getOutputSlot(slotName), data);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations should be set up till this point
     *
     * @param slotName              Slot name
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the global ones
     */
    public void addOutputData(String slotName, ACAQData data, List<ACAQAnnotation> additionalAnnotations) {
        addOutputData(algorithm.getOutputSlot(slotName), data, additionalAnnotations);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations that are added to all traits should be set up till this point
     *
     * @param slot Slot instance
     * @param data Added data
     */
    public void addOutputData(ACAQDataSlot slot, ACAQData data) {
        if (slot.getAlgorithm() != algorithm)
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
    public void addOutputData(ACAQDataSlot slot, ACAQData data, List<ACAQAnnotation> additionalAnnotations) {
        if (slot.getAlgorithm() != algorithm)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        List<ACAQAnnotation> finalAnnotations = new ArrayList<>(annotations.values());
        finalAnnotations.addAll(additionalAnnotations);
        slot.addData(data, finalAnnotations);
    }
}
