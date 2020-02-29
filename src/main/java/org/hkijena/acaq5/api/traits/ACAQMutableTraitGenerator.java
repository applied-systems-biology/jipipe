package org.hkijena.acaq5.api.traits;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.events.TraitsChangedEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Trait configuration designed for usage in {@link ACAQPreprocessingOutput}
 */
@JsonSerialize(using = ACAQMutableTraitGenerator.Serializer.class)
public class ACAQMutableTraitGenerator extends ACAQTraitConfiguration {
    private List<ACAQMutableTraitModifier.ModifyTask> modifyTasks = new ArrayList<>();
    private ACAQSlotConfiguration slotConfiguration;

    public ACAQMutableTraitGenerator(ACAQSlotConfiguration slotConfiguration) {
        this.slotConfiguration = slotConfiguration;
    }

    /**
     * Adds a trait to the specified output slot
     *
     * @param outputSlotName
     * @param trait
     * @return
     */
    public ACAQMutableTraitGenerator addTraitTo(String outputSlotName, Class<? extends ACAQTrait> trait) {
        if (!getSlotConfiguration().getSlots().containsKey(outputSlotName))
            throw new IllegalArgumentException("Slot must exist!");
        modifyTasks.add(new ACAQMutableTraitModifier.ModifyTask(outputSlotName, ACAQMutableTraitModifier.ModificationType.ADD, trait));
        getEventBus().post(new TraitsChangedEvent(this));
        return this;
    }

    /**
     * Removes trait from specified output slot
     *
     * @param outputSlotName
     * @param trait
     * @return
     */
    public ACAQMutableTraitGenerator removeTraitFrom(String outputSlotName, Class<? extends ACAQTrait> trait) {
        if (!getSlotConfiguration().getSlots().containsKey(outputSlotName))
            throw new IllegalArgumentException("Slot must exist!");
        modifyTasks.removeIf(task -> task.getTrait().equals(trait));
        getEventBus().post(new TraitsChangedEvent(this));
        return this;
    }

    /**
     * Returns the trais of specified slot
     *
     * @param slotName
     * @return
     */
    public Set<Class<? extends ACAQTrait>> getTraitsOf(String slotName) {
        return modifyTasks.stream().filter(task -> task.getSlotName().equals(slotName))
                .map(ACAQMutableTraitModifier.ModifyTask::getTrait).collect(Collectors.toSet());
    }

    /**
     * Removes all modifications and transfers
     */
    public ACAQTraitConfiguration clear() {
        modifyTasks.clear();
        getEventBus().post(new TraitsChangedEvent(this));
        return this;
    }

    /**
     * Transfers traits from an input slot to an output slot
     *
     * @param sourceSlotName Input slot name
     * @param source         Input slot traits
     * @param targetSlotName Output slot name
     * @param target         Output slot traits
     */
    @Override
    public void transfer(String sourceSlotName, Set<Class<? extends ACAQTrait>> source,
                         String targetSlotName, Set<Class<? extends ACAQTrait>> target) {
    }

    /**
     * Modifies the traits of a slot
     * This function is applied to output slots after transfer
     *
     * @param slotName Output slot name
     * @param target   Existing output slot traits
     */
    @Override
    public void modify(String slotName, Set<Class<? extends ACAQTrait>> target) {
        for (ACAQMutableTraitModifier.ModifyTask task : modifyTasks) {
            if (task.getSlotName() == null || slotName.equals(task.getSlotName())) {
                switch (task.getType()) {
                    case ADD:
                        target.add(task.getTrait());
                        break;
                    case REMOVE:
                        throw new RuntimeException("Preprocessing nodes cannot remove traits!");
                }
            }
        }
    }

    public ACAQSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }

    public void fromJson(JsonNode node) {
        if (node.has("traits")) {
//            for(JsonNode trait : ImmutableList.copyOf(node.get("traits").elements())) {
//                Class<? extends ACAQTrait> klass = ACAQTraitRegistry.getInstance().findTraitClass(trait.get("class").asText());
//                String slotName = trait.get("slot-name").asText();
//                addTraitTo(slotName, klass);
//            }
        }
    }

    public static class Serializer extends JsonSerializer<ACAQMutableTraitGenerator> {

        @Override
        public void serialize(ACAQMutableTraitGenerator configuration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeFieldName("traits");
            jsonGenerator.writeStartArray();
            for (ACAQMutableTraitModifier.ModifyTask task : configuration.modifyTasks) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("class", task.getTrait().getCanonicalName());
                jsonGenerator.writeStringField("slot-name", task.getSlotName());
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
    }
}
