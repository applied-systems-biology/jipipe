package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.AddsTrait;
import org.hkijena.acaq5.api.traits.RemovesTrait;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@JsonSerialize(using = ACAQEmptyAlgorithmDeclaration.Serializer.class)
public class ACAQEmptyAlgorithmDeclaration implements ACAQAlgorithmDeclaration {
    @Override
    public Class<? extends ACAQAlgorithm> getAlgorithmClass() {
        return null;
    }

    @Override
    public ACAQAlgorithm newInstance() {
        return null;
    }

    @Override
    public ACAQAlgorithm clone(ACAQAlgorithm algorithm) {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public ACAQAlgorithmCategory getCategory() {
        return ACAQAlgorithmCategory.Internal;
    }

    @Override
    public Set<Class<? extends ACAQTrait>> getPreferredTraits() {
        return Collections.emptySet();
    }

    @Override
    public Set<Class<? extends ACAQTrait>> getUnwantedTraits() {
        return Collections.emptySet();
    }

    @Override
    public List<AddsTrait> getAddedTraits() {
        return Collections.emptyList();
    }

    @Override
    public List<RemovesTrait> getRemovedTraits() {
        return Collections.emptyList();
    }

    @Override
    public List<AlgorithmInputSlot> getInputSlots() {
        return Collections.emptyList();
    }

    @Override
    public List<AlgorithmOutputSlot> getOutputSlots() {
        return Collections.emptyList();
    }

    @Override
    public boolean matches(JsonNode node) {
        return false;
    }

    public static class Serializer extends JsonSerializer<ACAQEmptyAlgorithmDeclaration> {
        @Override
        public void serialize(ACAQEmptyAlgorithmDeclaration acaqEmptyAlgorithmDeclaration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }
    }
}
