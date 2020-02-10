package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.events.SlotAddedEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A schema for slots
 */
public class ACAQMutableSlotConfiguration extends ACAQSlotConfiguration {
    private Map<String, ACAQSlotDefinition> slots = new HashMap<>();

    private boolean isSealed = false;
    private boolean allowInputSlots;
    private boolean allowOutputSlots;

    public ACAQMutableSlotConfiguration(boolean allowInputSlots, boolean allowOutputSlots) {
        this.allowInputSlots = allowInputSlots;
        this.allowOutputSlots = allowOutputSlots;
    }

    public ACAQMutableSlotConfiguration() {
        this(true, true);
    }

    public boolean hasSlot(String name) {
        return slots.containsKey(name);
    }

    public ACAQMutableSlotConfiguration addInputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        if(!allowInputSlots)
            throw new RuntimeException("Slot configuration does not allow input slots");
        if(isSealed)
            throw new RuntimeException("Slot configuration is sealed!");
        if(hasSlot(name))
            throw new RuntimeException("Slot already exists!");
        slots.put(name, new ACAQSlotDefinition(klass, ACAQDataSlot.SlotType.Input, name));
        getEventBus().post(new SlotAddedEvent(this, name));
        return this;
    }

    public ACAQMutableSlotConfiguration addOutputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        if(!allowOutputSlots)
            throw new RuntimeException("Slot configuration does not allow output slots");
        if(isSealed)
            throw new RuntimeException("Slot configuration is sealed!");
        if(hasSlot(name))
            throw new RuntimeException("Slot already exists!");
        slots.put(name, new ACAQSlotDefinition(klass, ACAQDataSlot.SlotType.Output, name));
        getEventBus().post(new SlotAddedEvent(this, name));
        return this;
    }

    public boolean hasInputSlots() {
        return slots.values().stream().anyMatch(x -> x.getSlotType() == ACAQDataSlot.SlotType.Input);
    }

    public boolean hasOutputSlots() {
        return slots.values().stream().anyMatch(x -> x.getSlotType() == ACAQDataSlot.SlotType.Output);
    }

    public ACAQMutableSlotConfiguration seal() {
        this.isSealed = true;
        return this;
    }

    @Override
    public Map<String, ACAQSlotDefinition> getSlots() {
        return Collections.unmodifiableMap(slots);
    }

    public boolean isSealed() {
        return isSealed;
    }

    public boolean allowsInputSlots() {
        return allowInputSlots;
    }

    public boolean allowsOutputSlots() {
        return allowOutputSlots;
    }
}
