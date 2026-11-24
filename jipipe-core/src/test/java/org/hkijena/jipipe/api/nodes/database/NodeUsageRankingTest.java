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
import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the usage-aware ranking contracts of the enhanced search
 * (#1404): the bounded usage bonus flips near-ties inside the
 * coverage-scored tier, usage never crosses the exact/name-prefix ladder,
 * entries without an origin node id (templates) take no bonus and never
 * throw, and the coverage bucket is rescaled by the name+token field
 * maximum. The prior is INJECTED (synthetic maps) - no corpus, no
 * settings, no shipped resource involved.
 */
class NodeUsageRankingTest {

    private static final String WIDE_ID = "usage-ranking-test:extract-blobs-wide";
    private static final String THIN_ID = "usage-ranking-test:extract-blobs-thin";
    private static final String USED_WORSE_NAME_ID = "usage-ranking-test:echo-delayed";
    private static final String UNUSED_BETTER_NAME_ID = "usage-ranking-test:echo-wide";
    private static final String FROM_MASK_ID = "usage-ranking-test:extract-blobs-from-mask";
    private static final String MAKE_ID = "usage-ranking-test:slow-extract";
    private static final String MATERIAL_ID = "usage-ranking-test:material-node";
    private static final String IMAGES_FN_ID = "jipipe:function:images";

    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("usage-ranking-test", "1.0.0", "usage-ranking-test");

    private static TestNodeDatabase database;
    private static JIPipeEnhancedNodeDatabaseSearch search;

    @BeforeAll
    static void boot() {
        JIPipe.ensureInstance();
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        registerNode(ExtractBlobsWideStub.class, WIDE_ID, nodes);
        registerNode(ExtractBlobsThinStub.class, THIN_ID, nodes);
        registerNode(DelayedExtractBlobsStub.class, USED_WORSE_NAME_ID, nodes);
        registerNode(WideExtractBlobsStub.class, UNUSED_BETTER_NAME_ID, nodes);
        registerNode(ExtractBlobsFromMaskStub.class, FROM_MASK_ID, nodes);
        registerNode(SlowExtractBlobsStub.class, MAKE_ID, nodes);
        registerNode(MaterialNode.class, MATERIAL_ID, nodes);

        database = new TestNodeDatabase();
        List<JIPipeNodeDatabaseEntry> entries = new ArrayList<>();
        // THIN is registered FIRST: with a stable sort and no usage signal,
        // the near-tie test would pass accidentally - the RED state must
        // show the missing bonus/tie-break.
        entries.add(nodeEntry(THIN_ID));
        entries.add(nodeEntry(WIDE_ID));
        entries.add(nodeEntry(UNUSED_BETTER_NAME_ID));
        entries.add(nodeEntry(USED_WORSE_NAME_ID));
        entries.add(nodeEntry(FROM_MASK_ID));
        entries.add(nodeEntry(MAKE_ID));
        entries.add(patternEntry("Extract blobs workflow", 2));
        database.setEntries(entries);
        search = new JIPipeEnhancedNodeDatabaseSearch(database);
    }

    @AfterAll
    static void cleanup() {
        for (String id : List.of(WIDE_ID, THIN_ID, USED_WORSE_NAME_ID, UNUSED_BETTER_NAME_ID, FROM_MASK_ID, MAKE_ID, MATERIAL_ID)) {
            JIPipe.getNodes().unregister(id);
        }
        NodeUsage.resetForTests();
    }

    @AfterEach
    void resetUsage() {
        NodeUsage.resetForTests();
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

    private static CreateNewNodesByTemplateDatabaseEntry patternEntry(String name, int nodeCount) {
        JIPipeGraph graph = new JIPipeGraph();
        for (int i = 0; i < nodeCount; i++) {
            graph.insertNode(JIPipe.getNodes().getInfoById(MATERIAL_ID).newInstance());
        }
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        template.setName(name);
        template.setDescription(new HTMLText("A stub pattern for the usage ranking test."));
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
     * The usage TIE-BREAK flips near-ties inside the name-prefix tier:
     * both names START WITH "extract blobs" (prefix tier, no usage bonus
     * there), everything else equal - the widely-used one wins on the
     * comparator's usage key (without it the alphabetical name order
     * puts "thin" first).
     */
    @Test
    void usageTieBreakFlipsNearTies() {
        NodeUsage.setPriorSupplier(() -> Map.of(WIDE_ID, 100));
        List<JIPipeNodeDatabaseEntry> results = search.query("extract blobs",
                JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of());
        assertTrue(indexOfName(results, "Extract blobs wide")
                        < indexOfName(results, "Extract blobs thin"),
                "results: " + names(results));
    }

    /**
     * The usage BONUS changes the outcome inside the coverage-scored tier
     * (Review finding, MR A: the tie-break test above never exercises the
     * bonus because prefix-tier entries skip scoring). Both names CONTAIN
     * but do not start with "extract blobs" (any name containing the query
     * words reaches full blended coverage - name, taxonomy-token, and
     * extra fields all saturate - so the score gap comes from the
     * normalized LD and the length penalty): "Delayed extract blobs"
     * (LD 8/21, penalty 0.0571) scores ~0.0258 worse than "Wide extract
     * blobs" (LD 5/18, penalty 0.0417). A prior of 10000 yields a bonus of
     * 0.05 * 10000/10010 ~ 0.04995, which overcomes the deficit: the
     * widely used node wins. Deleting the {@code - usageBonus(...)} term
     * flips the outcome - this test fails without it.
     */
    @Test
    void usageBonusFlipsTheOutcomeInTheCoverageTier() {
        NodeUsage.setPriorSupplier(() -> Map.of(USED_WORSE_NAME_ID, 10000));
        List<JIPipeNodeDatabaseEntry> results = search.query("extract blobs",
                JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of());
        assertTrue(indexOfName(results, "Delayed extract blobs")
                        < indexOfName(results, "Wide extract blobs"),
                "results: " + names(results));
    }

    /**
     * Usage never crosses the exact/name-prefix ladder: a name-prefix
     * match with zero usage outranks a coverage-only match with a huge
     * prior.
     */
    @Test
    void usageCannotCrossTheLadder() {
        NodeUsage.setPriorSupplier(() -> Map.of(MAKE_ID, 10000));
        List<JIPipeNodeDatabaseEntry> results = search.query("extract blobs",
                JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of());
        assertTrue(indexOfName(results, "Extract blobs from mask")
                        < indexOfName(results, "Slow extract blobs"),
                "results: " + names(results));
    }

    /**
     * Entries without an origin node id (templates) take no bonus and
     * never throw; ranking stays deterministic across repeated queries.
     */
    @Test
    void nullOriginEntriesGetNoBonusAndNoNpe() {
        NodeUsage.setPriorSupplier(() -> Map.of(WIDE_ID, 1000));
        List<JIPipeNodeDatabaseEntry> first = search.query("blobs workflow",
                JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of());
        List<JIPipeNodeDatabaseEntry> second = search.query("blobs workflow",
                JIPipeNodeDatabasePipelineVisibility.Pipeline, false, true, Set.of());
        assertEquals(first, second, "ranking must be deterministic");
        assertNotEquals(-1, indexOfName(first, "Extract blobs workflow"), "the template must be found");
    }

    /**
     * The bucket scale is rescaled by the name+token field maximum
     * (0.7 + 0.25): a full name+token match without description echo
     * (blended coverage 0.95) reaches the top bucket.
     */
    @Test
    void bucketForRescalesTheNameTokenMaximum() {
        assertEquals(0, JIPipeEnhancedNodeDatabaseSearch.bucketFor(0.95));
        assertEquals(0, JIPipeEnhancedNodeDatabaseSearch.bucketFor(1.00));
        assertEquals(1, JIPipeEnhancedNodeDatabaseSearch.bucketFor(0.94));
        assertEquals(4, JIPipeEnhancedNodeDatabaseSearch.bucketFor(0.60));
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

    private static class TestNodeDatabase extends JIPipeNodeDatabase {
        private TestNodeDatabase() {
            super();
        }

        @Override
        public void rebuildImmediately() {
            // No-op: the test supplies the entries directly.
        }
    }

    /** Near-tie variant A (widely used via the injected prior). */
    @SetJIPipeDocumentation(name = "Extract blobs wide", description = "A stub node for the usage ranking test.")
    @ConfigureJIPipeNode
    public static class ExtractBlobsWideStub extends JIPipeGraphNode {
        public ExtractBlobsWideStub(JIPipeNodeInfo info) {
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

    /** Near-tie variant B (unused). */
    @SetJIPipeDocumentation(name = "Extract blobs thin", description = "A stub node for the usage ranking test.")
    @ConfigureJIPipeNode
    public static class ExtractBlobsThinStub extends JIPipeGraphNode {
        public ExtractBlobsThinStub(JIPipeNodeInfo info) {
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

    /** The ladder guard's prefix match (zero usage). */
    @SetJIPipeDocumentation(name = "Extract blobs from mask", description = "A stub node for the usage ranking test.")
    @ConfigureJIPipeNode
    public static class ExtractBlobsFromMaskStub extends JIPipeGraphNode {
        public ExtractBlobsFromMaskStub(JIPipeNodeInfo info) {
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
     * The ladder guard's coverage-only match (huge prior): the name
     * contains the full query (full blended coverage) but is not a
     * name-prefix match, so it is coverage-scored and usage-boosted - and
     * still must lose to the zero-usage name-prefix match above.
     */
    @SetJIPipeDocumentation(name = "Slow extract blobs", description = "A stub node for the usage ranking test.")
    @ConfigureJIPipeNode
    public static class SlowExtractBlobsStub extends JIPipeGraphNode {
        public SlowExtractBlobsStub(JIPipeNodeInfo info) {
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
     * Coverage-tier near-tie variant A (widely used via the injected
     * prior): the name contains but does not start with the query, and is
     * the LONGER of the two (worse normalized LD and length penalty).
     */
    @SetJIPipeDocumentation(name = "Delayed extract blobs", description = "A stub node for the usage ranking test.")
    @ConfigureJIPipeNode
    public static class DelayedExtractBlobsStub extends JIPipeGraphNode {
        public DelayedExtractBlobsStub(JIPipeNodeInfo info) {
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
     * Coverage-tier near-tie variant B (unused): same query coverage as
     * variant A, but the SHORTER name (better normalized LD and length
     * penalty, a small fused advantage).
     */
    @SetJIPipeDocumentation(name = "Wide extract blobs", description = "A stub node for the usage ranking test.")
    @ConfigureJIPipeNode
    public static class WideExtractBlobsStub extends JIPipeGraphNode {
        public WideExtractBlobsStub(JIPipeNodeInfo info) {
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

    /** Pattern graph material. */
    @SetJIPipeDocumentation(name = "Usage Ranking Material Node", description = "A stub for the usage ranking patterns.")
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
