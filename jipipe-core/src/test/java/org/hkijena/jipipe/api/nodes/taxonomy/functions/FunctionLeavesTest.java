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

package org.hkijena.jipipe.api.nodes.taxonomy.functions;

import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.annotations.AnnotationsConvertFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.annotations.AnnotationsEditFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.annotations.GraphAnnotationsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.*;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.export.*;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.filaments.FilamentFilteringFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.filaments.FilamentQuantificationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.filaments.FilamentSmoothingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphCompartmentOutputsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphDebugFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphWrappersFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.*;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.machinelearning.MachineLearningInferenceFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.machinelearning.MachineLearningModelsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.machinelearning.MachineLearningTrainingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.*;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.*;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.scripting.ScriptingExpressionFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.scripting.ScriptingImageJMacroFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.scripting.ScriptingPythonFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.scripting.ScriptingRFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.statistics.StatisticsDescriptiveFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.statistics.StatisticsEstimationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.statistics.StatisticsTestingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tables.*;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingAnalysisFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingDetectionFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingFilteringFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingLinkingFunction;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contracts of the designed taxonomy function catalog (the 6.0.0 seed top-level branches and
 * leaves):
 * <ul>
 * <li>{@code idsMatchTheExactSeedLiterals} - every catalog class returns exactly its pinned
 * stable literal. Function ids are registration ids and are persisted in saved projects, so a
 * renamed literal silently breaks existing projects; the pins freeze them. Distinctness is
 * checked as well (two functions sharing a literal cannot coexist in a registry).</li>
 * <li>{@code leafParentsAreTopLevelBranchesOrSubBranches} - the tree shape: leaves hang directly
 * below a top-level branch, with {@link SegmentationFunction} as the only leaf that hosts
 * children. The production validator does not check depth, so this is the only shape guard.</li>
 * <li>{@code registryValidateReturnsEmptyForFullSet} - the production
 * {@link FunctionRegistry#validate()} is clean for the whole catalog (parent chains, cycles,
 * top-level hue uniqueness).</li>
 * </ul>
 * Deliberately not tested here: per-leaf hues, icons, visibility flags, and tags - every leaf
 * class hardcodes them as constants, so assertions would only restate the implementation (test
 * policy: no trivial tests). The live registry (including plugin-contributed functions) is gated
 * by TaxonomyFullValidationTest in jipipe-launcher; this file pins the designed core catalog.
 */
class FunctionLeavesTest {

    /**
     * The 14 top-level branches with their pinned stable literals.
     */
    private static Map<Class<? extends JIPipeNodeTaxonomyFunction>, String> topLevelLiterals() {
        Map<Class<? extends JIPipeNodeTaxonomyFunction>, String> literals = new LinkedHashMap<>();
        literals.put(DataSourcesFunction.class, "jipipe:function:data");
        literals.put(ImagesFunction.class, "jipipe:function:images");
        literals.put(RoisFunction.class, "jipipe:function:rois");
        literals.put(FilamentsFunction.class, "jipipe:function:filaments");
        literals.put(TablesFunction.class, "jipipe:function:tables");
        literals.put(StatisticsFunction.class, "jipipe:function:statistics");
        literals.put(TrackingFunction.class, "jipipe:function:tracking");
        literals.put(AnnotationsFunction.class, "jipipe:function:annotations");
        literals.put(MachineLearningFunction.class, "jipipe:function:machine-learning");
        literals.put(PlottingFunction.class, "jipipe:function:plotting");
        literals.put(ExportFunction.class, "jipipe:function:export");
        literals.put(GraphFunction.class, "jipipe:function:graph");
        literals.put(MiscellaneousFunction.class, "jipipe:function:misc");
        literals.put(ScriptingFunction.class, "jipipe:function:scripting");
        return literals;
    }

    /**
     * The 75 leaves with their pinned stable literals, grouped by subpackage.
     * ({@link ScriptingFunction} itself is a top-level branch since the spec B.5 hue migration;
     * {@link SegmentationCellBasedFunction} and the four scripting language leaves are the
     * leaves that host children.)
     */
    private static Map<Class<? extends JIPipeNodeTaxonomyFunction>, String> leafLiterals() {
        Map<Class<? extends JIPipeNodeTaxonomyFunction>, String> literals = new LinkedHashMap<>();
        // data
        literals.put(ImageDataFunction.class, "jipipe:function:image-data");
        literals.put(TableDataFunction.class, "jipipe:function:table-data");
        literals.put(RoiDataFunction.class, "jipipe:function:roi-data");
        literals.put(FilamentDataFunction.class, "jipipe:function:filament-data");
        literals.put(MiscDataFunction.class, "jipipe:function:misc-data");
        literals.put(Scene3DDataFunction.class, "jipipe:function:scene-3d-data");
        // images
        literals.put(PreprocessingFunction.class, "jipipe:function:preprocessing");
        literals.put(FilteringFunction.class, "jipipe:function:filtering");
        literals.put(ThresholdingFunction.class, "jipipe:function:thresholding");
        literals.put(SegmentationFunction.class, "jipipe:function:segmentation");
        literals.put(SegmentationCellBasedFunction.class, "jipipe:function:segmentation-cell-based");
        literals.put(MeasurementsFunction.class, "jipipe:function:measurements");
        literals.put(ResamplingFunction.class, "jipipe:function:resampling");
        literals.put(MergingSplittingFunction.class, "jipipe:function:merging-splitting");
        literals.put(ColorStackFunction.class, "jipipe:function:color-stack");
        // images - full-registry audit leaves
        literals.put(ImageArithmeticFunction.class, "jipipe:function:image-arithmetic");
        literals.put(ImageComparisonFunction.class, "jipipe:function:image-comparison");
        literals.put(ImageProjectionsFunction.class, "jipipe:function:image-projections");
        literals.put(ImageTypeConversionFunction.class, "jipipe:function:image-type-conversion");
        literals.put(ImageLabelingFunction.class, "jipipe:function:image-labeling");
        literals.put(ImageStatisticsFunction.class, "jipipe:function:image-statistics");
        literals.put(ImageMomentsFunction.class, "jipipe:function:image-moments");
        literals.put(ImageTransformationsFunction.class, "jipipe:function:image-transformations");
        literals.put(ImagePixelManipulationFunction.class, "jipipe:function:image-pixel-manipulation");
        literals.put(ImageNeighborMeasurementsFunction.class, "jipipe:function:image-neighbor-measurements");
        literals.put(ImagePropertiesFunction.class, "jipipe:function:image-properties");
        // data - full-registry audit leaves
        literals.put(FileSystemFunction.class, "jipipe:function:file-system");
        literals.put(OmeroFunction.class, "jipipe:function:omero");
        literals.put(FormsFunction.class, "jipipe:function:forms");
        // misc - full-registry audit leaves
        literals.put(DataUtilitiesFunction.class, "jipipe:function:data-utilities");
        literals.put(DebuggingFunction.class, "jipipe:function:debugging");
        literals.put(TextMarkupFunction.class, "jipipe:function:text-markup");
        // scripting - language sub-leaves (re-clustering audit)
        literals.put(ScriptingPythonFunction.class, "jipipe:function:scripting-python");
        literals.put(ScriptingRFunction.class, "jipipe:function:scripting-r");
        literals.put(ScriptingImageJMacroFunction.class, "jipipe:function:scripting-imagej-macro");
        literals.put(ScriptingExpressionFunction.class, "jipipe:function:scripting-expression");
        // rois
        literals.put(RoiFiltersFunction.class, "jipipe:function:roi-filters");
        literals.put(RoiPropertiesFunction.class, "jipipe:function:roi-properties");
        literals.put(RoiMeasurementsFunction.class, "jipipe:function:roi-measurements");
        literals.put(RoiMorphologyFunction.class, "jipipe:function:roi-morphology");
        literals.put(RoiIntersectionsFunction.class, "jipipe:function:roi-intersections");
        // filaments
        literals.put(FilamentSmoothingFunction.class, "jipipe:function:filament-smoothing");
        literals.put(FilamentFilteringFunction.class, "jipipe:function:filament-filtering");
        literals.put(FilamentQuantificationFunction.class, "jipipe:function:filament-quantification");
        // tables
        literals.put(TableFilterModifyFunction.class, "jipipe:function:table-filter-modify");
        literals.put(TableJoinSplitFunction.class, "jipipe:function:table-join-split");
        literals.put(TableAggregateFunction.class, "jipipe:function:table-aggregate");
        literals.put(TableTransformFunction.class, "jipipe:function:table-transform");
        literals.put(TableExportFileFunction.class, "jipipe:function:table-export-file");
        // statistics
        literals.put(StatisticsDescriptiveFunction.class, "jipipe:function:statistics-descriptive");
        literals.put(StatisticsTestingFunction.class, "jipipe:function:statistics-testing");
        literals.put(StatisticsEstimationFunction.class, "jipipe:function:statistics-estimation");
        // tracking
        literals.put(TrackingDetectionFunction.class, "jipipe:function:tracking-detection");
        literals.put(TrackingLinkingFunction.class, "jipipe:function:tracking-linking");
        literals.put(TrackingFilteringFunction.class, "jipipe:function:tracking-filtering");
        literals.put(TrackingAnalysisFunction.class, "jipipe:function:tracking-analysis");
        // annotations
        literals.put(AnnotationsEditFunction.class, "jipipe:function:annotations-edit");
        literals.put(AnnotationsConvertFunction.class, "jipipe:function:annotations-convert");
        literals.put(GraphAnnotationsFunction.class, "jipipe:function:graph-annotations");
        // deeplearning
        literals.put(MachineLearningInferenceFunction.class, "jipipe:function:machine-learning-inference");
        literals.put(MachineLearningTrainingFunction.class, "jipipe:function:machine-learning-training");
        literals.put(MachineLearningModelsFunction.class, "jipipe:function:machine-learning-models");
        // plotting
        literals.put(PlotScatterFunction.class, "jipipe:function:plot-scatter");
        literals.put(PlotLineFunction.class, "jipipe:function:plot-line");
        literals.put(PlotBarFunction.class, "jipipe:function:plot-bar");
        literals.put(PlotHeatmapFunction.class, "jipipe:function:plot-heatmap");
        literals.put(PlotMiscFunction.class, "jipipe:function:plot-misc");
        // export
        literals.put(ExportImagesFunction.class, "jipipe:function:export-images");
        literals.put(ExportTablesFunction.class, "jipipe:function:export-tables");
        literals.put(ExportRoiFunction.class, "jipipe:function:export-roi");
        literals.put(ExportScenesFunction.class, "jipipe:function:export-scenes");
        literals.put(ExportDataFunction.class, "jipipe:function:export-data");
        // graph
        literals.put(GraphCompartmentOutputsFunction.class, "jipipe:function:graph-compartment-outputs");
        literals.put(GraphWrappersFunction.class, "jipipe:function:graph-wrappers");
        literals.put(GraphDebugFunction.class, "jipipe:function:graph-debug");
        return literals;
    }

    private static JIPipeNodeTaxonomyFunction instantiate(Class<? extends JIPipeNodeTaxonomyFunction> leaf) {
        try {
            return leaf.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not instantiate " + leaf.getName(), e);
        }
    }

    @Test
    void idsMatchTheExactSeedLiterals() {
        Map<Class<? extends JIPipeNodeTaxonomyFunction>, String> catalog = new LinkedHashMap<>(topLevelLiterals());
        catalog.putAll(leafLiterals());
        for (Map.Entry<Class<? extends JIPipeNodeTaxonomyFunction>, String> entry : catalog.entrySet()) {
            assertEquals(entry.getValue(), instantiate(entry.getKey()).getId(),
                    "The pinned stable literal of " + entry.getKey().getName()
                            + " changed - it is persisted in saved projects and must not change");
        }
        assertEquals(catalog.size(), new HashSet<>(catalog.values()).size(),
                "Two taxonomy functions pin the same literal: " + catalog.values());
    }

    @Test
    void leafParentsAreTopLevelBranchesOrSubBranches() {
        Set<Class<? extends JIPipeNodeTaxonomyFunction>> allowedParents =
                new HashSet<>(topLevelLiterals().keySet());
        // The one leaf that hosts children (its language sub-leaves parent to it, the same
        // leaf-with-children pattern as SegmentationFunction)
        allowedParents.add(SegmentationFunction.class);
        for (Class<? extends JIPipeNodeTaxonomyFunction> leaf : leafLiterals().keySet()) {
            JIPipeNodeTaxonomyFunction function = instantiate(leaf);
            JIPipeNodeTaxonomyFunction parent = function.getParent();
            assertNotNull(parent, leaf.getName() + " must not be a top-level branch");
            assertTrue(allowedParents.contains(parent.getClass()),
                    "Unexpected parent of " + leaf.getName() + ": " + parent.getClass().getName());
        }
    }

    @Test
    void registryValidateReturnsEmptyForFullSet() {
        FunctionRegistry registry = new FunctionRegistry();
        for (Class<? extends JIPipeNodeTaxonomyFunction> function : topLevelLiterals().keySet()) {
            registry.register(instantiate(function));
        }
        for (Class<? extends JIPipeNodeTaxonomyFunction> leaf : leafLiterals().keySet()) {
            registry.register(instantiate(leaf));
        }
        Set<String> errors = registry.validate();
        assertTrue(errors.isEmpty(), "Expected no validation errors but got: " + errors);
    }
}
