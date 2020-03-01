package org.hkijena.acaq5.api.traits.global;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.io.IOException;
import java.util.Objects;

@JsonSerialize(using = ACAQTraitModificationTask.Serializer.class)
@JsonDeserialize(using = ACAQTraitModificationTask.Deserializer.class)
public class ACAQTraitModificationTask {
    private ACAQTraitDeclaration traitDeclaration;
    private Operation operation;
    private boolean dummy;

    public ACAQTraitModificationTask(ACAQTraitDeclaration traitDeclaration, Operation operation, boolean dummy) {
        this.traitDeclaration = traitDeclaration;
        this.operation = operation;
        this.dummy = dummy;
    }

    public ACAQTraitDeclaration getTraitDeclaration() {
        return traitDeclaration;
    }

    public Operation getOperation() {
        return operation;
    }

    public boolean isAddingTraits() {
        return operation == Operation.Add;
    }

    public boolean isRemovingTraits() {
        return !isAddingTraits();
    }

    public void applyTo(ACAQDataSlot slot) {
        if(isDummy())
            return;
        switch (operation) {
            case Add:
                slot.addSlotAnnotation(traitDeclaration);
                break;
            case RemoveThis:
                slot.removeSlotAnnotation(traitDeclaration);
                break;
            case RemoveCategory:
                slot.removeSlotAnnotationCategory(traitDeclaration);
                break;
        }
    }

    public boolean isDummy() {
        return dummy;
    }

    public enum Operation {
        Add,
        RemoveThis,
        RemoveCategory
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ACAQTraitModificationTask that = (ACAQTraitModificationTask) o;
        return dummy == that.dummy &&
                traitDeclaration.equals(that.traitDeclaration) &&
                operation == that.operation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(traitDeclaration, operation, dummy);
    }

    public static class Serializer extends JsonSerializer<ACAQTraitModificationTask> {
        @Override
        public void serialize(ACAQTraitModificationTask task, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("trait-type", task.traitDeclaration.getId());
            jsonGenerator.writeStringField("operation", task.operation.name());
            jsonGenerator.writeBooleanField("is-dummy", task.dummy);
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQTraitModificationTask> {
        @Override
        public ACAQTraitModificationTask deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            return new ACAQTraitModificationTask(
                    ACAQTraitRegistry.getInstance().getDeclarationById(node.get("trait-type").asText()),
                    Operation.valueOf(node.get("operation").asText()),
                    node.get("is-dummy").asBoolean()
            );
        }
    }
}
