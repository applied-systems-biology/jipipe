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

package org.hkijena.jipipe.extensions.parameters.references;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.api.grouping.JsonNodeInfo;

import java.io.IOException;

/**
 * Used by {@link JsonNodeInfo} to setup icons
 */
@JsonSerialize(using = JIPipeAlgorithmIconRef.Serializer.class)
@JsonDeserialize(using = JIPipeAlgorithmIconRef.Deserializer.class)
public class JIPipeAlgorithmIconRef {
    private String iconName;

    /**
     * Creates a new instance
     */
    public JIPipeAlgorithmIconRef() {
    }

    /**
     * Copies an instance
     *
     * @param other the original
     */
    public JIPipeAlgorithmIconRef(JIPipeAlgorithmIconRef other) {
        this.iconName = other.iconName;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    @Override
    public String toString() {
        return "" + iconName;
    }

    /**
     * Serializes {@link JIPipeAlgorithmIconRef}
     */
    public static class Serializer extends JsonSerializer<JIPipeAlgorithmIconRef> {
        @Override
        public void serialize(JIPipeAlgorithmIconRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.iconName);
        }
    }

    /**
     * Deserializes {@link JIPipeAlgorithmIconRef}
     */
    public static class Deserializer extends JsonDeserializer<JIPipeAlgorithmIconRef> {

        @Override
        public JIPipeAlgorithmIconRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            JIPipeAlgorithmIconRef result = new JIPipeAlgorithmIconRef();
            if (!node.isNull()) {
                result.setIconName(node.textValue());
            }
            return result;
        }
    }
}
