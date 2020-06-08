package org.hkijena.acaq5.extensions.parameters.roi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.awt.*;
import java.io.IOException;

/**
 * Deserializer for {@link Color}
 */
public class RectangleDeserializer extends JsonDeserializer<Rectangle> {
    @Override
    public Rectangle deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.readValueAsTree();
        return new Rectangle(node.get("x").asInt(),
                node.get("y").asInt(),
                node.get("width").asInt(),
                node.get("height").asInt());
    }


}
