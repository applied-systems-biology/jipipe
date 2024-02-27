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

package org.hkijena.jipipe.extensions.expressions.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Parse JSON", description = "Converts a JSON string to an object. Can output a boolean, string, number, array, or map.")
public class ParseJsonFunction extends ExpressionFunction {

    public ParseJsonFunction() {
        super("PARSE_JSON", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        try {
            JsonNode node = JsonUtils.getObjectMapper().readTree(parameters.get(0) + "");
            return parseJson(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Object parseJson(JsonNode node) {
        if (node.isBoolean())
            return node.asBoolean();
        else if (node.isTextual())
            return node.asText();
        else if (node.isNumber())
            return node.asDouble();
        else if (node.isArray()) {
            List<Object> result = new ArrayList<>();
            for (JsonNode element : ImmutableList.copyOf(node.elements())) {
                result.add(parseJson(element));
            }
            return result;
        } else if (node.isObject()) {
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(node.fields())) {
                result.put(entry.getKey(), parseJson(entry.getValue()));
            }
            return result;
        } else {
            return null;
        }
    }
}
