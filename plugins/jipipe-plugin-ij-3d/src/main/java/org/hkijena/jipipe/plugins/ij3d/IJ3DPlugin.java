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
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.filesystem.FilesystemPlugin;
import org.hkijena.jipipe.plugins.ij3d.compat.ROI3DImageJExporter;
import org.hkijena.jipipe.plugins.ij3d.compat.ROI3DImageJImporter;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.ij3d.display.AddROI3DToManagerDataDisplayOperation;
import org.hkijena.jipipe.plugins.ij3d.imageviewer.ImageViewerUIROI3DDisplayApplicationSettings;
import org.hkijena.jipipe.plugins.ij3d.nodes.ImportROI3DAlgorithm;
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
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.ExportROI3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.ExportROI3DAlgorithm2;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.convert.*;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.filter.FilterRoi3DByOverlapAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.filter.FilterRoi3DByStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.filter.FilterRoi3DListsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.generate.FindParticles3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.generate.Roi3DFromLabelsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.measure.ExtractRoi3DRelationStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.measure.ExtractRoi3DStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.merge.MergeROI3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.metadata.ExtractROI3DMetadataAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.metadata.RemoveROI3DMetadataAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.metadata.SetROI3DMetadataFromTableAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.modify.*;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.process.OutlineRoi3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.process.RemoveBorderRoi3DAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.split.ExplodeRoi3DListAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.split.SplitRoi3DIntoConnectedComponentsAlgorithm;
import org.hkijena.jipipe.plugins.ij3d.nodes.segmentation.*;
import org.hkijena.jipipe.plugins.ij3d.utils.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.multiparameters.MultiParameterAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.scene3d.Scene3DPlugin;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.UIUtils;
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
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {

        registerApplicationSettingsSheet(new ImageViewerUIROI3DDisplayApplicationSettings());

        registerEnumParameterType("ij3d-measurement", ROI3DMeasurement.class, "3D Measurement", "A 3D object measurement");
        registerEnumParameterType("ij3d-measurement-column", ROI3DMeasurementColumn.class, "3D measurement column", "A 3D object measurement column");
        registerParameterType("ij3d-measurement-set", ROI3DMeasurementSetParameter.class, "3D Measurements", "A selection of 3D object measurements");
        registerEnumParameterType("ij3d-relation-measurement", ROI3DRelationMeasurement.class, "3D relation measurement", "Relation between two 3D objects");
        registerEnumParameterType("ij3d-relation-measurement-column", ROI3DRelationMeasurementColumn.class, "3D relation measurement column", "Relation between two 3D objects");
        registerParameterType("ij3d-relation-measurement-set", ROI3DRelationMeasurementSetParameter.class, "3D relation measurements", "A selection of measurements between two 3D objects");
        registerEnumParameterType("ij3d-roi-outline", ROI3DOutline.class, "3D ROI outline", "Outline algorithm for 3D ROI");

        registerDatatype("roi-3d-list", ROI3DListData.class, RESOURCES.getIcon16URLFromResources("data-type-roi3d.png"), new AddROI3DToManagerDataDisplayOperation());
        registerImageJDataImporter("import-roi-3d", new ROI3DImageJImporter(), null);
        registerImageJDataExporter("export-roi-3d", new ROI3DImageJExporter(), null);
        registerNodeType("import-roi-3d", ImportROI3DAlgorithm.class);
        registerNodeType("export-roi-3d", ExportROI3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("export-roi-3d-v2", ExportROI3DAlgorithm2.class, UIUtils.getIconURLFromResources("actions/document-export.png"));

        registerNodeType("ij3d-analyze-find-particles", FindParticles3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/tool_elliptical_selection.png"));
        registerNodeType("ij3d-labels-to-roi", Roi3DFromLabelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/tool_elliptical_selection.png"));

        registerNodeType("ij3d-measure-roi3d", ExtractRoi3DStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("ij3d-measure-pairwise-roi3d", ExtractRoi3DRelationStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));

        registerNodeType("ij3d-filter-roi3d-list", FilterRoi3DListsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij3d-filter-roi3d-by-statistics", FilterRoi3DByStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));

        registerNodeType("ij3d-roi-split-explode", ExplodeRoi3DListAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij3d-roi-split-into-connected-components", SplitRoi3DIntoConnectedComponentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));

        registerNodeType("ij3d-roi-filter-by-overlap", FilterRoi3DByOverlapAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));

        registerNodeType("ij3d-roi-merge", MergeROI3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/rabbitvcs-merge.png"));

        registerNodeType("ij3d-roi-outline", OutlineRoi3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-connector.png"));
        registerNodeType("ij3d-roi-remove-border", RemoveBorderRoi3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));

        registerNodeType("ij3d-roi-change-properties-from-expressions", ChangeRoi3DPropertiesFromExpressionsAlgorithm.class, UIUtils.getIconURLFromResources("actions/stock_edit.png"));
        registerNodeType("ij3d-roi-change-properties", ChangeRoi3DPropertiesAlgorithm.class, UIUtils.getIconURLFromResources("actions/stock_edit.png"));
        registerNodeType("ij3d-roi-calculator", ROI3DCalculatorAlgorithm.class, UIUtils.getIconURLFromResources("actions/calculator.png"));
        registerNodeType("ij3d-roi-color-by-name", ColorRoi3DByNameAlgorithm.class, UIUtils.getIconURLFromResources("actions/fill-color.png"));
        registerNodeType("ij3d-roi-color-by-statistics", ColorRoi3DByStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/fill-color.png"));

        registerNodeType("ij3d-roi-convert-2d-to-3d", Roi2DToRoi3DConverterAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-cuboid.png"));
        registerNodeType("ij3d-roi-convert-3d-to-2d", Roi3DToRoi2DConverterAlgorithm.class, UIUtils.getIconURLFromResources("data-types/imgplus-2d.png"));
        registerNodeType("ij3d-roi-convert-to-mask", Roi3DToMaskAlgorithm.class, UIUtils.getIconURLFromResources("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij3d-roi-convert-to-labels", Roi3DToLabelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-tweak-jitter-color.png"));
        registerNodeType("ij3d-roi-convert-to-rgb", Roi3DToRGBAlgorithm.class, UIUtils.getIconURLFromResources("actions/colormanagement.png"));
        registerNodeType("ij3d-roi-convert-to-3d-mesh", Roi3DTo3DMeshAlgorithm.class, UIUtils.getIconURLFromResources("actions/shape-cuboid.png"));

        registerNodeType("ij3d-roi-extract-metadata", ExtractROI3DMetadataAlgorithm.class, UIUtils.getIconURLFromResources("actions/cm_extractfiles.png"));
        registerNodeType("ij3d-roi-set-metadata-from-table", SetROI3DMetadataFromTableAlgorithm.class, UIUtils.getIconURLFromResources("actions/cm_packfiles.png"));
        registerNodeType("ij3d-roi-remove-metadata", RemoveROI3DMetadataAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));

        registerNodeType("ij3d-roi-remove-overlay", RemoveOverlay3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/editclear.png"));
        registerNodeType("ij3d-roi-render-overlay", RenderOverlay3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/color-management.png"));
        registerNodeType("ij3d-roi-set-overlay", SetOverlay3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/roi.png"));
        registerNodeType("ij3d-roi-extract-overlay", ExtractOverlay3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/roi.png"));

        registerNodeType("ij3d-math-distance-map-3d", DistanceMap3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-binary-watershed-3d-splitting", Watershed3DSplittingAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-binary-voronoi-3d", Voronoi3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-math-eroded-volume-fraction-3d", ErodedVolumeFraction3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-feature-maxima-local-3d", FindMaxima3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-feature-canny-edge-3d", EdgeFilter3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-feature-symmetry-3d", SymmetryFilter3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerNodeType("ij3d-filter-adaptive-3d", Fast3DFiltersAdaptiveAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-close-3d", Fast3DFiltersCloseAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-local-max-3d", Fast3DFiltersLocalMaxAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-max-3d", Fast3DFiltersMaxAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-mean-3d", Fast3DFiltersMeanAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-median-3d", Fast3DFiltersMedianAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-min-3d", Fast3DFiltersMinAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-open-3d", Fast3DFiltersOpenAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-sobel-3d", Fast3DFiltersSobelAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-top-hat-3d", Fast3DFiltersTopHatAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij3d-filter-variance-3d", Fast3DFiltersVarianceAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerNodeType("ij3d-segmentation-watershed-3d", Watershed3DSegmentationAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-object-histogram-linear.png"));
        registerNodeType("ij3d-segmentation-nuclei-3d", NucleiSegmentation3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerNodeType("ij3d-segmentation-hysteresis", HysteresisSegmentation3DAlgorithm.class, UIUtils.getIconURLFromResources("data-types/imgplus-2d-greyscale-mask.png"));
        registerEnumParameterType("ij3d-segmentation-iterative-thresholding:criteria-method", IterativeThreshold3DAlgorithm.CriteriaMethod.class, "3D iterative thresholding: criteria", "Available criteria");
        registerEnumParameterType("ij3d-segmentation-iterative-thresholding:threshold-method", IterativeThreshold3DAlgorithm.ThresholdMethod.class, "3D iterative thresholding: threshold", "Available threshold methods");
        registerEnumParameterType("ij3d-segmentation-iterative-thresholding:segment-results-method", IterativeThreshold3DAlgorithm.SegmentResultsMethod.class, "3D iterative thresholding: segment results method", "Available methods for segmenting the rsults");
        registerNodeType("ij3d-segmentation-iterative-thresholding", IterativeThreshold3DAlgorithm.class, UIUtils.getIconURLFromResources("data-types/imgplus-2d-greyscale-mask.png"));
        registerEnumParameterType("ij3d-segmentation-spot3d:spot-segmentation-method", SpotSegmentation3DAlgorithm.SpotSegmentationMethod.class, "3D spot segmentation: spot segmentation method", "Available spot segmentation methods");
        registerNodeType("ij3d-segmentation-spot3d-constant", ConstantSpotSegmentation3DAlgorithm.class, UIUtils.getIconURLFromResources("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij3d-segmentation-spot3d-difference", DifferenceSpotSegmentation3DAlgorithm.class, UIUtils.getIconURLFromResources("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij3d-segmentation-spot3d-gaussian-fit", GaussianFitSpotSegmentation3DAlgorithm.class, UIUtils.getIconURLFromResources("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij3d-segmentation-spot3d-local-mean", LocalMeanSpotSegmentation3DAlgorithm.class, UIUtils.getIconURLFromResources("data-types/imgplus-2d-greyscale-mask.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-3d";
    }

}
