/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.expressions;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;

@JsonDeserialize(using = OptionalJIPipeExpressionParameter.Deserializer.class)
public class OptionalJIPipeExpressionParameter extends OptionalParameter<JIPipeExpressionParameter> {

    public OptionalJIPipeExpressionParameter() {
        super(JIPipeExpressionParameter.class);
        setContent(new JIPipeExpressionParameter());
    }

    public OptionalJIPipeExpressionParameter(boolean enabled, String expression) {
        super(JIPipeExpressionParameter.class);
        setEnabled(enabled);
        setContent(new JIPipeExpressionParameter(expression));
    }

    public OptionalJIPipeExpressionParameter(OptionalParameter<JIPipeExpressionParameter> other) {
        super(other);
        this.setContent(new JIPipeExpressionParameter(other.getContent()));
    }

    /**
     * Deserializes the parameter
     */
    public static class Deserializer extends JsonDeserializer<OptionalJIPipeExpressionParameter> {

        @Override
        public OptionalJIPipeExpressionParameter deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode root = jsonParser.readValueAsTree();

            OptionalJIPipeExpressionParameter parameter = new OptionalJIPipeExpressionParameter();
            ObjectReader objectReader = JsonUtils.getObjectMapper().readerFor(parameter.getContentClass());

            if (root.isObject() && root.has("enabled")) {
                parameter.setEnabled(root.get("enabled").booleanValue());
                parameter.setContent(objectReader.readValue(root.get("content")));
            } else {
                // Fallback for conversion from content to optional parameter
                parameter.setContent(objectReader.readValue(root));

                // Special case for expression style parameters: enable if not empty
                parameter.setEnabled(!parameter.getContent().isEmpty());
            }

            return parameter;
        }

    }
}
