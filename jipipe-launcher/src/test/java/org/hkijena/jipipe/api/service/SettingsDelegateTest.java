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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.components.JIPipeApplicationSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.api.settings.SettingsBlockPath;
import org.hkijena.jipipe.api.settings.SettingsMigration;
import org.hkijena.jipipe.plugins.settings.application.JIPipeExtensionApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task 13 of the modern settings system: the legacy settings facade
 * ({@link JIPipeApplicationSettingsServiceComponent} and the recent-projects
 * registry) must be transparently backed by the new
 * {@link org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent}
 * engine, while every existing signature keeps working.
 * <p>
 * Lives in <b>jipipe-launcher</b> because it boots the real service
 * ({@link JIPipe#ensureInstance()}) with every plugin registered, so the
 * sheet bridge covers <em>all</em> sheets of all plugins — the property the
 * production wiring relies on.
 */
class SettingsDelegateTest {

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent.
        JIPipe.ensureInstance();
    }

    @Test
    void registryRoundTripThroughDocument() {
        JIPipe.getSettings().putIntoRegistry("uitest", JIPipePathMetadataStore.key("a", "b"), Boolean.TRUE);
        assertTrue(JIPipe.getSettings().getFromRegistry("uitest", JIPipePathMetadataStore.key("a", "b"),
                Boolean.class, false, false));
        assertTrue(JIPipe.getSettingsService().document(SettingsScope.APPLICATION)
                .has("registry:uitest", "a/b"), "registry value must live in the new document");
    }

    /**
     * The exact {@code destructive} semantics of the legacy component, which
     * are two rules, not one:
     * <ul>
     * <li>an <em>absent</em> location is always seeded with the default (both
     * flags — the legacy store wrote when the key was missing);</li>
     * <li>an <em>existing</em> location whose stored value yields {@code null}
     * (a stored JSON null) is overwritten with the default only when
     * {@code destructive} is {@code true}.</li>
     * </ul>
     * A document path listener reports every write to the location, so a
     * read-side default write is visible here even though the read returns the
     * default either way.
     */
    @Test
    void destructiveFlagControlsDefaultMaterializationForStoredNulls() {
        SettingsDocument doc = JIPipe.getSettingsService().document(SettingsScope.APPLICATION);
        // Start from a clean slate, then observe (a block removal notifies the
        // listeners under it, so the reset happens before they are registered).
        doc.removeBlock("registry:delegate-destructive");
        List<String> writes = new ArrayList<>();
        doc.addPathListener("registry:delegate-destructive/absent", node -> writes.add("absent=" + node));
        doc.addPathListener("registry:delegate-destructive/null-a", node -> writes.add("null-a=" + node));
        doc.addPathListener("registry:delegate-destructive/null-b", node -> writes.add("null-b=" + node));
        doc.addPathListener("registry:delegate-destructive/list-null-a", node -> writes.add("list-null-a=" + node));
        doc.addPathListener("registry:delegate-destructive/list-null-b", node -> writes.add("list-null-b=" + node));

        // Rule 1: an absent location is seeded with the default, both flags.
        assertEquals(Boolean.TRUE, JIPipe.getSettings().getFromRegistry("delegate-destructive",
                JIPipePathMetadataStore.key("absent"), Boolean.class, Boolean.TRUE, false));
        assertTrue(doc.has("registry:delegate-destructive", "absent"),
                "an absent location is seeded with the default even with destructive=false");
        assertEquals(Boolean.TRUE, JIPipe.getSettings().getFromRegistry("delegate-destructive",
                JIPipePathMetadataStore.key("absent"), Boolean.class, Boolean.FALSE, true));
        assertEquals(List.of("absent=true"), writes,
                "a present, convertible value is never rewritten (both flags)");

        // Rule 2: an existing JSON null is kept with destructive=false and
        // replaced with the default with destructive=true.
        doc.set("registry:delegate-destructive", "null-a", JsonUtils.getObjectMapper().nullNode());
        doc.set("registry:delegate-destructive", "null-b", JsonUtils.getObjectMapper().nullNode());
        writes.clear();
        assertEquals(Boolean.TRUE, JIPipe.getSettings().getFromRegistry("delegate-destructive",
                JIPipePathMetadataStore.key("null-a"), Boolean.class, Boolean.TRUE, false));
        assertTrue(writes.isEmpty(), "destructive=false must not overwrite a stored null: " + writes);
        assertEquals(Boolean.TRUE, JIPipe.getSettings().getFromRegistry("delegate-destructive",
                JIPipePathMetadataStore.key("null-b"), Boolean.class, Boolean.TRUE, true));
        assertEquals(List.of("null-b=true"), writes,
                "destructive=true replaces a stored null with the default");
        assertTrue(doc.get("registry:delegate-destructive", "null-b").isBoolean());

        // The list API follows the same two rules, with an empty list default.
        assertTrue(JIPipe.getSettings().getListFromRegistry("delegate-destructive",
                JIPipePathMetadataStore.key("list-null-a"), String.class, true).isEmpty());
        assertTrue(doc.get("registry:delegate-destructive", "list-null-a").isArray(),
                "an absent list location is seeded with the empty list");
        assertTrue(JIPipe.getSettings().getListFromRegistry("delegate-destructive",
                JIPipePathMetadataStore.key("list-null-b"), String.class, false).isEmpty());

        // Minor (Task 13 review): putIntoRegistry(key, null) removes the
        // location, so the next read sees absence and seeds the default again.
        JIPipe.getSettings().putIntoRegistry("delegate-destructive",
                JIPipePathMetadataStore.key("null-b"), null);
        assertFalse(doc.has("registry:delegate-destructive", "null-b"),
                "putIntoRegistry(key, null) removes the location");
        writes.clear();
        assertEquals(Boolean.TRUE, JIPipe.getSettings().getFromRegistry("delegate-destructive",
                JIPipePathMetadataStore.key("null-b"), Boolean.class, Boolean.TRUE, false));
        assertTrue(doc.has("registry:delegate-destructive", "null-b"),
                "an absent location is seeded with the default again");
        assertEquals(List.of("null-b=true"), writes, "exactly one seeding write");

        doc.removeBlock("registry:delegate-destructive");
    }

    @Test
    void registryValuesSurviveSaveAndReload() {
        JIPipe.getSettings().putIntoRegistry("delegate-persist", JIPipePathMetadataStore.key("x", "y"), 42);
        JIPipe.getSettings().save();
        JIPipe.getSettings().reload();
        assertEquals(42, JIPipe.getSettings().getFromRegistry("delegate-persist",
                JIPipePathMetadataStore.key("x", "y"), Integer.class, 0, false));
    }

    @Test
    void recentProjectsUseTheDocument() {
        Path p = Paths.get("/tmp/recent-test.jip");
        JIPipe.getRecentProjects().removeAll(List.of(p));
        JIPipe.getRecentProjects().add(p);
        assertTrue(JIPipe.getSettingsService().document(SettingsScope.APPLICATION)
                .get("state:recent-projects", "0").asText().endsWith("recent-test.jip"),
                "a newly added project is the first entry of the document's state block");
        assertTrue(JIPipe.getRecentProjects().getRecentProjects().contains(p),
                "the legacy List<Path> API must reflect the document");
    }

    @Test
    void recentProjectsKeepOrderingAndEmitterBehavior() {
        JIPipe.getRecentProjects().clear();
        Path a = Paths.get("/tmp/delegate-recent-a.jip");
        Path b = Paths.get("/tmp/delegate-recent-b.jip");
        JIPipe.getRecentProjects().add(a);
        JIPipe.getRecentProjects().add(b);
        assertEquals(List.of(b, a), JIPipe.getRecentProjects().getRecentProjects());

        AtomicInteger changes = new AtomicInteger();
        JIPipe.getRecentProjects().getChangedEventEmitter()
                .subscribeLambda((emitter, event) -> changes.incrementAndGet());
        JIPipe.getRecentProjects().add(a); // already known, moves to the front
        assertEquals(List.of(a, b), JIPipe.getRecentProjects().getRecentProjects());
        assertEquals(1, changes.get(), "a re-ordering add fires exactly one change event");

        JIPipe.getRecentProjects().removeAll(List.of(a));
        assertEquals(List.of(b), JIPipe.getRecentProjects().getRecentProjects());
        assertEquals(2, changes.get(), "removeAll fires exactly one change event");
        JIPipe.getRecentProjects().clear();
    }

    @Test
    void everyRegisteredSheetHasADocumentBlock() {
        SettingsDocument doc = JIPipe.getSettingsService().document(SettingsScope.APPLICATION);
        assertFalse(JIPipe.getSettings().getRegisteredSheets().isEmpty(), "sheets must be registered");
        for (String sheetId : JIPipe.getSettings().getRegisteredSheets().keySet()) {
            assertTrue(doc.hasBlock("sheet:" + sheetId), "missing bridge for sheet " + sheetId);
        }
    }

    @Test
    void registeredSheetInstanceStaysLiveAfterReload() {
        JIPipeGeneralUIApplicationSettings sheet = JIPipe.getSettings()
                .getById(JIPipeGeneralUIApplicationSettings.ID, JIPipeGeneralUIApplicationSettings.class);
        assertNotNull(sheet, "sheet registration and getInstance() must keep working");

        JIPipeSettingKey<Boolean> key = JIPipeSettingKey.bool("sheet:" + JIPipeGeneralUIApplicationSettings.ID,
                "show-project-info", Boolean.class).defaultValue(true);
        boolean original = sheet.isShowProjectInfo();
        try {
            sheet.setShowProjectInfo(!original);
            JIPipe.getSettingsService().save();
            JIPipe.getSettingsService().reload();
            assertEquals(!original, sheet.isShowProjectInfo(),
                    "a registered sheet must read back the value written through the document");
            assertTrue(JIPipe.getSettingsService().document(SettingsScope.APPLICATION).has(key.blockPath(), key.subPath()),
                    "the value must be persisted in the sheet's document block");
        } finally {
            sheet.setShowProjectInfo(original);
            JIPipe.getSettingsService().save();
        }
    }

    /**
     * A sheet registered <em>before</em> the new component's document existed
     * (several sheets are registered very early in startup) must still be
     * bridged: the document's creation seeds it from the sheet and attaches
     * the bridge. Without that, the sheet's values would be missing from the
     * file the first save writes.
     */
    @Test
    void aSheetAttachedBeforeTheDocumentIsStillBridged() {
        JIPipeGeneralUIApplicationSettings sheet = JIPipe.getSettings()
                .getById(JIPipeGeneralUIApplicationSettings.ID, JIPipeGeneralUIApplicationSettings.class);
        assertTrue(JIPipe.getSettingsService().isSheetAttached(sheet),
                "a registered sheet must have a bridge on the live document");
        assertTrue(JIPipe.getSettingsService().document(SettingsScope.APPLICATION)
                        .hasBlock("sheet:" + JIPipeGeneralUIApplicationSettings.ID),
                "the sheet's block must exist in the document");
    }

    /**
     * The legacy aggregate emitter used to fire on every save. It now mirrors
     * the new component's aggregate change event, which fires on every
     * observed settings change (a save that changes nothing emits none, as
     * before the change was value-based).
     */
    @Test
    void legacyChangedEmitterReEmitsTheNewAggregateEvent() {
        AtomicInteger fired = new AtomicInteger();
        JIPipe.getSettings().getChangedEventEmitter()
                .subscribeLambda((emitter, event) -> fired.incrementAndGet());
        assertNotNull(JIPipe.getSettingsService().getChangedEventEmitter());
        assertNotNull(JIPipe.getSettingsService().getLegacyChangedEventEmitter());
        JIPipe.getSettingsService().getLegacyChangedEventEmitter()
                .emit(new org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent.LegacyChangedEvent());
        assertEquals(1, fired.get(), "the legacy emitter must re-emit the new aggregate change event");
    }

    /**
     * The raw reader is file-authoritative: while the profile is still in the
     * legacy (5.x flat) shape it exposes no {@code sheet:} blocks — the values
     * are not visible before the migration, which is the documented pre-startup
     * blindness. After the migration the same file yields them.
     * <p>
     * Finding 1(d) of the Task 13 review. Runs against a temp profile through
     * the migration statics, so it does not depend on the developer's profile.
     */
    @Test
    void rawReaderSeesLegacyValuesOnlyAfterTheMigration(@TempDir Path tempProfile) throws Exception {
        // Build a legacy profile: flat settings.json + settings-db + recent projects.
        Files.writeString(tempProfile.resolve("settings.json"),
                "{\"" + JIPipeExtensionApplicationSettings.ID + "\":{\"silent\":true}}");
        Files.writeString(tempProfile.resolve("settings-db-node-db.json"), "{\"pinned-node-ids\":[\"n1\"]}");
        Files.writeString(tempProfile.resolve("recent-projects.txt"), "/a/b.jip\n");

        // Legacy shape: the migration is needed and the raw file holds
        // unprefixed sheet IDs.
        assertTrue(SettingsMigration.needsMigration(tempProfile));
        JsonNode legacyFile = JsonUtils.getObjectMapper().readTree(
                Files.readString(tempProfile.resolve("settings.json")));
        assertTrue(legacyFile.has(JIPipeExtensionApplicationSettings.ID),
                "the fixture is a legacy flat settings file");

        // Run the startup preparation's migration step (the same public
        // static the initializer calls; its full fail-loud wrapper is covered
        // in jipipe-core's SettingsStartupPreparationTest).
        SettingsMigration.migrateLegacyProfile(tempProfile,
                path -> new SettingsBlockPath(path).kind() != null, Map.of(), JIPipeProgressInfo.SILENT);

        // After: new shape, the extension block moved to its prefixed name,
        // and the migration is done.
        assertFalse(SettingsMigration.needsMigration(tempProfile));
        JsonNode migrated = JsonUtils.getObjectMapper().readTree(
                Files.readString(tempProfile.resolve("settings.json")));
        assertTrue(migrated.has("$schema"), "the migrated file is a new-format document");
        assertTrue(migrated.get("sheet:" + JIPipeExtensionApplicationSettings.ID).get("silent").asBoolean(),
                "the extension sheet is readable after the migration");
        assertFalse(Files.exists(tempProfile.resolve("recent-projects.txt")),
                "the legacy recent-projects file is migrated away");
    }
}