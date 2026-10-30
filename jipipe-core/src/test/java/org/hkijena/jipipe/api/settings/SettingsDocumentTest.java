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

package org.hkijena.jipipe.api.settings;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsDocumentTest {
    @Test void absentSubPathIsMissingNotDefault() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        assertTrue(d.get("registry:ui-graph-editor", "any/dock/show-labels").isMissingNode());
    }

    @Test void subPathRoundTrip() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set("registry:node-db", "pinned-node-ids", JsonUtils.toJsonNode(List.of("a", "b")));
        assertEquals(2, d.get("registry:node-db", "pinned-node-ids").size());
    }

    @Test void nestedSubPathCreatesIntermediateObjects() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set("registry:ui-graph-editor", "any/dock/show-labels", BooleanNode.TRUE);
        assertTrue(d.get("registry:ui-graph-editor", "any/dock").isObject());
        assertTrue(d.get("registry:ui-graph-editor", "any/dock/show-labels").asBoolean());
    }

    @Test void unregisteredBlockCanBeMarkedPassThrough() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.setBlock("future-thing:abc", JsonUtils.toJsonNode(Map.of("x", 1)));
        assertTrue(d.isPassThrough("future-thing:abc"));
        assertFalse(d.isPassThrough("registry:ui-graph-editor"));
    }

    @Test void scopeIsStored() {
        assertEquals(SettingsScope.PROJECT, new SettingsDocument(SettingsScope.PROJECT).scope());
    }

    @Test void emptyOrNullSubPathAddressesWholeBlock() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.setBlock("registry:ui-graph-editor", JsonUtils.toJsonNode(Map.of("a", 1)));
        assertEquals(d.getBlock("registry:ui-graph-editor"), d.get("registry:ui-graph-editor", ""));
        assertEquals(d.getBlock("registry:ui-graph-editor"), d.get("registry:ui-graph-editor", null));
        d.set("registry:ui-graph-editor", "", JsonUtils.toJsonNode(Map.of("b", 2)));
        assertEquals(2, d.getBlock("registry:ui-graph-editor").get("b").asInt());
        d.remove("registry:ui-graph-editor", "");
        assertFalse(d.hasBlock("registry:ui-graph-editor"));
    }

    @Test void doubleSlashesNormalizeLikePathMetadataStore() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set("registry:ui-graph-editor", "any//dock/show-labels", BooleanNode.TRUE);
        assertTrue(d.get("registry:ui-graph-editor", "any/dock/show-labels").asBoolean());
    }

    @Test void setOverwritesNonObjectIntermediate() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set("registry:node-db", "a", BooleanNode.TRUE);
        d.set("registry:node-db", "a/b", BooleanNode.FALSE);
        assertTrue(d.get("registry:node-db", "a").isObject());
        assertFalse(d.get("registry:node-db", "a/b").asBoolean());
    }

    @Test void blockRoundTripAndMembership() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        assertFalse(d.hasBlock("registry:node-db"));
        assertTrue(d.getBlock("registry:node-db").isMissingNode());
        d.setBlock("registry:node-db", JsonUtils.toJsonNode(Map.of("pinned-node-ids", List.of("a"))));
        assertTrue(d.hasBlock("registry:node-db"));
        Set<String> paths = d.blockPaths();
        assertTrue(paths.contains("registry:node-db"));
        assertFalse(paths.contains("$schema"));
        assertFalse(paths.contains("$meta"));
    }

    @Test void arrayElementsAreAddressableByIndex() {
        // A list-valued block (e.g. state:recent-projects, a JSON array of path
        // strings) must be readable element by element: JsonNode#get(String)
        // only addresses object fields, so the document resolves a numeric
        // segment against an array as its index.
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        ArrayNode list = JsonUtils.getObjectMapper().createArrayNode();
        list.add("/tmp/a.jip");
        list.add("/tmp/b.jip");
        d.setBlock("state:recent-projects", list);

        assertEquals("/tmp/a.jip", d.get("state:recent-projects", "0").asText());
        assertEquals("/tmp/b.jip", d.get("state:recent-projects", "1").asText());
        assertTrue(d.has("state:recent-projects", "1"));
        assertTrue(d.get("state:recent-projects", "2").isMissingNode(), "past the end is absent");
        assertTrue(d.get("state:recent-projects", "first").isMissingNode(),
                "a non-numeric segment is not an array index");
        assertTrue(d.get("absent:thing", "0").isMissingNode());
    }

    @Test void subPathsListsAllNestedPaths() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set("registry:ui-graph-editor", "any/dock/show-labels", BooleanNode.TRUE);
        Set<String> subPaths = d.subPaths("registry:ui-graph-editor");
        assertTrue(subPaths.contains("any"));
        assertTrue(subPaths.contains("any/dock"));
        assertTrue(subPaths.contains("any/dock/show-labels"));
        assertTrue(d.has("registry:ui-graph-editor", "any/dock"));
        assertFalse(d.has("registry:ui-graph-editor", "missing/path"));
        assertTrue(d.subPaths("absent:thing").isEmpty());
    }

    @Test void removeAndRemoveBlock() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set("registry:ui-graph-editor", "any/dock/show-labels", BooleanNode.TRUE);
        d.remove("registry:ui-graph-editor", "any/dock/show-labels");
        assertFalse(d.has("registry:ui-graph-editor", "any/dock/show-labels"));
        d.remove("registry:ui-graph-editor", "any/other");
        assertTrue(d.getBlock("registry:ui-graph-editor").isObject());
        d.removeBlock("registry:ui-graph-editor");
        assertFalse(d.hasBlock("registry:ui-graph-editor"));
        assertTrue(d.getBlock("registry:ui-graph-editor").isMissingNode());
    }

    @Test void schemaVersionDefaultsToTwo() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        assertEquals(2, d.schemaVersion());
        d.setSchemaVersion(3);
        assertEquals(3, d.schemaVersion());
        assertEquals(3, d.root().get("$schema").asInt());
    }

    @Test void metaIsLiveReservedObjectNode() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        assertSame(d.meta(), d.root().get("$meta"));
        d.meta().put("written-by-version", "1345-test");
        assertEquals("1345-test", d.root().get("$meta").get("written-by-version").asText());
    }

    @Test void writerIdIsFixedHexPerInstance() {
        SettingsDocument a = new SettingsDocument(SettingsScope.RUNTIME);
        SettingsDocument b = new SettingsDocument(SettingsScope.RUNTIME);
        String id = a.writerId();
        assertEquals(id, a.writerId());
        assertNotEquals(id, b.writerId());
        assertTrue(id.matches("[0-9a-f]{32}"));
    }

    @Test void registeredBlockPredicateReceivesCanonicalPath() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION, p -> p.equals("registry:known"));
        assertFalse(d.isPassThrough("registry:known"));
        assertTrue(d.isPassThrough("registry:unknown-id"));
        assertTrue(d.isPassThrough("future-thing:abc"));
    }
}
