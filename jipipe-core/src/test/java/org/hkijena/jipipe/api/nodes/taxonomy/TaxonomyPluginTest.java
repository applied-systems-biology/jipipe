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

package org.hkijena.jipipe.api.nodes.taxonomy;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.misc.MiscellaneousUnassignedFunction;
import org.hkijena.jipipe.api.service.components.JIPipeNodeTaxonomyServiceComponent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check that the {@code TaxonomyPlugin} (discovered via the SciJava
 * {@code @Plugin} annotation during {@link JIPipe#ensureInstance()}) actually
 * registers the curated taxonomy into the live taxonomy service:
 * <ul>
 * <li>every expected core function id is present (14 top-level branches + 76 leaves),</li>
 * <li>every expected core framework id is present (20 frameworks),</li>
 * <li>a valid function tree ({@link JIPipeNodeTaxonomyServiceComponent#validateFunctions()}
 * empty),</li>
 * <li>the inference fallback is the unassigned sink.</li>
 * </ul>
 * <p>
 * Presence, not counts (AGENTS.md plugin-aware test ruling): a plugin may
 * legitimately register additional functions/frameworks into the live
 * registries, so this test never pins an exact registry size. The
 * {@code EXPECTED_*_IDS} lists also double as the ID-stability gate: they pin
 * the exact stable string literals persisted in saved projects.
 * <p>
 * Families are NOT asserted on the taxonomy service: families are
 * node-driven (assembled from node step annotations by the nodes service),
 * not plugin-contributed, so {@code getFamilies()} on the nodes service is
 * expected to be empty until nodes carry family annotations (Task 13).
 */
class TaxonomyPluginTest {

    /**
     * The 90 expected core function ids, alphabetical by file path.
     * This list is the ID-stability pin: every entry must remain registered
     * under exactly this id.
     */
    private static final List<String> EXPECTED_FUNCTION_IDS = List.of(
            "jipipe:function:annotations",
            "jipipe:function:data",
            "jipipe:function:data-utilities",
            "jipipe:function:export",
            "jipipe:function:filaments",
            "jipipe:function:graph",
            "jipipe:function:images",
            "jipipe:function:machine-learning",
            "jipipe:function:misc",
            "jipipe:function:plotting",
            "jipipe:function:rois",
            "jipipe:function:scripting",
            "jipipe:function:statistics",
            "jipipe:function:tables",
            "jipipe:function:templates",
            "jipipe:function:text-markup",
            "jipipe:function:tracking",
            "jipipe:function:annotations-convert",
            "jipipe:function:annotations-edit",
            "jipipe:function:graph-annotations",
            "jipipe:function:filament-data",
            "jipipe:function:file-system",
            "jipipe:function:forms",
            "jipipe:function:image-data",
            "jipipe:function:misc-data",
            "jipipe:function:omero",
            "jipipe:function:roi-data",
            "jipipe:function:scene-3d-data",
            "jipipe:function:table-data",
            "jipipe:function:export-data",
            "jipipe:function:export-images",
            "jipipe:function:export-roi",
            "jipipe:function:export-scenes",
            "jipipe:function:export-tables",
            "jipipe:function:filament-filtering",
            "jipipe:function:filament-quantification",
            "jipipe:function:filament-smoothing",
            "jipipe:function:graph-compartment-outputs",
            "jipipe:function:graph-debug",
            "jipipe:function:graph-wrappers",
            "jipipe:function:color-stack",
            "jipipe:function:filtering",
            "jipipe:function:image-arithmetic",
            "jipipe:function:image-comparison",
            "jipipe:function:image-labeling",
            "jipipe:function:image-moments",
            "jipipe:function:image-neighbor-measurements",
            "jipipe:function:image-pixel-manipulation",
            "jipipe:function:image-projections",
            "jipipe:function:image-properties",
            "jipipe:function:image-statistics",
            "jipipe:function:image-transformations",
            "jipipe:function:image-type-conversion",
            "jipipe:function:measurements",
            "jipipe:function:merging-splitting",
            "jipipe:function:preprocessing",
            "jipipe:function:resampling",
            "jipipe:function:segmentation-cell-based",
            "jipipe:function:segmentation",
            "jipipe:function:thresholding",
            "jipipe:function:machine-learning-inference",
            "jipipe:function:machine-learning-models",
            "jipipe:function:machine-learning-training",
            "jipipe:function:misc-unassigned",
            "jipipe:function:plot-bar",
            "jipipe:function:plot-heatmap",
            "jipipe:function:plot-line",
            "jipipe:function:plot-misc",
            "jipipe:function:plot-scatter",
            "jipipe:function:roi-filters",
            "jipipe:function:roi-intersections",
            "jipipe:function:roi-measurements",
            "jipipe:function:roi-morphology",
            "jipipe:function:roi-properties",
            "jipipe:function:scripting-expression",
            "jipipe:function:scripting-imagej-macro",
            "jipipe:function:scripting-python",
            "jipipe:function:scripting-r",
            "jipipe:function:statistics-descriptive",
            "jipipe:function:statistics-estimation",
            "jipipe:function:statistics-testing",
            "jipipe:function:table-aggregate",
            "jipipe:function:table-export-file",
            "jipipe:function:table-filter-modify",
            "jipipe:function:table-join-split",
            "jipipe:function:table-transform",
            "jipipe:function:tracking-analysis",
            "jipipe:function:tracking-detection",
            "jipipe:function:tracking-filtering",
            "jipipe:function:tracking-linking");

    /**
     * The 20 expected core framework ids, alphabetical by file path.
     * This list is the ID-stability pin: every entry must remain registered
     * under exactly this id.
     */
    private static final List<String> EXPECTED_FRAMEWORK_IDS = List.of(
            "jipipe:framework:cellpose2",
            "jipipe:framework:cellpose3",
            "jipipe:framework:cellpose4",
            "jipipe:framework:clij",
            "jipipe:framework:core",
            "jipipe:framework:filaments",
            "jipipe:framework:ij3d",
            "jipipe:framework:ilastik",
            "jipipe:framework:imagej1",
            "jipipe:framework:imagej2",
            "jipipe:framework:jfreechart",
            "jipipe:framework:jipipedl",
            "jipipe:framework:omero",
            "jipipe:framework:omnipose",
            "jipipe:framework:opencv",
            "jipipe:framework:python",
            "jipipe:framework:r",
            "jipipe:framework:scene-3d",
            "jipipe:framework:trackmate",
            "jipipe:framework:weka");

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    @Test
    void allCoreTaxonomyRegistered() {
        // Presence, not counts: plugins may legitimately register more (AGENTS.md
        // plugin-aware test ruling). Every expected core id must be present; the
        // list itself pins the stable literals (ID-stability gate).
        var taxonomy = JIPipe.getNodeTaxonomyService();
        assertTrue(taxonomy.allFunctions().size() >= EXPECTED_FUNCTION_IDS.size(),
                "at least the " + EXPECTED_FUNCTION_IDS.size() + " core functions");
        for (String id : EXPECTED_FUNCTION_IDS) {
            assertNotNull(taxonomy.functionById(id), "function " + id);
            assertEquals(id, taxonomy.functionById(id).getId(), "getId drift for " + id);
        }
        for (String id : EXPECTED_FRAMEWORK_IDS) {
            assertNotNull(taxonomy.frameworkById(id), "framework " + id);
            assertEquals(id, taxonomy.frameworkById(id).getId(), "getId drift for " + id);
        }
        assertTrue(taxonomy.validateFunctions().isEmpty(),
                "function tree errors: " + taxonomy.validateFunctions());
        // the inference fallback is the unassigned sink (Task 3 behavior change)
        assertEquals(MiscellaneousUnassignedFunction.ID,
                taxonomy.inferFunction("zzz no keywords here at all").getId());
        // Families are node-driven (assembled from node step annotations by the
        // nodes service, Task 13); no node carries one yet.
        assertTrue(JIPipe.getNodes().getFamilies().isEmpty(),
                "Expected no families yet (Task 13), got: " + JIPipe.getNodes().getFamilies().keySet());
    }
}
