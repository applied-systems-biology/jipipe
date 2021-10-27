/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms;

import com.google.common.collect.ImmutableMap;
import de.biomedical_imaging.ij.steger.OverlapOption;
import ij.process.AutoThresholder;
import inra.ijpb.binary.ChamferWeights;
import inra.ijpb.binary.ChamferWeights3D;
import inra.ijpb.color.ColorMaps;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.Strel3D;
import inra.ijpb.morphology.directional.DirectionalFilter;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.EigenvalueSelection2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.HyperstackDimensionPairParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.InterpolationMethod;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.MacroWrapperAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood2D3D;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood3D;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.analyze.AnnotateByImageStatisticsExpressionAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.analyze.FindParticles2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.analyze.ImageStatisticsAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.analyze.ImageStatisticsExpressionAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.background.RollingBallBackgroundEstimator2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.binary.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.BoxFilter2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.BoxFilter3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.GaussianBlur2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.GaussianBlur3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.MedianBlurFilter2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.MedianBlurFilter3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.MedianBlurGreyscale8U2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.MedianBlurRGB2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.calibration.SetPhysicalDimensionsAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.calibration.SetPhysicalDimensionsByExpressionsAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.color.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.contrast.CLAHEContrastEnhancer;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.contrast.HistogramContrastEnhancerAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.contrast.IlluminationCorrection2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.convert.ConvertImageAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.convolve.ConvolveByImage2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.convolve.ConvolveByParameter2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.datasources.ImageStackFromFolder;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.datasources.OMEImageFromImagePlus;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.edge.CannyEdgeDetectorAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.edge.LaplacianEdgeDetectorAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.edge.SobelEdgeDetectorAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.export.ExportImageAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.export.ExportROIAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.export.ExportTableAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features.DifferenceOfGaussian2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features.DirectionalFilter2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features.FrangiVesselnessFeatures;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features.LaplacianOfGaussian2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features.LocalMaxima2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features.MeijeringVesselness2DFeatures;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.fft.FFT2DForwardTransform;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.fft.FFT2DInverseTransform;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.fft.FFT2DSwapQuadrants;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.forms.DrawMaskAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.generate.GenerateLUTImageFromColorMap;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.generate.GenerateMissingImageFromMathExpression2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.generate.GenerateMissingTablesAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.generate.GenerateMissingZeroImage;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.generate.GenerateStructureElement2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.generate.GenerateStructureElement3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.generate.GenerateZeroImage;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.generate.ImageFromMatrix2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io.ImagePlusFromGUI;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io.ImagePlusToGUI;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io.ROIFromGUI;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io.ROIToGUI;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io.ResultsTableFromGUI;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io.ResultsTableToGUI;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.labels.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.lut.LUTInverterAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.lut.RemoveLUTAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.lut.SetLUTFromColorMapAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.lut.SetLUTFromColorsAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.lut.SetLUTFromImageAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math.distancemap.ApplyDistanceTransform2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math.distancemap.ChamferDistanceMap2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math.distancemap.ChamferDistanceMap3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math.distancemap.GeodesicDistanceMap2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math.distancemap.LabelChamferDistanceMap3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math.local.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.misc.DataToPreviewAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.MorphologicalReconstruction2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.MorphologicalReconstruction3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.Morphology2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.Morphology3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.MorphologyBinary2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.MorphologyFillHoles2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.MorphologyOutline2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.MorphologySkeletonize2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.noise.AddNoise2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.noise.AddSaltAndPepperNoise2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.noise.DespeckleFilter2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.noise.RemoveOutliersFilter2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.opticalflow.MSEBlockFlowAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.opticalflow.MSEGaussianFlowAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.opticalflow.PMCCBlockFlowAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.segment.ClassicWatershedSegmentationAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.segment.RidgeDetector2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.segment.SeededWatershedSegmentationAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.sharpen.LaplacianSharpen2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.statistics.GreyscalePixelsGenerator;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.statistics.HistogramGenerator;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.color.ColorThresholdExpression2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.color.ManualHSBThreshold2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.color.ManualLABThreshold2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.color.ManualRGBThreshold2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.local.BernsenLocalAutoThreshold2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.local.ContrastLocalAutoThreshold2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.local.LocalAutoThreshold2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.local.NiblackLocalAutoThreshold2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.local.PhansalkarLocalAutoThreshold2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.local.SauvolaLocalAutoThreshold2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.SourceWrapMode;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.DisplayRangeCalibrationAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.ImagePlus5DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.RoiOutline;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementColumn;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementColumnSortOrder;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extension that adds ImageJ2 algorithms
 */
@Plugin(type = JIPipeJavaExtension.class)
public class ImageJAlgorithmsExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Conversion rules from mask data types to their respective 8-bit types
     */
    public static final Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> REMOVE_MASK_QUALIFIER =
            ImmutableMap.of(
                    ImagePlusGreyscaleMaskData.class, ImagePlusGreyscale8UData.class,
                    ImagePlus2DGreyscaleMaskData.class, ImagePlus2DGreyscale8UData.class,
                    ImagePlus3DGreyscaleMaskData.class, ImagePlus3DGreyscale8UData.class,
                    ImagePlus4DGreyscaleMaskData.class, ImagePlus4DGreyscale8UData.class,
                    ImagePlus5DGreyscaleMaskData.class, ImagePlus5DGreyscale8UData.class);

    /**
     * Conversion rules that convert any input data type into their respective mask data type
     */
    public static final Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> ADD_MASK_QUALIFIER = getMaskQualifierMap();

    /**
     * Conversion rules that convert color types into greyscale
     */
    public static final Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> TO_GRAYSCALE_CONVERSION = getToGrayscaleConversion();

    /**
     * Conversion rules that convert color types into colored images
     */
    public static final Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> TO_COLOR_RGB_CONVERSION = getToColorRgbConversion();

    /**
     * Conversion rules that convert color types into colored images
     */
    public static final Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> TO_GRAYSCALE32F_CONVERSION = getToGrayscale32FConversion();

    /**
     * Conversion rules convert higher-dimensional data to a lower-dimensional counterpart.
     * 2D data remains 2D data.
     */
    public static final Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> DECREASE_DIMENSION_CONVERSION = getDecreaseDimensionConversion();

    /**
     * Conversion rules convert higher-dimensional data to their 2D counterparts
     */
    public static final Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> TO_2D_CONVERSION = get2DConversion();

    /**
     * Conversion rules convert data to their 3D counterparts
     */
    public static final Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> TO_3D_CONVERSION = get3DConversion();

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
    public void register() {
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

        registerEnumParameterType("ij1-export-image-to-web:file-format", ExportImageAlgorithm.FileFormat.class, "File format", "Exported file format.");
        registerEnumParameterType("ij1-export-table:file-format", ExportTableAlgorithm.FileFormat.class, "File format", "Exported file format.");
        registerNodeType("iji-export-image-to-web", ExportImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("iji-export-roi-list", ExportROIAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("iji-export-table", ExportTableAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-export.png"));

        registerNodeType("ij1-generate-missing-results-table", GenerateMissingTablesAlgorithm.class, UIUtils.getIconURLFromResources("actions/image-auto-adjust.png"));
        registerNodeType("ij1-generate-filter-kernel", GenerateStructureElement2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/morphology.png"));
        registerNodeType("ij1-generate-filter-kernel-3d", GenerateStructureElement3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/morphology.png"));
        registerNodeType("ij1-data-to-preview", DataToPreviewAlgorithm.class, UIUtils.getIconURLFromResources("actions/viewimage.png"));
        registerNodeType("external-imagej-macro", MacroWrapperAlgorithm.class, UIUtils.getIconURLFromResources("apps/imagej.png"));

        registerNodeType("ome-image-from-image-plus", OMEImageFromImagePlus.class);

        // Register enum parameters
        registerGlobalEnums();

        // Register other parameters
        registerGlobalParameters();

//        registerIJ2Algorithms();
    }

    private void registerCalibrationAlgorithms() {
        registerNodeType("ij1-calibration-set-physical-dimensions", SetPhysicalDimensionsAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-show-measuring-info.png"));
        registerNodeType("ij1-calibration-set-physical-dimensions-from-expressions", SetPhysicalDimensionsByExpressionsAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-show-measuring-info.png"));
    }

    private void registerSegmentationAlgorithms() {
        registerNodeType("ij1-segment-classic-watershed", ClassicWatershedSegmentationAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-object-histogram-linear.png"));
        registerNodeType("ij1-segment-seeded-watershed", SeededWatershedSegmentationAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-object-histogram-linear.png"));
        registerNodeType("ij1-segment-ridge-detector-2d", RidgeDetector2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-geometry-mirror.png"));
        registerEnumParameterType("ij1-segment-ridge-detector-2d:overlap-resolver", OverlapOption.class, "Overlap detector", "Method for overlap detection");
    }

    private void registerLabelAlgorithms() {
        registerEnumParameterType("ij1-label-color-maps:common-label-maps", ColorMaps.CommonLabelMaps.class, "Color map", "A color map for labels");

        registerNodeType("ij1-labels-to-rgb", LabelsToRGBAlgorithm.class, UIUtils.getIconURLFromResources("actions/colormanagement.png"));
        registerNodeType("ij1-labels-get-label-boundaries", GetLabelBoundariesAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-stroke.png"));
        registerNodeType("ij1-labels-remove-border-labels", RemoveBorderLabelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-labels-replace-labels", ReplaceLabelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/edit.png"));
        registerNodeType("ij1-labels-merge-labels", MergeLabelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("ij1-labels-filter-labels-by-id", FilterLabelsByIdAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-labels-filter-filter-by-expression-2d", FilterLabelsByExpression2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-labels-filter-filter-by-expression-3d", FilterLabelsByExpression3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-labels-crop-labels", CropLabelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/image-crop.png"));
        registerNodeType("ij1-labels-remap", RemapLabelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-visible.png"));
        registerNodeType("ij1-labels-expand-labels", ExpandLabelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-tweak-push.png"));
        registerNodeType("ij1-labels-remove-largest-label", RemoveLargestLabelAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-labels-keep-largest-label", KeepLargestLabelAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-labels-dilate-labels", DilateLabelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/morphology.png"));
    }

    private void registerConverterAlgorithms() {
        registerNodeType("ij-convert-image", ConvertImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-refresh.png"));
    }

    private void registerFormAlgorithms() {
        registerNodeType("ij-form-draw-mask", DrawMaskAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-brush.png"));
    }

    private void registerOpticalFlowAlgorithms() {
        registerNodeType("ij-optical-flow-mse-block-flow", MSEBlockFlowAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-tweak-rotate.png"));
        registerNodeType("ij-optical-flow-mse-gaussian-flow", MSEGaussianFlowAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-tweak-rotate.png"));
        registerNodeType("ij-optical-flow-pmcc-block-flow", PMCCBlockFlowAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-tweak-rotate.png"));
    }

    private void registerIOAlgorithms() {
        registerNodeType("ij-imgplus-from-gui", ImagePlusFromGUI.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
        registerNodeType("ij-imgplus-to-gui", ImagePlusToGUI.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
        registerNodeType("ij-results-table-from-gui", ResultsTableFromGUI.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
        registerNodeType("ij-results-table-to-gui", ResultsTableToGUI.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
        registerNodeType("ij-roi-from-gui", ROIFromGUI.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
        registerNodeType("ij-roi-to-gui", ROIToGUI.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
        registerNodeType("ij-import-stack", ImageStackFromFolder.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
    }

    private void registerLUTAlgorithms() {
        registerNodeType("ij1-remove-lut", RemoveLUTAlgorithm.class, UIUtils.getIconURLFromResources("actions/paint-gradient-linear.png"));
        registerNodeType("ij1-set-lut-from-colors", SetLUTFromColorsAlgorithm.class, UIUtils.getIconURLFromResources("actions/paint-gradient-linear.png"));
        registerNodeType("ij1-set-lut-from-color-map", SetLUTFromColorMapAlgorithm.class, UIUtils.getIconURLFromResources("actions/paint-gradient-linear.png"));
        registerNodeType("ij1-set-lut-from-color-image", SetLUTFromImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/paint-gradient-linear.png"));
        registerNodeType("ij1-invert-lut", LUTInverterAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-inverse.png"));
        registerNodeType("ij1-render-color-map", GenerateLUTImageFromColorMap.class, UIUtils.getIconURLFromResources("actions/paint-gradient-linear.png"));
    }

    private void registerGlobalParameters() {
        registerParameterType("ij1:measurement-column-sort-order",
                MeasurementColumnSortOrder.class,
                MeasurementColumnSortOrder::new,
                o -> new MeasurementColumnSortOrder((MeasurementColumnSortOrder) o),
                "Measurement column sort order",
                "Describes how a data is sorted by a measurement column",
                null);
        registerParameterType("ij1:measurement-column-sort-order-list",
                MeasurementColumnSortOrder.List.class,
                MeasurementColumnSortOrder.List::new,
                o -> new MeasurementColumnSortOrder.List((MeasurementColumnSortOrder.List) o),
                "Measurement column sort order list",
                "List of measurement column sort orders",
                null);
    }

    private void registerGlobalEnums() {
        registerEnumParameterType("ij1-interpolation-method", InterpolationMethod.class,
                "Interpolation method", "Available interpolation methods");
        registerEnumParameterType("ij1-measurement", Measurement.class,
                "Measurement", "Available measurements");
        registerEnumParameterType("ij1-measurement-column", MeasurementColumn.class,
                "Measurement column", "Available measurement columns");
        registerEnumParameterType("ij1-calibration-mode", ImageJCalibrationMode.class,
                "Contrast calibration", "Methods to apply display range calibration");
        registerParameterType("ij1-measurement-set", ImageStatisticsSetParameter.class,
                null,
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
                HyperstackDimensionPairParameter.List.class,
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
    }

    private void registerROIAlgorithms() {
        registerNodeType("ij1-roi-from-rectangles", DefineRectangularRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-from-rectangles-referenced", ReferencedDefineRectangularRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-append-rectangles", AppendRectangularRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-append-rectangles-referenced", ReferencedAppendRectangularRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-split", SplitRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-roi-split-into-connected-components", SplitRoiConnectedComponentsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-roi-explode", ExplodeRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-roi-merge", MergeRoiListsAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("ij1-roi-calculator", RoiCalculatorAlgorithm.class, UIUtils.getIconURLFromResources("actions/calculator.png"));
        registerNodeType("ij1-roi-to-mask-unreferenced", UnreferencedRoiToMaskAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-roi-to-mask", RoiToMaskAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-roi-outline", OutlineRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-connector.png"));
        registerNodeType("ij1-roi-remove-bordering", RemoveBorderRoisAlgorithm.class, UIUtils.getIconURLFromResources("actions/bordertool.png"));
        registerNodeType("ij1-roi-statistics", RoiStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("ij1-roi-count", CountROIAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("ij1-roi-filter-statistics", FilterRoiByStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-roi-filter-by-name", FilterRoiByNameAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-roi-color-statistics", ColorRoiByStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/fill-color.png"));
        registerNodeType("ij1-roi-sort-and-extract-statistics", SortAndExtractRoiByStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-sort.png"));
        registerNodeType("ij1-roi-set-properties", ChangeRoiPropertiesAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-edit.png"));
        registerNodeType("ij1-roi-set-properties-from-annotation", ChangeRoiPropertiesFromAnnotationsAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-edit.png"));
        registerNodeType("ij1-roi-to-rgb-unreferenced", UnreferencedRoiToRGBAlgorithm.class, UIUtils.getIconURLFromResources("actions/color-management.png"));
        registerNodeType("ij1-roi-to-rgb", RoiToRGBAlgorithm.class, UIUtils.getIconURLFromResources("actions/color-management.png"));
        registerNodeType("ij1-roi-filter-statistics-script", FilterRoiByStatisticsScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("ij1-roi-filter-and-merge-statistics-script", FilterAndMergeRoiByStatisticsScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("ij1-roi-from-table-rectangular", TableToRectangularROIAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-from-table-circle", TableToCircularROIAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-circle.png"));
        registerNodeType("ij1-roi-set-image", SetRoiImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/viewimage.png"));
        registerNodeType("ij1-roi-get-image", GetRoiImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/viewimage.png"));
        registerNodeType("ij1-roi-unset-image", UnsetRoiImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/delete.png"));
        registerNodeType("ij1-roi-from-mask", MaskToRoiAlgorithm.class, UIUtils.getIconURLFromResources("data-types/imgplus-2d-greyscale-mask.png"));
        registerNodeType("ij1-roi-extract-from-roi", ExtractFromROIAlgorithm.class, UIUtils.getIconURLFromResources("actions/image-crop.png"));
        registerNodeType("ij1-roi-assemble-from-roi", AssembleExtractedROIAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-image.png"));
        registerNodeType("ij1-roi-to-annotations", RoiPropertiesToAnnotationsAlgorithm.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("ij1-roi-filter-by-overlap", FilterROIByOverlapAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-roi-generate-name", GenerateROINameAlgorithm.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("ij1-roi-filter-roi-lists", FilterROIListsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-roi-dimension-reorder", ReorderRoiDimensionsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-roi-generate-missing", GenerateMissingRoiListsAlgorithm.class, UIUtils.getIconURLFromResources("actions/image-auto-adjust.png"));
        registerNodeType("ij1-roi-remove-overlay", RemoveOverlayAlgorithm.class, UIUtils.getIconURLFromResources("actions/editclear.png"));
        registerNodeType("ij1-roi-set-overlay", SetOverlayAlgorithm.class, UIUtils.getIconURLFromResources("actions/roi.png"));
        registerNodeType("ij1-roi-extract-overlay", ExtractOverlayAlgorithm.class, UIUtils.getIconURLFromResources("actions/roi.png"));

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
    }

    private void registerConvolutionAlgorithms() {
        registerNodeType("ij1-convolve-convolve2d-parameter", ConvolveByParameter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-convolve-convolve2d-slot", ConvolveByImage2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
    }

    private void registerTransformationAlgorithms() {
        registerNodeType("ij1-transform-flip2d", TransformFlip2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-flip-horizontal.png"));
        registerNodeType("ij1-transform-rotate2d", TransformRotate2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-rotate.png"));
        registerNodeType("ij1-transform-rotate2d-free", TransformRotateFree2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-rotate.png"));
        registerNodeType("ij1-transform-scale2d", TransformScale2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-scale.png"));
        registerNodeType("ij1-transform-scale3d", TransformScale3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-scale.png"));
        registerNodeType("ij1-transform-crop2d", TransformCrop2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/image-crop.png"));
        registerNodeType("ij1-transform-crop-to-roi", CropToRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/image-crop.png"));
        registerNodeType("ij1-transform-expand2d", TransformExpandCanvas2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-scale.png"));
        registerNodeType("ij1-transform-equalize-expand2d", TransformEqualCanvasSize2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-scale.png"));
        registerNodeType("ij1-transform-equalize-dimensions", TransformEqualizeDimensionsAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-scale.png"));
        registerNodeType("ij1-transform-set-dimensions", TransformSetHyperstackDimensionsAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-scale.png"));
        registerNodeType("ij1-transform-warp2d", Warp2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-tweak-rotate.png"));
        registerNodeType("ij1-overlay", MergeImagesAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-image.png"));
        registerNodeType("ij1-transform-tile", TileImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/grid-rectangular.png"));

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
    }

    private void registerFFTAlgorithms() {
        registerNodeType("ij1-fft-forward2d", FFT2DForwardTransform.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-fft-inverse2d", FFT2DInverseTransform.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-fft-swap2d", FFT2DSwapQuadrants.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
    }

    private void registerAnalysisAlgorithms() {
        registerNodeType("ij1-analyze-find-particles2d", FindParticles2D.class, UIUtils.getIconURLFromResources("actions/tool_elliptical_selection.png"));
        registerNodeType("ij1-analyze-image-statistics", ImageStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("ij1-analyze-image-statistics-expression", ImageStatisticsExpressionAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("ij1-analyze-annotate-by-image-statistics-expression", AnnotateByImageStatisticsExpressionAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("ij1-analyze-statistics-histogram", HistogramGenerator.class, UIUtils.getIconURLFromResources("actions/office-chart-bar.png"));
        registerNodeType("ij1-analyze-statistics-pixels-greyscale", GreyscalePixelsGenerator.class, UIUtils.getIconURLFromResources("actions/statistics.png"));

        registerEnumParameterType("ij1-analyze-statistics-histogram:multi-channel-mode", HistogramGenerator.MultiChannelMode.class,
                "Multichannel mode", "Available modes");
    }

    private void registerDimensionAlgorithms() {
        registerNodeType("ij1-dimensions-stack-to-2d", StackTo2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/layer-bottom.png"));
        registerNodeType("ij1-dimensions-stacksplitter", StackSplitterAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-dimensions-hyper-stacksplitter", HyperstackSplitterAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-dimensions-expression-slicer", ExpressionSlicerAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-dimensions-stackmerger", CreateStackAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-cuboid.png"));
        registerNodeType("ij1-dimensions-stacks-to-dimension", StackToDimensionMergerAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-cuboid.png"));
        registerNodeType("ij1-dimensions-stackinverter", StackInverterAlgorithm.class, UIUtils.getIconURLFromResources("actions/layer-previous.png"));
        registerNodeType("ij1-dimensions-zproject", ZProjectorAlgorithm.class, UIUtils.getIconURLFromResources("actions/layer-bottom.png"));
        registerNodeType("ij1-dimensions-stack2montage", StackToMontageAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-grid.png"));
        registerNodeType("ij1-dimensions-montage2stack", MontageToStackAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-grid.png"));
        registerNodeType("ij1-dimensions-reorder", ReorderDimensionsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-dimensions-stack-combine", StackCombinerAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-dimensions-inpput2montage", InputImagesToMontage.class, UIUtils.getIconURLFromResources("actions/view-grid.png"));

        registerEnumParameterType("ij1-dimensions-zproject:method", ZProjectorAlgorithm.Method.class,
                "Method", "Available methods");
    }

    private void registerThresholdAlgorithms() {
        registerNodeType("ij1-threshold-manual2d-color-hsb", ManualHSBThreshold2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-manual2d-color-rgb", ManualRGBThreshold2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-manual2d-color-lab", ManualLABThreshold2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-manual2d-8u", ManualThreshold8U2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-percentile2d-8u", PercentileThreshold8U2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-manual2d-16u", ManualThreshold16U2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-manual2d-32f", ManualThreshold32F2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-auto2d", AutoThreshold2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-expression2d-8u", CustomAutoThreshold2D8UAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-expression2d-16u", CustomAutoThreshold2D16UAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-expression2d-32f", CustomAutoThreshold2D32FAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-expression2d-color", ColorThresholdExpression2D.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-local-auto2d", LocalAutoThreshold2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-local-auto2d-bernsen", BernsenLocalAutoThreshold2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-local-auto2d-niblack", NiblackLocalAutoThreshold2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-local-auto2d-sauvola", SauvolaLocalAutoThreshold2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-local-auto2d-phansalkar", PhansalkarLocalAutoThreshold2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-local-auto2d-contrast", ContrastLocalAutoThreshold2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("threshold-brightspots2d", BrightSpotsSegmentation2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("threshold-hessian2d", HessianSegmentation2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("threshold-hough2d", HoughSegmentation2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("threshold-hough2d-fast", FastHoughSegmentation2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("threshold-internalgradient2d", InternalGradientSegmentation2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("threshold-by-annotation", ThresholdByAnnotation2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));

        registerEnumParameterType(AutoThresholder.Method.class.getCanonicalName(), AutoThresholder.Method.class,
                "Auto threshold method", "Available methods");
        registerEnumParameterType("slice-threshold-mode", AutoThreshold2DAlgorithm.SliceThresholdMode.class,
                "Slice thresholding mode", "How multi-slice images are thresholded");
        registerEnumParameterType(LocalAutoThreshold2DAlgorithm.Method.class.getCanonicalName(), LocalAutoThreshold2DAlgorithm.Method.class,
                "Local auto threshold method", "Available methods");
        registerEnumParameterType("ij1:eigenvalue-selection-2d", EigenvalueSelection2D.class,
                "Eigenvalue selection (2D)", "Determines whether to choose the smallest or largest Eigenvalue");
        registerEnumParameterType("ij1:roi-outline", RoiOutline.class,
                "ROI outline", "Available ways to outline a ROI");
    }

    private void registerSharpenAlgorithms() {
        registerNodeType("ij1-sharpen-laplacian2d", LaplacianSharpen2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
    }

    private void registerBackgroundAlgorithms() {
        registerNodeType("ij1-background-rollingball2d", RollingBallBackgroundEstimator2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerEnumParameterType("ij1-background-rollingball2d:background-type", RollingBallBackgroundEstimator2DAlgorithm.BackgroundType.class,
                "Background type", "Available background types");
        registerEnumParameterType("ij1-background-rollingball2d:background-method", RollingBallBackgroundEstimator2DAlgorithm.Method.class,
                "Rolling ball method", "Available methods");
    }

    private void registerNoiseAlgorithms() {
        registerNodeType("ij1-noise-addnormalnoise2d", AddNoise2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-noise-add-salt-and-pepper-noise2d", AddSaltAndPepperNoise2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-noise-despeckle2d", DespeckleFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/image-auto-adjust.png"));
        registerNodeType("ij1-noise-removeoutliers2d", RemoveOutliersFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-eraser.png"));

        registerEnumParameterType("ij1-noise-removeoutliers2d:mode", RemoveOutliersFilter2DAlgorithm.Mode.class,
                "Mode", "Available modes");
    }

    private void registerBinaryAlgorithms() {
        registerNodeType("ij1-binary-dtwatershed2d", DistanceTransformWatershed2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-binary-voronoi2d", Voronoi2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-binary-uep2d", UltimateErodedPoints2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-binary-bitwise", BitwiseLogicalOperationAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-tweak-paint.png"));
        registerNodeType("ij1-binary-connected-component-labeling-2d", ConnectedComponentsLabeling2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-tweak-jitter-color.png"));
        registerNodeType("ij1-binary-connected-component-labeling-3d", ConnectedComponentsLabeling3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-tweak-jitter-color.png"));
        registerNodeType("ij1-binary-binarize", BinarizeAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-binary-convexify", ConvexifyAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-polygon.png"));
        registerNodeType("ij1-binary-keep-largest-region", KeepLargestRegionAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-binary-remove-largest-region", RemoveLargestRegionAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-binary-volume-opening-2d", VolumeOpening2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-binary-volume-opening-3d", VolumeOpening3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-binary-conditional-invert", ConditionalInverterAlgorithm.class, UIUtils.getIconURLFromResources("actions/invertimage.png"));
    }

    private void registerMorphologyAlgorithms() {
//        registerNodeType("ij1-morph-binary-operation2d", MorphologyBinary2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/morphology.png"));
        registerNodeType("ij1-morph-operation2d", Morphology2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/morphology.png"));
        registerNodeType("ij1-morph-operation3d", Morphology3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/morphology.png"));
        registerNodeType("ij1-morph-binary-fillholes2d", MorphologyFillHoles2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-fill.png"));
        registerNodeType("ij1-morph-binary-outline2d", MorphologyOutline2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-connector.png"));
        registerNodeType("ij1-morph-binary-skeletonize2d", MorphologySkeletonize2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-to-path.png"));
        registerNodeType("ij1-morph-reconstruct-2d", MorphologicalReconstruction2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-morph-reconstruct-3d", MorphologicalReconstruction3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerEnumParameterType("ij1-morph-binary-operation2d:operation", MorphologyBinary2DAlgorithm.Operation.class,
                "Operation", "Available operations");
        registerEnumParameterType("ij1-morph:operation", Morphology.Operation.class,
                "Operation", "Available operations");
        registerEnumParameterType("ij1-morph:strel", Strel.Shape.class,
                "Structure element", "Available shapes");
        registerEnumParameterType("ij1-morph:strel-3d", Strel3D.Shape.class,
                "Structure element (3D)", "Available shapes");
    }

    private void registerMathAlgorithms() {
        registerNodeType("ij1-math-math2d", ApplyMath2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-transform2d", ApplyTransform2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-math2d-expression", ApplyMathExpression2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-math2d-expression-color", ApplyColorMathExpression2DExpression.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-vector-expression", ApplyVectorMathExpression2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-edt2d", ApplyDistanceTransform2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-variance2d", LocalVarianceFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-maximum2d", LocalMaximumFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-minimum2d", LocalMinimumFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-variance3d", LocalVarianceFilter3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-maximum3d", LocalMaximumFilter3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-minimum3d", LocalMinimumFilter3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-imagecalculator2d-expression", LocalImageCalculator2DExpression.class, UIUtils.getIconURLFromResources("actions/calculator.png"));
        registerNodeType("ij1-math-replace-nan-by-median2d", RemoveNaNFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-imagecalculator2d", ImageCalculator2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/calculator.png"));
        registerNodeType("ij1-math-imagecalculator2d-expression", ImageCalculator2DExpression.class, UIUtils.getIconURLFromResources("actions/calculator.png"));
        registerNodeType("ij1-math-hessian2d", Hessian2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-divide-by-maximum", DivideByMaximumAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerNodeType("ij1-math-regional-minima-2d", RegionalMinima2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-regional-maxima-2d", RegionalMaxima2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-regional-minima-3d", RegionalMinima3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-regional-maxima-3d", RegionalMaxima3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-extended-minima-2d", ExtendedMinima2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-extended-maxima-2d", ExtendedMaxima2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-extended-minima-3d", ExtendedMinima3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-extended-maxima-3d", ExtendedMaxima3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerNodeType("ij1-math-impose-maxima-2d", ImposeMaxima2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-impose-maxima-3d", ImposeMaxima3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerNodeType("ij1-math-generate-from-expression", GenerateFromMathExpression2D.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-generate-vector-from-expression", GenerateVectorFromMathExpression.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij-imgplus-from-matrix", ImageFromMatrix2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/table.png"));

        registerNodeType("ij1-math-generate-missing-from-expression", GenerateMissingImageFromMathExpression2D.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-generate-missing-zero-image", GenerateMissingZeroImage.class, UIUtils.getIconURLFromResources("actions/add.png"));
        registerNodeType("ij1-generate-zero-image", GenerateZeroImage.class, UIUtils.getIconURLFromResources("data-types/imgplus.png"));

        registerNodeType("ij-math-chamfer-distance-map-2d", ChamferDistanceMap2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij-math-chamfer-distance-map-3d", ChamferDistanceMap3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij-math-label-chamfer-distance-map-3d", LabelChamferDistanceMap3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij-math-geodesic-distance-map-2d", GeodesicDistanceMap2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerEnumParameterType("ij-math-chamfer-distance-map-2d:weights", ChamferWeights.class, "Chamfer weights (2D)", "Predefined weights for the Chamfer distance map");
        registerEnumParameterType("ij-math-chamfer-distance-map-23:weights", ChamferWeights3D.class, "Chamfer weights (3D)", "Predefined weights for the Chamfer distance map");

        registerEnumParameterType("ij1-math-math2d:transformation", ApplyMath2DAlgorithm.Transformation.class,
                "Transformation", "Available transformations");
        registerEnumParameterType("ij1-math-transform2d:transformation", ApplyTransform2DAlgorithm.Transformation.class,
                "Transformation", "Available transformations");

        registerEnumParameterType("ij1-math-imagecalculator2d:operation", ImageCalculator2DAlgorithm.Operation.class,
                "Operation", "Available operations");
        registerEnumParameterType("ij1-math-imagecalculator2d:operand", ImageCalculator2DAlgorithm.Operand.class,
                "Operand", "Available operands");
    }

    private void registerFeatureAlgorithms() {
        registerNodeType("ij1-feature-vesselness-frangi", FrangiVesselnessFeatures.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("feature-vesselness-meijering2d", MeijeringVesselness2DFeatures.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-maxima-local-2d", LocalMaxima2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-difference-of-gaussian", DifferenceOfGaussian2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-laplacian-of-gaussian", LaplacianOfGaussian2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-feature-directional-filter-2d", DirectionalFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-tweak-rotate.png"));

        registerEnumParameterType("ij1-feature-vesselness-frangi:slicing-mode", FrangiVesselnessFeatures.SlicingMode.class,
                "Slicing mode", "Available slicing modes");
        registerEnumParameterType("ij1-feature-maxima-local-2d:output-type", LocalMaxima2DAlgorithm.OutputType.class,
                "Output type", "Available output types");
        registerEnumParameterType("ij1-feature-directional-filter:operation", DirectionalFilter.Operation.class,
                "Directional filter operation", "Available operations");
        registerEnumParameterType("ij1-feature-directional-filter:type", DirectionalFilter.Type.class,
                "Directional filter types", "Available types");
    }

    private void registerContrastAlgorithms() {
        registerNodeType("ij1-contrast-clahe", CLAHEContrastEnhancer.class, UIUtils.getIconURLFromResources("actions/contrast.png"));
        registerNodeType("ij1-contrast-illumination-correction2d", IlluminationCorrection2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/contrast.png"));
        registerNodeType("ij1-contrast-calibrate", DisplayRangeCalibrationAlgorithm.class, UIUtils.getIconURLFromResources("actions/contrast.png"));
        registerNodeType("ij1-contrast-histogram-enhancer", HistogramContrastEnhancerAlgorithm.class, UIUtils.getIconURLFromResources("actions/contrast.png"));

        registerEnumParameterType(HistogramContrastEnhancerAlgorithm.Method.class.getCanonicalName(), HistogramContrastEnhancerAlgorithm.Method.class,
                "Histogram contrast enhancer method", "Available methods");
    }

    private void registerEdgeAlgorithms() {
        registerNodeType("ij1-edge-sobel", SobelEdgeDetectorAlgorithm.class, UIUtils.getIconURLFromResources("actions/path-offset-dynamic.png"));
        registerNodeType("ij1-edge-laplacian", LaplacianEdgeDetectorAlgorithm.class, UIUtils.getIconURLFromResources("actions/path-offset-dynamic.png"));
        registerEnumParameterType("ij1-edge-laplacian:mode",
                LaplacianEdgeDetectorAlgorithm.Mode.class,
                "Laplacian type",
                "The type of laplacian");
        registerNodeType("ij1-edge-canny", CannyEdgeDetectorAlgorithm.class, UIUtils.getIconURLFromResources("actions/path-offset-dynamic.png"));
    }

    private void registerColorAlgorithms() {
        registerNodeType("ij1-color-invert", InvertColorsAlgorithm.class, UIUtils.getIconURLFromResources("actions/invertimage.png"));
        registerNodeType("ij1-color-merge-channels", MergeChannelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("ij1-color-arrange-channels", ArrangeChannelsAlgorithm.class);
        registerNodeType("ij1-color-split-channels", SplitChannelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-color-combine-rgb", CombineChannelsToRGBAlgorithm.class, UIUtils.getIconURLFromResources("actions/colors-rgb.png"));
        registerNodeType("ij1-color-split-rgb", SplitRGBChannelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/channelmixer.png"));
        registerNodeType("ij1-color-set-to-color", SetToColorAlgorithm.class, UIUtils.getIconURLFromResources("actions/color-fill.png"));
        registerNodeType("ij1-color-set-to-grayscale-value", SetToValueAlgorithm.class, UIUtils.getIconURLFromResources("actions/color-fill.png"));
        registerNodeType("ij1-color-to-rgb", ToRGBAlgorithm.class, UIUtils.getIconURLFromResources("actions/colors-rgb.png"));
        registerNodeType("ij1-color-to-greyscale-expression", ColorToGreyscaleExpression2D.class, UIUtils.getIconURLFromResources("actions/color-picker-grey.png"));

        registerEnumParameterType("ij1-color-merge-channels:channel-color", MergeChannelsAlgorithm.ChannelColor.class,
                "Channel color", "Available channel colors");
    }

    private void registerBlurAlgorithms() {
        registerNodeType("ij1-blur-gaussian2d", GaussianBlur2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-gaussian3d", GaussianBlur3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-box2d", BoxFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-box3d", BoxFilter3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-median2d-8u", MedianBlurGreyscale8U2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-median2d-rgb", MedianBlurRGB2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-median2d", MedianBlurFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-blur-median3d", MedianBlurFilter3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:imagej-algorithms";
    }

    @Override
    public String getDependencyVersion() {
        return "1.46.0";
    }

    private static Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> get3DConversion() {
        Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> result = new HashMap<>();

        result.put(ImagePlusData.class, ImagePlus3DData.class);
        result.put(ImagePlusGreyscaleData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlusGreyscale8UData.class, ImagePlus3DGreyscale8UData.class);
        result.put(ImagePlusGreyscaleMaskData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlusGreyscale16UData.class, ImagePlus3DGreyscale16UData.class);
        result.put(ImagePlusGreyscale32FData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlusColorHSBData.class, ImagePlus3DColorHSBData.class);
        result.put(ImagePlusColorRGBData.class, ImagePlus3DColorRGBData.class);

        result.put(ImagePlus2DData.class, ImagePlus3DData.class);
        result.put(ImagePlus2DGreyscaleData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus2DGreyscale8UData.class, ImagePlus3DGreyscale8UData.class);
        result.put(ImagePlus2DGreyscaleMaskData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus2DGreyscale16UData.class, ImagePlus3DGreyscale16UData.class);
        result.put(ImagePlus2DGreyscale32FData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus2DColorHSBData.class, ImagePlus3DColorHSBData.class);
        result.put(ImagePlus2DColorRGBData.class, ImagePlus3DColorRGBData.class);

        result.put(ImagePlus4DData.class, ImagePlus3DData.class);
        result.put(ImagePlus4DGreyscaleData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus4DGreyscale8UData.class, ImagePlus3DGreyscale8UData.class);
        result.put(ImagePlus4DGreyscaleMaskData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscale16UData.class, ImagePlus3DGreyscale16UData.class);
        result.put(ImagePlus4DGreyscale32FData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus4DColorHSBData.class, ImagePlus3DColorHSBData.class);
        result.put(ImagePlus4DColorRGBData.class, ImagePlus3DColorRGBData.class);

        result.put(ImagePlus5DData.class, ImagePlus3DData.class);
        result.put(ImagePlus5DGreyscaleData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus5DGreyscale8UData.class, ImagePlus3DGreyscale8UData.class);
        result.put(ImagePlus5DGreyscaleMaskData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscale16UData.class, ImagePlus3DGreyscale16UData.class);
        result.put(ImagePlus5DGreyscale32FData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus5DColorHSBData.class, ImagePlus3DColorHSBData.class);
        result.put(ImagePlus5DColorRGBData.class, ImagePlus3DColorRGBData.class);

        return result;
    }

    private static Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> get2DConversion() {
        Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> result = new HashMap<>();

        result.put(ImagePlusData.class, ImagePlus2DData.class);
        result.put(ImagePlusGreyscaleData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlusGreyscale8UData.class, ImagePlus2DGreyscale8UData.class);
        result.put(ImagePlusGreyscaleMaskData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlusGreyscale16UData.class, ImagePlus2DGreyscale16UData.class);
        result.put(ImagePlusGreyscale32FData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlusColorHSBData.class, ImagePlus2DColorHSBData.class);
        result.put(ImagePlusColorRGBData.class, ImagePlus2DColorRGBData.class);

        result.put(ImagePlus3DData.class, ImagePlus2DData.class);
        result.put(ImagePlus3DGreyscaleData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlus3DGreyscale8UData.class, ImagePlus2DGreyscale8UData.class);
        result.put(ImagePlus3DGreyscaleMaskData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus3DGreyscale16UData.class, ImagePlus2DGreyscale16UData.class);
        result.put(ImagePlus3DGreyscale32FData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus3DColorHSBData.class, ImagePlus2DColorHSBData.class);
        result.put(ImagePlus3DColorRGBData.class, ImagePlus2DColorRGBData.class);

        result.put(ImagePlus4DData.class, ImagePlus2DData.class);
        result.put(ImagePlus4DGreyscaleData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlus4DGreyscale8UData.class, ImagePlus2DGreyscale8UData.class);
        result.put(ImagePlus4DGreyscaleMaskData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscale16UData.class, ImagePlus2DGreyscale16UData.class);
        result.put(ImagePlus4DGreyscale32FData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus4DColorHSBData.class, ImagePlus2DColorHSBData.class);
        result.put(ImagePlus4DColorRGBData.class, ImagePlus2DColorRGBData.class);

        result.put(ImagePlus5DData.class, ImagePlus2DData.class);
        result.put(ImagePlus5DGreyscaleData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlus5DGreyscale8UData.class, ImagePlus2DGreyscale8UData.class);
        result.put(ImagePlus5DGreyscaleMaskData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscale16UData.class, ImagePlus2DGreyscale16UData.class);
        result.put(ImagePlus5DGreyscale32FData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus5DColorHSBData.class, ImagePlus2DColorHSBData.class);
        result.put(ImagePlus5DColorRGBData.class, ImagePlus2DColorRGBData.class);

        return result;
    }

    private static Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> getDecreaseDimensionConversion() {
        Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> result = new HashMap<>();

        result.put(ImagePlus3DData.class, ImagePlus2DData.class);
        result.put(ImagePlus3DGreyscaleData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlus3DGreyscale8UData.class, ImagePlus2DGreyscale8UData.class);
        result.put(ImagePlus3DGreyscaleMaskData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus3DGreyscale16UData.class, ImagePlus2DGreyscale16UData.class);
        result.put(ImagePlus3DGreyscale32FData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus3DColorHSBData.class, ImagePlus2DColorHSBData.class);
        result.put(ImagePlus3DColorRGBData.class, ImagePlus2DColorRGBData.class);

        result.put(ImagePlus4DData.class, ImagePlus3DData.class);
        result.put(ImagePlus4DGreyscaleData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus4DGreyscale8UData.class, ImagePlus3DGreyscale8UData.class);
        result.put(ImagePlus4DGreyscaleMaskData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscale16UData.class, ImagePlus3DGreyscale16UData.class);
        result.put(ImagePlus4DGreyscale32FData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus4DColorHSBData.class, ImagePlus3DColorHSBData.class);
        result.put(ImagePlus4DColorRGBData.class, ImagePlus3DColorRGBData.class);

        result.put(ImagePlus5DData.class, ImagePlus4DData.class);
        result.put(ImagePlus5DGreyscaleData.class, ImagePlus4DGreyscaleData.class);
        result.put(ImagePlus5DGreyscale8UData.class, ImagePlus4DGreyscale8UData.class);
        result.put(ImagePlus5DGreyscaleMaskData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscale16UData.class, ImagePlus4DGreyscale16UData.class);
        result.put(ImagePlus5DGreyscale32FData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus5DColorHSBData.class, ImagePlus4DColorHSBData.class);
        result.put(ImagePlus5DColorRGBData.class, ImagePlus4DColorRGBData.class);

        return result;
    }

    private static Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> getMaskQualifierMap() {
        Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> result = new HashMap<>();

        result.put(ImagePlusData.class, ImagePlusGreyscaleMaskData.class);
        result.put(ImagePlusGreyscaleData.class, ImagePlusGreyscaleMaskData.class);
        result.put(ImagePlusGreyscale8UData.class, ImagePlusGreyscaleMaskData.class);
        result.put(ImagePlusGreyscale16UData.class, ImagePlusGreyscaleMaskData.class);
        result.put(ImagePlusGreyscale32FData.class, ImagePlusGreyscaleMaskData.class);
        result.put(ImagePlusColorHSBData.class, ImagePlusGreyscaleMaskData.class);
        result.put(ImagePlusColorRGBData.class, ImagePlusGreyscaleMaskData.class);

        result.put(ImagePlus2DData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DGreyscaleData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DGreyscale8UData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DGreyscale16UData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DGreyscale32FData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DColorHSBData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DColorRGBData.class, ImagePlus2DGreyscaleMaskData.class);

        result.put(ImagePlus3DData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DGreyscaleData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DGreyscale8UData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DGreyscale16UData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DGreyscale32FData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DColorHSBData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DColorRGBData.class, ImagePlus3DGreyscaleMaskData.class);

        result.put(ImagePlus4DData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscaleData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscale8UData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscale16UData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscale32FData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DColorHSBData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DColorRGBData.class, ImagePlus4DGreyscaleMaskData.class);

        result.put(ImagePlus5DData.class, ImagePlus5DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscaleData.class, ImagePlus5DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscale8UData.class, ImagePlus5DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscale16UData.class, ImagePlus5DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscale32FData.class, ImagePlus5DGreyscaleMaskData.class);
        result.put(ImagePlus5DColorHSBData.class, ImagePlus5DGreyscaleMaskData.class);
        result.put(ImagePlus5DColorRGBData.class, ImagePlus5DGreyscaleMaskData.class);

        return result;
    }

    private static Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> getToGrayscale32FConversion() {
        Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> result = new HashMap<>();

        result.put(ImagePlusData.class, ImagePlusGreyscale32FData.class);
        result.put(ImagePlusGreyscaleData.class, ImagePlusGreyscale32FData.class);
        result.put(ImagePlusGreyscale8UData.class, ImagePlusGreyscale32FData.class);
        result.put(ImagePlusGreyscaleMaskData.class, ImagePlusGreyscale32FData.class);
        result.put(ImagePlusGreyscale16UData.class, ImagePlusGreyscale32FData.class);
        result.put(ImagePlusColorHSBData.class, ImagePlusGreyscale32FData.class);
        result.put(ImagePlusColorRGBData.class, ImagePlusGreyscale32FData.class);

        result.put(ImagePlus2DData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DGreyscaleData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DGreyscale8UData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DGreyscaleMaskData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DGreyscale16UData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DColorHSBData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DColorRGBData.class, ImagePlus2DGreyscale32FData.class);

        result.put(ImagePlus3DData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DGreyscaleData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DGreyscale8UData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DGreyscaleMaskData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DGreyscale16UData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DColorHSBData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DColorRGBData.class, ImagePlus3DGreyscale32FData.class);

        result.put(ImagePlus4DData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DGreyscaleData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DGreyscale8UData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DGreyscaleMaskData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DGreyscale16UData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DColorHSBData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DColorRGBData.class, ImagePlus4DGreyscale32FData.class);

        result.put(ImagePlus5DData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DGreyscaleData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DGreyscale8UData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DGreyscaleMaskData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DGreyscale16UData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DColorHSBData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DColorRGBData.class, ImagePlus5DGreyscale32FData.class);

        return result;
    }

    private static Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> getToGrayscaleConversion() {
        Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> result = new HashMap<>();

        result.put(ImagePlusData.class, ImagePlusGreyscaleData.class);
        result.put(ImagePlusColorHSBData.class, ImagePlusGreyscaleData.class);
        result.put(ImagePlusColorRGBData.class, ImagePlusGreyscaleData.class);

        result.put(ImagePlus2DData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlus2DColorHSBData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlus2DColorRGBData.class, ImagePlus2DGreyscaleData.class);

        result.put(ImagePlus3DData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus3DColorHSBData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus3DColorRGBData.class, ImagePlus3DGreyscaleData.class);

        result.put(ImagePlus4DData.class, ImagePlus4DGreyscaleData.class);
        result.put(ImagePlus4DColorHSBData.class, ImagePlus4DGreyscaleData.class);
        result.put(ImagePlus4DColorRGBData.class, ImagePlus4DGreyscaleData.class);

        result.put(ImagePlus5DData.class, ImagePlus5DGreyscaleData.class);
        result.put(ImagePlus5DColorHSBData.class, ImagePlus5DGreyscaleData.class);
        result.put(ImagePlus5DColorRGBData.class, ImagePlus5DGreyscaleData.class);

        return result;
    }

    private static Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> getToColorRgbConversion() {
        Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> result = new HashMap<>();

        result.put(ImagePlusData.class, ImagePlusColorRGBData.class);
        result.put(ImagePlusGreyscaleData.class, ImagePlusColorRGBData.class);
        result.put(ImagePlusGreyscale8UData.class, ImagePlusColorRGBData.class);
        result.put(ImagePlusGreyscale16UData.class, ImagePlusColorRGBData.class);
        result.put(ImagePlusGreyscale32FData.class, ImagePlusColorRGBData.class);
        result.put(ImagePlusGreyscaleMaskData.class, ImagePlusColorRGBData.class);

        result.put(ImagePlus2DData.class, ImagePlus2DColorRGBData.class);
        result.put(ImagePlus2DGreyscaleData.class, ImagePlus2DColorRGBData.class);
        result.put(ImagePlus2DGreyscale8UData.class, ImagePlus2DColorRGBData.class);
        result.put(ImagePlus2DGreyscale16UData.class, ImagePlus2DColorRGBData.class);
        result.put(ImagePlus2DGreyscale32FData.class, ImagePlus2DColorRGBData.class);
        result.put(ImagePlus2DGreyscaleMaskData.class, ImagePlus2DColorRGBData.class);

        result.put(ImagePlus3DData.class, ImagePlus3DColorRGBData.class);
        result.put(ImagePlus3DGreyscaleData.class, ImagePlus3DColorRGBData.class);
        result.put(ImagePlus3DGreyscale8UData.class, ImagePlus3DColorRGBData.class);
        result.put(ImagePlus3DGreyscale16UData.class, ImagePlus3DColorRGBData.class);
        result.put(ImagePlus3DGreyscale32FData.class, ImagePlus3DColorRGBData.class);
        result.put(ImagePlus3DGreyscaleMaskData.class, ImagePlus3DColorRGBData.class);

        result.put(ImagePlus4DData.class, ImagePlus4DColorRGBData.class);
        result.put(ImagePlus4DGreyscaleData.class, ImagePlus4DColorRGBData.class);
        result.put(ImagePlus4DGreyscale8UData.class, ImagePlus4DColorRGBData.class);
        result.put(ImagePlus4DGreyscale16UData.class, ImagePlus4DColorRGBData.class);
        result.put(ImagePlus4DGreyscale32FData.class, ImagePlus4DColorRGBData.class);
        result.put(ImagePlus4DGreyscaleMaskData.class, ImagePlus4DColorRGBData.class);

        result.put(ImagePlus5DData.class, ImagePlus5DColorRGBData.class);
        result.put(ImagePlus5DGreyscaleData.class, ImagePlus5DColorRGBData.class);
        result.put(ImagePlus5DGreyscale8UData.class, ImagePlus5DColorRGBData.class);
        result.put(ImagePlus5DGreyscale16UData.class, ImagePlus5DColorRGBData.class);
        result.put(ImagePlus5DGreyscale32FData.class, ImagePlus5DColorRGBData.class);
        result.put(ImagePlus5DGreyscaleMaskData.class, ImagePlus5DColorRGBData.class);

        return result;
    }
}


