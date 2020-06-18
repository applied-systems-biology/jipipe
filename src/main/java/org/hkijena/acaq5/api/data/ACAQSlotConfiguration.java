package org.hkijena.acaq5.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A slot configuration determines which slots an {@link ACAQGraphNode} should have.
 * The algorithm then instantiates {@link ACAQDataSlot} instances to match the configuration.
 * This is required to allow multiple algorithms to share their slots.
 */
@JsonSerialize(using = ACAQSlotConfiguration.Serializer.class)
public interface ACAQSlotConfiguration {
    /**
     * @return the input slot order
     */
    List<String> getInputSlotOrder();

    /**
     * @return the output slot order
     */
    List<String> getOutputSlotOrder();

    /**
     * @return the event bus
     */
    EventBus getEventBus();

    /**
     * @return the input slots
     */
    Map<String, ACAQSlotDefinition> getInputSlots();

    /**
     * @return the output slots
     */
    Map<String, ACAQSlotDefinition> getOutputSlots();

    /**
     * Makes this slot configuration equivalent to the provided one
     *
     * @param configuration the configuration
     */
    void setTo(ACAQSlotConfiguration configuration);

    /**
     * Loads this configuration from JSON
     *
     * @param jsonNode JSON data
     */
    void fromJson(JsonNode jsonNode);

    /**
     * Saves this configuration to JSON.
     *
     * @param generator the Json generator
     * @throws JsonProcessingException thrown by Json
     */
    void toJson(JsonGenerator generator) throws IOException, JsonProcessingException;

    /**
     * Return true if there is an input slot with given name
     *
     * @param name the name
     * @return if there is an input slot with given name
     */
    default boolean hasInputSlot(String name) {
        return getInputSlots().containsKey(name);
    }

    /**
     * Return true if there is an output slot with given name
     *
     * @param name the name
     * @return if there is an output slot with given name
     */
    default boolean hasOutputSlot(String name) {
        return getOutputSlots().containsKey(name);
    }

    /**
     * Serializes the configuration
     */
    class Serializer extends JsonSerializer<ACAQSlotConfiguration> {

        @Override
        public void serialize(ACAQSlotConfiguration slotConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            slotConfiguration.toJson(jsonGenerator);
        }
    }
}
