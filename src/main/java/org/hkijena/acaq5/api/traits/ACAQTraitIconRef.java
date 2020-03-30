package org.hkijena.acaq5.api.traits;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

/**
 * Used by {@link ACAQJsonTraitDeclaration} to setup icons
 */
@JsonSerialize(using = ACAQTraitIconRef.Serializer.class)
@JsonDeserialize(using = ACAQTraitIconRef.Deserializer.class)
public class ACAQTraitIconRef {
    private String iconName;

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public static class Serializer extends JsonSerializer<ACAQTraitIconRef> {
        @Override
        public void serialize(ACAQTraitIconRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.iconName);
        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQTraitIconRef> {

        @Override
        public ACAQTraitIconRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            ACAQTraitIconRef result = new ACAQTraitIconRef();
            if (!node.isNull()) {
                result.setIconName(node.textValue());
            }
            return result;
        }
    }
}
