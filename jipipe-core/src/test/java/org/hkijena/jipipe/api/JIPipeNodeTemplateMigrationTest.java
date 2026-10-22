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

package org.hkijena.jipipe.api;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseBuilderRun;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodesByTemplateDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the one-shot migration of the legacy {@code menu-path} key to the
 * {@code taxonomy-function} reference (spec §9): the pure first-segment
 * name→id matcher, the transient legacy capture on the model (order-safety,
 * idempotency, the auditable "Previously" footer, and the drop-on-save of the
 * legacy key). Boots the JIPipe instance so the {@code FunctionRegistry} is
 * populated for the name match.
 */
class JIPipeNodeTemplateMigrationTest {

    /**
     * Throwaway base for the JIPipe user dir. This test's database-builder probe
     * ({@code downloadedGlobalTemplateGetsTheDownloadedScopeInTheDatabase}) adds
     * two templates to the global store, which writes real files via
     * {@code addToGlobal}. It cleans up with {@code removeFromGlobal} in a
     * finally, but in the shared full-suite JVM the relation-index mining
     * re-serializes the template's {@code data} between add and cleanup,
     * invalidating the value-keyed file-map key, so the on-disk file can be
     * orphaned (the "deleted" template resurrects on the next boot). Redirect
     * the user dir to a temp base so this test NEVER writes to the real user
     * store; {@link #cleanup()} restores the override and deletes the temp dir.
     */
    private static Path tempUserDirBase;

    @BeforeAll
    static void ensureJIPipe() throws Exception {
        tempUserDirBase = Files.createTempDirectory("jipipe-tmpl-migration-test");
        JIPipe.ensureInstance();
        JIPipe.getInstance().getInitializationSettings().setOverrideUserDirBase(tempUserDirBase);
        // Re-point the in-memory store + file map at the (empty) temp dir so a
        // real-dir template loaded by an earlier boot cannot linger in memory.
        JIPipe.getNodeTemplates().reloadGlobalTemplates(JIPipeProgressInfo.SILENT);
    }

    @AfterAll
    static void cleanup() {
        if (JIPipe.getInstance() != null) {
            JIPipe.getInstance().getInitializationSettings().setOverrideUserDirBase(null);
        }
        deleteRecursivelyQuietly(tempUserDirBase);
    }

    /** Best-effort recursive delete of the temp user-dir base. */
    private static void deleteRecursivelyQuietly(Path root) {
        if (root == null) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best-effort temp cleanup
                }
            });
        } catch (IOException ignored) {
            // temp dir may already be gone
        }
    }

    // --- the pure name→id matcher ---
    @Test
    void imageSegmentMapsToTopLevelBranchNotExportLeaf() {
        // "Image" (→ "Images") must map to the top-level branch, NOT the
        // childless export sub-leaf jipipe:function:export-images that
        // shadows it with the same display name.
        assertThat(JIPipeNodeTemplateMigration.migrate("Image").getId()).isEqualTo("jipipe:function:images");
    }

    @Test
    void tableSegmentMapsToTopLevelBranchNotExportLeaf() {
        // "Table" (→ "Tables") must map to the top-level branch, NOT
        // jipipe:function:export-tables.
        assertThat(JIPipeNodeTemplateMigration.migrate("Table").getId()).isEqualTo("jipipe:function:tables");
    }

    @Test
    void leafOnlyNameFallsThroughToLeaf() {
        // "Thresholding" has no same-named top-level branch → the child pass
        // must still resolve it to the leaf under Images.
        assertThat(JIPipeNodeTemplateMigration.migrate("Thresholding").getId()).isEqualTo("jipipe:function:thresholding");
    }

    @Test
    void unknownSegmentYieldsUnset() {
        JIPipeTaxonomyFunctionRef ref = JIPipeNodeTemplateMigration.migrate("TotallyUnknownXyz");
        assertThat(ref.isResolved()).isFalse();
        assertThat(ref.getId()).isNull();
    }

    @Test
    void emptySegmentYieldsUnset() {
        assertThat(JIPipeNodeTemplateMigration.migrate("").isResolved()).isFalse();
    }

    // --- the model migration (transient capture + order-safe + footer + drop-on-save) ---
    @Test
    void legacyTemplateMigratesFunctionAndFooters() throws Exception {
        String json = "{\"name\":\"legacy\",\"menu-path\":[\"Image\",\"Threshold\"]}";
        JIPipeNodeTemplate t = JsonUtils.readFromString(json, JIPipeNodeTemplate.class);
        t.migrateLegacyMenuPath();
        assertThat(t.getTaxonomyFunction().getId()).isEqualTo("jipipe:function:images");   // "Image"→top-level branch
        assertThat(t.getDescription().getHtml()).contains("Previously");  // auditable footer
    }

    @Test
    void existingFunctionWinsOverMenuPath() throws Exception {  // order-safety
        String json = "{\"name\":\"x\",\"menu-path\":[\"Image\"],\"taxonomy-function\":\"jipipe:function:tables\"}";
        JIPipeNodeTemplate t = JsonUtils.readFromString(json, JIPipeNodeTemplate.class);
        t.migrateLegacyMenuPath();
        assertThat(t.getTaxonomyFunction().getId()).isEqualTo("jipipe:function:tables");  // preserved
        assertThat(t.getDescription().getHtml()).doesNotContain("Previously");            // no footer
    }

    @Test
    void migrationIsIdempotent() throws Exception {
        JIPipeNodeTemplate t = JsonUtils.readFromString(
            "{\"name\":\"x\",\"menu-path\":[\"Image\"]}", JIPipeNodeTemplate.class);
        t.migrateLegacyMenuPath();
        String afterFirst = t.getDescription().getHtml();
        t.migrateLegacyMenuPath();
        assertThat(t.getDescription().getHtml()).isEqualTo(afterFirst);  // no double footer
    }

    @Test
    void saveDropsLegacyKey() throws Exception {
        JIPipeNodeTemplate t = JsonUtils.readFromString(
            "{\"name\":\"x\",\"menu-path\":[\"Image\",\"Threshold\"]}", JIPipeNodeTemplate.class);
        t.migrateLegacyMenuPath();
        assertThat(JsonUtils.toPrettyJsonString(t)).doesNotContain("menu-path");
    }

    // --- R7 call site (b): the database builder migrates project-scope templates ---
    @Test
    void projectTemplateIsMigratedByTheDatabaseBuilder() throws Exception {
        // A legacy (pre-6.0.0) project-stored template: deserialized from a JSON
        // string that still carries the removed "menu-path" key, so the transient
        // capture is populated and the taxonomy function is unset.
        String json = "{\"name\":\"legacy-project-template\",\"menu-path\":[\"Image\",\"Threshold\"]}";
        JIPipeNodeTemplate t = JsonUtils.readFromString(json, JIPipeNodeTemplate.class);
        t.setGraph(new JIPipeGraph()); // minimal valid graph so the entry can construct
        assertThat(t.getMenuPathLegacy()).hasSize(2);          // capture populated (legacy)
        assertThat(t.getTaxonomyFunction().getId()).isNull();  // function unset (legacy)

        JIPipeProject project = new JIPipeProject();
        assertThat(JIPipe.getNodeTemplates().addToProject(t, project)).isTrue();

        // Trigger the database build. rebuildImmediately() enqueues the builder
        // run ASYNCHRONOUSLY (SwingWorker), so — like NodeDatabaseTaxonomyTest —
        // we use a database whose virtual rebuildImmediately() is a no-op and
        // run the builder synchronously to assert its effect deterministically.
        // The database must hold the SAME project, because the builder's
        // getAllTemplates(database.getProject()) is what surfaces the
        // project-scope template for migration.
        JIPipeNodeDatabaseBuilderRun run = new JIPipeNodeDatabaseBuilderRun(new NoRebuildDatabase(project));
        run.run();

        // The builder loop must have called migrateLegacyMenuPath() on the
        // project-scope instance: the function resolved to a real function, the
        // auditable footer was appended, and the legacy capture was cleared (so
        // a subsequent project save drops the "menu-path" key for free).
        assertThat(t.getTaxonomyFunction().isResolved()).isTrue();
        assertThat(t.getTaxonomyFunction().getId()).isEqualTo("jipipe:function:images");
        assertThat(t.getDescription().getHtml()).contains("Previously");
        assertThat(t.getMenuPathLegacy()).isEmpty();

        // Clean up: do not leak the project template (the project is local to
        // this test, but drop it explicitly for tidiness / GC determinism).
        JIPipe.getNodeTemplates().removeFromProject(t, project);
    }

    // --- Round 2: the database builder assigns the Downloaded scope to
    // repository-sourced global templates (the true proof they land under the
    // Templates / Downloaded row of the node browser, not Global) ---
    @Test
    void downloadedGlobalTemplateGetsTheDownloadedScopeInTheDatabase() throws Exception {
        JIPipeNodeTemplate downloaded = JsonUtils.readFromString(
            "{\"name\":\"builder-downloaded-probe\"}", JIPipeNodeTemplate.class);
        downloaded.setGraph(new JIPipeGraph());
        downloaded.setSource("https://example.org/repo/node-templates.json");
        JIPipeNodeTemplate userOwned = JsonUtils.readFromString(
            "{\"name\":\"builder-user-probe\"}", JIPipeNodeTemplate.class);
        userOwned.setGraph(new JIPipeGraph()); // default source = the user source

        JIPipeProject project = new JIPipeProject();
        assertThat(JIPipe.getNodeTemplates().addToGlobal(downloaded)).isTrue();
        assertThat(JIPipe.getNodeTemplates().addToGlobal(userOwned)).isTrue();
        try {
            NoRebuildDatabase database = new NoRebuildDatabase(project);
            new JIPipeNodeDatabaseBuilderRun(database).run();

            var downloadedEntry = database.getEntries().stream()
                .filter(e -> "builder-downloaded-probe".equals(e.getName()))
                .findFirst().orElseThrow();
            var userEntry = database.getEntries().stream()
                .filter(e -> "builder-user-probe".equals(e.getName()))
                .findFirst().orElseThrow();

            assertThat(((CreateNewNodesByTemplateDatabaseEntry) downloadedEntry).getScope())
                .isEqualTo(TemplateScope.Downloaded);
            assertThat(((CreateNewNodesByTemplateDatabaseEntry) userEntry).getScope())
                .isEqualTo(TemplateScope.Global);
        } finally {
            JIPipe.getNodeTemplates().removeFromGlobal(downloaded);
            JIPipe.getNodeTemplates().removeFromGlobal(userOwned);
        }
    }

    /**
     * A node database that never schedules its async builder run: the
     * constructor (and the updater's one-shot timer) invoke the virtual
     * {@link #rebuildImmediately()}, which is overridden here to a no-op so the
     * test can run {@link JIPipeNodeDatabaseBuilderRun#run()} directly and
     * synchronously. Same technique as {@code NodeDatabaseTaxonomyTest}.
     */
    private static class NoRebuildDatabase extends JIPipeNodeDatabase {
        private NoRebuildDatabase(JIPipeProject project) {
            super(project);
        }

        @Override
        public void rebuildImmediately() {
            // No-op: the test drives the builder run synchronously.
        }
    }
}
