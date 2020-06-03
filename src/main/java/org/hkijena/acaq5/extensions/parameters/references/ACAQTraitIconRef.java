package org.hkijena.acaq5.extensions.parameters.references;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;

import java.io.IOException;

/**
 * Used by {@link ACAQJsonTraitDeclaration} to setup icons
 */
@JsonSerialize(using = ACAQTraitIconRef.Serializer.class)
@JsonDeserialize(using = ACAQTraitIconRef.Deserializer.class)
public class ACAQTraitIconRef {
    private String iconName;

    /**
     * Creates a new instance
     */
    public ACAQTraitIconRef() {
    }

    /**
     * Copies an instance
     *
     * @param other the original
     */
    public ACAQTraitIconRef(ACAQTraitIconRef other) {
        this.iconName = other.iconName;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    @Override
    public String toString() {
        return "" + iconName;
    }

    /**
     * Serializes {@link ACAQTraitIconRef}
     */
    public static class Serializer extends JsonSerializer<ACAQTraitIconRef> {
        @Override
        public void serialize(ACAQTraitIconRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.iconName);
        }
    }

    /**
     * Deserializes {@link ACAQTraitIconRef}
     */
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
