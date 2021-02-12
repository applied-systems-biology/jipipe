/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.utils.JsonUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A schema for slots
 */
@JsonSerialize(using = JIPipeSlotConfiguration.Serializer.class)
public class JIPipeDefaultMutableSlotConfiguration implements JIPipeMutableSlotConfiguration {
    private final EventBus eventBus = new EventBus();
    private Map<String, JIPipeDataSlotInfo> inputSlots = new HashMap<>();
    private Map<String, JIPipeDataSlotInfo> outputSlots = new HashMap<>();
    private List<String> inputSlotOrder = new ArrayList<>();
    private List<String> outputSlotOrder = new ArrayList<>();

    private boolean inputSlotsSealed = false;
    private boolean outputSlotsSealed = false;
    private boolean allowInputSlots = true;
    private boolean allowOutputSlots = true;
    private boolean allowInheritedOutputSlots = false;
    private Set<Class<? extends JIPipeData>> allowedInputSlotTypes;
    private Set<Class<? extends JIPipeData>> allowedOutputSlotTypes;
    private int maxInputSlots = Integer.MAX_VALUE;
    private int maxOutputSlots = Integer.MAX_VALUE;

    /**
     * Creates a new instance
     */
    public JIPipeDefaultMutableSlotConfiguration() {
        allowedInputSlotTypes = new HashSet<>(JIPipe.getDataTypes().getUnhiddenRegisteredDataTypes().values());
        allowedOutputSlotTypes = new HashSet<>(JIPipe.getDataTypes().getUnhiddenRegisteredDataTypes().values());
    }

    /**
     * Returns a collection of all unhidden slot data types
     *
     * @return the collection
     */
    public static Set<Class<? extends JIPipeData>> getUnhiddenRegisteredDataTypes() {
        return new HashSet<>(JIPipe.getDataTypes().getUnhiddenRegisteredDataTypes().values());
    }

    /**
     * @return A builder for creating a configuration
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if there is an input slot with name
     *
     * @param name The name
     * @return True if a slot with the name exists
     */
    public boolean hasInputSlot(String name) {
        return inputSlots.containsKey(name);
    }

    /**
     * Returns true if there is an output slot with name
     *
     * @param name The name
     * @return True if a slot with the name exists
     */
    public boolean hasOutputSlot(String name) {
        return outputSlots.containsKey(name);
    }

    /**
     * Adds a slots
     *
     * @param name       Unique slot name
     * @param definition Defines the slot
     * @param user       if the change was triggered by a user. If false, checks for slot sealing, types, etc. do not apply
     * @return the slot definition of the added slot
     */
    public JIPipeDataSlotInfo addSlot(String name, JIPipeDataSlotInfo definition, boolean user) {
        if (!Objects.equals(name, definition.getName())) {
            definition = definition.renamedCopy(name);
        }
        if (user) {
            if (definition.getSlotType() == JIPipeSlotType.Input &&
                    !allowedInputSlotTypes.contains(definition.getDataClass()))
                throw new UserFriendlyRuntimeException("Slot type is not accepted by this configuration!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept this type of slot.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == JIPipeSlotType.Input && !allowInputSlots)
                throw new UserFriendlyRuntimeException("Slot configuration does not allow input slots", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept this type of slot.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == JIPipeSlotType.Input && inputSlotsSealed)
                throw new UserFriendlyRuntimeException("Slot configuration is sealed!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept any more slots of this type.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == JIPipeSlotType.Input &&
                    inputSlotOrder.size() >= maxInputSlots)
                throw new UserFriendlyRuntimeException("Slot already reached the limit of input slots!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept any more slots of this type.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == JIPipeSlotType.Output &&
                    !allowedOutputSlotTypes.contains(definition.getDataClass()))
                throw new UserFriendlyRuntimeException("Slot type is not accepted by this configuration!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept this type of slot.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == JIPipeSlotType.Output && !allowOutputSlots)
                throw new UserFriendlyRuntimeException("Slot configuration does not allow output slots", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept this type of slot.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == JIPipeSlotType.Output && outputSlotsSealed)
                throw new UserFriendlyRuntimeException("Slot configuration is sealed!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept any more slots of this type.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == JIPipeSlotType.Output &&
                    outputSlotOrder.size() >= maxOutputSlots)
                throw new UserFriendlyRuntimeException("Slot already reached the limit of output slots!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept any more slots of this type.", "Contact the authors of the plugin that provides the algorithm.");
            if (definition.getSlotType() == JIPipeSlotType.Output &&
                    !allowInheritedOutputSlots && definition.getInheritedSlot() != null &&
                    !definition.getInheritedSlot().isEmpty())
                throw new UserFriendlyRuntimeException("Slot configuration does not allow slot inheritance!", "Unable to add slot!",
                        "Algorithm slot configuration",
                        "The algorithm is configured to not accept output slots with slot inheritance.", "Contact the authors of the plugin that provides the algorithm.");
        }
        if ((definition.isInput() && hasInputSlot(name)) || (definition.isOutput() && hasOutputSlot(name)))
            throw new UserFriendlyRuntimeException("Slot already exists!", "Unable to add slot!",
                    "Algorithm slot configuration",
                    "There is already a slot with the same name.", "Slot names have to be unique across input and output slots. Please choose another name.");


        if (definition.getSlotType() == JIPipeSlotType.Input) {
            inputSlots.put(name, definition);
            inputSlotOrder.add(name);
        } else if (definition.getSlotType() == JIPipeSlotType.Output) {
            outputSlots.put(name, definition);
            outputSlotOrder.add(name);
        } else
            throw new IllegalArgumentException("Unknown slot type!");
        getEventBus().post(new SlotsChangedEvent(this));

        return definition;
    }

    @Override
    public void toJson(JsonGenerator jsonGenerator) throws IOException, JsonProcessingException {
        jsonGenerator.writeStartObject();

        {
            jsonGenerator.writeFieldName("input");
            jsonGenerator.writeStartObject();
            for (String key : getInputSlotOrder()) {
                jsonGenerator.writeObjectField(key, inputSlots.get(key));
            }
            jsonGenerator.writeEndObject();
        }
        {
            jsonGenerator.writeFieldName("output");
            jsonGenerator.writeStartObject();
            for (String key : getOutputSlotOrder()) {
                jsonGenerator.writeObjectField(key, outputSlots.get(key));
            }
            jsonGenerator.writeEndObject();
        }

        jsonGenerator.writeEndObject();
    }

    /**
     * Removes the slot with given name
     *
     * @param name Slot name
     * @param user if the change was triggered by a user. If false, checks for slot modification, counts, etc. do not apply.
     */
    public void removeInputSlot(String name, boolean user) {
        JIPipeDataSlotInfo slot = inputSlots.getOrDefault(name, null);
        if (slot != null) {
            if (user) {
                if (!canModifyInputSlots())
                    throw new UserFriendlyRuntimeException("Input slots can not be modified!", "Unable to remove slot!",
                            "Algorithm slot configuration",
                            "The algorithm is configured, so input slots cannot be removed.", "Contact the authors of the plugin that provides the algorithm.");
                if(!slot.isUserModifiable())
                    throw new UserFriendlyRuntimeException("Input slots can not be modified!", "Unable to remove slot!",
                            "Algorithm slot configuration",
                            "The slot is configured, so it cannot be removed.", "Contact the authors of the plugin that provides the algorithm.");
            }

            inputSlotOrder.remove(name);
            inputSlots.remove(name);
            getEventBus().post(new SlotsChangedEvent(this));
        }
    }

    /**
     * Adds a new input slot
     *
     * @param name      the name
     * @param dataClass the data type
     * @param user      if the user triggered this
     * @return the slot info
     */
    public JIPipeDataSlotInfo addInputSlot(String name, Class<? extends JIPipeData> dataClass, boolean user) {
        JIPipeDataSlotInfo info = new JIPipeDataSlotInfo(dataClass, JIPipeSlotType.Input, name, null);
        return addSlot(name, info, user);
    }

    /**
     * Adds a new input slot
     *
     * @param name      the name
     * @param dataClass the data type
     * @param inherited inherited slot can be null or '*' or the slot name
     * @param user      if the user triggered this
     * @return the slot info
     */
    public JIPipeDataSlotInfo addOutputSlot(String name, Class<? extends JIPipeData> dataClass, String inherited, boolean user) {
        JIPipeDataSlotInfo info = new JIPipeDataSlotInfo(dataClass, JIPipeSlotType.Output, name, inherited);
        return addSlot(name, info, user);
    }

    /**
     * Removes the slot with given name
     *
     * @param name Slot name
     * @param user if the change was triggered by a user. If false, checks for slot modification, counts, etc. do not apply.
     */
    public void removeOutputSlot(String name, boolean user) {
        JIPipeDataSlotInfo slot = outputSlots.getOrDefault(name, null);
        if (slot != null) {
            if (user) {
                if (!canModifyOutputSlots())
                    throw new UserFriendlyRuntimeException("Output slots can not be modified!", "Unable to remove slot!",
                            "Algorithm slot configuration",
                            "The algorithm is configured, so output slots cannot be removed.", "Contact the authors of the plugin that provides the algorithm.");
                if(!slot.isUserModifiable())
                    throw new UserFriendlyRuntimeException("Input slots can not be modified!", "Unable to remove slot!",
                            "Algorithm slot configuration",
                            "The slot is configured, so it cannot be removed.", "Contact the authors of the plugin that provides the algorithm.");
            }

            outputSlots.remove(name);
            outputSlotOrder.remove(name);
            getEventBus().post(new SlotsChangedEvent(this));
        }
    }

    @Override
    public Map<String, JIPipeDataSlotInfo> getInputSlots() {
        return Collections.unmodifiableMap(inputSlots);
    }

    @Override
    public Map<String, JIPipeDataSlotInfo> getOutputSlots() {
        return Collections.unmodifiableMap(outputSlots);
    }

    @Override
    public JIPipeSlotConfiguration duplicate() {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = new JIPipeDefaultMutableSlotConfiguration();
        slotConfiguration.setTo(this);
        return slotConfiguration;
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
    public void setTo(JIPipeSlotConfiguration configuration) {
        inputSlots.clear();
        outputSlots.clear();
        inputSlotOrder.clear();
        outputSlotOrder.clear();
        for (Map.Entry<String, JIPipeDataSlotInfo> kv : configuration.getInputSlots().entrySet()) {
            inputSlots.put(kv.getKey(), new JIPipeDataSlotInfo(kv.getValue()));
        }
        for (Map.Entry<String, JIPipeDataSlotInfo> kv : configuration.getOutputSlots().entrySet()) {
            outputSlots.put(kv.getKey(), new JIPipeDataSlotInfo(kv.getValue()));
        }
        inputSlotOrder = new ArrayList<>(configuration.getInputSlotOrder());
        outputSlotOrder = new ArrayList<>(configuration.getOutputSlotOrder());
        if (configuration instanceof JIPipeMutableSlotConfiguration) {
            JIPipeMutableSlotConfiguration other = (JIPipeMutableSlotConfiguration) configuration;
            this.allowedInputSlotTypes = new HashSet<>(other.getAllowedInputSlotTypes());
            this.allowedOutputSlotTypes = new HashSet<>(other.getAllowedOutputSlotTypes());
            this.allowInheritedOutputSlots = other.isAllowInheritedOutputSlots();
            this.allowInputSlots = other.allowsInputSlots();
            this.allowOutputSlots = other.allowsOutputSlots();
            this.inputSlotsSealed = other.isInputSlotsSealed();
            this.outputSlotsSealed = other.isOutputSlotsSealed();
        }

        getEventBus().post(new SlotsChangedEvent(this));
    }

    @Override
    public void fromJson(JsonNode node) {
        ObjectReader objectReader = JsonUtils.getObjectMapper().readerFor(JIPipeDataSlotInfo.class);
        JsonNode inputsNode = node.path("input");
        JsonNode outputsNode = node.path("output");
        if (!inputsNode.isMissingNode()) {
            Set<String> definedSlots = new HashSet<>();
            List<String> order = new ArrayList<>();
            for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(inputsNode.fields())) {
                order.add(entry.getKey());
                try {
                    definedSlots.add(entry.getKey());
                    JIPipeDataSlotInfo slotDefinition = objectReader.readValue(entry.getValue());
                    JIPipeDataSlotInfo existing = inputSlots.getOrDefault(entry.getKey(), null);
                    if (existing != null) {
                        existing.copyMetadata(slotDefinition);
                    } else if (!inputSlotsSealed) {
                        addSlot(entry.getKey(), slotDefinition, false);
                    }
                } catch (IOException e) {
                    throw new UserFriendlyRuntimeException(e, "Unable to read slot from JSON!", "Algorithm slot configuration", "There is essential information missing in the JSON data.",
                            "Please check if the JSON data is valid.");
                }
            }
            if (!inputSlotsSealed) {
                Set<String> toRemove = inputSlots.keySet().stream().filter(s -> !definedSlots.contains(s)).collect(Collectors.toSet());
                for (String s : toRemove) {
                    removeInputSlot(s, false);
                }
            }
            trySetInputSlotOrder(order);
        }
        if (!outputsNode.isMissingNode()) {
            Set<String> definedSlots = new HashSet<>();
            List<String> order = new ArrayList<>();
            for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(outputsNode.fields())) {
                order.add(entry.getKey());
                try {
                    definedSlots.add(entry.getKey());
                    JIPipeDataSlotInfo slotDefinition = objectReader.readValue(entry.getValue());
                    JIPipeDataSlotInfo existing = outputSlots.getOrDefault(entry.getKey(), null);
                    if (existing != null) {
                        existing.copyMetadata(slotDefinition);
                    } else if (!outputSlotsSealed) {
                        addSlot(entry.getKey(), slotDefinition, false);
                    }
                } catch (IOException e) {
                    throw new UserFriendlyRuntimeException(e, "Unable to read slot from JSON!", "Algorithm slot configuration", "There is essential information missing in the JSON data.",
                            "Please check if the JSON data is valid.");
                }
            }
            if (!outputSlotsSealed) {
                Set<String> toRemove = inputSlots.keySet().stream().filter(s -> !definedSlots.contains(s)).collect(Collectors.toSet());
                for (String s : toRemove) {
                    removeOutputSlot(s, false);
                }
            }
            trySetOutputSlotOrder(order);
        }
        if (inputsNode.isMissingNode() && outputsNode.isMissingNode()) {
            ImmutableList<Map.Entry<String, JsonNode>> entries = ImmutableList.copyOf(node.fields());
            if (!entries.isEmpty()) {
                System.err.println("DefaultMutableSlotConfiguration attempts to import slot configuration via legacy method. This is a result of refactoring during development.");
                try {
                    for (Map.Entry<String, JsonNode> entry : entries) {
                        JIPipeDataSlotInfo slotDefinition = objectReader.readValue(entry.getValue());
                        JIPipeDataSlotInfo existing;
                        if (slotDefinition.isInput())
                            existing = inputSlots.getOrDefault(entry.getKey(), null);
                        else
                            existing = outputSlots.getOrDefault(entry.getKey(), null);
                        if (existing != null) {
                            existing.copyMetadata(slotDefinition);
                        } else {
                            addSlot(entry.getKey(), slotDefinition, false);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Moves the specified slot up in its order
     *
     * @param slot Slot name
     * @param type the slot type
     */
    public void moveUp(String slot, JIPipeSlotType type) {
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

            getEventBus().post(new SlotsChangedEvent(this));
        }
    }

    /**
     * Moves the specified slot down in its order
     *
     * @param slot Slot name
     * @param type the slot type
     */
    public void moveDown(String slot, JIPipeSlotType type) {
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

            getEventBus().post(new SlotsChangedEvent(this));
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
            JIPipeDataSlotInfo slot = inputSlots.getOrDefault(s, null);
            if (slot == null)
                continue;
            if (slot.getSlotType() != JIPipeSlotType.Input)
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
        getEventBus().post(new SlotsChangedEvent(this));
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
            JIPipeDataSlotInfo slot = outputSlots.getOrDefault(s, null);
            if (slot == null)
                continue;
            if (slot.getSlotType() != JIPipeSlotType.Output)
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
        getEventBus().post(new SlotsChangedEvent(this));
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
    public Set<Class<? extends JIPipeData>> getAllowedInputSlotTypes() {
        return allowedInputSlotTypes;
    }

    public void setAllowedInputSlotTypes(Set<Class<? extends JIPipeData>> allowedInputSlotTypes) {
        this.allowedInputSlotTypes = allowedInputSlotTypes;

    }

    /**
     * @return Allowed output slot types
     */
    public Set<Class<? extends JIPipeData>> getAllowedOutputSlotTypes() {
        return allowedOutputSlotTypes;
    }

    public void setAllowedOutputSlotTypes(Set<Class<? extends JIPipeData>> allowedOutputSlotTypes) {
        this.allowedOutputSlotTypes = allowedOutputSlotTypes;

    }

    /**
     * Seals/Unseals input slots
     *
     * @param b Seals/Unseals input slots
     */
    public void setInputSealed(boolean b) {
        this.inputSlotsSealed = b;

    }

    /**
     * Seals/Unseals output slots
     *
     * @param b Seals/Unseals output slots
     */
    public void setOutputSealed(boolean b) {
        this.outputSlotsSealed = b;

    }

    /**
     * Returns if an input slot with specified type can be created
     *
     * @param acceptedDataType Slot data type
     * @return True if it can be added
     */
    @Override
    public boolean canCreateCompatibleInputSlot(Class<? extends JIPipeData> acceptedDataType) {
        if (!canModifyInputSlots())
            return false;
        for (Class<? extends JIPipeData> allowedInputSlotType : allowedInputSlotTypes) {
            if (allowedInputSlotType.isAssignableFrom(acceptedDataType))
                return true;
        }
        return false;
    }

    /**
     * Returns if an output slot with specified type can be created
     *
     * @param acceptedDataType Slot data type
     * @return True if it can be added
     */
    @Override
    public boolean canCreateCompatibleOutputSlot(Class<? extends JIPipeData> acceptedDataType) {
        if (!canModifyOutputSlots())
            return false;
        for (Class<? extends JIPipeData> allowedOutputSlotType : allowedOutputSlotTypes) {
            if (allowedOutputSlotType.isAssignableFrom(acceptedDataType))
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
     *
     * @param user if done by a user
     */
    public void clearInputSlots(boolean user) {
        if (user && !canModifyInputSlots())
            throw new UnsupportedOperationException("Cannot modify input slots!");

        inputSlots.clear();
        inputSlotOrder.clear();
        getEventBus().post(new SlotsChangedEvent(this));
    }

    /**
     * Removes all output slots
     *
     * @param user if done by a user
     */
    public void clearOutputSlots(boolean user) {
        if (user && !canModifyOutputSlots())
            throw new UnsupportedOperationException("Cannot modify output slots!");
        outputSlots.clear();
        outputSlotOrder.clear();
        getEventBus().post(new SlotsChangedEvent(this));
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

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * A builder for creating a configuration
     */
    public static class Builder {
        private JIPipeDefaultMutableSlotConfiguration object = new JIPipeDefaultMutableSlotConfiguration();

        private Builder() {

        }

        /**
         * Adds an input slot
         *
         * @param name  Unique slot name
         * @param klass Slot data class
         * @return The builder
         */
        public Builder addInputSlot(String name, Class<? extends JIPipeData> klass) {
            object.addSlot(name, new JIPipeDataSlotInfo(klass, JIPipeSlotType.Input, name, null), false);
            return this;
        }

        /**
         * Adds an input slot
         *
         * @param name     Unique slot name
         * @param klass    Slot data class
         * @param optional Make the slot optional
         * @return The builder
         */
        public Builder addInputSlot(String name, Class<? extends JIPipeData> klass, boolean optional) {
            JIPipeDataSlotInfo slot = object.addSlot(name, new JIPipeDataSlotInfo(klass, JIPipeSlotType.Input, name, null), false);
            slot.setOptional(optional);
            return this;
        }

        /**
         * Adds an input slot
         *
         * @param name     Unique slot name
         * @param klass    Slot data class
         * @param optional Make the slot optional (default false in other overload)
         * @param userModifiable Make slot user-modifiable (default true in other overload)
         * @return The builder
         */
        public Builder addInputSlot(String name, Class<? extends JIPipeData> klass, boolean optional, boolean userModifiable) {
            JIPipeDataSlotInfo slot = object.addSlot(name, new JIPipeDataSlotInfo(klass, JIPipeSlotType.Input, name, null), false);
            slot.setOptional(optional);
            slot.setUserModifiable(userModifiable);
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
        public Builder addOutputSlot(String name, Class<? extends JIPipeData> klass, String inheritedSlot) {
            object.addSlot(name, new JIPipeDataSlotInfo(klass, JIPipeSlotType.Output, name, inheritedSlot), false);
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
        public Builder addOutputSlot(String name, Class<? extends JIPipeData> klass, String inheritedSlot, Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> inheritanceConversions) {
            JIPipeDataSlotInfo slotDefinition = object.addSlot(name, new JIPipeDataSlotInfo(klass, JIPipeSlotType.Output, name, inheritedSlot), false);
            for (Map.Entry<Class<? extends JIPipeData>, Class<? extends JIPipeData>> entry : inheritanceConversions.entrySet()) {
                slotDefinition.getInheritanceConversions().put(JIPipeDataInfo.getInstance(entry.getKey()),
                        JIPipeDataInfo.getInstance(entry.getValue()));
            }
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
         * @param userModifiable Make slot user-modifiable (default true in other overloads)
         * @return The builder
         */
        public Builder addOutputSlot(String name, Class<? extends JIPipeData> klass, String inheritedSlot, Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> inheritanceConversions, boolean userModifiable) {
            JIPipeDataSlotInfo slotDefinition = object.addSlot(name, new JIPipeDataSlotInfo(klass, JIPipeSlotType.Output, name, inheritedSlot), false);
            for (Map.Entry<Class<? extends JIPipeData>, Class<? extends JIPipeData>> entry : inheritanceConversions.entrySet()) {
                slotDefinition.getInheritanceConversions().put(JIPipeDataInfo.getInstance(entry.getKey()),
                        JIPipeDataInfo.getInstance(entry.getValue()));
            }
            if (inheritedSlot != null && !inheritedSlot.isEmpty()) {
                object.setAllowInheritedOutputSlots(true);
            }
            slotDefinition.setUserModifiable(userModifiable);
            return this;
        }

        /**
         * Adds a slot
         *
         * @param definition Slot definition
         * @return The builder
         */
        public Builder addSlot(JIPipeDataSlotInfo definition) {
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
        public final Builder restrictInputTo(Class<? extends JIPipeData>... types) {
            object.allowedInputSlotTypes = new HashSet<>(Arrays.asList(types));
            return this;
        }

        /**
         * Restricts the slot to specified data types
         *
         * @param types Allowed data types
         * @return The builder
         */
        public final Builder restrictInputTo(Collection<Class<? extends JIPipeData>> types) {
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
        public final Builder restrictOutputTo(Class<? extends JIPipeData>... types) {
            object.allowedOutputSlotTypes = new HashSet<>(Arrays.asList(types));
            return this;
        }

        /**
         * Restricts the slot to specified data types
         *
         * @param types Allowed data types
         * @return The builder
         */
        public final Builder restrictOutputTo(Collection<Class<? extends JIPipeData>> types) {
            object.allowedOutputSlotTypes = new HashSet<>(types);
            return this;
        }

        /**
         * @return The {@link JIPipeDefaultMutableSlotConfiguration} instance
         */
        public JIPipeDefaultMutableSlotConfiguration build() {
            return object;
        }
    }
}


