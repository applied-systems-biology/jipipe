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

package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the simplified JSON schema validator used to check automation operation parameters.
 */
public class AutomationSchemaUtilsTest {

    private static final JsonNode STRICT_SCHEMA = AutomationSchemaUtils.parseJson("""
            {
              "type": "object",
              "required": ["x"],
              "properties": {
                "x": {"type": "integer"},
                "mode": {"type": "string", "enum": ["A", "B"]}
              }
            }""");

    @Test
    void rejectsMissingRequiredProperty() {
        List<String> errors = AutomationSchemaUtils.validate(STRICT_SCHEMA,
                AutomationSchemaUtils.parseJson("{}"));
        assertFalse(errors.isEmpty(), "missing required property must be an error");
        assertTrue(errors.get(0).contains("x"), "the error must name the missing property");
    }

    @Test
    void rejectsWrongType() {
        List<String> errors = AutomationSchemaUtils.validate(STRICT_SCHEMA,
                AutomationSchemaUtils.parseJson("{\"x\": \"1\"}"));
        assertFalse(errors.isEmpty(), "string for integer property must be an error");
    }

    @Test
    void rejectsValueOutsideEnum() {
        List<String> errors = AutomationSchemaUtils.validate(STRICT_SCHEMA,
                AutomationSchemaUtils.parseJson("{\"x\": 1, \"mode\": \"C\"}"));
        assertFalse(errors.isEmpty(), "value outside the enum must be an error");
        assertTrue(errors.get(0).contains("mode"), "the error must name the property");
    }

    @Test
    void rejectsUnknownPropertyWhenAdditionalPropertiesIsFalse() {
        JsonNode schema = AutomationSchemaUtils.parseJson("""
                {
                  "type": "object",
                  "properties": {"x": {"type": "integer"}},
                  "additionalProperties": false
                }""");
        List<String> errors = AutomationSchemaUtils.validate(schema,
                AutomationSchemaUtils.parseJson("{\"x\": 1, \"extra\": 2}"));
        assertFalse(errors.isEmpty(), "unknown property must be an error");
        assertTrue(errors.get(0).contains("extra"), "the error must name the unknown property");
    }

    @Test
    void acceptsValidObject() {
        List<String> errors = AutomationSchemaUtils.validate(STRICT_SCHEMA,
                AutomationSchemaUtils.parseJson("{\"x\": 1, \"mode\": \"A\"}"));
        assertEquals(List.of(), errors, "valid arguments must produce no errors");
    }

    @Test
    void acceptsMissingOptionalProperties() {
        List<String> errors = AutomationSchemaUtils.validate(STRICT_SCHEMA,
                AutomationSchemaUtils.parseJson("{\"x\": 1}"));
        assertEquals(List.of(), errors, "optional properties may be absent");
    }
}
