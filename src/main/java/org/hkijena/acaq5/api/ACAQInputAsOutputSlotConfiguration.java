package org.hkijena.acaq5.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.SlotAddedEvent;
import org.hkijena.acaq5.api.events.SlotRemovedEvent;
import org.hkijena.acaq5.api.events.SlotRenamedEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonSerialize(using = ACAQSlotConfiguration.Serializer.class)
public class ACAQInputAsOutputSlotConfiguration extends ACAQSlotConfiguration {

    private ACAQSlotConfiguration slotConfiguration;

    public ACAQInputAsOutputSlotConfiguration(ACAQSlotConfiguration slotConfiguration) {
        this.slotConfiguration = slotConfiguration;
        slotConfiguration.getEventBus().register(this);
    }

    @Override
    public Map<String, ACAQSlotDefinition> getSlots() {
        Map<String, ACAQSlotDefinition> result = new HashMap<>();
        for(Map.Entry<String, ACAQSlotDefinition> kv : slotConfiguration.getSlots().entrySet()) {
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
    public List<String> getInputSlotOrder() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getOutputSlotOrder() {
        return slotConfiguration.getInputSlotOrder();
    }

    @Override
    public void setTo(ACAQSlotConfiguration configuration) {
        slotConfiguration.setTo(configuration);
    }

    @Override
    public void fromJson(JsonNode jsonNode) {
        // Pass to the parent configuration
        slotConfiguration.fromJson(jsonNode);
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
