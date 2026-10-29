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

package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeNodeTaxonomyTags;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeClassification;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewCompartmentNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.relations.NodeRelations;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.*;
import org.hkijena.jipipe.api.nodes.taxonomy.families.TrackMateFamily;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the taxonomy facets of the node database ({@link NodeDatabaseTaxonomyTest}):
 * the five new {@link JIPipeNodeDatabaseEntry} facet getters populated from
 * {@link JIPipeNodeInfo} in the "create node by info" entries, and the
 * {@link JIPipeNodeDatabase#relations(JIPipeNodeDatabaseEntry, int) relations}
 * query (Similar / Previous / Next) backed by the relation index.
 * <p>
 * The synthetic nodes are three steps of {@link TrackMateFamily}
 * (detect → track → filter), plus same-framework, shared-tag,
 * auto-import, and tagless siblings. The database is built directly from
 * {@link JIPipeNodeInfo}s via
 * {@link CreateNewNodeByInfoDatabaseEntry}
 * and {@link JIPipeNodeDatabase#setEntries(List)}, bypassing the full
 * {@code JIPipeNodeDatabaseBuilderRun} (whose global entry cache would
 * otherwise leak synthetic entries into later tests); the entry
 * constructors are the production population path for the facets.
 */
class NodeDatabaseTaxonomyTest {

    /**
     * Stable id literal for the stub taxonomy function (see the stable-ID rule).
     */
    private static final String FUNCTION_ID = "node-db-taxonomy-test:function";

    /**
     * Stable id literal for the stub taxonomy framework (see the stable-ID rule).
     */
    private static final String FRAMEWORK_ID = "node-db-taxonomy-test:framework";

    private static final String TEST_DEPENDENCY_ID = "node-db-taxonomy-test";

    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency(TEST_DEPENDENCY_ID, "1.0.0", "node-database-taxonomy-test");

    private static final String SPOT_DETECT_ID = "nodbtax:spot-detect";
    private static final String TRACK_ID = "nodbtax:track";
    private static final String FILTER_ID = "nodbtax:filter-tracks";
    private static final String OTHER_FRAMEWORK_ID = "nodbtax:other-framework";
    private static final String SHARED_TAG_1 = "nodbtax:shared-tag-1";
    private static final String SHARED_TAG_2 = "nodbtax:shared-tag-2";
    private static final String AUTO_IMPORT_ID = "nodbtax:auto-import";
    private static final String NO_TAGS_ID = "nodbtax:no-tags";
    private static final String SAME_FUNCTION_ID = "nodbtax:same-function";

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    /**
     * Registers all synthetic nodes. The order matters: it determines both
     * the {@code FamilyAssembler} step order (detect → track) and the
     * database entry order, and it guarantees that {@code after} classes
     * resolve through the class→node-id mapping (detect is registered
     * before track's step is assembled).
     */
    private static void registerNodes() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        registerNode(SpotDetectNode.class, SPOT_DETECT_ID, nodes);
        registerNode(TrackNode.class, TRACK_ID, nodes);
        registerNode(FilterTracksNode.class, FILTER_ID, nodes);
        registerNode(OtherFrameworkNode.class, OTHER_FRAMEWORK_ID, nodes);
        registerNode(SharedTagOneNode.class, SHARED_TAG_1, nodes);
        registerNode(SharedTagTwoNode.class, SHARED_TAG_2, nodes);
        registerNode(AutoImportNode.class, AUTO_IMPORT_ID, nodes);
        registerNode(NoTagsNode.class, NO_TAGS_ID, nodes);
        registerNode(SameFunctionNode.class, SAME_FUNCTION_ID, nodes);
    }

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    private static void registerNode(Class<? extends JIPipeGraphNode> nodeClass, String id, JIPipeNodesServiceComponent nodes) {
        if (nodes.hasNodeInfoWithId(id)) {
            nodes.unregister(id);
        }
        JIPipeNodeInfo info = new JIPipeJavaNodeInfo(id, nodeClass);
        nodes.register(info, TEST_DEPENDENCY);
    }

    /**
     * Builds a fresh database holding only the entries created here: one
     * "create node by info" entry per synthetic node.
     * <p>
     * Task 26a: the alias entry for the detect node was removed (the
     * menu-location-backed alias entry no longer exists).
     */
    private static JIPipeNodeDatabase databaseWithAllEntries() {
        JIPipeNodeDatabase database = new TestNodeDatabase();
        List<JIPipeNodeDatabaseEntry> entries = new java.util.ArrayList<>();
        for (String id : List.of(SPOT_DETECT_ID, TRACK_ID, FILTER_ID, OTHER_FRAMEWORK_ID,
                SHARED_TAG_1, SHARED_TAG_2, AUTO_IMPORT_ID, NO_TAGS_ID, SAME_FUNCTION_ID)) {
            JIPipeNodeInfo info = JIPipe.getNodes().getRegisteredNodeInfos().get(id);
            entries.add(new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + id, info));
        }
        database.setEntries(entries);
        return database;
    }

    private static CreateNewNodeByInfoDatabaseEntry byInfo(JIPipeNodeDatabase database, String nodeId) {
        for (JIPipeNodeDatabaseEntry entry : database.getEntries()) {
            if (entry instanceof CreateNewNodeByInfoDatabaseEntry && nodeId.equals(entry.getOriginNodeId())) {
                return (CreateNewNodeByInfoDatabaseEntry) entry;
            }
        }
        fail("no 'create node by info' entry for node id '" + nodeId + "' in the database");
        return null;
    }

    @AfterEach
    void cleanup() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        // Synthetic node registrations (registeredNodeInfos)
        nodes.unregister(SPOT_DETECT_ID);
        nodes.unregister(TRACK_ID);
        nodes.unregister(FILTER_ID);
        nodes.unregister(OTHER_FRAMEWORK_ID);
        nodes.unregister(SHARED_TAG_1);
        nodes.unregister(SHARED_TAG_2);
        nodes.unregister(AUTO_IMPORT_ID);
        nodes.unregister(NO_TAGS_ID);
        nodes.unregister(SAME_FUNCTION_ID);
        // The TrackMate steps assembled into the shared FamilyAssembler by
        // the three synthetic family nodes. TrackMateFamily is a
        // production family class (kept), but the resolver's
        // id→family registry only ever learns about it through node step
        // annotations, and no jipipe-core node carries TrackMate steps -
        // so the registry entry is ours to remove, keeping the shared
        // family registry clean for other tests.
        nodes.getFamilyAssembler().unregisterStep(SPOT_DETECT_ID, TrackMateFamily.ID);
        nodes.getFamilyAssembler().unregisterStep(TRACK_ID, TrackMateFamily.ID);
        nodes.getFamilyAssembler().unregisterStep(FILTER_ID, TrackMateFamily.ID);
        nodes.unregisterFamily(TrackMateFamily.ID);
        // Re-run the (idempotent) registration pipeline so that enrichment
        // recomputes over the surviving real nodes only.
        nodes.runRegistrationTasks();
    }

    /**
     * The "create node by info" entry carries the taxonomy facets of its
     * node info (the production population path).
     */
    @Test
    void entryCarriesTaxonomyFacets() {
        registerNodes();

        JIPipeNodeInfo detectInfo = JIPipe.getNodes().getRegisteredNodeInfos().get(SPOT_DETECT_ID);
        CreateNewNodeByInfoDatabaseEntry entry =
                new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + SPOT_DETECT_ID, detectInfo);

        // Function facets: the primary function id is present, unconverted
        // nodes have an empty set (default on the interface)
        assertTrue(entry.getFunctionIds().contains(FUNCTION_ID),
                "function ids must contain the primary function id, got: " + entry.getFunctionIds());
        assertTrue(entry.getFunctionIds().containsAll(detectInfo.getSecondaryFunctions().stream().map(JIPipeNodeTaxonomyFunction::getId).toList()));

        assertEquals(FRAMEWORK_ID, entry.getFramework().getId());
        assertEquals(Set.of(JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D), entry.getSpatialDimensions());
        assertEquals(JIPipeNodeTaxonomyInputProfile.NA, entry.getInputProfile());
        // Merged tags: the node-level tag plus the function's tag
        assertEquals(Set.of("detect", "tm-func-tag"), entry.getTags());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * The "create node by info" entry exposes the origin node id; entries
     * that do not wrap a node type (compartment entries) expose
     * {@code null}.
     * <p>
     * Task 26a: the "create node by info alias" entry (and its
     * menu-location-backed alias data) was removed - aliases
     * became secondary functions, which the base entry already carries.
     */
    @Test
    void baseEntryExposesOriginNodeId() {
        registerNodes();

        JIPipeNodeInfo detectInfo = JIPipe.getNodes().getRegisteredNodeInfos().get(SPOT_DETECT_ID);
        CreateNewNodeByInfoDatabaseEntry base =
                new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + SPOT_DETECT_ID, detectInfo);

        assertEquals(SPOT_DETECT_ID, base.getOriginNodeId());

        // A compartment entry does not wrap a node type
        CreateNewCompartmentNodeDatabaseEntry compartment = new CreateNewCompartmentNodeDatabaseEntry();
        assertNull(compartment.getOriginNodeId());
        assertTrue(compartment.getFunctionIds().isEmpty());
        assertInstanceOf(NoFramework.class, compartment.getFramework());
        assertTrue(compartment.getSpatialDimensions().isEmpty());
        assertEquals(JIPipeNodeTaxonomyInputProfile.NA, compartment.getInputProfile());
        assertTrue(compartment.getTags().isEmpty());
    }

    /**
     * {@link JIPipeNodeDatabase#relations(JIPipeNodeDatabaseEntry, int)}
     * exposes the workflow successors as {@code next} and the workflow
     * predecessors as {@code previous}, mapped back to entries, ranked by
     * edge support; an entry whose node has no workflow neighbors yields
     * empty lists. The synthetic TrackMate chain is detect → track →
     * filter, so detect's next is track, track's next is filter and its
     * previous is detect, and filter's next is empty.
     */
    @Test
    void relationsExposeFamilyNextAndPreviousMappedToEntries() {
        registerNodes();
        JIPipeNodeDatabase database = databaseWithAllEntries();
        CreateNewNodeByInfoDatabaseEntry detect = byInfo(database, SPOT_DETECT_ID);
        CreateNewNodeByInfoDatabaseEntry track = byInfo(database, TRACK_ID);
        CreateNewNodeByInfoDatabaseEntry filter = byInfo(database, FILTER_ID);

        // Family successors (the old followUps) now surface as `next`
        assertEquals(List.of(track), database.relations(detect, 10).next());
        assertEquals(List.of(filter), database.relations(track, 10).next());

        // The previously-missing `previous` direction: the family
        // predecessors, mapped back to entries
        assertEquals(List.of(), database.relations(detect, 10).previous());
        assertEquals(List.of(detect), database.relations(track, 10).previous());
        assertEquals(List.of(track), database.relations(filter, 10).previous());

        // A terminal node and a node outside any family have no neighbors
        assertEquals(List.of(), database.relations(filter, 10).next());
        assertEquals(List.of(), database.relations(byInfo(database, NO_TAGS_ID), 10).next());
        assertEquals(List.of(), database.relations(byInfo(database, NO_TAGS_ID), 10).previous());
    }

    /**
     * {@link JIPipeNodeDatabase#relations(JIPipeNodeDatabaseEntry, int)}
     * returns {@code similar} ranked in tiers: same-function, then
     * same-framework, then shared-tag (≥2 shared before 1), de-duplicated and
     * capped, with auto-import entries down-ranked to the end of their tier.
     * The workflow-neighbor nodes are NOT in {@code similar} - they are
     * surfaced by {@code next}/{@code previous} instead, so the three lists
     * stay disjoint.
     * <p>
     * Expected for the synthetic set (detect's family chain is
     * detect → track → filter, so track is a {@code next} neighbor and is
     * excluded from {@code similar}; filter is two hops away, so it is NOT a
     * neighbor but qualifies for no tier and is absent):
     * <ol>
     * <li>{@code similar(detect, 10)} =
     * [{@code sameFunction}, {@code otherFramework}, {@code autoImport},
     * {@code sharedTagOne}, {@code sharedTagTwo}] - {@code sameFunction}
     * ranks in the function tier (above the same-framework siblings, the
     * "shared functionality outranks a large shared framework" guard), then
     * the framework tier ({@code otherFramework} before the down-ranked
     * {@code autoImport}), then the tag tier;</li>
     * <li>{@code next(detect, 10)} = [{@code track}] (the family successor);</li>
     * <li>{@code previous(detect, 10)} = [] (a family start step).</li>
     * </ol>
     * {@code track} must appear in {@code next} and NOT in {@code similar};
     * {@code noTags} and {@code filter} qualify for no tier and are absent.
     */
    @Test
    void relationsRankSimilarAndKeepWorkflowNeighborsDisjoint() {
        registerNodes();
        JIPipeNodeDatabase database = databaseWithAllEntries();
        CreateNewNodeByInfoDatabaseEntry detect = byInfo(database, SPOT_DETECT_ID);
        CreateNewNodeByInfoDatabaseEntry track = byInfo(database, TRACK_ID);
        CreateNewNodeByInfoDatabaseEntry filter = byInfo(database, FILTER_ID);
        CreateNewNodeByInfoDatabaseEntry otherFramework = byInfo(database, OTHER_FRAMEWORK_ID);
        CreateNewNodeByInfoDatabaseEntry sharedTag1 = byInfo(database, SHARED_TAG_1);
        CreateNewNodeByInfoDatabaseEntry sharedTag2 = byInfo(database, SHARED_TAG_2);
        CreateNewNodeByInfoDatabaseEntry autoImport = byInfo(database, AUTO_IMPORT_ID);
        CreateNewNodeByInfoDatabaseEntry sameFunction = byInfo(database, SAME_FUNCTION_ID);

        NodeRelations relations = database.relations(detect, 10);

        // The three lists are disjoint
        assertEquals(List.of(track), relations.next());
        assertEquals(List.of(), relations.previous());
        List<JIPipeNodeDatabaseEntry> expectedSimilar = List.of(
                sameFunction, otherFramework, autoImport, sharedTag1, sharedTag2);
        assertEquals(expectedSimilar, relations.similar());

        // The family successor (track) is in `next`, NOT in `similar`
        assertTrue(relations.next().contains(track));
        assertFalse(relations.similar().contains(track),
                "a next-neighbor must be excluded from similar (the lists stay disjoint)");
        // filter is two hops away (not a direct neighbor) and qualifies for
        // no similar tier, so it is absent from all three lists
        assertFalse(relations.similar().contains(filter));
        assertFalse(relations.next().contains(filter));

        // The same-function node outranks the same-framework node, even
        // though it is registered (and listed) after them.
        int sameFunctionIdx = relations.similar().indexOf(sameFunction);
        int otherFrameworkIdx = relations.similar().indexOf(otherFramework);
        assertTrue(sameFunctionIdx < otherFrameworkIdx,
                "a same-function entry must rank above a same-framework entry");

        // Cap: each list is independently capped at maxPerList
        assertEquals(List.of(sameFunction, otherFramework),
                database.relations(detect, 2).similar());
        assertEquals(List.of(track), database.relations(detect, 2).next());
    }

    /**
     * A second {@code relations} call returns the same cached
     * {@link NodeRelations} instance; the cache is invalidated when the
     * entries are replaced, after which a fresh (differently ordered) result
     * is computed.
     */
    @Test
    void relationsResultIsCachedUntilEntriesAreReplaced() {
        registerNodes();
        JIPipeNodeDatabase database = databaseWithAllEntries();
        CreateNewNodeByInfoDatabaseEntry detect = byInfo(database, SPOT_DETECT_ID);
        CreateNewNodeByInfoDatabaseEntry track = byInfo(database, TRACK_ID);
        CreateNewNodeByInfoDatabaseEntry otherFramework = byInfo(database, OTHER_FRAMEWORK_ID);

        NodeRelations first = database.relations(detect, 10);
        assertSame(first, database.relations(detect, 10), "relations() must be cached per entry");

        // Replacing the entries (e.g. the builder run) invalidates the
        // cache; in the new entry set detect's same-function siblings
        // (sameFunction, track) are gone from the `similar` tier, so the
        // only remaining similar candidate is the same-framework
        // {@code otherFramework}, while {@code track} (the family
        // successor) still surfaces as the first `next` entry.
        List<JIPipeNodeDatabaseEntry> reordered = List.of(
                otherFramework, detect, track);
        database.setEntries(reordered);
        NodeRelations second = database.relations(detect, 10);
        assertNotSame(first, second, "replacing the entries must invalidate the relations cache");
        assertEquals(List.of(otherFramework), second.similar(),
                "the similar order must reflect the new entry set (framework tier)");
        assertEquals(List.of(track), second.next(),
                "the next neighbor (family successor) is unaffected by the entry-set change");
    }

    /**
     * A node database that never schedules its async builder run: the
     * constructor (and the updater's one-shot 1-second timer) invoke the
     * virtual {@link #rebuildImmediately()}, which is overridden here to
     * a no-op. The test builds its entries manually via
     * {@link JIPipeNodeDatabase#setEntries(List)}, which must not be
     * clobbered by a concurrently running builder.
     */
    private static class TestNodeDatabase extends JIPipeNodeDatabase {
        private TestNodeDatabase() {
            super();
        }

        @Override
        public void rebuildImmediately() {
            // No-op: the test supplies the entries directly.
        }
    }

    // ------------------------------------------------------------------
    // Stub taxonomy members
    // ------------------------------------------------------------------

    /**
     * Minimal taxonomy function carrying one tag (self-validating stub).
     */
    static class StubFunction implements JIPipeNodeTaxonomyFunction {
        public StubFunction() {
        }

        @Override
        public String getId() {
            return FUNCTION_ID;
        }

        @Override
        public String getName() {
            return "Node DB Test Function";
        }

        @Override
        public JIPipeNodeTaxonomyFunction getParent() {
            return null;
        }

        @Override
        public Float getColorHue() {
            return 0.42f;
        }

        @Override
        public String getDescription() {
            return "Test stub function";
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public Set<String> getTags() {
            return Set.of("tm-func-tag");
        }

        @Override
        public boolean isVisibleInPipeline() {
            return true;
        }

        @Override
        public boolean isVisibleInCompartments() {
            return true;
        }
    }

    /**
     * A tag-free taxonomy function: nodes carrying it are "converted"
     * (so that tag enrichment runs and {@code getMergedTags()} reflects
     * their node-level tags) without contributing their own tags.
     */
    static class StubNoTagFunction implements JIPipeNodeTaxonomyFunction {
        public StubNoTagFunction() {
        }

        @Override
        public String getId() {
            return FUNCTION_ID + ":no-tags";
        }

        @Override
        public String getName() {
            return "Node DB Test Function (no tags)";
        }

        @Override
        public JIPipeNodeTaxonomyFunction getParent() {
            return null;
        }

        @Override
        public Float getColorHue() {
            return 0.42f;
        }

        @Override
        public String getDescription() {
            return "Test stub function without tags";
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public Set<String> getTags() {
            return Set.of();
        }

        @Override
        public boolean isVisibleInPipeline() {
            return true;
        }

        @Override
        public boolean isVisibleInCompartments() {
            return true;
        }
    }

    /**
     * Minimal taxonomy framework.
     */
    static class StubFramework implements JIPipeNodeTaxonomyFramework {
        public StubFramework() {
        }

        @Override
        public String getId() {
            return FRAMEWORK_ID;
        }

        @Override
        public String getName() {
            return "Node DB Test Framework";
        }

        @Override
        public String getAbbreviation() {
            return "NDBT";
        }

        @Override
        public String getPatternStyleId() {
            return "initials";
        }
    }

    /**
     * Start step of the {@link TrackMateFamily}: the spot detector.
     * Carries the stub function (tags merged from function + node level),
     * the stub framework, 3D spatial dimension, and the
     * {@code after = {} start step annotation.
     */
    @ConfigureJIPipeNode(function = {StubFunction.class}, framework = StubFramework.class,
            spatialDimensions = {JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D})
    @JIPipeNodeTaxonomyTags("detect")
    @JIPipeNodeTaxonomyFamilyStepAnnotation(family = TrackMateFamily.class, label = "Detect spots")
    public static class SpotDetectNode extends JIPipeGraphNode {
        public SpotDetectNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }

    /**
     * Second step of the {@link TrackMateFamily}, after the spot detector.
     * Same function and framework as {@link SpotDetectNode}, no node-level
     * tags (only the function's tag is merged in).
     */
    @ConfigureJIPipeNode(function = {StubFunction.class}, framework = StubFramework.class)
    @JIPipeNodeTaxonomyFamilyStepAnnotation(family = TrackMateFamily.class, label = "Track",
            after = {SpotDetectNode.class})
    public static class TrackNode extends JIPipeGraphNode {
        public TrackNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }

    /**
     * Third (terminal) step of the {@link TrackMateFamily}, after the
     * track node: the sibling that {@code related(detect)} must rank
     * first (the follow-up, track, being excluded from the sibling tier).
     * Deliberately has no framework of its own and no tags.
     */
    @JIPipeNodeTaxonomyFamilyStepAnnotation(family = TrackMateFamily.class, label = "Filter tracks",
            after = {TrackNode.class})
    public static class FilterTracksNode extends JIPipeGraphNode {
        public FilterTracksNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }

    /**
     * Same framework as the family nodes, no family step and no shared tags:
     * a pure tier-2 candidate.
     */
    @ConfigureJIPipeNode(framework = StubFramework.class)
    public static class OtherFrameworkNode extends JIPipeGraphNode {
        public OtherFrameworkNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }

    /**
     * Shares both of the detect node's tags (node-level; the tag-free
     * function contributes none): tier 3, two-shared slot.
     */
    @ConfigureJIPipeNode(function = {StubNoTagFunction.class})
    @JIPipeNodeTaxonomyTags({"detect", "tm-func-tag"})
    public static class SharedTagOneNode extends JIPipeGraphNode {
        public SharedTagOneNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }

    /**
     * Shares exactly one of the detect node's tags (node-level; the
     * tag-free function contributes none): tier 3, one-shared slot.
     */
    @ConfigureJIPipeNode(function = {StubNoTagFunction.class})
    @JIPipeNodeTaxonomyTags("detect")
    public static class SharedTagTwoNode extends JIPipeGraphNode {
        public SharedTagTwoNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }

    /**
     * Same framework as the family nodes and classified as auto-import:
     * must be de-ranked to the end of the framework tier (and of the tag
     * tier, since it also shares both of detect's tags).
     */
    @ConfigureJIPipeNode(function = {StubNoTagFunction.class}, framework = StubFramework.class,
            nodeClassification = JIPipeNodeClassification.AutoImport)
    @JIPipeNodeTaxonomyTags({"detect", "tm-func-tag"})
    public static class AutoImportNode extends JIPipeGraphNode {
        public AutoImportNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }

    /**
     * No framework, no family step, no tags: excluded from related().
     */
    public static class NoTagsNode extends JIPipeGraphNode {
        public NoTagsNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }

    /**
     * Shares {@code detect}'s function leaf (the stub function) but has no
     * framework, no family step and no node-level tags: a pure
     * <em>function-tier</em> candidate. It must rank above the
     * same-framework nodes (regression guard for "identical follow-ups for
     * functionally different nodes"): shared functionality outranks a large
     * shared framework.
     */
    @ConfigureJIPipeNode(function = {StubFunction.class})
    public static class SameFunctionNode extends JIPipeGraphNode {
        public SameFunctionNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }
}
