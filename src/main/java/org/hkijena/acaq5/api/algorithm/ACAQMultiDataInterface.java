package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.util.*;

/**
 * Wraps a set of input and output slots that belong together.
 * This is a less restricted variant of {@link ACAQDataInterface} used by {@link ACAQMergingAlgorithm}
 */
public class ACAQMultiDataInterface {
    private ACAQGraphNode algorithm;
    private Map<ACAQDataSlot, Set<Integer>> inputSlotRows;
    private Map<ACAQTraitDeclaration, ACAQTrait> annotations = new HashMap<>();

    /**
     * Creates a new interface
     *
     * @param algorithm The algorithm
     */
    public ACAQMultiDataInterface(ACAQGraphNode algorithm) {
        this.algorithm = algorithm;
        this.inputSlotRows = new HashMap<>();
//        initialize(inputSlots, row);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ACAQMultiDataInterface(ACAQMultiDataInterface other) {
        this.algorithm = other.algorithm;
        this.inputSlotRows = new HashMap<>(other.inputSlotRows);
        this.annotations = new HashMap<>(other.annotations);
    }

    /**
     * Adds the data row of a given slot. This should not be called after the interface was generated
     *
     * @param slot the data slot
     * @param row  the row
     */
    public void addData(ACAQDataSlot slot, int row) {
        Set<Integer> rows = inputSlotRows.getOrDefault(slot, null);
        if (rows == null) {
            rows = new HashSet<>();
            inputSlotRows.put(slot, rows);
        }
        rows.add(row);
    }

    /**
     * Adds annotations to the global annotation storage of this interface.
     * Global annotations are passed to all output slots.
     *
     * @param annotations the annotations
     * @param overwrite   if true, annotations of the same type are overwritten
     */
    public void addGlobalAnnotations(List<ACAQTrait> annotations, boolean overwrite) {
        for (ACAQTrait annotation : annotations) {
            if (annotation != null) {
                if (overwrite)
                    this.annotations.put(annotation.getDeclaration(), annotation);
                else
                    this.annotations.putIfAbsent(annotation.getDeclaration(), annotation);
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
    public <T extends ACAQData> List<T> getInputData(String slotName, Class<T> dataClass) {
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
    public <T extends ACAQData> List<T> getInputData(ACAQDataSlot slot, Class<T> dataClass) {
        if (slot.getAlgorithm() != algorithm)
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
        return getInputRows(algorithm.getInputSlot(slot));
    }

    /**
     * Returns the row indices that belong to this data interface
     *
     * @param slot slot
     * @return the row indices that belong to this data interface
     */
    public Set<Integer> getInputRows(ACAQDataSlot slot) {
        if (slot.getAlgorithm() != algorithm)
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
    public Map<ACAQTraitDeclaration, ACAQTrait> getAnnotations() {
        return annotations;
    }

    /**
     * Adds an annotation to the annotation list
     *
     * @param trait added annotation. Cannot be null.
     */
    public void addGlobalAnnotation(ACAQTrait trait) {
        annotations.put(trait.getDeclaration(), trait);
    }

    /**
     * Removes an annotation of provided type
     *
     * @param declaration removed annotation
     */
    public void removeGlobalAnnotation(ACAQTraitDeclaration declaration) {
        annotations.remove(declaration);
    }

    /**
     * Returns an existing annotation
     * Only the exact type is used for searching
     *
     * @param declaration annotation type
     * @return null if it does not exist
     */
    public ACAQTrait getAnnotationOfType(ACAQTraitDeclaration declaration) {
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
    public void addOutputData(ACAQDataSlot slot, ACAQData data, List<ACAQTrait> additionalAnnotations) {
        if (slot.getAlgorithm() != algorithm)
            throw new IllegalArgumentException("The provided slot does not belong to the data interface algorithm!");
        if (!slot.isOutput())
            throw new IllegalArgumentException("Slot is not an output slot!");
        List<ACAQTrait> finalAnnotations = new ArrayList<>(annotations.values());
        finalAnnotations.addAll(additionalAnnotations);
        slot.addData(data, finalAnnotations);
    }
}
