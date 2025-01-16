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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.scripting.MacroUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

/**
 * An {@link AbstractExpressionParameter} that utilizes the {@link JIPipeExpressionEvaluator} to generate results
 */
@JsonSerialize(using = JIPipeExpressionParameter.Serializer.class)
@JsonDeserialize(using = JIPipeExpressionParameter.Deserializer.class)
public class JIPipeExpressionParameter extends AbstractExpressionParameter {
    private static JIPipeExpressionEvaluator EVALUATOR;

    private java.util.Set<JIPipeExpressionParameterVariableInfo> additionalUIVariables = new HashSet<>();

    public JIPipeExpressionParameter() {
    }

    public JIPipeExpressionParameter(String expression) {
        super(expression);
    }

    public JIPipeExpressionParameter(AbstractExpressionParameter other) {
        super(other);
    }

    public static JIPipeExpressionEvaluator getEvaluatorInstance() {
        if (EVALUATOR == null) {
            EVALUATOR = new JIPipeExpressionEvaluator();
            // Prevent evaluator stuck without registered functions
            if (JIPipe.getInstance() != null) {
                JIPipe.getInstance().getExtensionRegisteredEventEmitter().subscribeLambda((emitter, event) -> {
                    EVALUATOR = new JIPipeExpressionEvaluator();
                });
            }
        }
        return EVALUATOR;
    }

    /**
     * Escapes a string
     *
     * @param s         the string
     * @param addQuotes if quotes should be added
     * @return the escaped string
     */
    public static String escapeString(String s, boolean addQuotes) {
        if (addQuotes) {
            return String.format("\"%s\"", MacroUtils.escapeString(s));
        } else {
            return MacroUtils.escapeString(s);
        }
    }

    /**
     * Escapes a string as valid expression (with quotes)
     *
     * @param s the string
     * @return the escaped string
     */
    public static String escapeString(String s) {
        return escapeString(s, true);
    }

    /**
     * Returns true if a string is a valid variable name
     *
     * @param key the string
     * @return if the key is a valid variable name
     */
    public static boolean isValidVariableName(String key) {
        return MacroUtils.isValidVariableName(key);
    }

    @Override
    public ExpressionEvaluator getEvaluator() {
        return getEvaluatorInstance();
    }

    public java.util.Set<JIPipeExpressionParameterVariableInfo> getAdditionalUIVariables() {
        return additionalUIVariables;
    }

    public void setAdditionalUIVariables(java.util.Set<JIPipeExpressionParameterVariableInfo> additionalUIVariables) {
        this.additionalUIVariables = additionalUIVariables;
    }

    public static class List extends ListParameter<JIPipeExpressionParameter> {
        public List() {
            super(JIPipeExpressionParameter.class);
        }

        public List(Collection<JIPipeExpressionParameter> other) {
            super(JIPipeExpressionParameter.class);
            for (JIPipeExpressionParameter parameter : other) {
                add(new JIPipeExpressionParameter(parameter));
            }
        }
    }

    public static class Deserializer extends JsonDeserializer<JIPipeExpressionParameter> implements ContextualDeserializer {

        private JavaType deserializedType;

        @Override
        public JIPipeExpressionParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode root = p.readValueAsTree();
            if (root.isNumber()) {
                // Upgrade a numeric parameter to expression
                return (JIPipeExpressionParameter) ReflectionUtils.newInstance(deserializedType.getRawClass(), root.numberValue().toString());
            } else if (root.isTextual()) {
                return (JIPipeExpressionParameter) ReflectionUtils.newInstance(deserializedType.getRawClass(), JIPipeExpressionParameter.escapeString(root.textValue(), true));
            } else if (root.isObject()) {
                String expression = "";
                if (root.has("expression")) {
                    expression = root.get("expression").asText();
                }
                return (JIPipeExpressionParameter) ReflectionUtils.newInstance(deserializedType.getRawClass(), expression);
            } else {
                return (JIPipeExpressionParameter) ReflectionUtils.newInstance(deserializedType.getRawClass());
            }
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            //beanProperty is null when the type to deserialize is the top-level type or a generic type, not a type of a bean property
            JavaType type = ctxt.getContextualType() != null
                    ? ctxt.getContextualType()
                    : property.getMember().getType();
            Deserializer deserializer = new Deserializer();
            deserializer.deserializedType = type;
            return deserializer;
        }
    }

    public static class Serializer extends JsonSerializer<JIPipeExpressionParameter> {
        @Override
        public void serialize(JIPipeExpressionParameter value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("expression", value.getExpression());
            gen.writeEndObject();
        }
    }
}
