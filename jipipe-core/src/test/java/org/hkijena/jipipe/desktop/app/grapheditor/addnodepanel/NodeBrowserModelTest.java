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

package org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeNodeTaxonomyTags;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.*;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeNodeStateChips;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests of {@link NodeBrowserModel}, the non-Swing state behind the
 * adaptive node browser (Task 16).
 * <p>
 * The first three cases are pure model logic (no {@code JIPipe} instance
 * needed). The {@code filterQuery} case exercises the taxonomy facet
 * post-filter of {@link JIPipeNodeDatabase} over a fresh synthetic
 * database: stub node classes carrying
 * {@link ConfigureJIPipeNode function/framework/spatialDimensions} and
 * {@link JIPipeNodeTaxonomyTags}, the same pattern as
 * {@code NodeDatabaseTaxonomyTest}.
 */
class NodeBrowserModelTest {

    /**
     * Stable id literal for the stub taxonomy function (see the stable-ID rule).
     */
    private static final String FUNCTION_ID = "node-browser-model-test:function";

    /**
     * Stable id literal for a stub function that is visible in the
     * compartments editor but NOT in the pipeline (used to exercise the
     * visibility-role filter of {@code populatedFunctionIds}).
     */
    private static final String COMP_FUNCTION_ID = "node-browser-model-test:comp-function";

    /**
     * Stable id literal for the stub taxonomy framework (see the stable-ID rule).
     */
    private static final String FRAMEWORK_ID = "node-browser-model-test:framework";

    private static final String TEST_DEPENDENCY_ID = "node-browser-model-test";

    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency(TEST_DEPENDENCY_ID, "1.0.0", "node-browser-model-test");

    private static final String PLANE_NODE_ID = "nodebrowser:plane-node";
    private static final String VOLUME_NODE_ID = "nodebrowser:volume-node";
    private static final String OTHER_NODE_ID = "nodebrowser:other-node";
    private static final String COMP_NODE_ID = "nodebrowser:comp-node";
    private static final String UNSTABLE_NODE_ID = "nodebrowser:unstable-node";
    private static final String DEPRECATED_NODE_ID = "nodebrowser:deprecated-node";
    private static final String AUTOIMPORT_NODE_ID = "nodebrowser:autoimport-node";
    private static final String TAG_A = "nodebrowser:tag-a";
    private static final String TAG_B = "nodebrowser:tag-b";
    private static final String TAG_C = "nodebrowser:tag-c";

    /**
     * The stub input-profile provider for {@link NodeBrowserModelTestPlaneData}.
     */
    private static final InputProfileProvider TEST_PROFILE_PROVIDER =
            dataClass -> dataClass == NodeBrowserModelTestPlaneData.class
                    ? JIPipeNodeTaxonomyInputProfile.PLANE_2D
                    : JIPipeNodeTaxonomyInputProfile.NA;

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static JIPipeNodeDatabaseEntry entryOf(String nodeId) {
        JIPipeNodeInfo info = JIPipe.getNodes().getRegisteredNodeInfos().get(nodeId);
        return new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + nodeId, info);
    }

    // ------------------------------------------------------------------
    // Brief case 1: tier boundary
    // ------------------------------------------------------------------

    private static Set<String> originNodeIds(List<JIPipeNodeDatabaseEntry> entries) {
        Set<String> ids = new java.util.LinkedHashSet<>();
        for (JIPipeNodeDatabaseEntry entry : entries) {
            ids.add(entry.getOriginNodeId());
        }
        return ids;
    }

    // ------------------------------------------------------------------
    // Brief case 2: search overrides the tree
    // ------------------------------------------------------------------

    private static List<String> originNodeIdsList(List<JIPipeNodeDatabaseEntry> entries) {
        List<String> ids = new ArrayList<>();
        for (JIPipeNodeDatabaseEntry entry : entries) {
            ids.add(entry.getOriginNodeId());
        }
        return ids;
    }

    // ------------------------------------------------------------------
    // Brief case 3: facet combination, no cross-reset
    // ------------------------------------------------------------------

    private static void registerNodes() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        // The stub provider must be registered before the nodes: the input
        // profile enrichment (which derives PLANE_2D for the plane node's
        // first input slot) happens during registration.
        nodes.registerInputProfileProvider(TEST_PROFILE_PROVIDER);
        registerNode(PlaneNode.class, PLANE_NODE_ID, nodes);
        registerNode(VolumeNode.class, VOLUME_NODE_ID, nodes);
        registerNode(OtherNode.class, OTHER_NODE_ID, nodes);
    }

    // ------------------------------------------------------------------
    // Brief case 4: the facet post-filter over the synthetic database
    // ------------------------------------------------------------------

    private static void registerNode(Class<? extends JIPipeGraphNode> klass, String id, JIPipeNodesServiceComponent nodes) {
        if (nodes.hasNodeInfoWithId(id)) {
            nodes.unregister(id);
        }
        JIPipeNodeInfo info = new JIPipeJavaNodeInfo(id, klass);
        nodes.register(info, TEST_DEPENDENCY);
    }

    // ------------------------------------------------------------------
    // Data-type facet (input / output) — merged "Data" filter
    // ------------------------------------------------------------------

    /**
     * Builds a fresh database holding one "create node by info" entry per
     * synthetic node.
     */
    private static JIPipeNodeDatabase databaseWithAllEntries() {
        JIPipeNodeDatabase database = new TestNodeDatabase();
        List<JIPipeNodeDatabaseEntry> entries = new ArrayList<>();
        for (String nodeId : List.of(PLANE_NODE_ID, VOLUME_NODE_ID, OTHER_NODE_ID)) {
            JIPipeNodeInfo info = JIPipe.getNodes().getRegisteredNodeInfos().get(nodeId);
            entries.add(new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + nodeId, info));
        }
        database.setEntries(entries);
        return database;
    }

    // ------------------------------------------------------------------
    // Node state: deprecated / unstable / auto-imported (order + labels)
    // ------------------------------------------------------------------

    @AfterEach
    void cleanup() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        nodes.unregister(PLANE_NODE_ID);
        nodes.unregister(VOLUME_NODE_ID);
        nodes.unregister(OTHER_NODE_ID);
        nodes.unregister(COMP_NODE_ID);
        nodes.unregister(UNSTABLE_NODE_ID);
        nodes.unregister(DEPRECATED_NODE_ID);
        nodes.unregister(AUTOIMPORT_NODE_ID);
        nodes.unregisterInputProfileProviders(List.of(TEST_PROFILE_PROVIDER));
        // Re-run the (idempotent) registration pipeline so that enrichment
        // recomputes over the surviving real nodes only.
        nodes.runRegistrationTasks();
    }

    @Test
    void tierBoundary() {
        NodeBrowserModel model = new NodeBrowserModel();
        assertEquals(NodeBrowserModel.TIER_COMPACT, model.getTier(379));
        assertEquals(NodeBrowserModel.TIER_COMFORTABLE, model.getTier(380));
        // The tier is a pure function of the width
        assertEquals(NodeBrowserModel.TIER_COMPACT, model.getTier(0));
        assertEquals(NodeBrowserModel.TIER_COMFORTABLE, model.getTier(2000));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    @Test
    void searchOverridesTree() {
        NodeBrowserModel model = new NodeBrowserModel();

        // Empty search + a tree path: tree browsing mode
        model.setTreePath(List.of());
        model.setSearch("");
        assertFalse(model.isSearchActive());
        assertTrue(model.getTreePath().isEmpty());

        // A non-empty search flips into flat-results mode, overriding the
        // tree
        model.setSearch("threshold");
        assertTrue(model.isSearchActive());
        assertEquals("threshold", model.getSearch());

        // Back to empty: tree mode again (the tree path is not lost)
        model.setSearch("");
        assertFalse(model.isSearchActive());
    }

    @Test
    void facetCombination() {
        NodeBrowserModel model = new NodeBrowserModel();
        StubFramework framework = new StubFramework();
        model.setFramework(framework);
        model.setSpatial(Set.of(JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D));
        model.setTags(Set.of("alpha"));

        // All three are stored
        assertEquals(framework, model.getFramework());
        assertEquals(Set.of(JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D), model.getSpatial());
        assertEquals(Set.of("alpha"), model.getTags());

        // Clearing one leaves the others (no cross-reset)
        model.setFramework(null);
        assertNull(model.getFramework());
        assertEquals(Set.of(JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D), model.getSpatial());
        assertEquals(Set.of("alpha"), model.getTags());

        model.setSpatial(Set.of());
        model.setTags(Set.of());
        assertNull(model.getFramework());
        assertTrue(model.getSpatial().isEmpty());
        assertTrue(model.getTags().isEmpty());
    }

    @Test
    void filterQuery() {
        registerNodes();
        JIPipeNodeDatabase database = databaseWithAllEntries();

        // Framework filter returns only that framework's nodes
        assertEquals(Set.of(PLANE_NODE_ID, VOLUME_NODE_ID),
                originNodeIds(database.queryWithFacets("",
                        JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of(),
                        new StubFramework(), Set.of(), Set.of(), Set.of())));

        // Spatial SPATIAL_3D returns the nodes declaring it; SPATIAL_2D the others
        assertEquals(Set.of(VOLUME_NODE_ID),
                originNodeIds(database.queryWithFacets("",
                        JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of(),
                        null, Set.of(JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D), Set.of(), Set.of())));
        assertEquals(Set.of(PLANE_NODE_ID),
                originNodeIds(database.queryWithFacets("",
                        JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of(),
                        null, Set.of(JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D), Set.of(), Set.of())));

        // Input PLANE_2D returns the nodes with that derived profile
        assertEquals(Set.of(PLANE_NODE_ID),
                originNodeIds(database.queryWithFacets("",
                        JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of(),
                        null, Set.of(), Set.of(JIPipeNodeTaxonomyInputProfile.PLANE_2D), Set.of())));

        // Tags intersect. The unconverted OtherNode's node-level tags are
        // not merged (tag enrichment only runs for converted nodes), so
        // only the converted VolumeNode carries tag-a.
        assertEquals(Set.of(VOLUME_NODE_ID),
                originNodeIds(database.queryWithFacets("",
                        JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of(),
                        null, Set.of(), Set.of(), Set.of(TAG_A))));
        assertEquals(Set.of(PLANE_NODE_ID),
                originNodeIds(database.queryWithFacets("",
                        JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of(),
                        null, Set.of(), Set.of(), Set.of(TAG_B))));
        assertEquals(Set.of(PLANE_NODE_ID),
                originNodeIds(database.queryWithFacets("",
                        JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of(),
                        null, Set.of(), Set.of(), Set.of(TAG_C))));

        // Combined facets: framework + spatial + tags must all hold
        assertEquals(Set.of(VOLUME_NODE_ID),
                originNodeIds(database.queryWithFacets("",
                        JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of(),
                        new StubFramework(), Set.of(JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D),
                        Set.of(), Set.of(TAG_A))));

        // No facets: the plain query result (all three, name-sorted)
        assertEquals(Set.of(PLANE_NODE_ID, VOLUME_NODE_ID, OTHER_NODE_ID),
                originNodeIds(database.query("",
                        JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of())));
        // Name-sorted: "Browser Test" < "Browser Test Plane" < "Browser Test Volume"
        assertEquals(List.of(OTHER_NODE_ID, PLANE_NODE_ID, VOLUME_NODE_ID),
                originNodeIdsList(database.queryWithFacets("",
                        JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of(),
                        null, Set.of(), Set.of(), Set.of())));
    }

    @Test
    void dataTypeFacets() {
        registerNodes();
        NodeBrowserModel model = new NodeBrowserModel();
        // PlaneNode has an input AND an output of NodeBrowserModelTestPlaneData;
        // VolumeNode/OtherNode have no slots at all.
        JIPipeNodeDatabaseEntry plane = new CreateNewNodeByInfoDatabaseEntry(
                "create-node-by-info:" + PLANE_NODE_ID,
                JIPipe.getNodes().getRegisteredNodeInfos().get(PLANE_NODE_ID));
        JIPipeNodeDatabaseEntry volume = new CreateNewNodeByInfoDatabaseEntry(
                "create-node-by-info:" + VOLUME_NODE_ID,
                JIPipe.getNodes().getRegisteredNodeInfos().get(VOLUME_NODE_ID));
        JIPipeNodeDatabaseEntry other = new CreateNewNodeByInfoDatabaseEntry(
                "create-node-by-info:" + OTHER_NODE_ID,
                JIPipe.getNodes().getRegisteredNodeInfos().get(OTHER_NODE_ID));

        // No data-type filter: all pass
        assertTrue(model.matches(plane));
        assertTrue(model.matches(volume));
        assertTrue(model.matches(other));

        // Input filter: only the node with a matching input slot passes
        model.setInputDataTypes(Set.of(NodeBrowserModelTestPlaneData.class));
        assertTrue(model.matches(plane));
        assertFalse(model.matches(volume));
        assertFalse(model.matches(other));

        // Clearing the input filter restores the pass-through
        model.setInputDataTypes(Set.of());
        assertTrue(model.matches(volume));

        // Output filter: symmetric
        model.setOutputDataTypes(Set.of(NodeBrowserModelTestPlaneData.class));
        assertTrue(model.matches(plane));
        assertFalse(model.matches(volume));

        // Combined: the node must match BOTH
        model.setInputDataTypes(Set.of(NodeBrowserModelTestPlaneData.class));
        assertTrue(model.matches(plane));
        assertFalse(model.matches(volume));

        // No cross-reset: setting output left the input filter in place
        model.setOutputDataTypes(Set.of());
        assertEquals(Set.of(NodeBrowserModelTestPlaneData.class), model.getInputDataTypes());
        assertTrue(model.matches(plane));
        assertFalse(model.matches(volume));
    }

    @Test
    void nodeStateClassificationAndDownRanking() {
        registerNodes();
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        registerNode(UnstableNode.class, UNSTABLE_NODE_ID, nodes);
        registerNode(DeprecatedNode.class, DEPRECATED_NODE_ID, nodes);
        registerNode(AutoImportNode.class, AUTOIMPORT_NODE_ID, nodes);

        // The entry mirrors the node info's state flags
        JIPipeNodeDatabaseEntry stable = entryOf(PLANE_NODE_ID);
        JIPipeNodeDatabaseEntry unstable = entryOf(UNSTABLE_NODE_ID);
        JIPipeNodeDatabaseEntry deprecated = entryOf(DEPRECATED_NODE_ID);
        JIPipeNodeDatabaseEntry autoImport = entryOf(AUTOIMPORT_NODE_ID);
        assertFalse(stable.isUnstable());
        assertFalse(stable.isDeprecated());
        assertTrue(unstable.isUnstable());
        assertFalse(unstable.isDeprecated());
        assertTrue(deprecated.isDeprecated());
        assertFalse(deprecated.isUnstable());
        assertEquals(JIPipeNodeClassification.AutoImport, autoImport.getNodeClassification());

        // isDeprecatedOrUnstable must cover BOTH flags (the name-suffix and
        // search-penalty consumers rely on it)
        assertFalse(stable.isDeprecatedOrUnstable());
        assertTrue(unstable.isDeprecatedOrUnstable());
        assertTrue(deprecated.isDeprecatedOrUnstable());

        // The generalized down-ranking rule: standard curated nodes rank
        // first; auto-imported, unstable and deprecated nodes rank after
        // them — no node-id or plugin heuristics involved
        assertFalse(JIPipeNodeStateChips.isDownRanked(stable));
        assertTrue(JIPipeNodeStateChips.isDownRanked(unstable));
        assertTrue(JIPipeNodeStateChips.isDownRanked(deprecated));
        assertTrue(JIPipeNodeStateChips.isDownRanked(autoImport));
    }

    /**
     * The function-tree pruning support ({@link
     * NodeBrowserModel#populatedFunctionIds}) returns the union of the
     * nodes' function ids (primary + secondary), filters by the visibility
     * role (a function visible only in the other editor is not counted),
     * and is safe on null input.
     */
    @Test
    void populatedFunctionIdsUnionsNodeFunctionsAndFiltersByRole() {
        // null -> empty set (defensive; callers keep the full skeleton)
        assertTrue(NodeBrowserModel.populatedFunctionIds(null,
                JIPipeNodeDatabasePipelineVisibility.Pipeline).isEmpty());

        registerNodes();
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        // Only the three registered stubs (PlaneNode + VolumeNode declare the
        // visible stub function, OtherNode has no @ConfigureJIPipeNode at all
        // (primary null) and contributes nothing). Scoping to the stub ids
        // mirrors databaseWithAllEntries() and keeps this independent of the
        // rest of the registered plugin set.
        List<JIPipeNodeInfo> stubInfos = List.of(PLANE_NODE_ID, VOLUME_NODE_ID, OTHER_NODE_ID)
                .stream().map(nodes::getInfoById).toList();
        assertEquals(Set.of(FUNCTION_ID),
                NodeBrowserModel.populatedFunctionIds(stubInfos,
                        JIPipeNodeDatabasePipelineVisibility.Pipeline));
        assertEquals(Set.of(FUNCTION_ID),
                NodeBrowserModel.populatedFunctionIds(stubInfos,
                        JIPipeNodeDatabasePipelineVisibility.Compartments));

        // A node whose primary function is pipeline-hidden must not count
        // toward the Pipeline role, but must for Compartments.
        nodes.register(new JIPipeJavaNodeInfo(COMP_NODE_ID, CompNode.class), TEST_DEPENDENCY);
        try {
            List<JIPipeNodeInfo> withComp = List.of(PLANE_NODE_ID, VOLUME_NODE_ID, OTHER_NODE_ID, COMP_NODE_ID)
                    .stream().map(nodes::getInfoById).toList();
            assertEquals(Set.of(FUNCTION_ID),
                    NodeBrowserModel.populatedFunctionIds(withComp,
                            JIPipeNodeDatabasePipelineVisibility.Pipeline));
            assertEquals(Set.of(FUNCTION_ID, COMP_FUNCTION_ID),
                    NodeBrowserModel.populatedFunctionIds(withComp,
                            JIPipeNodeDatabasePipelineVisibility.Compartments));
        } finally {
            nodes.unregister(COMP_NODE_ID);
        }
    }

    /**
     * A node database that never schedules its async builder run: the
     * constructor invokes the virtual {@link #rebuildImmediately()}, which
     * is overridden here to a no-op. The test builds its entries manually
     * via {@link JIPipeNodeDatabase#setEntries(List)}, which must not be
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
     * Minimal taxonomy function (self-validating stub, no tags).
     */
    public static class StubFunction implements JIPipeNodeTaxonomyFunction {
        public StubFunction() {
        }

        @Override
        public String getId() {
            return FUNCTION_ID;
        }

        @Override
        public String getName() {
            return "Node Browser Test Function";
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
     * A taxonomy function that is visible in the compartments editor but NOT
     * in the pipeline: the role-filter half of
     * {@link NodeBrowserModel#populatedFunctionIds}.
     */
    public static class StubCompFunction implements JIPipeNodeTaxonomyFunction {
        public StubCompFunction() {
        }

        @Override
        public String getId() {
            return COMP_FUNCTION_ID;
        }

        @Override
        public String getName() {
            return "Node Browser Test Comp Function";
        }

        @Override
        public JIPipeNodeTaxonomyFunction getParent() {
            return null;
        }

        @Override
        public Float getColorHue() {
            return 0.21f;
        }

        @Override
        public String getDescription() {
            return "Test stub function, compartments only";
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
            return false;
        }

        @Override
        public boolean isVisibleInCompartments() {
            return true;
        }
    }

    /**
     * Minimal taxonomy framework.
     */
    public static class StubFramework implements JIPipeNodeTaxonomyFramework {
        public StubFramework() {
        }

        @Override
        public String getId() {
            return FRAMEWORK_ID;
        }

        @Override
        public String getName() {
            return "Node Browser Test Framework";
        }

        @Override
        public String getAbbreviation() {
            return "NBM";
        }

        @Override
        public String getPatternStyleId() {
            return "initials";
        }
    }

    /**
     * 2D node: stub function + stub framework, 2D spatial dimension,
     * a data input slot of {@link NodeBrowserModelTestPlaneData} (so that
     * the stub profile provider derives {@code PLANE_2D}), tags
     * {@code tag-b} and {@code tag-c}.
     */
    @ConfigureJIPipeNode(function = {StubFunction.class}, framework = StubFramework.class,
            spatialDimensions = {JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D})
    @JIPipeNodeTaxonomyTags({TAG_B, TAG_C})
    @AddJIPipeInputSlot(value = NodeBrowserModelTestPlaneData.class, name = "Input", create = true)
    @AddJIPipeOutputSlot(value = NodeBrowserModelTestPlaneData.class, name = "Output")
    public static class PlaneNode extends JIPipeGraphNode {
        public PlaneNode(JIPipeNodeInfo info) {
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
     * 3D node: same function and framework, 3D spatial dimension, no input
     * slots (input profile {@code NA}), tag {@code tag-a}.
     */
    @ConfigureJIPipeNode(function = {StubFunction.class}, framework = StubFramework.class,
            spatialDimensions = {JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D})
    @JIPipeNodeTaxonomyTags(TAG_A)
    public static class VolumeNode extends JIPipeGraphNode {
        public VolumeNode(JIPipeNodeInfo info) {
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
     * No-framework node: no function, no spatial dimensions, no input slots
     * (input profile {@code NA}), tag {@code tag-a}.
     */
    @JIPipeNodeTaxonomyTags(TAG_A)
    public static class OtherNode extends JIPipeGraphNode {
        public OtherNode(JIPipeNodeInfo info) {
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
     * Node whose primary function is compartments-only (not pipeline-visible):
     * exercises the role filter of {@code populatedFunctionIds}.
     */
    @ConfigureJIPipeNode(function = {StubCompFunction.class}, framework = StubFramework.class)
    public static class CompNode extends JIPipeGraphNode {
        public CompNode(JIPipeNodeInfo info) {
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
     * Unstable node: {@code @MarkNodeAsUnstable} (the node-info flag source).
     */
    @MarkNodeAsUnstable
    public static class UnstableNode extends JIPipeGraphNode {
        public UnstableNode(JIPipeNodeInfo info) {
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
     * Deprecated node: {@code @Deprecated} (the node-info flag source).
     */
    @Deprecated
    public static class DeprecatedNode extends JIPipeGraphNode {
        public DeprecatedNode(JIPipeNodeInfo info) {
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
     * Auto-imported node: {@code nodeClassification = AutoImport} (the same
     * classification the IJ2/CLIJ node infos report).
     */
    @ConfigureJIPipeNode(nodeClassification = JIPipeNodeClassification.AutoImport)
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
}
