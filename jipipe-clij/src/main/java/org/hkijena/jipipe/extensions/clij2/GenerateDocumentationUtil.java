package org.hkijena.jipipe.extensions.clij2;
import ch.systemsx.cisd.base.annotation.JsonObject;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import net.haesleinhuepf.clij2.plugins.*;
import net.haesleinhuepf.clij2.utilities.HasAuthor;
import net.haesleinhuepf.clij2.utilities.HasLicense;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GenerateDocumentationUtil {
    public static void main(String[] args) {
        Class[] classes = new Class[] {net.haesleinhuepf.clij2.plugins.AbsoluteDifference.class,
                net.haesleinhuepf.clij2.plugins.GenerateParametricImageFromResultsTableColumn.class,
                net.haesleinhuepf.clij2.plugins.EqualConstant.class,
                net.haesleinhuepf.clij2.plugins.LabelToMask.class,
                net.haesleinhuepf.clij2.plugins.Threshold.class,
                net.haesleinhuepf.clij2.plugins.ThresholdHuang.class,
                net.haesleinhuepf.clij2.plugins.MinimumZProjectionBounded.class,
                net.haesleinhuepf.clij2.plugins.NotEqual.class,
                net.haesleinhuepf.clij2.plugins.MedianOfTouchingNeighbors.class,
                net.haesleinhuepf.clij2.plugins.FloodFillDiamond.class,
                net.haesleinhuepf.clij2.plugins.Mean2DSphere.class,
                net.haesleinhuepf.clij2.plugins.DetectMaximaBox.class,
                net.haesleinhuepf.clij2.plugins.MaximumYProjection.class,
                net.haesleinhuepf.clij2.plugins.Power.class,
                net.haesleinhuepf.clij2.plugins.ExcludeLabels.class,
                net.haesleinhuepf.clij2.plugins.CloseIndexGapsInLabelMap.class,
                net.haesleinhuepf.clij2.plugins.ErodeSphereSliceBySlice.class,
                net.haesleinhuepf.clij2.plugins.Maximum3DSphere.class,
                net.haesleinhuepf.clij2.plugins.MinimumImageAndScalar.class,
                net.haesleinhuepf.clij2.plugins.MinimumSliceBySliceSphere.class,
                net.haesleinhuepf.clij2.plugins.ThresholdIJ_IsoData.class,
                net.haesleinhuepf.clij2.plugins.ClosingDiamond.class,
                net.haesleinhuepf.clij2.plugins.Minimum2DSphere.class,
                net.haesleinhuepf.clij2.plugins.MultiplyMatrix.class,
                net.haesleinhuepf.clij2.plugins.TopHatBox.class,
                net.haesleinhuepf.clij2.plugins.Crop2D.class,
                net.haesleinhuepf.clij2.plugins.ThresholdLi.class,
                net.haesleinhuepf.clij2.plugins.TransposeXY.class,
                net.haesleinhuepf.clij2.plugins.ReplacePixelsIfZero.class,
                net.haesleinhuepf.clij2.plugins.AffineTransform2D.class,
                net.haesleinhuepf.clij2.plugins.GaussianBlur3D.class,
                net.haesleinhuepf.clij2.plugins.PointlistToLabelledSpots.class,
                net.haesleinhuepf.clij2.plugins.ApplyVectorField3D.class,
                net.haesleinhuepf.clij2.plugins.GaussianBlur2D.class,
                net.haesleinhuepf.clij2.plugins.ReplaceIntensities.class,
                net.haesleinhuepf.clij2.plugins.ExcludeLabelsWithValuesWithinRange.class,
                net.haesleinhuepf.clij2.plugins.AddImages.class,
                net.haesleinhuepf.clij2.plugins.Logarithm.class,
                net.haesleinhuepf.clij2.plugins.AverageDistanceOfNClosestPoints.class,
                net.haesleinhuepf.clij2.plugins.VarianceOfMaskedPixels.class,
                net.haesleinhuepf.clij2.plugins.MinimumXProjection.class,
                net.haesleinhuepf.clij2.plugins.ExcludeLabelsOnSurface.class,
                net.haesleinhuepf.clij2.plugins.OnlyzeroOverwriteMaximumDiamond.class,
                net.haesleinhuepf.clij2.plugins.NonzeroMaximumBox.class,
                net.haesleinhuepf.clij2.plugins.MultiplyStackWithPlane.class,
                net.haesleinhuepf.clij2.plugins.BinaryFillHoles.class,
                net.haesleinhuepf.clij2.plugins.DownsampleSliceBySliceHalfMedian.class,
                net.haesleinhuepf.clij2.plugins.Resample.class,
                net.haesleinhuepf.clij2.plugins.NotEqualConstant.class,
                net.haesleinhuepf.clij2.plugins.UndefinedToZero.class,
                net.haesleinhuepf.clij2.plugins.ClosingBox.class,
                net.haesleinhuepf.clij2.plugins.SmallerOrEqualConstant.class,
                net.haesleinhuepf.clij2.plugins.Convolve.class,
                net.haesleinhuepf.clij2.plugins.SubtractImages.class,
                net.haesleinhuepf.clij2.plugins.Rotate3D.class,
                net.haesleinhuepf.clij2.plugins.SetNonZeroPixelsToPixelIndex.class,
                net.haesleinhuepf.clij2.plugins.CombineHorizontally.class,
                net.haesleinhuepf.clij2.plugins.MeanClosestSpotDistance.class,
                net.haesleinhuepf.clij2.plugins.Mean2DBox.class,
                net.haesleinhuepf.clij2.plugins.MaximumOfTouchingNeighbors.class,
                net.haesleinhuepf.clij2.plugins.ResliceTop.class,
                net.haesleinhuepf.clij2.plugins.GradientY.class,
                net.haesleinhuepf.clij2.plugins.NClosestDistances.class,
                net.haesleinhuepf.clij2.plugins.ErodeBoxSliceBySlice.class,
                net.haesleinhuepf.clij2.plugins.ArgMaximumZProjection.class,
                net.haesleinhuepf.clij2.plugins.CentroidsOfLabels.class,
                net.haesleinhuepf.clij2.plugins.StatisticsOfLabelledPixels.class,
                net.haesleinhuepf.clij2.plugins.WriteValuesToPositions.class,
                net.haesleinhuepf.clij2.plugins.LabelCentroidsToPointList.class,
                net.haesleinhuepf.clij2.plugins.CombineVertically.class,
                net.haesleinhuepf.clij2.plugins.Absolute.class,
                net.haesleinhuepf.clij2.plugins.ThresholdOtsu.class,
                net.haesleinhuepf.clij2.plugins.MaximumSliceBySliceSphere.class,
                net.haesleinhuepf.clij2.plugins.MaskStackWithPlane.class,
                net.haesleinhuepf.clij2.plugins.NonzeroMinimumBox.class,
                net.haesleinhuepf.clij2.plugins.MedianSliceBySliceSphere.class,
                net.haesleinhuepf.clij2.plugins.SquaredDifference.class,
                net.haesleinhuepf.clij2.plugins.Maximum3DBox.class,
                net.haesleinhuepf.clij2.plugins.JaccardIndex.class,
                net.haesleinhuepf.clij2.plugins.SumYProjection.class,
                net.haesleinhuepf.clij2.plugins.Copy.class,
                net.haesleinhuepf.clij2.plugins.TouchMatrixToAdjacencyMatrix.class,
                net.haesleinhuepf.clij2.plugins.DivideImages.class,
                net.haesleinhuepf.clij2.plugins.GreaterOrEqualConstant.class,
                net.haesleinhuepf.clij2.plugins.Rotate2D.class,
                net.haesleinhuepf.clij2.plugins.ThresholdShanbhag.class,
                net.haesleinhuepf.clij2.plugins.ExtendLabelingViaVoronoi.class,
                net.haesleinhuepf.clij2.plugins.MeanSliceBySliceSphere.class,
                net.haesleinhuepf.clij2.plugins.TransposeYZ.class,
                net.haesleinhuepf.clij2.plugins.ImageToStack.class,
                net.haesleinhuepf.clij2.plugins.SubtractImageFromScalar.class,
                net.haesleinhuepf.clij2.plugins.BinarySubtract.class,
                net.haesleinhuepf.clij2.plugins.MeanOfMaskedPixels.class,
                net.haesleinhuepf.clij2.plugins.Mask.class,
                net.haesleinhuepf.clij2.plugins.MaximumZProjection.class,
                net.haesleinhuepf.clij2.plugins.MultiplyImages.class,
                net.haesleinhuepf.clij2.plugins.ConnectedComponentsLabeling.class,
                net.haesleinhuepf.clij2.plugins.MinimumImages.class,
                net.haesleinhuepf.clij2.plugins.Median2DBox.class,
                net.haesleinhuepf.clij2.plugins.LaplaceDiamond.class,
                net.haesleinhuepf.clij2.plugins.Equal.class,
                net.haesleinhuepf.clij2.plugins.ThresholdPercentile.class,
                net.haesleinhuepf.clij2.plugins.ThresholdIntermodes.class,
                net.haesleinhuepf.clij2.plugins.GenerateDistanceMatrix.class,
                net.haesleinhuepf.clij2.plugins.CountNonZeroVoxels3DSphere.class,
                net.haesleinhuepf.clij2.plugins.ExcludeLabelsWithValuesOutOfRange.class,
                net.haesleinhuepf.clij2.plugins.CopySlice.class,
                net.haesleinhuepf.clij2.plugins.ConcatenateStacks.class,
                net.haesleinhuepf.clij2.plugins.Exponential.class,
                net.haesleinhuepf.clij2.plugins.EqualizeMeanIntensitiesOfSlices.class,
                net.haesleinhuepf.clij2.plugins.NonzeroMinimumDiamond.class,
                net.haesleinhuepf.clij2.plugins.StandardDeviationZProjection.class,
                net.haesleinhuepf.clij2.plugins.MinimumZProjectionThresholdedBounded.class,
                net.haesleinhuepf.clij2.plugins.MinimumYProjection.class,
                net.haesleinhuepf.clij2.plugins.AffineTransform3D.class,
                net.haesleinhuepf.clij2.plugins.DepthColorProjection.class,
                net.haesleinhuepf.clij2.plugins.MaximumImageAndScalar.class,
                net.haesleinhuepf.clij2.plugins.Maximum2DSphere.class,
                net.haesleinhuepf.clij2.plugins.Watershed.class,
                net.haesleinhuepf.clij2.plugins.TransposeXZ.class,
                net.haesleinhuepf.clij2.plugins.CountNonZeroPixels2DSphere.class,
                net.haesleinhuepf.clij2.plugins.GreaterConstant.class,
                net.haesleinhuepf.clij2.plugins.OpeningDiamond.class,
                net.haesleinhuepf.clij2.plugins.Histogram.class,
                net.haesleinhuepf.clij2.plugins.Median3DSphere.class,
                net.haesleinhuepf.clij2.plugins.DetectMinimaSliceBySliceBox.class,
                net.haesleinhuepf.clij2.plugins.ErodeSphere.class,
                net.haesleinhuepf.clij2.plugins.MeanOfTouchingNeighbors.class,
                net.haesleinhuepf.clij2.plugins.GetJaccardIndex.class,
                net.haesleinhuepf.clij2.plugins.GetSorensenDiceCoefficient.class,
                net.haesleinhuepf.clij2.plugins.BinaryNot.class,
                net.haesleinhuepf.clij2.plugins.AddImagesWeighted.class,
                net.haesleinhuepf.clij2.plugins.Sobel.class,
                net.haesleinhuepf.clij2.plugins.BottomHatBox.class,
                net.haesleinhuepf.clij2.plugins.StandardDeviationOfMaskedPixels.class,
                net.haesleinhuepf.clij2.plugins.Flip3D.class,
                net.haesleinhuepf.clij2.plugins.Paste3D.class,
                net.haesleinhuepf.clij2.plugins.GenerateBinaryOverlapMatrix.class,
                net.haesleinhuepf.clij2.plugins.GenerateParametricImage.class,
                net.haesleinhuepf.clij2.plugins.BottomHatSphere.class,
                net.haesleinhuepf.clij2.plugins.SumImageSliceBySlice.class,
                net.haesleinhuepf.clij2.plugins.ApplyVectorField2D.class,
                net.haesleinhuepf.clij2.plugins.ThresholdDefault.class,
                net.haesleinhuepf.clij2.plugins.TopHatSphere.class,
                net.haesleinhuepf.clij2.plugins.DifferenceOfGaussian3D.class,
                net.haesleinhuepf.clij2.plugins.BinaryXOr.class,
                net.haesleinhuepf.clij2.plugins.RotateCounterClockwise.class,
                net.haesleinhuepf.clij2.plugins.Mean3DBox.class,
                net.haesleinhuepf.clij2.plugins.DifferenceOfGaussian2D.class,
                net.haesleinhuepf.clij2.plugins.BinaryEdgeDetection.class,
                net.haesleinhuepf.clij2.plugins.EntropyBox.class,
                net.haesleinhuepf.clij2.plugins.ResliceBottom.class,
                net.haesleinhuepf.clij2.plugins.LabelVoronoiOctagon.class,
                net.haesleinhuepf.clij2.plugins.AddImageAndScalar.class,
                net.haesleinhuepf.clij2.plugins.NClosestPoints.class,
                net.haesleinhuepf.clij2.plugins.AverageDistanceOfTouchingNeighbors.class,
                net.haesleinhuepf.clij2.plugins.Paste2D.class,
                net.haesleinhuepf.clij2.plugins.DilateBoxSliceBySlice.class,
                net.haesleinhuepf.clij2.plugins.AutomaticThreshold.class,
                net.haesleinhuepf.clij2.plugins.VoronoiLabeling.class,
                net.haesleinhuepf.clij2.plugins.MultiplyImageAndScalar.class,
                net.haesleinhuepf.clij2.plugins.BinaryOr.class,
                net.haesleinhuepf.clij2.plugins.BinaryAnd.class,
                net.haesleinhuepf.clij2.plugins.MeanXProjection.class,
                net.haesleinhuepf.clij2.plugins.ThresholdYen.class,
                net.haesleinhuepf.clij2.plugins.Translate3D.class,
                net.haesleinhuepf.clij2.plugins.MinimumZProjection.class,
                net.haesleinhuepf.clij2.plugins.LocalThreshold.class,
                net.haesleinhuepf.clij2.plugins.LabelSpots.class,
                net.haesleinhuepf.clij2.plugins.PowerImages.class,
                net.haesleinhuepf.clij2.plugins.ThresholdMinimum.class,
                net.haesleinhuepf.clij2.plugins.CountNonZeroPixelsSliceBySliceSphere.class,
                net.haesleinhuepf.clij2.plugins.MinimumOfTouchingNeighbors.class,
                net.haesleinhuepf.clij2.plugins.MeanYProjection.class,
                net.haesleinhuepf.clij2.plugins.MeanZProjection.class,
                net.haesleinhuepf.clij2.plugins.ReduceStack.class,
                net.haesleinhuepf.clij2.plugins.DetectMaximaSliceBySliceBox.class,
                net.haesleinhuepf.clij2.plugins.Smaller.class,
                net.haesleinhuepf.clij2.plugins.ThresholdIsoData.class,
                net.haesleinhuepf.clij2.plugins.ShortestDistances.class,
                net.haesleinhuepf.clij2.plugins.Mean3DSphere.class,
                net.haesleinhuepf.clij2.plugins.SmallerOrEqual.class,
                net.haesleinhuepf.clij2.plugins.AverageDistanceOfNFarOffPoints.class,
                net.haesleinhuepf.clij2.plugins.Maximum2DBox.class,
                net.haesleinhuepf.clij2.plugins.SorensenDiceCoefficient.class,
                net.haesleinhuepf.clij2.plugins.ThresholdRenyiEntropy.class,
                net.haesleinhuepf.clij2.plugins.DetectLabelEdges.class,
                net.haesleinhuepf.clij2.plugins.Flip2D.class,
                net.haesleinhuepf.clij2.plugins.SpotsToPointList.class,
                net.haesleinhuepf.clij2.plugins.OnlyzeroOverwriteMaximumBox.class,
                net.haesleinhuepf.clij2.plugins.ExcludeLabelsOnEdges.class,
                net.haesleinhuepf.clij2.plugins.MinimumOfMaskedPixels.class,
                net.haesleinhuepf.clij2.plugins.ResliceLeft.class,
                net.haesleinhuepf.clij2.plugins.ThresholdMean.class,
                net.haesleinhuepf.clij2.plugins.GenerateTouchMatrix.class,
                net.haesleinhuepf.clij2.plugins.ResliceRight.class,
                net.haesleinhuepf.clij2.plugins.Translate2D.class,
                net.haesleinhuepf.clij2.plugins.RotateClockwise.class,
                net.haesleinhuepf.clij2.plugins.Minimum2DBox.class,
                net.haesleinhuepf.clij2.plugins.GetMeanSquaredError.class,
                net.haesleinhuepf.clij2.plugins.LaplaceBox.class,
                net.haesleinhuepf.clij2.plugins.DistanceMap.class,
                net.haesleinhuepf.clij2.plugins.Median3DBox.class,
                net.haesleinhuepf.clij2.plugins.GradientX.class,
                net.haesleinhuepf.clij2.plugins.Scale2D.class,
                net.haesleinhuepf.clij2.plugins.DilateSphereSliceBySlice.class,
                net.haesleinhuepf.clij2.plugins.SmallerConstant.class,
                net.haesleinhuepf.clij2.plugins.GenerateJaccardIndexMatrix.class,
                net.haesleinhuepf.clij2.plugins.StandardDeviationOfTouchingNeighbors.class,
                net.haesleinhuepf.clij2.plugins.MultiplyImageStackWithScalars.class,
                net.haesleinhuepf.clij2.plugins.MaximumOctagon.class,
                net.haesleinhuepf.clij2.plugins.Crop3D.class,
                net.haesleinhuepf.clij2.plugins.ErodeBox.class,
                net.haesleinhuepf.clij2.plugins.Minimum3DBox.class,
                net.haesleinhuepf.clij2.plugins.Median2DSphere.class,
                net.haesleinhuepf.clij2.plugins.MinimumDistanceOfTouchingNeighbors.class,
                net.haesleinhuepf.clij2.plugins.DetectMinimaBox.class,
                net.haesleinhuepf.clij2.plugins.Greater.class,
                net.haesleinhuepf.clij2.plugins.MaximumImages.class,
                net.haesleinhuepf.clij2.plugins.NonzeroMaximumDiamond.class,
                net.haesleinhuepf.clij2.plugins.CountTouchingNeighbors.class,
                net.haesleinhuepf.clij2.plugins.MaximumZProjectionBounded.class,
                net.haesleinhuepf.clij2.plugins.Minimum3DSphere.class,
                net.haesleinhuepf.clij2.plugins.MeanZProjectionBounded.class,
                net.haesleinhuepf.clij2.plugins.MinimumOctagon.class,
                net.haesleinhuepf.clij2.plugins.GenerateTouchCountMatrix.class,
                net.haesleinhuepf.clij2.plugins.OpeningBox.class,
                net.haesleinhuepf.clij2.plugins.GreaterOrEqual.class,
                net.haesleinhuepf.clij2.plugins.ThresholdTriangle.class,
                net.haesleinhuepf.clij2.plugins.SumXProjection.class,
                net.haesleinhuepf.clij2.plugins.ExcludeLabelsSubSurface.class,
                net.haesleinhuepf.clij2.plugins.MedianSliceBySliceBox.class,
                net.haesleinhuepf.clij2.plugins.ThresholdMoments.class,
                net.haesleinhuepf.clij2.plugins.VoronoiOctagon.class,
                net.haesleinhuepf.clij2.plugins.AdjacencyMatrixToTouchMatrix.class,
                net.haesleinhuepf.clij2.plugins.TouchMatrixToMesh.class,
                net.haesleinhuepf.clij2.plugins.ThresholdMinError.class,
                net.haesleinhuepf.clij2.plugins.DilateBox.class,
                net.haesleinhuepf.clij2.plugins.MaximumXProjection.class,
                net.haesleinhuepf.clij2.plugins.MaximumOfMaskedPixels.class,
                net.haesleinhuepf.clij2.plugins.LabelledSpotsToPointList.class,
                net.haesleinhuepf.clij2.plugins.NeighborsOfNeighbors.class,
                net.haesleinhuepf.clij2.plugins.ReplaceIntensity.class,
                net.haesleinhuepf.clij2.plugins.MaskLabel.class,
                net.haesleinhuepf.clij2.plugins.ResliceRadial.class,
                net.haesleinhuepf.clij2.plugins.SumZProjection.class,
                net.haesleinhuepf.clij2.plugins.Invert.class,
                net.haesleinhuepf.clij2.plugins.Scale3D.class,
                net.haesleinhuepf.clij2.plugins.MedianZProjection.class,
                net.haesleinhuepf.clij2.plugins.MeanSquaredError.class,
                net.haesleinhuepf.clij2.plugins.GradientZ.class,
                net.haesleinhuepf.clij2.plugins.MultiplyImageAndCoordinate.class,
                net.haesleinhuepf.clij2.plugins.PointIndexListToMesh.class,
                net.haesleinhuepf.clij2.plugins.ThresholdMaxEntropy.class,
                net.haesleinhuepf.clij2.plugins.DilateSphere.class };

        ObjectNode node = JsonUtils.getObjectMapper().getNodeFactory().objectNode();
        for (int i = 0; i < classes.length; i++) {
            Object instance = ReflectionUtils.newInstance(classes[i]);
            StringBuilder stringBuilder = new StringBuilder();
            if(instance instanceof OffersDocumentation) {
                stringBuilder.append(((OffersDocumentation) instance).getDescription());
                stringBuilder.append(" Works for following image dimensions: ").append(((OffersDocumentation) instance).getAvailableForDimensions()).append(".");
            }
            if(instance instanceof HasAuthor) {
                stringBuilder.append(" Developed by ").append(((HasAuthor) instance).getAuthorName()).append(".");
            }
            if(instance instanceof HasLicense) {
                stringBuilder.append(" Licensed under ").append(((HasLicense) instance).getLicense());
            }
            node.put(classes[i].getCanonicalName(), stringBuilder.toString());
        }
        Path path = Paths.get("descriptions.json").toAbsolutePath();
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(path);

    }
}
