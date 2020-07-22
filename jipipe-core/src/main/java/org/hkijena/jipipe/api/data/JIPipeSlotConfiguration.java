/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A slot configuration determines which slots an {@link JIPipeGraphNode} should have.
 * The algorithm then instantiates {@link JIPipeDataSlot} instances to match the configuration.
 * This is required to allow multiple algorithms to share their slots.
 */
@JsonSerialize(using = JIPipeSlotConfiguration.Serializer.class)
public interface JIPipeSlotConfiguration {

    /**
     * Creates a deep copy
     *
     * @return a deep copy
     */
    JIPipeSlotConfiguration duplicate();

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
    Map<String, JIPipeDataSlotInfo> getInputSlots();

    /**
     * @return the output slots
     */
    Map<String, JIPipeDataSlotInfo> getOutputSlots();

    /**
     * Makes this slot configuration equivalent to the provided one
     *
     * @param configuration the configuration
     */
    void setTo(JIPipeSlotConfiguration configuration);

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
    class Serializer extends JsonSerializer<JIPipeSlotConfiguration> {

        @Override
        public void serialize(JIPipeSlotConfiguration slotConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            slotConfiguration.toJson(jsonGenerator);
        }
    }
}
