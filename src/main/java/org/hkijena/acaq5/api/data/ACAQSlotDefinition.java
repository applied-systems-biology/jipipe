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

package org.hkijena.acaq5.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Defines an {@link ACAQGraphNode} data slot.
 * This class is used within {@link ACAQSlotConfiguration}
 */
@JsonSerialize(using = ACAQSlotDefinition.Serializer.class)
@JsonDeserialize(using = ACAQSlotDefinition.Deserializer.class)
public class ACAQSlotDefinition implements ACAQParameterCollection {
    private EventBus eventBus = new EventBus();
    private Class<? extends ACAQData> dataClass;
    private ACAQSlotType slotType;
    private String name;
    private String inheritedSlot;
    private Map<ACAQDataDeclaration, ACAQDataDeclaration> inheritanceConversions = new HashMap<>();
    private String customName;

    /**
     * @param dataClass     slot data class
     * @param slotType      slot type
     * @param name          unique slot name
     * @param inheritedSlot only relevant if output slot. Can be an input slot name or '*' to automatically select the first input slot
     */
    public ACAQSlotDefinition(Class<? extends ACAQData> dataClass, ACAQSlotType slotType, String name, String inheritedSlot) {
        this.dataClass = dataClass;
        this.slotType = slotType;
        this.name = name;
        this.inheritedSlot = inheritedSlot;
    }

    /**
     * Creates an unnamed slot.
     * The name is assigned from the {@link ACAQDefaultMutableSlotConfiguration}
     *
     * @param dataClass     slot data class
     * @param slotType      slot type
     * @param inheritedSlot only relevant if output slot. Can be an input slot name or '*' to automatically select the first input slot
     */
    public ACAQSlotDefinition(Class<? extends ACAQData> dataClass, ACAQSlotType slotType, String inheritedSlot) {
        this.dataClass = dataClass;
        this.slotType = slotType;
        this.inheritedSlot = inheritedSlot;
    }

    /**
     * @param slot Imported annotation
     */
    public ACAQSlotDefinition(AlgorithmInputSlot slot) {
        this(slot.value(), ACAQSlotType.Input, slot.slotName(), null);
    }

    /**
     * @param slot Imported annotation
     */
    public ACAQSlotDefinition(AlgorithmOutputSlot slot) {
        this(slot.value(), ACAQSlotType.Output, slot.slotName(), null);
    }

    /**
     * Copies the definition
     *
     * @param other The original
     */
    public ACAQSlotDefinition(ACAQSlotDefinition other) {
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
    public ACAQSlotDefinition(Class<? extends ACAQData> dataClass, ACAQSlotType slotType) {
        this(dataClass, slotType, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ACAQSlotDefinition that = (ACAQSlotDefinition) o;
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
        return slotType == ACAQSlotType.Input;
    }

    public boolean isOutput() {
        return slotType == ACAQSlotType.Output;
    }

    /**
     * @param newName new name
     * @return Slot definition copy with new name
     */
    public ACAQSlotDefinition renamedCopy(String newName) {
        ACAQSlotDefinition result = new ACAQSlotDefinition(this);
        result.name = newName;
        return result;
    }

    public Class<? extends ACAQData> getDataClass() {
        return dataClass;
    }

    public ACAQSlotType getSlotType() {
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

    public Map<ACAQDataDeclaration, ACAQDataDeclaration> getInheritanceConversions() {
        return inheritanceConversions;
    }

    public void setInheritanceConversions(Map<ACAQDataDeclaration, ACAQDataDeclaration> inheritanceConversions) {
        this.inheritanceConversions = inheritanceConversions;
    }

    public void setInheritanceConversionsFromRaw(Map<Class<? extends ACAQData>, Class<? extends ACAQData>> inheritanceConversions) {
        this.inheritanceConversions = new HashMap<>();
        for (Map.Entry<Class<? extends ACAQData>, Class<? extends ACAQData>> entry : inheritanceConversions.entrySet()) {
            this.inheritanceConversions.put(ACAQDataDeclaration.getInstance(entry.getKey()), ACAQDataDeclaration.getInstance(entry.getValue()));
        }
    }

    /**
     * @return A custom name that the UI is displaying instead of getName() if the return value is not null or empty
     */
    @ACAQParameter("custom-name")
    @ACAQDocumentation(name = "Custom label", description = "Custom name for this slot. This does not change how the output folders are named.")
    public String getCustomName() {
        return customName;
    }

    /**
     * Overrides the name displayed in the UI
     *
     * @param customName a custom name or null to reset the custom name
     */
    @ACAQParameter("custom-name")
    public void setCustomName(String customName) {
        this.customName = customName;
        eventBus.post(new ParameterChangedEvent(this, "custom-name"));
    }

    /**
     * Copies additional parameters such as custom names from the other slot
     *
     * @param other other slot
     */
    public void copyMetadata(ACAQSlotDefinition other) {
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
    public static Class<? extends ACAQData> applyInheritanceConversion(ACAQSlotDefinition definition, Class<? extends ACAQData> dataClass) {
        Set<ACAQDataDeclaration> visited = new HashSet<>();
        ACAQDataDeclaration currentData = ACAQDataDeclaration.getInstance(dataClass);
        ACAQDataDeclaration lastData = currentData;
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
    public static Map<Class<? extends ACAQData>, Class<? extends ACAQData>> composeRawInheritanceConversions(Map<Class<? extends ACAQData>, Class<? extends ACAQData>> outer,
                                                                                                             Map<Class<? extends ACAQData>, Class<? extends ACAQData>> inner) {
        Map<Class<? extends ACAQData>, Class<? extends ACAQData>> result = new HashMap<>(inner);
        for (Map.Entry<Class<? extends ACAQData>, Class<? extends ACAQData>> entry : result.entrySet()) {
            Class<? extends ACAQData> transformed = outer.getOrDefault(entry.getValue(), null);
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
    public static Map<ACAQDataDeclaration, ACAQDataDeclaration> composeInheritanceConversions(Map<ACAQDataDeclaration, ACAQDataDeclaration> outer,
                                                                                              Map<ACAQDataDeclaration, ACAQDataDeclaration> inner) {
        Map<ACAQDataDeclaration, ACAQDataDeclaration> result = new HashMap<>(inner);
        for (Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration> entry : result.entrySet()) {
            ACAQDataDeclaration transformed = outer.getOrDefault(entry.getValue(), null);
            if (transformed != null) {
                entry.setValue(transformed);
            }
        }
        return result;
    }

    /**
     * Serializes an {@link ACAQSlotDefinition}
     */
    public static class Serializer extends JsonSerializer<ACAQSlotDefinition> {
        @Override
        public void serialize(ACAQSlotDefinition definition, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("slot-data-type", ACAQDatatypeRegistry.getInstance().getIdOf(definition.dataClass));
            jsonGenerator.writeStringField("slot-type", definition.slotType.name());
            jsonGenerator.writeStringField("inherited-slot", definition.inheritedSlot);
            jsonGenerator.writeStringField("name", definition.name);
            jsonGenerator.writeStringField("custom-name", definition.customName);
            jsonGenerator.writeFieldName("inheritance-conversions");
            jsonGenerator.writeStartObject();
            for (Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration> entry : definition.getInheritanceConversions().entrySet()) {
                jsonGenerator.writeStringField(entry.getKey().getId(), entry.getValue().getId());
            }
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes an {@link ACAQSlotDefinition}
     */
    public static class Deserializer extends JsonDeserializer<ACAQSlotDefinition> {

        @Override
        public ACAQSlotDefinition deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            JsonNode inheritedSlotNode = node.path("inherited-slot");
            ACAQSlotDefinition definition = new ACAQSlotDefinition(ACAQDatatypeRegistry.getInstance().getById(node.get("slot-data-type").asText()),
                    ACAQSlotType.valueOf(node.get("slot-type").asText()),
                    node.get("name").asText(),
                    inheritedSlotNode.isMissingNode() ? "" : inheritedSlotNode.asText(null));
            JsonNode conversionsNode = node.path("inheritance-conversions");
            if (!conversionsNode.isMissingNode()) {
                for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(conversionsNode.fields())) {
                    String idKey = entry.getKey();
                    String idValue = entry.getValue().asText();
                    ACAQDataDeclaration key = ACAQDataDeclaration.getInstance(idKey);
                    ACAQDataDeclaration value = ACAQDataDeclaration.getInstance(idValue);
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
