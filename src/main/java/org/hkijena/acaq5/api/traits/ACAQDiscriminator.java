package org.hkijena.acaq5.api.traits;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

@HiddenTrait
@JsonSerialize(using = ACAQDiscriminator.Serializer.class)
public interface ACAQDiscriminator extends ACAQTrait {
    String getValue();

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
