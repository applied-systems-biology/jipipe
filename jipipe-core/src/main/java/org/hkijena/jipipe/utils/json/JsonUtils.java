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

package org.hkijena.jipipe.utils.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Global utilities for JSON data
 */
public class JsonUtils {

    private static ObjectMapper objectMapper;

    private JsonUtils() {

    }

    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // Override standard behavior to serialize paths as URI
            SimpleModule m = new SimpleModule("PathToString");
            m.addSerializer(Path.class, new ToNormalizedPathStringSerializer());
            m.addDeserializer(Path.class, new FromNormalizedPathStringDeserializer());
            m.addSerializer(Dimension.class, new DimensionSerializer());
            m.addDeserializer(Dimension.class, new DimensionDeserializer());
            objectMapper.registerModule(m);
        }
        return objectMapper;
    }

    public static void saveToFile(Object obj, Path targetFile) {
        try {
            getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(targetFile.toFile(), obj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readFromString(String json, Class<T> klass) {
        try {
            return getObjectMapper().readerFor(klass).readValue(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readFromFile(Path file, Class<T> klass) {
        try {
            return getObjectMapper().readerFor(klass).readValue(file.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toJsonString(Object data) {
        try {
            return getObjectMapper().writerFor(data.getClass()).writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toPrettyJsonString(Object data) {
        try {
            return getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static class DimensionSerializer extends JsonSerializer<Dimension> {
        @Override
        public void serialize(Dimension dimension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeNumberField("width", dimension.width);
            jsonGenerator.writeNumberField("height", dimension.height);
            jsonGenerator.writeEndObject();
        }
    }

    private static class DimensionDeserializer extends JsonDeserializer<Dimension> {
        @Override
        public Dimension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            return new Dimension(node.get("width").asInt(), node.get("height").asInt());
        }
    }

    private static class ToNormalizedPathStringSerializer extends JsonSerializer<Path> {
        @Override
        public void serialize(Path value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(StringUtils.nullToEmpty(value).replace('\\', '/'));
        }
    }

    private static class FromNormalizedPathStringDeserializer extends JsonDeserializer<Path> {
        @Override
        public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String s = p.readValueAs(String.class);
            if (StringUtils.isNullOrEmpty(s)) {
                return Paths.get("");
            } else {
                try {
                    return Paths.get(s.replace('\\', '/'));
                } catch (InvalidPathException e) {
                    return Paths.get("");
                }
            }
        }
    }
}
