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

package org.hkijena.jipipe.plugins.settings;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsAccessDeniedException;
import org.hkijena.jipipe.api.settings.SettingsAccessPolicy;
import org.hkijena.jipipe.api.settings.SettingsMigration;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the scope assignment of the existing settings blocks (spec §3.1: runtime
 * = execution state, application = UI and user preference) and the access-policy
 * enforcement seam (the single write choke point the #1102 runtime host will
 * hook into).
 * <p>
 * Pure/headless: a testing component and a legacy profile in a temp directory;
 * never calls {@code JIPipe.ensureInstance()}.
 */
class SettingsScopesTest {
    @TempDir Path tempDir;

    @Test
    void runtimeExecutionBlocksAreRuntimeScoped() {
        Map<String, SettingsScope> m = SettingsScopes.scopeByBlock();
        assertEquals(SettingsScope.RUNTIME, m.get("sheet:org.hkijena.jipipe:runtime"));
        assertEquals(SettingsScope.RUNTIME, m.get("sheet:org.hkijena.jipipe:hardware-acceleration"));
    }

    @Test
    void uiBlocksAreNotRuntimeScoped() {
        assertFalse(SettingsScopes.scopeByBlock().containsKey("sheet:org.hkijena.jipipe:general-ui"));
    }

    @Test
    void writeGuardBlocksRejectedKey() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        JIPipeSettingKey<Boolean> k = JIPipeSettingKey.bool("registry:x", "a", Boolean.class)
                .defaultValue(true).access(SettingsAccessPolicy.RUNTIME_ADMIN);
        c.registerKey(k);
        c.setWriteGuard(key -> false);   // simulate a remote runtime that denies writes
        assertThrows(SettingsAccessDeniedException.class, () -> c.bind(k).set(false));
        assertTrue(c.bind(k).get(), "a denied write must not change the value");
    }

    @Test
    void allKeysAreWritableWhenNoGuardIsInstalled() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        JIPipeSettingKey<Boolean> k = JIPipeSettingKey.bool("registry:x", "a", Boolean.class)
                .defaultValue(true).access(SettingsAccessPolicy.READ_ONLY);
        c.registerKey(k);
        c.bind(k).set(false);   // local runtime: READ_ONLY is not enforced yet
        assertFalse(c.bind(k).get());
    }

    /**
     * The point of the scope map: a migrated 5.x profile routes a runtime block
     * into {@code runtime-settings.json} and leaves a UI block in
     * {@code settings.json}. Runs the real migration with the map the startup
     * uses, so a regression to an empty map (everything to APPLICATION) fails.
     */
    @Test
    void migrationRoutesRuntimeBlocksViaScopeMap() throws Exception {
        Path profile = tempDir.resolve("6.0.0");
        Files.createDirectories(profile);
        Files.writeString(profile.resolve("settings.json"),
                "{\"org.hkijena.jipipe:runtime\":{\"num-threads\":4},"
                        + "\"org.hkijena.jipipe:general-ui\":{\"theme\":\"Dark\"}}");

        // Exactly what the initializer passes: the live component's merged view.
        JIPipeSettingsServiceComponent component = JIPipeSettingsServiceComponent.forTesting(profile);
        SettingsMigration.migrateLegacyProfile(profile, block -> true,
                component.effectiveScopeByBlock(), JIPipeProgressInfo.SILENT);

        JsonNode application = JsonUtils.getObjectMapper().readTree(
                Files.readString(profile.resolve("settings.json")));
        JsonNode runtime = JsonUtils.getObjectMapper().readTree(
                Files.readString(profile.resolve("runtime-settings.json")));
        assertFalse(application.has("sheet:org.hkijena.jipipe:runtime"),
                "the runtime block must leave settings.json");
        assertEquals(4, runtime.get("sheet:org.hkijena.jipipe:runtime").get("num-threads").asInt());
        assertEquals("Dark", application.get("sheet:org.hkijena.jipipe:general-ui").get("theme").asText());
        assertFalse(runtime.has("sheet:org.hkijena.jipipe:general-ui"),
                "a UI block must not be routed to the runtime document");
    }

    /**
     * Registered keys are the primary source of truth: a {@code scope(RUNTIME)}
     * key's block is in the component's merged view even when the static map does
     * not list it (the block is keyified but not yet in {@link SettingsScopes}).
     */
    @Test
    void runtimeScopedKeyContributesItsBlockToTheMergedView() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        JIPipeSettingKey<Boolean> k = JIPipeSettingKey.bool("registry:some-future-runtime-db", "a", Boolean.class)
                .defaultValue(true).scope(SettingsScope.RUNTIME);
        c.registerKey(k);
        assertEquals(SettingsScope.RUNTIME, c.effectiveScopeByBlock().get("registry:some-future-runtime-db"));
        assertFalse(c.effectiveScopeByBlock().containsKey("registry:some-application-db"));
    }
}