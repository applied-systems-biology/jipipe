package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ACAQSlotConfiguration {
    private EventBus eventBus = new EventBus();
    public abstract Map<String, ACAQSlotDefinition> getSlots();
    public abstract List<String> getInputSlotOrder();
    public abstract List<String> getOutputSlotOrder();
    public EventBus getEventBus() {
        return eventBus;
    }

    public Map<String, ACAQSlotDefinition> getInputSlots() {
        Map<String, ACAQSlotDefinition> result = new HashMap<>();
        for(Map.Entry<String, ACAQSlotDefinition> kv : getSlots().entrySet()) {
            if(kv.getValue().getSlotType() == ACAQDataSlot.SlotType.Input)
                result.put(kv.getKey(), kv.getValue());
        }
        return result;
    }

    public Map<String, ACAQSlotDefinition> getOutputSlots() {
        Map<String, ACAQSlotDefinition> result = new HashMap<>();
        for(Map.Entry<String, ACAQSlotDefinition> kv : getSlots().entrySet()) {
            if(kv.getValue().getSlotType() == ACAQDataSlot.SlotType.Output)
                result.put(kv.getKey(), kv.getValue());
        }
        return result;
    }

    /**
     * Returns the maximum of number(input slot) and number(output slots)
     * @return
     */
    public int getRows() {
        return Math.max(getInputSlots().size(), getOutputSlots().size());
    }
}
