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

package org.hkijena.jipipe.extensions.parameters.primitives;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Parameter that acts as dynamic enum.
 * Use {@link DynamicEnumParameterSettings} to define a supplier for the
 * items.
 */
@JsonSerialize(using = DynamicStringEnumParameter.Serializer.class)
@JsonDeserialize(using = DynamicStringEnumParameter.Deserializer.class)
public class DynamicStringEnumParameter extends DynamicEnumParameter<String> {
    /**
     * Creates a new instance with null value
     */
    public DynamicStringEnumParameter() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public DynamicStringEnumParameter(DynamicStringEnumParameter other) {
        setValue(other.getValue());
        setAllowedValues(new ArrayList<>(other.getAllowedValues()));
    }

    /**
     * Creates a new instance
     *
     * @param value initial value
     */
    public DynamicStringEnumParameter(String value) {
        super(value);
    }

    /**
     * Serializes {@link DynamicStringEnumParameter}
     */
    public static class Serializer extends JsonSerializer<DynamicStringEnumParameter> {
        @Override
        public void serialize(DynamicStringEnumParameter value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeString("" + value.getValue());
        }
    }

    /**
     * Deserializes {@link DynamicStringEnumParameter}
     */
    public static class Deserializer extends JsonDeserializer<DynamicStringEnumParameter> {
        @Override
        public DynamicStringEnumParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return new DynamicStringEnumParameter(((JsonNode) p.readValueAsTree()).textValue());
        }
    }
}
