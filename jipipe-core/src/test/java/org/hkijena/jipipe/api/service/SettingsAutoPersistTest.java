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
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the <b>local-edit auto-persist</b> contract of
 * {@link JIPipeSettingsServiceComponent}: a local write (a UI parameter edit,
 * a feature setting, ...) must schedule the debounced save on its own, so the
 * value reaches the settings file without any explicit
 * {@code save()}/{@code saveLater()} by the caller (in particular without
 * the settings dialog's old Save button). The engine's own machinery — the
 * debounced save, the foreign merge and the quiescent bookkeeping writes —
 * must not re-trigger it in a cycle.
 * <p>
 * Deterministic: the pending debounced save is run synchronously through
 * {@link JIPipeSettingsServiceComponent#saveNowForTesting()}; the test never
 * polls the 250 ms wall clock.
 */
class SettingsAutoPersistTest {

    @TempDir
    Path tempDir;

    /**
     * Every component the test creates; disposed in {@link #disposeComponents()}
     * so a non-daemon Swing debounce timer cannot outlive the test and fire a
     * save on a finished component.
     */
    private final List<JIPipeSettingsServiceComponent> components = new ArrayList<>();

    private JIPipeSettingsServiceComponent component() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        components.add(c);
        return c;
    }

    @AfterEach
    void disposeComponents() {
        // dispose() is idempotent; the test methods keep their own local refs.
        for (JIPipeSettingsServiceComponent c : components) {
            c.dispose();
        }
    }

    private static JsonNode readSettingsFile(Path userDir) throws Exception {
        Path file = userDir.resolve("settings.json");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        return JsonUtils.getObjectMapper().readTree(Files.readString(file));
    }

    private static boolean readFlag(Path userDir, String blockKey, String flagKey) throws Exception {
        JsonNode root = readSettingsFile(userDir);
        if (root == null) {
            return false;
        }
        JsonNode block = root.get(blockKey);
        if (block == null) {
            return false;
        }
        JsonNode flag = block.get(flagKey);
        return flag != null && flag.asBoolean(false);
    }

    /**
     * RED pre-fix: a local {@code binding.set(...)} scheduled no save, so the
     * file never received the value. The key's default is {@code false}, the
     * written value is {@code true}; an absent file reads as {@code false}.
     */
    @Test
    void localEditIsSavedWithoutExplicitSave() throws Exception {
        JIPipeSettingsServiceComponent c = component();
        JIPipeSettingKey<Boolean> key = JIPipeSettingKey.bool("registry:test-db", "flag", Boolean.class)
                .defaultValue(false)
                .scope(SettingsScope.APPLICATION);
        c.registerKey(key);
        SettingsBinding<Boolean> binding = c.bind(key);

        binding.set(true);

        // No explicit save(): the engine must have scheduled the debounced save.
        c.saveNowForTesting();

        assertTrue(readFlag(tempDir, "registry:test-db", "flag"),
                "A local settings edit must reach settings.json without an explicit save()/saveLater()");
    }

    /**
     * The counterpart for a sheet-shaped block (what the settings tab edits):
     * the same origin, the same auto-persist.
     */
    @Test
    void sheetShapedBlockEditIsSavedWithoutExplicitSave() throws Exception {
        JIPipeSettingsServiceComponent c = component();
        JIPipeSettingKey<Boolean> key = JIPipeSettingKey.bool("sheet:test-sheet", "x", Boolean.class)
                .defaultValue(false)
                .scope(SettingsScope.APPLICATION);
        c.registerKey(key);
        SettingsBinding<Boolean> binding = c.bind(key);

        binding.set(true);
        c.saveNowForTesting();

        assertTrue(readFlag(tempDir, "sheet:test-sheet", "x"),
                "A sheet parameter edit must reach settings.json without an explicit save()/saveLater()");
    }

    /**
     * Gate, direction 1: a save the engine runs must not schedule another
     * save in a cycle. {@link JIPipeSettingsServiceComponent#saveNowForTesting()}
     * reports whether a save was pending <em>before</em> it ran; after two
     * consecutive runs nothing may be pending any more.
     */
    @Test
    void engineSaveDoesNotRescheduleItself() {
        JIPipeSettingsServiceComponent c = component();
        JIPipeSettingKey<Boolean> key = JIPipeSettingKey.bool("registry:test-db", "flag", Boolean.class)
                .defaultValue(false)
                .scope(SettingsScope.APPLICATION);
        c.registerKey(key);
        SettingsBinding<Boolean> binding = c.bind(key);

        binding.set(true);

        assertTrue(c.saveNowForTesting(), "the local edit must have scheduled a save");
        assertFalse(c.saveNowForTesting(), "the engine's own save must not schedule another save");
    }

    /**
     * Gate, direction 2: a quiescent bookkeeping write (the recent-projects
     * path, {@code writeQuiescently}) manages its own persistence and must
     * not trigger the local auto-save. This pins the gate at the write level,
     * exercising it exactly as the recent-projects list does.
     */
    @Test
    void quiescentWriteDoesNotScheduleAutoSave() {
        JIPipeSettingsServiceComponent c = component();
        JsonNode value = JsonUtils.getObjectMapper().valueToTree(true);

        c.writeQuiescently(c.document(SettingsScope.APPLICATION), "registry:recent", value);

        assertFalse(c.saveNowForTesting(), "a quiescent bookkeeping write must not trigger the local auto-save");
    }
}
