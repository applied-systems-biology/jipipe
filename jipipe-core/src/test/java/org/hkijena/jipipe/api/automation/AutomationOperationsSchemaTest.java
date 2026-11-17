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
import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the input schemas of the REAL registered automation operations (spec section 11
 * intent): a typo'd schema text block must surface here, not only at first dispatch.
 * <p>
 * Plugin-aware per the test policy: presence-based assertions on a curated set of core
 * operation ids, no pinned registry counts (plugins may register more operations).
 */
public class AutomationOperationsSchemaTest {

    /**
     * Stable core operation ids that must be registered (presence gate: the tests below
     * would pass vacuously against an empty registry otherwise).
     */
    private static final Set<String> EXPECTED_CORE_OPERATION_IDS = Set.of(
            "list_projects",
            "list_operations",
            "list_algorithms",
            "get_project_info",
            "get_pipeline_map",
            "run_pipeline");

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static AutomationOperationRegistry registry() {
        return JIPipe.getInstance().getAutomation().getOperationRegistry();
    }

    @Test
    void coreOperationsAreRegistered() {
        Map<String, AutomationOperation> operations = registry().getAll();
        assertFalse(operations.isEmpty(), "the automation registry must not be empty");
        for (String id : EXPECTED_CORE_OPERATION_IDS) {
            assertTrue(operations.containsKey(id),
                    "core operation '" + id + "' must be registered (found: " + operations.keySet() + ")");
        }
    }

    @Test
    void everyOperationDeclaresAnObjectSchema() {
        for (Map.Entry<String, AutomationOperation> entry : registry().getAll().entrySet()) {
            JsonNode schema = entry.getValue().getInputSchema();
            assertNotNull(schema, "operation '" + entry.getKey() + "' must declare an input schema");
            assertTrue(schema.isObject(), "the input schema of '" + entry.getKey()
                    + "' must be a JSON object, got: " + schema.getNodeType());
            JsonNode type = schema.get("type");
            assertNotNull(type, "the input schema of '" + entry.getKey() + "' must declare a type");
            assertEquals("object", type.asText(), "the input schema type of '" + entry.getKey() + "'");
        }
    }

    @Test
    void emptyArgsValidateAccordingToRequired() {
        JsonNode emptyArgs = AutomationSchemaUtils.parseJson("{}");
        for (Map.Entry<String, AutomationOperation> entry : registry().getAll().entrySet()) {
            JsonNode schema = entry.getValue().getInputSchema();
            JsonNode required = schema.get("required");
            boolean hasRequired = required != null && required.isArray() && !required.isEmpty();
            List<String> errors = AutomationSchemaUtils.validate(schema, emptyArgs);
            if (hasRequired) {
                assertFalse(errors.isEmpty(), "empty args against the schema of '" + entry.getKey()
                        + "' must be rejected (required: " + required + ")");
                String firstRequired = required.get(0).asText();
                assertTrue(errors.get(0).contains(firstRequired), "the first error for '" + entry.getKey()
                        + "' must name the first required property '" + firstRequired + "': " + errors);
            } else {
                assertEquals(List.of(), errors, "empty args against the schema of '"
                        + entry.getKey() + "' (no required properties) must validate cleanly");
            }
        }
    }
}
