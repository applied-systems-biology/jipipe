package org.hkijena.acaq5.api.traits;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;

import java.io.IOException;

/**
 * An {@link ACAQTrait} that contains string data for discriminating data sets
 */
@ACAQHidden
@ACAQDocumentation(name = "Valued annotation", description = "An annotation with a string value")
@JsonSerialize(using = ACAQDiscriminator.Serializer.class)
public interface ACAQDiscriminator extends ACAQTrait, Comparable<ACAQDiscriminator> {
    /**
     * @return The string value. Can be null.
     */
    String getValue();

    /**
     * Serializes a discriminator
     */
    class Serializer extends JsonSerializer<ACAQDiscriminator> {
        @Override
        public void serialize(ACAQDiscriminator trait, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("acaq:trait-type", trait.getDeclaration().getId());
            jsonGenerator.writeStringField("name", trait.getDeclaration().getName());
            jsonGenerator.writeStringField("value", trait.getValue());
            jsonGenerator.writeEndObject();
        }
    }
}
