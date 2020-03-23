package org.hkijena.acaq5.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;

import java.io.IOException;

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

    public ACAQSlotDefinition(Class<? extends ACAQData> dataClass, ACAQDataSlot.SlotType slotType, String name, String inheritedSlot) {
        this.dataClass = dataClass;
        this.slotType = slotType;
        this.name = name;
        this.inheritedSlot = inheritedSlot;
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

    public static class Serializer extends JsonSerializer<ACAQSlotDefinition> {
        @Override
        public void serialize(ACAQSlotDefinition definition, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("slot-data-type", ACAQDatatypeRegistry.getInstance().getIdOf(definition.dataClass));
            jsonGenerator.writeStringField("slot-type", definition.slotType.name());
            jsonGenerator.writeStringField("inherited-slot", definition.inheritedSlot);
            jsonGenerator.writeStringField("name", definition.name);
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQSlotDefinition> {

        @Override
        public ACAQSlotDefinition deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            JsonNode inheritedSlotNode = node.path("inherited-slot");
            return new ACAQSlotDefinition(ACAQDatatypeRegistry.getInstance().getById(node.get("slot-data-type").asText()),
                    ACAQDataSlot.SlotType.valueOf(node.get("slot-type").asText()),
                    node.get("name").asText(),
                    inheritedSlotNode.isMissingNode() ? "" : inheritedSlotNode.asText());
        }
    }
}
