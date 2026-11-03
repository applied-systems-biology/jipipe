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
import com.fasterxml.jackson.databind.node.NullNode;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsBindingTest {
    private JIPipeSettingKey<Boolean> key() {
        return JIPipeSettingKey.bool("registry:ui-graph-editor", "any/dock/show-labels", Boolean.class)
                .defaultValue(true).scope(SettingsScope.APPLICATION).access(SettingsAccessPolicy.USER_EDITABLE);
    }

    @Test void absentValueReadsAsDefault() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> b = new SettingsBinding<>(d, key());
        assertTrue(b.get());
        assertFalse(b.isModified());
    }

    @Test void setWritesThroughAndMarksModified() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> b = new SettingsBinding<>(d, key());
        b.set(false);
        assertFalse(b.get());
        assertTrue(b.isModified());
        assertTrue(d.has("registry:ui-graph-editor", "any/dock/show-labels"));
    }

    @Test void resetRemovesValueAndRestoresDefault() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> b = new SettingsBinding<>(d, key());
        b.set(false);
        b.reset();
        assertTrue(b.get());
        assertFalse(b.isModified());
    }

    @Test void subscribersFireOnForeignChange() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> a = new SettingsBinding<>(d, key());
        SettingsBinding<Boolean> b = new SettingsBinding<>(d, key());
        List<Boolean> seen = new ArrayList<>();
        b.subscribe(seen::add);
        a.set(false);
        assertEquals(List.of(false), seen);
    }

    @Test void noEventWhenValueUnchanged() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> b = new SettingsBinding<>(d, key());
        List<Boolean> seen = new ArrayList<>();
        b.subscribe(seen::add);
        b.set(true);   // already default true -> no modification
        assertTrue(d.get("registry:ui-graph-editor", "any/dock/show-labels").isMissingNode(),
                "setting the default value must not materialize the key");
        assertTrue(seen.isEmpty(), "no event for an unchanged value");
    }

    @Test void readerAndWriterOfSameKeyStayInSync() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> writer = new SettingsBinding<>(d, key());
        SettingsBinding<Boolean> reader = new SettingsBinding<>(d, key());
        assertTrue(reader.get());
        assertFalse(reader.isModified());
        writer.set(false);
        assertFalse(reader.get());
        assertTrue(reader.isModified());
        List<Boolean> seen = new ArrayList<>();
        reader.subscribe(seen::add);
        writer.set(true);   // back to the default
        assertEquals(List.of(true), seen);
        assertTrue(reader.get());
        assertFalse(reader.isModified());
    }

    @Test void malformedStoredValueFallsBackToDefault() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<String> binding = new SettingsBinding<>(d,
                JIPipeSettingKey.string("registry:ui-graph-editor", "any/dock/title", String.class)
                        .defaultValue("Fallback"));
        d.set("registry:ui-graph-editor", "any/dock/title", JsonUtils.toJsonNode(List.of(1, 2, 3)));
        assertEquals("Fallback", binding.get());
        assertTrue(binding.isModified(), "a malformed value is present, so the key counts as modified");
    }

    @Test void jsonNodeKeyRoundTripsUnchanged() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        JIPipeSettingKey<JsonNode> nodeKey = JIPipeSettingKey.object("registry:ui-graph-editor", "any/dock/layout", JsonNode.class)
                .defaultValue(JsonUtils.toJsonNode(Map.of("columns", 3)));
        SettingsBinding<JsonNode> binding = new SettingsBinding<>(d, nodeKey);
        assertEquals(nodeKey.defaultValue(), binding.get());
        binding.set(JsonUtils.toJsonNode(Map.of("columns", 7)));
        assertEquals(JsonUtils.toJsonNode(Map.of("columns", 7)), binding.get());
        assertTrue(binding.isModified());
    }

    @Test void closedBindingDoesNotReceiveEvents() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> b = new SettingsBinding<>(d, key());
        List<Boolean> seen = new ArrayList<>();
        b.subscribe(seen::add);
        b.close();
        assertDoesNotThrow(b::close, "close must be idempotent");
        // A closed binding may still read and write through to the document...
        assertTrue(b.get());
        assertFalse(b.isModified());
        b.set(false);
        assertFalse(d.get("registry:ui-graph-editor", "any/dock/show-labels").isMissingNode());
        // ...and late subscriptions stay detached: writes must not notify them
        b.subscribe(seen::add);
        d.set("registry:ui-graph-editor", "any/dock/show-labels", BooleanNode.TRUE);
        assertTrue(seen.isEmpty(), "a closed binding must not receive events");
    }

    @Test void subscriberThrowingDoesNotBreakOtherSubscribersOrWriter() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> b = new SettingsBinding<>(d, key());
        List<Boolean> healthy = new ArrayList<>();
        b.subscribe(v -> {
            throw new IllegalStateException("boom");
        });
        b.subscribe(healthy::add);
        assertDoesNotThrow(() -> b.set(false), "a throwing subscriber must not propagate out of set()");
        assertEquals(List.of(false), healthy, "other subscribers must still be notified");
    }

    @Test void storedNullFallsBackToDefault() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<String> binding = new SettingsBinding<>(d,
                JIPipeSettingKey.string("registry:ui-graph-editor", "any/dock/title", String.class)
                        .defaultValue("Fallback"));
        d.set("registry:ui-graph-editor", "any/dock/title", NullNode.getInstance());
        assertEquals("Fallback", binding.get());
        assertTrue(binding.isModified());
    }

    @Test void doubleSlashSubPathStillReceivesNotifications() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<String> b = new SettingsBinding<>(d,
                JIPipeSettingKey.string("registry:ui-graph-editor", "any//dock//title", String.class)
                        .defaultValue("Fallback"));
        List<String> seen = new ArrayList<>();
        b.subscribe(seen::add);
        b.set("stored");
        assertEquals(List.of("stored"), seen);
        assertTrue(d.has("registry:ui-graph-editor", "any/dock/title"), "empty segments must be dropped");
    }
}
