package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;

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

    /**
     * Returns the name of an algorithm
     * @param klass
     * @return
     */
    public static String getName(Class<? extends  ACAQAlgorithm> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if(annotations.length > 0) {
            return annotations[0].name();
        }
        else {
            return klass.getSimpleName();
        }
    }

}
