package org.hkijena.acaq5.api;

public class ACAQDataSlot<T extends ACAQData> {
    private SlotType type;
    private String name;
    private Class<T> acceptedDataType;
    private T data;

    public ACAQDataSlot(SlotType type, String name, Class<T> acceptedDataType) {
        this.type = type;
        this.name = name;
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

    public SlotType getType() {
        return type;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        if(!accepts(data))
            throw new RuntimeException("Data slot does not accept data");
        this.data = data;
    }

    public enum SlotType {
        Input,
        Output
    }
}
