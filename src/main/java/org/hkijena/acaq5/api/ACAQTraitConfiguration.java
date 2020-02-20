package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.TraitsChangedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Processes algorithm traits of an {@link ACAQAlgorithm}
 */
public abstract class ACAQTraitConfiguration {

    private EventBus eventBus = new EventBus();

    public ACAQTraitConfiguration() {
    }

    /**
     * Transfers traits from an input slot to an output slot
     * @param sourceSlotName Input slot name
     * @param source Input slot traits
     * @param targetSlotName Output slot name
     * @param target Output slot traits
     */
    public abstract void transfer(String sourceSlotName, Set<Class<? extends ACAQTrait>> source,
                         String targetSlotName, Set<Class<? extends ACAQTrait>> target);

    /**
     * Modifies the traits of a slot
     * This function is applied to output slots after transfer
     * @param slotName Output slot name
     * @param target Existing output slot traits
     */
    public abstract void modify(String slotName, Set<Class<? extends ACAQTrait>> target);

    public EventBus getEventBus() {
        return eventBus;
    }
}
