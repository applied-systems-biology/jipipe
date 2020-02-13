package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.*;

import java.awt.*;
import java.util.*;
import java.util.List;

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

    public String getName() {
        return getNameOf(getClass());
    }

    public ACAQAlgorithmCategory getCategory() {
        return getCategoryOf(getClass());
    }

    /**
     * Returns the name of an algorithm
     * @param klass
     * @return
     */
    public static String getNameOf(Class<? extends  ACAQAlgorithm> klass) {
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
    public static ACAQAlgorithmCategory getCategoryOf(Class<? extends  ACAQAlgorithm> klass) {
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

    public List<String> getInputSlotOrder() {
        return getSlotConfiguration().getInputSlotOrder();
    }

    public List<String> getOutputSlotOrder() {
        return getSlotConfiguration().getOutputSlotOrder();
    }

    public List<ACAQDataSlot<?>> getInputSlots() {
        List<ACAQDataSlot<?>> result = new ArrayList<>();
        for(String key : getInputSlotOrder()) {
           result.add(slots.get(key));
        }
        return Collections.unmodifiableList(result);
    }

    public List<ACAQDataSlot<?>> getOutputSlots() {
        List<ACAQDataSlot<?>> result = new ArrayList<>();
        for(String key : getOutputSlotOrder()) {
            result.add(slots.get(key));
        }
        return Collections.unmodifiableList(result);
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

    @Subscribe
    public void onSlotOrderChanged(SlotOrderChangedEvent event) {
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
