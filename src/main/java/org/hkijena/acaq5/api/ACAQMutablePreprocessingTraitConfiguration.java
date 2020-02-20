package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.TraitsChangedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Trait configuration designed for usage in {@link ACAQPreprocessingOutput}
 */
public class ACAQMutablePreprocessingTraitConfiguration extends ACAQTraitConfiguration {
    private List<ACAQMutableTraitConfiguration.ModifyTask> modifyTasks = new ArrayList<>();
    private EventBus eventBus = new EventBus();
    private ACAQSlotConfiguration slotConfiguration;

    public ACAQMutablePreprocessingTraitConfiguration(ACAQSlotConfiguration slotConfiguration) {
        this.slotConfiguration = slotConfiguration;
    }

    /**
     * Adds a trait to the specified output slot
     * @param outputSlotName
     * @param trait
     * @return
     */
    public ACAQMutablePreprocessingTraitConfiguration addTraitTo(String outputSlotName, Class<? extends ACAQTrait> trait) {
        if(getSlotConfiguration().getSlots().containsKey(outputSlotName))
            throw new IllegalArgumentException("Slot must exist!");
        modifyTasks.add(new ACAQMutableTraitConfiguration.ModifyTask(outputSlotName, ACAQMutableTraitConfiguration.ModificationType.ADD, trait));
        eventBus.post(new TraitsChangedEvent(this));
        return this;
    }

    /**
     * Removes trait from specified output slot
     * @param outputSlotName
     * @param trait
     * @return
     */
    public ACAQMutablePreprocessingTraitConfiguration removeTraitFrom(String outputSlotName, Class<? extends ACAQTrait> trait) {
        if (getSlotConfiguration().getSlots().containsKey(outputSlotName))
            throw new IllegalArgumentException("Slot must exist!");
        modifyTasks.removeIf(task -> task.getTrait().equals(trait));
        eventBus.post(new TraitsChangedEvent(this));
        return this;
    }

    /**
     * Returns the trais of specified slot
     * @param slotName
     * @return
     */
    public Set<Class<? extends ACAQTrait>> getTraitsOf(String slotName) {
        return modifyTasks.stream().filter(task -> task.getSlotName().equals(slotName))
                .map(ACAQMutableTraitConfiguration.ModifyTask::getTrait).collect(Collectors.toSet());
    }

    /**
     * Removes all modifications and transfers
     */
    public ACAQTraitConfiguration clear() {
        modifyTasks.clear();
        eventBus.post(new TraitsChangedEvent(this));
        return this;
    }

    /**
     * Transfers traits from an input slot to an output slot
     * @param sourceSlotName Input slot name
     * @param source Input slot traits
     * @param targetSlotName Output slot name
     * @param target Output slot traits
     */
    @Override
    public void transfer(String sourceSlotName, Set<Class<? extends ACAQTrait>> source,
                         String targetSlotName, Set<Class<? extends ACAQTrait>> target) {
    }

    /**
     * Modifies the traits of a slot
     * This function is applied to output slots after transfer
     * @param slotName Output slot name
     * @param target Existing output slot traits
     */
    @Override
    public void modify(String slotName, Set<Class<? extends ACAQTrait>> target) {
        for(ACAQMutableTraitConfiguration.ModifyTask task : modifyTasks) {
            if(task.getSlotName() == null || slotName.equals(task.getSlotName())) {
                switch(task.getType()) {
                    case ADD:
                        target.add(task.getTrait());
                        break;
                    case REMOVE:
                        throw new RuntimeException("Preprocessing nodes cannot remove traits!");
                }
            }
        }
    }

    public ACAQSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }
}
