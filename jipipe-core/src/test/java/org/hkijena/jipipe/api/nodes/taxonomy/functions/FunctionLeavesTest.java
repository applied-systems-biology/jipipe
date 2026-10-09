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
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.FilamentDataFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.ImageDataFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.MiscDataFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.RoiDataFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.data.TableDataFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.deeplearning.DeepLearningInferenceFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.deeplearning.DeepLearningModelsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.deeplearning.DeepLearningTrainingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.export.ExportArchiveFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.export.ExportFileFormatsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.filaments.FilamentFilteringFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.filaments.FilamentQuantificationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.filaments.FilamentSmoothingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphCompartmentOutputsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphDebugFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphWrappersFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ColorStackFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.FilteringFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MeasurementsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MergingSplittingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.PreprocessingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ResamplingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.SegmentationCellBasedFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.SegmentationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ThresholdingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotBarFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotHeatmapFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotLineFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotMiscFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.plotting.PlotScatterFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.RoiFiltersFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.RoiIntersectionsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.RoiMeasurementsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.RoiMorphologyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.statistics.StatisticsDescriptiveFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.statistics.StatisticsEstimationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.statistics.StatisticsTestingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tables.TableAggregateFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tables.TableExportFileFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tables.TableFilterModifyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tables.TableJoinSplitFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tables.TableTransformFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingAnalysisFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingDetectionFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingFilteringFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingLinkingFunction;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the 48 seed {@link JIPipeNodeTaxonomyFunction} leaves (the exact
 * list in the Phase 1a plan for Task 10; its "39 files" note is an arithmetic
 * slip — 5+9+4+3+5+3+4+2+3+5+2+3 = 48):
 * the exact stable literal ids, parent wiring (the 13 top-level branches
 * plus the one deeper level: cell-based segmentation under segmentation),
 * null hues (sub-branches inherit), null icons, visibility mirroring the
 * parent branch (false/false for the transparent Graph branch), the 3 seed
 * tags (all others empty), reachability of every leaf via its parent chain,
 * and a clean {@link FunctionRegistry#validate()} for the full
 * top-level + leaf set.
 */
class FunctionLeavesTest {

    /**
     * The 48 leaf class literals, grouped by subpackage.
     */
    private static List<Class<? extends JIPipeNodeTaxonomyFunction>> allLeaves() {
        return List.of(
            // data
            ImageDataFunction.class,
            TableDataFunction.class,
            RoiDataFunction.class,
            FilamentDataFunction.class,
            MiscDataFunction.class,
            // images
            PreprocessingFunction.class,
            FilteringFunction.class,
            ThresholdingFunction.class,
            SegmentationFunction.class,
            SegmentationCellBasedFunction.class,
            MeasurementsFunction.class,
            ResamplingFunction.class,
            MergingSplittingFunction.class,
            ColorStackFunction.class,
            // rois
            RoiFiltersFunction.class,
            RoiMeasurementsFunction.class,
            RoiMorphologyFunction.class,
            RoiIntersectionsFunction.class,
            // filaments
            FilamentSmoothingFunction.class,
            FilamentFilteringFunction.class,
            FilamentQuantificationFunction.class,
            // tables
            TableFilterModifyFunction.class,
            TableJoinSplitFunction.class,
            TableAggregateFunction.class,
            TableTransformFunction.class,
            TableExportFileFunction.class,
            // statistics
            StatisticsDescriptiveFunction.class,
            StatisticsTestingFunction.class,
            StatisticsEstimationFunction.class,
            // tracking
            TrackingDetectionFunction.class,
            TrackingLinkingFunction.class,
            TrackingFilteringFunction.class,
            TrackingAnalysisFunction.class,
            // annotations
            AnnotationsEditFunction.class,
            AnnotationsConvertFunction.class,
            // deeplearning
            DeepLearningInferenceFunction.class,
            DeepLearningTrainingFunction.class,
            DeepLearningModelsFunction.class,
            // plotting
            PlotScatterFunction.class,
            PlotLineFunction.class,
            PlotBarFunction.class,
            PlotHeatmapFunction.class,
            PlotMiscFunction.class,
            // export
            ExportFileFormatsFunction.class,
            ExportArchiveFunction.class,
            // graph
            GraphCompartmentOutputsFunction.class,
            GraphWrappersFunction.class,
            GraphDebugFunction.class
        );
    }

    /**
     * The 13 top-level branches from Task 9.
     */
    private static List<JIPipeNodeTaxonomyFunction> allTopLevels() {
        return List.of(
            new DataSourcesFunction(),
            new ImagesFunction(),
            new RoisFunction(),
            new FilamentsFunction(),
            new TablesFunction(),
            new StatisticsFunction(),
            new TrackingFunction(),
            new AnnotationsFunction(),
            new DeepLearningFunction(),
            new PlottingFunction(),
            new ExportFunction(),
            new GraphFunction(),
            new MiscellaneousFunction()
        );
    }

    private static JIPipeNodeTaxonomyFunction instantiate(Class<? extends JIPipeNodeTaxonomyFunction> leaf) {
        try {
            return leaf.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not instantiate " + leaf.getName(), e);
        }
    }

    @Test
    void idsAreUniqueStableLiterals() {
        Set<String> ids = new HashSet<>();
        for (Class<? extends JIPipeNodeTaxonomyFunction> leaf : allLeaves()) {
            String id = instantiate(leaf).getId();
            assertTrue(id != null && id.startsWith("jipipe:function:"),
                "Id of " + leaf.getName() + " must be a non-null literal starting with 'jipipe:function:', got: " + id);
            ids.add(id);
        }
        assertEquals(48, ids.size(), "Expected 48 unique leaf ids, got " + ids.size() + " for: " + ids);
    }

    @Test
    void idsMatchTheExactSeedLiterals() {
        assertEquals("jipipe:function:image-data", new ImageDataFunction().getId());
        assertEquals("jipipe:function:table-data", new TableDataFunction().getId());
        assertEquals("jipipe:function:roi-data", new RoiDataFunction().getId());
        assertEquals("jipipe:function:filament-data", new FilamentDataFunction().getId());
        assertEquals("jipipe:function:misc-data", new MiscDataFunction().getId());
        assertEquals("jipipe:function:preprocessing", new PreprocessingFunction().getId());
        assertEquals("jipipe:function:filtering", new FilteringFunction().getId());
        assertEquals("jipipe:function:thresholding", new ThresholdingFunction().getId());
        assertEquals("jipipe:function:segmentation", new SegmentationFunction().getId());
        assertEquals("jipipe:function:segmentation-cell-based", new SegmentationCellBasedFunction().getId());
        assertEquals("jipipe:function:measurements", new MeasurementsFunction().getId());
        assertEquals("jipipe:function:resampling", new ResamplingFunction().getId());
        assertEquals("jipipe:function:merging-splitting", new MergingSplittingFunction().getId());
        assertEquals("jipipe:function:color-stack", new ColorStackFunction().getId());
        assertEquals("jipipe:function:roi-filters", new RoiFiltersFunction().getId());
        assertEquals("jipipe:function:roi-measurements", new RoiMeasurementsFunction().getId());
        assertEquals("jipipe:function:roi-morphology", new RoiMorphologyFunction().getId());
        assertEquals("jipipe:function:roi-intersections", new RoiIntersectionsFunction().getId());
        assertEquals("jipipe:function:filament-smoothing", new FilamentSmoothingFunction().getId());
        assertEquals("jipipe:function:filament-filtering", new FilamentFilteringFunction().getId());
        assertEquals("jipipe:function:filament-quantification", new FilamentQuantificationFunction().getId());
        assertEquals("jipipe:function:table-filter-modify", new TableFilterModifyFunction().getId());
        assertEquals("jipipe:function:table-join-split", new TableJoinSplitFunction().getId());
        assertEquals("jipipe:function:table-aggregate", new TableAggregateFunction().getId());
        assertEquals("jipipe:function:table-transform", new TableTransformFunction().getId());
        assertEquals("jipipe:function:table-export-file", new TableExportFileFunction().getId());
        assertEquals("jipipe:function:statistics-descriptive", new StatisticsDescriptiveFunction().getId());
        assertEquals("jipipe:function:statistics-testing", new StatisticsTestingFunction().getId());
        assertEquals("jipipe:function:statistics-estimation", new StatisticsEstimationFunction().getId());
        assertEquals("jipipe:function:tracking-detection", new TrackingDetectionFunction().getId());
        assertEquals("jipipe:function:tracking-linking", new TrackingLinkingFunction().getId());
        assertEquals("jipipe:function:tracking-filtering", new TrackingFilteringFunction().getId());
        assertEquals("jipipe:function:tracking-analysis", new TrackingAnalysisFunction().getId());
        assertEquals("jipipe:function:annotations-edit", new AnnotationsEditFunction().getId());
        assertEquals("jipipe:function:annotations-convert", new AnnotationsConvertFunction().getId());
        assertEquals("jipipe:function:deeplearning-inference", new DeepLearningInferenceFunction().getId());
        assertEquals("jipipe:function:deeplearning-training", new DeepLearningTrainingFunction().getId());
        assertEquals("jipipe:function:deeplearning-models", new DeepLearningModelsFunction().getId());
        assertEquals("jipipe:function:plot-scatter", new PlotScatterFunction().getId());
        assertEquals("jipipe:function:plot-line", new PlotLineFunction().getId());
        assertEquals("jipipe:function:plot-bar", new PlotBarFunction().getId());
        assertEquals("jipipe:function:plot-heatmap", new PlotHeatmapFunction().getId());
        assertEquals("jipipe:function:plot-misc", new PlotMiscFunction().getId());
        assertEquals("jipipe:function:export-file-formats", new ExportFileFormatsFunction().getId());
        assertEquals("jipipe:function:export-archive", new ExportArchiveFunction().getId());
        assertEquals("jipipe:function:graph-compartment-outputs", new GraphCompartmentOutputsFunction().getId());
        assertEquals("jipipe:function:graph-wrappers", new GraphWrappersFunction().getId());
        assertEquals("jipipe:function:graph-debug", new GraphDebugFunction().getId());
    }

    @Test
    void everyLeafHasAParent() {
        for (Class<? extends JIPipeNodeTaxonomyFunction> leaf : allLeaves()) {
            JIPipeNodeTaxonomyFunction function = instantiate(leaf);
            assertTrue(function.getParent() != null,
                "Expected " + leaf.getName() + " to have a parent");
        }
    }

    @Test
    void parentsAreTask9BranchesOrSegmentationLeaf() {
        Set<Class<? extends JIPipeNodeTaxonomyFunction>> allowedParents = allTopLevels().stream()
            .map(JIPipeNodeTaxonomyFunction::getClass)
            .collect(Collectors.toSet());
        allowedParents.add(SegmentationFunction.class);
        for (Class<? extends JIPipeNodeTaxonomyFunction> leaf : allLeaves()) {
            JIPipeNodeTaxonomyFunction function = instantiate(leaf);
            assertTrue(allowedParents.contains(function.getParent().getClass()),
                "Unexpected parent of " + leaf.getName() + ": " + function.getParent().getClass().getName());
        }
    }

    @Test
    void leavesHaveNoHueAndNoIcon() {
        for (Class<? extends JIPipeNodeTaxonomyFunction> leaf : allLeaves()) {
            JIPipeNodeTaxonomyFunction function = instantiate(leaf);
            assertNull(function.getColorHue(),
                "Expected null hue (inheritance) for " + leaf.getName());
            assertNull(function.getIcon(),
                "Expected null icon for " + leaf.getName());
        }
    }

    @Test
    void visibilityMirrorsTheParentBranch() {
        for (Class<? extends JIPipeNodeTaxonomyFunction> leaf : allLeaves()) {
            JIPipeNodeTaxonomyFunction function = instantiate(leaf);
            JIPipeNodeTaxonomyFunction parent = function.getParent();
            assertEquals(parent.isVisibleInPipeline(), function.isVisibleInPipeline(),
                "isVisibleInPipeline of " + leaf.getName() + " must mirror its parent");
            assertEquals(parent.isVisibleInCompartments(), function.isVisibleInCompartments(),
                "isVisibleInCompartments of " + leaf.getName() + " must mirror its parent");
        }
        // All seed leaves are hidden in compartments (their branch is).
        for (Class<? extends JIPipeNodeTaxonomyFunction> leaf : allLeaves()) {
            assertFalse(instantiate(leaf).isVisibleInCompartments(),
                "Expected " + leaf.getName() + " to be hidden in compartments");
        }
    }

    @Test
    void exactlyThreeLeavesCarrySeedTags() {
        assertEquals(Set.of("spot-detection"), new TrackingDetectionFunction().getTags());
        assertEquals(Set.of("inference"), new DeepLearningInferenceFunction().getTags());
        assertEquals(Set.of("cell-segmentation"), new SegmentationCellBasedFunction().getTags());

        Set<Class<? extends JIPipeNodeTaxonomyFunction>> tagged = Set.of(
            TrackingDetectionFunction.class,
            DeepLearningInferenceFunction.class,
            SegmentationCellBasedFunction.class
        );
        for (Class<? extends JIPipeNodeTaxonomyFunction> leaf : allLeaves()) {
            if (!tagged.contains(leaf)) {
                assertTrue(instantiate(leaf).getTags().isEmpty(),
                    "Expected empty tags for " + leaf.getName());
            }
        }
    }

    @Test
    void cellBasedSegmentationIsChildOfSegmentation() {
        JIPipeNodeTaxonomyFunction parent = new SegmentationCellBasedFunction().getParent();
        assertTrue(parent instanceof SegmentationFunction,
            "Expected parent to be an instance of SegmentationFunction, got " +
                (parent == null ? "null" : parent.getClass().getName()));
    }

    @Test
    void everyLeafChainReachesATopLevel() {
        for (Class<? extends JIPipeNodeTaxonomyFunction> leaf : allLeaves()) {
            JIPipeNodeTaxonomyFunction function = instantiate(leaf);
            Set<String> visited = new HashSet<>();
            while (function.getParent() != null) {
                assertTrue(visited.add(function.getId()),
                    "Cycle in the parent chain of " + leaf.getName() + " at " + function.getId());
                function = function.getParent();
            }
        }
    }

    @Test
    void registryValidateReturnsEmptyForFullSet() {
        FunctionRegistry registry = new FunctionRegistry();
        for (JIPipeNodeTaxonomyFunction function : allTopLevels()) {
            registry.register(function);
        }
        for (Class<? extends JIPipeNodeTaxonomyFunction> leaf : allLeaves()) {
            registry.register(instantiate(leaf));
        }
        assertEquals(13 + 48, registry.all().size());
        Set<String> errors = registry.validate();
        assertTrue(errors.isEmpty(), "Expected no validation errors but got: " + errors);
    }
}
