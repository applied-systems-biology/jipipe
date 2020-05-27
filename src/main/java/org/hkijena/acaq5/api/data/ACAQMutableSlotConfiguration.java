package org.hkijena.acaq5.api.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.SlotAddedEvent;
import org.hkijena.acaq5.api.events.SlotOrderChangedEvent;
import org.hkijena.acaq5.api.events.SlotRemovedEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
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
    private boolean allowInheritedOutputSlots = false;
    private Set<Class<? extends ACAQData>> allowedInputSlotTypes;
    private Set<Class<? extends ACAQData>> allowedOutputSlotTypes;
    private int maxInputSlots = 32;
    private int maxOutputSlots = 32;

    /**
     * Creates a new instance
     */
    public ACAQMutableSlotConfiguration() {
        allowedInputSlotTypes = new HashSet<>(ACAQDatatypeRegistry.getInstance().getUnhiddenRegisteredDataTypes().values());
        allowedOutputSlotTypes = new HashSet<>(ACAQDatatypeRegistry.getInstance().getUnhiddenRegisteredDataTypes().values());
    }

    /**
     * Returns true if there is a slot with name
     *
     * @param name The name
     * @return True if a slot with the name exists
     */
    public boolean hasSlot(String name) {
        return slots.containsKey(name);
    }

    /**
     * Adds a slots
     *
     * @param name       Unique slot name
     * @param definition Defines the slot
     * @param user       if the change was triggered by a user. If false, checks for slot sealing, types, etc. do not apply
     * @return the slot definition of the added slot
     */
    public ACAQSlotDefinition addSlot(String name, ACAQSlotDefinition definition, boolean user) {
        if (!Objects.equals(name, definition.getName())) {
            definition = definition.renamedCopy(name);
        }
        if (user) {
            if (definition.getSlotType() == ACAQDataSlot.SlotType.Input &&
                    !allowedInputSlotTypes.contains(definition.getDataClass()))
                throw new UserFriendlyRuntimeException("Slot type is not accepted by this configuration!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept this type of slot.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == ACAQDataSlot.SlotType.Input && !allowInputSlots)
                throw new UserFriendlyRuntimeException("Slot configuration does not allow input slots", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept this type of slot.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == ACAQDataSlot.SlotType.Input && inputSlotsSealed)
                throw new UserFriendlyRuntimeException("Slot configuration is sealed!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept any more slots of this type.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == ACAQDataSlot.SlotType.Input &&
                    inputSlotOrder.size() >= maxInputSlots)
                throw new UserFriendlyRuntimeException("Slot already reached the limit of input slots!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept any more slots of this type.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == ACAQDataSlot.SlotType.Output &&
                    !allowedOutputSlotTypes.contains(definition.getDataClass()))
                throw new UserFriendlyRuntimeException("Slot type is not accepted by this configuration!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept this type of slot.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == ACAQDataSlot.SlotType.Output && !allowOutputSlots)
                throw new UserFriendlyRuntimeException("Slot configuration does not allow output slots", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept this type of slot.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == ACAQDataSlot.SlotType.Output && outputSlotsSealed)
                throw new UserFriendlyRuntimeException("Slot configuration is sealed!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept any more slots of this type.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == ACAQDataSlot.SlotType.Output &&
                    outputSlotOrder.size() >= maxOutputSlots)
                throw new UserFriendlyRuntimeException("Slot already reached the limit of output slots!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept any more slots of this type.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == ACAQDataSlot.SlotType.Output &&
                    !allowInheritedOutputSlots && definition.getInheritedSlot() != null &&
                    !definition.getInheritedSlot().isEmpty())
                throw new UserFriendlyRuntimeException("Slot configuration does not allow slot inheritance!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept output slots with slot inheritance.", "Contact the authors of the plugin that provides the algorithm.");
        }
        if (hasSlot(name))
            throw new UserFriendlyRuntimeException("Slot already exists!", "Unable to add slot!",
                    "Algorithm slot configuration",
                    "There is already a slot with the same name.", "Slot names have to be unique across input and output slots. Please choose another name.");

        slots.put(name, definition);
        if (definition.getSlotType() == ACAQDataSlot.SlotType.Input)
            inputSlotOrder.add(name);
        else if (definition.getSlotType() == ACAQDataSlot.SlotType.Output)
            outputSlotOrder.add(name);
        else
            throw new IllegalArgumentException("Unknown slot type!");
        getEventBus().post(new SlotAddedEvent(this, name));

        return definition;
    }

    /**
     * Removes the slot with given name
     *
     * @param name Slot name
     * @param user if the change was triggered by a user. If false, checks for slot modification, counts, etc. do not apply.
     */
    public void removeSlot(String name, boolean user) {
        ACAQSlotDefinition slot = slots.getOrDefault(name, null);
        if (slot != null) {
            if (user) {
                switch (slot.getSlotType()) {
                    case Input:
                        if (!canModifyInputSlots())
                            throw new UserFriendlyRuntimeException("Input slots can not be modified!", "Unable to remove slot!",
                                    "Algorithm slot configuration",
                                    "The algorithm is configured, so input slots cannot be removed.", "Contact the authors of the plugin that provides the algorithm.");
                        break;
                    case Output:
                        if (!canModifyOutputSlots())
                            throw new UserFriendlyRuntimeException("Output slots can not be modified!", "Unable to remove slot!",
                                    "Algorithm slot configuration",
                                    "The algorithm is configured, so output slots cannot be removed.", "Contact the authors of the plugin that provides the algorithm.");
                        break;
                    default:
                        throw new RuntimeException("Unknown slot type!");
                }
            }

            slots.remove(name);
            inputSlotOrder.remove(name);
            outputSlotOrder.remove(name);
            getEventBus().post(new SlotRemovedEvent(this, name));
        }
    }

    /**
     * @return True if there are input slots
     */
    public boolean hasInputSlots() {
        return slots.values().stream().anyMatch(x -> x.getSlotType() == ACAQDataSlot.SlotType.Input);
    }

    /**
     * @return True if there are output slots
     */
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
        for (Map.Entry<String, ACAQSlotDefinition> kv : configuration.getSlots().entrySet()) {
            slots.put(kv.getKey(), new ACAQSlotDefinition(kv.getValue()));
        }
        if (configuration instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration other = (ACAQMutableSlotConfiguration) configuration;
            this.allowedInputSlotTypes = new HashSet<>(other.allowedInputSlotTypes);
            this.allowedOutputSlotTypes = new HashSet<>(other.allowedOutputSlotTypes);
            this.allowInheritedOutputSlots = other.allowInheritedOutputSlots;
            this.allowInputSlots = other.allowInputSlots;
            this.allowOutputSlots = other.allowOutputSlots;
            this.inputSlotsSealed = other.inputSlotsSealed;
            this.outputSlotsSealed = other.outputSlotsSealed;
        }
        // Update slot order
        inputSlotOrder = new ArrayList<>(configuration.getInputSlotOrder());
        outputSlotOrder = new ArrayList<>(configuration.getOutputSlotOrder());
        for (String name : newSlots) {
            getEventBus().post(new SlotAddedEvent(this, name));
        }
        for (String name : removedSlots) {
            getEventBus().post(new SlotRemovedEvent(this, name));
        }
        getEventBus().post(new SlotOrderChangedEvent(this));
    }

    @Override
    public void fromJson(JsonNode node) {
        ObjectReader objectReader = JsonUtils.getObjectMapper().readerFor(ACAQSlotDefinition.class);
        for (Map.Entry<String, JsonNode> kv : ImmutableList.copyOf(node.fields())) {
            if (!slots.containsKey(kv.getKey())) {
                try {
                    ACAQSlotDefinition slotDefinition = objectReader.readValue(kv.getValue());
                    addSlot(kv.getKey(), slotDefinition, false);
                } catch (IOException e) {
                    throw new UserFriendlyRuntimeException(e, "Unable to read slot from JSON!", "Algorithm slot configuration", "There is essential information missing in the JSON data.",
                            "Please check if the JSON data is valid.");
                }
            } else {
                try {
                    ACAQSlotDefinition slotDefinition = objectReader.readValue(kv.getValue());
                    slots.get(kv.getKey()).setTo(slotDefinition);
                } catch (IOException e) {
                    throw new UserFriendlyRuntimeException(e, "Unable to read slot from JSON!", "Algorithm slot configuration", "There is essential information missing in the JSON data.",
                            "Please check if the JSON data is valid.");
                }
            }
        }
    }

    /**
     * Moves the specified slot up in its order
     *
     * @param slot Slot name
     */
    public void moveUp(String slot) {
        ACAQDataSlot.SlotType type = slots.get(slot).getSlotType();
        List<String> order;
        switch (type) {
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
        if (index > 0) {
            String other = order.get(index - 1);
            order.set(index - 1, slot);
            order.set(index, other);

            getEventBus().post(new SlotOrderChangedEvent(this));
        }
    }

    /**
     * Moves the specified slot down in order
     *
     * @param slot Slot name
     */
    public void moveDown(String slot) {
        ACAQDataSlot.SlotType type = slots.get(slot).getSlotType();
        List<String> order;
        switch (type) {
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
        if (index < order.size() - 1) {
            String other = order.get(index + 1);
            order.set(index + 1, slot);
            order.set(index, other);

            getEventBus().post(new SlotOrderChangedEvent(this));
        }
    }

    /**
     * Sets the input slot order. Automatically ignores missing or invalid slots.
     * Undefined slots are put at the end of the order.
     *
     * @param newOrder the order
     */
    public void trySetInputSlotOrder(List<String> newOrder) {
        ImmutableList<String> before = ImmutableList.copyOf(this.inputSlotOrder);
        this.inputSlotOrder.clear();
        for (String s : newOrder) {
            ACAQSlotDefinition slot = slots.getOrDefault(s, null);
            if (slot.getSlotType() != ACAQDataSlot.SlotType.Input)
                continue;
            if (inputSlotOrder.contains(s))
                continue;
            inputSlotOrder.add(s);
        }
        for (String s : before) {
            if (!inputSlotOrder.contains(s)) {
                inputSlotOrder.add(s);
            }
        }
        getEventBus().post(new SlotOrderChangedEvent(this));
    }

    /**
     * Sets the output slot order. Automatically ignores missing or invalid slots.
     * Undefined slots are put at the end of the order.
     *
     * @param newOrder the order
     */
    public void trySetOutputSlotOrder(List<String> newOrder) {
        ImmutableList<String> before = ImmutableList.copyOf(this.outputSlotOrder);
        this.outputSlotOrder.clear();
        for (String s : newOrder) {
            ACAQSlotDefinition slot = slots.getOrDefault(s, null);
            if (slot.getSlotType() != ACAQDataSlot.SlotType.Output)
                continue;
            if (outputSlotOrder.contains(s))
                continue;
            outputSlotOrder.add(s);
        }
        for (String s : before) {
            if (!outputSlotOrder.contains(s)) {
                outputSlotOrder.add(s);
            }
        }
        getEventBus().post(new SlotOrderChangedEvent(this));
    }

    /**
     * @return If the configuration allows input slots
     */
    public boolean allowsInputSlots() {
        return allowInputSlots;
    }

    /**
     * @return If the configuration allows output slots
     */
    public boolean allowsOutputSlots() {
        return allowOutputSlots;
    }

    /**
     * @return If input slots are unmodifiable
     */
    public boolean isInputSlotsSealed() {
        return inputSlotsSealed;
    }

    /**
     * @return If output slots are unmodifiable
     */
    public boolean isOutputSlotsSealed() {
        return outputSlotsSealed;
    }

    /**
     * @return If input slots can be modified
     */
    public boolean canModifyInputSlots() {
        return allowsInputSlots() && !isInputSlotsSealed();
    }

    /**
     * @return If output slots can be modified
     */
    public boolean canModifyOutputSlots() {
        return allowsOutputSlots() && !isOutputSlotsSealed();
    }

    /**
     * @return Allowed input slot types.
     */
    public Set<Class<? extends ACAQData>> getAllowedInputSlotTypes() {
        return allowedInputSlotTypes;
    }

    public void setAllowedInputSlotTypes(Set<Class<? extends ACAQData>> allowedInputSlotTypes) {
        this.allowedInputSlotTypes = allowedInputSlotTypes;
        getEventBus().post(new ParameterChangedEvent(this, "allowed-input-slot-types"));
    }

    /**
     * @return Allowed output slot types
     */
    public Set<Class<? extends ACAQData>> getAllowedOutputSlotTypes() {
        return allowedOutputSlotTypes;
    }

    public void setAllowedOutputSlotTypes(Set<Class<? extends ACAQData>> allowedOutputSlotTypes) {
        this.allowedOutputSlotTypes = allowedOutputSlotTypes;
        getEventBus().post(new ParameterChangedEvent(this, "allowed-output-slot-types"));
    }

    /**
     * Seals/Unseals input slots
     *
     * @param b Seals/Unseals input slots
     */
    public void setInputSealed(boolean b) {
        this.inputSlotsSealed = b;
        getEventBus().post(new ParameterChangedEvent(this, "input-sealed"));
    }

    /**
     * Seals/Unseals output slots
     *
     * @param b Seals/Unseals output slots
     */
    public void setOutputSealed(boolean b) {
        this.outputSlotsSealed = b;
        getEventBus().post(new ParameterChangedEvent(this, "output-sealed"));
    }

    /**
     * Returns if an input slot with specified type can be created
     *
     * @param acceptedDataType Slot data type
     * @return True if it can be added
     */
    public boolean canCreateCompatibleInputSlot(Class<? extends ACAQData> acceptedDataType) {
        if (!canModifyInputSlots())
            return false;
        for (Class<? extends ACAQData> allowedInputSlotType : allowedInputSlotTypes) {
            if (allowedInputSlotType.isAssignableFrom(acceptedDataType))
                return true;
        }
        return false;
    }

    /**
     * @return Maximum number of input slots
     */
    public int getMaxInputSlots() {
        return maxInputSlots;
    }

    /**
     * Sets maximum number of input slots
     *
     * @param maxInputSlots Number
     */
    public void setMaxInputSlots(int maxInputSlots) {
        this.maxInputSlots = maxInputSlots;
        getEventBus().post(new ParameterChangedEvent(this, "max-input-slots"));
    }

    /**
     * @return Maximum number of output slots
     */
    public int getMaxOutputSlots() {
        return maxOutputSlots;
    }

    /**
     * Sets maximum number of output slots
     *
     * @param maxOutputSlots Number
     */
    public void setMaxOutputSlots(int maxOutputSlots) {
        this.maxOutputSlots = maxOutputSlots;
        getEventBus().post(new ParameterChangedEvent(this, "max-output-slots"));
    }

    /**
     * @return If an input slot can be added
     */
    public boolean canAddInputSlot() {
        return allowsInputSlots() && !isInputSlotsSealed() && inputSlotOrder.size() < maxInputSlots;
    }

    /**
     * @return If an output slot can be added
     */
    public boolean canAddOutputSlot() {
        return allowsOutputSlots() && !isOutputSlotsSealed() && outputSlotOrder.size() < maxOutputSlots;
    }

    /**
     * Removes all input slots
     */
    public void clearInputSlots() {
        if (!canModifyInputSlots())
            throw new UnsupportedOperationException("Cannot modify input slots!");
        for (Map.Entry<String, ACAQSlotDefinition> entry : ImmutableList.copyOf(slots.entrySet())) {
            if (entry.getValue().getSlotType() == ACAQDataSlot.SlotType.Input) {
                removeSlot(entry.getKey(), false);
            }
        }
    }

    /**
     * Removes all output slots
     */
    public void clearOutputSlots() {
        if (!canModifyOutputSlots())
            throw new UnsupportedOperationException("Cannot modify output slots!");
        for (Map.Entry<String, ACAQSlotDefinition> entry : ImmutableList.copyOf(slots.entrySet())) {
            if (entry.getValue().getSlotType() == ACAQDataSlot.SlotType.Output) {
                removeSlot(entry.getKey(), false);
            }
        }
    }

    /**
     * @return If output slots can inherit from input slots
     */
    public boolean isAllowInheritedOutputSlots() {
        return allowInheritedOutputSlots;
    }

    /**
     * Enables/Disables if output slots can inherit from input slots
     *
     * @param allowInheritedOutputSlots Enables/Disables if output slots can inherit from input slots
     */
    public void setAllowInheritedOutputSlots(boolean allowInheritedOutputSlots) {
        this.allowInheritedOutputSlots = allowInheritedOutputSlots;
    }

    /**
     * Returns a collection of all unhidden slot data types
     *
     * @return the collection
     */
    public static Set<Class<? extends ACAQData>> getUnhiddenRegisteredDataTypes() {
        return new HashSet<>(ACAQDatatypeRegistry.getInstance().getUnhiddenRegisteredDataTypes().values());
    }

    /**
     * @return A builder for creating a configuration
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for creating a configuration
     */
    public static class Builder {
        private ACAQMutableSlotConfiguration object = new ACAQMutableSlotConfiguration();

        private Builder() {

        }

        /**
         * Adds an input slot
         *
         * @param name  Unique slot name
         * @param klass Slot data class
         * @return The builder
         */
        public Builder addInputSlot(String name, Class<? extends ACAQData> klass) {
            object.addSlot(name, new ACAQSlotDefinition(klass, ACAQDataSlot.SlotType.Input, name, null), false);
            return this;
        }

        /**
         * Adds an output slot
         *
         * @param name          Unique slot name
         * @param klass         Slot data class
         * @param inheritedSlot From which slot the data type is inherited. Slot name of an input or '*' to select the first available slot. Can be null or empty.
         * @return The builder
         */
        public Builder addOutputSlot(String name, Class<? extends ACAQData> klass, String inheritedSlot) {
            object.addSlot(name, new ACAQSlotDefinition(klass, ACAQDataSlot.SlotType.Output, name, inheritedSlot), false);
            if (inheritedSlot != null && !inheritedSlot.isEmpty()) {
                object.setAllowInheritedOutputSlots(true);
            }
            return this;
        }

        /**
         * Adds an output slot
         *
         * @param name                   Unique slot name
         * @param klass                  Slot data class
         * @param inheritedSlot          From which slot the data type is inherited. Slot name of an input or '*' to select the first available slot. Can be null or empty.
         * @param inheritanceConversions Instructions on how to convert inherited slot types.
         * @return The builder
         */
        public Builder addOutputSlot(String name, Class<? extends ACAQData> klass, String inheritedSlot, Map<Class<? extends ACAQData>, Class<? extends ACAQData>> inheritanceConversions) {
            ACAQSlotDefinition slotDefinition = object.addSlot(name, new ACAQSlotDefinition(klass, ACAQDataSlot.SlotType.Output, name, inheritedSlot), false);
            for (Map.Entry<Class<? extends ACAQData>, Class<? extends ACAQData>> entry : inheritanceConversions.entrySet()) {
                slotDefinition.getInheritanceConversions().put(ACAQDataDeclaration.getInstance(entry.getKey()),
                        ACAQDataDeclaration.getInstance(entry.getValue()));
            }
            if (inheritedSlot != null && !inheritedSlot.isEmpty()) {
                object.setAllowInheritedOutputSlots(true);
            }
            return this;
        }

        /**
         * Adds a slot
         *
         * @param definition Slot definition
         * @return The builder
         */
        public Builder addSlot(ACAQSlotDefinition definition) {
            object.addSlot(definition.getName(), definition, false);
            return this;
        }

        /**
         * Restrict the number of input slots
         *
         * @param maxCount Maximum input slot number
         * @return The builder
         */
        public Builder restrictInputSlotCount(int maxCount) {
            object.maxInputSlots = maxCount;
            return this;
        }

        /**
         * Restrict the number output slots
         *
         * @param maxCount Maximum output slot number
         * @return The builder
         */
        public Builder restrictOutputSlotCount(int maxCount) {
            object.maxOutputSlots = maxCount;
            return this;
        }

        /**
         * Disables input
         *
         * @return The builder
         */
        public Builder withoutInput() {
            object.allowInputSlots = false;
            object.inputSlotsSealed = true;
            return this;
        }

        /**
         * Disables output
         *
         * @return The builder
         */
        public Builder withoutOutput() {
            object.allowOutputSlots = false;
            object.outputSlotsSealed = true;
            return this;
        }

        /**
         * Seals input and output
         *
         * @return The builder
         */
        public Builder seal() {
            object.inputSlotsSealed = true;
            object.outputSlotsSealed = true;
            return this;
        }

        /**
         * Seals input
         *
         * @return The builder
         */
        public Builder sealInput() {
            object.inputSlotsSealed = true;
            return this;
        }

        /**
         * Seals output
         *
         * @return The builder
         */
        public Builder sealOutput() {
            object.outputSlotsSealed = true;
            return this;
        }

        /**
         * Enables/disables if slot inheritance is allowed
         *
         * @param enabled Enables/disables if slot inheritance is allowed
         * @return The builder
         */
        public Builder allowOutputSlotInheritance(boolean enabled) {
            object.setAllowInheritedOutputSlots(enabled);
            return this;
        }

        /**
         * Restricts the slot to specified data types
         *
         * @param types Allowed data types
         * @return The builder
         */
        @SafeVarargs
        public final Builder restrictInputTo(Class<? extends ACAQData>... types) {
            object.allowedInputSlotTypes = new HashSet<>(Arrays.asList(types));
            return this;
        }

        /**
         * Restricts the slot to specified data types
         *
         * @param types Allowed data types
         * @return The builder
         */
        public final Builder restrictInputTo(Collection<Class<? extends ACAQData>> types) {
            object.allowedInputSlotTypes = new HashSet<>(types);
            return this;
        }

        /**
         * Restricts the slot to specified data types
         *
         * @param types Allowed data types
         * @return The builder
         */
        @SafeVarargs
        public final Builder restrictOutputTo(Class<? extends ACAQData>... types) {
            object.allowedOutputSlotTypes = new HashSet<>(Arrays.asList(types));
            return this;
        }

        /**
         * Restricts the slot to specified data types
         *
         * @param types Allowed data types
         * @return The builder
         */
        public final Builder restrictOutputTo(Collection<Class<? extends ACAQData>> types) {
            object.allowedOutputSlotTypes = new HashSet<>(types);
            return this;
        }

        /**
         * @return The {@link ACAQMutableSlotConfiguration} instance
         */
        public ACAQMutableSlotConfiguration build() {
            return object;
        }
    }

}
