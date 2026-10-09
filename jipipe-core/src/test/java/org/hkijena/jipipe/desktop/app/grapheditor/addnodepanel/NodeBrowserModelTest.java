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
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
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

    /** Stable id literal for the stub taxonomy function (see the stable-ID rule). */
    private static final String FUNCTION_ID = "node-browser-model-test:function";

    /** Stable id literal for the stub taxonomy framework (see the stable-ID rule). */
    private static final String FRAMEWORK_ID = "node-browser-model-test:framework";

    private static final String TEST_DEPENDENCY_ID = "node-browser-model-test";

    private static final JIPipeMutableDependency TEST_DEPENDENCY =
        new JIPipeMutableDependency(TEST_DEPENDENCY_ID, "1.0.0", "node-browser-model-test");

    private static final String PLANE_NODE_ID = "nodebrowser:plane-node";
    private static final String VOLUME_NODE_ID = "nodebrowser:volume-node";
    private static final String OTHER_NODE_ID = "nodebrowser:other-node";
    private static final String TAG_A = "nodebrowser:tag-a";
    private static final String TAG_B = "nodebrowser:tag-b";
    private static final String TAG_C = "nodebrowser:tag-c";

    /** The stub input-profile provider for {@link NodeBrowserModelTestPlaneData}. */
    private static final InputProfileProvider TEST_PROFILE_PROVIDER =
        dataClass -> dataClass == NodeBrowserModelTestPlaneData.class
            ? JIPipeNodeTaxonomyInputProfile.PLANE_2D
            : JIPipeNodeTaxonomyInputProfile.NA;

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    @AfterEach
    void cleanup() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        nodes.unregister(PLANE_NODE_ID);
        nodes.unregister(VOLUME_NODE_ID);
        nodes.unregister(OTHER_NODE_ID);
        nodes.unregisterInputProfileProviders(List.of(TEST_PROFILE_PROVIDER));
        // Re-run the (idempotent) registration pipeline so that enrichment
        // recomputes over the surviving real nodes only.
        nodes.runRegistrationTasks();
    }

    // ------------------------------------------------------------------
    // Brief case 1: tier boundary
    // ------------------------------------------------------------------

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
    // Brief case 2: search overrides the tree
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

    // ------------------------------------------------------------------
    // Brief case 3: facet combination, no cross-reset
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Brief case 4: the facet post-filter over the synthetic database
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Set<String> originNodeIds(List<JIPipeNodeDatabaseEntry> entries) {
        Set<String> ids = new java.util.LinkedHashSet<>();
        for (JIPipeNodeDatabaseEntry entry : entries) {
            ids.add(entry.getOriginNodeId());
        }
        return ids;
    }

    private static List<String> originNodeIdsList(List<JIPipeNodeDatabaseEntry> entries) {
        List<String> ids = new ArrayList<>();
        for (JIPipeNodeDatabaseEntry entry : entries) {
            ids.add(entry.getOriginNodeId());
        }
        return ids;
    }

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

    private static void registerNode(Class<? extends JIPipeGraphNode> klass, String id, JIPipeNodesServiceComponent nodes) {
        if (nodes.hasNodeInfoWithId(id)) {
            nodes.unregister(id);
        }
        JIPipeNodeInfo info = new JIPipeJavaNodeInfo(id, klass);
        nodes.register(info, TEST_DEPENDENCY);
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
    // Stub taxonomy members
    // ------------------------------------------------------------------

    /** Minimal taxonomy function (self-validating stub, no tags). */
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

    /** Minimal taxonomy framework. */
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
        spatialDimensions = {JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D},
        nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
    @JIPipeNodeTaxonomyTags({TAG_B, TAG_C})
    @AddJIPipeInputSlot(value = NodeBrowserModelTestPlaneData.class, name = "Input", create = true)
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
        spatialDimensions = {JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D},
        nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
    @JIPipeNodeTaxonomyTags(TAG_A)
    public static class VolumeNode extends JIPipeGraphNode {        public VolumeNode(JIPipeNodeInfo info) {
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
}
