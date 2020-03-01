package org.hkijena.acaq5.api.traits.global;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonSerialize(using = ACAQDataSlotTraitConfiguration.Serializer.class)
@JsonDeserialize(using = ACAQDataSlotTraitConfiguration.Deserializer.class)
public class ACAQDataSlotTraitConfiguration {
    private Map<ACAQTraitDeclaration, ACAQTraitModificationOperation> operations = new HashMap<>();

    public void clear() {
        operations.clear();
    }

    public void merge(ACAQDataSlotTraitConfiguration other) {
        for (Map.Entry<ACAQTraitDeclaration, ACAQTraitModificationOperation> entry : other.operations.entrySet()) {
            operations.put(entry.getKey(), entry.getValue());
        }
    }

    public void applyTo(ACAQDataSlot slot) {
        for (Map.Entry<ACAQTraitDeclaration, ACAQTraitModificationOperation> entry : operations.entrySet()) {
            switch (entry.getValue()) {
                case Add:
                    slot.addSlotAnnotation(entry.getKey());
                    break;
                case RemoveThis:
                    slot.removeSlotAnnotation(entry.getKey());
                    break;
                case RemoveCategory:
                    slot.removeSlotAnnotationCategory(entry.getKey());
                    break;
            }
        }
    }

    public void set(ACAQTraitDeclaration traitDeclaration, ACAQTraitModificationOperation operation) {
        operations.put(traitDeclaration, operation);
    }

    public Map<ACAQTraitDeclaration, ACAQTraitModificationOperation> getOperations() {
        return operations;
    }

    public Set<ACAQTraitDeclaration> getAddedTraits() {
        Set<ACAQTraitDeclaration> result = new HashSet<>();
        for (Map.Entry<ACAQTraitDeclaration, ACAQTraitModificationOperation> entry : operations.entrySet()) {
            if(entry.getValue() == ACAQTraitModificationOperation.Add)
                result.add(entry.getKey());
        }
        return result;
    }

    public Set<ACAQTraitDeclaration> getRemovedTraits() {
        Set<ACAQTraitDeclaration> result = new HashSet<>();
        for (Map.Entry<ACAQTraitDeclaration, ACAQTraitModificationOperation> entry : operations.entrySet()) {
            if(entry.getValue() == ACAQTraitModificationOperation.RemoveThis ||
                    entry.getValue() == ACAQTraitModificationOperation.RemoveCategory)
                result.add(entry.getKey());
        }
        return result;
    }

    public static class Serializer extends JsonSerializer<ACAQDataSlotTraitConfiguration> {
        @Override
        public void serialize(ACAQDataSlotTraitConfiguration traitConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("operations", traitConfiguration.operations);
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQDataSlotTraitConfiguration> {
        @Override
        public ACAQDataSlotTraitConfiguration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ACAQDataSlotTraitConfiguration configuration = new ACAQDataSlotTraitConfiguration();
            JsonNode root = jsonParser.readValueAsTree();
            TypeReference<Map<ACAQTraitDeclaration, ACAQTraitModificationOperation>> typeReference =
                    new TypeReference<Map<ACAQTraitDeclaration, ACAQTraitModificationOperation>>() {};
            configuration.operations = JsonUtils.getObjectMapper().readerFor(typeReference).readValue(root.get("operations"));
            return  configuration;
        }
    }
}
