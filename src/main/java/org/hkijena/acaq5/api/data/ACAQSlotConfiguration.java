package org.hkijena.acaq5.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.EventBus;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A slot configuration determines which slots an {@link org.hkijena.acaq5.api.algorithm.ACAQAlgorithm} should have.
 * The algorithm then instantiates {@link ACAQDataSlot} instances to match the configuration.
 * This is required to allow multiple algorithms to share their slots.
 */
@JsonSerialize(using = ACAQSlotConfiguration.Serializer.class)
public abstract class ACAQSlotConfiguration {
    private EventBus eventBus = new EventBus();

    /**
     * @return the slots
     */
    public abstract Map<String, ACAQSlotDefinition> getSlots();

    /**
     * @return the input slot order
     */
    public abstract List<String> getInputSlotOrder();

    /**
     * @return the output slot order
     */
    public abstract List<String> getOutputSlotOrder();

    /**
     * @return the event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * @return the input slots
     */
    public Map<String, ACAQSlotDefinition> getInputSlots() {
        Map<String, ACAQSlotDefinition> result = new HashMap<>();
        for (Map.Entry<String, ACAQSlotDefinition> kv : getSlots().entrySet()) {
            if (kv.getValue().getSlotType() == ACAQDataSlot.SlotType.Input)
                result.put(kv.getKey(), kv.getValue());
        }
        return result;
    }

    /**
     * @return the output slots
     */
    public Map<String, ACAQSlotDefinition> getOutputSlots() {
        Map<String, ACAQSlotDefinition> result = new HashMap<>();
        for (Map.Entry<String, ACAQSlotDefinition> kv : getSlots().entrySet()) {
            if (kv.getValue().getSlotType() == ACAQDataSlot.SlotType.Output)
                result.put(kv.getKey(), kv.getValue());
        }
        return result;
    }

    /**
     * Returns the maximum of number(input slot) and number(output slots)
     *
     * @return maximum number of rows
     */
    public int getRows() {
        return Math.max(getInputSlots().size(), getOutputSlots().size());
    }

    /**
     * Makes this slot configuration equivalent to the provided one
     *
     * @param configuration the configuration
     */
    public abstract void setTo(ACAQSlotConfiguration configuration);

    /**
     * Loads this configuration from JSON
     *
     * @param jsonNode JSON data
     */
    public abstract void fromJson(JsonNode jsonNode);

    /**
     * Serializes the configuration
     */
    public static class Serializer extends JsonSerializer<ACAQSlotConfiguration> {

        @Override
        public void serialize(ACAQSlotConfiguration slotConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();

            Map<String, ACAQSlotDefinition> inputSlots = slotConfiguration.getInputSlots();
            Map<String, ACAQSlotDefinition> outputSlots = slotConfiguration.getOutputSlots();

            for (String key : slotConfiguration.getInputSlotOrder()) {
                jsonGenerator.writeObjectField(key, inputSlots.get(key));
            }
            for (String key : slotConfiguration.getOutputSlotOrder()) {
                jsonGenerator.writeObjectField(key, outputSlots.get(key));
            }

            jsonGenerator.writeEndObject();
        }
    }
}
