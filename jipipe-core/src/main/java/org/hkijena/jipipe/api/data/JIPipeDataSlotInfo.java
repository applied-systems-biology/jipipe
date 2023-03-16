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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Defines an {@link JIPipeGraphNode} data slot.
 * This class is used within {@link JIPipeSlotConfiguration}
 */
@JsonSerialize(using = JIPipeDataSlotInfo.Serializer.class)
@JsonDeserialize(using = JIPipeDataSlotInfo.Deserializer.class)
public class JIPipeDataSlotInfo implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private JIPipeDataSlotRole role = JIPipeDataSlotRole.Data;
    private Class<? extends JIPipeData> dataClass;
    private JIPipeSlotType slotType;
    private String name;
    private String customName;
    private String description;
    private boolean virtual;
    private boolean saveOutputs = true;
    private boolean optional = false;
    private boolean userModifiable = true;

    protected JIPipeDataSlotInfo() {
    }

    /**
     * @param dataClass     slot data class
     * @param slotType      slot type
     * @param name          unique slot name
     * @param description   description of the slot
     * @param inheritedSlot deprecated
     * @param optional      only relevant if an input slot. marks the slot as optional input if true
     * @deprecated inheritedSlot is deprecated and non-functional
     */
    @Deprecated
    public JIPipeDataSlotInfo(Class<? extends JIPipeData> dataClass, JIPipeSlotType slotType, String name, String description, String inheritedSlot, boolean optional) {
        this.dataClass = dataClass;
        this.slotType = slotType;
        this.name = name;
        this.description = description;
        this.optional = optional;
    }

    /**
     * @param dataClass     slot data class
     * @param slotType      slot type
     * @param name          unique slot name
     * @param description   description of the slot
     * @param inheritedSlot only relevant if output slot. Can be an input slot name or '*' to automatically select the first input slot
     * @deprecated inheritedSlot is deprecated and non-functional
     */
    @Deprecated
    public JIPipeDataSlotInfo(Class<? extends JIPipeData> dataClass, JIPipeSlotType slotType, String name, String description, String inheritedSlot) {
        this(dataClass, slotType, name, description, inheritedSlot, false);
    }

    public JIPipeDataSlotInfo(Class<? extends JIPipeData> dataClass, JIPipeSlotType slotType, String name, String description, boolean optional) {
        this.dataClass = dataClass;
        this.slotType = slotType;
        this.name = name;
        this.description = description;
        this.optional = optional;
    }

    /**
     * @param dataClass   slot data class
     * @param slotType    slot type
     * @param name        unique slot name
     * @param description description of the slot
     */
    public JIPipeDataSlotInfo(Class<? extends JIPipeData> dataClass, JIPipeSlotType slotType, String name, String description) {
        this(dataClass, slotType, name, description, false);
    }

    /**
     * @param dataClass slot data class
     * @param slotType  slot type
     */
    public JIPipeDataSlotInfo(Class<? extends JIPipeData> dataClass, JIPipeSlotType slotType) {
        this(dataClass, slotType, null, null, false);
    }

    /**
     * @param slot Imported annotation
     */
    public JIPipeDataSlotInfo(JIPipeInputSlot slot) {
        this(slot.value(), JIPipeSlotType.Input, slot.slotName(), slot.description(), null);
        this.role = slot.role();
        this.optional = slot.optional();
    }


    /**
     * @param slot Imported annotation
     */
    public JIPipeDataSlotInfo(JIPipeOutputSlot slot) {
        this(slot.value(), JIPipeSlotType.Output, slot.slotName(), null);
        this.role = slot.role();
    }

    /**
     * Copies the definition
     *
     * @param other The original
     */
    public JIPipeDataSlotInfo(JIPipeDataSlotInfo other) {
        this.dataClass = other.dataClass;
        this.slotType = other.slotType;
        this.name = other.name;
        this.description = other.description;
        this.customName = other.customName;
        this.virtual = other.virtual;
        this.saveOutputs = other.saveOutputs;
        this.optional = other.optional;
        this.userModifiable = other.userModifiable;
        this.role = other.role;
    }

    /**
     * Returns true if the provided string is a valid slot name
     *
     * @param slotName the name
     * @return if the name is valid
     */
    public static boolean isValidName(String slotName) {
        return slotName.matches("[\\w.\\-,# ]+");
    }

    /**
     * Converts this info into the annotation form.
     * Throws an {@link UnsupportedOperationException} if the info describes an output.
     *
     * @return the annotation
     */
    public JIPipeInputSlot toInputSlotAnnotation() {
        return new DefaultJIPipeInputSlot(getDataClass(), getName(), getDescription(), false, isOptional(), getRole());
    }

    /**
     * Converts this info into the annotation form.
     * Throws an {@link UnsupportedOperationException} if the info describes an output.
     *
     * @return the annotation
     */
    public JIPipeOutputSlot toOutputSlotAnnotation() {
        return new DefaultJIPipeOutputSlot(getDataClass(), getName(), getDescription(), "", false, getRole());
    }

    public JIPipeDataSlot createInstance(JIPipeGraphNode node) {
        if (isInput()) {
            return new JIPipeInputDataSlot(this, node);
        } else if (isOutput()) {
            return new JIPipeOutputDataSlot(this, node);
        } else {
            throw new UnsupportedOperationException("Invalid slot info state!");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeDataSlotInfo that = (JIPipeDataSlotInfo) o;
        return optional == that.optional && role == that.role && Objects.equals(dataClass, that.dataClass) && slotType == that.slotType && Objects.equals(name, that.name) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, dataClass, slotType, name, description, optional);
    }

    public boolean isInput() {
        return slotType == JIPipeSlotType.Input;
    }

    public boolean isOutput() {
        return slotType == JIPipeSlotType.Output;
    }

    /**
     * @param newName new name
     * @return Slot definition copy with new name
     */
    public JIPipeDataSlotInfo renamedCopy(String newName) {
        JIPipeDataSlotInfo result = new JIPipeDataSlotInfo(this);
        result.name = newName;
        return result;
    }

    public Class<? extends JIPipeData> getDataClass() {
        return dataClass;
    }

    public void setDataClass(Class<? extends JIPipeData> dataClass) {
        this.dataClass = dataClass;
    }

    public JIPipeSlotType getSlotType() {
        return slotType;
    }

    public String getName() {
        return name;
    }

    /**
     * @return A custom name that the UI is displaying instead of getName() if the return value is not null or empty
     */
    @JIPipeParameter("custom-name")
    @JIPipeDocumentation(name = "Custom label", description = "Custom name for this slot. This does not change how the output folders are named.")
    public String getCustomName() {
        return customName;
    }

    /**
     * Overrides the name displayed in the UI
     *
     * @param customName a custom name or null to reset the custom name
     */
    @JIPipeParameter("custom-name")
    public void setCustomName(String customName) {
        this.customName = customName;
        eventBus.post(new ParameterChangedEvent(this, "custom-name"));
    }

    @JIPipeParameter("description")
    @JIPipeDocumentation(name = "Description", description = "Description of this slot")
    public String getDescription() {
        return description;
    }

    @JIPipeParameter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JIPipeDocumentation(name = "Is virtual", description = "Determines if the slot should be virtual.")
    @JIPipeParameter("is-virtual")
    public boolean isVirtual() {
        return virtual;
    }

    @JIPipeParameter("is-virtual")
    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    @JIPipeDocumentation(name = "Save outputs", description = "Determines if this slot's output should be saved. Only valid for output slots.")
    @JIPipeParameter("save-outputs")
    public boolean isSaveOutputs() {
        return saveOutputs;
    }

    @JIPipeParameter("save-outputs")
    public void setSaveOutputs(boolean saveOutputs) {
        this.saveOutputs = saveOutputs;
    }

    @JIPipeDocumentation(name = "Optional", description = "If true, the input does not need an incoming edge")
    @JIPipeParameter("is-optional")
    public boolean isOptional() {
        return optional;
    }

    @JIPipeParameter("is-optional")
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * Additional control variable to make a slot not removable/editable.
     * Use this to have "internal" slots that should never be removed.
     * This property is not serialized! The node must set it via code!
     *
     * @return if the slot is removable/editable
     */
    public boolean isUserModifiable() {
        return userModifiable;
    }

    /**
     * Set this variable to make a slot not removable/editable.
     * This property is not serialized! The node must set it via code!
     *
     * @param userModifiable if the slot is removable/editable
     */
    public void setUserModifiable(boolean userModifiable) {
        this.userModifiable = userModifiable;
    }

    /**
     * Copies additional parameters such as custom names from the other slot
     *
     * @param other other slot
     */
    public void copyMetadata(JIPipeDataSlotInfo other) {
        setCustomName(other.getCustomName());
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public JIPipeDataSlotRole getRole() {
        return role;
    }

    public void setRole(JIPipeDataSlotRole role) {
        this.role = role;
    }

    /**
     * Serializes an {@link JIPipeDataSlotInfo}
     */
    public static class Serializer extends JsonSerializer<JIPipeDataSlotInfo> {
        @Override
        public void serialize(JIPipeDataSlotInfo definition, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("slot-data-type", JIPipe.getDataTypes().getIdOf(definition.dataClass));
            jsonGenerator.writeStringField("slot-type", definition.slotType.name());
            jsonGenerator.writeStringField("name", definition.name);
            jsonGenerator.writeStringField("custom-name", definition.customName);
            jsonGenerator.writeStringField("description", definition.description);
            jsonGenerator.writeBooleanField("is-virtual", definition.virtual);
            jsonGenerator.writeBooleanField("save-outputs", definition.saveOutputs);
            jsonGenerator.writeBooleanField("is-optional", definition.optional);
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes an {@link JIPipeDataSlotInfo}
     */
    public static class Deserializer extends JsonDeserializer<JIPipeDataSlotInfo> {

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public JIPipeDataSlotInfo deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            JsonNode inheritedSlotNode = node.path("inherited-slot");
            JIPipeDataSlotInfo definition = new JIPipeDataSlotInfo(JIPipe.getDataTypes().getById(node.get("slot-data-type").asText()),
                    JIPipeSlotType.valueOf(node.get("slot-type").asText()),
                    node.get("name").asText(),
                    "", inheritedSlotNode.isMissingNode() ? "" : inheritedSlotNode.asText(null));
            JsonNode customNameNode = node.path("custom-name");
            if (!customNameNode.isMissingNode() && !customNameNode.isNull()) {
                definition.customName = customNameNode.textValue();
            }
            JsonNode descriptionNode = node.path("description");
            if (!descriptionNode.isMissingNode() && !descriptionNode.isNull()) {
                definition.description = descriptionNode.textValue();
            }
            JsonNode isVirtualNode = node.path("is-virtual");
            if (!isVirtualNode.isMissingNode()) {
                definition.virtual = isVirtualNode.asBoolean();
            }
            JsonNode saveOutputsNode = node.path("save-outputs");
            if (!saveOutputsNode.isMissingNode()) {
                definition.saveOutputs = saveOutputsNode.asBoolean();
            }
            JsonNode isOptionalNode = node.path("is-optional");
            if (!isOptionalNode.isMissingNode()) {
                definition.optional = isOptionalNode.asBoolean();
            }
            return definition;
        }

        public static class Builder {
            private JIPipeDataSlotInfo result = new JIPipeDataSlotInfo();

            public Builder makeInput() {
                result.slotType = JIPipeSlotType.Input;
                return this;
            }

            public Builder makeOutput() {
                result.slotType = JIPipeSlotType.Output;
                return this;
            }

            public Builder setName(String name) {
                result.name = name;
                return this;
            }

            public Builder setDescription(String description) {
                result.description = description;
                return this;
            }

            @Deprecated
            public Builder setInheritedSlot(String slotName) {
                return this;
            }

            @Deprecated
            public Builder resetInheritedSlot() {
                return this;
            }

            @Deprecated
            public Builder setInheritanceConversions(Map<JIPipeDataInfo, JIPipeDataInfo> conversions) {
                return this;
            }

            @Deprecated
            public Builder addInheritanceConversion(JIPipeDataInfo from, JIPipeDataInfo to) {
                return this;
            }

            @Deprecated
            public Builder addInheritanceConversion(Class<? extends JIPipeData> from, Class<? extends JIPipeData> to) {
                return addInheritanceConversion(JIPipeDataInfo.getInstance(from), JIPipeDataInfo.getInstance(to));
            }

            public Builder setCustomName(String name) {
                result.customName = name;
                return this;
            }

            public Builder resetCustomName() {
                return setCustomName(null);
            }

            public Builder setUserModifiable(boolean userModifiable) {
                result.userModifiable = userModifiable;
                return this;
            }

            public Builder setFrom(JIPipeDataSlotInfo info) {
                result = new JIPipeDataSlotInfo(info);
                return this;
            }

            public boolean isValid() {
                if (StringUtils.isNullOrEmpty(result.getName())) {
                    return false;
                }
                if (!StringUtils.isFilesystemCompatible(result.getName()) && result.isOutput()) {
                    return false;
                }
                return true;
            }

            public JIPipeDataSlotInfo build() {
                if (StringUtils.isNullOrEmpty(result.getName())) {
                    throw new IllegalArgumentException("The slot name is empty!");
                }
                if (!StringUtils.isFilesystemCompatible(result.getName()) && result.isOutput()) {
                    throw new IllegalArgumentException("The output slot name '" + result.getName() + "' is not compatible to file systems!");
                }
                return result;
            }
        }
    }
}
