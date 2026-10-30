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

package org.hkijena.jipipe.api.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.settings.project.JIPipeDataStorageProjectSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end contract for the project-scope settings document as used by
 * {@link JIPipeProject}: a <b>legacy</b> project file (unprefixed settings
 * keys, no {@code $schema}) that carries an <b>unknown</b> settings sheet must
 * load, expose the registered sheet's values on the live project, and save
 * back in the new shape ({@code $schema}/{@code $meta}, {@code "sheet:<id>"}
 * blocks) with the unknown sheet's content preserved <em>value-identically</em>.
 * <p>
 * <b>Scope: jipipe-launcher</b> — the registered sheet used here (data
 * storage) and the pass-through case require the full plugin registry.
 */
class JIPipeProjectSettingsPersistenceTest {

    private static final String FIXTURE = "/projects/5.3.0-taxonomy-fixtures/basic-images.jipipe";
    private static final String DATA_STORAGE_SHEET = "org.hkijena.jipipe:data-storage";
    private static final String UNKNOWN_SHEET = "org.example:future-plugin-sheet";

    @TempDir
    Path tmp;

    @BeforeAll
    static void init() {
        JIPipe.ensureInstance();
    }

    @Test
    void legacyProjectFileIsUpgradedAndUnknownSheetPassesThrough() throws IOException, URISyntaxException {
        // Build a legacy-shaped project: the fixture plus an unprefixed
        // "settings" object holding one registered and one unknown sheet.
        Path fixture = Path.of(getClass().getResource(FIXTURE).toURI());
        ObjectNode legacy = (ObjectNode) JsonUtils.getObjectMapper().readTree(fixture.toFile());
        ObjectNode settings = legacy.putObject("settings");
        settings.putObject(DATA_STORAGE_SHEET).put("force-global-temp-directory", true);
        ObjectNode unknown = settings.putObject(UNKNOWN_SHEET);
        unknown.put("knob", 42);
        unknown.putObject("nested").put("flag", true);
        Path legacyFile = tmp.resolve("legacy.jip");
        JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(legacyFile.toFile(), legacy);

        // Load: the unknown sheet must be readable as a pass-through block.
        JIPipeProject project = load(legacyFile);
        JIPipeDataStorageProjectSettings dataStorage =
                project.getSettingsSheet(DATA_STORAGE_SHEET, JIPipeDataStorageProjectSettings.class);
        assertNotNull(dataStorage, "The registered data-storage sheet must be known to the project");
        assertTrue(dataStorage.isForceGlobalTempDirectory(),
                "The legacy unprefixed value must be applied to the live registered sheet at load");
        JsonNode unloaded = project.getUnloadedSettingsSheet(UNKNOWN_SHEET);
        assertNotNull(unloaded, "The unknown sheet must be preserved as a pass-through block");
        assertEquals(42, unloaded.get("knob").asInt());

        // A live change on the sheet must reach the written block (the write
        // direction is sheet → document, the project owns the sheet instances).
        dataStorage.setForceGlobalTempDirectory(false);

        // Save: the member keeps its position but switches to the new shape.
        Path saved = tmp.resolve("saved.jip");
        project.saveProject(saved, false);
        JsonNode savedSettings = JsonUtils.getObjectMapper().readTree(saved.toFile()).get("settings");
        assertEquals(2, savedSettings.get("$schema").asInt(), "Saved settings must carry $schema 2");
        assertTrue(savedSettings.has("$meta"), "Saved settings must carry $meta");
        assertFalse(savedSettings.has(DATA_STORAGE_SHEET), "Legacy unprefixed keys must not be written anymore");

        // The unknown sheet's content is value-identical, now prefixed.
        JsonNode savedUnknown = savedSettings.get("sheet:" + UNKNOWN_SHEET);
        assertEquals(unknown, savedUnknown, "The unknown sheet must round-trip value-identically");

        // The public sheet API is unchanged: the registered sheet is live and
        // re-serialized from the live instance into its prefixed block.
        JsonNode savedDataStorage = savedSettings.get("sheet:" + DATA_STORAGE_SHEET);
        assertNotNull(savedDataStorage, "The registered sheet must be written under its prefixed block path");
        assertTrue(savedDataStorage.isObject(), "The sheet block must be a JSON object");
        assertFalse(savedDataStorage.path("force-global-temp-directory").asBoolean(true),
                "The written block must reflect the live sheet value, not the loaded one");
        JsonNode expectedDataStorage = JsonUtils.getObjectMapper().readTree(
                dataStorage.serializeToJsonString());
        assertEquals(expectedDataStorage, savedDataStorage,
                "The registered block must be the live sheet's serialized state");

        // Reload: the new shape is read as-is and stays length-stable.
        JIPipeProject reloaded = load(saved);
        assertEquals(unknown, reloaded.getUnloadedSettingsSheet(UNKNOWN_SHEET),
                "Pass-through content must survive a second load unchanged");
        Path saved2 = tmp.resolve("saved2.jip");
        reloaded.saveProject(saved2, false);
        JsonNode savedSettings2 = JsonUtils.getObjectMapper().readTree(saved2.toFile()).get("settings");
        assertEquals(canonical(savedSettings), canonical(savedSettings2),
                "Loading and saving the new shape must be a fixpoint (except $meta)");
    }

    @Test
    void projectWithoutSettingsWritesAnEmptyObject() throws IOException, URISyntaxException {
        Path fixture = Path.of(getClass().getResource(FIXTURE).toURI());
        JIPipeProject project = load(fixture);
        assertNull(project.getUnloadedSettingsSheet(UNKNOWN_SHEET));

        Path saved = tmp.resolve("empty.jip");
        project.saveProject(saved, false);
        JsonNode savedSettings = JsonUtils.getObjectMapper().readTree(saved.toFile()).get("settings");
        assertNotNull(savedSettings, "The \"settings\" member keeps its position");
        assertEquals(0, savedSettings.size(),
                "A project file without a settings member must keep writing an empty member (no invented $schema document)");
    }

    private static JIPipeProject load(Path path) throws IOException {
        return JIPipeProject.loadProject(path, new UnspecifiedValidationReportContext(),
                new JIPipeValidationReport(), new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
    }

    /**
     * Compares two settings trees ignoring the {@code $meta} timestamps.
     */
    private static JsonNode canonical(JsonNode settings) {
        ObjectNode copy = settings.deepCopy();
        copy.remove("$meta");
        return copy;
    }
}