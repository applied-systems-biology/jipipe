package org.hkijena.acaq5.api;

import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class ACAQDataSlot<T extends ACAQData> {
    private ACAQAlgorithm algorithm;
    private String name;
    private Class<T> acceptedDataType;
    private T data;
    private SlotType slotType;

    public ACAQDataSlot(ACAQAlgorithm algorithm, SlotType slotType, String name, Class<T> acceptedDataType) {
        this.algorithm = algorithm;
        this.name = name;
        this.slotType = slotType;
        this.acceptedDataType = acceptedDataType;
    }

    public Class<? extends ACAQData> getAcceptedDataType() {
        return acceptedDataType;
    }

    public boolean accepts(T data) {
        return acceptedDataType.isAssignableFrom(data.getClass());
    }

    public String getName() {
        return name;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        if(!accepts(data))
            throw new RuntimeException("Data slot does not accept data");
        this.data = data;
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }

    public SlotType getSlotType() {
        return slotType;
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
