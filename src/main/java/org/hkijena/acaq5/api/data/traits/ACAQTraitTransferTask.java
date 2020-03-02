package org.hkijena.acaq5.api.data.traits;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@JsonSerialize(using = ACAQTraitTransferTask.Serializer.class)
@JsonDeserialize(using = ACAQTraitTransferTask.Deserializer.class)
public class ACAQTraitTransferTask {
    private String inputSlotName;
    private String outputSlotName;
    private Set<ACAQTraitDeclaration> traitRestrictions;

    /**
     * @param inputSlotName
     * @param outputSlotName
     * @param traitRestrictions If empty, no restrictions apply
     */
    public ACAQTraitTransferTask(String inputSlotName, String outputSlotName, Set<ACAQTraitDeclaration> traitRestrictions) {
        this.inputSlotName = inputSlotName;
        this.outputSlotName = outputSlotName;
        this.traitRestrictions = traitRestrictions;
    }

    public void applyTo(ACAQAlgorithm algorithm) {
        ACAQDataSlot sourceSlot = algorithm.getInputSlot(getInputSlotName());
        ACAQDataSlot targetSlot = algorithm.getOutputSlot(getOutputSlotName());
        for (ACAQTraitDeclaration slotAnnotation : sourceSlot.getSlotAnnotations()) {
            if (getTraitRestrictions().isEmpty() || getTraitRestrictions().contains(slotAnnotation)) {
                targetSlot.addSlotAnnotation(slotAnnotation);
            }
        }
    }

    public String getInputSlotName() {
        return inputSlotName;
    }

    public String getOutputSlotName() {
        return outputSlotName;
    }

    public Set<ACAQTraitDeclaration> getTraitRestrictions() {
        return traitRestrictions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ACAQTraitTransferTask that = (ACAQTraitTransferTask) o;
        return inputSlotName.equals(that.inputSlotName) &&
                outputSlotName.equals(that.outputSlotName) &&
                traitRestrictions.equals(that.traitRestrictions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputSlotName, outputSlotName, traitRestrictions);
    }

    public static class Serializer extends JsonSerializer<ACAQTraitTransferTask> {
        @Override
        public void serialize(ACAQTraitTransferTask task, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("input-slot", task.inputSlotName);
            jsonGenerator.writeStringField("output-slot", task.outputSlotName);
            jsonGenerator.writeObjectField("trait-restrictions", task.traitRestrictions.stream().map(ACAQTraitDeclaration::getId).collect(Collectors.toList()));
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQTraitTransferTask> {
        @Override
        public ACAQTraitTransferTask deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            Set<ACAQTraitDeclaration> restrictions = new HashSet<>();
            for (JsonNode jsonNode : ImmutableList.copyOf(node.get("trait-restrictions").elements())) {
                restrictions.add(ACAQTraitRegistry.getInstance().getDeclarationById(jsonNode.asText()));
            }
            return new ACAQTraitTransferTask(
                    node.get("input-slot").asText(),
                    node.get("output-slot").asText(),
                    restrictions
            );
        }
    }
}
