package org.hkijena.acaq5.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.eventbus.EventBus;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class ACAQAlgorithm {

    private Map<String, ACAQDataSlot> inputSlots = new HashMap<>();
    private Map<String, ACAQDataSlot> outputSlots = new HashMap<>();
    private EventBus eventBus = new EventBus();

    public ACAQAlgorithm(ACAQDataSlot... slots) {
        for(ACAQDataSlot slot : slots) {
            slot.setAlgorithm(this);
            switch (slot.getType()) {
                case Input:
                    inputSlots.put(slot.getName(), slot);
                    break;
                case Output:
                    outputSlots.put(slot.getName(), slot);
                    break;
                default:
                    throw new RuntimeException("Unsupported slot type " + slot.getType());
            }
        }
    }

    public abstract void run();

    public Map<String, ACAQDataSlot> getInputSlots() {
        return Collections.unmodifiableMap(inputSlots);
    }

    public Map<String, ACAQDataSlot> getOutputSlots() {
        return Collections.unmodifiableMap(outputSlots);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

}
