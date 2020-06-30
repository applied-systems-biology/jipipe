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

package org.hkijena.acaq5.extensions.parameters.references;

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
import org.hkijena.acaq5.api.grouping.JsonAlgorithmDeclaration;

import java.io.IOException;

/**
 * Used by {@link JsonAlgorithmDeclaration} to setup icons
 */
@JsonSerialize(using = ACAQAlgorithmIconRef.Serializer.class)
@JsonDeserialize(using = ACAQAlgorithmIconRef.Deserializer.class)
public class ACAQAlgorithmIconRef {
    private String iconName;

    /**
     * Creates a new instance
     */
    public ACAQAlgorithmIconRef() {
    }

    /**
     * Copies an instance
     *
     * @param other the original
     */
    public ACAQAlgorithmIconRef(ACAQAlgorithmIconRef other) {
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
     * Serializes {@link ACAQAlgorithmIconRef}
     */
    public static class Serializer extends JsonSerializer<ACAQAlgorithmIconRef> {
        @Override
        public void serialize(ACAQAlgorithmIconRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.iconName);
        }
    }

    /**
     * Deserializes {@link ACAQAlgorithmIconRef}
     */
    public static class Deserializer extends JsonDeserializer<ACAQAlgorithmIconRef> {

        @Override
        public ACAQAlgorithmIconRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            ACAQAlgorithmIconRef result = new ACAQAlgorithmIconRef();
            if (!node.isNull()) {
                result.setIconName(node.textValue());
            }
            return result;
        }
    }
}
