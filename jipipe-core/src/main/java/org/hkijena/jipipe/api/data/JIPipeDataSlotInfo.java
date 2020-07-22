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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;

import java.io.IOException;
import java.util.*;

/**
 * Defines an {@link JIPipeGraphNode} data slot.
 * This class is used within {@link JIPipeSlotConfiguration}
 */
@JsonSerialize(using = JIPipeDataSlotInfo.Serializer.class)
@JsonDeserialize(using = JIPipeDataSlotInfo.Deserializer.class)
public class JIPipeDataSlotInfo implements JIPipeParameterCollection {
    private EventBus eventBus = new EventBus();
    private Class<? extends JIPipeData> dataClass;
    private JIPipeSlotType slotType;
    private String name;
    private String inheritedSlot;
    private Map<JIPipeDataInfo, JIPipeDataInfo> inheritanceConversions = new HashMap<>();
    private String customName;

    /**
     * @param dataClass     slot data class
     * @param slotType      slot type
     * @param name          unique slot name
     * @param inheritedSlot only relevant if output slot. Can be an input slot name or '*' to automatically select the first input slot
     */
    public JIPipeDataSlotInfo(Class<? extends JIPipeData> dataClass, JIPipeSlotType slotType, String name, String inheritedSlot) {
        this.dataClass = dataClass;
        this.slotType = slotType;
        this.name = name;
        this.inheritedSlot = inheritedSlot;
    }

    /**
     * Creates an unnamed slot.
     * The name is assigned from the {@link JIPipeDefaultMutableSlotConfiguration}
     *
     * @param dataClass     slot data class
     * @param slotType      slot type
     * @param inheritedSlot only relevant if output slot. Can be an input slot name or '*' to automatically select the first input slot
     */
    public JIPipeDataSlotInfo(Class<? extends JIPipeData> dataClass, JIPipeSlotType slotType, String inheritedSlot) {
        this.dataClass = dataClass;
        this.slotType = slotType;
        this.inheritedSlot = inheritedSlot;
    }

    /**
     * @param slot Imported annotation
     */
    public JIPipeDataSlotInfo(JIPipeInputSlot slot) {
        this(slot.value(), JIPipeSlotType.Input, slot.slotName(), null);
    }

    /**
     * @param slot Imported annotation
     */
    public JIPipeDataSlotInfo(JIPipeOutputSlot slot) {
        this(slot.value(), JIPipeSlotType.Output, slot.slotName(), null);
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
        this.inheritedSlot = other.inheritedSlot;
        this.inheritanceConversions = new HashMap<>(other.inheritanceConversions);
        this.customName = other.customName;
    }

    /**
     * Creates a new instance. The name is set to null.
     *
     * @param dataClass slot data class
     * @param slotType  slot type
     */
    public JIPipeDataSlotInfo(Class<? extends JIPipeData> dataClass, JIPipeSlotType slotType) {
        this(dataClass, slotType, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeDataSlotInfo that = (JIPipeDataSlotInfo) o;
        return Objects.equals(dataClass, that.dataClass) &&
                slotType == that.slotType &&
                Objects.equals(name, that.name) &&
                Objects.equals(inheritedSlot, that.inheritedSlot) &&
                Objects.equals(inheritanceConversions, that.inheritanceConversions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataClass, slotType, name, inheritedSlot, inheritanceConversions);
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
     * Serializes an {@link JIPipeDataSlotInfo}
     */
    public static class Serializer extends JsonSerializer<JIPipeDataSlotInfo> {
        @Override
        public void serialize(JIPipeDataSlotInfo definition, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("slot-data-type", JIPipeDatatypeRegistry.getInstance().getIdOf(definition.dataClass));
            jsonGenerator.writeStringField("slot-type", definition.slotType.name());
            jsonGenerator.writeStringField("inherited-slot", definition.inheritedSlot);
            jsonGenerator.writeStringField("name", definition.name);
            jsonGenerator.writeStringField("custom-name", definition.customName);
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
            JIPipeDataSlotInfo definition = new JIPipeDataSlotInfo(JIPipeDatatypeRegistry.getInstance().getById(node.get("slot-data-type").asText()),
                    JIPipeSlotType.valueOf(node.get("slot-type").asText()),
                    node.get("name").asText(),
                    inheritedSlotNode.isMissingNode() ? "" : inheritedSlotNode.asText(null));
            JsonNode conversionsNode = node.path("inheritance-conversions");
            if (!conversionsNode.isMissingNode()) {
                for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(conversionsNode.fields())) {
                    String idKey = entry.getKey();
                    String idValue = entry.getValue().asText();
                    JIPipeDataInfo key = JIPipeDataInfo.getInstance(idKey);
                    JIPipeDataInfo value = JIPipeDataInfo.getInstance(idValue);
                    definition.inheritanceConversions.put(key, value);
                }
            }
            JsonNode customNameNode = node.path("custom-name");
            if (!customNameNode.isMissingNode() && !customNameNode.isNull()) {
                definition.customName = customNameNode.textValue();
            }
            return definition;
        }
    }
}
