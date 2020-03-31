package org.hkijena.acaq5.api.traits;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;

import java.io.IOException;
import java.util.Objects;

/**
 * Base interface for an algorithm data trait
 * Traits are generated by data slots and accumulated over algorithm depth.
 * Algorithms can modify output traits by adding or removing them.
 */
@ACAQHidden
@JsonSerialize(using = ACAQTrait.Serializer.class)
@JsonDeserialize(using = ACAQTrait.Deserializer.class)
public interface ACAQTrait {

    /**
     * Returns the declaration of this trait
     *
     * @return
     */
    ACAQTraitDeclaration getDeclaration();

    /**
     * Returns the name of given trait
     *
     * @param klass data class
     * @return name
     */
    static String getNameOf(Class<? extends ACAQTrait> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if (annotations.length > 0) {
            return annotations[0].name();
        } else {
            return klass.getSimpleName();
        }
    }

    /**
     * Returns the description of given trait
     *
     * @param klass data class
     * @return description
     */
    static String getDescriptionOf(Class<? extends ACAQTrait> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if (annotations.length > 0) {
            return annotations[0].description();
        } else {
            return null;
        }
    }

    /**
     * Faster method to compare traits
     *
     * @param first first trait
     * @param second second trait
     * @return if traits are equivalent
     */
    static boolean equals(ACAQTrait first, ACAQTrait second) {
        if (first == null && second == null)
            return true;
        else if (first == null)
            return false;
        else if (second == null)
            return false;
        else if (first.getDeclaration() != second.getDeclaration())
            return false;
        else if (first.getDeclaration().isDiscriminator())
            return Objects.equals(((ACAQDiscriminator) first).getValue(), ((ACAQDiscriminator) second).getValue());
        else
            return true;
    }

    /**
     * Serializes an {@link ACAQTrait}
     */
    class Serializer extends JsonSerializer<ACAQTrait> {
        @Override
        public void serialize(ACAQTrait trait, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("acaq:trait-type", trait.getDeclaration().getId());
            jsonGenerator.writeStringField("name", trait.getDeclaration().getName());
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes an {@link ACAQTrait}
     */
    class Deserializer extends JsonDeserializer<ACAQTrait> {
        @Override
        public ACAQTrait deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            String traitTypeId = node.get("acaq:trait-type").asText();
            ACAQTraitDeclaration declaration = ACAQTraitRegistry.getInstance().getDeclarationById(traitTypeId);
            JsonNode valueNode = node.path("value");
            if (valueNode.isMissingNode()) {
                return declaration.newInstance();
            } else {
                return declaration.newInstance(valueNode.asText());
            }
        }
    }
}
