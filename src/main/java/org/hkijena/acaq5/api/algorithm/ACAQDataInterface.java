package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps a set of input and output slots that belong together
 * This can be used for convenience
 */
public class ACAQDataInterface {
    private ACAQAlgorithm algorithm;
    private Map<ACAQDataSlot, Integer> inputSlotRows;
    private List<ACAQTrait> annotations;

    /**
     * Creates a new interface
     *
     * @param algorithm  The algorithm
     * @param inputSlots Input slots that are considered during the calculation. The first one is used as reference
     * @param row        the row this data interface is generated for
     */
    public ACAQDataInterface(ACAQAlgorithm algorithm, List<ACAQDataSlot> inputSlots, int row) {
        this.algorithm = algorithm;
        this.inputSlotRows = new HashMap<>();
        initialize(inputSlots, row);
    }

    private void initialize(List<ACAQDataSlot> inputSlots, int referenceInputSlotRow) {
        ACAQDataSlot referenceInputSlot = inputSlots.get(0);
        inputSlotRows.put(referenceInputSlot, referenceInputSlotRow);
        annotations = referenceInputSlot.getAnnotations(referenceInputSlotRow);
        for (ACAQDataSlot inputSlot : inputSlots) {
            if (inputSlot != referenceInputSlot) {
                int row = inputSlot.findRowWithTraits(annotations);
                if (row == -1)
                    throw new UserFriendlyRuntimeException("Could not find matching input slot for provided annotations!",
                            "Unable to group input data!",
                            "Algorithm " + algorithm.getName(), "The algorithm '" + algorithm.getName() + "' has multiple input data slots. Tho process it, ACAQ must find input data " +
                                    "that belongs to the same data set. It uses annotations for this purpose. If you have duplicate annotations, or missing columns, then " +
                                    "ACAQ is not able to find matching data.",
                            "Run the testbench on each input data set and check that annotation columns with unique values are created. You can have " +
                                    "multiple columns - all of them will be taken into consideration. If you do not have annotations, use nodes in the 'Annotation' " +
                                    "category to add them early on during file processing.");
                inputSlotRows.put(inputSlot, row);
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
     * Gets the list of annotations
     *
     * @return list of annotations
     */
    public List<ACAQTrait> getAnnotations() {
        return annotations;
    }

    /**
     * Adds an annotation to the annotation list
     * If there is already existing an annotation of the sample type, it will be overwritten
     *
     * @param trait added annotation
     */
    public void addGlobalAnnotation(ACAQTrait trait) {
        removeGlobalAnnotation(trait.getDeclaration());
        annotations.add(trait);
    }

    /**
     * Removes an annotation of provided type
     * Only the exact type is removed.
     *
     * @param declaration removed annotation
     */
    public void removeGlobalAnnotation(ACAQTraitDeclaration declaration) {
        annotations.removeIf(a -> a.getDeclaration() == declaration);
    }

    /**
     * Returns an existing annotation
     * Only the exact type is used for searching
     *
     * @param declaration annotation type
     * @return null if it does not exist
     */
    public ACAQTrait getAnnotationOfType(ACAQTraitDeclaration declaration) {
        return annotations.stream().filter(a -> a.getDeclaration() == declaration).findFirst().orElse(null);
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
    public void addOutputData(String slotName, ACAQData data, List<ACAQTrait> additionalAnnotations) {
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
        slot.addData(data, annotations);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations that are added to all traits should be set up till this point
     *
     * @param slot                  Slot instance
     * @param data                  Added data
     * @param additionalAnnotations Annotations that are added additionally to the global ones
     */
    public void addOutputData(ACAQDataSlot slot, ACAQData data, List<ACAQTrait> additionalAnnotations) {
        if (slot.getAlgorithm() != algorithm)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        List<ACAQTrait> finalAnnotations = new ArrayList<>(annotations);
        finalAnnotations.addAll(additionalAnnotations);
        slot.addData(data, finalAnnotations);
    }
}
