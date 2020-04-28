package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;

import java.io.IOException;

/**
 * A parameter that holds a reference to an {@link ACAQAlgorithmDeclaration}
 */
@JsonSerialize(using = ACAQAlgorithmDeclarationRef.Serializer.class)
@JsonDeserialize(using = ACAQAlgorithmDeclarationRef.Deserializer.class)
public class ACAQAlgorithmDeclarationRef implements ACAQValidatable {
    private ACAQAlgorithmDeclaration declaration;

    /**
     * @param declaration The referenced declaration
     */
    public ACAQAlgorithmDeclarationRef(ACAQAlgorithmDeclaration declaration) {
        this.declaration = declaration;
    }

    /**
     * New instance
     */
    public ACAQAlgorithmDeclarationRef() {

    }

    public ACAQAlgorithmDeclaration getDeclaration() {
        return declaration;
    }

    public void setDeclaration(ACAQAlgorithmDeclaration declaration) {
        this.declaration = declaration;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (declaration == null)
            report.reportIsInvalid("No algorithm type is selected!",
                    "You have to select an algorithm type.",
                    "Please select an algorithm type.",
                    this);
    }

    @Override
    public String toString() {
        if (declaration != null)
            return declaration.getId();
        else
            return "<Null>";
    }

    /**
     * Serializes the reference as ID
     */
    public static class Serializer extends JsonSerializer<ACAQAlgorithmDeclarationRef> {

        @Override
        public void serialize(ACAQAlgorithmDeclarationRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.getDeclaration() != null ? ref.getDeclaration().getId() : null);
        }

    }

    /**
     * Deserializes the reference from a string
     */
    public static class Deserializer extends JsonDeserializer<ACAQAlgorithmDeclarationRef> {

        @Override
        public ACAQAlgorithmDeclarationRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            ACAQAlgorithmDeclarationRef result = new ACAQAlgorithmDeclarationRef();
            if (!node.isNull()) {
                result.setDeclaration(ACAQAlgorithmRegistry.getInstance().getDeclarationById(node.textValue()));
            }
            return result;
        }
    }
}
