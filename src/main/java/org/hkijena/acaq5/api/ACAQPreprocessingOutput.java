package org.hkijena.acaq5.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQInputAsOutputSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitConfiguration;
import org.hkijena.acaq5.api.traits.AddsTrait;
import org.hkijena.acaq5.api.traits.RemovesTrait;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An internal node that interfaces sample-specific processing with the analysis
 * The preprocessing output exists both in sample graphs (with a {@link ACAQMutableSlotConfiguration} slot configuration)
 * and in the analysis project graph (with a {@link ACAQInputAsOutputSlotConfiguration}).
 * The preprocessing output does not pass traits like other algorithms, but instead generates traits from an
 * {@link ACAQTraitConfiguration} that is global for each project.
 *
 * Note: This node is not designed to be carried into an {@link ACAQRun} and should not be registered. It should be
 * only added by the project logic.
 */
@ACAQDocumentation(name="Preprocessing output")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Internal)
public class ACAQPreprocessingOutput extends ACAQAlgorithm {

    public ACAQPreprocessingOutput(ACAQSlotConfiguration slotConfiguration, ACAQTraitConfiguration traitConfiguration) {
        super(new Declaration(), slotConfiguration, traitConfiguration);
    }

    @Override
    protected ACAQSlotConfiguration copySlotConfiguration(ACAQAlgorithm other) {
        // The slot configuration is global
        return other.getSlotConfiguration();
    }

    @Override
    public void run() {

    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    @JsonSerialize(using = DeclarationSerializer.class)
    public static class Declaration implements ACAQAlgorithmDeclaration {

        @Override
        public Class<? extends ACAQAlgorithm> getAlgorithmClass() {
            return ACAQPreprocessingOutput.class;
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
            return "Preprocessing output";
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
        public ACAQAlgorithmVisibility getVisibility() {
            return ACAQAlgorithmVisibility.All;
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
    }

    public static class DeclarationSerializer extends JsonSerializer<Declaration> {
        @Override
        public void serialize(Declaration declaration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }
    }
}
