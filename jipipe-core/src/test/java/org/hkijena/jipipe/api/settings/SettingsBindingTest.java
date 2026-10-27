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
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
}
