package org.hkijena.acaq5.api.compartments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.compartments.dataslots.ACAQPreprocessingOutputDataSlot;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.events.SlotAddedEvent;
import org.hkijena.acaq5.api.events.SlotRemovedEvent;
import org.hkijena.acaq5.api.events.SlotRenamedEvent;

import java.util.*;

@JsonSerialize(using = ACAQSlotConfiguration.Serializer.class)
public class ACAQAnalysisInputSlotConfiguration extends ACAQMutableSlotConfiguration {

    private ACAQMutableSlotConfiguration wrappedConfiguration;

    public ACAQAnalysisInputSlotConfiguration(ACAQMutableSlotConfiguration wrappedConfiguration) {
        this.wrappedConfiguration = wrappedConfiguration;
        wrappedConfiguration.getEventBus().register(this);
    }

    @Override
    public Map<String, ACAQSlotDefinition> getSlots() {
        Map<String, ACAQSlotDefinition> result = new HashMap<>();
        for(Map.Entry<String, ACAQSlotDefinition> kv : wrappedConfiguration.getSlots().entrySet()) {
           if(kv.getValue().getSlotType() == ACAQDataSlot.SlotType.Output) {
                ACAQSlotDefinition sourceSlot = kv.getValue();
                ACAQSlotDefinition slot = new ACAQSlotDefinition(sourceSlot.getSlotClass(),
                        ACAQDataSlot.SlotType.Input,
                        sourceSlot.getName());
                result.put(kv.getKey(), slot);
            }
        }
        for (Map.Entry<String, ACAQSlotDefinition> entry : super.getSlots().entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override
    public boolean hasSlot(String name) {
        if(super.hasSlot(name))
            return true;
        return wrappedConfiguration.hasSlot(name);
    }

    @Override
    public void removeSlot(String name) {
        if(super.hasSlot(name)) {
            super.removeSlot(name);
        }
        else {
            wrappedConfiguration.removeSlot(name);
        }
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
        return super.getInputSlotOrder();
    }

    @Override
    public List<String> getOutputSlotOrder() {
        return wrappedConfiguration.getInputSlotOrder();
    }

    @Override
    public void addInputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        super.addInputSlot(name, klass);
    }

    @Override
    public void addOutputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        wrappedConfiguration.addInputSlot(name, klass);
    }

    @Override
    public boolean hasInputSlots() {
        return true;
    }

    @Override
    public boolean hasOutputSlots() {
        return wrappedConfiguration.hasInputSlots();
    }

    @Override
    public boolean allowsInputSlots() {
        return true;
    }

    @Override
    public boolean allowsOutputSlots() {
        return wrappedConfiguration.allowsInputSlots();
    }

    @Override
    public boolean isInputSlotsSealed() {
        return false;
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
        return Collections.singleton(ACAQPreprocessingOutputDataSlot.class);
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
