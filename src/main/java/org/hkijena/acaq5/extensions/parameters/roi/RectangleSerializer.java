package org.hkijena.acaq5.extensions.parameters.roi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.awt.*;
import java.io.IOException;

/**
 * Serializer for {@link Rectangle}
 */
public class RectangleSerializer extends JsonSerializer<Rectangle> {
    @Override
    public void serialize(Rectangle value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeStartObject();
        gen.writeNumberField("x", value.getX());
        gen.writeNumberField("y", value.getY());
        gen.writeNumberField("width", value.getWidth());
        gen.writeNumberField("height", value.getHeight());
        gen.writeEndObject();
    }
}
