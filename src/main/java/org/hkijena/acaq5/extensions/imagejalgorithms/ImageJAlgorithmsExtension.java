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

package org.hkijena.acaq5.extensions.imagejalgorithms;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ij.Menus;
import ij.process.AutoThresholder;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.EigenvalueSelection2D;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.InterpolationMethod;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.LogicalOperation;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.MacroWrapperAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.analyze.FindParticles2D;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.background.RollingBallBackgroundEstimator2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.binary.DistanceTransformWatershed2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.binary.UltimateErodedPoints2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.binary.Voronoi2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur.BoxFilter2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur.BoxFilter3DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur.GaussianBlur2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur.GaussianBlur3DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur.MedianBlurFilter2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur.MedianBlurFilter3DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur.MedianBlurGreyscale8U2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur.MedianBlurRGB2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.color.ArrangeChannelsAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.color.InvertColorsAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.color.MergeChannelsAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.color.SplitChannelsAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.contrast.CLAHEContrastEnhancer;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.contrast.IlluminationCorrection2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.convolve.Convolve2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions.StackInverterAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions.StackMergerAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions.StackSplitterAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions.StackTo2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions.ZProjectorAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.edge.SobelEdgeDetectorAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.features.FrangiVesselnessFeatures;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.features.LocalMaxima2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.features.MeijeringVesselness2DFeatures;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.fft.FFT2DForwardTransform;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.fft.FFT2DInverseTransform;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.fft.FFT2DSwapQuadrants;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.math.*;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.Measurement;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.MeasurementColumn;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.MeasurementColumnSortOrder;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.MeasurementFilter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.SortOrder;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.morphology.MorphologyBinary2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.morphology.MorphologyFillHoles2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.morphology.MorphologyGreyscale2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.morphology.MorphologyInternalGradient2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.morphology.MorphologyOutline2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.morphology.MorphologySkeletonize2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.noise.AddNoise2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.noise.DespeckleFilter2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.noise.RemoveOutliersFilter2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi.*;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.sharpen.LaplacianSharpen2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.statistics.GreyscalePixelsGenerator;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.statistics.HistogramGenerator;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold.AutoThreshold2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold.BrightSpotsSegmentation2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold.HessianSegmentation2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold.HoughSegmentation2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold.InternalGradientSegmentation2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold.ManualThreshold16U2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold.ManualThreshold8U2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.transform.TransformCrop2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.transform.TransformFlip2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.transform.TransformRotate2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.transform.TransformScale2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.transform.TransformScale3DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.parameters.MacroCode;
import org.hkijena.acaq5.extensions.imagejalgorithms.parameters.MacroParameterEditorUI;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale16UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale16UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale16UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale32FData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscaleMaskData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.ImagePlus5DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscale16UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscale32FData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscale8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscaleMaskData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptService;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extension that adds ImageJ2 algorithms
 */
@Plugin(type = ACAQJavaExtension.class)
public class ImageJAlgorithmsExtension extends ACAQPrepackagedDefaultJavaExtension {

    /**
     * Conversion rules from mask data types to their respective 8-bit types
     */
    public static final Map<Class<? extends ACAQData>, Class<? extends ACAQData>> REMOVE_MASK_QUALIFIER =
            ImmutableMap.of(
                    ImagePlusGreyscaleMaskData.class, ImagePlusGreyscale8UData.class,
                    ImagePlus2DGreyscaleMaskData.class, ImagePlus2DGreyscale8UData.class,
                    ImagePlus3DGreyscaleMaskData.class, ImagePlus3DGreyscale8UData.class,
                    ImagePlus4DGreyscaleMaskData.class, ImagePlus4DGreyscale8UData.class,
                    ImagePlus5DGreyscaleMaskData.class, ImagePlus5DGreyscale8UData.class);

    /**
     * Conversion rules that convert any input data type into their respective mask data type
     */
    public static final Map<Class<? extends ACAQData>, Class<? extends ACAQData>> ADD_MASK_QUALIFIER = getMaskQualifierMap();

    /**
     * Conversion rules that convert color types into greyscale
     */
    public static final Map<Class<? extends ACAQData>, Class<? extends ACAQData>> TO_GRAYSCALE_CONVERSION = getToGrayscaleConversion();

    /**
     * Conversion rules that convert color types into colored images
     */
    public static final Map<Class<? extends ACAQData>, Class<? extends ACAQData>> TO_COLOR_CONVERSION = getToColorConversion();

    /**
     * Conversion rules that convert color types into colored images
     */
    public static final Map<Class<? extends ACAQData>, Class<? extends ACAQData>> TO_GRAYSCALE32F_CONVERSION = getToGrayscale32FConversion();

    /**
     * Conversion rules convert higher-dimensional data to a lower-dimensional counterpart.
     * 2D data remains 2D data.
     */
    public static final Map<Class<? extends ACAQData>, Class<? extends ACAQData>> DECREASE_DIMENSION_CONVERSION = getDecreaseDimensionConversion();

    /**
     * Conversion rules convert higher-dimensional data to their 2D counterparts
     */
    public static final Map<Class<? extends ACAQData>, Class<? extends ACAQData>> TO_2D_CONVERSION = get2DConversion();

    /**
     * Conversion rules convert data to their 3D counterparts
     */
    public static final Map<Class<? extends ACAQData>, Class<? extends ACAQData>> TO_3D_CONVERSION = get3DConversion();

    @Parameter
    private CommandService commandService;

    @Override
    public String getName() {
        return "ImageJ algorithms";
    }

    @Override
    public String getDescription() {
        return "Integrates ImageJ algorithms into ACAQ5";
    }

    @Override
    public void register() {
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

        registerAlgorithm("external-imagej-macro", MacroWrapperAlgorithm.class, UIUtils.getAlgorithmIconURL("imagej.png"));

        // Register enum parameters
        registerGlobalEnums();

        // Register other parameters
        registerGlobalParameters();

//        registerIJ2Algorithms();
    }

    private void registerGlobalParameters() {
        registerParameterType("ij1:measurement-filter",
                MeasurementFilter.class,
                MeasurementFilter::new,
                o -> new MeasurementFilter((MeasurementFilter) o),
                "Measurement filter",
                "Models filtering of measurements",
                null);
        registerParameterType("ij1:measurement-filter-list",
                MeasurementFilter.List.class,
                MeasurementFilter.List::new,
                o -> new MeasurementFilter.List((MeasurementFilter.List) o),
                "Measurement filter list",
                "Models filtering of measurements",
                null);
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
        registerParameterType("ij-macro-code",
                MacroCode.class,
                MacroCode::new,
                m -> new MacroCode((MacroCode) m),
                "ImageJ macro",
                "An ImageJ macro code",
                MacroParameterEditorUI.class);
    }

    private void registerGlobalEnums() {
        registerEnumParameterType("ij1-interpolation-method", InterpolationMethod.class,
                "Interpolation method", "Available interpolation methods");
        registerEnumParameterType("ij1-measurement", Measurement.class,
                "Measurement", "Available measurements");
        registerEnumParameterType("ij1-measurement-column", MeasurementColumn.class,
                "Measurement column", "Available measurement columns");
        registerEnumParameterType("ij1-sort-order", SortOrder.class,
                "Sort order", "Available sort orders");
        registerEnumParameterType("ij1:logical-operation", LogicalOperation.class,
                "Logical operation", "Available logical operations");
    }

    private void registerROIAlgorithms() {
        registerAlgorithm("ij1-roi-from-rectangles", DefineRectangularRoiAlgorithm.class, UIUtils.getAlgorithmIconURL("draw-rectangle.png"));
        registerAlgorithm("ij1-roi-from-rectangles-referenced", ReferencedDefineRectangularRoiAlgorithm.class, UIUtils.getAlgorithmIconURL("draw-rectangle.png"));
        registerAlgorithm("ij1-roi-append-rectangles", AppendRectangularRoiAlgorithm.class, UIUtils.getAlgorithmIconURL("draw-rectangle.png"));
        registerAlgorithm("ij1-roi-append-rectangles-referenced", ReferencedAppendRectangularRoiAlgorithm.class, UIUtils.getAlgorithmIconURL("draw-rectangle.png"));
        registerAlgorithm("ij1-roi-split", SplitRoiAlgorithm.class, UIUtils.getAlgorithmIconURL("split.png"));
        registerAlgorithm("ij1-roi-explode", ExplodeRoiAlgorithm.class, UIUtils.getAlgorithmIconURL("split.png"));
        registerAlgorithm("ij1-roi-merge", MergeRoiListsAlgorithm.class, UIUtils.getAlgorithmIconURL("merge.png"));
        registerAlgorithm("ij1-roi-calculator", RoiCalculatorAlgorithm.class, UIUtils.getAlgorithmIconURL("calculator.png"));
        registerAlgorithm("ij1-roi-to-mask-unreferenced", UnreferencedRoiToMaskAlgorithm.class, UIUtils.getAlgorithmIconURL("segment.png"));
        registerAlgorithm("ij1-roi-to-mask", RoiToMaskAlgorithm.class, UIUtils.getAlgorithmIconURL("segment.png"));
        registerAlgorithm("ij1-roi-outline", OutlineRoiAlgorithm.class, UIUtils.getAlgorithmIconURL("shapes.png"));
        registerAlgorithm("ij1-roi-remove-bordering", RemoveBorderRoisAlgorithm.class, UIUtils.getAlgorithmIconURL("bordertool.png"));
        registerAlgorithm("ij1-roi-statistics", RoiStatisticsAlgorithm.class, UIUtils.getAlgorithmIconURL("statistics.png"));
        registerAlgorithm("ij1-roi-filter-statistics", FilterRoiByStatisticsAlgorithm.class, UIUtils.getAlgorithmIconURL("filter.png"));
        registerAlgorithm("ij1-roi-color-statistics", ColorRoiByStatisticsAlgorithm.class, UIUtils.getAlgorithmIconURL("fill-color.png"));
        registerAlgorithm("ij1-roi-sort-and-extract-statistics", SortAndExtractRoiByStatisticsAlgorithm.class, UIUtils.getAlgorithmIconURL("sort-amount-up-alt.png"));
        registerAlgorithm("ij1-roi-set-properties", ChangeRoiPropertiesAlgorithm.class, UIUtils.getAlgorithmIconURL("edit.png"));
        registerAlgorithm("ij1-roi-to-rgb-unreferenced", UnreferencedRoiToRGBAlgorithm.class, UIUtils.getAlgorithmIconURL("color-management.png"));
        registerAlgorithm("ij1-roi-to-rgb", RoiToRGBAlgorithm.class, UIUtils.getAlgorithmIconURL("color-management.png"));
    }

    private void registerConvolutionAlgorithms() {
        registerAlgorithm("ij1-convolve-convolve2d", Convolve2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
    }

    private void registerTransformationAlgorithms() {
        registerAlgorithm("ij1-transform-flip2d", TransformFlip2DAlgorithm.class, UIUtils.getAlgorithmIconURL("object-flip-horizontal.png"));
        registerAlgorithm("ij1-transform-rotate2d", TransformRotate2DAlgorithm.class, UIUtils.getAlgorithmIconURL("transform-rotate.png"));
        registerAlgorithm("ij1-transform-scale2d", TransformScale2DAlgorithm.class, UIUtils.getAlgorithmIconURL("transform-scale.png"));
        registerAlgorithm("ij1-transform-scale3d", TransformScale3DAlgorithm.class, UIUtils.getAlgorithmIconURL("transform-scale.png"));
        registerAlgorithm("ij1-transform-crop2d", TransformCrop2DAlgorithm.class, UIUtils.getAlgorithmIconURL("transform-crop.png"));

        registerEnumParameterType("ij1-transform-flip2d:flip-mode", TransformFlip2DAlgorithm.FlipMode.class,
                "Flip mode", "Available modes");
        registerEnumParameterType("ij1-transform-rotate2d:rotation-mode", TransformRotate2DAlgorithm.RotationMode.class,
                "Rotation mode", "Available modes");
    }

    private void registerFFTAlgorithms() {
        registerAlgorithm("ij1-fft-forward2d", FFT2DForwardTransform.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-fft-inverse2d", FFT2DInverseTransform.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-fft-swap2d", FFT2DSwapQuadrants.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
    }

    private void registerAnalysisAlgorithms() {
        registerAlgorithm("ij1-analyze-find-particles2d", FindParticles2D.class, UIUtils.getAlgorithmIconURL("tool_elliptical_selection.png"));
        registerAlgorithm("ij1-analyze-statistics-histogram", HistogramGenerator.class, UIUtils.getAlgorithmIconURL("chart-bar.png"));
        registerAlgorithm("ij1-analyze-statistics-pixels-greyscale", GreyscalePixelsGenerator.class, UIUtils.getAlgorithmIconURL("statistics.png"));

        registerEnumParameterType("ij1-analyze-statistics-histogram:multi-channel-mode", HistogramGenerator.MultiChannelMode.class,
                "Multichannel mode", "Available modes");
    }

    private void registerDimensionAlgorithms() {
        registerAlgorithm("ij1-dimensions-stack-to-2d", StackTo2DAlgorithm.class, UIUtils.getAlgorithmIconURL("layer-bottom.png"));
        registerAlgorithm("ij1-dimensions-stacksplitter", StackSplitterAlgorithm.class, UIUtils.getAlgorithmIconURL("split.png"));
        registerAlgorithm("ij1-dimensions-stackmerger", StackMergerAlgorithm.class, UIUtils.getAlgorithmIconURL("cube.png"));
        registerAlgorithm("ij1-dimensions-stackinverter", StackInverterAlgorithm.class, UIUtils.getAlgorithmIconURL("layer-previous.png"));
        registerAlgorithm("ij1-dimensions-zproject", ZProjectorAlgorithm.class, UIUtils.getAlgorithmIconURL("layer-bottom.png"));

        registerEnumParameterType("ij1-dimensions-zproject:method", ZProjectorAlgorithm.Method.class,
                "Method", "Available methods");
    }

    private void registerThresholdAlgorithms() {
        registerAlgorithm("ij1-threshold-manual2d-8u", ManualThreshold8U2DAlgorithm.class, UIUtils.getAlgorithmIconURL("segment.png"));
        registerAlgorithm("ij1-threshold-manual2d-16u", ManualThreshold16U2DAlgorithm.class, UIUtils.getAlgorithmIconURL("segment.png"));
        registerAlgorithm("ij1-threshold-auto2d", AutoThreshold2DAlgorithm.class, UIUtils.getAlgorithmIconURL("segment.png"));
        registerAlgorithm("threshold-brightspots2d", BrightSpotsSegmentation2DAlgorithm.class, UIUtils.getAlgorithmIconURL("segment.png"));
        registerAlgorithm("threshold-hessian2d", HessianSegmentation2DAlgorithm.class, UIUtils.getAlgorithmIconURL("segment.png"));
        registerAlgorithm("threshold-hough2d", HoughSegmentation2DAlgorithm.class, UIUtils.getAlgorithmIconURL("segment.png"));
        registerAlgorithm("threshold-internalgradient2d", InternalGradientSegmentation2DAlgorithm.class, UIUtils.getAlgorithmIconURL("segment.png"));

        registerEnumParameterType(AutoThresholder.Method.class.getCanonicalName(), AutoThresholder.Method.class,
                "Auto threshold method", "Available methods");
        registerEnumParameterType("ij1:eigenvalue-selection-2d", EigenvalueSelection2D.class,
                "Eigenvalue selection (2D)", "Determines whether to choose the smallest or largest Eigenvalue");
        registerEnumParameterType("ij1:roi-outline", RoiOutline.class,
                "ROI outline", "Available ways to outline a ROI");
    }

    private void registerSharpenAlgorithms() {
        registerAlgorithm("ij1-sharpen-laplacian2d", LaplacianSharpen2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
    }

    private void registerBackgroundAlgorithms() {
        registerAlgorithm("ij1-background-rollingball2d", RollingBallBackgroundEstimator2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));

        registerEnumParameterType("ij1-background-rollingball2d:background-type", RollingBallBackgroundEstimator2DAlgorithm.BackgroundType.class,
                "Background type", "Available background types");
        registerEnumParameterType("ij1-background-rollingball2d:background-method", RollingBallBackgroundEstimator2DAlgorithm.Method.class,
                "Rolling ball method", "Available methods");
    }

    private void registerNoiseAlgorithms() {
        registerAlgorithm("ij1-noise-addnormalnoise2d", AddNoise2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-noise-despeckle2d", DespeckleFilter2DAlgorithm.class, UIUtils.getAlgorithmIconURL("tool_imageeffects.png"));
        registerAlgorithm("ij1-noise-removeoutliers2d", RemoveOutliersFilter2DAlgorithm.class, UIUtils.getAlgorithmIconURL("eraser.png"));

        registerEnumParameterType("ij1-noise-removeoutliers2d:mode", RemoveOutliersFilter2DAlgorithm.Mode.class,
                "Mode", "Available modes");
    }

    private void registerBinaryAlgorithms() {
        registerAlgorithm("ij1-binary-dtwatershed2d", DistanceTransformWatershed2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-binary-voronoi2d", Voronoi2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-binary-uep2d", UltimateErodedPoints2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
    }

    private void registerMorphologyAlgorithms() {
        registerAlgorithm("ij1-morph-binary-operation2d", MorphologyBinary2DAlgorithm.class, UIUtils.getAlgorithmIconURL("path-mask-edit.png"));
        registerAlgorithm("ij1-morph-greyscale-operation2d", MorphologyGreyscale2DAlgorithm.class, UIUtils.getAlgorithmIconURL("path-mask-edit.png"));
        registerAlgorithm("ij1-morph-binary-fillholes2d", MorphologyFillHoles2DAlgorithm.class, UIUtils.getAlgorithmIconURL("fill.png"));
        registerAlgorithm("ij1-morph-binary-outline2d", MorphologyOutline2DAlgorithm.class, UIUtils.getAlgorithmIconURL("shapes.png"));
        registerAlgorithm("ij1-morph-binary-skeletonize2d", MorphologySkeletonize2DAlgorithm.class, UIUtils.getAlgorithmIconURL("path-simplify.png"));
        registerAlgorithm("ij1-morph-greyscale-internalgradient2d", MorphologyInternalGradient2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));

        registerEnumParameterType("ij1-morph-binary-operation2d:operation", MorphologyBinary2DAlgorithm.Operation.class,
                "Operation", "Available operations");
        registerEnumParameterType("ij1-morph-greyscale-operation2d:operation", MorphologyGreyscale2DAlgorithm.Operation.class,
                "Operation", "Available operations");
    }

    private void registerMathAlgorithms() {
        registerAlgorithm("ij1-math-math2d", ApplyMath2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-math-transform2d", ApplyTransform2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-math-edt2d", ApplyDistanceTransform2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-math-local-variance2d", LocalVarianceFilter2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-math-local-maximum2d", LocalMaximumFilter2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-math-local-minimum2d", LocalMinimumFilter2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-math-local-variance3d", LocalVarianceFilter2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-math-local-maximum3d", LocalMaximumFilter3DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-math-local-minimum3d", LocalMinimumFilter3DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-math-replace-nan-by-median2d", RemoveNaNFilter2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-math-imagecalculator2d", ImageCalculator2DAlgorithm.class, UIUtils.getAlgorithmIconURL("calculator.png"));
        registerAlgorithm("ij1-math-hessian2d", Hessian2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));

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
        registerAlgorithm("ij1-feature-vesselness-frangi", FrangiVesselnessFeatures.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("feature-vesselness-meijering2d", MeijeringVesselness2DFeatures.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-feature-maxima-local-2d", LocalMaxima2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));

        registerEnumParameterType("ij1-feature-vesselness-frangi:slicing-mode", FrangiVesselnessFeatures.SlicingMode.class,
                "Slicing mode", "Available slicing modes");
        registerEnumParameterType("ij1-feature-maxima-local-2d:output-type", LocalMaxima2DAlgorithm.OutputType.class,
                "Output type", "Available output types");
    }

    private void registerContrastAlgorithms() {
        registerAlgorithm("ij1-contrast-clahe", CLAHEContrastEnhancer.class, UIUtils.getAlgorithmIconURL("adjust.png"));
        registerAlgorithm("ij1-contrast-illumination-correction2d", IlluminationCorrection2DAlgorithm.class, UIUtils.getAlgorithmIconURL("adjust.png"));
    }

    private void registerEdgeAlgorithms() {
        registerAlgorithm("ij1-edge-sobel", SobelEdgeDetectorAlgorithm.class);
    }

    private void registerColorAlgorithms() {
        registerAlgorithm("ij1-color-invert", InvertColorsAlgorithm.class, UIUtils.getAlgorithmIconURL("edit-select-invert.png"));
        registerAlgorithm("ij1-color-merge-channels", MergeChannelsAlgorithm.class, UIUtils.getAlgorithmIconURL("merge.png"));
        registerAlgorithm("ij1-color-arrange-channels", ArrangeChannelsAlgorithm.class);
        registerAlgorithm("ij1-color-split-channels", SplitChannelsAlgorithm.class, UIUtils.getAlgorithmIconURL("split.png"));

        registerEnumParameterType("ij1-color-merge-channels:channel-color", MergeChannelsAlgorithm.ChannelColor.class,
                "Channel color", "Available channel colors");
    }

    private void registerBlurAlgorithms() {
        registerAlgorithm("ij1-blur-gaussian2d", GaussianBlur2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-blur-gaussian3d", GaussianBlur3DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-blur-box2d", BoxFilter2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-blur-box3d", BoxFilter3DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-blur-median2d-8u", MedianBlurGreyscale8U2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-blur-median2d-rgb", MedianBlurRGB2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-blur-median2d", MedianBlurFilter2DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
        registerAlgorithm("ij1-blur-median3d", MedianBlurFilter3DAlgorithm.class, UIUtils.getAlgorithmIconURL("insert-math-expression.png"));
    }

    private void registerIJ2Algorithms() {
        for (Object entry : Menus.getCommands().entrySet()) {
            System.out.println("IJ1_Command: " + entry);
        }
        ScriptService scriptService = getContext().getService(ScriptService.class);
        for (ScriptInfo script : scriptService.getScripts()) {
            System.out.println("IJ2_Script: " + script + " || Params: " + ImmutableList.copyOf(script.inputs()).stream().map(Object::toString).collect(Collectors.joining("; ")));
        }
        for (CommandInfo command : commandService.getCommands()) {
            System.out.println("IJ2_Command: " + command);
//            if(ImageJ2AlgorithmWrapper.isCompatible(command, getContext())) {
//                try {
//                    ImageJ2AlgorithmWrapperDeclaration declaration = new ImageJ2AlgorithmWrapperDeclaration(command, getContext());
//                    registerAlgorithm(new ImageJ2AlgorithmWrapperRegistrationTask(this, declaration));
//                } catch (ModuleException e) {
//                    e.printStackTrace();
//                }
//            }
        }
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:imagej-algorithms";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    private static Map<Class<? extends ACAQData>, Class<? extends ACAQData>> get3DConversion() {
        Map<Class<? extends ACAQData>, Class<? extends ACAQData>> result = new HashMap<>();

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

    private static Map<Class<? extends ACAQData>, Class<? extends ACAQData>> get2DConversion() {
        Map<Class<? extends ACAQData>, Class<? extends ACAQData>> result = new HashMap<>();

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

    private static Map<Class<? extends ACAQData>, Class<? extends ACAQData>> getDecreaseDimensionConversion() {
        Map<Class<? extends ACAQData>, Class<? extends ACAQData>> result = new HashMap<>();

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

    private static Map<Class<? extends ACAQData>, Class<? extends ACAQData>> getMaskQualifierMap() {
        Map<Class<? extends ACAQData>, Class<? extends ACAQData>> result = new HashMap<>();

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

    private static Map<Class<? extends ACAQData>, Class<? extends ACAQData>> getToGrayscale32FConversion() {
        Map<Class<? extends ACAQData>, Class<? extends ACAQData>> result = new HashMap<>();

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

    private static Map<Class<? extends ACAQData>, Class<? extends ACAQData>> getToGrayscaleConversion() {
        Map<Class<? extends ACAQData>, Class<? extends ACAQData>> result = new HashMap<>();

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

    private static Map<Class<? extends ACAQData>, Class<? extends ACAQData>> getToColorConversion() {
        Map<Class<? extends ACAQData>, Class<? extends ACAQData>> result = new HashMap<>();

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
