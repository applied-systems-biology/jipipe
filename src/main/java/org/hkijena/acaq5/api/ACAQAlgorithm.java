package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.SlotAddedEvent;
import org.hkijena.acaq5.api.events.SlotRemovedEvent;
import org.hkijena.acaq5.api.events.SlotRenamedEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class ACAQAlgorithm {

    private ACAQSlotConfiguration slotConfiguration;
    private Map<String, ACAQDataSlot<?>> slots = new HashMap<>();
    private EventBus eventBus = new EventBus();

    public ACAQAlgorithm(ACAQSlotConfiguration slotConfiguration) {
       this.slotConfiguration = slotConfiguration;
    }

    public abstract void run();

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

    public ACAQSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }

    public Map<String, ACAQDataSlot<?>> getSlots() {
        return Collections.unmodifiableMap(slots);
    }

    public Map<String, ACAQDataSlot<?>> getInputSlots() {
        Map<String, ACAQDataSlot<?>> result = new HashMap<>();
        for(Map.Entry<String, ACAQDataSlot<?>> kv : slots.entrySet()) {
            if(kv.getValue().isInput())
                result.put(kv.getKey(), kv.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    public Map<String, ACAQDataSlot<?>> getOutputSlots() {
        Map<String, ACAQDataSlot<?>> result = new HashMap<>();
        for(Map.Entry<String, ACAQDataSlot<?>> kv : slots.entrySet()) {
            if(kv.getValue().isOutput())
                result.put(kv.getKey(), kv.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    @Subscribe
    public void onSlotAdded(SlotAddedEvent event) {
        ACAQSlotDefinition definition = slotConfiguration.getSlots().get(event.getSlotName());
        slots.put(definition.getName(), ACAQDataSlot.createInstance(this, definition));
    }

    @Subscribe
    public void onSlotRemoved(SlotRemovedEvent event) {
        slots.remove(event.getSlotName());
    }

    @Subscribe
    public void onSlotRenamed(SlotRenamedEvent event) {
        ACAQDataSlot<?> slot = slots.get(event.getOldSlotName());
        slots.remove(event.getOldSlotName());
        slots.put(event.getNewSlotName(), slot);
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
    }
}
