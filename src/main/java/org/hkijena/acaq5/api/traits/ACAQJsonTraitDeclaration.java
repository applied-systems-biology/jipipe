package org.hkijena.acaq5.api.traits;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.ACAQDependency;

import java.io.IOException;
import java.util.Set;

@JsonSerialize(using = ACAQJsonTraitDeclaration.Serializer.class)
@JsonDeserialize(using = ACAQJsonTraitDeclaration.Deserializer.class)
public class ACAQJsonTraitDeclaration extends ACAQMutableTraitDeclaration {


    @Override
    public ACAQTrait newInstance() {
        return null;
    }

    @Override
    public ACAQTrait newInstance(String value) {
        return null;
    }

    @Override
    public Set<ACAQDependency> getDependencies() {
        return null;
    }

    public static class Serializer extends JsonSerializer<ACAQJsonTraitDeclaration> {
        @Override
        public void serialize(ACAQJsonTraitDeclaration acaqJsonTraitDeclaration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQJsonTraitDeclaration> {
        @Override
        public ACAQJsonTraitDeclaration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            return null;
        }
    }
}
