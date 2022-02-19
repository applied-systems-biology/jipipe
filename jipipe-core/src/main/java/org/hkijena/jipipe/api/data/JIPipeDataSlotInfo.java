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
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.VirtualDataSettings;

import java.io.IOException;
import java.util.*;

/**
 * Defines an {@link JIPipeGraphNode} data slot.
 * This class is used within {@link JIPipeSlotConfiguration}
 */
@JsonSerialize(using = JIPipeDataSlotInfo.Serializer.class)
@JsonDeserialize(using = JIPipeDataSlotInfo.Deserializer.class)
public class JIPipeDataSlotInfo implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private Class<? extends JIPipeData> dataClass;
    private JIPipeSlotType slotType;
    private String name;
    private String inheritedSlot;
    private Map<JIPipeDataInfo, JIPipeDataInfo> inheritanceConversions = new HashMap<>();
    private String customName;
    private String description;
    private boolean virtual;
    private boolean saveOutputs = true;
    private boolean optional = false;
    private boolean userModifiable = true;

    /**
     * @param dataClass     slot data class
     * @param slotType      slot type
     * @param name          unique slot name
     * @param description description of the slot
     * @param inheritedSlot only relevant if output slot. Can be an input slot name or '*' to automatically select the first input slot
     */
    public JIPipeDataSlotInfo(Class<? extends JIPipeData> dataClass, JIPipeSlotType slotType, String name, String description, String inheritedSlot) {
        this.dataClass = dataClass;
        this.slotType = slotType;
        this.name = name;
        this.description = description;
        this.inheritedSlot = inheritedSlot;
        setVirtualByDataType();
    }

    /**
     * @param dataClass slot data class
     * @param slotType  slot type
     * @param name      unique slot name
     * @param description description of the slot
     */
    public JIPipeDataSlotInfo(Class<? extends JIPipeData> dataClass, JIPipeSlotType slotType, String name, String description) {
        this(dataClass, slotType, name, description, null);
    }

    /**
     * @param dataClass slot data class
     * @param slotType  slot type
     */
    public JIPipeDataSlotInfo(Class<? extends JIPipeData> dataClass, JIPipeSlotType slotType) {
        this(dataClass, slotType, null, null, null);
    }

    /**
     * @param slot Imported annotation
     */
    public JIPipeDataSlotInfo(JIPipeInputSlot slot) {
        this(slot.value(), JIPipeSlotType.Input, slot.slotName(), null, null);
        this.optional = slot.optional();
    }


    /**
     * @param slot Imported annotation
     */
    public JIPipeDataSlotInfo(JIPipeOutputSlot slot) {
        this(slot.value(), JIPipeSlotType.Output, slot.slotName(), null, slot.inheritedSlot());
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
        this.inheritedSlot = other.inheritedSlot;
        this.inheritanceConversions = new HashMap<>(other.inheritanceConversions);
        this.customName = other.customName;
        this.virtual = other.virtual;
        this.saveOutputs = other.saveOutputs;
        this.optional = other.optional;
        this.userModifiable = other.userModifiable;
    }

    /**
     * Applies inheritance conversion.
     * This is a text replacement system with termination condition of never visiting the same time twice.
     *
     * @param definition The slot definition
     * @param dataClass  The slot data
     * @return The converted data
     */
    public static Class<? extends JIPipeData> applyInheritanceConversion(JIPipeDataSlotInfo definition, Class<? extends JIPipeData> dataClass) {
        Set<JIPipeDataInfo> visited = new HashSet<>();
        JIPipeDataInfo currentData = JIPipeDataInfo.getInstance(dataClass);
        JIPipeDataInfo lastData = currentData;
        visited.add(currentData);
        while (true) {
            currentData = definition.inheritanceConversions.getOrDefault(currentData, null);
            if (currentData == null)
                return lastData.getDataClass();
            lastData = currentData;
            if (visited.contains(currentData))
                return currentData.getDataClass();
            visited.add(currentData);
        }
    }

    /**
     * Creates the composition of two inheritance conversions.
     * The result is outer(inner(x))
     *
     * @param outer the outer conversion
     * @param inner the inner conversion
     * @return Inheritance conversion that is outer(inner(x))
     */
    public static Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> composeRawInheritanceConversions(Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> outer,
                                                                                                                 Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> inner) {
        Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> result = new HashMap<>(inner);
        for (Map.Entry<Class<? extends JIPipeData>, Class<? extends JIPipeData>> entry : result.entrySet()) {
            Class<? extends JIPipeData> transformed = outer.getOrDefault(entry.getValue(), null);
            if (transformed != null) {
                entry.setValue(transformed);
            }
        }
        return result;
    }

    /**
     * Creates the composition of two inheritance conversions.
     * The result is outer(inner(x))
     *
     * @param outer the outer conversion
     * @param inner the inner conversion
     * @return Inheritance conversion that is outer(inner(x))
     */
    public static Map<JIPipeDataInfo, JIPipeDataInfo> composeInheritanceConversions(Map<JIPipeDataInfo, JIPipeDataInfo> outer,
                                                                                    Map<JIPipeDataInfo, JIPipeDataInfo> inner) {
        Map<JIPipeDataInfo, JIPipeDataInfo> result = new HashMap<>(inner);
        for (Map.Entry<JIPipeDataInfo, JIPipeDataInfo> entry : result.entrySet()) {
            JIPipeDataInfo transformed = outer.getOrDefault(entry.getValue(), null);
            if (transformed != null) {
                entry.setValue(transformed);
            }
        }
        return result;
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

    private void setVirtualByDataType() {
        if (JIPipe.getInstance() != null && JIPipe.getSettings().getRegisteredSheets().containsKey(VirtualDataSettings.ID)
                && VirtualDataSettings.getInstance().isLargeVirtualDataTypesByDefault()) {
            if (dataClass.getAnnotation(JIPipeHeavyData.class) != null) {
                this.virtual = true;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeDataSlotInfo that = (JIPipeDataSlotInfo) o;
        return Objects.equals(dataClass, that.dataClass) &&
                slotType == that.slotType &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(inheritedSlot, that.inheritedSlot) &&
                Objects.equals(inheritanceConversions, that.inheritanceConversions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataClass, slotType, name, description, inheritedSlot, inheritanceConversions);
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

    public JIPipeSlotType getSlotType() {
        return slotType;
    }

    public String getName() {
        return name;
    }

    /**
     * Gets slot to inherit the data type from
     * If null or empty, this slot does not inherit
     * If it equals '*', the first available input slot is chosen
     *
     * @return the slot name or '*' to pick the first slot or null
     */
    public String getInheritedSlot() {
        return inheritedSlot;
    }

    public Map<JIPipeDataInfo, JIPipeDataInfo> getInheritanceConversions() {
        return inheritanceConversions;
    }

    public void setInheritanceConversions(Map<JIPipeDataInfo, JIPipeDataInfo> inheritanceConversions) {
        this.inheritanceConversions = inheritanceConversions;
    }

    public void setInheritanceConversionsFromRaw(Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> inheritanceConversions) {
        this.inheritanceConversions = new HashMap<>();
        for (Map.Entry<Class<? extends JIPipeData>, Class<? extends JIPipeData>> entry : inheritanceConversions.entrySet()) {
            this.inheritanceConversions.put(JIPipeDataInfo.getInstance(entry.getKey()), JIPipeDataInfo.getInstance(entry.getValue()));
        }
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

    /**
     * Serializes an {@link JIPipeDataSlotInfo}
     */
    public static class Serializer extends JsonSerializer<JIPipeDataSlotInfo> {
        @Override
        public void serialize(JIPipeDataSlotInfo definition, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("slot-data-type", JIPipe.getDataTypes().getIdOf(definition.dataClass));
            jsonGenerator.writeStringField("slot-type", definition.slotType.name());
            jsonGenerator.writeStringField("inherited-slot", definition.inheritedSlot);
            jsonGenerator.writeStringField("name", definition.name);
            jsonGenerator.writeStringField("custom-name", definition.customName);
            jsonGenerator.writeStringField("description", definition.description);
            jsonGenerator.writeBooleanField("is-virtual", definition.virtual);
            jsonGenerator.writeBooleanField("save-outputs", definition.saveOutputs);
            jsonGenerator.writeBooleanField("is-optional", definition.optional);
            jsonGenerator.writeFieldName("inheritance-conversions");
            jsonGenerator.writeStartObject();
            for (Map.Entry<JIPipeDataInfo, JIPipeDataInfo> entry : definition.getInheritanceConversions().entrySet()) {
                jsonGenerator.writeStringField(entry.getKey().getId(), entry.getValue().getId());
            }
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes an {@link JIPipeDataSlotInfo}
     */
    public static class Deserializer extends JsonDeserializer<JIPipeDataSlotInfo> {

        @Override
        public JIPipeDataSlotInfo deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            JsonNode inheritedSlotNode = node.path("inherited-slot");
            JIPipeDataSlotInfo definition = new JIPipeDataSlotInfo(JIPipe.getDataTypes().getById(node.get("slot-data-type").asText()),
                    JIPipeSlotType.valueOf(node.get("slot-type").asText()),
                    node.get("name").asText(),
                    "", inheritedSlotNode.isMissingNode() ? "" : inheritedSlotNode.asText(null));
            JsonNode conversionsNode = node.path("inheritance-conversions");
            if (!conversionsNode.isMissingNode()) {
                for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(conversionsNode.fields())) {
                    String idKey = entry.getKey();
                    String idValue = entry.getValue().asText();
                    try {
                        JIPipeDataInfo key = JIPipeDataInfo.getInstance(idKey);
                        JIPipeDataInfo value = JIPipeDataInfo.getInstance(idValue);
                        definition.inheritanceConversions.put(key, value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
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
    }
}
