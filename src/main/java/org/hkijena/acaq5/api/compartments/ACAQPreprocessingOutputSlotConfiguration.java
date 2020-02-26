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
public class ACAQPreprocessingOutputSlotConfiguration extends ACAQMutableSlotConfiguration {

    private ACAQMutableSlotConfiguration wrappedConfiguration;

    public ACAQPreprocessingOutputSlotConfiguration(ACAQMutableSlotConfiguration wrappedConfiguration) {
        this.wrappedConfiguration = wrappedConfiguration;
        wrappedConfiguration.getEventBus().register(this);
    }

    @Override
    public Map<String, ACAQSlotDefinition> getSlots() {
        return wrappedConfiguration.getSlots();
    }

    @Override
    public boolean hasSlot(String name) {
        if(name.equals("Data"))
            return true;
        return wrappedConfiguration.hasSlot(name);
    }

    @Override
    public void removeSlot(String name) {
        if(name.equals("Data"))
            throw new UnsupportedOperationException();
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
        return wrappedConfiguration.getInputSlotOrder();
    }

    @Override
    public List<String> getOutputSlotOrder() {
        return Arrays.asList("Data");
    }

    @Override
    public void addInputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        wrappedConfiguration.addInputSlot(name, klass);
    }

    @Override
    public void addOutputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
       throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasInputSlots() {
        return wrappedConfiguration.hasInputSlots();
    }

    @Override
    public boolean hasOutputSlots() {
        return true;
    }

    @Override
    public boolean allowsInputSlots() {
        return wrappedConfiguration.allowsInputSlots();
    }

    @Override
    public boolean allowsOutputSlots() {
        return true;
    }

    @Override
    public boolean isInputSlotsSealed() {
        return  wrappedConfiguration.isInputSlotsSealed();
    }

    @Override
    public boolean isOutputSlotsSealed() {
        return true;
    }

    @Override
    public boolean canModifyInputSlots() {
        return wrappedConfiguration.canModifyInputSlots();
    }

    @Override
    public boolean canModifyOutputSlots() {
        return false;
    }

    @Override
    public Set<Class<? extends ACAQDataSlot<?>>> getAllowedInputSlotTypes() {
        return wrappedConfiguration.getAllowedInputSlotTypes();
    }

    @Override
    public Set<Class<? extends ACAQDataSlot<?>>> getAllowedOutputSlotTypes() {
        return Collections.singleton(ACAQPreprocessingOutputDataSlot.class);
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
