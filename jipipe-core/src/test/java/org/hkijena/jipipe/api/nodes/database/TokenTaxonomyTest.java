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
import org.hkijena.jipipe.api.JIPipeNodeTaxonomyTags;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeEmptyData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeNodeAlias;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoAliasDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.InputProfileProvider;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.Cellpose4Framework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.ImagesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.RoisFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.SegmentationCellBasedFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.SegmentationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.RoiMeasurementsFunction;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the node taxonomy is fed into the node search's
 * {@link WeightedTokens} <b>additively</b> (Task 18, dual-read): a
 * converted node's token set gains function-path words (leaf 6 → parent 4 →
 * top 2), the framework name (6) and abbreviation (5), and the merged tags
 * (6), while the legacy name (8) / menu (2) / description (1) tokens are
 * kept unchanged. An unconverted node's token set is identical to the
 * pre-taxonomy one, so search can never regress during 1b.
 * <p>
 * {@code WeightedTokens.add} lowercases and splits on whitespace, so all
 * assertions here are per-word (the same word can carry several weights).
 * The test's converted node uses two real, disjoint function branches:
 * <ul>
 * <li><b>Primary path</b> Images &gt; Segmentation &gt; Cell-based:
 *     "cell-based" (leaf word) ⇒ {6}; "segmentation" (leaf word AND the
 *     parent "Segmentation"'s only word) ⇒ {6, 4}; "images" (top word) ⇒ {2};</li>
 * <li><b>Secondary leaf</b> "ROI measurements" (parent "ROIs", top none):
 *     "roi", "measurements" (leaf words) ⇒ {6}; the parent/top words are
 *     deliberately NOT walked — "rois" must be absent (no ancestor
 *     double-add for secondary functions);</li>
 * <li><b>Framework</b> "Cellpose 4" / "CP4": "cellpose", "4" ⇒ {6}, "cp4" ⇒ {5};</li>
 * <li><b>Merged tags</b> "cells" (node-level) + "cell-segmentation"
 *     (function-path tag) ⇒ {6}.</li>
 * </ul>
 * The 3D spatial dimension and the derived input profile are filter-only
 * facets and must NOT become tokens.
 */
class TokenTaxonomyTest {

    private static final String CONVERTED_ID = "tokentax:converted-cell-segmenter";
    private static final String UNCONVERTED_ID = "tokentax:unconverted-legacy";
    private static final String CONVERTED_ALIAS_ID = CONVERTED_ID + ":alias-0";

    private static final JIPipeMutableDependency TEST_DEPENDENCY =
        new JIPipeMutableDependency("token-taxonomy-test", "1.0.0", "token-taxonomy-test");

    /**
     * A test input-profile provider mapping the stub's
     * {@link JIPipeEmptyData} input slot to a non-NA profile, so that the
     * converted node's derived input profile is non-NA (the same pattern
     * as {@code NodeBrowserModelTest}). It must NOT influence the token
     * set (filter-only facet).
     */
    private static final InputProfileProvider TEST_PROFILE_PROVIDER = dataClass ->
        JIPipeEmptyData.class.equals(dataClass) ? JIPipeNodeTaxonomyInputProfile.PLANE_2D
            : JIPipeNodeTaxonomyInputProfile.NA;

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    @AfterEach
    void cleanup() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        nodes.unregister(CONVERTED_ID);
        nodes.unregister(UNCONVERTED_ID);
        nodes.unregisterInputProfileProviders(List.of(TEST_PROFILE_PROVIDER));
        // Re-run the (idempotent) registration pipeline so that the
        // taxonomy enrichment recomputes over the surviving real nodes only.
        nodes.runRegistrationTasks();
    }

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    /**
     * Brief case 1: a converted node (function path
     * Images &gt; Segmentation &gt; Cell-based primary,
     * ROI measurements secondary, framework Cellpose 4 / CP4, 3D spatial
     * dimension, node-level tag "cells") gains the taxonomy tokens
     * additively; the legacy name/menu/description tokens remain.
     */
    @Test
    void convertedNodeAddsTaxonomyTokensAdditively() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        // Must be registered BEFORE the node so that the registration
        // enrichment derives a non-NA input profile for the stub
        nodes.registerInputProfileProvider(TEST_PROFILE_PROVIDER);
        JIPipeNodeInfo info = registerNode(ConvertedCellSegmenterNode.class, CONVERTED_ID);

        // Sanity: the node is genuinely converted
        assertInstanceOf(SegmentationCellBasedFunction.class, info.getPrimaryFunction());
        assertEquals(1, info.getSecondaryFunctions().size());
        assertInstanceOf(RoiMeasurementsFunction.class, info.getSecondaryFunctions().iterator().next());
        assertInstanceOf(Cellpose4Framework.class, info.getFramework());
        assertEquals(Set.of(JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D), info.getSpatialDimensions());
        assertNotEquals(JIPipeNodeTaxonomyInputProfile.NA, info.getInputProfile());
        // Merged tags: the node-level tag plus the primary function's path tag
        assertEquals(Set.of("cells", "cell-segmentation"), info.getMergedTags());

        WeightedTokens tokens = new CreateNewNodeByInfoDatabaseEntry(
            "create-node-by-info:" + CONVERTED_ID, info).getTokens();

        // --- Legacy name tokens (existing, highest weight 8) ---
        assertEquals(Set.of(WeightedTokens.WEIGHT_NAME), tokenWeights(tokens, "segment"),
            "name word 'segment' must be at WEIGHT_NAME: " + tokenDump(tokens));
        assertTrue(tokenWeights(tokens, "cells").contains(WeightedTokens.WEIGHT_NAME),
            "name word 'cells' must be at WEIGHT_NAME: " + tokenDump(tokens));

        // --- Function-path gradient 6/4/2 (per word, see class Javadoc) ---
        // Primary leaf "Cell-based segmentation": 'cell-based' is unique to
        // the leaf level
        assertEquals(Set.of(WeightedTokens.WEIGHT_FUNCTION_LEAF), tokenWeights(tokens, "cell-based"),
            "primary function leaf word must be at WEIGHT_FUNCTION_LEAF: " + tokenDump(tokens));
        // 'segmentation' is a leaf word (6) AND the parent "Segmentation"'s
        // only word (4)
        assertEquals(Set.of(WeightedTokens.WEIGHT_FUNCTION_LEAF, WeightedTokens.WEIGHT_FUNCTION_PARENT),
            tokenWeights(tokens, "segmentation"),
            "'segmentation' must be at the leaf weight AND the parent weight: " + tokenDump(tokens));
        // Top "Images": 'images' is unique to the top level
        assertEquals(Set.of(WeightedTokens.WEIGHT_FUNCTION_TOP), tokenWeights(tokens, "images"),
            "top function word must be at WEIGHT_FUNCTION_TOP: " + tokenDump(tokens));

        // --- Secondary function: leaf weight only, NO ancestor walk ---
        assertEquals(Set.of(WeightedTokens.WEIGHT_FUNCTION_LEAF), tokenWeights(tokens, "roi"),
            "secondary function leaf word must be at WEIGHT_FUNCTION_LEAF: " + tokenDump(tokens));
        assertEquals(Set.of(WeightedTokens.WEIGHT_FUNCTION_LEAF), tokenWeights(tokens, "measurements"),
            "secondary function leaf word must be at WEIGHT_FUNCTION_LEAF: " + tokenDump(tokens));
        assertFalse(tokens.contains("rois"),
            "the secondary function's ancestor chain must NOT be walked (no double-add): "
                + tokenDump(tokens));

        // --- Framework: name 6 per word, abbreviation 5 ---
        assertEquals(Set.of(WeightedTokens.WEIGHT_FRAMEWORK), tokenWeights(tokens, "cellpose"),
            "framework name word must be at WEIGHT_FRAMEWORK: " + tokenDump(tokens));
        assertEquals(Set.of(WeightedTokens.WEIGHT_FRAMEWORK), tokenWeights(tokens, "4"),
            "framework name word must be at WEIGHT_FRAMEWORK: " + tokenDump(tokens));
        assertEquals(Set.of(WeightedTokens.WEIGHT_FRAMEWORK_ABBREV), tokenWeights(tokens, "cp4"),
            "framework abbreviation must be at WEIGHT_FRAMEWORK_ABBREV: " + tokenDump(tokens));

        // --- Merged tags at WEIGHT_TAG (6) ---
        assertEquals(Set.of(WeightedTokens.WEIGHT_TAG), tokenWeights(tokens, "cell-segmentation"),
            "function-path tag must be at WEIGHT_TAG: " + tokenDump(tokens));
        assertTrue(tokenWeights(tokens, "cells").contains(WeightedTokens.WEIGHT_TAG),
            "node-level tag 'cells' must be at WEIGHT_TAG: " + tokenDump(tokens));

        // --- Legacy menu token still present at WEIGHT_MENU (2) ---
        assertTrue(tokenWeights(tokens, "miscellaneous").contains(WeightedTokens.WEIGHT_MENU),
            "legacy category/menu token 'miscellaneous' must still be at WEIGHT_MENU: " + tokenDump(tokens));

        // --- Legacy description token still present at WEIGHT_DESCRIPTION (1) ---
        assertEquals(Set.of(WeightedTokens.WEIGHT_DESCRIPTION), tokenWeights(tokens, "stub"),
            "legacy description word must still be at WEIGHT_DESCRIPTION: " + tokenDump(tokens));

        // --- Required relative order: name(8) > leaf/framework/tags(6) >
        //     abbrev(5) > parent(4) > top(2) = menu(2) > description(1) ---
        assertTrue(WeightedTokens.WEIGHT_NAME > WeightedTokens.WEIGHT_FUNCTION_LEAF
                && WeightedTokens.WEIGHT_FUNCTION_LEAF == WeightedTokens.WEIGHT_FRAMEWORK
                && WeightedTokens.WEIGHT_FUNCTION_LEAF == WeightedTokens.WEIGHT_TAG
                && WeightedTokens.WEIGHT_FUNCTION_LEAF > WeightedTokens.WEIGHT_FRAMEWORK_ABBREV
                && WeightedTokens.WEIGHT_FRAMEWORK_ABBREV > WeightedTokens.WEIGHT_FUNCTION_PARENT
                && WeightedTokens.WEIGHT_FUNCTION_PARENT > WeightedTokens.WEIGHT_FUNCTION_TOP
                && WeightedTokens.WEIGHT_FUNCTION_TOP == WeightedTokens.WEIGHT_MENU
                && WeightedTokens.WEIGHT_MENU > WeightedTokens.WEIGHT_DESCRIPTION,
            "the weight constants must preserve the brief's required relative order");

        // --- Spatial dimensions + input profile are filter-only, NOT tokens ---
        for (String spatialToken : Set.of("spatial", "2d", "3d",
            JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D.name().toLowerCase(Locale.ROOT),
            JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D.name().toLowerCase(Locale.ROOT))) {
            assertFalse(tokens.contains(spatialToken),
                "spatial dimension token '" + spatialToken + "' must not be a search token");
        }
        for (JIPipeNodeTaxonomyInputProfile profile : JIPipeNodeTaxonomyInputProfile.values()) {
            assertFalse(tokens.contains(profile.name().toLowerCase(Locale.ROOT)),
                "input profile '" + profile + "' must not be a search token");
        }

        // --- The alias entry (dual-read) carries the same taxonomy tokens ---
        CreateNewNodeByInfoAliasDatabaseEntry alias =
            new CreateNewNodeByInfoAliasDatabaseEntry(CONVERTED_ALIAS_ID, info, info.getAliases().get(0));
        WeightedTokens aliasTokens = alias.getTokens();
        assertEquals(Set.of(WeightedTokens.WEIGHT_FUNCTION_LEAF), tokenWeights(aliasTokens, "cell-based"));
        assertEquals(Set.of(WeightedTokens.WEIGHT_FUNCTION_LEAF), tokenWeights(aliasTokens, "roi"));
        assertFalse(aliasTokens.contains("rois"),
            "the alias entry must not walk the secondary ancestor chain either");
        assertEquals(Set.of(WeightedTokens.WEIGHT_FUNCTION_TOP), tokenWeights(aliasTokens, "images"));
        assertEquals(Set.of(WeightedTokens.WEIGHT_FRAMEWORK), tokenWeights(aliasTokens, "cellpose"));
        assertEquals(Set.of(WeightedTokens.WEIGHT_FRAMEWORK_ABBREV), tokenWeights(aliasTokens, "cp4"));
        assertEquals(Set.of(WeightedTokens.WEIGHT_TAG), tokenWeights(aliasTokens, "cell-segmentation"));
        assertTrue(tokenWeights(aliasTokens, "segment").contains(WeightedTokens.WEIGHT_NAME),
            "the alias entry must keep the node name token at WEIGHT_NAME");
        assertEquals(Set.of(WeightedTokens.WEIGHT_NAME), tokenWeights(aliasTokens, "alt"),
            "the alias entry must keep the alternative name token at WEIGHT_NAME");
    }

    /**
     * Brief case 2: an unconverted node (legacy {@code nodeTypeCategory},
     * no function) yields a token set identical to the pre-taxonomy one —
     * the legacy name/menu/description tokens are present and NO taxonomy
     * word (function, framework, or tag) appears.
     */
    @Test
    void unconvertedNodeHasNoTaxonomyTokens() {
        JIPipeNodeInfo info = registerNode(UnconvertedLegacyNode.class, UNCONVERTED_ID);

        // Sanity: genuinely unconverted
        assertNull(info.getPrimaryFunction());
        assertTrue(info.getSecondaryFunctions().isEmpty());
        assertInstanceOf(NoFramework.class, info.getFramework());
        assertTrue(info.getSpatialDimensions().isEmpty());
        assertTrue(info.getMergedTags().isEmpty());

        WeightedTokens tokens = new CreateNewNodeByInfoDatabaseEntry(
            "create-node-by-info:" + UNCONVERTED_ID, info).getTokens();

        // Legacy tokens are intact: name words (highest), the category word
        // "miscellaneous" at the menu weight, description words at weight 1
        assertEquals(Set.of(WeightedTokens.WEIGHT_NAME), tokenWeights(tokens, "unconverted"));
        assertTrue(tokenWeights(tokens, "legacy").contains(WeightedTokens.WEIGHT_NAME));
        assertTrue(tokenWeights(tokens, "miscellaneous").contains(WeightedTokens.WEIGHT_MENU),
            "the legacy category/menu token must be present: " + tokenDump(tokens));
        assertEquals(Set.of(WeightedTokens.WEIGHT_DESCRIPTION), tokenWeights(tokens, "stub"));

        // No taxonomy word may appear
        for (String taxonomyWord : taxonomyWords()) {
            assertFalse(tokens.contains(taxonomyWord),
                "unconverted node must not carry the taxonomy token '" + taxonomyWord + "': "
                    + tokenDump(tokens));
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static JIPipeNodeInfo registerNode(Class<? extends JIPipeGraphNode> nodeClass, String id) {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        if (nodes.hasNodeInfoWithId(id)) {
            nodes.unregister(id);
        }
        JIPipeNodeInfo info = new JIPipeJavaNodeInfo(id, nodeClass);
        nodes.register(info, TEST_DEPENDENCY);
        // register() runs the registration pipeline and the idempotent
        // taxonomy enrichment, which populates getMergedTags() from the
        // node-level tags plus the tags of every function on the node's
        // paths (leaf → root, primary and secondary).
        return nodes.getRegisteredNodeInfos().get(id);
    }

    /**
     * Every word that the converted test node's taxonomy would contribute
     * (function-name words, function tags, framework words, node-level
     * tag), lowercased — the set the unconverted node must NOT carry.
     */
    private static Set<String> taxonomyWords() {
        Set<String> words = new HashSet<>();
        for (JIPipeNodeTaxonomyFunction function : new JIPipeNodeTaxonomyFunction[]{
            new SegmentationCellBasedFunction(), new SegmentationFunction(), new ImagesFunction(),
            new RoiMeasurementsFunction(), new RoisFunction()}) {
            for (String word : function.getName().toLowerCase(Locale.ROOT).split(" ")) {
                words.add(word);
            }
            for (String tag : function.getTags()) {
                words.add(tag.toLowerCase(Locale.ROOT));
            }
        }
        Cellpose4Framework framework = new Cellpose4Framework();
        for (String word : framework.getName().toLowerCase(Locale.ROOT).split(" ")) {
            words.add(word);
        }
        words.add(framework.getAbbreviation().toLowerCase(Locale.ROOT));
        words.add("cells");
        return words;
    }

    private static Set<Integer> tokenWeights(WeightedTokens tokens, String word) {
        Set<Integer> weights = new HashSet<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (word.equals(tokens.getToken(i))) {
                weights.add(tokens.getWeight(i));
            }
        }
        assertFalse(weights.isEmpty(),
            "expected the token '" + word + "' to be present, tokens=" + tokenDump(tokens));
        return weights;
    }

    private static String tokenDump(WeightedTokens tokens) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(tokens.getToken(i)).append('@').append(tokens.getWeight(i));
        }
        return sb.append("]").toString();
    }

    // ------------------------------------------------------------------
    // Stub taxonomy nodes
    // ------------------------------------------------------------------

    /**
     * A converted node: primary function
     * {@link SegmentationCellBasedFunction} (the real 3-level path
     * Images &gt; Segmentation &gt; Cell-based) plus the secondary
     * {@link RoiMeasurementsFunction} (a different branch: ROIs &gt; ROI
     * measurements — its leaf words "roi"/"measurements" are unique to the
     * leaf level, while its parent word "rois" must NOT appear, proving
     * the secondary ancestor chain is not walked), framework
     * {@link Cellpose4Framework} ("Cellpose 4" / "CP4"), 3D spatial
     * dimension (filter-only, must NOT become a token), and the
     * node-level tag {@code cells} (merged with the primary function's
     * path tag {@code cell-segmentation}).
     */
    @SetJIPipeDocumentation(name = "Segment Cells",
        description = "A stub node for token taxonomy testing.")
    @ConfigureJIPipeNode(
        function = {SegmentationCellBasedFunction.class, RoiMeasurementsFunction.class},
        framework = Cellpose4Framework.class,
        spatialDimensions = {JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D},
        nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
    @JIPipeNodeTaxonomyTags("cells")
    @AddJIPipeInputSlot(value = JIPipeEmptyData.class)
    @AddJIPipeNodeAlias(nodeTypeCategory = MiscellaneousNodeTypeCategory.class,
        menuPath = "alt/cells", aliasName = "Cell Segmenter Alt")
    public static class ConvertedCellSegmenterNode extends JIPipeGraphNode {
        public ConvertedCellSegmenterNode(JIPipeNodeInfo info) {
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
     * An unconverted node: only the legacy {@code nodeTypeCategory} is
     * set — no function, no framework, no tags, no spatial dimensions.
     */
    @SetJIPipeDocumentation(name = "Unconverted Legacy Node",
        description = "A legacy stub node.")
    @ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
    public static class UnconvertedLegacyNode extends JIPipeGraphNode {
        public UnconvertedLegacyNode(JIPipeNodeInfo info) {
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
