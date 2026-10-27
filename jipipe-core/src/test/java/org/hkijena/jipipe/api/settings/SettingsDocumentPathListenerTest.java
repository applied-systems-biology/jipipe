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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsDocumentPathListenerTest {
    private static final String BLOCK = "registry:ui-graph-editor";
    private static final String PATH = BLOCK + "/any/dock/show-labels";

    @Test void setFiresListenerWithNewValue() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        List<JsonNode> seen = new ArrayList<>();
        d.addPathListener(PATH, seen::add);
        d.set(BLOCK, "any/dock/show-labels", BooleanNode.TRUE);
        assertEquals(List.of(BooleanNode.TRUE), seen);
    }

    @Test void removeFiresListenerWithMissingNode() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set(BLOCK, "any/dock/show-labels", BooleanNode.TRUE);
        List<JsonNode> seen = new ArrayList<>();
        d.addPathListener(PATH, seen::add);
        d.remove(BLOCK, "any/dock/show-labels");
        assertEquals(1, seen.size());
        assertTrue(seen.get(0).isMissingNode());
    }

    @Test void settingMissingValueNotifiesWithMissingNode() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set(BLOCK, "any/dock/show-labels", BooleanNode.TRUE);
        List<JsonNode> seen = new ArrayList<>();
        d.addPathListener(PATH, seen::add);
        d.set(BLOCK, "any/dock/show-labels", null);
        assertEquals(1, seen.size());
        assertTrue(seen.get(0).isMissingNode());
    }

    @Test void setBlockNotifiesListenersUnderTheBlock() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set(BLOCK, "any/dock/show-labels", BooleanNode.TRUE);
        List<JsonNode> leafSeen = new ArrayList<>();
        List<JsonNode> blockSeen = new ArrayList<>();
        d.addPathListener(PATH, leafSeen::add);
        d.addPathListener(BLOCK, blockSeen::add);
        d.setBlock(BLOCK, JsonUtils.toJsonNode(Map.of("any", Map.of("dock", Map.of("show-labels", false)))));
        assertEquals(List.of(BooleanNode.FALSE), leafSeen);
        assertEquals(1, blockSeen.size());
        assertTrue(blockSeen.get(0).isObject());
        assertFalse(blockSeen.get(0).get("any").get("dock").get("show-labels").asBoolean());
    }

    @Test void setBlockRemovalNotifiesWithMissingNode() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set(BLOCK, "any/dock/show-labels", BooleanNode.TRUE);
        List<JsonNode> leafSeen = new ArrayList<>();
        d.addPathListener(PATH, leafSeen::add);
        d.setBlock(BLOCK, null);
        assertEquals(1, leafSeen.size());
        assertTrue(leafSeen.get(0).isMissingNode());
    }

    @Test void removePathListenerStopsNotifications() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        List<JsonNode> seen = new ArrayList<>();
        Consumer<JsonNode> listener = seen::add;
        d.addPathListener(PATH, listener);
        d.removePathListener(PATH, listener);
        d.set(BLOCK, "any/dock/show-labels", BooleanNode.TRUE);
        assertTrue(seen.isEmpty());
    }
}
