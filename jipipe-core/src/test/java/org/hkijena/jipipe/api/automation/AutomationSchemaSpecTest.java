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
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.json.schema.JsonArraySchema;
import org.hkijena.jipipe.utils.json.schema.JsonBooleanSchema;
import org.hkijena.jipipe.utils.json.schema.JsonIntegerSchema;
import org.hkijena.jipipe.utils.json.schema.JsonNumberSchema;
import org.hkijena.jipipe.utils.json.schema.JsonObjectSchema;
import org.hkijena.jipipe.utils.json.schema.JsonStringSchema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the typed schema spec renders the same JSON Schema as the general AST
 * (one source, two projections that cannot diverge) and exposes the flattened,
 * immutable parameter metadata.
 */
class AutomationSchemaSpecTest {

    @Test
    void specRendersTheSameJsonAsTheAstDirectly() throws Exception {
        AutomationSchemaSpec spec = AutomationSchemaSpec.object()
                .string("compartment", "Compartment whose nodes are arranged").required()
                .bool("apply", "true commits the computed positions").defaultsTo(false)
                .integer("limit", "Maximum results").defaultsTo(8).minimum(1)
                .enumeration("kind", "The note kind", "text-box", "comment").required()
                .arrayOfStrings("ids", "Nodes to move").required()
                .arrayOfObjects("authors", "Project authors", JsonObjectSchema.object()
                        .property("firstName", JsonStringSchema.string().description("Author first name"), true)
                        .build()).required()
                .number("factor", "Scale factor").defaultsTo(0.5)
                .build();
        JsonNode expected = JsonObjectSchema.object()
                .property("compartment", JsonStringSchema.string().description("Compartment whose nodes are arranged"), true)
                .property("apply", JsonBooleanSchema.bool().description("true commits the computed positions").defaultsTo(false))
                .property("limit", JsonIntegerSchema.integer().description("Maximum results").defaultsTo(8).minimum(1))
                .property("kind", JsonStringSchema.string().description("The note kind").enumeration("text-box", "comment"), true)
                .property("ids", JsonArraySchema.array(JsonStringSchema.string()).description("Nodes to move"), true)
                .property("authors", JsonArraySchema.array(JsonObjectSchema.object()
                        .property("firstName", JsonStringSchema.string().description("Author first name"), true)
                        .build()).description("Project authors"), true)
                .property("factor", JsonNumberSchema.number().description("Scale factor").defaultsTo(0.5))
                .build()
                .toJson();
        assertEquals(expected, spec.toJsonSchema(),
                "the spec's wire schema must be exactly the hand-built AST rendering");
    }

    @Test
    void parametersExposeNameTypeRequiredAndDefault() {
        AutomationSchemaSpec spec = AutomationSchemaSpec.object()
                .string("project", "The project").required()
                .integer("limit", "Maximum results").defaultsTo(8)
                .bool("verbose", "Verbose output")
                .build();
        List<AutomationParameterSpec> parameters = spec.getParameters();
        assertEquals(3, parameters.size(), "one spec entry per declared parameter");
        assertEquals("project", parameters.get(0).getName());
        assertTrue(parameters.get(0).isRequired(), "the required flag");
        assertNull(parameters.get(0).getDefaultValue(), "no default declared");
        assertEquals("string", parameters.get(0).getType().toJson().get("type").asText(),
                "the type is the general AST node");
        assertEquals("The project", parameters.get(0).getType().toJson().get("description").asText(),
                "the parameter description lives on the AST node");
        assertEquals("limit", parameters.get(1).getName());
        assertFalse(parameters.get(1).isRequired());
        assertEquals(8, parameters.get(1).getDefaultValue());
        assertEquals("integer", parameters.get(1).getType().toJson().get("type").asText());
        assertEquals("verbose", parameters.get(2).getName());
        assertFalse(parameters.get(2).isRequired());
        assertNull(parameters.get(2).getDefaultValue());
    }

    @Test
    void parameterListIsImmutableAndStable() {
        AutomationSchemaSpec spec = AutomationSchemaSpec.object()
                .string("project", "The project").required()
                .build();
        List<AutomationParameterSpec> first = spec.getParameters();
        List<AutomationParameterSpec> second = spec.getParameters();
        assertSame(first, second, "the frozen list instance is returned on every call");
        assertThrows(UnsupportedOperationException.class, () -> first.add(null),
                "the parameter list must be unmodifiable");
    }

    @Test
    void emptySpecRendersLikeTheLegacyEmptyObjectSchema() {
        // The interface default schema (getSchemaSpec() of an un-migrated operation) must be
        // structurally identical to the parameterless wire shape used today
        assertEquals(AutomationSchemaUtils.emptyObjectSchema(),
                AutomationSchemaSpec.object().build().toJsonSchema());
    }

    @Test
    void terminalMethodsChainInAnyOrder() throws Exception {
        JsonNode defaultsThenMinimum = AutomationSchemaSpec.object()
                .integer("limit", "Maximum results").defaultsTo(8).minimum(1)
                .build().toJsonSchema();
        JsonNode minimumThenDefaults = AutomationSchemaSpec.object()
                .integer("limit", "Maximum results").minimum(1).defaultsTo(8)
                .build().toJsonSchema();
        JsonNode expectedLimit = JsonUtils.getObjectMapper().readTree(
                "{\"type\":\"integer\",\"description\":\"Maximum results\",\"default\":8,\"minimum\":1}");
        assertEquals(defaultsThenMinimum, minimumThenDefaults,
                "both terminal orders must render the same schema");
        assertEquals(expectedLimit, defaultsThenMinimum.get("properties").get("limit"));
        AutomationSchemaSpec spec = AutomationSchemaSpec.object()
                .string("mode", "The mode").defaultsTo("fast").required()
                .build();
        assertTrue(spec.getParameters().get(0).isRequired(),
                "required() after defaultsTo() must reach the declared parameter");
        assertEquals("fast", spec.getParameters().get(0).getDefaultValue());
    }

    @Test
    void duplicateParameterNamesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> AutomationSchemaSpec.object()
                        .string("project", "The project")
                        .string("project", "The project again")
                        .build(),
                "duplicate names would silently collapse two parameters into one property");
    }

    @Test
    void minimumOnNonIntegerParameterIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> AutomationSchemaSpec.object()
                        .string("mode", "The mode")
                        .minimum(1)
                        .build(),
                "minimum is an integer keyword and must fail fast on other types");
    }

    @Test
    void arrayOfEnumStringsAndTheIntegralNumberDefaultRenderTheEnumeratedAndIntegralShapes() throws Exception {
        AutomationSchemaSpec spec = AutomationSchemaSpec.object()
                .arrayOfEnumStrings("fields", "Whitelist of fields", List.of("id", "name"))
                .number("scale", "Scale factor").defaultsTo(2)
                .build();
        JsonNode expected = JsonUtils.getObjectMapper().readTree("""
                {
                  "type": "object",
                  "properties": {
                    "fields": {"type": "array", "description": "Whitelist of fields",
                               "items": {"type": "string", "enum": ["id", "name"]}},
                    "scale": {"type": "number", "description": "Scale factor", "default": 2}
                  },
                  "additionalProperties": false
                }
                """);
        assertEquals(expected, spec.toJsonSchema(),
                "arrayOfEnumStrings must render the enumerated items and an int default on a number"
                        + " parameter the integral spelling");
    }

    @Test
    void arrayOfAnyAndFreeFormObjectRenderTheItemlessAndFreeFormShapes() throws Exception {
        AutomationSchemaSpec spec = AutomationSchemaSpec.object()
                .arrayOfAny("docker-env-vars", "Array of {key, value} objects")
                .freeFormObject("user-path-overrides", "Mapping of a user path to its role")
                .build();
        JsonNode expected = JsonUtils.getObjectMapper().readTree("""
                {
                  "type": "object",
                  "properties": {
                    "docker-env-vars": {"type": "array", "description": "Array of {key, value} objects"},
                    "user-path-overrides": {"type": "object", "description": "Mapping of a user path to its role"}
                  },
                  "additionalProperties": false
                }
                """);
        assertEquals(expected, spec.toJsonSchema(),
                "arrayOfAny must render no items and freeFormObject no properties or additionalProperties");
        assertEquals("array", spec.getParameters().get(0).getType().toJson().get("type").asText(),
                "the parameter metadata exposes the itemless array as the type");
        assertEquals("object", spec.getParameters().get(1).getType().toJson().get("type").asText(),
                "the parameter metadata exposes the free-form object as the type");
    }
}
