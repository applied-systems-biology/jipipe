package org.hkijena.acaq5.api.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.events.SlotAddedEvent;
import org.hkijena.acaq5.api.events.SlotOrderChangedEvent;
import org.hkijena.acaq5.api.events.SlotRemovedEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A schema for slots
 */
@JsonSerialize(using = ACAQSlotConfiguration.Serializer.class)
public class ACAQMutableSlotConfiguration extends ACAQSlotConfiguration {
    private Map<String, ACAQSlotDefinition> slots = new HashMap<>();
    private List<String> inputSlotOrder = new ArrayList<>();
    private List<String> outputSlotOrder = new ArrayList<>();

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
        inputSlotOrder.add(name);
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
        outputSlotOrder.add(name);
        getEventBus().post(new SlotAddedEvent(this, name));
    }

    public void removeSlot(String name) {

        ACAQSlotDefinition slot = slots.getOrDefault(name, null);
        if(slot != null) {
            switch (slot.getSlotType()) {
                case Input:
                    if(!canModifyInputSlots())
                        throw new RuntimeException("Input slots can not be modified!");
                    break;
                case Output:
                    if(!canModifyOutputSlots())
                        throw new RuntimeException("Output slots can not be modified!");
                    break;
                default:
                    throw new RuntimeException("Unknown slot type!");
            }

            slots.remove(name);
            inputSlotOrder.remove(name);
            outputSlotOrder.remove(name);
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

    @Override
    public List<String> getInputSlotOrder() {
        return Collections.unmodifiableList(inputSlotOrder);
    }

    @Override
    public List<String> getOutputSlotOrder() {
        return Collections.unmodifiableList(outputSlotOrder);
    }

    @Override
    public void setTo(ACAQSlotConfiguration configuration) {
        slots.clear();
        List<String> newSlots = configuration.getSlots().keySet().stream().filter(s -> !slots.containsKey(s)).collect(Collectors.toList());
        List<String> removedSlots = slots.keySet().stream().filter(s -> !configuration.getSlots().containsKey(s)).collect(Collectors.toList());
        for(Map.Entry<String, ACAQSlotDefinition> kv : configuration.getSlots().entrySet()) {
            slots.put(kv.getKey(), new ACAQSlotDefinition(kv.getValue().getSlotClass(), kv.getValue().getSlotType(), kv.getKey()));
        }
        if(configuration instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration other = (ACAQMutableSlotConfiguration)configuration;
            this.allowedInputSlotTypes = new HashSet<>(other.allowedInputSlotTypes);
            this.allowedOutputSlotTypes = new HashSet<>(other.allowedOutputSlotTypes);
            this.allowInputSlots = other.allowInputSlots;
            this.allowOutputSlots = other.allowOutputSlots;
            this.inputSlotsSealed = other.inputSlotsSealed;
            this.outputSlotsSealed = other.outputSlotsSealed;
        }
        // Update slot order
        inputSlotOrder = new ArrayList<>(configuration.getInputSlotOrder());
        outputSlotOrder = new ArrayList<>(configuration.getOutputSlotOrder());
        for(String name : newSlots) {
            getEventBus().post(new SlotAddedEvent(this, name));
        }
        for(String name : removedSlots) {
            getEventBus().post(new SlotRemovedEvent(this, name));
        }
        getEventBus().post(new SlotOrderChangedEvent(this));
    }

    @Override
    public void fromJson(JsonNode node) {
        for(Map.Entry<String, JsonNode> kv : ImmutableList.copyOf(node.fields())) {
            if(!slots.containsKey(kv.getKey())) {
                String name = kv.getValue().get("name").asText();
                Class<? extends ACAQDataSlot<?>> klass = ACAQRegistryService.getInstance().getDatatypeRegistry()
                        .findDataSlotClass(kv.getValue().get("slot-class").asText());
                if(kv.getValue().get("slot-type").asText().equalsIgnoreCase("input")) {
                    addInputSlot(name, klass);
                }
                else {
                    addOutputSlot(name, klass);
                }
            }
        }
    }

    public void moveUp(String slot) {
        ACAQDataSlot.SlotType type = slots.get(slot).getSlotType();
        List<String> order;
        switch(type) {
            case Input:
                order = inputSlotOrder;
                break;
            case Output:
                order = outputSlotOrder;
                break;
            default:
                throw new RuntimeException("Unknown slot type!");
        }

        int index = order.indexOf(slot);
        if(index > 0) {
            String other = order.get(index - 1);
            order.set(index - 1, slot);
            order.set(index, other);

            getEventBus().post(new SlotOrderChangedEvent(this));
        }
    }

    public void moveDown(String slot) {
        ACAQDataSlot.SlotType type = slots.get(slot).getSlotType();
        List<String> order;
        switch(type) {
            case Input:
                order = inputSlotOrder;
                break;
            case Output:
                order = outputSlotOrder;
                break;
            default:
                throw new RuntimeException("Unknown slot type!");
        }

        int index = order.indexOf(slot);
        if(index < order.size() - 1) {
            String other = order.get(index + 1);
            order.set(index + 1, slot);
            order.set(index, other);

            getEventBus().post(new SlotOrderChangedEvent(this));
        }
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

    public boolean canModifyInputSlots() {
        return allowsInputSlots() && !isInputSlotsSealed();
    }

    public boolean canModifyOutputSlots() {
        return allowsOutputSlots() && !isOutputSlotsSealed();
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
