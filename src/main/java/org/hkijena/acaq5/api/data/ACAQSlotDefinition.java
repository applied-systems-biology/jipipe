package org.hkijena.acaq5.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.ACAQRegistryService;

import java.io.IOException;

@JsonSerialize(using = ACAQSlotDefinition.Serializer.class)
@JsonDeserialize(using = ACAQSlotDefinition.Deserializer.class)
public class ACAQSlotDefinition {
    private Class<? extends ACAQDataSlot<?>> slotClass;
    private ACAQDataSlot.SlotType slotType;
    private String name;

    public ACAQSlotDefinition(Class<? extends ACAQDataSlot<?>> slotClass, ACAQDataSlot.SlotType slotType, String name) {
        this.slotClass = slotClass;
        this.slotType = slotType;
        this.name = name;
    }

    public Class<? extends ACAQDataSlot<?>> getSlotClass() {
        return slotClass;
    }

    public ACAQDataSlot.SlotType getSlotType() {
        return slotType;
    }

    public String getName() {
        return name;
    }

    public static class Serializer extends JsonSerializer<ACAQSlotDefinition> {
        @Override
        public void serialize(ACAQSlotDefinition definition, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("slot-class", definition.slotClass.getCanonicalName());
            jsonGenerator.writeStringField("slot-type", definition.slotType.name());
            jsonGenerator.writeStringField("name", definition.name);
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQSlotDefinition> {

        @Override
        public ACAQSlotDefinition deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            return new ACAQSlotDefinition(ACAQRegistryService.getInstance().getDatatypeRegistry().findDataSlotClass(node.get("slot-class").asText()),
                    ACAQDataSlot.SlotType.valueOf(node.get("slot-type").asText()),
                    node.get("name").asText());
        }
    }
}
