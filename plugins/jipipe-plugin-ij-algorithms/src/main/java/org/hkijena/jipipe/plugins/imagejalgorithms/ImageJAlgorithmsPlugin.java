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

package org.hkijena.jipipe.plugins.imagejalgorithms;

import com.google.common.collect.Sets;
import de.biomedical_imaging.ij.steger.OverlapOption;
import ij.process.AutoThresholder;
import inra.ijpb.binary.ChamferWeights;
import inra.ijpb.binary.ChamferWeights3D;
import inra.ijpb.color.ColorMaps;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.Strel3D;
import inra.ijpb.morphology.directional.DirectionalFilter;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.forms.FormsPlugin;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.analyze.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.background.RollingBallBackgroundEstimator2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.binary.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.blur.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.calibration.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.colocalization.Coloc2Node;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.color.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.contrast.CLAHEContrastEnhancer;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.contrast.HistogramContrastEnhancerAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.contrast.IlluminationCorrection2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.contrast.ImageJContrastEnhancerAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.convert.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.convolve.ConvolveByImage2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.convolve.ConvolveByParameter2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.datasources.ImageStackFromFolder;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.datasources.OMEImageFromImagePlus;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.detect.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.dimensions.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.edge.CannyEdgeDetectorAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.edge.LaplacianEdgeDetectorAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.edge.SobelEdgeDetectorAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.enhance.BleachCorrectionAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.features.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.fft.FFT2DForwardTransform;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.fft.FFT2DInverseTransform;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.fft.FFT2DSwapQuadrants;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.fft.FFTBandPassFilter;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.forms.DrawMaskAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.forms.DrawROIAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.generate.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.io.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.labels.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.labels.filter.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.lut.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.macro.RunImageJMacroAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.masking.SetToColorAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.masking.SetToContentAwareAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.masking.SetToValueAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.math.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.math.distancemap.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.math.local.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.metadata.ChangeImageMetadataFromExpressionsAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.metadata.ExtractImageMetadataAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.metadata.RemoveImageMetadataAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.metadata.SetImageMetadataFromTableAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.misc.DataToPreviewAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.misc.RenderJIPipeProjectAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.montage.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.morphology.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.noise.AddNoise2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.noise.AddSaltAndPepperNoise2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.noise.DespeckleFilter2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.noise.RemoveOutliersFilter2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.ome.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.opticalflow.MSEBlockFlowAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.opticalflow.MSEGaussianFlowAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.opticalflow.PMCCBlockFlowAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.overlay.ExtractOverlayAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.overlay.RemoveOverlayAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.overlay.RenderOverlayAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.overlay.SetOverlayAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.turboreg.TurboRegRegistration2DReferencedAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.turboreg.TurboRegRegistration2DSingleAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.turboreg.TurboRegRegistrationAlgorithmRuleType;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.CropToRoi2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.Roi2DRelationMeasurement;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.Roi2DRelationMeasurementColumn;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.Roi2DRelationMeasurementSetParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.annotations.RoiPropertiesToAnnotationsAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.assemble.AssembleExtractedROIAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.assemble.ExtractFromROIAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.convert.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.draw.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.filter.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.generate.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.measure.CountROIAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.measure.ExtractRoi2DProfileAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.measure.ExtractRoi2DRelationStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.measure.ExtractRoi2DStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.merge.FilterAndMergeRoiByStatisticsScriptAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.merge.MergeRoiListsOrderedAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.merge.MergeRoiListsPairwiseOrAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.merge.MergeRoiListsUnorderedAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.modify.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.outline.InterpolateRoiAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.outline.OutlineRoiAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.outline.OutlineRoiConcaveHullMoreiraSantosAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.process.CopyRoi2DAcrossZCTAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.properties.ExtractROIMetadataAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.properties.RemoveROIMetadataAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.properties.SetROIMetadataFromTableAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.properties.SetRoiMetadataByStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.sort.SortAndExtractRoiByStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.sort.SortAndExtractRoiByStatisticsAlgorithm2;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.sort.SortRoiListByExpressionsAndMeasurementsAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.split.SplitRoi2DByStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.split.SplitRoi2dIntoIndividualListsAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.split.SplitMultiComponentRoi2dAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.split.SplitRoiConnectedComponentsAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.segment.ClassicWatershedSegmentationAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.segment.RidgeDetector2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.segment.SeededWatershedSegmentationAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.sharpen.LaplacianSharpen2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.sharpen.UnsharpMasking2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.statistics.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.color.ColorThresholdExpression2D;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.color.ManualHSBThreshold2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.color.ManualLABThreshold2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.color.ManualRGBThreshold2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.iterative.IterativeThresholdByROIStatistics2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.local.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.transform.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.LineMirror;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.OMEAccessorStorage;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.OrientationJGradientOperator;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.OrientationJVectorFieldType;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.turboreg.TurboRegTransformationType;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.ApplyDisplayContrastAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.DisplayRangeCalibrationAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.InvalidRoiOutlineBehavior;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.RoiLabel;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.RoiOutline;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.blending.ImageBlendLayer;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.blending.ImageBlendMode;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.HyperstackDimension;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.*;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameterList;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.util.LogicalOperation;
import org.hkijena.jipipe.plugins.strings.StringsPlugin;
import org.hkijena.jipipe.plugins.tables.TablesPlugin;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import sc.fiji.coloc.algorithms.AutoThresholdRegression;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Extension that adds ImageJ2 algorithms
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class ImageJAlgorithmsPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(ImageJAlgorithmsPlugin.class, "org/hkijena/jipipe/plugins/imagejalgorithms");

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:imagej-algorithms",
            JIPipe.getJIPipeVersion(),
            "ImageJ algorithms");

    /**
     * Contains registered OME accessors
     */
    public static final OMEAccessorStorage OME_ACCESSOR_STORAGE = new OMEAccessorStorage();

    public ImageJAlgorithmsPlugin() {
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, TablesPlugin.AS_DEPENDENCY, StringsPlugin.AS_DEPENDENCY, FormsPlugin.AS_DEPENDENCY, ImageJDataTypesPlugin.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public PluginCategoriesEnumParameterList getCategories() {
        return new PluginCategoriesEnumParameterList(PluginCategoriesEnumParameter.CATEGORY_IMAGE_ANALYSIS, PluginCategoriesEnumParameter.CATEGORY_IMAGE_ANNOTATION, PluginCategoriesEnumParameter.CATEGORY_ANALYSIS,
                PluginCategoriesEnumParameter.CATEGORY_ANNOTATION, PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION, PluginCategoriesEnumParameter.CATEGORY_FILTERING, PluginCategoriesEnumParameter.CATEGORY_MONTAGE, PluginCategoriesEnumParameter.CATEGORY_IMPORT_EXPORT, PluginCategoriesEnumParameter.CATEGORY_FIJI,
                PluginCategoriesEnumParameter.CATEGORY_IMAGE_SCIENCE, PluginCategoriesEnumParameter.CATEGORY_NOISE, PluginCategoriesEnumParameter.CATEGORY_TRANSFORM, PluginCategoriesEnumParameter.CATEGORY_BINARY, PluginCategoriesEnumParameter.CATEGORY_OBJECT_DETECTION, PluginCategoriesEnumParameter.CATEGORY_FEATURE_EXTRACTION,
                PluginCategoriesEnumParameter.CATEGORY_VISUALIZATION);
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        // --- Common Affiliations ---
        final JIPipeOrganizationMetadata loci = new JIPipeOrganizationMetadata.Builder()
                .name("Laboratory for Optical and Computational Instrumentation, University of Wisconsin at Madison, Madison, Wisconsin, USA")
                .ror("https://ror.org/01y2jtd41")
                .website("https://loci.wisc.edu")
                .build();

        final JIPipeOrganizationMetadata morgridge = new JIPipeOrganizationMetadata.Builder()
                .name("Morgridge Institute for Research, Madison, Wisconsin, USA")
                .ror("https://ror.org/05cb4rb43")
                .website("https://morgridge.org")
                .build();

        final JIPipeOrganizationMetadata nihInstr = new JIPipeOrganizationMetadata.Builder()
                .name("Section on Instrumentation, US National Institutes of Health, Bethesda, Maryland, USA")
                .ror("https://ror.org/01cwqze88")
                .website("https://www.nih.gov")
                .build();

        final JIPipeOrganizationMetadata lociMolBio = new JIPipeOrganizationMetadata.Builder()
                .name("Laboratory for Optical and Computational Instrumentation, Department of Molecular Biology")
                .build();

        final JIPipeOrganizationMetadata uwBME = new JIPipeOrganizationMetadata.Builder()
                .name("Department of Biomedical Engineering, Graduate School, University of Wisconsin at Madison, Madison, WI 53711")
                .build();

        final JIPipeOrganizationMetadata glencoe = new JIPipeOrganizationMetadata.Builder()
                .name("Glencoe Software, Inc., Seattle, WA 98101")
                .website("https://glencoesoftware.com")
                .build();

        final JIPipeOrganizationMetadata dundee = new JIPipeOrganizationMetadata.Builder()
                .name("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee DD1 5EH, Scotland, UK")
                .ror("https://ror.org/03h2bxq36")
                .website("https://www.dundee.ac.uk/cells-and-development")
                .build();

        final JIPipeOrganizationMetadata rupress = new JIPipeOrganizationMetadata.Builder()
                .name("The Rockefeller University Press, New York, NY 10065")
                .website("https://rupress.org")
                .build();

        final JIPipeOrganizationMetadata inraBiopolymers = new JIPipeOrganizationMetadata.Builder()
                .name("UR1268 Biopolymers, Interactions and Assemblies, INRA, Nantes, France")
                .website("https://www.inrae.fr")
                .build();

        final JIPipeOrganizationMetadata inraFood = new JIPipeOrganizationMetadata.Builder()
                .name("UMR782 Food Process Engineering and Microbiology, INRA, AgroParisTech, Thiverval-Grignon, France")
                .build();

        final JIPipeOrganizationMetadata jpb = new JIPipeOrganizationMetadata.Builder()
                .name("Institut Jean-Pierre Bourgin, INRA, AgroParisTech, CNRS, Université Paris-Saclay, Versailles, France")
                .build();

        final JIPipeOrganizationMetadata ikerbasque = new JIPipeOrganizationMetadata.Builder()
                .name("Basque Foundation for Science, Ikerbasque, Bilbao, Spain")
                .ror("https://ror.org/01cc3fy72")
                .website("https://www.ikerbasque.net")
                .build();

        final JIPipeOrganizationMetadata upv = new JIPipeOrganizationMetadata.Builder()
                .name("Computer Science and Artificial Intelligence Department, Basque Country University (UPV/EHU), Donostia-San Sebastian, Spain")
                .build();

        final JIPipeOrganizationMetadata dipc = new JIPipeOrganizationMetadata.Builder()
                .name("Donostia International Physics Center (DIPC), Donostia-San Sebastian, Spain")
                .website("https://dipc.ehu.es")
                .build();

        final JIPipeOrganizationMetadata sorbonne = new JIPipeOrganizationMetadata.Builder()
                .name("Sorbonne Universités, UPMC Univ Paris 06, UFR 927, Paris, France")
                .build();

        final JIPipeOrganizationMetadata uniKonstanz = new JIPipeOrganizationMetadata.Builder()
                .name("Department of Computer and Information Science, Universität Konstanz, Konstanz, Germany")
                .build();

        final JIPipeOrganizationMetadata nihAging = new JIPipeOrganizationMetadata.Builder()
                .name("National Institute on Aging, National Institutes of Health, Baltimore, Maryland, USA")
                .build();

        final JIPipeOrganizationMetadata kitware = new JIPipeOrganizationMetadata.Builder()
                .name("Kitware Inc., New York, New York, USA")
                .website("https://www.kitware.com")
                .build();

        final JIPipeOrganizationMetadata ucsbECE = new JIPipeOrganizationMetadata.Builder()
                .name("Department of Electrical and Computer Engineering, University of California Santa Barbara, USA")
                .build();

        final JIPipeOrganizationMetadata ncmir = new JIPipeOrganizationMetadata.Builder()
                .name("National Center for Microscopy and Imaging Research, University of California San Diego, USA")
                .build();

        final JIPipeOrganizationMetadata cmu = new JIPipeOrganizationMetadata.Builder()
                .name("Lane Center for Computational Biology, Carnegie Mellon University, Pittsburgh, Pennsylvania, USA")
                .build();

        final JIPipeOrganizationMetadata janelia = new JIPipeOrganizationMetadata.Builder()
                .name("Janelia Farm Research Campus, Howard Hughes Medical Institute, Ashburn, Virginia, USA")
                .build();

        final JIPipeOrganizationMetadata nist = new JIPipeOrganizationMetadata.Builder()
                .name("Biochemical Science Division, National Institute of Standards and Technology, Gaithersburg, Maryland, USA")
                .build();

        final JIPipeOrganizationMetadata uh = new JIPipeOrganizationMetadata.Builder()
                .name("Department of Electrical and Computer Engineering, University of Houston, Texas, USA")
                .build();

        final JIPipeOrganizationMetadata ucsf = new JIPipeOrganizationMetadata.Builder()
                .name("Department of Cellular and Molecular Pharmacology, University of California, San Francisco, USA")
                .build();

        final JIPipeOrganizationMetadata broad = new JIPipeOrganizationMetadata.Builder()
                .name("Imaging Platform, Broad Institute of Harvard and MIT, Cambridge, Massachusetts, USA")
                .website("https://www.broadinstitute.org")
                .build();

        final JIPipeOrganizationMetadata tugraz = new JIPipeOrganizationMetadata.Builder()
                .name("Graz University of Technology Institute of Technical Informatics, Graz, Austria")
                .build();

        final JIPipeOrganizationMetadata unsw = new JIPipeOrganizationMetadata.Builder()
                .name("School of Computer Science and Engineering & Graduate School of Biomedical Engineering, University of New South Wales, Sydney, Australia")
                .build();

        final JIPipeOrganizationMetadata epfl = new JIPipeOrganizationMetadata.Builder()
                .name("Biomedical Image Group (BIG), EPFL, Switzerland")
                .website("https://bigwww.epfl.ch")
                .build();

        // Author list using builder style and shared affiliations
        return new JIPipeAuthorMetadata.List(
                new JIPipeAuthorMetadata.Builder().firstName("Curtis T.").lastName("Rueden").affiliations(List.of(loci)).firstAuthor(true).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Johannes").lastName("Schindelin").affiliations(List.of(loci, morgridge)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Mark C.").lastName("Hiner").affiliations(List.of(loci)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Barry E.").lastName("DeZonia").affiliations(List.of(loci)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Alison E.").lastName("Walter").affiliations(List.of(loci, morgridge)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Ellen T.").lastName("Arena").affiliations(List.of(loci, morgridge)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Kevin W.").lastName("Eliceiri").affiliations(List.of(loci, morgridge)).firstAuthor(true).correspondingAuthor(true).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Caroline A.").lastName("Schneider").affiliations(List.of(loci)).firstAuthor(true).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Wayne S.").lastName("Rasband").affiliations(List.of(nihInstr)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Melissa").lastName("Linkert").affiliations(List.of(lociMolBio, uwBME, glencoe)).firstAuthor(true).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Chris").lastName("Allan").affiliations(List.of(dundee, glencoe)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Jean-Marie").lastName("Burel").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Will").lastName("Moore").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Andrew").lastName("Patterson").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Brian").lastName("Loranger").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Josh").lastName("Moore").affiliations(List.of(glencoe)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Carlos").lastName("Neves").affiliations(List.of(glencoe)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Donald").lastName("MacDonald").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Aleksandra").lastName("Tarkowska").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Caitlin").lastName("Sticco").affiliations(List.of(lociMolBio, uwBME)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Emma").lastName("Hill").affiliations(List.of(rupress)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Mike").lastName("Rossner").affiliations(List.of(rupress)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Jason R.").lastName("Swedlow").affiliations(List.of(dundee, glencoe)).correspondingAuthor(true).build(),
                new JIPipeAuthorMetadata.Builder().firstName("David").lastName("Legland").affiliations(List.of(inraBiopolymers, inraBiopolymers, inraFood)).firstAuthor(true).correspondingAuthor(true).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Ignacio").lastName("Arganda-Carreras").affiliations(List.of(jpb, ikerbasque, upv, dipc)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Philippe").lastName("Andrey").affiliations(List.of(jpb, sorbonne)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Michael R.").lastName("Berthold").affiliations(List.of(uniKonstanz)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Ilya G.").lastName("Goldberg").affiliations(List.of(nihAging)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Luis").lastName("Ibáñez").affiliations(List.of(kitware)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("B S.").lastName("Manjunath").affiliations(List.of(ucsbECE)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Maryann E.").lastName("Martone").affiliations(List.of(ncmir)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Robert F.").lastName("Murphy").affiliations(List.of(cmu)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Hanchuan").lastName("Peng").affiliations(List.of(janelia)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Anne L.").lastName("Plant").affiliations(List.of(nist)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Badrinath").lastName("Roysam").affiliations(List.of(uh)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Nico").lastName("Stuurman").affiliations(List.of(ucsf)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Anne E.").lastName("Carpenter").affiliations(List.of(broad)).correspondingAuthor(true).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Christian").lastName("Steger").affiliations(List.of(tugraz)).firstAuthor(true).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Erik").lastName("Meijering").affiliations(List.of(unsw)).firstAuthor(true).correspondingAuthor(true).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Daniel").lastName("Sage").affiliations(List.of(epfl)).firstAuthor(true).correspondingAuthor(true).build()
        );
    }

    @Override
    public StringList getDependencyCitations() {
        StringList result = new StringList();
        result.add("Rueden, C. T.; Schindelin, J. & Hiner, M. C. et al. (2017), \"ImageJ2: ImageJ for the next generation of scientific image data\", " +
                "BMC Bioinformatics 18:529");
        result.add("Schneider, C. A.; Rasband, W. S. & Eliceiri, K. W. (2012), \"NIH Image to ImageJ: 25 years of image analysis\", " +
                "Nature methods 9(7): 671-675");
        result.add("Melissa Linkert, Curtis T. Rueden, Chris Allan, Jean-Marie Burel, Will Moore, Andrew Patterson, Brian Loranger, Josh Moore, " +
                "Carlos Neves, Donald MacDonald, Aleksandra Tarkowska, Caitlin Sticco, Emma Hill, Mike Rossner, Kevin W. Eliceiri, " +
                "and Jason R. Swedlow (2010) Metadata matters: access to image data in the real world. The Journal of Cell Biology 189(5), 777-782");
        result.add("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
                "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413");
        result.add("Eliceiri K. V., Berthold M. R., Goldberg I. G., Ibanez L., Manjunath B. S., Martone M. E., Murphy R. F., Peng H., Plant A. L., Roysam B., Stuurmann N.," +
                " Swedlow J.R., Tomancak P., Carpenter A. E. (2012) Biological Imaging Software Tools Nature Methods 9(7), 697-710");
        result.add("Steger, C., 1998. An unbiased detector of curvilinear structures. IEEE Transactions on Pattern Analysis and Machine Intelligence, 20(2), pp.113–125.");
        result.add("FeatureJ by Erik Meijering. http://imagescience.org/meijering/software/featurej/");
        result.add("MTrackJ by Erik Meijering. https://imagescience.org/meijering/software/mtrackj/");
        result.add("RandomJ by Erik Meijering. https://imagescience.org/meijering/software/randomj/");
        result.add("ImageScience by Erik Meijering. https://imagescience.org/meijering/software/imagescience/");
        result.add("OrientationJ by Daniel Sage at the Biomedical Image Group (BIG), EPFL, Switzerland. https://bigwww.epfl.ch/demo/orientation/");
        return result;
    }

    @Override
    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return Arrays.asList(new JIPipeImageJUpdateSiteDependency("IJPB-plugins", "https://sites.imagej.net/IJPB-plugins/"),
                new JIPipeImageJUpdateSiteDependency("ImageScience", "https://sites.imagej.net/ImageScience/"));
    }

    @Override
    public String getName() {
        return "ImageJ algorithms";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates ImageJ algorithms into JIPipe");
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {

        registerEnumParameterType("ij1-export-image-to-web:file-format", ExportImageAlgorithm.FileFormat.class, "File format", "Exported file format.");
        registerEnumParameterType("ij1-export-table:file-format", ExportTableAlgorithm.FileFormat.class, "File format", "Exported file format.");
        registerParameterType("ome-accessor-type", OMEAccessorTypeEnumParameter.class, JIPipeParameterArchetype.Value, "OME metadata key", "Metadata from OME");
        registerParameterType("ome-accessor", OMEAccessorParameter.class, JIPipeParameterArchetype.Value, "OME metadata query", "Queries metadata from OME", JIPipeDesktopOMEAccessorParameterEditorUI.class);

        registerIOAlgorithms();
        registerBlurAlgorithms();
        registerColorAlgorithms();
        registerEdgeAlgorithms();
        registerContrastAlgorithms();
        registerFeatureAlgorithms();
        registerMathAlgorithms();
        registerMorphologyAlgorithms();
        registerBinaryAlgorithms();
        registerNoiseAlgorithms();
        registerBackgroundAlgorithms();
        registerSharpenAlgorithms();
        registerThresholdAlgorithms();
        registerDimensionAlgorithms();
        registerAnalysisAlgorithms();
        registerFFTAlgorithms();
        registerTransformationAlgorithms();
        registerConvolutionAlgorithms();
        registerROIAlgorithms();
        registerLUTAlgorithms();
        registerOpticalFlowAlgorithms();
        registerFormAlgorithms();
        registerConverterAlgorithms();
        registerLabelAlgorithms();
        registerSegmentationAlgorithms();
        registerCalibrationAlgorithms();
        registerColocalizationAlgorithms();
        registerMetadataAlgorithms();
        registerRegistrationAlgorithms();
        registerMaskingAlgorithms();
        registerEnhancementAlgorithms();

        registerNodeType("ij1-generate-missing-results-table", GenerateMissingTablesAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/image-auto-adjust.png"));
        registerNodeType("ij1-generate-missing-results-table-2", GenerateMissingTablesAlgorithm2.class, JIPipe.RESOURCES.getIcon16URL("actions/image-auto-adjust.png"));
        registerNodeType("ij1-generate-filter-kernel", GenerateStructureElement2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/morphology.png"));
        registerNodeType("ij1-generate-filter-kernel-3d", GenerateStructureElement3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/morphology.png"));
        registerNodeType("ij1-data-to-preview", DataToPreviewAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/viewimage.png"));
        registerNodeType("render-jipipe-project", RenderJIPipeProjectAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/viewimage.png"));
        registerNodeType("external-imagej-macro", RunImageJMacroAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/imagej.png"));

        registerNodeType("ome-image-from-image-plus", OMEImageFromImagePlus.class);

        registerNodeType("ome-annotate-with-metadata", AnnotateOMEWithMetadataAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));
        registerNodeType("ome-annotate-data-with-metadata", AnnotateDataWithOMEMetadataAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));
        registerNodeType("ome-extract-metadata-as-table", OMEMetadataToTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/table.png"));

        // Register enum parameters
        registerGlobalEnums();

        // Register other parameters
        registerGlobalParameters();

        // Register examples
        registerNodeExamplesFromResources(RESOURCES, "examples");
        registerProjectTemplatesFromResources(RESOURCES, "templates");
    }

    private void registerEnhancementAlgorithms() {
        registerNodeType("ij1-enhance-bleach-correction", BleachCorrectionAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/wand-magic-sparkles.png"));
        registerEnumParameterType("ij1-enhance-bleach-correction:method", BleachCorrectionAlgorithm.Method.class, "Bleach correction method", "Available methods");
    }

    private void registerMaskingAlgorithms() {
        registerNodeType("ij1-color-set-to-color", SetToColorAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/color-fill.png"));
        registerNodeType("ij1-color-set-to-grayscale-value", SetToValueAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/color-fill.png"));
        registerNodeType("ij1-color-set-to-content-aware-2d", SetToContentAwareAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/color-fill.png"));
        registerEnumParameterType("ij1-color-set-to-content-aware-2d:method", SetToContentAwareAlgorithm.Method.class, "Method", "Content-aware fill method");
    }

    private void registerRegistrationAlgorithms() {
        // Algorithms temporarily deactivated (no time to develop them)
//        registerEnumParameterType("ij1-simple-image-registration:model", SimpleImageRegistrationModel.class, "Simple image registration model", "Image registration model");
//        registerEnumParameterType("ij1-simple-image-registration:feature-model", SimpleImageRegistrationFeatureModel.class, "Simple image registration feature model", "Model to extract image features");
//        registerEnumParameterType("ij1-bunwarpj-registration:max-scale-deformation", BUnwarpJMaxScaleDeformation.class, "BUnwarpJ max scale deformation", "");
//        registerEnumParameterType("ij1-bunwarpj-registration:min-scale-deformation", BUnwarpJMinScaleDeformation.class, "BUnwarpJ min scale deformation", "");
//        registerEnumParameterType("ij1-bunwarpj-registration:mode", BUnwarpJMode.class, "BUnwarpJ mode", "");
//        registerNodeType("ij1-simple-image-registration", SimpleImageRegistrationAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-shear-right.png"));

        registerEnumParameterType("ij1-turbo-reg:transformation-type", TurboRegTransformationType.class, "TurboReg transformation", "Transformation type");
        registerEnumParameterType("ij1-turbo-reg-image-registration:rule-type", TurboRegRegistrationAlgorithmRuleType.class, "TurboReg registration rule type", "Determines the behavior of the algorithm");
        registerNodeType("ij1-turbo-reg-image-registration", TurboRegRegistration2DReferencedAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-shear-right.png"));
        registerNodeType("ij1-turbo-reg-image-registration-io", TurboRegRegistration2DSingleAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-shear-right.png"));
    }

    @Override
    public void postprocess(JIPipeProgressInfo progressInfo) {
        super.postprocess(progressInfo);

        // Register examples
        registerNodeExample(FastImageArithmeticsAlgorithm.class, "Add images", node -> node.configureTwoInputExample("I1 + I2"));
        registerNodeExample(FastImageArithmeticsAlgorithm.class, "Subtract images", node -> node.configureTwoInputExample("I1 - I2"));
        registerNodeExample(FastImageArithmeticsAlgorithm.class, "Multiply images", node -> node.configureTwoInputExample("I1 * I2"));
        registerNodeExample(FastImageArithmeticsAlgorithm.class, "Divide images", node -> node.configureTwoInputExample("I1 / I2"));
        registerNodeExample(FastImageArithmeticsAlgorithm.class, "Image difference", node -> node.configureTwoInputExample("ABS(I1 - I2)"));
        registerNodeExample(FastImageArithmeticsAlgorithm.class, "Pixel-wise min", node -> node.configureTwoInputExample("MIN(I1, I2)"));
        registerNodeExample(FastImageArithmeticsAlgorithm.class, "Pixel-wise max", node -> node.configureTwoInputExample("MAX(I1, I2)"));
        registerNodeExample(FastImageArithmeticsAlgorithm.class, "Pixel-wise average", node -> node.configureTwoInputExample("(I1 + I2) / 2"));
        registerNodeExample(FastImageArithmeticsAlgorithm.class, "Pixel-wise AND", node -> node.configureTwoInputExample("AND(I1, I2)"));
        registerNodeExample(FastImageArithmeticsAlgorithm.class, "Pixel-wise OR", node -> node.configureTwoInputExample("OR(I1, I2)"));
        registerNodeExample(FastImageArithmeticsAlgorithm.class, "Pixel-wise XOR", node -> node.configureTwoInputExample("XOR(I1, I2)"));

        registerNodeExample(SetToValueAlgorithm.class, "Inside mask", node -> node.setTargetArea(ImageROITargetArea.InsideMask));
        registerNodeExample(SetToValueAlgorithm.class, "Outside mask", node -> node.setTargetArea(ImageROITargetArea.OutsideMask));
        registerNodeExample(SetToValueAlgorithm.class, "Inside ROI", node -> node.setTargetArea(ImageROITargetArea.InsideRoi));
        registerNodeExample(SetToValueAlgorithm.class, "Outside ROI", node -> node.setTargetArea(ImageROITargetArea.OutsideRoi));
        registerNodeExample(SetToColorAlgorithm.class, "Inside mask", node -> node.setTargetArea(ImageROITargetArea.InsideMask));
        registerNodeExample(SetToColorAlgorithm.class, "Outside mask", node -> node.setTargetArea(ImageROITargetArea.OutsideMask));
        registerNodeExample(SetToColorAlgorithm.class, "Inside ROI", node -> node.setTargetArea(ImageROITargetArea.InsideRoi));
        registerNodeExample(SetToColorAlgorithm.class, "Outside ROI", node -> node.setTargetArea(ImageROITargetArea.OutsideRoi));

        // Init the OME accessors (needs parameter types)
        OME_ACCESSOR_STORAGE.initialize(progressInfo.resolve("Initializing OME data access storage"));
    }

    private void registerMetadataAlgorithms() {
        registerNodeType("ij1-image-extract-metadata", ExtractImageMetadataAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/cm_extractfiles.png"));
        registerNodeType("ij1-image-set-metadata-from-table", SetImageMetadataFromTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/cm_packfiles.png"));
        registerNodeType("ij1-image-remove-metadata", RemoveImageMetadataAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-image-set-properties-from-expressions", ChangeImageMetadataFromExpressionsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/edit.png"));
    }

    private void registerColocalizationAlgorithms() {
        registerEnumParameterType("ij1-colocalization-coloc2:auto-threshold-regression-implementation",
                AutoThresholdRegression.Implementation.class, "Auto threshold regression implementation", "Coloc2 Auto threshold regression");
        registerNodeType("ij1-colocalization-coloc2", Coloc2Node.class, JIPipe.RESOURCES.getIcon16URL("actions/color-gradient.png"));
    }

    private void registerCalibrationAlgorithms() {
        registerEnumParameterType("ij1-calibration-draw-scale-bar:location", ScaleBarGenerator.ScaleBarPosition.class, "Scale bar location", "Location of the scale bar");

        registerNodeType("ij1-calibration-set-physical-dimensions", SetPhysicalDimensionsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-geometry-show-measuring-info.png"));
        registerNodeType("ij1-calibration-copy-physical-dimensions", CopyPhysicalDimensionsBetweenImagesAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-geometry-show-measuring-info.png"));
        registerNodeType("ij1-calibration-set-physical-dimensions-from-expressions", SetPhysicalDimensionsByExpressionsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-geometry-show-measuring-info.png"));
        registerNodeType("ij1-calibration-set-physical-dimensions-from-annotations", SetPhysicalDimensionsByAnnotationsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-geometry-show-measuring-info.png"));
        registerNodeType("ij1-calibration-draw-scale-bar", DrawScaleBarAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-geometry-show-measuring-info.png"));
    }

    private void registerSegmentationAlgorithms() {
        registerNodeType("ij1-segment-classic-watershed", ClassicWatershedSegmentationAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/view-object-histogram-linear.png"));
        registerNodeType("ij1-segment-seeded-watershed", SeededWatershedSegmentationAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/view-object-histogram-linear.png"));
        registerNodeType("ij1-segment-ridge-detector-2d", RidgeDetector2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-geometry-mirror.png"));
        registerEnumParameterType("ij1-segment-ridge-detector-2d:overlap-resolver", OverlapOption.class, "Overlap detector", "Method for overlap detection");
    }

    private void registerLabelAlgorithms() {
        registerEnumParameterType("ij1-label-color-maps:common-label-maps", ColorMaps.CommonLabelMaps.class, "Color map", "A color map for labels");
        registerEnumParameterType("ij1-labels-to-roi:method", LabelsToROIAlgorithm.Method.class, "Label to ROI method", "A method that converts a label image to ROI");

        registerNodeType("ij1-labels-to-rgb", LabelsToRGBAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/colormanagement.png"));
        registerNodeType("ij1-labels-to-roi", LabelsToROIAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/roi.png"));
        registerNodeType("ij1-labels-to-mask", LabelsToMaskAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij1-labels-get-label-boundaries", GetLabelBoundariesAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-stroke.png"));
        registerNodeType("ij1-labels-remove-border-labels", RemoveBorderLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-labels-replace-labels", ReplaceLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/edit.png"));
        registerNodeType("ij1-labels-replace-labels-by-table", ReplaceLabelsByTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/edit.png"));
        registerNodeType("ij1-labels-merge-labels", MergeLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/merge.png"));
        registerNodeType("ij1-labels-filter-labels-by-id", FilterLabelsByIdAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-labels-filter-filter-by-expression-2d", FilterLabelsByExpression2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-labels-filter-filter-by-expression-3d", FilterLabelsByExpression3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-labels-crop-labels", CropLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/image-crop.png"));
        registerNodeType("ij1-labels-remap", RemapLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-visible.png"));
        registerNodeType("ij1-labels-expand-labels", ExpandLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-push.png"));
        registerNodeType("ij1-labels-separate-touching-labels", SeparateTouchingLabels2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-push.png"));
        registerNodeType("ij1-labels-remove-largest-label", RemoveLargestLabelAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-labels-keep-largest-label", KeepLargestLabelAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-labels-dilate-labels", DilateLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/morphology.png"));
        registerNodeType("ij1-labels-extract-statistics", ExtractLabelStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-labels-filter-by-statistics", FilterLabelsByStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-labels-filter-by-mask", FilterLabelsByMaskAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-labels-filter-by-overlap", FilterLabelsByOverlapAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-labels-overlap-statistics", OverlapMeasureLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-labels-annotate-with-overlap-statistics", AnnotateWithOverlapMeasureLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-labels-merge-small-labels", MergeSmallLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-labels-merge-labels-to-thickness", MergeLabelsToThicknessAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-labels-merge-labels-to-bins", MergeLabelsToBinsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-labels-filter-labels-by-thickness", FilterLabelsByThicknessAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-labels-separate", SeparateLabelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-labels-split-connected-components-2d", SplitLabelsConnectedComponents2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
    }

    private void registerConverterAlgorithms() {
        registerNodeType("ij-convert-image", ConvertImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/view-refresh.png"));
        registerNodeType("ij-convert-image-to-8-bit", ConvertImageTo8BitAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-greyscale-8u.png"));
        registerNodeType("ij-convert-image-to-16-bit", ConvertImageTo16BitAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-greyscale-16u.png"));
        registerNodeType("ij-convert-image-to-32-bit", ConvertImageTo32BitAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-greyscale-32f.png"));
        registerNodeType("ij-convert-image-to-hsb-colors", ConvertImageToHSBAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-color-hsb.png"));
        registerNodeType("ij-convert-image-to-rgb-colors", ConvertImageToRGBAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-color-rgb.png"));
        registerNodeType("ij-convert-image-to-lab-colors", ConvertImageToLABAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-color-lab.png"));
        registerNodeType("ij1-convert-image-to-table", ImageToTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/table.png"));
        registerNodeType("ij1-convert-multiple-images-to-table", MultipleImagesToTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/table.png"));
        registerNodeType("ij1-convert-table-to-image", TableToImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/table.png"));
        registerNodeType("ij1-convert-image-to-matrix", ImageToMatrixAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/table.png"));
        registerNodeType("ij1-convert-matrix-to-image", MatrixToImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/table.png"));
        registerNodeType("ij1-convert-table-column-to-image", TableColumnToImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/table.png"));
        registerNodeType("ij1-convert-image-to-table-column", ImageToTableColumnAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/table.png"));
        registerNodeType("ij1-extract-ome-image-xml", ExtractOMEXMLAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/dialog-xml-editor.png"));
        registerNodeType("ij1-extract-ome-image-roi", ExtractOMEROIAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/roi.png"));

        registerNodeType("ij-convert-image-to-8-bit-ij-auto-contrast", ConvertImageTo8BitAutoContrastAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-greyscale-8u.png"));
        registerNodeType("ij-convert-image-to-16-bit-ij-auto-contrast", ConvertImageTo16BitAutoContrastAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-greyscale-16u.png"));
    }

    private void registerFormAlgorithms() {
        registerNodeType("ij-form-draw-mask", DrawMaskAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-brush.png"));
        registerNodeType("ij-form-draw-rois", DrawROIAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-brush.png"));
    }

    private void registerOpticalFlowAlgorithms() {
        registerNodeType("ij-optical-flow-mse-block-flow", MSEBlockFlowAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-rotate.png"));
        registerNodeType("ij-optical-flow-mse-gaussian-flow", MSEGaussianFlowAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-rotate.png"));
        registerNodeType("ij-optical-flow-pmcc-block-flow", PMCCBlockFlowAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-rotate.png"));
    }

    private void registerIOAlgorithms() {
        registerNodeType("ij-imgplus-from-gui", ImagePlusFromGUI.class, JIPipe.RESOURCES.getIcon16URL("apps/imagej.png"));
        registerNodeType("ij-imgplus-to-gui", ImagePlusToGUI.class, JIPipe.RESOURCES.getIcon16URL("apps/imagej.png"));
        registerNodeType("ij-results-table-from-gui", ResultsTableFromGUI.class, JIPipe.RESOURCES.getIcon16URL("apps/imagej.png"));
        registerNodeType("ij-results-table-to-gui", ResultsTableToGUI.class, JIPipe.RESOURCES.getIcon16URL("apps/imagej.png"));
        registerNodeType("ij-roi-from-gui", ROIFromGUI.class, JIPipe.RESOURCES.getIcon16URL("apps/imagej.png"));
        registerNodeType("ij-roi-to-gui", ROIToGUI.class, JIPipe.RESOURCES.getIcon16URL("apps/imagej.png"));
        registerNodeType("ij-import-stack", ImageStackFromFolder.class, JIPipe.RESOURCES.getIcon16URL("apps/imagej.png"));

        registerNodeType("iji-export-image-to-web", ExportImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/document-export.png"));
        registerNodeType("iji-export-image-v2", ExportImage2Algorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/document-export.png"));
        for (ExportImageAlgorithm.FileFormat format : ExportImageAlgorithm.FileFormat.values()) {
            registerNodeExample(ExportImage2Algorithm.class, format.name(), node -> node.setFileFormat(format));
        }

        registerNodeType("iji-export-image-to-web:directory-slot", ExportImageDirectorySlotAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/document-export.png"));
        registerNodeType("iji-export-roi-list", ExportROIAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/document-export.png"));
        registerNodeType("iji-export-roi-list-v2", ExportROIAlgorithm2.class, JIPipe.RESOURCES.getIcon16URL("actions/document-export.png"));
        registerNodeType("iji-export-table", ExportTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/document-export.png"));
        registerNodeType("iji-export-table-v2", ExportTableAlgorithm2.class, JIPipe.RESOURCES.getIcon16URL("actions/document-export.png"));
        registerNodeType("iji-export-table-as-xlsx", ExportTableAsXLSXAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/document-export.png"));
        registerNodeType("iji-export-table-as-xlsx-v2", ExportTableAsXLSXAlgorithm2.class, JIPipe.RESOURCES.getIcon16URL("actions/document-export.png"));
        for (ExportTableAlgorithm.FileFormat format : ExportTableAlgorithm.FileFormat.values()) {
            registerNodeExample(ExportTableAlgorithm2.class, format.name(), node -> node.setFileFormat(format));
        }

        registerNodeType("ij-import-from-imagej", RunImageJImporterAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/imagej.png"));
        registerNodeType("ij-export-to-imagej", RunImageJExporterAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/imagej.png"));
    }

    private void registerLUTAlgorithms() {
        registerNodeType("ij1-remove-lut", RemoveLUTAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/paint-gradient-linear.png"));
        registerNodeType("ij1-apply-lut", ApplyLUTAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/paint-gradient-linear.png"));
        registerNodeType("ij1-set-lut-from-colors", SetLUTFromColorsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/paint-gradient-linear.png"));
        registerNodeType("ij1-set-lut-from-color-map", SetLUTFromColorMapAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/paint-gradient-linear.png"));
        registerNodeType("ij1-set-lut-from-color-image", SetLUTFromImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/paint-gradient-linear.png"));
        registerNodeType("ij1-invert-lut", LUTInverterAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-inverse.png"));
        registerNodeType("ij1-render-color-map", GenerateLUTImageFromColorMap.class, JIPipe.RESOURCES.getIcon16URL("actions/paint-gradient-linear.png"));
    }

    private void registerGlobalParameters() {
        registerParameterType("ij1:measurement-column-sort-order",
                ImageJMeasurementColumnSortOrder.class,
                JIPipeParameterArchetype.Value, ImageJMeasurementColumnSortOrder::new,
                o -> new ImageJMeasurementColumnSortOrder((ImageJMeasurementColumnSortOrder) o),
                "Measurement column sort order",
                "Describes how a data is sorted by a measurement column",
                null);
        registerParameterType("ij1:measurement-column-sort-order-list",
                ImageJMeasurementColumnSortOrderList.class,
                JIPipeParameterArchetype.List, ImageJMeasurementColumnSortOrderList::new,
                o -> new ImageJMeasurementColumnSortOrderList((ImageJMeasurementColumnSortOrderList) o),
                "Measurement column sort order list",
                "List of measurement column sort orders",
                null);
    }

    private void registerGlobalEnums() {
        registerEnumParameterType("ij1-interpolation-method", InterpolationMethod.class,
                "Interpolation method", "Available interpolation methods");
        registerEnumParameterType("ij1-measurement", ImageJMeasurement.class,
                "Measurement", "Available measurements");
        registerEnumParameterType("ij1-measurement-column", ImageJMeasurementColumn.class,
                "Measurement column", "Available measurement columns");
        registerEnumParameterType("ij1-calibration-mode", ImageJCalibrationMode.class,
                "Contrast calibration", "Methods to apply display range calibration");
        registerParameterType("ij1-measurement-set", ImageJMeasurementsSetParameter.class,
                JIPipeParameterArchetype.MultiSelect, null,
                null,
                "Measurements",
                "Selectable measurements",
                null);
        registerEnumParameterType("ij1-hyperstack-dimension",
                HyperstackDimension.class,
                "Hyperstack dimension",
                "Dimension assigned to a plane within a Hyperstack");
        registerParameterType("ij1-hyperstack-dimension:pair",
                HyperstackDimensionPairParameter.class,
                JIPipeParameterArchetype.Value, HyperstackDimensionPairParameterList.class,
                null,
                null,
                "Hyperstack dimension",
                "Dimension assigned to a plane within a Hyperstack",
                null);
        registerEnumParameterType("ij1-neighborhood-2d",
                Neighborhood2D.class,
                "2D neighborhood",
                "A 2D neighborhood");
        registerEnumParameterType("ij1-neighborhood-3d",
                Neighborhood3D.class,
                "3D neighborhood",
                "A 3D neighborhood");
        registerEnumParameterType("ij1-neighborhood-2d-3d",
                Neighborhood2D3D.class,
                "2D/3D neighborhood",
                "A 2D/3D neighborhood");
        registerEnumParameterType("ij1-roi-target", ImageROITargetArea.class,
                "Target area", "Defines an area where an algorithm is applied");
        registerEnumParameterType("ij1-source-wrap-mode",
                SourceWrapMode.class,
                "Border pixels mode",
                "Determines how source pixels at borders are acquired.");
        registerEnumParameterType("ij1-overlap-statistics",
                OverlapStatistics.class,
                "Overlap statistics",
                "Measurements of similarity/error between two binary or label images.");
        registerParameterType("ij1-overlap-statistics:set",
                OverlapStatisticsSetParameter.class,
                JIPipeParameterArchetype.MultiSelect, "Overlap statistics set",
                "Measurements of similarity/error between two binary or label images.");
    }

    private void registerROIAlgorithms() {
        registerEnumParameterType("ij-roi-relation-measurement", Roi2DRelationMeasurement.class, "2D relation measurement", "Relation between two 2D objects");
        registerEnumParameterType("ij-roi-relation-measurement-column", Roi2DRelationMeasurementColumn.class, "2D relation measurement column", "Relation between two 2D objects");
        registerParameterType("ij-roi-relation-measurement-set", Roi2DRelationMeasurementSetParameter.class, JIPipeParameterArchetype.MultiSelect, "2D relation measurements", "A selection of measurements between two 2D objects");

        registerNodeType("ij1-roi-from-rectangles", DefineRectangularRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-from-rectangles-referenced", ReferencedDefineRectangularRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-append-rectangles", AppendRectangularRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-append-rectangles-referenced", ReferencedAppendRectangularRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-split", SplitMultiComponentRoi2dAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-roi-split-into-connected-components", SplitRoiConnectedComponentsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-roi-split-by-statistics", SplitRoi2DByStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeExample(SplitRoi2DByStatisticsAlgorithm.class, "Split by frame", node -> node.setClassifier(new JIPipeExpressionParameter("t")));
        registerNodeExample(SplitRoi2DByStatisticsAlgorithm.class, "Split by channel", node -> node.setClassifier(new JIPipeExpressionParameter("c")));
        registerNodeExample(SplitRoi2DByStatisticsAlgorithm.class, "Split by depth", node -> node.setClassifier(new JIPipeExpressionParameter("z")));
        registerNodeType("ij1-roi-explode", SplitRoi2dIntoIndividualListsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-roi-merge", MergeRoiListsUnorderedAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/merge.png"));
        registerNodeType("ij1-roi-combine", MergeRoiListsOrderedAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/merge.png"));
        registerNodeType("ij1-roi-merge-pairwise-or", MergeRoiListsPairwiseOrAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/asterisk.png"));
        registerNodeType("ij1-roi-calculator", RoiCalculatorAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/calculator.png"));
        registerNodeExample(RoiCalculatorAlgorithm.class, "Merge into one ROI", node -> {
            node.setOperation(LogicalOperation.LogicalOr);
            node.setSplitAfterwards(false);
            node.setApplyPerChannel(false);
            node.setApplyPerFrame(false);
            node.setApplyPerSlice(false);
        });
        registerNodeType("ij1-roi-to-mask-unreferenced", UnreferencedRoiToMaskAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-roi-to-mask", RoiToMaskAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));

        registerNodeType("ij1-roi-outline", OutlineRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-connector.png"));
        for (RoiOutline outline : RoiOutline.values()) {
            registerNodeExample(OutlineRoiAlgorithm.class, outline.toString(), node -> node.setOutline(outline));
        }
        registerNodeType("ij1-roi-outline-concave-hull-moreira-santos", OutlineRoiConcaveHullMoreiraSantosAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-connector.png"));
        registerNodeType("ij1-roi-interpolate", InterpolateRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-connector.png"));

        registerNodeType("ij1-roi-crop-list", CropRoiListAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/image-crop.png"));
        registerNodeType("ij1-roi-to-centroid", RoiToCentroidAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-connector.png"));
        registerNodeType("ij1-roi-remove-bordering", RemoveBorderRoi2dAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/bordertool.png"));
        registerNodeType("ij1-roi-statistics", ExtractRoi2DStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/statistics.png"));
        registerNodeType("ij1-roi-relation-2d", ExtractRoi2DRelationStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/statistics.png"));
        registerNodeType("ij1-roi-count", CountROIAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/statistics.png"));
        registerNodeType("ij1-roi-filter-statistics", FilterRoi2DByStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-roi-filter-slice", SliceRoi2dListAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-roi-filter-by-name", FilterRoi2dByNameAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-roi-color-statistics", ColorRoiByStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/fill-color.png"));
        registerNodeType("ij1-roi-color-by-name", ColorRoiByNameAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/fill-color.png"));
        registerNodeType("ij1-roi-sort-and-extract-statistics", SortAndExtractRoiByStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/view-sort.png"));
        registerNodeType("ij1-roi-sort-and-extract-statistics-2", SortAndExtractRoiByStatisticsAlgorithm2.class, JIPipe.RESOURCES.getIcon16URL("actions/view-sort.png"));
        registerNodeType("ij1-roi-sort-by-statistics-expression", SortRoiListByExpressionsAndMeasurementsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/view-sort.png"));
        registerNodeType("ij1-roi-set-properties", ChangeRoiPropertiesAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/document-edit.png"));
        registerNodeExample(ChangeRoiPropertiesAlgorithm.class, "Make ROI show in all channels", node -> {
            node.setPositionC(new OptionalIntegerParameter(true, 0));
        });
        registerNodeExample(ChangeRoiPropertiesAlgorithm.class, "Make ROI show in all Z-slices", node -> {
            node.setPositionZ(new OptionalIntegerParameter(true, 0));
        });
        registerNodeExample(ChangeRoiPropertiesAlgorithm.class, "Make ROI show in all frames", node -> {
            node.setPositionT(new OptionalIntegerParameter(true, 0));
        });
        registerNodeExample(ChangeRoiPropertiesAlgorithm.class, "Make ROI show in all channels/frames/slices", node -> {
            node.setPositionC(new OptionalIntegerParameter(true, 0));
            node.setPositionZ(new OptionalIntegerParameter(true, 0));
            node.setPositionT(new OptionalIntegerParameter(true, 0));
        });
        registerNodeType("ij1-roi-scale", ScaleRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-scale.png"));
        registerNodeType("ij1-roi-rotate", RotateRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-rotate.png"));
        registerNodeType("ij1-roi-set-properties-from-annotation", ChangeRoiPropertiesFromAnnotationsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/document-edit.png"));
        registerNodeType("ij1-roi-set-properties-from-expressions", ChangeRoiPropertiesFromExpressionsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/document-edit.png"));
        registerNodeType("ij1-roi-set-properties-from-table", ChangeRoiPropertiesFromTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/document-edit.png"));
        registerNodeType("ij1-roi-to-rgb-unreferenced", UnreferencedRoiToRGBAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/color-management.png"));
        registerNodeType("ij1-roi-to-rgb", RoiToRGBAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/color-management.png"));
        registerNodeType("ij1-roi-filter-statistics-script", FilterRoi2dByStatisticsScriptAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));
        registerNodeType("ij1-roi-filter-and-merge-statistics-script", FilterAndMergeRoiByStatisticsScriptAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));
        registerNodeType("ij1-roi-from-table-rectangular", TableToRectangularROIAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-from-table-circle", TableToCircularROIAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-circle.png"));
        registerNodeType("ij1-roi-from-table-line", TableToLineROIAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-line.png"));
        registerNodeType("ij1-roi-from-table-point", TableToPointROIAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/labplot-xy-curve-points.png"));
        registerNodeType("ij1-roi-from-table-text", TableToTextROIAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/edit-select-text.png"));
        registerNodeType("ij1-roi-set-image", SetRoiImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/viewimage.png"));
        registerNodeType("ij1-roi-get-image", GetRoiImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/viewimage.png"));
        registerNodeType("ij1-roi-unset-image", UnsetRoiImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/delete.png"));
        registerNodeType("ij1-roi-from-mask", MaskToRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij1-roi-extract-from-roi", ExtractFromROIAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/image-crop.png"));
        registerNodeType("ij1-roi-assemble-from-roi", AssembleExtractedROIAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-image.png"));
        registerNodeType("ij1-roi-to-annotations", RoiPropertiesToAnnotationsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));
        registerNodeType("ij1-roi-filter-by-overlap", FilterRoi2dByOverlapOldAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-roi-filter-by-overlap-v2", FilterRoi2dByOverlapAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-roi-generate-name", ChangeRoiNameFromExpressionsAndMeasurementsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));
        registerNodeType("ij1-roi-filter-roi-lists", FilterRoi2dListsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-roi-dimension-reorder", ReorderRoiDimensionsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-roi-generate-missing", GenerateMissingRoiListsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/image-auto-adjust.png"));
        registerNodeType("ij1-roi-remove-overlay", RemoveOverlayAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/editclear.png"));
        registerNodeType("ij1-roi-render-overlay", RenderOverlayAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/color-management.png"));
        registerNodeType("ij1-roi-set-overlay", SetOverlayAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/roi.png"));
        registerNodeType("ij1-roi-extract-overlay", ExtractOverlayAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/roi.png"));
        registerNodeType("ij1-roi-to-labels-expression", ROIToLabelsExpressionAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-jitter-color.png"));
        registerNodeType("ij1-roi-to-labels-name", ROIToLabelsByNameAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-jitter-color.png"));
        registerNodeType("ij1-roi-flood-fill", RoiFloodFillAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/color-fill.png"));
        registerNodeType("ij1-roi-extract-metadata", ExtractROIMetadataAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/cm_extractfiles.png"));
        registerNodeType("ij1-roi-set-metadata-from-table", SetROIMetadataFromTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/cm_packfiles.png"));
        registerNodeType("ij1-roi-set-metadata-from-statistics-expression", SetRoiMetadataByStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/cm_packfiles.png"));
        registerNodeType("ij1-roi-remove-metadata", RemoveROIMetadataAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-roi-flatten", FlattenRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/layer-flatten-z.png"));
        registerNodeType("ij1-roi-enlarge-shrink", EnlargeShrinkRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/zoom-draw.png"));
        registerNodeType("ij1-roi-transform-2d", TransformRoiFromExpressionsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/dialog-transform.png"));
        registerNodeType("ij1-roi-process-copy-across-zct", CopyRoi2DAcrossZCTAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));

        registerNodeType("ij1-roi-draw-rectangle", DrawRectangleRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-draw-oval", DrawOvalRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-ellipse.png"));
        registerNodeType("ij1-roi-draw-text", DrawTextRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-text.png"));
        registerNodeType("ij1-roi-draw-line", DrawLineOvalRectangleRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-line.png"));
        registerNodeType("ij1-roi-draw-scalebar", DrawScaleBarRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-geometry-show-measuring-info.png"));

//        registerNodeType("ij1-roi-register-max-brightness", RegisterRoiToImageByBrightnessAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/cm_search.png"));
        registerNodeType("ij1-roi-extract-profile", ExtractRoi2DProfileAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-line.png"));

        registerNodeType("ij1-roi-convert-to-table", ConvertRoiToTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/table.png"));

        registerEnumParameterType("ij1-roi-process-copy-across-zct:output-mode",
                CopyRoi2DAcrossZCTAlgorithm.OutputMode.class,
                "ROI2D ZCT Copy Output Mode",
                "Output mode for the algorithm 'Copy 2D ROI across Z/C/T'");
        registerEnumParameterType("ij1-roi-filter-by-overlap-v2:overlap-type",
                FilterRoi2dByOverlapAlgorithm.OverlapMode.class,
                "ROI2D Overlap mode",
                "Different modes for the overlap filtering node");
        registerEnumParameterType("ij1-roi-filter-by-overlap-v2:reference-mode",
                FilterRoi2dByOverlapAlgorithm.ReferenceMode.class,
                "ROI2D overlap reference mode",
                "Different image reference modes for the overlap filtering node");
        registerEnumParameterType("ij1-roi-draw-line:roi-type",
                DrawLineOvalRectangleRoiAlgorithm.RoiType.class,
                "ROI type",
                "Available ROI types");
        registerEnumParameterType("ij1-roi-flood-fill:mode",
                RoiFloodFillAlgorithm.Mode.class,
                "Magic wand mode",
                "Determines how the flood fill behaves");
        registerEnumParameterType("ij1-roi-from-table-rectangular:anchor",
                TableToRectangularROIAlgorithm.Anchor.class,
                "Anchor",
                "Describes how objects are created.");
        registerEnumParameterType("ij1-roi-from-table-rectangular:mode",
                TableToRectangularROIAlgorithm.Mode.class,
                "Mode",
                "Describes which objects are created.");
        registerEnumParameterType("ij1-roi-split-into-connected-components:dimension-operation",
                SplitRoiConnectedComponentsAlgorithm.DimensionOperation.class,
                "Dimension operation",
                "Determines how a dimension is incorporated");
        registerEnumParameterType("roi-label",
                RoiLabel.class,
                "ROI label",
                "Determines how ROI labels are drawn");
        registerEnumParameterType("ij1-roi-extract-profile:rectangle-mode",
                ExtractRoi2DProfileAlgorithm.RectangleMode.class,
                "Rectangle mode",
                "Determines the behavior for profile extraction from rectangle bounds");
    }

    private void registerConvolutionAlgorithms() {
        registerNodeType("ij1-convolve-convolve2d-parameter", ConvolveByParameter2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-convolve-convolve2d-slot", ConvolveByImage2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
    }

    private void registerTransformationAlgorithms() {
        registerNodeType("ij1-transform-flip2d", TransformFlip2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-flip-horizontal.png"));
        registerNodeType("ij1-transform-rotate2d", TransformRotate2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-rotate.png"));
        registerNodeType("ij1-transform-rotate2d-free", TransformRotateFree2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-rotate.png"));
        registerNodeType("ij1-transform-scale2d", TransformScale2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-scale.png"));
        registerNodeType("ij1-transform-scale3d", TransformScale3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-scale.png"));
        registerNodeType("ij1-transform-crop2d", TransformCrop2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/image-crop.png"));
        registerNodeType("ij1-transform-crop-to-roi", CropToRoi2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/image-crop.png"));
        registerNodeType("ij1-transform-expand2d", TransformExpandCanvas2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-scale.png"));
        registerNodeType("ij1-transform-equalize-expand2d", TransformEqualCanvasSize2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-scale.png"));
        registerNodeType("ij1-transform-equalize-dimensions", TransformEqualizeDimensionsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-scale.png"));
        registerNodeType("ij1-transform-equalize-dimensions-max-io", TransformEqualizeDimensionsToMaxAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-scale.png"));
        registerNodeType("ij1-transform-set-dimensions", TransformSetHyperstackDimensionsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/transform-scale.png"));
        registerNodeType("ij1-transform-warp2d", Warp2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-rotate.png"));
        registerNodeType("ij1-overlay", MergeImagesAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-image.png"));
        registerNodeType("ij1-transform-tile-2d", TileImage2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/grid-rectangular.png"));
        registerNodeType("ij1-transform-tile-2d-v2", TileImage2Dv2Algorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/grid-rectangular.png"));
        registerNodeType("ij1-transform-un-tile-2d", UnTileImage2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/grid-rectangular.png"));
        registerNodeType("ij1-transform-add-border-2d", AddBorder2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/bordertool.png"));
        registerNodeType("ij1-transform-line-mirror-by-expression", LineMirror2DFromExpressionsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-geometry-mirror.png"));
        registerNodeType("ij1-transform-line-mirror-by-roi", LineMirror2DFromRoiAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-geometry-mirror.png"));

        registerEnumParameterType("ij1-transform-flip2d:flip-mode", TransformFlip2DAlgorithm.FlipMode.class,
                "Flip mode", "Available modes");
        registerEnumParameterType("ij1-transform-rotate2d:rotation-mode", TransformRotate2DAlgorithm.RotationMode.class,
                "Rotation mode", "Available modes");
        registerEnumParameterType("ij1-transform:wrap-mode", WrapMode.class,
                "Wrap mode", "Available wrap modes");
        registerEnumParameterType("ij1-transform:scale-mode",
                ScaleMode.class,
                "Scale mode",
                "Determines how the image is scaled");
        registerEnumParameterType("ij1-border-mode", BorderMode.class, "Border type", "Types of borders");
        registerEnumParameterType("ij1-transform-line-mirror:mode", LineMirror.MirrorOperationMode.class, "Line mirror mode", "Modes for the line mirror operator");

    }

    private void registerFFTAlgorithms() {
        registerNodeType("ij1-fft-forward2d", FFT2DForwardTransform.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-fft-inverse2d", FFT2DInverseTransform.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-fft-swap2d", FFT2DSwapQuadrants.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-fft-bandpass2d", FFTBandPassFilter.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
//        registerNodeType("ij1-fft-custom2d", FFTCustomFilter.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));

        registerEnumParameterType("ij1-fft-bandpass2d:stripe-suppression", FFTBandPassFilter.SuppressStripesMode.class, "Suppress stripes", "Available modes");
    }

    private void registerAnalysisAlgorithms() {
        registerNodeType("ij1-analyze-find-particles2d", FindParticles2D.class, JIPipe.RESOURCES.getIcon16URL("actions/tool_elliptical_selection.png"));
        registerNodeType("ij1-analyze-skeleton-2d3d", AnalyzeSkeleton2D3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-to-path.png"));
        registerNodeType("ij1-analyze-image-statistics", ImageStatisticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/statistics.png"));
        registerNodeType("ij1-analyze-image-statistics-expression", ImageStatisticsExpressionAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/statistics.png"));
        registerNodeType("ij1-analyze-annotate-by-image-statistics-expression", AnnotateByImageStatisticsExpressionAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/statistics.png"));
        registerNodeType("ij1-analyze-statistics-histogram", HistogramGenerator.class, JIPipe.RESOURCES.getIcon16URL("actions/office-chart-bar.png"));
        registerNodeType("ij1-analyze-statistics-key-value-avg", KeyValueAveragesGenerator.class, JIPipe.RESOURCES.getIcon16URL("actions/office-chart-bar.png"));
        registerNodeType("ij1-analyze-statistics-histogram-key-value", KeyValueHistogramGenerator.class, JIPipe.RESOURCES.getIcon16URL("actions/office-chart-bar.png"));
        registerNodeType("ij1-analyze-statistics-threshold-value", KeyValueThresholdStatisticsGenerator.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij1-analyze-statistics-threshold-value-fast", FastKeyValueThresholdStatisticsGenerator.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij1-analyze-statistics-threshold-value-fast-avg", AverageKeyValueThresholdStatisticsGenerator.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij1-analyze-statistics-threshold-partition-key-value", KeyValueThresholdPartitionGenerator.class, JIPipe.RESOURCES.getIcon16URL("actions/office-chart-bar.png"));
        registerNodeType("ij1-analyze-statistics-threshold-partition-key-value-avg", KeyValueThresholdPartitionAveragesGenerator.class, JIPipe.RESOURCES.getIcon16URL("actions/office-chart-bar.png"));
        registerNodeType("ij1-analyze-orientationj-vector-field-2d", OrientationVectorField2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));

        registerEnumParameterType("ij1-analyze-statistics-histogram:multi-channel-mode", HistogramGenerator.MultiChannelMode.class,
                "Multichannel mode", "Available modes");
        registerEnumParameterType("ij1-analyze-skeleton-2d3d:remove-cycles-method", AnalyzeSkeleton2D3DAlgorithm.CycleRemovalMethod.class, "Cycle removal method", "Method to remove cycles");
        registerEnumParameterType("ij1-analyze-skeleton-2d3d:remove-ends-method", AnalyzeSkeleton2D3DAlgorithm.EndRemovalMethod.class, "End removal method", "Method to remove ends in an end-point");
        registerEnumParameterType("ij1-orientationj-vector-field-type", OrientationJVectorFieldType.class, "OrientationJ vector field type", "Vector field type");
    }

    private void registerDimensionAlgorithms() {
        registerNodeType("ij1-dimensions-stack-to-2d", StackTo2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/layer-bottom.png"));
        registerNodeType("ij1-dimensions-stacksplitter", StackSplitterAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-dimensions-hyper-stackslicer", HyperstackSlicerAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-dimensions-hyper-stacksplitter", SplitByDimensionAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-dimensions-expression-slicer", ExpressionSlicerAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-dimensions-stackmerger", CreateStackAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-cuboid.png"));
        registerNodeType("ij1-dimensions-stacks-to-dimension", StackToDimensionMergerAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-cuboid.png"));
        registerNodeType("ij1-dimensions-stacks-to-dimension-2", StackToDimensionMerger2Algorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-cuboid.png"));
        registerNodeType("ij1-dimensions-stackinverter", StackInverterAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/layer-previous.png"));
        registerNodeType("ij1-dimensions-zproject", ZProjectorAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/layer-bottom.png"));
        registerNodeType("ij1-dimensions-zproject-2", NewZProjectorAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/layer-bottom.png"));
        registerNodeType("ij1-dimensions-stack2montage", StackToMontageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/view-grid.png"));
        registerNodeType("ij1-dimensions-stack2montage-v2", StackToMontage2Algorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/view-grid.png"));
        registerNodeType("ij1-dimensions-montage2stack", MontageToStackAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/view-grid.png"));
        registerNodeType("ij1-dimensions-reorder", ReorderDimensionsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-dimensions-stack-combine", StackCombinerAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-dimensions-inpput2montage", InputImagesToMontage.class, JIPipe.RESOURCES.getIcon16URL("actions/view-grid.png"));
        registerNodeType("ij1-dimensions-input2montage-v2", InputImagesToMontage2.class, JIPipe.RESOURCES.getIcon16URL("actions/view-grid.png"));
        registerNodeType("ij1-dimensions-reslice", ResliceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-cuboid.png"));
        registerNodeType("ij1-dimensions-merge-2d-to-hyperstack", Merge2DToHyperstackAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/merge.png"));
        registerNodeType("ij1-dimensions-reorder-hyperstack-slices", ReorderHyperstackSlicesExpressionAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-cuboid.png"));
        registerNodeType("ij1-dimensions-extended-depth-of-focus", ExtendedDepthOfFocusProjectorAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/view-continuous.png"));

        registerEnumParameterType("ij1-dimensions-zproject:method", ZProjectorAlgorithm.Method.class,
                "Method", "Available methods");
        registerEnumParameterType("ij1-dimensions-extended-depth-of-focus:scoring-method",
                ExtendedDepthOfFocusProjectorAlgorithm.ScoringMethod.class,
                "Extended depth of focus scoring method",
                "A scoring method");
        registerEnumParameterType("ij1-dimensions-extended-depth-of-focus:selection-method",
                ExtendedDepthOfFocusProjectorAlgorithm.SelectionMethod.class,
                "Extended depth of focus score selection method",
                "Select by minimum or maximum score");
    }

    private void registerThresholdAlgorithms() {
        registerNodeType("ij1-threshold-manual2d-color-hsb", ManualHSBThreshold2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-manual2d-color-rgb", ManualRGBThreshold2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-manual2d-color-lab", ManualLABThreshold2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-manual2d-8u", ManualThreshold8U2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-percentile2d-8u", PercentileThreshold8U2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-manual2d-16u", ManualThreshold16U2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-manual2d-32f", ManualThreshold32F2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-auto2d", AutoThreshold2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        for (AutoThresholder.Method method : AutoThresholder.Method.values()) {
            registerNodeExample(AutoThreshold2DAlgorithm.class, method.name(), node -> node.setMethod(method));
        }
        registerNodeType("ij1-threshold-expression2d-8u", CustomAutoThreshold2D8UAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-expression2d-16u", CustomAutoThreshold2D16UAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-expression2d-32f", CustomAutoThreshold2D32FAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-expression2d-8u-v2", CustomAutoThreshold2D8Uv2Algorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-expression2d-16u-v2", CustomAutoThreshold2D16Uv2Algorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-expression2d-32f-v2", CustomAutoThreshold2D32Fv2Algorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-expression2d-color", ColorThresholdExpression2D.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-local-auto2d", LocalAutoThreshold2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-local-auto2d-bernsen", BernsenLocalAutoThreshold2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-local-auto2d-niblack", NiblackLocalAutoThreshold2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-local-auto2d-sauvola", SauvolaLocalAutoThreshold2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-local-auto2d-phansalkar", PhansalkarLocalAutoThreshold2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-threshold-local-auto2d-contrast", ContrastLocalAutoThreshold2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("threshold-brightspots2d", BrightSpotsSegmentation2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("threshold-hessian2d", HessianSegmentation2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("threshold-hough2d", CircularHoughSegmentation2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("threshold-hough2d-fast", FastCircularHoughSegmentation2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("threshold-internalgradient2d", InternalGradientSegmentation2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("threshold-by-annotation", ThresholdByAnnotation2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("threshold-iterative-by-roi-statistics-2d", IterativeThresholdByROIStatistics2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));

        registerEnumParameterType(AutoThresholder.Method.class.getCanonicalName(), AutoThresholder.Method.class,
                "Auto threshold method", "Available methods");
        registerEnumParameterType("slice-threshold-mode", AutoThreshold2DAlgorithm.SliceThresholdMode.class,
                "Slice thresholding mode", "How multi-slice images are thresholded");
        registerEnumParameterType(LocalAutoThreshold2DAlgorithm.Method.class.getCanonicalName(), LocalAutoThreshold2DAlgorithm.Method.class,
                "Local auto threshold method", "Available methods");
        registerEnumParameterType("ij1:eigenvalue-selection-2d", EigenvalueSelection2D.class,
                "Eigenvalue selection (2D)", "Determines whether to choose the smallest or largest Eigenvalue");
        registerEnumParameterType("ij1:roi-outline", RoiOutline.class,
                "ROI outline", "Available ways to outline a ROI. " +
                        "<ul>" +
                        "<li>Polygon: outline the ROI with an open polygon (ImageJ)</li>" +
                        "<li>Closed polygon: outline the ROI with a closed polygon (ImageJ)</li>" +
                        "<li>Convex hull: find the convex hull of the ROI</li>" +
                        "<li>Bounding rectangle: outline the ROI with its bounding rectangle. Please note that this rectangle is not rotated.</li>" +
                        "<li>Minimum bounding rectangle: outline the ROI with its minimum bounding rectangle. The rectangle is rotated to minimize its area.</li>" +
                        "<li>Oriented line: Finds the minimum bounding rectangle and chooses the center of the two short sides as the endpoint of a line.</li>" +
                        "</ul>");
        registerEnumParameterType("ij1:invalid-roi-outline-behavior", InvalidRoiOutlineBehavior.class,
                "Invalid ROI outline behavior", "What should be done if a ROI processing/outlining operation could not be applied to a ROI");
    }

    private void registerSharpenAlgorithms() {
        registerNodeType("ij1-sharpen-laplacian2d", LaplacianSharpen2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-unsharp-mask-2d", UnsharpMasking2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
    }

    private void registerBackgroundAlgorithms() {
        registerNodeType("ij1-background-rollingball2d", RollingBallBackgroundEstimator2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));

        registerEnumParameterType("ij1-background-rollingball2d:background-type", RollingBallBackgroundEstimator2DAlgorithm.BackgroundType.class,
                "Background type", "Available background types");
        registerEnumParameterType("ij1-background-rollingball2d:background-method", RollingBallBackgroundEstimator2DAlgorithm.Method.class,
                "Rolling ball method", "Available methods");
    }

    private void registerNoiseAlgorithms() {
        registerNodeType("ij1-noise-addnormalnoise2d", AddNoise2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-noise-add-salt-and-pepper-noise2d", AddSaltAndPepperNoise2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-noise-despeckle2d", DespeckleFilter2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/image-auto-adjust.png"));
        registerNodeType("ij1-noise-removeoutliers2d", RemoveOutliersFilter2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-eraser.png"));

        registerEnumParameterType("ij1-noise-removeoutliers2d:mode", RemoveOutliersFilter2DAlgorithm.Mode.class,
                "Mode", "Available modes");
    }

    private void registerBinaryAlgorithms() {
        registerNodeType("ij1-binary-dtwatershed2d", DistanceTransformWatershed2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-binary-voronoi2d", Voronoi2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-binary-uep2d", UltimateErodedPoints2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-binary-bitwise", BitwiseLogicalOperationAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-paint.png"));
        registerNodeType("ij1-binary-connected-component-labeling-2d", ConnectedComponentsLabeling2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-jitter-color.png"));
        registerNodeType("ij1-binary-connected-component-labeling-3d", ConnectedComponentsLabeling3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-jitter-color.png"));
        registerNodeType("ij1-binary-binarize", BinarizeAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
        registerNodeType("ij1-binary-convexify", ConvexifyAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-polygon.png"));
        registerNodeType("ij1-binary-keep-largest-region", KeepLargestRegionAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-binary-remove-largest-region", RemoveLargestRegionAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-binary-volume-opening-2d", VolumeOpening2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-binary-volume-opening-3d", VolumeOpening3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-binary-conditional-invert", ConditionalInverterAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/invertimage.png"));
        registerNodeType("ij1-binary-hough-lines", LinesHoughDetection2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-geometry-line-segment.png"));
        registerNodeType("ij1-binary-hough-line-segments", LineSegmentsHoughDetection2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-geometry-line-segment.png"));
        registerNodeType("ij1-binary-hough-lines-global", GlobalLinesHoughDetection2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-geometry-line-segment.png"));
        registerEnumParameterType("ij1-hough-nms-algorithm", HoughLinesNMSAlgorithm.class, "NMS algorithm", "NMS algorithm for Hough");
    }

    private void registerMorphologyAlgorithms() {
//        registerNodeType("ij1-morph-binary-operation2d", MorphologyBinary2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/morphology.png"));
        registerNodeType("ij1-morph-operation2d", Morphology2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/morphology.png"));
        registerNodeType("ij1-morph-operation3d", Morphology3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/morphology.png"));
        registerNodeType("ij1-morph-binary-fillholes2d", MorphologyFillHoles2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-fill.png"));
        registerNodeType("ij1-morph-binary-outline2d", MorphologyOutline2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-connector.png"));
        registerNodeType("ij1-morph-binary-skeletonize2d", MorphologySkeletonize2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-to-path.png"));
        registerNodeType("ij1-morph-binary-skeletonize3d", MorphologySkeletonize3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-to-path.png"));
        registerNodeType("ij1-morph-reconstruct-2d", MorphologicalReconstruction2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-morph-reconstruct-3d", MorphologicalReconstruction3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-morph-find-holes-3d", FindHoles2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-randomize.png"));
        registerNodeType("ij1-morph-grayscale-attribute-filtering-2d", GrayscaleAttributeFiltering2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("ij1-morph-grayscale-attribute-filtering-3d", GrayscaleAttributeFiltering3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));

        registerEnumParameterType("ij1-morph-binary-operation2d:operation", MorphologyBinary2DAlgorithm.Operation.class,
                "Operation", "Available operations");
        registerEnumParameterType("ij1-morph:operation", Morphology.Operation.class,
                "Operation", "Available operations");
        registerEnumParameterType("ij1-morph:strel", Strel.Shape.class,
                "Structure element", "Available shapes");
        registerEnumParameterType("ij1-morph:strel-3d", Strel3D.Shape.class,
                "Structure element (3D)", "Available shapes");
        registerEnumParameterType("ij1-morph-grayscale-attribute-filtering-2d:operation", GrayscaleAttributeFiltering2DAlgorithm.Operation.class,
                "Operation", "Available operations");
        registerEnumParameterType("ij1-morph-grayscale-attribute-filtering-3d:operation", GrayscaleAttributeFiltering3DAlgorithm.Operation.class,
                "Operation", "Available operations");
        registerEnumParameterType("ij1-morph-grayscale-attribute-filtering-2d:attribute", GrayscaleAttributeFiltering2DAlgorithm.Attribute.class,
                "Attributes", "Available filter attributes");
        registerEnumParameterType("ij1-morph-grayscale-attribute-filtering-3d:attribute", GrayscaleAttributeFiltering3DAlgorithm.Attribute.class,
                "Attributes", "Available filter attributes");
    }

    private void registerMathAlgorithms() {
        registerNodeType("ij1-math-round-32f", RoundFloatImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-math2d", LegacyApplyMath2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-transform2d", ApplyTransform2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-math2d-expression", ApplyMathExpression2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-math2d-expression-color", ApplyColorMathExpression2DExpression.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-vector-expression", ApplyVectorMathExpression2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-edt2d", ApplyDistanceTransform2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-variance2d", LocalVarianceFilter2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-maximum2d", LocalMaximumFilter2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-minimum2d", LocalMinimumFilter2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-variance3d", LocalVarianceFilter3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-maximum3d", LocalMaximumFilter3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-minimum3d", LocalMinimumFilter3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-imagecalculator2d-expression", LocalImageCalculator2DExpression.class, JIPipe.RESOURCES.getIcon16URL("actions/calculator.png"));
        registerNodeType("ij1-math-replace-nan-by-median2d", RemoveNaNFilter2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-imagecalculator2d", LegacyImageCalculator2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/calculator.png"));
        registerNodeType("ij1-math-fast-image-arithmetics", FastImageArithmeticsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/calculator.png"));
        registerNodeType("ij1-math-imagecalculator2d-merging", LegacyImageCalculator2DMergingAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/calculator.png"));
        registerNodeType("ij1-math-compare-images-2d", ImageComparer2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/calculator.png"));
        registerNodeType("ij1-math-imagecalculator2d-expression", ImageCalculator2DExpression.class, JIPipe.RESOURCES.getIcon16URL("actions/calculator.png"));
        registerNodeType("ij1-math-divide-by-maximum", DivideByMaximumAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-mean2d", LocalMeanFilter2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-mean3d", LocalMeanFilter3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));

        registerNodeType("ij1-math-regional-minima-2d", RegionalMinima2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-regional-maxima-2d", RegionalMaxima2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-regional-minima-3d", RegionalMinima3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-regional-maxima-3d", RegionalMaxima3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-extended-minima-2d", ExtendedMinima2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-extended-maxima-2d", ExtendedMaxima2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-extended-minima-3d", ExtendedMinima3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-extended-maxima-3d", ExtendedMaxima3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));

        registerNodeType("ij1-math-impose-maxima-2d", ImposeMaxima2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-impose-maxima-3d", ImposeMaxima3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));

        registerNodeType("ij1-math-generate-from-expression", GenerateFromMathExpression2D.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-generate-vector-from-expression", GenerateVectorFromMathExpression.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij-imgplus-from-matrix", ImageFromMatrix2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/table.png"));

        registerNodeType("ij1-math-generate-missing-from-expression", GenerateMissingImageFromMathExpression2D.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-generate-missing-zero-image", GenerateMissingZeroImage.class, JIPipe.RESOURCES.getIcon16URL("actions/add.png"));
        registerNodeType("ij1-generate-zero-image", GenerateZeroImage.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus.png"));

        registerNodeType("ij-math-chamfer-distance-map-2d", ChamferDistanceMap2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij-math-chamfer-distance-map-3d", ChamferDistanceMap3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij-math-label-chamfer-distance-map-3d", LabelChamferDistanceMap3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij-math-geodesic-distance-map-2d", GeodesicDistanceMap2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));

        registerEnumParameterType("ij-math-chamfer-distance-map-2d:weights", ChamferWeights.class, "Chamfer weights (2D)", "Predefined weights for the Chamfer distance map");
        registerEnumParameterType("ij-math-chamfer-distance-map-23:weights", ChamferWeights3D.class, "Chamfer weights (3D)", "Predefined weights for the Chamfer distance map");

        registerEnumParameterType("ij1-math-math2d:transformation", LegacyApplyMath2DAlgorithm.Transformation.class,
                "Transformation", "Available transformations");
        registerEnumParameterType("ij1-math-transform2d:transformation", ApplyTransform2DAlgorithm.Transformation.class,
                "Transformation", "Available transformations");

        registerEnumParameterType("ij1-math-imagecalculator2d:operation", LegacyImageCalculator2DAlgorithm.Operation.class,
                "Operation", "Available operations");
        registerEnumParameterType("ij1-math-imagecalculator2d:operand", LegacyImageCalculator2DAlgorithm.Operand.class,
                "Operand", "Available operands");

        registerEnumParameterType("ij1-math-compare-images-2d:operation", ImageComparer2DAlgorithm.Operation.class,
                "Operation", "Available operations");
        registerEnumParameterType("ij1-math-compare-images-2d:operand", ImageComparer2DAlgorithm.Operand.class,
                "Operand", "Available operands");
    }

    private void registerFeatureAlgorithms() {
        registerNodeType("ij1-feature-vesselness-frangi", FrangiVesselnessFeatures.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("feature-vesselness-meijering2d", MeijeringVesselness2DFeatures.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-maxima-local-2d", LocalMaxima2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-difference-of-gaussian", DifferenceOfGaussian2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-laplacian-of-gaussian", LaplacianOfGaussian2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-directional-filter-2d", DirectionalFilter2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-rotate.png"));
        registerNodeType("ij1-feature-mtc-2d", MorphologicalTextureContrast2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-amf-2d", AlternatingMorphologicalFilters2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-mfc-2d", MorphologicalFeatureContrast2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-harris-corner-2d", CornerHarris2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-orientationj-2d", OrientationFeatures2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-featurej-derivatives", DerivativesFeaturesAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-featurej-hessian", HessianFeatureAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-featurej-structure", StructureFeatureAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-hessian2d", Hessian2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-shadows-2d", Shadows2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/object-tweak-duplicate.png"));

        registerEnumParameterType("ij1-feature-vesselness-frangi:slicing-mode", FrangiVesselnessFeatures.SlicingMode.class,
                "Slicing mode", "Available slicing modes");
        registerEnumParameterType("ij1-feature-maxima-local-2d:output-type", LocalMaxima2DAlgorithm.OutputType.class,
                "Output type", "Available output types");
        registerEnumParameterType("ij1-feature-directional-filter:operation", DirectionalFilter.Operation.class,
                "Directional filter operation", "Available operations");
        registerEnumParameterType("ij1-feature-directional-filter:type", DirectionalFilter.Type.class,
                "Directional filter types", "Available types");
        registerEnumParameterType("ij1-feature-mfc-2d:mode", MorphologicalFeatureContrast2DAlgorithm.Mode.class,
                "MFC mode", "Available modes");
        registerEnumParameterType("ij1-orientationj-gradient", OrientationJGradientOperator.class,
                "OrientationJ gradient operator", "A gradient operator");
        registerEnumParameterType("ij1-feature-shadows-2d:direction",
                Shadows2DAlgorithm.Direction.class,
                "Shadow direction",
                "A direction");
    }

    private void registerContrastAlgorithms() {
        registerNodeType("ij1-contrast-clahe", CLAHEContrastEnhancer.class, JIPipe.RESOURCES.getIcon16URL("actions/contrast.png"));
        registerNodeType("ij1-contrast-illumination-correction2d", IlluminationCorrection2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/contrast.png"));
        registerNodeType("ij1-contrast-calibrate", DisplayRangeCalibrationAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/contrast.png"));
        registerNodeType("ij1-contrast-apply-displayed-contrast", ApplyDisplayContrastAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/contrast.png"));
        registerNodeType("ij1-contrast-histogram-enhancer", HistogramContrastEnhancerAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/contrast.png"));
        registerNodeType("ij1-contrast-apply-ij-per-slice", ImageJContrastEnhancerAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/contrast.png"));

        registerEnumParameterType(HistogramContrastEnhancerAlgorithm.Method.class.getCanonicalName(), HistogramContrastEnhancerAlgorithm.Method.class,
                "Histogram contrast enhancer method", "Available methods");
    }

    private void registerEdgeAlgorithms() {
        registerNodeType("ij1-edge-sobel", SobelEdgeDetectorAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/path-offset-dynamic.png"));
        registerNodeType("ij1-edge-laplacian", LaplacianEdgeDetectorAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/path-offset-dynamic.png"));
        registerEnumParameterType("ij1-edge-laplacian:mode",
                LaplacianEdgeDetectorAlgorithm.Mode.class,
                "Laplacian type",
                "The type of laplacian");
        registerNodeType("ij1-edge-canny", CannyEdgeDetectorAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/path-offset-dynamic.png"));
    }

    private void registerColorAlgorithms() {
        registerParameterType("ij1-color-overlay-channels:channel", OverlayImagesAlgorithm.Channel.class, JIPipeParameterArchetype.Value, "Channel settings", "Settings for a channel");
        registerParameterType("ij1-color-blend-images:layer", ImageBlendLayer.class, JIPipeParameterArchetype.Value, "Layer settings", "Settings for a layer");
        registerEnumParameterType("ij1-color-blend-images:blend-mode", ImageBlendMode.class, "Layer blend mode", "Blend mode for the layer");

        registerNodeType("ij1-color-invert", InvertImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/invertimage.png"));
        registerNodeType("ij1-color-invert-greyscale", InvertGreyscaleValuesAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/invertimage.png"));
        registerNodeType("ij1-color-invert-rgb", InvertRGBColorsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/invertimage.png"));
        registerNodeType("ij1-color-merge-channels", MergeChannelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/merge.png"));
        registerNodeType("ij1-color-merge-channels-composite", MergeChannelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/merge.png"));
        for (int c = 2; c <= 7; c++) {
            int finalC = c;
            registerNodeExample(MergeChannelsCompositeAlgorithm.class, "Merge " + c + " channels", node -> {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) node.getSlotConfiguration();
                slotConfiguration.clearInputSlots(true);
                for (int i = 0; i < finalC; i++) {
                    slotConfiguration.addSlot("C" + (i + 1), new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Input), true);
                }
            });
        }
        registerNodeType("ij1-color-overlay-channels", MergeChannelsCompositeAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/merge.png"));
        registerNodeType("ij1-color-blend-images", BlendImagesAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/merge.png"));
        registerNodeType("ij1-color-arrange-channels", ArrangeChannelsAlgorithm.class);
        registerNodeType("ij1-color-split-channels", SplitChannelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-color-split-channels-2", NewSplitChannelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-color-split-channels-by-table", SplitChannelsByTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("ij1-color-combine-rgb", CombineChannelsToRGBAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/colors-rgb.png"));
        registerNodeType("ij1-color-merge-channels-rgb", MergeRGBChannelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/colors-rgb.png"));
        registerNodeType("ij1-color-merge-channels-hsb", MergeHSBChannelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/colors-rgb.png"));
        registerNodeType("ij1-color-merge-channels-lab", MergeLABChannelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/colors-rgb.png"));
        registerNodeType("ij1-color-split-rgb", SplitRGBChannelsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/channelmixer.png"));
        registerNodeType("ij1-color-to-rgb", RenderImageToRGBAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("data-types/imgplus-color-rgb.png"));
        registerNodeType("ij1-color-to-greyscale-expression", ColorToGreyscaleExpression2D.class, JIPipe.RESOURCES.getIcon16URL("actions/color-picker-grey.png"));

        registerEnumParameterType("ij1-color-merge-channels:channel-color", MergeChannelsAlgorithm.ChannelColor.class,
                "Channel color", "Available channel colors");
    }

    private void registerBlurAlgorithms() {
        registerNodeType("ij1-blur-gaussian2d", GaussianBlur2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-gaussian3d", GaussianBlur3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-box2d", BoxFilter2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-box2d-v2", BoxFilter2Dv2Algorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-box3d", BoxFilter3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-median2d-8u", MedianGreyscale8U2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-median2d-rgb", MedianRGB2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-median2d", MedianFilter2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-median3d", MedianFilter3DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
        registerNodeType("ij1-filter-kuwahara2d", KuwaharaFilter2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));

        registerEnumParameterType("ij1-filter-kuwahara2d:criterion", KuwaharaFilter2DAlgorithm.Criterion.class, "Kuwahara criterion", "Available criteria");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:imagej-algorithms";
    }

}



