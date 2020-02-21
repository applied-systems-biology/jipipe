package org.hkijena.acaq5.api.data;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

public abstract class ACAQDataSlot<T extends ACAQData> {
    private ACAQAlgorithm algorithm;
    private String name;
    private Class<T> acceptedDataType;
    private T data;
    private SlotType slotType;
    private Path storagePath;

    public ACAQDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name, Class<T> acceptedDataType) {
        this.algorithm = algorithm;
        this.name = name;
        this.slotType = slotType;
        this.acceptedDataType = acceptedDataType;
    }

    public Class<? extends ACAQData> getAcceptedDataType() {
        return acceptedDataType;
    }

    public boolean accepts(ACAQData data) {
        return acceptedDataType.isAssignableFrom(data.getClass());
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return algorithm.getName() + " \uD83E\uDC92 " + getName();
    }

    public T getData() {
        return data;
    }

    public void setData(ACAQData data) {
        if(!accepts(data))
            throw new RuntimeException("Data slot does not accept data");
        this.data = (T)data;
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    /**
     * Saves the stored data to the provided storage path and sets data to null
     * Warning: Ensure that depending input slots do not use this slot, anymore!
     */
    public void flush() {
        if(isOutput() && storagePath != null && data != null) {
            data.saveTo(storagePath, getName());
            data = null;
        }
    }

    public boolean isInput() {
        switch(slotType) {
            case Input:
                return true;
            case Output:
                return false;
            default:
                throw new RuntimeException("Unknown slot type!");
        }
    }

    public boolean isOutput() {
        switch(slotType) {
            case Input:
                return false;
            case Output:
                return true;
            default:
                throw new RuntimeException("Unknown slot type!");
        }
    }

    /**
     * Gets the storage path that is used during running the algorithm for saving the results
     * This is not used during project creation
     * @return
     */
    public Path getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }

    public enum SlotType {
        Input,
        Output
    }

    /**
     * Instantiates a slot.
     * This requires from the slot class that it has the same parameters as {@link ACAQDataSlot}, but without acceptedDataType
     * @param algorithm
     * @param definition
     * @return
     */
    public static ACAQDataSlot<?> createInstance(ACAQAlgorithm algorithm, ACAQSlotDefinition definition) {
        Constructor<?> constructor = ConstructorUtils.getMatchingAccessibleConstructor(definition.getSlotClass(), ACAQAlgorithm.class, SlotType.class, String.class);
        try {
            return (ACAQDataSlot<?>) constructor.newInstance(algorithm, definition.getSlotType(), definition.getName());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
