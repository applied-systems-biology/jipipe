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

package org.hkijena.jipipe.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent.ChangeOrigin;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent.SettingsChangedEvent;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins two reload/carry-over contracts of {@link JIPipeSettingsServiceComponent}:
 * <ul>
 * <li><b>Live bindings are re-targeted by {@code reload()}.</b> A reload
 * replaces the scope's document. A binding that still pointed at the old
 * document would write into an orphan (silently lost: no auto-save, no
 * subscribers), so an open binding must follow the new document.</li>
 * <li><b>Carry-over reports its recovered blocks.</b> The recovery writes the
 * blocks into the file and reloads, and a reload emits no per-block events;
 * the recovered blocks must be announced as
 * {@link ChangeOrigin#CARRY_OVER} so the UI can treat them as foreign.</li>
 * </ul>
 */
class SettingsReloadBindingTest {

    @TempDir
    Path tempDir;

    private final List<JIPipeSettingsServiceComponent> components = new ArrayList<>();

    private JIPipeSettingsServiceComponent component() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        components.add(c);
        return c;
    }

    @AfterEach
    void disposeComponents() {
        for (JIPipeSettingsServiceComponent c : components) {
            c.dispose();
        }
    }

    /**
     * {@code dispatchChangeEvent} hands events to the Swing EDT when a display
     * is present (a plain JVM here is not headless). Drain that queue so the
     * assertions see the events on any machine.
     */
    private static void flushEventDispatchQueue() throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return;
        }
        javax.swing.SwingUtilities.invokeAndWait(() -> {
        });
    }

    private JsonNode readSettings() throws Exception {
        Path file = tempDir.resolve("settings.json");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        return JsonUtils.getObjectMapper().readTree(Files.readString(file));
    }

    /**
     * RED pre-fix: after {@code reload()} the binding still referenced the
     * replaced document, so {@code set(v)} wrote into an orphan — the new
     * document never saw the value, no subscriber fired, and nothing was
     * auto-saved.
     */
    @Test
    void openBindingFollowsTheReloadedDocument() throws Exception {
        JIPipeSettingsServiceComponent c = component();
        JIPipeSettingKey<Boolean> key = JIPipeSettingKey.bool("registry:test-db", "flag", Boolean.class)
                .defaultValue(false)
                .scope(SettingsScope.APPLICATION);
        c.registerKey(key);
        SettingsBinding<Boolean> binding = c.bind(key);

        c.reload();
        assertNotSame(binding.get(), true); // sanity: nothing wrote yet

        List<Boolean> seen = new ArrayList<>();
        binding.subscribe(seen::add);

        binding.set(true);

        assertTrue(c.document(SettingsScope.APPLICATION).get("registry:test-db", "flag").asBoolean(),
                "the binding must write into the NEW document after a reload");
        assertEquals(List.of(true), seen, "subscribers must fire for a post-reload write");
        assertTrue(c.saveNowForTesting(), "a post-reload binding write must still schedule the auto-save");
        JsonNode root = readSettings();
        assertNotNull(root, "the post-reload write must reach settings.json");
        assertTrue(root.path("registry:test-db").path("flag").asBoolean(),
                "the post-reload binding write must be persisted");
    }

    /**
     * A closed binding is dead by contract: a reload must not resurrect it
     * (no listener, no writes reaching subscribers).
     */
    @Test
    void closedBindingStaysClosedAcrossAReload() throws Exception {
        JIPipeSettingsServiceComponent c = component();
        JIPipeSettingKey<Boolean> key = JIPipeSettingKey.bool("registry:test-db", "flag", Boolean.class)
                .defaultValue(false)
                .scope(SettingsScope.APPLICATION);
        c.registerKey(key);
        SettingsBinding<Boolean> binding = c.bind(key);
        binding.close();

        c.reload();

        List<Boolean> seen = new ArrayList<>();
        binding.subscribe(seen::add);
        // A write through a different binding on the same path must not reach
        // the closed one's subscribers.
        c.bind(key).set(true);

        assertTrue(seen.isEmpty(), "a closed binding must not be re-attached by a reload");
    }

    /**
     * RED pre-fix: the recovery reloaded silently, so no CARRY_OVER event ever
     * reached a subscriber (the settings banner could not report it).
     */
    @Test
    void materializedDocumentReportsOneCarryOverEventPerRecoveredBlock() throws Exception {
        String version = VersionUtils.getJIPipeVersion();
        // forTesting(Path) treats the given directory AS the profile directory
        // (<...>/profiles/<version>), so the sibling lives next to it.
        Path base = tempDir.resolve("profiles");
        Path profileDir = base.resolve(version);
        Path sibling = base.resolve("5.9.0");
        Files.createDirectories(profileDir);
        Files.createDirectories(sibling);
        // A sibling profile with one recoverable (unregistered) block.
        Files.writeString(sibling.resolve("settings.json"),
                "{\"$schema\": 2, \"state:from-old-version\": {\"a\": {\"b\": \"recovered\"}}}");
        Files.writeString(profileDir.resolve("settings.json"), "{\"$schema\": 2}");

        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(profileDir);
        components.add(c);
        // Materialize first: the recovery then has to reload + announce.
        c.document(SettingsScope.APPLICATION);

        List<SettingsChangedEvent> events = new ArrayList<>();
        c.getChangedEventEmitter().subscribeLambda((emitter, event) -> events.add(event));

        var recovered = c.runCarryOver();
        flushEventDispatchQueue();

        assertTrue(recovered.contains("state:from-old-version"), "the sibling block must be recovered");
        List<SettingsChangedEvent> carryOver = events.stream()
                .filter(e -> e.getOrigin() == ChangeOrigin.CARRY_OVER)
                .collect(Collectors.toList());
        assertEquals(1, carryOver.size(),
                "exactly one CARRY_OVER event per recovered block, got " + events);
        assertEquals("state:from-old-version", carryOver.get(0).getPath());
        assertEquals("recovered",
                carryOver.get(0).getNewValue().path("a").path("b").asText(),
                "the event must carry the recovered block content");
        assertNull(carryOver.get(0).getOldValue(), "a recovered block had no previous value");
    }

    /**
     * Without a materialized document there is nothing to notify (the next
     * session starts from the merged file), so no event is dispatched.
     */
    @Test
    void nonMaterializedDocumentReportsNoCarryOverEvent() throws Exception {
        String version = VersionUtils.getJIPipeVersion();
        // forTesting(Path) treats the given directory AS the profile directory
        // (<...>/profiles/<version>), so the sibling lives next to it.
        Path base = tempDir.resolve("profiles");
        Path profileDir = base.resolve(version);
        Path sibling = base.resolve("5.9.0");
        Files.createDirectories(profileDir);
        Files.createDirectories(sibling);
        Files.writeString(sibling.resolve("settings.json"),
                "{\"$schema\": 2, \"state:from-old-version\": {\"a\": {\"b\": \"recovered\"}}}");
        Files.writeString(profileDir.resolve("settings.json"), "{\"$schema\": 2}");

        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(profileDir);
        components.add(c);

        List<SettingsChangedEvent> events = new ArrayList<>();
        c.getChangedEventEmitter().subscribeLambda((emitter, event) -> events.add(event));

        var recovered = c.runCarryOver();
        flushEventDispatchQueue();

        assertTrue(recovered.contains("state:from-old-version"));
        assertTrue(events.stream().noneMatch(e -> e.getOrigin() == ChangeOrigin.CARRY_OVER),
                "no CARRY_OVER event without a materialized document, got " + events);
    }
}