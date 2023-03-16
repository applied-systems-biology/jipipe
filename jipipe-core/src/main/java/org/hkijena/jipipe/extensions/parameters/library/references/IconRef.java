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

package org.hkijena.jipipe.extensions.parameters.library.references;

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
@JsonSerialize(using = IconRef.Serializer.class)
@JsonDeserialize(using = IconRef.Deserializer.class)
public class IconRef {
    private String iconName;

    /**
     * Creates a new instance
     */
    public IconRef() {
    }

    public IconRef(String iconName) {
        this.iconName = iconName;
    }

    /**
     * Copies an instance
     *
     * @param other the original
     */
    public IconRef(IconRef other) {
        if (other != null) {
            this.iconName = other.iconName;
        }
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
     * Serializes {@link IconRef}
     */
    public static class Serializer extends JsonSerializer<IconRef> {
        @Override
        public void serialize(IconRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.iconName);
        }
    }

    /**
     * Deserializes {@link IconRef}
     */
    public static class Deserializer extends JsonDeserializer<IconRef> {

        @Override
        public IconRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            IconRef result = new IconRef();
            if (!node.isNull()) {
                result.setIconName(node.textValue());
            }
            return result;
        }
    }
}
