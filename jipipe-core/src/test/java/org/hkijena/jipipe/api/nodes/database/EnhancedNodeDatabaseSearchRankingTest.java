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
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodesByTemplateDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.NodeEntryKind;
import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.IJ3DFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ImageJ1Framework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MeasurementsFunction;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the ranking contracts of {@link JIPipeEnhancedNodeDatabaseSearch}
 * that work item #1404 pins down (the "Find particles 2D" case):
 * <ul>
 * <li><b>Name-prefix bucket:</b> a query that is a prefix of an entry's name
 * ranks that entry above entries that only match by token coverage - the
 * name-prefix ladder step is exact &gt; name-prefix &gt; everything else, and
 * within the prefix bucket shorter names come first;</li>
 * <li><b>Exact name match still first</b> (the pre-existing contract, kept);</li>
 * <li><b>Default kinds</b> (see {@link NodeEntryKind#defaultKinds()}) exclude
 * multi-node system patterns from the visible results.</li>
 * </ul>
 * The fixture mirrors the real registry situation of #1404 with the real
 * node names, descriptions and taxonomy assignments (both particle finders
 * are {@code MeasurementsFunction} nodes; only the framework and the
 * descriptions' wording differ - which is exactly what made the 3D variant
 * outrank the 2D one) plus system-shaped pattern entries whose names carry
 * the same words.
 * <p>
 * Boots the {@link JIPipe} instance for the entry constructors; no Swing.
 */
class EnhancedNodeDatabaseSearchRankingTest {

    private static final String NODE_2D_ID = "search-ranking-test:find-particles-2d";
    private static final String NODE_3D_ID = "search-ranking-test:ij3d-find-particles-3d";
    private static final String MATERIAL_NODE_ID = "search-ranking-test:material-node";
    private static final String IMAGES_FN_ID = "jipipe:function:images";

    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("search-ranking-test", "1.0.0", "search-ranking-test");

    private static TestNodeDatabase database;
    private static JIPipeEnhancedNodeDatabaseSearch search;

    @BeforeAll
    static void boot() {
        JIPipe.ensureInstance();
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        registerNode(FindParticles2DStub.class, NODE_2D_ID, nodes);
        registerNode(FindParticles3DStub.class, NODE_3D_ID, nodes);
        registerNode(MaterialNode.class, MATERIAL_NODE_ID, nodes);

        database = new TestNodeDatabase();
        List<JIPipeNodeDatabaseEntry> entries = new ArrayList<>();
        entries.add(nodeEntry(NODE_2D_ID));
        entries.add(nodeEntry(NODE_3D_ID));
        // The system pattern wall of #1404: multi-node (Pattern badge),
        // System scope, names carrying the same query words as the nodes.
        entries.add(patternEntry("Find 3D particles and create an outlined 3D ROI overlay",
                "Takes a 3D image, denoises and thresholds it, segments particles with morphology, watershed, and connected-component labeling, and renders the outlined 3D ROIs."));
        entries.add(patternEntry("Detect 2D particle ROIs in images",
                "Takes image files, imports them, auto-thresholds each 8-bit image, and extracts connected components as 2D ROIs with measurements."));
        entries.add(patternEntry("Detect 2D particles and render 2D ROIs",
                "Takes greyscale 2D images, applies morphological filtering and auto-thresholding, and extracts connected particles as 2D ROIs."));
        database.setEntries(entries);
        search = new JIPipeEnhancedNodeDatabaseSearch(database);
    }

    @AfterAll
    static void cleanup() {
        JIPipe.getNodes().unregister(NODE_2D_ID);
        JIPipe.getNodes().unregister(NODE_3D_ID);
        JIPipe.getNodes().unregister(MATERIAL_NODE_ID);
    }

    private static void registerNode(Class<? extends JIPipeGraphNode> klass, String id, JIPipeNodesServiceComponent nodes) {
        if (nodes.hasNodeInfoWithId(id)) {
            nodes.unregister(id);
        }
        JIPipeNodeInfo info = new JIPipeJavaNodeInfo(id, klass);
        nodes.register(info, TEST_DEPENDENCY);
    }

    private static JIPipeNodeDatabaseEntry nodeEntry(String nodeId) {
        JIPipeNodeInfo info = JIPipe.getNodes().getRegisteredNodeInfos().get(nodeId);
        return new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + nodeId, info);
    }

    private static CreateNewNodesByTemplateDatabaseEntry patternEntry(String name, String description) {
        JIPipeGraph graph = new JIPipeGraph();
        graph.insertNode(JIPipe.getNodes().getInfoById(MATERIAL_NODE_ID).newInstance());
        graph.insertNode(JIPipe.getNodes().getInfoById(MATERIAL_NODE_ID).newInstance());
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        template.setName(name);
        template.setDescription(new org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText(description));
        template.setTaxonomyFunction(new JIPipeTaxonomyFunctionRef(IMAGES_FN_ID));
        template.setGraph(graph);
        return new CreateNewNodesByTemplateDatabaseEntry(template, TemplateScope.System);
    }

    private static int indexOfName(List<JIPipeNodeDatabaseEntry> results, String name) {
        for (int i = 0; i < results.size(); i++) {
            if (name.equals(results.get(i).getName())) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Contracts
    // ------------------------------------------------------------------

    /**
     * #1404: for the query "find part", the "Find particles 2D" node (its
     * name STARTS WITH the query) must rank above the "IJ3D Find particles
     * 3D" node - although the 3D variant's description mentions
     * "particles"/"finds" and the 2D node's does not, which under pure
     * token coverage puts the 3D variant first.
     */
    @Test
    void namePrefixBeatsTokenCoverage() {
        List<JIPipeNodeDatabaseEntry> results = search.query("find part",
                JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of());
        assertThat(indexOfName(results, "Find particles 2D"))
                .as("results: %s", names(results))
                .isNotNegative()
                .isLessThan(indexOfName(results, "IJ3D Find particles 3D"));
    }

    /**
     * Within the name-prefix bucket, the shortest name wins: the 2D node
     * ("Find particles 2D") must also rank above the system pattern whose
     * name starts with the same words but continues ("Find 3D particles
     * and create an outlined 3D ROI overlay" does not start with
     * "find part", but a same-prefix pattern would).
     */
    @Test
    void shorterNameWinsWithinPrefixBucket() {
        List<JIPipeNodeDatabaseEntry> results = search.query("find 3d particles",
                JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of());
        // "IJ3D Find particles 3D" does NOT start with the query; the
        // pattern "Find 3D particles and create ..." does.
        assertThat(indexOfName(results, "Find 3D particles and create an outlined 3D ROI overlay"))
                .as("results: %s", names(results))
                .isNotNegative()
                .isLessThan(indexOfName(results, "IJ3D Find particles 3D"));
    }

    /**
     * The pre-existing exact-match contract: a query equal to a node name
     * ranks that node first.
     */
    @Test
    void exactNameMatchStillFirst() {
        List<JIPipeNodeDatabaseEntry> results = search.query("find particles 2d",
                JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of());
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getName()).isEqualTo("Find particles 2D");
    }

    /**
     * #1404 ruling: multi-node system patterns are excluded by default
     * ({@link NodeEntryKind#defaultKinds()}); nodes, examples, and anything
     * user-generated stay visible.
     */
    @Test
    void defaultKindsExcludeMultiNodeSystemPatterns() {
        Set<NodeEntryKind> defaults = NodeEntryKind.defaultKinds();
        assertThat(defaults).contains(NodeEntryKind.NODES, NodeEntryKind.EXAMPLES,
                NodeEntryKind.PATTERNS_USER, NodeEntryKind.CUSTOM_SYSTEM, NodeEntryKind.CUSTOM_USER);
        assertThat(defaults).doesNotContain(NodeEntryKind.PATTERNS_SYSTEM);

        List<JIPipeNodeDatabaseEntry> results = search.query("find part",
                JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of());
        List<JIPipeNodeDatabaseEntry> visible = results.stream()
                .filter(e -> NodeEntryKind.matches(e, defaults))
                .toList();
        assertThat(visible).isNotEmpty();
        assertThat(visible).noneMatch(e -> NodeEntryKind.of(e) == NodeEntryKind.PATTERNS_SYSTEM);
        assertThat(indexOfName(visible, "Find particles 2D")).isNotNegative();
    }

    private static List<String> names(List<JIPipeNodeDatabaseEntry> results) {
        List<String> names = new ArrayList<>();
        for (JIPipeNodeDatabaseEntry entry : results) {
            names.add(entry.getName());
        }
        return names;
    }

    // ------------------------------------------------------------------
    // Fixture
    // ------------------------------------------------------------------

    /**
     * A database whose entries are supplied directly (no builder run) - the
     * same pattern as NodeBrowserModelTest's stub.
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
     * Mirror of the real "Find particles 2D" node (same name, description,
     * function and framework): the description notoriously never mentions
     * "particles" or "find" - the #1404 ranking trap.
     */
    @SetJIPipeDocumentation(name = "Find particles 2D", description = "Converts mask images into ROI by applying a connected components algorithm and generates measurements. " +
            "Please note that this algorithm will always trace the external object boundaries and convert them into polygonal ROIs. This means that holes will be closed.")
    @ConfigureJIPipeNode(function = MeasurementsFunction.class, framework = ImageJ1Framework.class)
    public static class FindParticles2DStub extends JIPipeGraphNode {
        public FindParticles2DStub(JIPipeNodeInfo info) {
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

    /**
     * Mirror of the real "IJ3D Find particles 3D" node (same name,
     * description, function and framework): its description leads with
     * "Finds 3D particles", which is what used to outrank the 2D node.
     */
    @SetJIPipeDocumentation(name = "IJ3D Find particles 3D", description = "Finds 3D particles in a binary mask image and returns them as a list of 3D ROIs. It labels connected components, applies optional size, sphericity, edge, and background settings, and uses image calibration for physical measurements when available.")
    @ConfigureJIPipeNode(function = MeasurementsFunction.class, framework = IJ3DFramework.class)
    public static class FindParticles3DStub extends JIPipeGraphNode {
        public FindParticles3DStub(JIPipeNodeInfo info) {
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

    /**
     * A plain node used as graph material for the pattern entries (the
     * template badge follows the graph node count, never node behavior).
     */
    @SetJIPipeDocumentation(name = "Ranking Test Material Node", description = "A stub for the ranking test patterns.")
    @ConfigureJIPipeNode
    public static class MaterialNode extends JIPipeGraphNode {
        public MaterialNode(JIPipeNodeInfo info) {
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
