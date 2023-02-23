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
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.parameters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hkijena.jipipe.extensions.parameters.library.primitives.DynamicStringEnumParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonDeserialize(using = SpotFeature.Deserializer.class)
public class SpotFeature extends DynamicStringEnumParameter {

    public static final List<String> ALLOWED_VALUES = new ArrayList<>();
    public static final Map<String, String> VALUE_LABELS = new HashMap<>();

    public SpotFeature() {
        setAllowedValues(ALLOWED_VALUES);
    }

    public SpotFeature(SpotFeature other) {
        super(other);
        setAllowedValues(ALLOWED_VALUES);
    }

    public SpotFeature(String value) {
        super(value);
        setAllowedValues(ALLOWED_VALUES);
    }

    @Override
    public String renderLabel(String value) {
        return VALUE_LABELS.getOrDefault(value, value);
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    public static class Deserializer extends JsonDeserializer<DynamicStringEnumParameter> {
        @Override
        public DynamicStringEnumParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return new SpotFeature(((JsonNode) p.readValueAsTree()).textValue());
        }
    }
}
