package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.SlotAddedEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A schema for slots
 */
public class ACAQSlotConfiguration {
    private Map<String, ACAQSlotDefinition> slots = new HashMap<>();

    private boolean isSealed = false;
    private EventBus eventBus = new EventBus();
    private boolean allowInputSlots;
    private boolean allowOutputSlots;

    public ACAQSlotConfiguration(boolean allowInputSlots, boolean allowOutputSlots) {
        this.allowInputSlots = allowInputSlots;
        this.allowOutputSlots = allowOutputSlots;
    }

    public ACAQSlotConfiguration() {
        this(true, true);
    }

    public boolean hasSlot(String name) {
        return slots.containsKey(name);
    }

    public ACAQSlotConfiguration addInputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        if(!allowInputSlots)
            throw new RuntimeException("Slot configuration does not allow input slots");
        if(isSealed)
            throw new RuntimeException("Slot configuration is sealed!");
        if(hasSlot(name))
            throw new RuntimeException("Slot already exists!");
        slots.put(name, new ACAQSlotDefinition(klass, ACAQDataSlot.SlotType.Input, name));
        eventBus.post(new SlotAddedEvent(this, name));
        return this;
    }

    public ACAQSlotConfiguration addOutputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        if(!allowOutputSlots)
            throw new RuntimeException("Slot configuration does not allow output slots");
        if(isSealed)
            throw new RuntimeException("Slot configuration is sealed!");
        if(hasSlot(name))
            throw new RuntimeException("Slot already exists!");
        slots.put(name, new ACAQSlotDefinition(klass, ACAQDataSlot.SlotType.Output, name));
        eventBus.post(new SlotAddedEvent(this, name));
        return this;
    }

    public boolean hasInputSlots() {
        return slots.values().stream().anyMatch(x -> x.getSlotType() == ACAQDataSlot.SlotType.Input);
    }

    public boolean hasOutputSlots() {
        return slots.values().stream().anyMatch(x -> x.getSlotType() == ACAQDataSlot.SlotType.Output);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ACAQSlotConfiguration seal() {
        this.isSealed = true;
        return this;
    }

    public Map<String, ACAQSlotDefinition> getSlots() {
        return Collections.unmodifiableMap(slots);
    }
}
