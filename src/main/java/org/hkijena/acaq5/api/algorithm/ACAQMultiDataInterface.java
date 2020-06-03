package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Groups inputs together based on annotations. This is a weaker variant of {@link ACAQDataInterface}.
 * Unlike {@link ACAQDataInterface}, this class does not assume uniqueness.
 */
public class ACAQMultiDataInterface {
    private ACAQAlgorithm algorithm;
    private Map<ACAQDataSlot, List<Integer>> inputSlotRows;

    /**
     * Creates a new interface
     *
     * @param algorithm       The algorithm
     * @param inputSlots      Input slots that are considered during the calculation. The first one is used as reference
     * @param referenceTraits Annotation types considered as reference. If empty, all traits are used as reference
     * @param row             the row this data interface is generated for
     */
    public ACAQMultiDataInterface(ACAQAlgorithm algorithm, List<ACAQDataSlot> inputSlots, Set<ACAQTraitDeclaration> referenceTraits, int row) {
        this.algorithm = algorithm;
        this.inputSlotRows = new HashMap<>();
        initialize(inputSlots, row, referenceTraits);
    }

    private void initialize(List<ACAQDataSlot> inputSlots, int referenceInputSlotRow, Set<ACAQTraitDeclaration> referenceTraitDeclarations) {
        ACAQDataSlot referenceInputSlot = inputSlots.get(0);
        List<ACAQTrait> referenceAnnotations;
        if (referenceTraitDeclarations.isEmpty()) {
            referenceAnnotations = referenceInputSlot.getAnnotations(referenceInputSlotRow);
        } else {
            referenceAnnotations = new ArrayList<>();
            for (ACAQTrait annotation : referenceInputSlot.getAnnotations(referenceInputSlotRow)) {
                if (annotation != null && referenceTraitDeclarations.contains(annotation.getDeclaration())) {
                    referenceAnnotations.add(annotation);
                }
            }
        }
        for (ACAQDataSlot inputSlot : inputSlots) {
            inputSlotRows.put(inputSlot, inputSlot.findRowsWithTraits(referenceAnnotations));
        }
        if (inputSlotRows.get(referenceInputSlot).isEmpty()) {
            inputSlotRows.get(referenceInputSlot).add(referenceInputSlotRow);
        }
    }

    /**
     * Gets stored data from an input slot.
     * Order is consistent with getRows()
     *
     * @param <T>       Data type
     * @param slotName  The slot name
     * @param dataClass The data type that should be returned
     * @return Input data with provided name
     */
    public <T extends ACAQData> List<T> getInputData(String slotName, Class<T> dataClass) {
        return getInputData(algorithm.getInputSlot(slotName), dataClass);
    }

    /**
     * Gets stored data from an input slot
     * Order is consistent with getRows()
     *
     * @param <T>       Data type
     * @param slot      The slot
     * @param dataClass The data type that should be returned
     * @return Input data with provided name
     */
    public <T extends ACAQData> List<T> getInputData(ACAQDataSlot slot, Class<T> dataClass) {
        if (slot.getAlgorithm() != algorithm)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isInput())
            throw new IllegalArgumentException("Slot is not an input slot!");
        return getRows(slot).stream().map(row -> slot.getData(row, dataClass)).collect(Collectors.toList());
    }

    /**
     * Returns the rows that belong to this data interface for given slot
     *
     * @param slot the data slot
     * @return the rows that belong to this data interface for given slot
     */
    public List<Integer> getRows(ACAQDataSlot slot) {
        return inputSlotRows.get(slot);
    }

    /**
     * Returns the list of all annotations for each data set row.
     * Order is consistent to getRows()
     *
     * @param slot the data slot
     * @return list of all annotations for each data set row
     */
    public List<List<ACAQTrait>> getAnnotations(ACAQDataSlot slot) {
        return getRows(slot).stream().map(slot::getAnnotations).collect(Collectors.toList());
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations should be set up till this point
     *
     * @param slotName Slot name
     * @param data     Added data
     */
    public void addOutputData(String slotName, ACAQData data) {
        addOutputData(algorithm.getOutputSlot(slotName), data, Collections.emptyList());
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations should be set up till this point
     *
     * @param slotName    Slot name
     * @param data        Added data
     * @param annotations Added annotations
     */
    public void addOutputData(String slotName, ACAQData data, List<ACAQTrait> annotations) {
        addOutputData(algorithm.getOutputSlot(slotName), data, annotations);
    }

    /**
     * Writes output data into the provided slot
     * Please note that annotations that are added to all traits should be set up till this point
     *
     * @param slot        Slot instance
     * @param data        Added data
     * @param annotations Added annotations
     */
    public void addOutputData(ACAQDataSlot slot, ACAQData data, List<ACAQTrait> annotations) {
        if (slot.getAlgorithm() != algorithm)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        slot.addData(data, annotations);
    }
}
