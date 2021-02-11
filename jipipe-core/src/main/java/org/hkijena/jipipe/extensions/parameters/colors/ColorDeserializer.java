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

package org.hkijena.jipipe.extensions.parameters.colors;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.*;
import java.io.IOException;

/**
 * Deserializer for {@link Color}
 */
public class ColorDeserializer extends JsonDeserializer<Color> {
    @Override
    public Color deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.readValueAsTree();
        if (node.isTextual()) {
            // User provided Hex string #RRGGBB or #RRGGBBAA
            String s = node.textValue();
            if (s.startsWith("#")) {
                return ColorUtils.hexStringToColor(s);
            } else {
                return Color.decode(s);
            }
        } else {
            JsonNode redNode = node.path("r");
            JsonNode greenNode = node.path("g");
            JsonNode blueNode = node.path("b");
            JsonNode alphaNode = node.path("a");
            JsonNode hueNode = node.path("h");
            JsonNode saturationNode = node.path("s");
            JsonNode brightnessNode = node.path("b");
            if (brightnessNode.isMissingNode()) {
                brightnessNode = node.path("v");
            }

            if (!redNode.isMissingNode() && !greenNode.isMissingNode() && !blueNode.isMissingNode()) {
                int alpha = 255;
                if (!alphaNode.isMissingNode()) {
                    alpha = alphaNode.asInt();
                }
                return new Color(redNode.asInt(), greenNode.asInt(), blueNode.asInt(), alpha);
            } else if (!hueNode.isMissingNode() && !saturationNode.isMissingNode() && !brightnessNode.isMissingNode()) {
                Color hsv = Color.getHSBColor((float) hueNode.asDouble(), (float) saturationNode.asDouble(), (float) brightnessNode.asDouble());
                int alpha = 255;
                if (!alphaNode.isMissingNode()) {
                    alpha = alphaNode.asInt();
                }
                return new Color(hsv.getRed(), hsv.getGreen(), hsv.getBlue(), alpha);
            } else {
                throw new RuntimeException("Invalid color format! Allowed are RGB or RGBA hex strings, or objects that define 'r', 'g', 'b' or 'h', 's', 'b'/'v' and optionally alpha 'a'");
            }
        }
    }


}
