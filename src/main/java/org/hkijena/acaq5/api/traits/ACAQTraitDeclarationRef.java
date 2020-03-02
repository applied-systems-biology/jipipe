package org.hkijena.acaq5.api.traits;

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
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import java.io.IOException;

/**
 * Helper to allow easy serialization of {@link ACAQTraitDeclaration} references
 */
@JsonSerialize(using = ACAQTraitDeclarationRef.Serializer.class)
@JsonDeserialize(using = ACAQTraitDeclarationRef.Deserializer.class)
public class ACAQTraitDeclarationRef {

    private ACAQTraitDeclaration declaration;

    public ACAQTraitDeclarationRef(ACAQTraitDeclaration declaration) {
        this.declaration = declaration;
    }

    public ACAQTraitDeclarationRef() {

    }

    public ACAQTraitDeclaration getDeclaration() {
        return declaration;
    }

    public void setDeclaration(ACAQTraitDeclaration declaration) {
        this.declaration = declaration;
    }

    public static class Serializer extends JsonSerializer<ACAQTraitDeclarationRef> {

        @Override
        public void serialize(ACAQTraitDeclarationRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.getDeclaration() != null ? ref.getDeclaration().getId() : null);
        }

    }

    public static class Deserializer extends JsonDeserializer<ACAQTraitDeclarationRef> {

        @Override
        public ACAQTraitDeclarationRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            ACAQTraitDeclarationRef result = new ACAQTraitDeclarationRef();
            if(!node.isNull()) {
                result.setDeclaration(ACAQTraitRegistry.getInstance().getDeclarationById(node.asText()));
            }
            return result;
        }
    }
}
