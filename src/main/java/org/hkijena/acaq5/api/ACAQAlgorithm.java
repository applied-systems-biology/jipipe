package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.SlotAddedEvent;
import org.hkijena.acaq5.api.events.SlotRemovedEvent;
import org.hkijena.acaq5.api.events.SlotRenamedEvent;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class ACAQAlgorithm {
    private ACAQSlotConfiguration slotConfiguration;
    private Map<String, ACAQDataSlot<?>> slots = new HashMap<>();
    private EventBus eventBus = new EventBus();
    private Point location;

    public ACAQAlgorithm(ACAQSlotConfiguration slotConfiguration) {
       this.slotConfiguration = slotConfiguration;
       slotConfiguration.getEventBus().register(this);
       initalize();
    }

    private void initalize() {
        for(Map.Entry<String, ACAQSlotDefinition> kv : slotConfiguration.getSlots().entrySet()) {
            slots.put(kv.getKey(), ACAQDataSlot.createInstance(this, kv.getValue()));
        }
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

    /**
     * Returns the name of an algorithm
     * @param klass
     * @return
     */
    public static ACAQAlgorithmCategory getCategory(Class<? extends  ACAQAlgorithm> klass) {
        ACAQAlgorithmMetadata[] annotations = klass.getAnnotationsByType(ACAQAlgorithmMetadata.class);
        if(annotations.length > 0) {
            return annotations[0].category();
        }
        else {
            return ACAQAlgorithmCategory.Special;
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
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
    }

    @Subscribe
    public void onSlotRemoved(SlotRemovedEvent event) {
        slots.remove(event.getSlotName());
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
    }

    @Subscribe
    public void onSlotRenamed(SlotRenamedEvent event) {
        ACAQDataSlot<?> slot = slots.get(event.getOldSlotName());
        slots.remove(event.getOldSlotName());
        slots.put(event.getNewSlotName(), slot);
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
    }

    /**
     * Gets the location within UI representations
     * @return
     */
    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public static <T extends ACAQAlgorithm> T createInstance(Class<? extends T> klass) {
        try {
            return klass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
