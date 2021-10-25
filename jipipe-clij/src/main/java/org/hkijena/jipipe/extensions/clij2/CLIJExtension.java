package org.hkijena.jipipe.extensions.clij2;

import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.clij2.algorithms.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageDataImageJAdapter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageToImagePlusDataConverter;
import org.hkijena.jipipe.extensions.clij2.datatypes.ImagePlusDataToCLIJImageDataConverter;
import org.hkijena.jipipe.extensions.clij2.parameters.OpenCLKernelScript;
import org.hkijena.jipipe.extensions.clij2.ui.CLIJControlPanelJIPipeMenuExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ImagePlusDataImporterUI;
import org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis.ImagePlusDataImportIntoImageJOperation;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * Integrates CLIJ
 */
@Plugin(type = JIPipeJavaExtension.class)
public class CLIJExtension extends JIPipePrepackagedDefaultJavaExtension {

    public static final Class[] ALLOWED_PARAMETER_TYPES = new Class[]{Boolean.class, Character.class, Short.class, Integer.class, Float.class, Double.class};

    @Override
    public StringList getDependencyCitations() {
        StringList result = new StringList();
        result.add("Robert Haase, Loic Alain Royer, Peter Steinbach, Deborah Schmidt, Alexandr Dibrov, Uwe Schmidt, Martin Weigert, " +
                "Nicola Maghelli, Pavel Tomancak, Florian Jug, Eugene W Myers. CLIJ: GPU-accelerated image processing for everyone. Nat Methods 17, 5-6 (2020)");
        return result;
    }

    @Override
    public String getName() {
        return "CLIJ2 integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates data types and algorithms for GPU computing based on CLIJ2.");
    }

    @Override
    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return Arrays.asList(
                new JIPipeImageJUpdateSiteDependency(new UpdateSite("clij", "https://sites.imagej.net/clij/", "", "", "", "", 0)),
                new JIPipeImageJUpdateSiteDependency(new UpdateSite("clij2", "https://sites.imagej.net/clij2/", "", "", "", "", 0))
        );
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/clij.png"));
    }

    @Override
    public void register() {
        registerParameterType("clij2:opencl-kernel",
                OpenCLKernelScript.class,
                null,
                null,
                "OpenCL Kernel",
                "A OpenCL kernel",
                null);
        registerDatatype("clij2-image",
                CLIJImageData.class,
                UIUtils.getIconURLFromResources("data-types/clij.png"),
                null,
                null,
                new ImagePlusDataImportIntoImageJOperation());
        for (Class<? extends JIPipeData> imageType : ImageJDataTypesExtension.IMAGE_TYPES) {
            registerDatatypeConversion(new CLIJImageToImagePlusDataConverter(imageType));
        }
        registerDatatypeConversion(new ImagePlusDataToCLIJImageDataConverter());
        registerImageJDataAdapter(new CLIJImageDataImageJAdapter(), ImagePlusDataImporterUI.class);
        registerAlgorithms();

        registerSettingsSheet(CLIJSettings.ID,
                "CLIJ2",
                UIUtils.getIconFromResources("apps/clij.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new CLIJSettings());
        registerMenuExtension(CLIJControlPanelJIPipeMenuExtension.class);
    }

    private void registerAlgorithms() {
        registerNodeType("clij2:execute-kernel-simple-iterating", Clij2ExecuteKernelSimpleIterating.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:execute-kernel-iterating", Clij2ExecuteKernelIterating.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:absolute-difference", Clij2AbsoluteDifference.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:generate-parametric-image-from-results-table-column", Clij2GenerateParametricImageFromResultsTableColumn.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:equal-constant", Clij2EqualConstant.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:label-to-mask", Clij2LabelToMask.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold", Clij2Threshold.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-huang", Clij2ThresholdHuang.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-z-projection-bounded", Clij2MinimumZProjectionBounded.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:not-equal", Clij2NotEqual.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:median-of-touching-neighbors", Clij2MedianOfTouchingNeighbors.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:flood-fill-diamond", Clij2FloodFillDiamond.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-2d-sphere", Clij2Mean2dSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-y-projection", Clij2MaximumYProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:power", Clij2Power.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:exclude-labels", Clij2ExcludeLabels.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:close-index-gaps-in-label-map", Clij2CloseIndexGapsInLabelMap.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:erode-sphere-slice-by-slice", Clij2ErodeSphereSliceBySlice.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-3d-sphere", Clij2Maximum3dSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-image-and-scalar", Clij2MinimumImageAndScalar.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-slice-by-slice-sphere", Clij2MinimumSliceBySliceSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-ij-iso-data", Clij2ThresholdIjIsoData.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:closing-diamond", Clij2ClosingDiamond.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-2d-sphere", Clij2Minimum2dSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:multiply-matrix", Clij2MultiplyMatrix.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:top-hat-box", Clij2TopHatBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:crop-2d", Clij2Crop2d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-li", Clij2ThresholdLi.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:transpose-xy", Clij2TransposeXy.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:replace-pixels-if-zero", Clij2ReplacePixelsIfZero.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:affine-transform-2d", Clij2AffineTransform2d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:gaussian-blur-3d", Clij2GaussianBlur3d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:pointlist-to-labelled-spots", Clij2PointlistToLabelledSpots.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:apply-vector-field-3d", Clij2ApplyVectorField3d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:gaussian-blur-2d", Clij2GaussianBlur2d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:replace-intensities", Clij2ReplaceIntensities.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:exclude-labels-with-values-within-range", Clij2ExcludeLabelsWithValuesWithinRange.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:add-images", Clij2AddImages.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:logarithm", Clij2Logarithm.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:average-distance-of-n-closest-points", Clij2AverageDistanceOfNClosestPoints.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:variance-of-masked-pixels", Clij2VarianceOfMaskedPixels.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-x-projection", Clij2MinimumXProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:exclude-labels-on-surface", Clij2ExcludeLabelsOnSurface.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:onlyzero-overwrite-maximum-diamond", Clij2OnlyzeroOverwriteMaximumDiamond.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:nonzero-maximum-box", Clij2NonzeroMaximumBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:multiply-stack-with-plane", Clij2MultiplyStackWithPlane.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:binary-fill-holes", Clij2BinaryFillHoles.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:downsample-slice-by-slice-half-median", Clij2DownsampleSliceBySliceHalfMedian.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:not-equal-constant", Clij2NotEqualConstant.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:undefined-to-zero", Clij2UndefinedToZero.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:closing-box", Clij2ClosingBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:smaller-or-equal-constant", Clij2SmallerOrEqualConstant.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:convolve", Clij2Convolve.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:subtract-images", Clij2SubtractImages.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:set-non-zero-pixels-to-pixel-index", Clij2SetNonZeroPixelsToPixelIndex.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:combine-horizontally", Clij2CombineHorizontally.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-closest-spot-distance", Clij2MeanClosestSpotDistance.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-2d-box", Clij2Mean2dBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-of-touching-neighbors", Clij2MaximumOfTouchingNeighbors.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:reslice-top", Clij2ResliceTop.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:gradient-y", Clij2GradientY.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:n-closest-distances", Clij2NClosestDistances.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:erode-box-slice-by-slice", Clij2ErodeBoxSliceBySlice.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:statistics-of-background-and-labelled-pixels", Clij2StatisticsOfBackgroundAndLabelledPixels.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:arg-maximum-z-projection", Clij2ArgMaximumZProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:centroids-of-labels", Clij2CentroidsOfLabels.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:statistics-of-labelled-pixels", Clij2StatisticsOfLabelledPixels.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:write-values-to-positions", Clij2WriteValuesToPositions.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:spots-to-point-list", Clij2LabelCentroidsToPointList.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:combine-vertically", Clij2CombineVertically.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:absolute", Clij2Absolute.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-otsu", Clij2ThresholdOtsu.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-slice-by-slice-sphere", Clij2MaximumSliceBySliceSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mask-stack-with-plane", Clij2MaskStackWithPlane.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:nonzero-minimum-box", Clij2NonzeroMinimumBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:median-slice-by-slice-sphere", Clij2MedianSliceBySliceSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:squared-difference", Clij2SquaredDifference.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-3d-box", Clij2Maximum3dBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:jaccard-index", Clij2JaccardIndex.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:sum-y-projection", Clij2SumYProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:touch-matrix-to-adjacency-matrix", Clij2TouchMatrixToAdjacencyMatrix.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:divide-images", Clij2DivideImages.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:greater-or-equal-constant", Clij2GreaterOrEqualConstant.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-shanbhag", Clij2ThresholdShanbhag.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:extend-labeling-via-voronoi", Clij2ExtendLabelingViaVoronoi.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-slice-by-slice-sphere", Clij2MeanSliceBySliceSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:transpose-yz", Clij2TransposeYz.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:image-to-stack", Clij2ImageToStack.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:subtract-image-from-scalar", Clij2SubtractImageFromScalar.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:binary-subtract", Clij2BinarySubtract.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-of-masked-pixels", Clij2MeanOfMaskedPixels.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mask", Clij2Mask.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-z-projection", Clij2MaximumZProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:multiply-images", Clij2MultiplyImages.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-images", Clij2MinimumImages.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:median-2d-box", Clij2Median2dBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:laplace-diamond", Clij2LaplaceDiamond.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:equal", Clij2Equal.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-percentile", Clij2ThresholdPercentile.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-intermodes", Clij2ThresholdIntermodes.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:generate-distance-matrix", Clij2GenerateDistanceMatrix.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:count-non-zero-voxels-3d-sphere", Clij2CountNonZeroVoxels3dSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:exclude-labels-with-values-out-of-range", Clij2ExcludeLabelsWithValuesOutOfRange.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:concatenate-stacks", Clij2ConcatenateStacks.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:exponential", Clij2Exponential.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:equalize-mean-intensities-of-slices", Clij2EqualizeMeanIntensitiesOfSlices.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:nonzero-minimum-diamond", Clij2NonzeroMinimumDiamond.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:standard-deviation-z-projection", Clij2StandardDeviationZProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-z-projection-thresholded-bounded", Clij2MinimumZProjectionThresholdedBounded.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-y-projection", Clij2MinimumYProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:affine-transform-3d", Clij2AffineTransform3d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:depth-color-projection", Clij2DepthColorProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-image-and-scalar", Clij2MaximumImageAndScalar.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-2d-sphere", Clij2Maximum2dSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:watershed", Clij2Watershed.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:transpose-xz", Clij2TransposeXz.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:count-non-zero-pixels-2d-sphere", Clij2CountNonZeroPixels2dSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:greater-constant", Clij2GreaterConstant.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:opening-diamond", Clij2OpeningDiamond.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:histogram", Clij2Histogram.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:median-3d-sphere", Clij2Median3dSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:detect-minima-slice-by-slice-box", Clij2DetectMinimaSliceBySliceBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:erode-sphere", Clij2ErodeSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-of-touching-neighbors", Clij2MeanOfTouchingNeighbors.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:get-jaccard-index", Clij2GetJaccardIndex.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:statistics-of-image", Clij2StatisticsOfImage.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:get-sorensen-dice-coefficient", Clij2GetSorensenDiceCoefficient.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:binary-not", Clij2BinaryNot.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:add-images-weighted", Clij2AddImagesWeighted.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:sobel", Clij2Sobel.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:bottom-hat-box", Clij2BottomHatBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:standard-deviation-of-masked-pixels", Clij2StandardDeviationOfMaskedPixels.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:paste-3d", Clij2Paste3d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:generate-binary-overlap-matrix", Clij2GenerateBinaryOverlapMatrix.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:generate-parametric-image", Clij2GenerateParametricImage.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:bottom-hat-sphere", Clij2BottomHatSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:sum-image-slice-by-slice", Clij2SumImageSliceBySlice.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:apply-vector-field-2d", Clij2ApplyVectorField2d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-default", Clij2ThresholdDefault.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:top-hat-sphere", Clij2TopHatSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:difference-of-gaussian-3d", Clij2DifferenceOfGaussian3d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:binary-x-or", Clij2BinaryXOr.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:rotate-counter-clockwise", Clij2RotateCounterClockwise.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-3d-box", Clij2Mean3dBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:difference-of-gaussian-2d", Clij2DifferenceOfGaussian2d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:binary-edge-detection", Clij2BinaryEdgeDetection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:entropy-box", Clij2EntropyBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:reslice-bottom", Clij2ResliceBottom.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:label-voronoi-octagon", Clij2LabelVoronoiOctagon.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:add-image-and-scalar", Clij2AddImageAndScalar.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:n-closest-points", Clij2NClosestPoints.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:average-distance-of-touching-neighbors", Clij2AverageDistanceOfTouchingNeighbors.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:paste-2d", Clij2Paste2d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:dilate-box-slice-by-slice", Clij2DilateBoxSliceBySlice.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:automatic-threshold", Clij2AutomaticThreshold.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:voronoi-labeling", Clij2VoronoiLabeling.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:multiply-image-and-scalar", Clij2MultiplyImageAndScalar.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:binary-or", Clij2BinaryOr.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:binary-and", Clij2BinaryAnd.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-x-projection", Clij2MeanXProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-yen", Clij2ThresholdYen.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:translate-3d", Clij2Translate3d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-z-projection", Clij2MinimumZProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:local-threshold", Clij2LocalThreshold.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:label-spots", Clij2LabelSpots.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:power-images", Clij2PowerImages.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-minimum", Clij2ThresholdMinimum.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:count-non-zero-pixels-slice-by-slice-sphere", Clij2CountNonZeroPixelsSliceBySliceSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-of-touching-neighbors", Clij2MinimumOfTouchingNeighbors.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-y-projection", Clij2MeanYProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-z-projection", Clij2MeanZProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:reduce-stack", Clij2ReduceStack.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:detect-maxima-slice-by-slice-box", Clij2DetectMaximaSliceBySliceBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:smaller", Clij2Smaller.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-iso-data", Clij2ThresholdIsoData.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:pull-to-results-table", Clij2PullToResultsTable.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:shortest-distances", Clij2ShortestDistances.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-3d-sphere", Clij2Mean3dSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:smaller-or-equal", Clij2SmallerOrEqual.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:average-distance-of-n-far-off-points", Clij2AverageDistanceOfNFarOffPoints.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-2d-box", Clij2Maximum2dBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:sorensen-dice-coefficient", Clij2SorensenDiceCoefficient.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-renyi-entropy", Clij2ThresholdRenyiEntropy.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:detect-label-edges", Clij2DetectLabelEdges.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:spots-to-point-list-1", Clij2SpotsToPointList1.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:onlyzero-overwrite-maximum-box", Clij2OnlyzeroOverwriteMaximumBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:exclude-labels-on-edges", Clij2ExcludeLabelsOnEdges.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-of-masked-pixels", Clij2MinimumOfMaskedPixels.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:reslice-left", Clij2ResliceLeft.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-mean", Clij2ThresholdMean.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:generate-touch-matrix", Clij2GenerateTouchMatrix.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:reslice-right", Clij2ResliceRight.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:translate-2d", Clij2Translate2d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:push-results-table", Clij2PushResultsTable.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:rotate-clockwise", Clij2RotateClockwise.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-2d-box", Clij2Minimum2dBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:get-mean-squared-error", Clij2GetMeanSquaredError.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:laplace-box", Clij2LaplaceBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:distance-map", Clij2DistanceMap.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:median-3d-box", Clij2Median3dBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:gradient-x", Clij2GradientX.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:scale-2d", Clij2Scale2d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:dilate-sphere-slice-by-slice", Clij2DilateSphereSliceBySlice.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:smaller-constant", Clij2SmallerConstant.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:generate-jaccard-index-matrix", Clij2GenerateJaccardIndexMatrix.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:standard-deviation-of-touching-neighbors", Clij2StandardDeviationOfTouchingNeighbors.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:multiply-image-stack-with-scalars", Clij2MultiplyImageStackWithScalars.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-octagon", Clij2MaximumOctagon.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:crop-3d", Clij2Crop3d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:erode-box", Clij2ErodeBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-3d-box", Clij2Minimum3dBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:median-2d-sphere", Clij2Median2dSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-distance-of-touching-neighbors", Clij2MinimumDistanceOfTouchingNeighbors.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:greater", Clij2Greater.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-images", Clij2MaximumImages.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:nonzero-maximum-diamond", Clij2NonzeroMaximumDiamond.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:count-touching-neighbors", Clij2CountTouchingNeighbors.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-z-projection-bounded", Clij2MaximumZProjectionBounded.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-3d-sphere", Clij2Minimum3dSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-z-projection-bounded", Clij2MeanZProjectionBounded.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:minimum-octagon", Clij2MinimumOctagon.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:generate-touch-count-matrix", Clij2GenerateTouchCountMatrix.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:opening-box", Clij2OpeningBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:greater-or-equal", Clij2GreaterOrEqual.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-triangle", Clij2ThresholdTriangle.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:sum-x-projection", Clij2SumXProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:exclude-labels-sub-surface", Clij2ExcludeLabelsSubSurface.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:median-slice-by-slice-box", Clij2MedianSliceBySliceBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-moments", Clij2ThresholdMoments.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:voronoi-octagon", Clij2VoronoiOctagon.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:adjacency-matrix-to-touch-matrix", Clij2AdjacencyMatrixToTouchMatrix.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:touch-matrix-to-mesh", Clij2TouchMatrixToMesh.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-min-error", Clij2ThresholdMinError.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:dilate-box", Clij2DilateBox.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-x-projection", Clij2MaximumXProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:maximum-of-masked-pixels", Clij2MaximumOfMaskedPixels.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:labelled-spots-to-point-list", Clij2LabelledSpotsToPointList.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:neighbors-of-neighbors", Clij2NeighborsOfNeighbors.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:replace-intensity", Clij2ReplaceIntensity.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mask-label", Clij2MaskLabel.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:reslice-radial", Clij2ResliceRadial.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:sum-z-projection", Clij2SumZProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:invert", Clij2Invert.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:scale-3d", Clij2Scale3d.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:median-z-projection", Clij2MedianZProjection.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:mean-squared-error", Clij2MeanSquaredError.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:gradient-z", Clij2GradientZ.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:multiply-image-and-coordinate", Clij2MultiplyImageAndCoordinate.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:point-index-list-to-mesh", Clij2PointIndexListToMesh.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:threshold-max-entropy", Clij2ThresholdMaxEntropy.class, UIUtils.getIconURLFromResources("apps/clij.png"));
        registerNodeType("clij2:dilate-sphere", Clij2DilateSphere.class, UIUtils.getIconURLFromResources("apps/clij.png"));


    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:clij2-integration";
    }

    @Override
    public String getDependencyVersion() {
        return "1.45.0";
    }
}
