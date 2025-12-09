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

package org.hkijena.jipipe.plugins.ij3d;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.filesystem.FilesystemPlugin;
import org.hkijena.jipipe.plugins.ij3d.compat.Roi3dImageJExporter;
import org.hkijena.jipipe.plugins.ij3d.compat.Roi3dImageJImporter;
import org.hkijena.jipipe.plugins.ij3d.datatypes.Ij3dSuiteRoiListData;
import org.hkijena.jipipe.plugins.ij3d.display.AddRoi3dToManagerDataDisplayOperation;
import org.hkijena.jipipe.plugins.ij3d.imageviewer.ImageViewerUIRoi3dDisplayApplicationSettings;
import org.hkijena.jipipe.plugins.ij3d.nodes.ImportRoi3dAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.binary.DistanceMap3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.binary.ErodedVolumeFraction3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.binary.Voronoi3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.binary.Watershed3DSplittingAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.features.EdgeFilter3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.features.FindMaxima3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.features.SymmetryFilter3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.filters.*;
import org.hkijena.jipipe.plugins.ij3d.nodes.overlay.ExtractOverlay3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.overlay.RemoveOverlay3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.overlay.RenderOverlay3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.overlay.SetOverlay3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.ExportRoi3dAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.ExportRoi3dAlgorithm2;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.convert.*;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.filter.FilterRoi3dByOverlapOldAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.filter.FilterRoi3dByOverlapAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.filter.FilterRoi3dByStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.filter.FilterRoi3dListsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.generate.FindParticles3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.generate.Roi3dFromLabelsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.measure.ExtractRoi3dRelationStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.measure.ExtractRoi3dStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.merge.MergeRoi3dAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.metadata.ExtractRoi3dMetadataAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.metadata.RemoveRoi3dMetadataAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.metadata.SetRoi3dMetadataFromTableAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.modify.*;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.process.OutlineRoi3dAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.process.RemoveBorderRoi3dAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.split.ExplodeRoi3dListAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.split.SplitRoi3dByStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.split.SplitRoi3dIntoConnectedComponentsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.segmentation.*;
import org.hkijena.jipipe.plugins.ij3d.utils.*;
import org.hkijena.jipipe.plugins.ij3d.viewers.Roi3dListDataViewer;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.multiparameters.MultiParameterAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.scene3d.Scene3DPlugin;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Plugin(type = JIPipeJavaPlugin.class)
public class IJ3DPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    public static final String RESOURCE_BASE_PATH = "/org/hkijena/jipipe/plugins/ij3d";

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(IJ3DPlugin.class, "org/hkijena/jipipe/plugins/ij3d");

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ij-3d",
            JIPipe.getJIPipeVersion(),
            "3D ImageJ Suite integration");

    @Override
    public StringList getDependencyCitations() {
        StringList strings = new StringList();
        strings.add("J. Ollion, J. Cochennec, F. Loll, C. Escudé, T. Boudier. (2013) TANGO: A Generic Tool for High-throughput 3D Image Analysis for Studying Nuclear Organization. Bioinformatics 2013 Jul 15;29(14):1840-1.");
        return strings;
    }

    @Override
    public String getName() {
        return "3D ImageJ Suite integration";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates the 3D ImageJ Suite into JIPipe");
    }

    @Override
    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return Collections.singletonList(new JIPipeImageJUpdateSiteDependency("3D ImageJ Suite", "https://sites.imagej.net/Tboudier/"));
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, FilesystemPlugin.AS_DEPENDENCY, ImageJDataTypesPlugin.AS_DEPENDENCY,
                ImageJAlgorithmsPlugin.AS_DEPENDENCY, MultiParameterAlgorithmsPlugin.AS_DEPENDENCY, Scene3DPlugin.AS_DEPENDENCY);
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {

        registerApplicationSettingsSheet(new ImageViewerUIRoi3dDisplayApplicationSettings());

        registerEnumParameterType("ij3d-measurement", Roi3dMeasurement.class, "IJ3D Measurement", "A 3D object measurement");
        registerEnumParameterType("ij3d-measurement-column", Roi3dMeasurementColumn.class, "IJ3D measurement column", "A 3D object measurement column");
        registerParameterType("ij3d-measurement-set", Roi3dMeasurementSetParameter.class, JIPipeParameterArchetype.MultiSelect, "IJ3D Measurements", "A selection of 3D object measurements");
        registerEnumParameterType("ij3d-relation-measurement", Roi3dRelationMeasurement.class, "IJ3D relation measurement", "Relation between two 3D objects");
        registerEnumParameterType("ij3d-relation-measurement-column", Roi3dRelationMeasurementColumn.class, "IJ3D relation measurement column", "Relation between two 3D objects");
        registerParameterType("ij3d-relation-measurement-set", Roi3dRelationMeasurementSetParameter.class, JIPipeParameterArchetype.MultiSelect, "IJ3D relation measurements", "A selection of measurements between two 3D objects");
        registerEnumParameterType("ij3d-roi-outline", Roi3dOutline.class, "IJ3D ROI outline", "Outline algorithm for 3D ROI");

        registerDatatype("roi-3d-list", Ij3dSuiteRoiListData.class, RESOURCES.getIcon16URL("data-type-roi3d.png"), new AddRoi3dToManagerDataDisplayOperation());
        registerDefaultDataTypeViewer(Ij3dSuiteRoiListData.class, Roi3dListDataViewer.class);
        registerImageJDataImporter("import-roi-3d", new Roi3dImageJImporter(), null);
        registerImageJDataExporter("export-roi-3d", new Roi3dImageJExporter(), null);
        registerNodeType("import-roi-3d", ImportRoi3dAlgorithm.class);
        registerNodeType("export-roi-3d", ExportRoi3dAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/document-export.png"));
        registerNodeType("export-roi-3d-v2", ExportRoi3dAlgorithm2.class, JIPipe.RESOURCES.getIcon16URL("actions/document-export.png"));

        registerNodeType("ij3d-analyze-find-particles", FindParticles3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/tool_elliptical_selection.png"));
        registerNodeType("ij3d-labels-to-roi", Roi3dFromLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/tool_elliptical_selection.png"));

        registerNodeType("ij3d-measure-roi3d", ExtractRoi3dStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/statistics.png"));
        registerNodeType("ij3d-measure-pairwise-roi3d", ExtractRoi3dRelationStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/statistics.png"));

        registerNodeType("ij3d-filter-roi3d-list", FilterRoi3dListsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij3d-filter-roi3d-by-statistics", FilterRoi3dByStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));

        registerNodeType("ij3d-roi-split-explode", ExplodeRoi3dListAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij3d-roi-split-into-connected-components", SplitRoi3dIntoConnectedComponentsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij3d-roi-split-by-statistics", SplitRoi3dByStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));

        registerNodeType("ij3d-roi-filter-by-overlap", FilterRoi3dByOverlapOldAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij3d-roi-filter-by-overlap-v2", FilterRoi3dByOverlapAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));

        registerNodeType("ij3d-roi-merge", MergeRoi3dAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/rabbitvcs-merge.png"));

        registerNodeType("ij3d-roi-outline", OutlineRoi3dAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-connector.png"));
        registerNodeType("ij3d-roi-remove-border", RemoveBorderRoi3dAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));

        registerNodeType("ij3d-roi-change-properties-from-expressions", ChangeRoi3dPropertiesFromExpressionsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/stock_edit.png"));
        registerNodeType("ij3d-roi-change-properties-from-table", ChangeRoi3dPropertiesFromTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/stock_edit.png"));
        registerNodeType("ij3d-roi-change-properties", ChangeRoi3dPropertiesAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/stock_edit.png"));
        registerNodeType("ij3d-roi-calculator", Roi3dCalculatorAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/calculator.png"));
        registerNodeType("ij3d-roi-color-by-name", ColorRoi3dByNameAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/fill-color.png"));
        registerNodeType("ij3d-roi-color-by-statistics", ColorRoi3dByStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/fill-color.png"));

        registerNodeType("ij3d-roi-convert-2d-to-3d", Roi2dToRoi3dConverterAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-cuboid.png"));
        registerNodeType("ij3d-roi-convert-3d-to-2d", Roi3dToRoi2dConverterAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d.png"));
        registerNodeType("ij3d-roi-convert-to-mask", Roi3dToMaskAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij3d-roi-convert-to-labels", Roi3dToLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-jitter-color.png"));
        registerNodeType("ij3d-roi-convert-to-rgb", Roi3dToRGBAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/colormanagement.png"));
        registerNodeType("ij3d-roi-convert-to-3d-mesh", Roi3dTo3DMeshAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/shape-cuboid.png"));

        registerNodeType("ij3d-roi-extract-metadata", ExtractRoi3dMetadataAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/cm_extractfiles.png"));
        registerNodeType("ij3d-roi-set-metadata-from-table", SetRoi3dMetadataFromTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/cm_packfiles.png"));
        registerNodeType("ij3d-roi-remove-metadata", RemoveRoi3dMetadataAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));

        registerNodeType("ij3d-roi-remove-overlay", RemoveOverlay3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/editclear.png"));
        registerNodeType("ij3d-roi-render-overlay", RenderOverlay3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/color-management.png"));
        registerNodeType("ij3d-roi-set-overlay", SetOverlay3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/roi.png"));
        registerNodeType("ij3d-roi-extract-overlay", ExtractOverlay3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/roi.png"));

        registerNodeType("ij3d-math-distance-map-3d", DistanceMap3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-binary-watershed-3d-splitting", Watershed3DSplittingAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-binary-voronoi-3d", Voronoi3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-math-eroded-volume-fraction-3d", ErodedVolumeFraction3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-feature-maxima-local-3d", FindMaxima3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-feature-canny-edge-3d", EdgeFilter3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-feature-symmetry-3d", SymmetryFilter3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));

        registerNodeType("ij3d-filter-adaptive-3d", Fast3DFiltersAdaptiveAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-close-3d", Fast3DFiltersCloseAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-local-max-3d", Fast3DFiltersLocalMaxAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-max-3d", Fast3DFiltersMaxAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-mean-3d", Fast3DFiltersMeanAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-median-3d", Fast3DFiltersMedianAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-min-3d", Fast3DFiltersMinAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-open-3d", Fast3DFiltersOpenAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-sobel-3d", Fast3DFiltersSobelAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-top-hat-3d", Fast3DFiltersTopHatAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-variance-3d", Fast3DFiltersVarianceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));

        registerNodeType("ij3d-segmentation-watershed-3d", Watershed3DSegmentationAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/view-object-histogram-linear.png"));
        registerNodeType("ij3d-segmentation-nuclei-3d", NucleiSegmentation3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));

        registerNodeType("ij3d-segmentation-hysteresis", HysteresisSegmentation3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d-greyscale-mask.png"));
        registerEnumParameterType("ij3d-segmentation-iterative-thresholding:criteria-method", IterativeThreshold3DAlgorithm.CriteriaMethod.class, "3D iterative thresholding: criteria", "Available criteria");
        registerEnumParameterType("ij3d-segmentation-iterative-thresholding:threshold-method", IterativeThreshold3DAlgorithm.ThresholdMethod.class, "3D iterative thresholding: threshold", "Available threshold methods");
        registerEnumParameterType("ij3d-segmentation-iterative-thresholding:segment-results-method", IterativeThreshold3DAlgorithm.SegmentResultsMethod.class, "3D iterative thresholding: segment results method", "Available methods for segmenting the rsults");
        registerNodeType("ij3d-segmentation-iterative-thresholding", IterativeThreshold3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d-greyscale-mask.png"));
        registerEnumParameterType("ij3d-segmentation-spot3d:spot-segmentation-method", SpotSegmentation3DAlgorithm.SpotSegmentationMethod.class, "3D spot segmentation: spot segmentation method", "Available spot segmentation methods");
        registerNodeType("ij3d-segmentation-spot3d-constant", ConstantSpotSegmentation3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij3d-segmentation-spot3d-difference", DifferenceSpotSegmentation3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij3d-segmentation-spot3d-gaussian-fit", GaussianFitSpotSegmentation3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij3d-segmentation-spot3d-local-mean", LocalMeanSpotSegmentation3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d-greyscale-mask.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-3d";
    }

}
