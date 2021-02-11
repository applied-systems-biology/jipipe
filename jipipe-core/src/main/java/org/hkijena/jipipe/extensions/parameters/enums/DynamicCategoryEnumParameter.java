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

package org.hkijena.jipipe.extensions.parameters.enums;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.extensions.parameters.primitives.DynamicStringEnumParameter;

import javax.swing.*;
import java.io.IOException;

@JsonSerialize(using = DynamicCategoryEnumParameter.Serializer.class)
@JsonDeserialize(using = DynamicCategoryEnumParameter.Deserializer.class)
public class DynamicCategoryEnumParameter extends DynamicStringEnumParameter {

    /**
     * Creates a new instance with null value
     */
    public DynamicCategoryEnumParameter() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public DynamicCategoryEnumParameter(DynamicStringEnumParameter other) {
        setValue(other.getValue());
    }

    /**
     * Creates a new instance
     *
     * @param value initial value
     */
    public DynamicCategoryEnumParameter(String value) {
        super(value);
    }

    @Override
    public String renderLabel(String value) {
        JIPipeNodeTypeCategory category = JIPipe.getNodes().getRegisteredCategories().getOrDefault("" + value, null);
        if (category != null) {
            return category.getName();
        } else {
            return super.renderLabel(value);
        }
    }

    @Override
    public Icon renderIcon(String value) {
        JIPipeNodeTypeCategory category = JIPipe.getNodes().getRegisteredCategories().getOrDefault("" + value, null);
        if (category != null) {
            return category.getIcon();
        } else {
            return super.renderIcon(value);
        }
    }

    /**
     * Serializes {@link DynamicCategoryEnumParameter}
     */
    public static class Serializer extends JsonSerializer<DynamicCategoryEnumParameter> {
        @Override
        public void serialize(DynamicCategoryEnumParameter value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeString("" + value.getValue());
        }
    }

    /**
     * Deserializes {@link DynamicCategoryEnumParameter}
     */
    public static class Deserializer extends JsonDeserializer<DynamicCategoryEnumParameter> {
        @Override
        public DynamicCategoryEnumParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return new DynamicCategoryEnumParameter(((JsonNode) p.readValueAsTree()).textValue());
        }
    }
}
