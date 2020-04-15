package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

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
                    throw new NullPointerException("Could not find matching input slot for provided annotations!");
                inputSlotRows.put(inputSlot, row);
            }
        }
    }

    /**
     * Gets stored data from an input slot
     *
     * @param slotName The slot name
     * @param <T>      Data type
     * @return Input data with provided name
     */
    public <T extends ACAQData> T getInputData(String slotName) {
        return getInputData(algorithm.getInputSlot(slotName));
    }

    /**
     * Gets stored data from an input slot
     *
     * @param slot The slot
     * @param <T>  Data type
     * @return Input data with provided name
     */
    public <T extends ACAQData> T getInputData(ACAQDataSlot slot) {
        if (slot.getAlgorithm() != algorithm)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isInput())
            throw new IllegalArgumentException("Slot is not an input slot!");
        return (T) slot.getData(inputSlotRows.get(slot));
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
    public void addAnnotation(ACAQTrait trait) {
        removeAnnotation(trait.getDeclaration());
        annotations.add(trait);
    }

    /**
     * Removes an annotation of provided type
     * Only the exact type is removed.
     *
     * @param declaration removed annotation
     */
    public void removeAnnotation(ACAQTraitDeclaration declaration) {
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
}
