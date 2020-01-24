package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A special {@link ACAQAlgorithm} that passes input through output.
 * The input and output slots can be edited
 */
public class ACAQIO extends ACAQAlgorithm {

    private Map<String, ACAQDataSlot> slots = new HashMap<>();

    @Override
    public void run() {

    }

    public void addSlot(ACAQDataSlot<?> slot) {
        if(slots.containsKey(slot.getName()))
            throw new RuntimeException("Already contains slot!");
        slots.put(slot.getName(), slot);
        getEventBus().post(new AlgorithmSlotsChangedEvent(this));
    }

    public void removeSlot(String name) {
        slots.remove(name);
        getEventBus().post(new AlgorithmSlotsChangedEvent(this));
    }

    @Override
    public Map<String, ACAQDataSlot> getInputSlots() {
        return Collections.unmodifiableMap(slots);
    }

    @Override
    public Map<String, ACAQDataSlot> getOutputSlots() {
        return Collections.unmodifiableMap(slots);
    }
}
