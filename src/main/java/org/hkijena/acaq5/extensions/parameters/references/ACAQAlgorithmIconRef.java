package org.hkijena.acaq5.extensions.parameters.references;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.GraphWrapperAlgorithmDeclaration;

import java.io.IOException;

/**
 * Used by {@link GraphWrapperAlgorithmDeclaration} to setup icons
 */
@JsonSerialize(using = ACAQAlgorithmIconRef.Serializer.class)
@JsonDeserialize(using = ACAQAlgorithmIconRef.Deserializer.class)
public class ACAQAlgorithmIconRef {
    private String iconName;

    /**
     * Creates a new instance
     */
    public ACAQAlgorithmIconRef() {
    }

    /**
     * Copies an instance
     *
     * @param other the original
     */
    public ACAQAlgorithmIconRef(ACAQAlgorithmIconRef other) {
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
     * Serializes {@link ACAQAlgorithmIconRef}
     */
    public static class Serializer extends JsonSerializer<ACAQAlgorithmIconRef> {
        @Override
        public void serialize(ACAQAlgorithmIconRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.iconName);
        }
    }

    /**
     * Deserializes {@link ACAQAlgorithmIconRef}
     */
    public static class Deserializer extends JsonDeserializer<ACAQAlgorithmIconRef> {

        @Override
        public ACAQAlgorithmIconRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            ACAQAlgorithmIconRef result = new ACAQAlgorithmIconRef();
            if (!node.isNull()) {
                result.setIconName(node.textValue());
            }
            return result;
        }
    }
}
