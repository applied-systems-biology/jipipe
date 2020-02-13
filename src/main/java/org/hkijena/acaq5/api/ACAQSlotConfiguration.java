package org.hkijena.acaq5.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.EventBus;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonSerialize(using = ACAQSlotConfiguration.Serializer.class)
public abstract class ACAQSlotConfiguration {
    private EventBus eventBus = new EventBus();
    public abstract Map<String, ACAQSlotDefinition> getSlots();
    public abstract List<String> getInputSlotOrder();
    public abstract List<String> getOutputSlotOrder();
    public EventBus getEventBus() {
        return eventBus;
    }

    public Map<String, ACAQSlotDefinition> getInputSlots() {
        Map<String, ACAQSlotDefinition> result = new HashMap<>();
        for(Map.Entry<String, ACAQSlotDefinition> kv : getSlots().entrySet()) {
            if(kv.getValue().getSlotType() == ACAQDataSlot.SlotType.Input)
                result.put(kv.getKey(), kv.getValue());
        }
        return result;
    }

    public Map<String, ACAQSlotDefinition> getOutputSlots() {
        Map<String, ACAQSlotDefinition> result = new HashMap<>();
        for(Map.Entry<String, ACAQSlotDefinition> kv : getSlots().entrySet()) {
            if(kv.getValue().getSlotType() == ACAQDataSlot.SlotType.Output)
                result.put(kv.getKey(), kv.getValue());
        }
        return result;
    }

    /**
     * Returns the maximum of number(input slot) and number(output slots)
     * @return
     */
    public int getRows() {
        return Math.max(getInputSlots().size(), getOutputSlots().size());
    }

    /**
     * Loads this configuration from JSON
     * @param jsonNode
     */
    public abstract void fromJson(JsonNode jsonNode);

    public static class Serializer extends JsonSerializer<ACAQSlotConfiguration> {

        @Override
        public void serialize(ACAQSlotConfiguration slotConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();

            Map<String, ACAQSlotDefinition> inputSlots = slotConfiguration.getInputSlots();
            Map<String, ACAQSlotDefinition> outputSlots = slotConfiguration.getOutputSlots();

            for(String key : slotConfiguration.getInputSlotOrder()) {
                jsonGenerator.writeObjectField(key, inputSlots.get(key));
            }
            for(String key : slotConfiguration.getOutputSlotOrder()) {
                jsonGenerator.writeObjectField(key, outputSlots.get(key));
            }

            jsonGenerator.writeEndObject();
        }
    }
}
