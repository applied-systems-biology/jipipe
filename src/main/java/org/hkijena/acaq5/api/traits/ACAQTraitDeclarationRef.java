package org.hkijena.acaq5.api.traits;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;

import java.io.IOException;

/**
 * Helper to allow easy serialization of {@link ACAQTraitDeclaration} references
 */
@JsonSerialize(using = ACAQTraitDeclarationRef.Serializer.class)
@JsonDeserialize(using = ACAQTraitDeclarationRef.Deserializer.class)
public class ACAQTraitDeclarationRef implements ACAQValidatable {

    private ACAQTraitDeclaration declaration;

    /**
     * @param declaration The referenced declaration
     */
    public ACAQTraitDeclarationRef(ACAQTraitDeclaration declaration) {
        this.declaration = declaration;
    }

    /**
     * New instance
     */
    public ACAQTraitDeclarationRef() {

    }

    public ACAQTraitDeclaration getDeclaration() {
        return declaration;
    }

    public void setDeclaration(ACAQTraitDeclaration declaration) {
        this.declaration = declaration;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (declaration == null)
            report.reportIsInvalid("No annotation type is selected! Please select an annotation type.");
    }

    /**
     * Serializes the reference as ID
     */
    public static class Serializer extends JsonSerializer<ACAQTraitDeclarationRef> {

        @Override
        public void serialize(ACAQTraitDeclarationRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.getDeclaration() != null ? ref.getDeclaration().getId() : null);
        }

    }

    /**
     * Deserializes the reference from a string
     */
    public static class Deserializer extends JsonDeserializer<ACAQTraitDeclarationRef> {

        @Override
        public ACAQTraitDeclarationRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            ACAQTraitDeclarationRef result = new ACAQTraitDeclarationRef();
            if (!node.isNull()) {
                result.setDeclaration(ACAQTraitRegistry.getInstance().getDeclarationById(node.asText()));
            }
            return result;
        }
    }
}
