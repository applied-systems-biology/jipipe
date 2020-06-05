package org.hkijena.acaq5.extensions.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.awt.*;
import java.io.IOException;

/**
 * Serializer for {@link Color}
 */
public class ColorSerializer extends JsonSerializer<Color> {
    @Override
    public void serialize(Color value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        if (value.getAlpha() == 255) {
            gen.writeString("#" + Integer.toHexString(value.getRGB()).toUpperCase());
        } else {
            gen.writeString("#" + Integer.toHexString(value.getRGB()).toUpperCase() + Integer.toHexString(value.getAlpha()).toUpperCase());
        }
    }
}
