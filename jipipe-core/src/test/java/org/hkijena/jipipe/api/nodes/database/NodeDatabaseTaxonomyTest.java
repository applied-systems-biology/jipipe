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
import org.hkijena.jipipe.api.nodes.AddJIPipeNodeAlias;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeNodeTaxonomyTags;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewCompartmentNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoAliasDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamilyStepAnnotation;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.families.TrackMateFamily;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the taxonomy facets of the node database ({@link NodeDatabaseTaxonomyTest}):
 * the five new {@link JIPipeNodeDatabaseEntry} facet getters populated from
 * {@link JIPipeNodeInfo} in the "create node by info" entries, and the
 * {@link JIPipeNodeDatabase#followUps(JIPipeNodeDatabaseEntry) follow-ups} /
 * {@link JIPipeNodeDatabase#related(JIPipeNodeDatabaseEntry, int) related}
 * queries backed by the {@code FamilyAssembler}.
 * <p>
 * The synthetic nodes are three steps of {@link TrackMateFamily}
 * (detect → track → filter), plus same-framework, shared-tag,
 * auto-import, and tagless siblings. The database is built directly from
 * {@link JIPipeNodeInfo}s via
 * {@link CreateNewNodeByInfoDatabaseEntry}/{@link CreateNewNodeByInfoAliasDatabaseEntry}
 * and {@link JIPipeNodeDatabase#setEntries(List)}, bypassing the full
 * {@code JIPipeNodeDatabaseBuilderRun} (whose global entry cache would
 * otherwise leak synthetic entries into later tests); the entry
 * constructors are the production population path for the facets.
 */
class NodeDatabaseTaxonomyTest {

    /** Stable id literal for the stub taxonomy function (see the stable-ID rule). */
    private static final String FUNCTION_ID = "node-db-taxonomy-test:function";

    /** Stable id literal for the stub taxonomy framework (see the stable-ID rule). */
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
    private static final String SPOT_DETECT_ALIAS_ID = SPOT_DETECT_ID + ":alias-0";

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
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
        // The TrackMate steps assembled into the shared FamilyAssembler by
        // the three synthetic family nodes. TrackMateFamily is a
        // production family class (kept), but the resolver's
        // id→family registry only ever learns about it through node step
        // annotations, and no jipipe-core node carries TrackMate steps —
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

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    /**
     * The "create node by info" entry carries the taxonomy facets of its
     * node info (the production population path), and the legacy
     * {@link JIPipeNodeDatabaseEntry#getCategoryIds()} is still populated.
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

        // Legacy category facet is kept (Task 27 removes it)
        assertTrue(entry.getCategoryIds().contains(new MiscellaneousNodeTypeCategory().getId()),
            "legacy category ids must still be populated, got: " + entry.getCategoryIds());
    }

    /**
     * The "create node by info alias" entry carries the same facets as the
     * base entry (same node, same taxonomy), and both entries expose the
     * origin node id; entries that do not wrap a node type expose {@code null}.
     */
    @Test
    void aliasEntryCarriesSameFacetsAndOriginNodeId() {
        registerNodes();

        JIPipeNodeInfo detectInfo = JIPipe.getNodes().getRegisteredNodeInfos().get(SPOT_DETECT_ID);
        CreateNewNodeByInfoDatabaseEntry base =
            new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + SPOT_DETECT_ID, detectInfo);
        CreateNewNodeByInfoAliasDatabaseEntry alias =
            new CreateNewNodeByInfoAliasDatabaseEntry(SPOT_DETECT_ALIAS_ID, detectInfo, detectInfo.getAliases().get(0));

        assertEquals(SPOT_DETECT_ID, base.getOriginNodeId());
        assertEquals(SPOT_DETECT_ID, alias.getOriginNodeId());
        assertEquals(base.getFunctionIds(), alias.getFunctionIds());
        assertEquals(base.getFramework().getId(), alias.getFramework().getId());
        assertEquals(base.getSpatialDimensions(), alias.getSpatialDimensions());
        assertEquals(base.getInputProfile(), alias.getInputProfile());
        assertEquals(base.getTags(), alias.getTags());

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
     * {@link JIPipeNodeDatabase#followUps(JIPipeNodeDatabaseEntry)} returns
     * the direct family successors of an entry, mapped back to entries, in
     * assembler order; an entry whose node has no successors (or whose
     * origin is unknown) yields an empty list.
     */
    @Test
    void followUpsReturnsFamilySuccessorsMappedToEntries() {
        registerNodes();
        JIPipeNodeDatabase database = databaseWithAllEntries();
        CreateNewNodeByInfoDatabaseEntry detect = byInfo(database, SPOT_DETECT_ID);
        CreateNewNodeByInfoDatabaseEntry track = byInfo(database, TRACK_ID);
        CreateNewNodeByInfoDatabaseEntry filter = byInfo(database, FILTER_ID);

        List<JIPipeNodeDatabaseEntry> followUps = database.followUps(detect);
        assertEquals(List.of(track), followUps);
        assertEquals(List.of(filter), database.followUps(track));

        assertEquals(List.of(), database.followUps(filter));
        assertEquals(List.of(), database.followUps(byInfo(database, NO_TAGS_ID)));
    }

    /**
     * {@link JIPipeNodeDatabase#related(JIPipeNodeDatabaseEntry, int)} ranks
     * candidates in tiers: same-family siblings (excluding follow-ups)
     * first, then same-framework, then shared-tag (≥2 shared before 1),
     * de-duplicated and capped, with auto-import entries down-ranked to the
     * end of their tier.
     * <p>
     * Expected order for {@code related(detect, 10)} with the synthetic set:
     * <ol>
     * <li>{@code filter} — the only family sibling that is not a follow-up
     * (the sibling {@code track} is a follow-up and is excluded from this
     * tier);</li>
     * <li>{@code track} — qualifies via the shared framework (tier 2);
     * being a follow-up only removes it from the sibling tier;</li>
     * <li>{@code otherFramework} — tier 2, standard classification;</li>
     * <li>{@code autoImport} — tier 2 candidate (shared framework) that is
     * auto-import classified, hence down-ranked to the end of the tier; it
     * is de-duplicated out of the tag tier where it would also qualify;</li>
     * <li>{@code sharedTagOne} — two shared tags;</li>
     * <li>{@code sharedTagTwo} — exactly one shared tag.</li>
     * </ol>
     * {@code noTags} qualifies for no tier and must be absent.
     */
    @Test
    void relatedRanksFamilyThenFrameworkThenTags() {
        registerNodes();
        JIPipeNodeDatabase database = databaseWithAllEntries();
        CreateNewNodeByInfoDatabaseEntry detect = byInfo(database, SPOT_DETECT_ID);
        CreateNewNodeByInfoDatabaseEntry track = byInfo(database, TRACK_ID);
        CreateNewNodeByInfoDatabaseEntry filter = byInfo(database, FILTER_ID);
        CreateNewNodeByInfoDatabaseEntry otherFramework = byInfo(database, OTHER_FRAMEWORK_ID);
        CreateNewNodeByInfoDatabaseEntry sharedTag1 = byInfo(database, SHARED_TAG_1);
        CreateNewNodeByInfoDatabaseEntry sharedTag2 = byInfo(database, SHARED_TAG_2);
        CreateNewNodeByInfoDatabaseEntry autoImport = byInfo(database, AUTO_IMPORT_ID);

        List<JIPipeNodeDatabaseEntry> expected = List.of(
            filter, track, otherFramework, autoImport, sharedTag1, sharedTag2);
        assertEquals(expected, database.related(detect, 10));

        // Cap
        assertEquals(List.of(filter, track), database.related(detect, 2));
    }

    /**
     * A second {@code related} call returns the same cached list instance;
     * the cache is invalidated when the entries are replaced, after which
     * a fresh (differently ordered) result is computed.
     */
    @Test
    void relatedResultIsCachedUntilEntriesAreReplaced() {
        registerNodes();
        JIPipeNodeDatabase database = databaseWithAllEntries();
        CreateNewNodeByInfoDatabaseEntry detect = byInfo(database, SPOT_DETECT_ID);
        CreateNewNodeByInfoDatabaseEntry track = byInfo(database, TRACK_ID);
        CreateNewNodeByInfoDatabaseEntry otherFramework = byInfo(database, OTHER_FRAMEWORK_ID);

        List<JIPipeNodeDatabaseEntry> first = database.related(detect, 10);
        assertSame(first, database.related(detect, 10), "related() must be cached per entry");

        // Replacing the entries (e.g. the builder run) invalidates the
        // cache; in the new entry set the family sibling (filter) is
        // gone, so the framework candidate becomes the first result
        List<JIPipeNodeDatabaseEntry> reordered = List.of(
            otherFramework, detect, track);
        database.setEntries(reordered);
        List<JIPipeNodeDatabaseEntry> second = database.related(detect, 10);
        assertNotSame(first, second, "replacing the entries must invalidate the related cache");
        assertEquals(otherFramework, second.get(0), "the related order must reflect the new entry set");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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
    }

    private static void registerNode(Class<? extends JIPipeGraphNode> nodeClass, String id, JIPipeNodesServiceComponent nodes) {
        if (nodes.hasNodeInfoWithId(id)) {
            nodes.unregister(id);
        }
        JIPipeNodeInfo info = new JIPipeJavaNodeInfo(id, nodeClass);
        nodes.register(info, TEST_DEPENDENCY);
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

    /**
     * Builds a fresh database holding only the entries created here: one
     * "create node by info" entry per synthetic node, plus one alias entry
     * for the detect node.
     */
    private static JIPipeNodeDatabase databaseWithAllEntries() {
        JIPipeNodeDatabase database = new TestNodeDatabase();
        List<JIPipeNodeDatabaseEntry> entries = new java.util.ArrayList<>();
        for (String id : List.of(SPOT_DETECT_ID, TRACK_ID, FILTER_ID, OTHER_FRAMEWORK_ID,
            SHARED_TAG_1, SHARED_TAG_2, AUTO_IMPORT_ID, NO_TAGS_ID)) {
            JIPipeNodeInfo info = JIPipe.getNodes().getRegisteredNodeInfos().get(id);
            entries.add(new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + id, info));
        }
        JIPipeNodeInfo detectInfo = JIPipe.getNodes().getRegisteredNodeInfos().get(SPOT_DETECT_ID);
        entries.add(new CreateNewNodeByInfoAliasDatabaseEntry(SPOT_DETECT_ALIAS_ID, detectInfo, detectInfo.getAliases().get(0)));
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

    // ------------------------------------------------------------------
    // Stub taxonomy members
    // ------------------------------------------------------------------

    /** Minimal taxonomy function carrying one tag (self-validating stub). */
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

    /** Minimal taxonomy framework. */
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
     * the stub framework, 3D spatial dimension, an alias, and the
     * {@code after = {} start step annotation.
     */
    @ConfigureJIPipeNode(function = {StubFunction.class}, framework = StubFramework.class,
        spatialDimensions = {JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D},
        nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
    @JIPipeNodeTaxonomyTags("detect")
    @AddJIPipeNodeAlias(nodeTypeCategory = MiscellaneousNodeTypeCategory.class,
        menuPath = "alias/detect", aliasName = "Spot Detector")
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
    @ConfigureJIPipeNode(function = {StubFunction.class}, framework = StubFramework.class,
        nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
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
    @ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
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
    @ConfigureJIPipeNode(framework = StubFramework.class, nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
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
    @ConfigureJIPipeNode(function = {StubNoTagFunction.class},
        nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
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
    @ConfigureJIPipeNode(function = {StubNoTagFunction.class},
        nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
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
        nodeTypeCategory = MiscellaneousNodeTypeCategory.class,
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

    /** No framework, no family step, no tags: excluded from related(). */
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
}
