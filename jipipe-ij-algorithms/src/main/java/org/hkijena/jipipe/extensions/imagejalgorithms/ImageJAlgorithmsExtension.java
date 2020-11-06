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
import ij.process.AutoThresholder;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.EigenvalueSelection2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.HyperstackDimensionPairParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.InterpolationMethod;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.MacroWrapperAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.analyze.FindParticles2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.background.RollingBallBackgroundEstimator2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.binary.DistanceTransformWatershed2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.binary.UltimateErodedPoints2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.binary.Voronoi2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.BoxFilter2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.BoxFilter3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.GaussianBlur2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.GaussianBlur3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.MedianBlurFilter2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.MedianBlurFilter3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.MedianBlurGreyscale8U2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur.MedianBlurRGB2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.color.ArrangeChannelsAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.color.InvertColorsAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.color.MergeChannelsAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.color.SplitChannelsAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.contrast.CLAHEContrastEnhancer;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.contrast.IlluminationCorrection2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.convolve.Convolve2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.edge.SobelEdgeDetectorAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features.FrangiVesselnessFeatures;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features.LocalMaxima2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features.MeijeringVesselness2DFeatures;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.fft.FFT2DForwardTransform;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.fft.FFT2DInverseTransform;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.fft.FFT2DSwapQuadrants;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.generate.GenerateStructureElementAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io.ImagePlusFromGUI;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io.ImagePlusToGUI;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io.ROIFromGUI;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io.ROIToGUI;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io.ResultsTableFromGUI;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io.ResultsTableToGUI;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.lut.LUTInverterAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.lut.RemoveLUTAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.lut.SetLUTFromColorAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.misc.DataToPreviewAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.Morphology2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.MorphologyBinary2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.MorphologyFillHoles2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.MorphologyOutline2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology.MorphologySkeletonize2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.noise.AddNoise2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.noise.DespeckleFilter2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.noise.RemoveOutliersFilter2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.sharpen.LaplacianSharpen2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.statistics.GreyscalePixelsGenerator;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.statistics.HistogramGenerator;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TransformCrop2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TransformEqualCanvasSize2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TransformExpandCanvas2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TransformFlip2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TransformRotate2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TransformScale2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TransformScale3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.DisplayRangeCalibrationAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColor8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColor8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColor8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColor8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.ImagePlus5DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColor8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorData;
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
import org.hkijena.jipipe.extensions.imagejdatatypes.util.CalibrationMode;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.RoiOutline;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementColumn;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementColumnSortOrder;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
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
    public static final Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> TO_COLOR_CONVERSION = getToColorConversion();

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
    public String getDescription() {
        return "Integrates ImageJ algorithms into JIPipe";
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

        registerNodeType("ij1-generate-filter-kernel", GenerateStructureElementAlgorithm.class, UIUtils.getIconURLFromResources("actions/morphology.png"));
        registerNodeType("ij1-data-to-preview", DataToPreviewAlgorithm.class, UIUtils.getIconURLFromResources("actions/viewimage.png"));
        registerNodeType("external-imagej-macro", MacroWrapperAlgorithm.class, UIUtils.getIconURLFromResources("apps/imagej.png"));

        // Register enum parameters
        registerGlobalEnums();

        // Register other parameters
        registerGlobalParameters();

//        registerIJ2Algorithms();
    }

    private void registerIOAlgorithms() {
        registerNodeType("ij-imgplus-from-gui", ImagePlusFromGUI.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
        registerNodeType("ij-imgplus-to-gui", ImagePlusToGUI.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
        registerNodeType("ij-results-table-from-gui", ResultsTableFromGUI.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
        registerNodeType("ij-results-table-to-gui", ResultsTableToGUI.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
        registerNodeType("ij-roi-from-gui", ROIFromGUI.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
        registerNodeType("ij-roi-to-gui", ROIToGUI.class, UIUtils.getIconURLFromResources("apps/imagej.png"));
    }

    private void registerLUTAlgorithms() {
        registerNodeType("ij1-remove-lut", RemoveLUTAlgorithm.class, UIUtils.getIconURLFromResources("actions/paint-gradient-linear.png"));
        registerNodeType("ij1-set-lut-from-colors", SetLUTFromColorAlgorithm.class, UIUtils.getIconURLFromResources("actions/paint-gradient-linear.png"));
        registerNodeType("ij1-invert-lut", LUTInverterAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-inverse.png"));
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
        registerEnumParameterType("ij1-calibration-mode", CalibrationMode.class,
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
    }

    private void registerROIAlgorithms() {
        registerNodeType("ij1-roi-from-rectangles", DefineRectangularRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-from-rectangles-referenced", ReferencedDefineRectangularRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-append-rectangles", AppendRectangularRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-append-rectangles-referenced", ReferencedAppendRectangularRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-rectangle.png"));
        registerNodeType("ij1-roi-split", SplitRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-roi-explode", ExplodeRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-roi-merge", MergeRoiListsAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("ij1-roi-calculator", RoiCalculatorAlgorithm.class, UIUtils.getIconURLFromResources("actions/calculator.png"));
        registerNodeType("ij1-roi-to-mask-unreferenced", UnreferencedRoiToMaskAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-roi-to-mask", RoiToMaskAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-roi-outline", OutlineRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-connector.png"));
        registerNodeType("ij1-roi-remove-bordering", RemoveBorderRoisAlgorithm.class, UIUtils.getIconURLFromResources("actions/bordertool.png"));
        registerNodeType("ij1-roi-statistics", RoiStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("ij1-roi-filter-statistics", FilterRoiByStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("ij1-roi-color-statistics", ColorRoiByStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/fill-color.png"));
        registerNodeType("ij1-roi-sort-and-extract-statistics", SortAndExtractRoiByStatisticsAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-sort.png"));
        registerNodeType("ij1-roi-set-properties", ChangeRoiPropertiesAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-edit.png"));
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

        registerEnumParameterType("ij1-roi-from-table-rectangular:anchor",
                TableToRectangularROIAlgorithm.Anchor.class,
                "Anchor",
                "Describes how objects are created.");
        registerEnumParameterType("ij1-roi-from-table-rectangular:mode",
                TableToRectangularROIAlgorithm.Mode.class,
                "Mode",
                "Describes which objects are created.");
    }

    private void registerConvolutionAlgorithms() {
        registerNodeType("ij1-convolve-convolve2d", Convolve2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
    }

    private void registerTransformationAlgorithms() {
        registerNodeType("ij1-transform-flip2d", TransformFlip2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-flip-horizontal.png"));
        registerNodeType("ij1-transform-rotate2d", TransformRotate2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-rotate.png"));
        registerNodeType("ij1-transform-scale2d", TransformScale2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-scale.png"));
        registerNodeType("ij1-transform-scale3d", TransformScale3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-scale.png"));
        registerNodeType("ij1-transform-crop2d", TransformCrop2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/image-crop.png"));
        registerNodeType("ij1-transform-crop-to-roi", CropToRoiAlgorithm.class, UIUtils.getIconURLFromResources("actions/image-crop.png"));
        registerNodeType("ij1-transform-expand2d", TransformExpandCanvas2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-scale.png"));
        registerNodeType("ij1-transform-equalize-expand2d", TransformEqualCanvasSize2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/transform-scale.png"));

        registerEnumParameterType("ij1-transform-flip2d:flip-mode", TransformFlip2DAlgorithm.FlipMode.class,
                "Flip mode", "Available modes");
        registerEnumParameterType("ij1-transform-rotate2d:rotation-mode", TransformRotate2DAlgorithm.RotationMode.class,
                "Rotation mode", "Available modes");
    }

    private void registerFFTAlgorithms() {
        registerNodeType("ij1-fft-forward2d", FFT2DForwardTransform.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-fft-inverse2d", FFT2DInverseTransform.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-fft-swap2d", FFT2DSwapQuadrants.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
    }

    private void registerAnalysisAlgorithms() {
        registerNodeType("ij1-analyze-find-particles2d", FindParticles2D.class, UIUtils.getIconURLFromResources("actions/tool_elliptical_selection.png"));
        registerNodeType("ij1-analyze-statistics-histogram", HistogramGenerator.class, UIUtils.getIconURLFromResources("actions/office-chart-bar.png"));
        registerNodeType("ij1-analyze-statistics-pixels-greyscale", GreyscalePixelsGenerator.class, UIUtils.getIconURLFromResources("actions/statistics.png"));

        registerEnumParameterType("ij1-analyze-statistics-histogram:multi-channel-mode", HistogramGenerator.MultiChannelMode.class,
                "Multichannel mode", "Available modes");
    }

    private void registerDimensionAlgorithms() {
        registerNodeType("ij1-dimensions-stack-to-2d", StackTo2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/layer-bottom.png"));
        registerNodeType("ij1-dimensions-stacksplitter", StackSplitterAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-dimensions-stackmerger", StackMergerAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-cuboid.png"));
        registerNodeType("ij1-dimensions-stackinverter", StackInverterAlgorithm.class, UIUtils.getIconURLFromResources("actions/layer-previous.png"));
        registerNodeType("ij1-dimensions-zproject", ZProjectorAlgorithm.class, UIUtils.getIconURLFromResources("actions/layer-bottom.png"));
        registerNodeType("ij1-dimensions-stack2montage", StackToMontageAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-grid.png"));
        registerNodeType("ij1-dimensions-montage2stack", MontageToStackAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-grid.png"));
        registerNodeType("ij1-dimensions-reorder", ReorderDimensionsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("ij1-dimensions-inpput2montage", InputImagesToMontage.class, UIUtils.getIconURLFromResources("actions/view-grid.png"));

        registerEnumParameterType("ij1-dimensions-zproject:method", ZProjectorAlgorithm.Method.class,
                "Method", "Available methods");
    }

    private void registerThresholdAlgorithms() {
        registerNodeType("ij1-threshold-manual2d-8u", ManualThreshold8U2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-percentile2d-8u", PercentileThreshold8U2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-manual2d-16u", ManualThreshold16U2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("ij1-threshold-auto2d", AutoThreshold2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("threshold-brightspots2d", BrightSpotsSegmentation2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("threshold-hessian2d", HessianSegmentation2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("threshold-hough2d", HoughSegmentation2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("threshold-hough2d-fast", FastHoughSegmentation2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));
        registerNodeType("threshold-internalgradient2d", InternalGradientSegmentation2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/segment.png"));

        registerEnumParameterType(AutoThresholder.Method.class.getCanonicalName(), AutoThresholder.Method.class,
                "Auto threshold method", "Available methods");
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
        registerNodeType("ij1-noise-despeckle2d", DespeckleFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/image-auto-adjust.png"));
        registerNodeType("ij1-noise-removeoutliers2d", RemoveOutliersFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-eraser.png"));

        registerEnumParameterType("ij1-noise-removeoutliers2d:mode", RemoveOutliersFilter2DAlgorithm.Mode.class,
                "Mode", "Available modes");
    }

    private void registerBinaryAlgorithms() {
        registerNodeType("ij1-binary-dtwatershed2d", DistanceTransformWatershed2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-binary-voronoi2d", Voronoi2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-binary-uep2d", UltimateErodedPoints2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
    }

    private void registerMorphologyAlgorithms() {
//        registerNodeType("ij1-morph-binary-operation2d", MorphologyBinary2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/morphology.png"));
        registerNodeType("ij1-morph-operation2d", Morphology2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/morphology.png"));
        registerNodeType("ij1-morph-binary-fillholes2d", MorphologyFillHoles2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-fill.png"));
        registerNodeType("ij1-morph-binary-outline2d", MorphologyOutline2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/draw-connector.png"));
        registerNodeType("ij1-morph-binary-skeletonize2d", MorphologySkeletonize2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-to-path.png"));
//        registerNodeType("ij1-morph-greyscale-internalgradient2d", MorphologyInternalGradient2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerEnumParameterType("ij1-morph-binary-operation2d:operation", MorphologyBinary2DAlgorithm.Operation.class,
                "Operation", "Available operations");
        registerEnumParameterType("ij1-morph:operation", Morphology.Operation.class,
                "Operation", "Available operations");
        registerEnumParameterType("ij1-morph:strel", Strel.Shape.class,
                "Structure element", "Available operations");
    }

    private void registerMathAlgorithms() {
        registerNodeType("ij1-math-math2d", ApplyMath2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-transform2d", ApplyTransform2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-math2d-expression", ApplyMathExpression2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-edt2d", ApplyDistanceTransform2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-variance2d", LocalVarianceFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-maximum2d", LocalMaximumFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-minimum2d", LocalMinimumFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-variance3d", LocalVarianceFilter3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-maximum3d", LocalMaximumFilter3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-local-minimum3d", LocalMinimumFilter3DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-replace-nan-by-median2d", RemoveNaNFilter2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-imagecalculator2d", ImageCalculator2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/calculator.png"));
        registerNodeType("ij1-math-hessian2d", Hessian2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("ij1-math-divide-by-maximum", DivideByMaximumAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerNodeType("ij1-math-generate-from-expression", GenerateFromMathExpression2D.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

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

        registerEnumParameterType("ij1-feature-vesselness-frangi:slicing-mode", FrangiVesselnessFeatures.SlicingMode.class,
                "Slicing mode", "Available slicing modes");
        registerEnumParameterType("ij1-feature-maxima-local-2d:output-type", LocalMaxima2DAlgorithm.OutputType.class,
                "Output type", "Available output types");
    }

    private void registerContrastAlgorithms() {
        registerNodeType("ij1-contrast-clahe", CLAHEContrastEnhancer.class, UIUtils.getIconURLFromResources("actions/contrast.png"));
        registerNodeType("ij1-contrast-illumination-correction2d", IlluminationCorrection2DAlgorithm.class, UIUtils.getIconURLFromResources("actions/contrast.png"));
        registerNodeType("ij1-contrast-calibrate", DisplayRangeCalibrationAlgorithm.class, UIUtils.getIconURLFromResources("actions/contrast.png"));
    }

    private void registerEdgeAlgorithms() {
        registerNodeType("ij1-edge-sobel", SobelEdgeDetectorAlgorithm.class);
    }

    private void registerColorAlgorithms() {
        registerNodeType("ij1-color-invert", InvertColorsAlgorithm.class, UIUtils.getIconURLFromResources("actions/invertimage.png"));
        registerNodeType("ij1-color-merge-channels", MergeChannelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("ij1-color-arrange-channels", ArrangeChannelsAlgorithm.class);
        registerNodeType("ij1-color-split-channels", SplitChannelsAlgorithm.class, UIUtils.getIconURLFromResources("actions/split.png"));

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
        return "2020.11";
    }

    private static Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> get3DConversion() {
        Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> result = new HashMap<>();

        result.put(ImagePlusData.class, ImagePlus3DData.class);
        result.put(ImagePlusGreyscaleData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlusGreyscale8UData.class, ImagePlus3DGreyscale8UData.class);
        result.put(ImagePlusGreyscaleMaskData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlusGreyscale16UData.class, ImagePlus3DGreyscale16UData.class);
        result.put(ImagePlusGreyscale32FData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlusColorData.class, ImagePlus3DColorData.class);
        result.put(ImagePlusColor8UData.class, ImagePlus3DColor8UData.class);
        result.put(ImagePlusColorRGBData.class, ImagePlus3DColorRGBData.class);

        result.put(ImagePlus2DData.class, ImagePlus3DData.class);
        result.put(ImagePlus2DGreyscaleData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus2DGreyscale8UData.class, ImagePlus3DGreyscale8UData.class);
        result.put(ImagePlus2DGreyscaleMaskData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus2DGreyscale16UData.class, ImagePlus3DGreyscale16UData.class);
        result.put(ImagePlus2DGreyscale32FData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus2DColorData.class, ImagePlus3DColorData.class);
        result.put(ImagePlus2DColor8UData.class, ImagePlus3DColor8UData.class);
        result.put(ImagePlus2DColorRGBData.class, ImagePlus3DColorRGBData.class);

        result.put(ImagePlus4DData.class, ImagePlus3DData.class);
        result.put(ImagePlus4DGreyscaleData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus4DGreyscale8UData.class, ImagePlus3DGreyscale8UData.class);
        result.put(ImagePlus4DGreyscaleMaskData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscale16UData.class, ImagePlus3DGreyscale16UData.class);
        result.put(ImagePlus4DGreyscale32FData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus4DColorData.class, ImagePlus3DColorData.class);
        result.put(ImagePlus4DColor8UData.class, ImagePlus3DColor8UData.class);
        result.put(ImagePlus4DColorRGBData.class, ImagePlus3DColorRGBData.class);

        result.put(ImagePlus5DData.class, ImagePlus3DData.class);
        result.put(ImagePlus5DGreyscaleData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus5DGreyscale8UData.class, ImagePlus3DGreyscale8UData.class);
        result.put(ImagePlus5DGreyscaleMaskData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscale16UData.class, ImagePlus3DGreyscale16UData.class);
        result.put(ImagePlus5DGreyscale32FData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus5DColorData.class, ImagePlus3DColorData.class);
        result.put(ImagePlus5DColor8UData.class, ImagePlus3DColor8UData.class);
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
        result.put(ImagePlusColorData.class, ImagePlus2DColorData.class);
        result.put(ImagePlusColor8UData.class, ImagePlus2DColor8UData.class);
        result.put(ImagePlusColorRGBData.class, ImagePlus2DColorRGBData.class);

        result.put(ImagePlus3DData.class, ImagePlus2DData.class);
        result.put(ImagePlus3DGreyscaleData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlus3DGreyscale8UData.class, ImagePlus2DGreyscale8UData.class);
        result.put(ImagePlus3DGreyscaleMaskData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus3DGreyscale16UData.class, ImagePlus2DGreyscale16UData.class);
        result.put(ImagePlus3DGreyscale32FData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus3DColorData.class, ImagePlus2DColorData.class);
        result.put(ImagePlus3DColor8UData.class, ImagePlus2DColor8UData.class);
        result.put(ImagePlus3DColorRGBData.class, ImagePlus2DColorRGBData.class);

        result.put(ImagePlus4DData.class, ImagePlus2DData.class);
        result.put(ImagePlus4DGreyscaleData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlus4DGreyscale8UData.class, ImagePlus2DGreyscale8UData.class);
        result.put(ImagePlus4DGreyscaleMaskData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscale16UData.class, ImagePlus2DGreyscale16UData.class);
        result.put(ImagePlus4DGreyscale32FData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus4DColorData.class, ImagePlus2DColorData.class);
        result.put(ImagePlus4DColor8UData.class, ImagePlus2DColor8UData.class);
        result.put(ImagePlus4DColorRGBData.class, ImagePlus2DColorRGBData.class);

        result.put(ImagePlus5DData.class, ImagePlus2DData.class);
        result.put(ImagePlus5DGreyscaleData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlus5DGreyscale8UData.class, ImagePlus2DGreyscale8UData.class);
        result.put(ImagePlus5DGreyscaleMaskData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscale16UData.class, ImagePlus2DGreyscale16UData.class);
        result.put(ImagePlus5DGreyscale32FData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus5DColorData.class, ImagePlus2DColorData.class);
        result.put(ImagePlus5DColor8UData.class, ImagePlus2DColor8UData.class);
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
        result.put(ImagePlus3DColorData.class, ImagePlus2DColorData.class);
        result.put(ImagePlus3DColor8UData.class, ImagePlus2DColor8UData.class);
        result.put(ImagePlus3DColorRGBData.class, ImagePlus2DColorRGBData.class);

        result.put(ImagePlus4DData.class, ImagePlus3DData.class);
        result.put(ImagePlus4DGreyscaleData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus4DGreyscale8UData.class, ImagePlus3DGreyscale8UData.class);
        result.put(ImagePlus4DGreyscaleMaskData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscale16UData.class, ImagePlus3DGreyscale16UData.class);
        result.put(ImagePlus4DGreyscale32FData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus4DColorData.class, ImagePlus3DColorData.class);
        result.put(ImagePlus4DColor8UData.class, ImagePlus3DColor8UData.class);
        result.put(ImagePlus4DColorRGBData.class, ImagePlus3DColorRGBData.class);

        result.put(ImagePlus5DData.class, ImagePlus4DData.class);
        result.put(ImagePlus5DGreyscaleData.class, ImagePlus4DGreyscaleData.class);
        result.put(ImagePlus5DGreyscale8UData.class, ImagePlus4DGreyscale8UData.class);
        result.put(ImagePlus5DGreyscaleMaskData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscale16UData.class, ImagePlus4DGreyscale16UData.class);
        result.put(ImagePlus5DGreyscale32FData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus5DColorData.class, ImagePlus4DColorData.class);
        result.put(ImagePlus5DColor8UData.class, ImagePlus4DColor8UData.class);
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
        result.put(ImagePlusColorData.class, ImagePlusGreyscaleMaskData.class);
        result.put(ImagePlusColor8UData.class, ImagePlusGreyscaleMaskData.class);
        result.put(ImagePlusColorRGBData.class, ImagePlusGreyscaleMaskData.class);

        result.put(ImagePlus2DData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DGreyscaleData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DGreyscale8UData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DGreyscale16UData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DGreyscale32FData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DColorData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DColor8UData.class, ImagePlus2DGreyscaleMaskData.class);
        result.put(ImagePlus2DColorRGBData.class, ImagePlus2DGreyscaleMaskData.class);

        result.put(ImagePlus3DData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DGreyscaleData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DGreyscale8UData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DGreyscale16UData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DGreyscale32FData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DColorData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DColor8UData.class, ImagePlus3DGreyscaleMaskData.class);
        result.put(ImagePlus3DColorRGBData.class, ImagePlus3DGreyscaleMaskData.class);

        result.put(ImagePlus4DData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscaleData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscale8UData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscale16UData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DGreyscale32FData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DColorData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DColor8UData.class, ImagePlus4DGreyscaleMaskData.class);
        result.put(ImagePlus4DColorRGBData.class, ImagePlus4DGreyscaleMaskData.class);

        result.put(ImagePlus5DData.class, ImagePlus5DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscaleData.class, ImagePlus5DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscale8UData.class, ImagePlus5DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscale16UData.class, ImagePlus5DGreyscaleMaskData.class);
        result.put(ImagePlus5DGreyscale32FData.class, ImagePlus5DGreyscaleMaskData.class);
        result.put(ImagePlus5DColorData.class, ImagePlus5DGreyscaleMaskData.class);
        result.put(ImagePlus5DColor8UData.class, ImagePlus5DGreyscaleMaskData.class);
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
        result.put(ImagePlusColorData.class, ImagePlusGreyscale32FData.class);
        result.put(ImagePlusColor8UData.class, ImagePlusGreyscale32FData.class);
        result.put(ImagePlusColorRGBData.class, ImagePlusGreyscale32FData.class);

        result.put(ImagePlus2DData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DGreyscaleData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DGreyscale8UData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DGreyscaleMaskData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DGreyscale16UData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DColorData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DColor8UData.class, ImagePlus2DGreyscale32FData.class);
        result.put(ImagePlus2DColorRGBData.class, ImagePlus2DGreyscale32FData.class);

        result.put(ImagePlus3DData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DGreyscaleData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DGreyscale8UData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DGreyscaleMaskData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DGreyscale16UData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DColorData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DColor8UData.class, ImagePlus3DGreyscale32FData.class);
        result.put(ImagePlus3DColorRGBData.class, ImagePlus3DGreyscale32FData.class);

        result.put(ImagePlus4DData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DGreyscaleData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DGreyscale8UData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DGreyscaleMaskData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DGreyscale16UData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DColorData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DColor8UData.class, ImagePlus4DGreyscale32FData.class);
        result.put(ImagePlus4DColorRGBData.class, ImagePlus4DGreyscale32FData.class);

        result.put(ImagePlus5DData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DGreyscaleData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DGreyscale8UData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DGreyscaleMaskData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DGreyscale16UData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DColorData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DColor8UData.class, ImagePlus5DGreyscale32FData.class);
        result.put(ImagePlus5DColorRGBData.class, ImagePlus5DGreyscale32FData.class);

        return result;
    }

    private static Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> getToGrayscaleConversion() {
        Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> result = new HashMap<>();

        result.put(ImagePlusData.class, ImagePlusGreyscaleData.class);
        result.put(ImagePlusColorData.class, ImagePlusGreyscaleData.class);
        result.put(ImagePlusColor8UData.class, ImagePlusGreyscaleData.class);
        result.put(ImagePlusColorRGBData.class, ImagePlusGreyscaleData.class);

        result.put(ImagePlus2DData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlus2DColorData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlus2DColor8UData.class, ImagePlus2DGreyscaleData.class);
        result.put(ImagePlus2DColorRGBData.class, ImagePlus2DGreyscaleData.class);

        result.put(ImagePlus3DData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus3DColorData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus3DColor8UData.class, ImagePlus3DGreyscaleData.class);
        result.put(ImagePlus3DColorRGBData.class, ImagePlus3DGreyscaleData.class);

        result.put(ImagePlus4DData.class, ImagePlus4DGreyscaleData.class);
        result.put(ImagePlus4DColorData.class, ImagePlus4DGreyscaleData.class);
        result.put(ImagePlus4DColor8UData.class, ImagePlus4DGreyscaleData.class);
        result.put(ImagePlus4DColorRGBData.class, ImagePlus4DGreyscaleData.class);

        result.put(ImagePlus5DData.class, ImagePlus5DGreyscaleData.class);
        result.put(ImagePlus5DColorData.class, ImagePlus5DGreyscaleData.class);
        result.put(ImagePlus5DColor8UData.class, ImagePlus5DGreyscaleData.class);
        result.put(ImagePlus5DColorRGBData.class, ImagePlus5DGreyscaleData.class);

        return result;
    }

    private static Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> getToColorConversion() {
        Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> result = new HashMap<>();

        result.put(ImagePlusData.class, ImagePlusColorData.class);
        result.put(ImagePlusGreyscaleData.class, ImagePlusColorData.class);
        result.put(ImagePlusGreyscale8UData.class, ImagePlusColorData.class);
        result.put(ImagePlusGreyscale16UData.class, ImagePlusColorData.class);
        result.put(ImagePlusGreyscale32FData.class, ImagePlusColorData.class);
        result.put(ImagePlusGreyscaleMaskData.class, ImagePlusColorData.class);

        result.put(ImagePlus2DData.class, ImagePlus2DColorData.class);
        result.put(ImagePlus2DGreyscaleData.class, ImagePlus2DColorData.class);
        result.put(ImagePlus2DGreyscale8UData.class, ImagePlus2DColorData.class);
        result.put(ImagePlus2DGreyscale16UData.class, ImagePlus2DColorData.class);
        result.put(ImagePlus2DGreyscale32FData.class, ImagePlus2DColorData.class);
        result.put(ImagePlus2DGreyscaleMaskData.class, ImagePlus2DColorData.class);

        result.put(ImagePlus3DData.class, ImagePlus3DColorData.class);
        result.put(ImagePlus3DGreyscaleData.class, ImagePlus3DColorData.class);
        result.put(ImagePlus3DGreyscale8UData.class, ImagePlus3DColorData.class);
        result.put(ImagePlus3DGreyscale16UData.class, ImagePlus3DColorData.class);
        result.put(ImagePlus3DGreyscale32FData.class, ImagePlus3DColorData.class);
        result.put(ImagePlus3DGreyscaleMaskData.class, ImagePlus3DColorData.class);

        result.put(ImagePlus4DData.class, ImagePlus4DColorData.class);
        result.put(ImagePlus4DGreyscaleData.class, ImagePlus4DColorData.class);
        result.put(ImagePlus4DGreyscale8UData.class, ImagePlus4DColorData.class);
        result.put(ImagePlus4DGreyscale16UData.class, ImagePlus4DColorData.class);
        result.put(ImagePlus4DGreyscale32FData.class, ImagePlus4DColorData.class);
        result.put(ImagePlus4DGreyscaleMaskData.class, ImagePlus4DColorData.class);

        result.put(ImagePlus5DData.class, ImagePlus5DColorData.class);
        result.put(ImagePlus5DGreyscaleData.class, ImagePlus5DColorData.class);
        result.put(ImagePlus5DGreyscale8UData.class, ImagePlus5DColorData.class);
        result.put(ImagePlus5DGreyscale16UData.class, ImagePlus5DColorData.class);
        result.put(ImagePlus5DGreyscale32FData.class, ImagePlus5DColorData.class);
        result.put(ImagePlus5DGreyscaleMaskData.class, ImagePlus5DColorData.class);

        return result;
    }
}
