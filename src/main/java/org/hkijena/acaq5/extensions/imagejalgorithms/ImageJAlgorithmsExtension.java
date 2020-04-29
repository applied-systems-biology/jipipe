package org.hkijena.acaq5.extensions.imagejalgorithms;

import com.google.common.collect.ImmutableMap;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.traits.ACAQJavaTraitDeclaration;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.binary.DistanceTransformWatershed2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.binary.UltimateErodedPoints2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.binary.Voronoi2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur.*;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.color.InvertColorsAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.color.MergeChannelsAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions.StackMergerAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions.StackSplitterAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions.ZProjectorAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.edge.SobelEdgeDetectorAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.math.*;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.morphology.*;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.noise.AddNoise2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.noise.DespeckleFilter2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.noise.RemoveOutliersFilter2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.sharpen.LaplacianSharpen2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold.AutoThreshold2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold.ManualThreshold16U2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold.ManualThreshold8U2DAlgorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.ImagePlus5DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.greyscale.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.*;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

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
        registerTrait(new ACAQJavaTraitDeclaration("image-index", ImageIndexDiscriminator.class),
                ResourceUtils.getPluginResource("icons/traits/search-location-blue.png"));

        registerAlgorithm("ij1-blur-gaussian2d", GaussianBlur2DAlgorithm.class);
        registerAlgorithm("ij1-blur-box2d", BoxFilter2DAlgorithm.class);
        registerAlgorithm("ij1-blur-median2d-8u", MedianBlurGreyscale8U2DAlgorithm.class);
        registerAlgorithm("ij1-blur-median2d-rgb", MedianBlurRGB2DAlgorithm.class);
        registerAlgorithm("ij1-blur-median2d", MedianBlurFilter2DAlgorithm.class);
        registerAlgorithm("ij1-color-invert", InvertColorsAlgorithm.class);
        registerAlgorithm("ij1-color-merge-channels", MergeChannelsAlgorithm.class);
        registerAlgorithm("ij1-edge-sobel", SobelEdgeDetectorAlgorithm.class);
        registerAlgorithm("ij1-math-math2d", ApplyMath2DAlgorithm.class);
        registerAlgorithm("ij1-math-transform2d", ApplyTransform2DAlgorithm.class);
        registerAlgorithm("ij1-math-edt2d", ApplyDistanceTransform2DAlgorithm.class);
        registerAlgorithm("ij1-math-local-variance2d", LocalVarianceFilter2DAlgorithm.class);
        registerAlgorithm("ij1-math-local-maximum2d", LocalMaximumFilter2DAlgorithm.class);
        registerAlgorithm("ij1-math-local-minimum2d", LocalMinimumFilter2DAlgorithm.class);
        registerAlgorithm("ij1-math-replace-nan-by-median2d", RemoveNaNFilter2DAlgorithm.class);
        registerAlgorithm("ij1-morph-binary-operation2d", MorphologyBinary2DAlgorithm.class);
        registerAlgorithm("ij1-morph-greyscale-operation2d", MorphologyGreyscale2DAlgorithm.class);
        registerAlgorithm("ij1-morph-binary-fillholes2d", MorphologyFillHoles2DAlgorithm.class);
        registerAlgorithm("ij1-morph-binary-outline2d", MorphologyOutline2DAlgorithm.class);
        registerAlgorithm("ij1-morph-binary-skeletonize2d", MorphologySkeletonize2DAlgorithm.class);
        registerAlgorithm("ij1-binary-dtwatershed2d", DistanceTransformWatershed2DAlgorithm.class);
        registerAlgorithm("ij1-binary-voronoi2d", Voronoi2DAlgorithm.class);
        registerAlgorithm("ij1-binary-uep2d", UltimateErodedPoints2DAlgorithm.class);
        registerAlgorithm("ij1-noise-addnormalnoise2d", AddNoise2DAlgorithm.class);
        registerAlgorithm("ij1-noise-despeckle2d", DespeckleFilter2DAlgorithm.class);
        registerAlgorithm("ij1-noise-removeoutliers2d", RemoveOutliersFilter2DAlgorithm.class);
        registerAlgorithm("ij1-sharpen-laplacian2d", LaplacianSharpen2DAlgorithm.class);
        registerAlgorithm("ij1-threshold-manual2d-8u", ManualThreshold8U2DAlgorithm.class);
        registerAlgorithm("ij1-threshold-manual2d-16u", ManualThreshold16U2DAlgorithm.class);
        registerAlgorithm("ij1-threshold-auto2d", AutoThreshold2DAlgorithm.class);
        registerAlgorithm("ij1-dimensions-stacksplitter", StackSplitterAlgorithm.class);
        registerAlgorithm("ij1-dimensions-stackmerger", StackMergerAlgorithm.class);
        registerAlgorithm("ij1-dimensions-zproject", ZProjectorAlgorithm.class);
//        for (CommandInfo command : commandService.getCommands()) {
//            if (ImageJ2AlgorithmWrapper.isCompatible(command, getContext())) {
//                try {
//                    ImageJ2AlgorithmWrapperDeclaration declaration = new ImageJ2AlgorithmWrapperDeclaration(command, getContext());
//                    registerAlgorithm(new ImageJ2AlgorithmWrapperRegistrationTask(this, declaration));
//                } catch (ModuleException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
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
