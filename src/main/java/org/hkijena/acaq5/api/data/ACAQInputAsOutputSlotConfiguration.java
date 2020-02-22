package org.hkijena.acaq5.api.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.SlotAddedEvent;
import org.hkijena.acaq5.api.events.SlotRemovedEvent;
import org.hkijena.acaq5.api.events.SlotRenamedEvent;

import java.util.*;

/**
 * Slot configuration that wraps around another mutable configuration and converts all input slots into output slots
 * This is only used by {@link org.hkijena.acaq5.api.ACAQPreprocessingOutput}
 */
@JsonSerialize(using = ACAQSlotConfiguration.Serializer.class)
public class ACAQInputAsOutputSlotConfiguration extends ACAQMutableSlotConfiguration {

    private ACAQMutableSlotConfiguration wrappedConfiguration;

    public ACAQInputAsOutputSlotConfiguration(ACAQMutableSlotConfiguration wrappedConfiguration) {
        this.wrappedConfiguration = wrappedConfiguration;
        wrappedConfiguration.getEventBus().register(this);
    }

    @Override
    public Map<String, ACAQSlotDefinition> getSlots() {
        Map<String, ACAQSlotDefinition> result = new HashMap<>();
        for(Map.Entry<String, ACAQSlotDefinition> kv : wrappedConfiguration.getSlots().entrySet()) {
            if(kv.getValue().getSlotType() == ACAQDataSlot.SlotType.Input) {
                ACAQSlotDefinition sourceSlot = kv.getValue();
                ACAQSlotDefinition slot = new ACAQSlotDefinition(sourceSlot.getSlotClass(),
                        ACAQDataSlot.SlotType.Output,
                        sourceSlot.getName());
                result.put(kv.getKey(), slot);
            }
        }
        return result;
    }

    @Override
    public boolean hasSlot(String name) {
        return wrappedConfiguration.hasSlot(name);
    }

    @Override
    public void removeSlot(String name) {
        wrappedConfiguration.removeSlot(name);
    }

    @Override
    public void moveUp(String slot) {
        wrappedConfiguration.moveUp(slot);
    }

    @Override
    public void moveDown(String slot) {
        wrappedConfiguration.moveDown(slot);
    }

    @Override
    public List<String> getInputSlotOrder() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getOutputSlotOrder() {
        return wrappedConfiguration.getInputSlotOrder();
    }

    @Override
    public void addInputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addOutputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        wrappedConfiguration.addInputSlot(name, klass);
    }

    @Override
    public boolean hasInputSlots() {
        return false;
    }

    @Override
    public boolean hasOutputSlots() {
        return wrappedConfiguration.hasInputSlots();
    }

    @Override
    public boolean allowsInputSlots() {
        return false;
    }

    @Override
    public boolean allowsOutputSlots() {
        return wrappedConfiguration.allowsInputSlots();
    }

    @Override
    public boolean isInputSlotsSealed() {
        return true;
    }

    @Override
    public boolean isOutputSlotsSealed() {
        return wrappedConfiguration.isInputSlotsSealed();
    }

    @Override
    public boolean canModifyInputSlots() {
        return false;
    }

    @Override
    public boolean canModifyOutputSlots() {
        return wrappedConfiguration.canModifyInputSlots();
    }

    @Override
    public Set<Class<? extends ACAQDataSlot<?>>> getAllowedInputSlotTypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Class<? extends ACAQDataSlot<?>>> getAllowedOutputSlotTypes() {
        return wrappedConfiguration.getAllowedInputSlotTypes();
    }

    @Override
    public void setTo(ACAQSlotConfiguration configuration) {
        wrappedConfiguration.setTo(configuration);
    }

    @Override
    public void fromJson(JsonNode jsonNode) {
        // Pass to the parent configuration
        wrappedConfiguration.fromJson(jsonNode);
    }

    @Subscribe
    public void onSlotAdded(SlotAddedEvent event) {
       getEventBus().post(event);
    }

    @Subscribe
    public void onSlotRemoved(SlotRemovedEvent event) {
        getEventBus().post(event);
    }

    @Subscribe
    public void onSlotRenamed(SlotRenamedEvent event) {
        getEventBus().post(event);
    }
}
