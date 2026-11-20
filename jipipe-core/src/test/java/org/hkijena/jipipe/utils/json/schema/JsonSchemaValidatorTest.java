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

package org.hkijena.jipipe.utils.json.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hkijena.jipipe.utils.json.schema.JsonIntegerSchema.integer;
import static org.hkijena.jipipe.utils.json.schema.JsonObjectSchema.object;
import static org.hkijena.jipipe.utils.json.schema.JsonStringSchema.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link JsonSchemaValidator}.
 * Schemas are built with the AST of this package, arguments are raw JSON nodes
 * (as they arrive from external callers).
 */
class JsonSchemaValidatorTest {

    @Test
    void missingRequiredIsReportedAndValidArgsAreAccepted() throws Exception {
        JsonNode schema = object().property("name", string(), true).build().toJson();
        List<String> errors = JsonSchemaValidator.validate(schema,
                JsonUtils.getObjectMapper().readTree("{}"));
        assertEquals(List.of("name is required"), errors);
        List<String> valid = JsonSchemaValidator.validate(schema,
                JsonUtils.getObjectMapper().readTree("{\"name\": \"abc\"}"));
        assertEquals(List.of(), valid);
    }

    @Test
    void wrongTypeIsReported() throws Exception {
        JsonNode schema = object().property("x", integer()).build().toJson();
        List<String> errors = JsonSchemaValidator.validate(schema,
                JsonUtils.getObjectMapper().readTree("{\"x\": \"1\"}"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("x"), "the error must name the property: " + errors);
        assertTrue(errors.get(0).contains("integer"), "the error must mention the expected type: " + errors);
    }

    @Test
    void unknownPropertyIsRejectedWhenAdditionalPropertiesIsFalse() throws Exception {
        JsonNode schema = object().property("x", integer()).build().toJson();
        List<String> errors = JsonSchemaValidator.validate(schema,
                JsonUtils.getObjectMapper().readTree("{\"x\": 1, \"extra\": 2}"));
        assertEquals(List.of("extra is not an allowed property"), errors);
    }

    @Test
    void enumViolationIsReported() throws Exception {
        JsonNode schema = object().property("mode", string().enumeration("A", "B")).build().toJson();
        List<String> errors = JsonSchemaValidator.validate(schema,
                JsonUtils.getObjectMapper().readTree("{\"mode\": \"C\"}"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("mode"), "the error must name the property: " + errors);
        assertTrue(errors.get(0).contains("must be one of"), "the error must describe the violation: " + errors);
    }

    @Test
    void nestedObjectRequiredAndAdditionalAreEnforced() throws Exception {
        JsonNode schema = object().property("sub", object().property("name", string(), true).build()).build().toJson();
        List<String> missing = JsonSchemaValidator.validate(schema,
                JsonUtils.getObjectMapper().readTree("{\"sub\": {}}"));
        assertEquals(List.of("sub.name is required"), missing);
        List<String> additional = JsonSchemaValidator.validate(schema,
                JsonUtils.getObjectMapper().readTree("{\"sub\": {\"name\": \"n\", \"extra\": 1}}"));
        assertEquals(List.of("sub.extra is not an allowed property"), additional);
        assertEquals(List.of(), JsonSchemaValidator.validate(schema,
                JsonUtils.getObjectMapper().readTree("{\"sub\": {\"name\": \"n\"}}")));
    }

    @Test
    void nullArgsAreTreatedAsEmptyObject() {
        JsonNode schema = object().property("name", string(), true).build().toJson();
        List<String> errors = JsonSchemaValidator.validate(schema, null);
        assertEquals(List.of("name is required"), errors);
    }
}
