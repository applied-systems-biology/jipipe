package org.hkijena.acaq5.extensions.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.hkijena.acaq5.utils.StringUtils;

import java.awt.*;
import java.io.IOException;

/**
 * Serializer for {@link Color}
 */
public class ColorSerializer extends JsonSerializer<Color> {
    @Override
    public void serialize(Color value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeString(StringUtils.colorToHexString(value));
    }
}
