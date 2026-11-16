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

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the registry validates parameters against the operation schema before dispatching.
 */
public class AutomationRegistryValidationTest {

    private static JsonNode emptyResult() {
        return AutomationSchemaUtils.parseJson("{\"ok\": true}");
    }

    /**
     * Returns a context that is never touched by the stub operations.
     */
    private static AutomationContext unusedContext() {
        return new AutomationContext(null, null, null, null, null);
    }

    @Test
    void invalidParametersThrowWithoutInvokingTheOperation() {
        AutomationOperationRegistry registry = new AutomationOperationRegistry();
        registry.register("stub", new AutomationOperation() {
            @Override
            public String getId() {
                return "stub";
            }

            @Override
            public String getDescription() {
                return "stub";
            }

            @Override
            public JsonNode getInputSchema() {
                return AutomationSchemaUtils.parseJson("""
                        {"type": "object", "required": ["x"], "properties": {"x": {"type": "integer"}}}""");
            }

            @Override
            public JsonNode execute(AutomationContext ctx, JsonNode params) {
                throw new AssertionError("must not be called");
            }
        });

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registry.execute("stub", unusedContext(), AutomationSchemaUtils.parseJson("{}")));
        assertTrue(ex.getMessage().contains("x"), "the exception must list the schema errors");
    }

    @Test
    void validParametersAreDispatched() {
        AutomationOperationRegistry registry = new AutomationOperationRegistry();
        AtomicBoolean called = new AtomicBoolean(false);
        registry.register("stub", new AutomationOperation() {
            @Override
            public String getId() {
                return "stub";
            }

            @Override
            public String getDescription() {
                return "stub";
            }

            @Override
            public JsonNode getInputSchema() {
                return AutomationSchemaUtils.parseJson("""
                        {"type": "object", "required": ["x"], "properties": {"x": {"type": "integer"}}}""");
            }

            @Override
            public JsonNode execute(AutomationContext ctx, JsonNode params) {
                called.set(true);
                return emptyResult();
            }
        });

        JsonNode result = assertDoesNotThrowExecute(registry, AutomationSchemaUtils.parseJson("{\"x\": 1}"));
        assertTrue(called.get(), "valid parameters must reach the operation");
        assertTrue(result.get("ok").asBoolean(), "the operation result must be returned");
    }

    @Test
    void unknownOperationIdThrows() {
        AutomationOperationRegistry registry = new AutomationOperationRegistry();
        assertThrows(IllegalArgumentException.class,
                () -> registry.execute("missing", unusedContext(), AutomationSchemaUtils.parseJson("{}")));
    }

    @Test
    void missingParametersAreTreatedAsEmptyObject() {
        AutomationOperationRegistry registry = new AutomationOperationRegistry();
        registry.register("stub", new AutomationOperation() {
            @Override
            public String getId() {
                return "stub";
            }

            @Override
            public String getDescription() {
                return "stub";
            }

            @Override
            public JsonNode getInputSchema() {
                return AutomationSchemaUtils.parseJson("""
                        {"type": "object", "properties": {}, "additionalProperties": false}""");
            }

            @Override
            public JsonNode execute(AutomationContext ctx, JsonNode params) {
                return emptyResult();
            }
        });

        assertFalse(assertDoesNotThrowExecute(registry, null).isEmpty(), "a parameterless operation runs with null params");
    }

    private static JsonNode assertDoesNotThrowExecute(AutomationOperationRegistry registry, JsonNode params) {
        try {
            return registry.execute("stub", unusedContext(), params);
        } catch (Exception e) {
            throw new AssertionError("valid parameters must not throw", e);
        }
    }
}