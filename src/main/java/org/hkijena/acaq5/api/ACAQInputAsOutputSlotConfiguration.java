package org.hkijena.acaq5.api;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.SlotAddedEvent;
import org.hkijena.acaq5.api.events.SlotRemovedEvent;
import org.hkijena.acaq5.api.events.SlotRenamedEvent;

import java.util.HashMap;
import java.util.Map;

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
