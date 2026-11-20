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

import static org.hkijena.jipipe.utils.json.schema.JsonArraySchema.array;
import static org.hkijena.jipipe.utils.json.schema.JsonIntegerSchema.integer;
import static org.hkijena.jipipe.utils.json.schema.JsonNumberSchema.number;
import static org.hkijena.jipipe.utils.json.schema.JsonObjectSchema.object;
import static org.hkijena.jipipe.utils.json.schema.JsonStringSchema.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests that the JSON Schema AST renders the expected JSON Schema keywords
 * (structurally compared, so keyword order does not matter).
 */
class JsonSchemaRenderingTest {

    @Test
    void stringSchemaRendersTypeDescriptionEnumAndDefault() throws Exception {
        JsonNode expected = JsonUtils.getObjectMapper().readTree("""
                {"type":"string","description":"d","enum":["a","b"],"default":"a"}
                """);
        JsonNode actual = string().description("d").enumeration("a", "b").defaultsTo("a").toJson();
        assertEquals(expected, actual);
    }

    @Test
    void integerSchemaRendersMinimumAndDefault() throws Exception {
        JsonNode expected = JsonUtils.getObjectMapper().readTree("""
                {"type":"integer","description":"d","default":8,"minimum":1}
                """);
        JsonNode actual = integer().description("d").minimum(1).defaultsTo(8).toJson();
        assertEquals(expected, actual);
    }

    @Test
    void numberSchemaRendersAnIntegralDefaultAsAJsonInteger() throws Exception {
        JsonNode expected = JsonUtils.getObjectMapper().readTree("""
                {"type":"number","description":"d","default":2}
                """);
        JsonNode actual = number().description("d").defaultsTo(2).toJson();
        assertEquals(expected, actual, "an int default keeps the integral spelling (2, not 2.0)");
    }

    @Test
    void numberSchemaRendersAFractionalDefaultAsADouble() throws Exception {
        JsonNode expected = JsonUtils.getObjectMapper().readTree("""
                {"type":"number","description":"d","default":0.5}
                """);
        JsonNode actual = number().description("d").defaultsTo(0.5).toJson();
        assertEquals(expected, actual);
    }

    @Test
    void objectSchemaRendersRequiredInDeclarationOrderAndAdditionalPropertiesFalse() throws Exception {
        JsonNode expected = JsonUtils.getObjectMapper().readTree("""
                {
                  "type": "object",
                  "required": ["b", "a"],
                  "properties": {
                    "b": {"type": "string", "description": "B"},
                    "a": {"type": "string", "description": "A"}
                  },
                  "additionalProperties": false
                }
                """);
        JsonNode actual = object()
                .property("b", string().description("B"), true)
                .property("a", string().description("A"), true)
                .build()
                .toJson();
        assertEquals(expected, actual);
    }

    @Test
    void objectSchemaOmitsRequiredWhenEmptyAndOmitsEmptyDescription() {
        JsonNode actual = object().property("x", string().description("X")).build().toJson();
        assertFalse(actual.has("required"));
        assertFalse(actual.has("description"));
        assertEquals(3, actual.size());
    }

    @Test
    void freeFormObjectRendersOnlyTypeAndDescription() throws Exception {
        JsonNode expected = JsonUtils.getObjectMapper().readTree("""
                {"type":"object","description":"Arbitrary object payload"}
                """);
        JsonNode actual = JsonObjectSchema.freeFormObject("Arbitrary object payload").toJson();
        assertEquals(expected, actual);
    }

    @Test
    void freeFormObjectWithoutDescriptionRendersOnlyTheType() {
        JsonNode actual = JsonObjectSchema.freeFormObject(null).toJson();
        assertEquals(1, actual.size());
        assertEquals("object", actual.get("type").asText());
    }

    @Test
    void arraySchemaRendersItemsFromTheGivenType() throws Exception {
        JsonNode expected = JsonUtils.getObjectMapper().readTree("""
                {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "x": {"type": "string", "description": "X"}
                    },
                    "additionalProperties": false
                  }
                }
                """);
        JsonNode actual = array(object().property("x", string().description("X")).build()).toJson();
        assertEquals(expected, actual);
    }

    @Test
    void nestedObjectsNestArbitrarily() throws Exception {
        JsonNode expected = JsonUtils.getObjectMapper().readTree("""
                {
                  "type": "object",
                  "description": "Top",
                  "required": ["data"],
                  "properties": {
                    "data": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "description": "Inner",
                        "required": ["entries"],
                        "properties": {
                          "entries": {
                            "type": "array",
                            "description": "Inner items",
                            "items": {
                              "type": "object",
                              "description": "Leaf holder",
                              "required": ["leaf"],
                              "properties": {
                                "leaf": {"type": "string", "description": "Leaf"}
                              },
                              "additionalProperties": false
                            }
                          }
                        },
                        "additionalProperties": false
                      }
                    }
                  },
                  "additionalProperties": false
                }
                """);
        JsonSchema leafHolder = object()
                .property("leaf", string().description("Leaf"), true)
                .description("Leaf holder")
                .build();
        JsonSchema innerArray = array(leafHolder).description("Inner items");
        JsonSchema innerObject = object()
                .property("entries", innerArray, true)
                .description("Inner")
                .build();
        JsonSchema outerArray = array(innerObject);
        JsonSchema top = object()
                .property("data", outerArray, true)
                .description("Top")
                .build();
        JsonNode actual = top.toJson();
        assertEquals(expected, actual);
    }
}
