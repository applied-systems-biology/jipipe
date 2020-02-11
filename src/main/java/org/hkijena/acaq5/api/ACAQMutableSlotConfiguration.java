package org.hkijena.acaq5.api;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.events.SlotAddedEvent;
import org.hkijena.acaq5.api.events.SlotRemovedEvent;

import java.util.*;

/**
 * A schema for slots
 */
public class ACAQMutableSlotConfiguration extends ACAQSlotConfiguration {
    private Map<String, ACAQSlotDefinition> slots = new HashMap<>();

    private boolean inputSlotsSealed = false;
    private boolean outputSlotsSealed = false;
    private boolean allowInputSlots = true;
    private boolean allowOutputSlots = true;
    private Set<Class<? extends ACAQDataSlot<?>>> allowedInputSlotTypes;
    private Set<Class<? extends ACAQDataSlot<?>>> allowedOutputSlotTypes;

    private ACAQMutableSlotConfiguration() {
        allowedInputSlotTypes = new HashSet<>(ACAQRegistryService.getInstance().getDatatypeRegistry().getRegisteredSlotDataTypes().values());
        allowedOutputSlotTypes = new HashSet<>(ACAQRegistryService.getInstance().getDatatypeRegistry().getRegisteredSlotDataTypes().values());
    }

    public boolean hasSlot(String name) {
        return slots.containsKey(name);
    }

    public void addInputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        if(!allowedInputSlotTypes.contains(klass))
            throw new RuntimeException("Slot type is not accepted by this configuration!");
        if(!allowInputSlots)
            throw new RuntimeException("Slot configuration does not allow input slots");
        if(inputSlotsSealed)
            throw new RuntimeException("Slot configuration is sealed!");
        if(hasSlot(name))
            throw new RuntimeException("Slot already exists!");
        slots.put(name, new ACAQSlotDefinition(klass, ACAQDataSlot.SlotType.Input, name));
        getEventBus().post(new SlotAddedEvent(this, name));
    }

    public void addOutputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
        if(!allowedOutputSlotTypes.contains(klass))
            throw new RuntimeException("Slot type is not accepted by this configuration!");
        if(!allowOutputSlots)
            throw new RuntimeException("Slot configuration does not allow output slots");
        if(outputSlotsSealed)
            throw new RuntimeException("Slot configuration is sealed!");
        if(hasSlot(name))
            throw new RuntimeException("Slot already exists!");
        slots.put(name, new ACAQSlotDefinition(klass, ACAQDataSlot.SlotType.Output, name));
        getEventBus().post(new SlotAddedEvent(this, name));
    }

    public void removeSlot(String name) {
        if(slots.remove(name) != null) {
            getEventBus().post(new SlotRemovedEvent(this, name));
        }
    }

    public boolean hasInputSlots() {
        return slots.values().stream().anyMatch(x -> x.getSlotType() == ACAQDataSlot.SlotType.Input);
    }

    public boolean hasOutputSlots() {
        return slots.values().stream().anyMatch(x -> x.getSlotType() == ACAQDataSlot.SlotType.Output);
    }

    @Override
    public Map<String, ACAQSlotDefinition> getSlots() {
        return Collections.unmodifiableMap(slots);
    }

    public boolean allowsInputSlots() {
        return allowInputSlots;
    }

    public boolean allowsOutputSlots() {
        return allowOutputSlots;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isInputSlotsSealed() {
        return inputSlotsSealed;
    }

    public boolean isOutputSlotsSealed() {
        return outputSlotsSealed;
    }

    public Set<Class<? extends ACAQDataSlot<?>>> getAllowedInputSlotTypes() {
        return allowedInputSlotTypes;
    }

    public Set<Class<? extends ACAQDataSlot<?>>> getAllowedOutputSlotTypes() {
        return allowedOutputSlotTypes;
    }

    public static class Builder {
        private ACAQMutableSlotConfiguration object = new ACAQMutableSlotConfiguration();

        private Builder() {

        }

        public Builder addInputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
            object.addInputSlot(name, klass);
            return this;
        }

        public Builder addOutputSlot(String name, Class<? extends ACAQDataSlot<?>> klass) {
            object.addOutputSlot(name, klass);
            return this;
        }

        public Builder withoutInput() {
            object.allowInputSlots = false;
            object.inputSlotsSealed = true;
            return this;
        }

        public Builder withoutOutput() {
            object.allowOutputSlots = false;
            object.outputSlotsSealed = true;
            return this;
        }

        public Builder seal() {
            object.inputSlotsSealed = true;
            object.outputSlotsSealed = true;
            return this;
        }

        public Builder sealInput() {
            object.inputSlotsSealed = true;
            return this;
        }

        public Builder sealOutput() {
            object.outputSlotsSealed = true;
            return this;
        }

        @SafeVarargs
        public final Builder restrictInputTo(Class<? extends ACAQDataSlot<?>>... types) {
            object.allowedInputSlotTypes = new HashSet<>(Arrays.asList(types));
            return this;
        }

        @SafeVarargs
        public final Builder restrictOutputTo(Class<? extends ACAQDataSlot<?>>... types) {
            object.allowedOutputSlotTypes = new HashSet<>(Arrays.asList(types));
            return this;
        }

        public ACAQMutableSlotConfiguration build() {
            return object;
        }
    }

}
