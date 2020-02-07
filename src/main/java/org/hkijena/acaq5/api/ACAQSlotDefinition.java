package org.hkijena.acaq5.api;

public class ACAQSlotDefinition {
    private Class<? extends ACAQDataSlot<?>> slotClass;
    private ACAQDataSlot.SlotType slotType;
    private String name;

    public ACAQSlotDefinition(Class<? extends ACAQDataSlot<?>> slotClass, ACAQDataSlot.SlotType slotType, String name) {
        this.slotClass = slotClass;
        this.slotType = slotType;
        this.name = name;
    }

    public Class<? extends ACAQDataSlot<?>> getSlotClass() {
        return slotClass;
    }

    public ACAQDataSlot.SlotType getSlotType() {
        return slotType;
    }

    public String getName() {
        return name;
    }
}
