package org.hkijena.acaq5.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines an {@link org.hkijena.acaq5.api.algorithm.ACAQAlgorithm} data slot.
 * This class is used within {@link ACAQSlotConfiguration}
 */
@JsonSerialize(using = ACAQSlotDefinition.Serializer.class)
@JsonDeserialize(using = ACAQSlotDefinition.Deserializer.class)
public class ACAQSlotDefinition {
    private Class<? extends ACAQData> dataClass;
    private ACAQDataSlot.SlotType slotType;
    private String name;
    private String inheritedSlot;
    private Map<ACAQDataDeclaration, ACAQDataDeclaration> inheritanceConversions = new HashMap<>();

    public ACAQSlotDefinition(Class<? extends ACAQData> dataClass, ACAQDataSlot.SlotType slotType, String name, String inheritedSlot) {
        this.dataClass = dataClass;
        this.slotType = slotType;
        this.name = name;
        this.inheritedSlot = inheritedSlot;
    }

    public ACAQSlotDefinition(ACAQSlotDefinition other) {
        this.dataClass = other.dataClass;
        this.slotType = other.slotType;
        this.name = other.name;
        this.inheritedSlot = other.inheritedSlot;
        this.inheritanceConversions = new HashMap<>(other.inheritanceConversions);
    }

    public ACAQSlotDefinition renamedCopy(String newName) {
        ACAQSlotDefinition result = new ACAQSlotDefinition(this);
        result.name = newName;
        return result;
    }

    public Class<? extends ACAQData> getDataClass() {
        return dataClass;
    }

    public ACAQDataSlot.SlotType getSlotType() {
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
     * @return
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

    public static Class<? extends ACAQData> applyInheritanceConversion(ACAQSlotDefinition definition, Class<? extends ACAQData> dataClass) {
        return definition.inheritanceConversions.getOrDefault(ACAQDataDeclaration.getInstance(dataClass),
                ACAQDataDeclaration.getInstance(dataClass)).getDataClass();
    }

    public static class Serializer extends JsonSerializer<ACAQSlotDefinition> {
        @Override
        public void serialize(ACAQSlotDefinition definition, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("slot-data-type", ACAQDatatypeRegistry.getInstance().getIdOf(definition.dataClass));
            jsonGenerator.writeStringField("slot-type", definition.slotType.name());
            jsonGenerator.writeStringField("inherited-slot", definition.inheritedSlot);
            jsonGenerator.writeFieldName("inheritance-conversions");
            jsonGenerator.writeStartObject();
            for (Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration> entry : definition.getInheritanceConversions().entrySet()) {
                jsonGenerator.writeStringField(entry.getKey().getId(), entry.getValue().getId());
            }
            jsonGenerator.writeEndObject();
            jsonGenerator.writeStringField("name", definition.name);
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQSlotDefinition> {

        @Override
        public ACAQSlotDefinition deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            JsonNode inheritedSlotNode = node.path("inherited-slot");
            ACAQSlotDefinition definition = new ACAQSlotDefinition(ACAQDatatypeRegistry.getInstance().getById(node.get("slot-data-type").asText()),
                    ACAQDataSlot.SlotType.valueOf(node.get("slot-type").asText()),
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
            return definition;
        }
    }
}
