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

package org.hkijena.jipipe.desktop.app.grapheditor.templatemanage;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEvent;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEventListener;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Swing-free {@link TemplateManagementController} - the store
 * logic behind the "Configure templates" modal (task 9, spec §8.3).
 * <p>
 * The controller partitions the template store by {@link TemplateScope}
 * (R9d), copies / deletes / edits templates through the service component,
 * and round-trips the legacy JSON-array export/import format. System scope is
 * read-only (a no-op for every mutating operation). Boots the {@link JIPipe}
 * instance so the real service component (and the bundled System pattern
 * templates) are available. It never touches Swing - no dialog, no list.
 */
class TemplateManagementControllerTest {

    private static final String STUB_ID = "template-mgmt-ctrl-test:stub-node";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("template-mgmt-ctrl-test", "1.0.0", "template-mgmt-ctrl-test");
    /**
     * The user-owned templates this test adds to the (shared) global store, cleaned up in {@link #cleanup()}.
     */
    private static final List<JIPipeNodeTemplate> addedGlobal = new ArrayList<>();
    /**
     * The plugin (System-scope) templates this test registers via addFromPlugin, removed in {@link #cleanup()}.
     */
    private static final List<JIPipeNodeTemplate> addedPlugin = new ArrayList<>();
    /**
     * Throwaway base for the JIPipe user dir. {@link #boot()} points the shared
     * singleton's {@code overrideUserDirBase} here so every global-template
     * file this test writes lands in a temp dir, NEVER the real user store
     * (~/.local/share/JIPipe/profiles/&#60;v&#62;/node-templates/). Pre-hermetic,
     * the stub templates leaked there: the in-memory cleanup removed them from
     * the list, but {@code removeFromGlobal}'s value-keyed file-map lookup
     * misses once a template's graph has been re-serialized (its {@code data}
     * field - part of the map key's hash - changes), orphaning the on-disk
     * file. Each orphan then made every later JVM print
     * "Unable to find node type ... template-mgmt-ctrl-test:stub-node" at boot.
     * {@link #cleanup()} restores the override and deletes the temp dir.
     */
    private static Path tempUserDirBase;
    private final TemplateManagementController controller = new TemplateManagementController();

    @BeforeAll
    static void boot() throws Exception {
        // Hermeticize the global store (see tempUserDirBase javadoc): redirect
        // the user dir to a temp base BEFORE any template is added.
        tempUserDirBase = Files.createTempDirectory("jipipe-tmpl-ctrl-test");
        JIPipe.ensureInstance();
        JIPipe.getInstance().getInitializationSettings().setOverrideUserDirBase(tempUserDirBase);
        // Re-point the in-memory store + file map at the (empty) temp dir so a
        // real-dir template loaded by an earlier boot cannot linger in memory.
        JIPipe.getNodeTemplates().reloadGlobalTemplates(JIPipeProgressInfo.SILENT);

        // A namespaced stub node so the template graphs can be built from the
        // live registry without depending on which bundled core plugins booted.
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        if (!nodes.hasNodeInfoWithId(STUB_ID)) {
            nodes.register(new JIPipeJavaNodeInfo(STUB_ID, StubTemplateNode.class), TEST_DEPENDENCY);
        }
    }

    @AfterAll
    static void cleanup() {
        // Remove any templates this test added to the (temp) global store, so
        // the in-memory store is left as we found it; do not leak the stub into
        // the shared registry either.
        for (JIPipeNodeTemplate t : addedGlobal) {
            JIPipe.getNodeTemplates().removeFromGlobal(t);
        }
        for (JIPipeNodeTemplate t : addedPlugin) {
            JIPipe.getNodeTemplates().removeFromGlobal(t);
        }
        JIPipe.getNodes().unregister(STUB_ID);
        // Restore the shared singleton's user-dir override so no later test in
        // this JVM is silently redirected, then drop the temp base.
        if (JIPipe.getInstance() != null) {
            JIPipe.getInstance().getInitializationSettings().setOverrideUserDirBase(null);
        }
        deleteRecursivelyQuietly(tempUserDirBase);
    }

    /**
     * Best-effort recursive delete of the temp user-dir base.
     */
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

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    @Test
    void copyGlobalToProject() {
        JIPipeProject project = new JIPipeProject();
        JIPipeNodeTemplate global = addUniqueGlobal("Copy To Project Src");
        String name = global.getName();

        boolean[] refresh = {false};
        NodeTemplatesRefreshedEventListener listener = (NodeTemplatesRefreshedEvent e) -> refresh[0] = true;
        JIPipe.getNodeTemplates().getNodeTemplatesRefreshedEventEmitter().subscribe(listener);
        try {
            boolean ok = controller.copyTo(project, TemplateScope.Global, TemplateScope.Project, global);
            assertThat(ok).isTrue();
        } finally {
            JIPipe.getNodeTemplates().getNodeTemplatesRefreshedEventEmitter().unsubscribe(listener);
        }

        // A same-named copy now lives in the project metadata ...
        assertThat(project.getMetadata().getNodeTemplates())
                .anySatisfy(p -> assertThat(p.getName()).isEqualTo(name));
        // ... the original is still Global (a copy, not a move) ...
        assertThat(controller.forScope(null, TemplateScope.Global))
                .anySatisfy(g -> assertThat(g.getName()).isEqualTo(name));
        // ... a distinct object, so the project copy is not the global one.
        JIPipeNodeTemplate projectCopy = project.getMetadata().getNodeTemplates().stream()
                .filter(t -> t.getName().equals(name)).findFirst().orElseThrow();
        assertThat(projectCopy).isNotSameAs(global);
        // ... and a refresh fired.
        assertThat(refresh[0]).isTrue();
    }

    @Test
    void deleteProjectTemplate() {
        JIPipeProject project = new JIPipeProject();
        JIPipeNodeTemplate projectTemplate = tmpl("Delete Me Target");
        JIPipe.getNodeTemplates().addToProject(projectTemplate, project);
        assertThat(controller.forScope(project, TemplateScope.Project))
                .anySatisfy(t -> assertThat(t.getName()).isEqualTo("Delete Me Target"));

        boolean ok = controller.delete(project, TemplateScope.Project, projectTemplate);

        assertThat(ok).isTrue();
        assertThat(project.getMetadata().getNodeTemplates().stream()
                .map(JIPipeNodeTemplate::getName))
                .doesNotContain("Delete Me Target");
        assertThat(controller.forScope(project, TemplateScope.Project).stream()
                .map(JIPipeNodeTemplate::getName))
                .doesNotContain("Delete Me Target");
    }

    @Test
    void systemScopeIsReadOnly() {
        // A template registered as a plugin template (via addFromPlugin) is a
        // GENUINE System-scope template - it is in pluginTemplates AND carries
        // the Extension source AND is present in globalTemplates - so it
        // exercises the real read-only path (not a synthetic Extension-source
        // template that never entered the store).
        JIPipeNodeTemplate system = tmpl("Read Only System");
        JIPipe.getNodeTemplates().addFromPlugin(system);
        addedPlugin.add(system);
        assertThat(JIPipe.getNodeTemplates().isPluginTemplate(system)).isTrue();
        assertThat(controller.forScope(null, TemplateScope.System))
                .anySatisfy(t -> assertThat(t.getName()).isEqualTo(system.getName()));

        // Delete is a no-op (returns false) - the template stays in the System scope.
        assertThat(controller.delete(null, TemplateScope.System, system)).isFalse();
        assertThat(controller.forScope(null, TemplateScope.System))
                .anySatisfy(t -> assertThat(t.getName()).isEqualTo(system.getName()));

        // Edit is also a no-op (returns false) - the original is untouched.
        JIPipeNodeTemplate editedCopy = new JIPipeNodeTemplate(system);
        editedCopy.setName("Should Not Apply");
        assertThat(controller.edit(null, TemplateScope.System, system, editedCopy)).isFalse();
        assertThat(system.getName()).isEqualTo("Read Only System");
    }

    @Test
    void escapeCopyOfAPatternLandsInGlobal() {
        // The System -> Global "Copy to Global" escape hatch (spec §8.3): a user
        // copies a bundled pattern into their own store so they can edit the
        // copy. The copy is value-equal to the bundled original, so the naive
        // path (new JIPipeNodeTemplate + addToGlobal) is rejected by
        // addToGlobal_'s value-based isPluginTemplate / globalTemplates.contains
        // guards. This reproduces that with a REAL plugin template (registered
        // via addFromPlugin, so it is in pluginTemplates AND present in
        // globalTemplates) and asserts the dedicated user-copy path lands it in
        // the Global scope. (FAILS on the pre-fix code: copyTo returned false.)
        JIPipeNodeTemplate pattern = tmpl("Escaped Pattern");
        JIPipe.getNodeTemplates().addFromPlugin(pattern);
        addedPlugin.add(pattern);
        assertThat(controller.forScope(null, TemplateScope.System))
                .anySatisfy(t -> assertThat(t.getName()).isEqualTo("Escaped Pattern"));

        boolean ok = controller.copyTo(null, TemplateScope.System, TemplateScope.Global, pattern);
        assertThat(ok).isTrue();

        // A same-named copy now lives in the Global scope ...
        List<JIPipeNodeTemplate> globalScope = controller.forScope(null, TemplateScope.Global);
        assertThat(globalScope.stream().anyMatch(t -> t.getName().equals("Escaped Pattern"))).isTrue();
        // ... and it is NOT a plugin template (classified Global, not System) -
        // the identity-based pluginTemplates set + the user re-stamp make this hold.
        JIPipeNodeTemplate globalCopy = globalScope.stream()
                .filter(t -> t.getName().equals("Escaped Pattern")).findFirst().orElseThrow();
        assertThat(JIPipe.getNodeTemplates().isPluginTemplate(globalCopy)).isFalse();
        addedGlobal.add(globalCopy);
        // ... and the original is STILL in the System scope (a copy, not a move).
        assertThat(controller.forScope(null, TemplateScope.System))
                .anySatisfy(t -> assertThat(t.getName()).isEqualTo("Escaped Pattern"));
    }

    @Test
    void globalTabExcludesPluginTemplates() {
        // R9d's critical filter: getGlobalTemplates() now INCLUDES the bundled
        // System (plugin) templates, so the Global tab must filter them out,
        // or it would list the shipped patterns as user-global templates.
        List<JIPipeNodeTemplate> globalScope = controller.forScope(null, TemplateScope.Global);
        assertThat(globalScope.stream().noneMatch(JIPipe.getNodeTemplates()::isPluginTemplate)).isTrue();

        // The System tab, by contrast, IS exactly the plugin (bundled) set and
        // here is non-empty (the 64 curated corpus patterns are registered at init).
        List<JIPipeNodeTemplate> systemScope = controller.forScope(null, TemplateScope.System);
        assertThat(systemScope).isNotEmpty();
        assertThat(systemScope.stream().allMatch(JIPipe.getNodeTemplates()::isPluginTemplate)).isTrue();
    }

    @Test
    void exportAndImportRoundTrip() throws Exception {
        JIPipeNodeTemplate global = addUniqueGlobal("Round Trip Template");

        Path file = Files.createTempFile("template-roundtrip", ".json");
        Files.delete(file);
        boolean exported = controller.exportTemplates(null, TemplateScope.Global, List.of(global), file);
        assertThat(exported).isTrue();
        assertThat(Files.isRegularFile(file)).isTrue();

        JIPipeNodeTemplate original = global;
        // Import into a fresh Project (no collision with the already-stored
        // global, so the import genuinely lands rather than de-duping away).
        JIPipeProject project = new JIPipeProject();
        List<JIPipeNodeTemplate> imported = controller.importTemplates(project, TemplateScope.Project, file);
        assertThat(imported).hasSize(1);
        assertThat(imported.get(0).getName()).isEqualTo(original.getName());

        assertThat(project.getMetadata().getNodeTemplates().stream()
                .map(JIPipeNodeTemplate::getName))
                .contains(original.getName());
        // The imported copy is a distinct object from the original global one.
        JIPipeNodeTemplate projectCopy = project.getMetadata().getNodeTemplates().stream()
                .filter(t -> t.getName().equals(original.getName())).findFirst().orElseThrow();
        assertThat(projectCopy).isNotSameAs(original);
    }

    @Test
    void legacyMenuPathFileStillImports() throws Exception {
        // Task 3's lenient menu-path deserializer keeps old exports (which
        // still carry the removed "menu-path" key) importing. The key is
        // captured (and dropped on re-save) and never fails the read.
        Path file = Files.createTempFile("template-legacy", ".json");
        Files.writeString(file, "[{\"name\":\"Legacy Menu Path Import\",\"menu-path\":[\"Image\",\"Threshold\"]}]");

        List<JIPipeNodeTemplate> imported = controller.importTemplates(null, TemplateScope.Global, file);
        assertThat(imported).hasSize(1);
        assertThat(imported.get(0).getName()).isEqualTo("Legacy Menu Path Import");
        addedGlobal.add(imported.get(0));
    }

    @Test
    void deleteUneditedEscapeCopyKeepsBundledOriginalInMemory() {
        // Whole-branch follow-up: the bundled original P and its unedited user
        // copy C are VALUE-equal (JIPipeNodeTemplate.equals compares
        // name+description+data). removeFromGlobal used to match by value
        // (globalTemplates.contains/remove), so removing C evicted P - the
        // FIRST value match in the list (P was added before C) - from the
        // in-memory store. The on-disk file deleted was still C's (the BiMap
        // only maps C), so no data was lost and P re-registered on the next
        // start, but P silently vanished from the System tab for the rest of
        // the session. (FAILS on the value-based removal: P is gone from the
        // System scope after deleting C.)
        JIPipeNodeTemplate pattern = tmpl("Keep Me Bundled");
        JIPipe.getNodeTemplates().addFromPlugin(pattern);
        addedPlugin.add(pattern);
        assertThat(controller.forScope(null, TemplateScope.System))
                .anySatisfy(t -> assertThat(t.getName()).isEqualTo("Keep Me Bundled"));

        boolean ok = controller.copyTo(null, TemplateScope.System, TemplateScope.Global, pattern);
        assertThat(ok).isTrue();
        JIPipeNodeTemplate copy = controller.forScope(null, TemplateScope.Global).stream()
                .filter(t -> t.getName().equals("Keep Me Bundled")).findFirst().orElseThrow();
        assertThat(copy).isNotSameAs(pattern);
        addedGlobal.add(copy);

        // Delete the UNEDITED copy.
        assertThat(controller.delete(null, TemplateScope.Global, copy)).isTrue();

        // The copy is gone from Global ...
        assertThat(controller.forScope(null, TemplateScope.Global).stream()
                .map(JIPipeNodeTemplate::getName)).doesNotContain("Keep Me Bundled");
        // ... AND the bundled original is still in the System scope (pre-fix it
        // was the one evicted from the in-memory list).
        assertThat(controller.forScope(null, TemplateScope.System))
                .anySatisfy(t -> assertThat(t.getName()).isEqualTo("Keep Me Bundled"));
    }

    @Test
    void rePickingTheFunctionClearsThePreviouslyFooter() {
        // §9.4: on auto-migration the old menu path is appended to the
        // description as a "Previously:" footer; RE-PICKING the function in
        // the editor clears it (the function is now an explicit user choice,
        // not the auto-mapped one). A mere re-save that does NOT change the
        // function keeps the footer.
        JIPipeNodeTemplate t = addUniqueGlobal("Footer RePick Template");
        JIPipeTaxonomyFunctionRef fnImages = new JIPipeTaxonomyFunctionRef("jipipe:function:images");
        JIPipeTaxonomyFunctionRef fnTables = new JIPipeTaxonomyFunctionRef("jipipe:function:tables");
        t.setTaxonomyFunction(fnImages);
        t.setDescription(new HTMLText("<p>My desc</p><i><br>Previously: Image → Threshold</i>"));

        // Re-pick: the edited copy has a DIFFERENT function -> footer cleared.
        JIPipeNodeTemplate edited = new JIPipeNodeTemplate(t);
        edited.setTaxonomyFunction(fnTables);
        assertThat(controller.edit(null, TemplateScope.Global, t, edited)).isTrue();
        assertThat(t.getTaxonomyFunction().getId()).isEqualTo("jipipe:function:tables");
        assertThat(t.getDescription().getHtml()).doesNotContain("Previously");

        // Re-save WITHOUT changing the function -> footer kept.
        t.setDescription(new HTMLText("<p>My desc</p><i><br>Previously: Image → Threshold</i>"));
        JIPipeNodeTemplate editedSame = new JIPipeNodeTemplate(t); // same function (copy)
        assertThat(controller.edit(null, TemplateScope.Global, t, editedSame)).isTrue();
        assertThat(t.getTaxonomyFunction().getId()).isEqualTo("jipipe:function:tables");
        assertThat(t.getDescription().getHtml()).contains("Previously");
    }

    @Test
    void deletedGlobalTemplateFileIsRemovedFromDisk() throws Exception {
        // The tests write into the REAL user dir (service.getStoragePath()), so
        // this test must leave the directory exactly as it found it: add a
        // template (+1 file), delete it (back to baseline). Pre-fix,
        // removeFromGlobal could not find the file - addToGlobal_ /
        // addToGlobalAsUserCopy never recorded it in globalTemplatesPaths - so
        // the delete left the file behind (count stayed baseline + 1) and the
        // "deleted" template resurrected on the next reload. This asserts the
        // file is genuinely removed, and is self-cleaning (net-zero on disk).
        long before = countStorageFiles();
        JIPipeNodeTemplate t = addUniqueGlobal("Disk Cleanup Template");
        assertThat(countStorageFiles()).isEqualTo(before + 1);

        boolean deleted = controller.delete(null, TemplateScope.Global, t);
        assertThat(deleted).isTrue();
        assertThat(countStorageFiles()).isEqualTo(before);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Counts regular files in the global template storage dir (0 when absent).
     */
    private long countStorageFiles() throws Exception {
        Path dir = JIPipe.getNodeTemplates().getStoragePath();
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (var stream = Files.list(dir)) {
            return stream.filter(Files::isRegularFile).count();
        }
    }

    /**
     * Adds a unique-named template to the global store, tracking it for cleanup.
     */
    private JIPipeNodeTemplate addUniqueGlobal(String baseName) {
        String name = baseName + " " + System.nanoTime();
        JIPipeNodeTemplate template = tmpl(name);
        boolean added = JIPipe.getNodeTemplates().addToGlobal(template);
        assertThat(added).as("addToGlobal(" + name + ")").isTrue();
        addedGlobal.add(template);
        return template;
    }

    /**
     * Builds a real template whose graph holds one stub node under the given name.
     */
    private JIPipeNodeTemplate tmpl(String name) {
        JIPipeGraph graph = new JIPipeGraph();
        graph.insertNode(JIPipe.getNodes().getInfoById(STUB_ID).newInstance());
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        template.setName(name);
        template.setGraph(graph);
        return template;
    }

    // ------------------------------------------------------------------
    // Downloaded scope (round-2 spec: downloaded templates are read-only;
    // the escape hatch is a copy into the user's own templates, publishing
    // the local version is the future #1333 feature)
    // ------------------------------------------------------------------

    /**
     * Adds a template to the global store whose source is a repository URL (a downloaded template).
     */
    private JIPipeNodeTemplate addUniqueDownloaded(String baseName) {
        String name = baseName + " " + System.nanoTime();
        JIPipeNodeTemplate template = tmpl(name);
        template.setSource("https://asb-git.hki-jena.de/RGerst/jipipe-templates/raw/main/repo.json");
        boolean added = JIPipe.getNodeTemplates().addToGlobal(template);
        assertThat(added).as("addToGlobal(" + name + ")").isTrue();
        addedGlobal.add(template);
        return template;
    }

    @Test
    void downloadedTemplatesArePartitionedOutOfUserGlobal() {
        JIPipeNodeTemplate user = addUniqueGlobal("User Global Template");
        JIPipeNodeTemplate downloaded = addUniqueDownloaded("Downloaded Template");

        // The partition is membership-based (the shared store may hold other
        // user/downloaded templates from elsewhere): the repository-sourced
        // template is in Downloaded and NOT in the user's own list, and the
        // user-sourced one is in Global and NOT in Downloaded.
        assertThat(controller.forScope(null, TemplateScope.Downloaded)).contains(downloaded);
        assertThat(controller.forScope(null, TemplateScope.Global))
                .contains(user)
                .doesNotContain(downloaded);
        assertThat(controller.forScope(null, TemplateScope.Downloaded)).doesNotContain(user);
        // ... and neither leaks into System or Project.
        assertThat(controller.forScope(null, TemplateScope.System)).doesNotContain(downloaded, user);
        assertThat(controller.forScope(null, TemplateScope.Project)).doesNotContain(downloaded, user);
    }

    @Test
    void downloadedTemplateCannotBeEdited() {
        JIPipeNodeTemplate downloaded = addUniqueDownloaded("Downloaded Read-Only");
        JIPipeNodeTemplate edited = new JIPipeNodeTemplate(downloaded);
        edited.setDescription(new HTMLText("edited"));

        assertThat(controller.edit(null, TemplateScope.Downloaded, downloaded, edited))
                .as("editing a downloaded template is rejected (read-only repository artifact)")
                .isFalse();
        assertThat(downloaded.getDescription().getHtml()).doesNotContain("edited");
    }

    @Test
    void downloadedTemplateCopyToGlobalCreatesAnEditableUserVersion() {
        JIPipeProject project = new JIPipeProject();
        JIPipeNodeTemplate downloaded = addUniqueDownloaded("Downloaded Copy Src");

        boolean ok = controller.copyTo(project, TemplateScope.Downloaded, TemplateScope.Global, downloaded);
        assertThat(ok).as("the escape hatch must store the user copy").isTrue();

        // The copy is a USER-owned template (source re-stamped) ...
        JIPipeNodeTemplate copy = controller.forScope(null, TemplateScope.Global).stream()
                .filter(t -> t.getName().equals(downloaded.getName()))
                .findFirst().orElseThrow();
        assertThat(copy).isNotSameAs(downloaded);
        assertThat(copy.getSource()).isEqualTo(JIPipeNodeTemplate.SOURCE_USER);
        // ... the downloaded original is untouched and still read-only scoped,
        // and Downloaded is never a copy TARGET.
        assertThat(downloaded.getSource()).isNotEqualTo(JIPipeNodeTemplate.SOURCE_USER);
        assertThat(controller.forScope(null, TemplateScope.Downloaded)).contains(downloaded);
        assertThat(controller.copyTo(project, TemplateScope.Global, TemplateScope.Downloaded, copy))
                .as("Downloaded is a valid source, never a target")
                .isFalse();

        // Tidy: remove the user copy we just made (the addedGlobal cleanup
        // tracks the downloaded original, not the copy).
        assertThat(controller.delete(null, TemplateScope.Global, copy)).isTrue();
    }

    @Test
    void downloadedTemplateCanBeRemovedButNotViaSystemPath() {
        JIPipeNodeTemplate downloaded = addUniqueDownloaded("Downloaded Removable");

        assertThat(controller.delete(null, TemplateScope.Downloaded, downloaded))
                .as("removing a download is allowed (re-obtainable via 'Get more')")
                .isTrue();
        assertThat(controller.forScope(null, TemplateScope.Downloaded)).doesNotContain(downloaded);
        assertThat(JIPipe.getNodeTemplates().isInGlobal(downloaded)).isFalse();
    }

    // ------------------------------------------------------------------
    // Stub node
    // ------------------------------------------------------------------

    /**
     * A minimal, unconverted node used purely as material for the template
     * graphs. It declares no taxonomy, so the entry/service paths that walk
     * the graph stay deterministic.
     */
    @SetJIPipeDocumentation(name = "Template Management Stub Node", description = "A stub for template-management testing.")
    @ConfigureJIPipeNode
    public static class StubTemplateNode extends JIPipeGraphNode {
        public StubTemplateNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }
    }
}
