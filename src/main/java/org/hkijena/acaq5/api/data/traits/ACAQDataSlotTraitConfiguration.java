package org.hkijena.acaq5.api.data.traits;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Serializable description of how annotations transfer through an {@link org.hkijena.acaq5.api.algorithm.ACAQAlgorithm}
 * Annotations are either added, removed, or ignored.
 * Ignoring means that existing traits are transferred to the output.
 */
@JsonSerialize(using = ACAQDataSlotTraitConfiguration.Serializer.class)
@JsonDeserialize(using = ACAQDataSlotTraitConfiguration.Deserializer.class)
public class ACAQDataSlotTraitConfiguration {
    private Map<ACAQTraitDeclaration, ACAQTraitModificationOperation> operations = new HashMap<>();

    /**
     * Creates a new instance
     */
    public ACAQDataSlotTraitConfiguration() {
    }

    /**
     * Copies a configuration
     *
     * @param other The original
     */
    public ACAQDataSlotTraitConfiguration(ACAQDataSlotTraitConfiguration other) {
        this.operations = new HashMap<>(other.operations);
    }

    /**
     * Removes all operations
     */
    public void clear() {
        operations.clear();
    }

    /**
     * Merges another instance into this one
     *
     * @param other The other instance
     */
    public void merge(ACAQDataSlotTraitConfiguration other) {
        for (Map.Entry<ACAQTraitDeclaration, ACAQTraitModificationOperation> entry : other.operations.entrySet()) {
            operations.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Applies the operations to the slot
     *
     * @param slot The slot
     */
    public void applyTo(ACAQDataSlot slot) {
        for (Map.Entry<ACAQTraitDeclaration, ACAQTraitModificationOperation> entry : operations.entrySet()) {
//            System.out.println(entry.getValue().toString().toUpperCase() + " " + entry.getKey().getName() + " @ " + slot.getNameWithAlgorithmName());
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

    /**
     * Sets the operation for an annotation type
     *
     * @param traitDeclaration Annotation type
     * @param operation        The operation
     */
    public void set(ACAQTraitDeclaration traitDeclaration, ACAQTraitModificationOperation operation) {
        operations.put(traitDeclaration, operation);
    }

    /**
     * Returns map of operations for each annotation type.
     * The map is writable.
     *
     * @return A map of operations for each annotation type
     */
    public Map<ACAQTraitDeclaration, ACAQTraitModificationOperation> getOperations() {
        return operations;
    }

    /**
     * @return All annotations added by the operations
     */
    public Set<ACAQTraitDeclaration> getAddedTraits() {
        Set<ACAQTraitDeclaration> result = new HashSet<>();
        for (Map.Entry<ACAQTraitDeclaration, ACAQTraitModificationOperation> entry : operations.entrySet()) {
            if (entry.getValue() == ACAQTraitModificationOperation.Add)
                result.add(entry.getKey());
        }
        return result;
    }

    /**
     * @return All annotations removed by the operations
     */
    public Set<ACAQTraitDeclaration> getRemovedTraits() {
        Set<ACAQTraitDeclaration> result = new HashSet<>();
        for (Map.Entry<ACAQTraitDeclaration, ACAQTraitModificationOperation> entry : operations.entrySet()) {
            if (entry.getValue() == ACAQTraitModificationOperation.RemoveThis ||
                    entry.getValue() == ACAQTraitModificationOperation.RemoveCategory)
                result.add(entry.getKey());
        }
        return result;
    }

    /**
     * Serializes the configuration
     */
    public static class Serializer extends JsonSerializer<ACAQDataSlotTraitConfiguration> {
        @Override
        public void serialize(ACAQDataSlotTraitConfiguration traitConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            for (Map.Entry<ACAQTraitDeclaration, ACAQTraitModificationOperation> entry : traitConfiguration.operations.entrySet()) {
                jsonGenerator.writeFieldName(entry.getKey().getId());
                jsonGenerator.writeString(entry.getValue().name());
            }
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes the configuration
     */
    public static class Deserializer extends JsonDeserializer<ACAQDataSlotTraitConfiguration> {
        @Override
        public ACAQDataSlotTraitConfiguration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ACAQDataSlotTraitConfiguration configuration = new ACAQDataSlotTraitConfiguration();
            JsonNode root = jsonParser.readValueAsTree();
            for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(root.fields())) {
                ACAQTraitDeclaration declaration = ACAQTraitRegistry.getInstance().getDeclarationById(entry.getKey());
                ACAQTraitModificationOperation operation = ACAQTraitModificationOperation.valueOf(entry.getValue().asText());
                configuration.operations.put(declaration, operation);
            }
            return configuration;
        }
    }
}
