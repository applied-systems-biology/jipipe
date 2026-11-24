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

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the registry fills absent optional parameters from the schema's top-level
 * {@code default} keywords before dispatch: the default reaches the operation, an explicit
 * value wins, required parameters still fail validation, parameters without a default stay
 * absent, the caller's node is never mutated, and an operation can opt out when its
 * absence-vs-value semantics demand it.
 */
class AutomationOperationRegistryDefaultsTest {

    private static final String LIMIT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "x": {"type": "integer"},
                "limit": {"type": "integer", "default": 8}
              },
              "additionalProperties": false
            }""";

    /**
     * Returns a context that is never touched by the stub operations.
     */
    private static AutomationContext unusedContext() {
        return new AutomationContext(null, null, null, null, null);
    }

    /**
     * Returns a local registry holding one stub operation with the given schema.
     * The operation records the params node it received.
     */
    private static AutomationOperationRegistry registryWith(String schemaJson, AtomicReference<JsonNode> received) {
        return registryWith(schemaJson, received, true);
    }

    /**
     * Same as {@link #registryWith(String, AtomicReference)}, with control over the
     * schema-defaults opt-out.
     */
    private static AutomationOperationRegistry registryWith(String schemaJson, AtomicReference<JsonNode> received,
                                                            boolean appliesSchemaDefaults) {
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
                return AutomationSchemaUtils.parseJson(schemaJson);
            }

            @Override
            public boolean appliesSchemaDefaults() {
                return appliesSchemaDefaults;
            }

            @Override
            public JsonNode execute(AutomationContext ctx, JsonNode params) {
                received.set(params);
                return AutomationSchemaUtils.parseJson("{\"ok\": true}");
            }
        });
        return registry;
    }

    @Test
    void absentOptionalGetsItsDefault() throws Exception {
        AtomicReference<JsonNode> received = new AtomicReference<>();
        AutomationOperationRegistry registry = registryWith(LIMIT_SCHEMA, received);
        registry.execute("stub", unusedContext(), AutomationSchemaUtils.parseJson("{\"x\": 1}"));
        assertEquals(8, received.get().get("limit").asInt(),
                "the schema default must reach the operation");
    }

    @Test
    void callerArgsNodeIsNotMutated() throws Exception {
        AtomicReference<JsonNode> received = new AtomicReference<>();
        AutomationOperationRegistry registry = registryWith(LIMIT_SCHEMA, received);
        JsonNode params = AutomationSchemaUtils.parseJson("{\"x\": 1}");
        JsonNode snapshot = params.deepCopy();
        registry.execute("stub", unusedContext(), params);
        assertEquals(snapshot, params, "the caller's node must stay structurally untouched");
        assertEquals(snapshot.toString(), params.toString(), "the caller's node must stay byte-identical");
        assertFalse(params.has("limit"), "the default must not leak into the caller's node");
        assertNotSame(params, received.get(),
                "the operation must receive a new node, never the caller's instance");
        assertEquals(8, received.get().get("limit").asInt());
    }

    @Test
    void presentValueWinsOverDefault() throws Exception {
        AtomicReference<JsonNode> received = new AtomicReference<>();
        AutomationOperationRegistry registry = registryWith(LIMIT_SCHEMA, received);
        registry.execute("stub", unusedContext(), AutomationSchemaUtils.parseJson("{\"limit\": 3}"));
        assertEquals(3, received.get().get("limit").asInt(),
                "an explicit value must win over the default");
    }

    @Test
    void absentRequiredStillFailsValidation() throws Exception {
        AtomicReference<JsonNode> received = new AtomicReference<>();
        AutomationOperationRegistry registry = registryWith("""
                {
                  "type": "object",
                  "required": ["x"],
                  "properties": {"x": {"type": "integer"}}
                }""", received);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registry.execute("stub", unusedContext(), AutomationSchemaUtils.parseJson("{}")));
        assertTrue(ex.getMessage().contains("x"), "the exception must list the validation errors");
        assertNull(received.get(), "a failed validation must not invoke the operation");
    }

    @Test
    void requiredWithDeclaredDefaultStillFailsValidation() throws Exception {
        // Validation runs before the default application, so a required parameter is never
        // defaulted into existence
        AtomicReference<JsonNode> received = new AtomicReference<>();
        AutomationOperationRegistry registry = registryWith("""
                {
                  "type": "object",
                  "required": ["x"],
                  "properties": {"x": {"type": "integer", "default": 42}}
                }""", received);
        assertThrows(IllegalArgumentException.class,
                () -> registry.execute("stub", unusedContext(), AutomationSchemaUtils.parseJson("{}")));
        assertNull(received.get(), "a failed validation must not invoke the operation");
    }

    @Test
    void parameterWithoutDefaultIsNotInjected() throws Exception {
        AtomicReference<JsonNode> received = new AtomicReference<>();
        AutomationOperationRegistry registry = registryWith("""
                {
                  "type": "object",
                  "properties": {"x": {"type": "integer"}},
                  "additionalProperties": false
                }""", received);
        registry.execute("stub", unusedContext(), AutomationSchemaUtils.parseJson("{}"));
        assertNotNull(received.get());
        assertFalse(received.get().has("x"),
                "an absent optional parameter without a default must stay absent (presence semantics preserved)");
    }

    @Test
    void nullArgsAreDispatchedAsAnObjectWithDefaultsApplied() throws Exception {
        AtomicReference<JsonNode> received = new AtomicReference<>();
        AutomationOperationRegistry registry = registryWith(LIMIT_SCHEMA, received);
        registry.execute("stub", unusedContext(), null);
        assertNotNull(received.get(), "null args must be dispatched as an object");
        assertEquals(8, received.get().get("limit").asInt());
    }

    @Test
    void nestedDefaultsAreNotApplied() throws Exception {
        // Top-level only: the registry must not recurse into nested objects or array items
        AtomicReference<JsonNode> received = new AtomicReference<>();
        AutomationOperationRegistry registry = registryWith("""
                {
                  "type": "object",
                  "properties": {
                    "options": {
                      "type": "object",
                      "properties": {"verbose": {"type": "boolean", "default": true}}
                    }
                  },
                  "additionalProperties": false
                }""", received);
        registry.execute("stub", unusedContext(), AutomationSchemaUtils.parseJson("{\"options\": {}}"));
        assertFalse(received.get().get("options").has("verbose"),
                "nested object defaults must not be applied");
    }

    @Test
    void operationCanOptOutOfDefaultApplication() throws Exception {
        AtomicReference<JsonNode> received = new AtomicReference<>();
        AutomationOperationRegistry registry = registryWith(LIMIT_SCHEMA, received, false);
        JsonNode params = AutomationSchemaUtils.parseJson("{}");
        registry.execute("stub", unusedContext(), params);
        assertNotNull(received.get());
        assertFalse(received.get().has("limit"),
                "an opted-out operation must see absence, not the default");
    }

    @Test
    void applyDefaultsReturnsANewNodeAndTreatsNullAsEmpty() {
        JsonNode schema = AutomationSchemaUtils.parseJson(LIMIT_SCHEMA);
        JsonNode args = AutomationSchemaUtils.parseJson("{\"limit\": 3}");
        JsonNode result = AutomationSchemaUtils.applyDefaults(schema, args);
        assertNotSame(args, result, "applyDefaults must return a new node");
        assertEquals(3, result.get("limit").asInt(), "a present value must win");
        JsonNode fromNull = AutomationSchemaUtils.applyDefaults(schema, null);
        assertEquals(8, fromNull.get("limit").asInt(), "null args are treated as an empty object");
        assertEquals(AutomationSchemaUtils.parseJson("{}"),
                AutomationSchemaUtils.applyDefaults(null, AutomationSchemaUtils.parseJson("{}")),
                "a null schema changes nothing");
    }
}
