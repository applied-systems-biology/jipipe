package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.data.traits.ACAQDataSlotTraitConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An empty algorithm declaration.
 * Use this when you initialize an {@link ACAQAlgorithm} manually within another algorithm.
 * Warning: May break the algorithm.
 */
@JsonSerialize(using = ACAQEmptyAlgorithmDeclaration.Serializer.class)
public class ACAQEmptyAlgorithmDeclaration implements ACAQAlgorithmDeclaration {
    @Override
    public String getId() {
        return "acaq:empty";
    }

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
    public String getMenuPath() {
        return "";
    }

    @Override
    public ACAQAlgorithmCategory getCategory() {
        return ACAQAlgorithmCategory.Internal;
    }

    @Override
    public Set<ACAQTraitDeclaration> getPreferredTraits() {
        return Collections.emptySet();
    }

    @Override
    public Set<ACAQTraitDeclaration> getUnwantedTraits() {
        return Collections.emptySet();
    }

    @Override
    public ACAQDataSlotTraitConfiguration getSlotTraitConfiguration() {
        return new ACAQDataSlotTraitConfiguration();
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
    public Set<ACAQDependency> getDependencies() {
        return Collections.emptySet();
    }

    /**
     * Serializes the empty algorithm declaration
     */
    public static class Serializer extends JsonSerializer<ACAQEmptyAlgorithmDeclaration> {
        @Override
        public void serialize(ACAQEmptyAlgorithmDeclaration acaqEmptyAlgorithmDeclaration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }
    }
}
