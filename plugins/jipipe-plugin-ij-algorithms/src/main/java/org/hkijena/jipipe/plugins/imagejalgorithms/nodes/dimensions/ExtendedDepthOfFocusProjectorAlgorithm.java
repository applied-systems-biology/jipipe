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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.dimensions;

import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.HyperstackDimension;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


@SetJIPipeDocumentation(name = "Custom extended depth of focus (Z/C/T)", description = "Performs a Z, C, or T-Projection ")
@ConfigureJIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Score", create = true, description = "Optional image that scores each pixel. Must have the exact same size as the input. If not provided, the fallback method is used.", optional = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
public class ExtendedDepthOfFocusProjectorAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ScoringMethod fallbackScoringMethod = ScoringMethod.Tenengrad;
    private SelectionMethod selectionMethod = SelectionMethod.Maximum;
    private HyperstackDimension projectedAxis = HyperstackDimension.Depth;

    public ExtendedDepthOfFocusProjectorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtendedDepthOfFocusProjectorAlgorithm(ExtendedDepthOfFocusProjectorAlgorithm other) {
        super(other);
        this.projectedAxis = other.projectedAxis;
        this.selectionMethod = other.selectionMethod;
        this.fallbackScoringMethod = other.fallbackScoringMethod;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus score = ImageJUtils.unwrap(iterationStep.getInputData("Score", ImagePlusGreyscaleData.class, progressInfo));
        ImagePlus img = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo).getImage();

        if(score == null) {
            switch (fallbackScoringMethod) {
                case Tenengrad: {
                    progressInfo.log("Applying Tenengrad scoring ...");
                    score = ImageJUtils.convertToGreyscaleIfNeeded(ImageJUtils.duplicate(img));
                    ImageJIterationUtils.forEachSlice(score, ImageProcessor::findEdges, progressInfo);
                }
                break;
                case Variance: {
                    progressInfo.log("Applying Variance (r=1) scoring ...");
                    RankFilters rankFilters = new RankFilters();
                    score = ImageJUtils.convertToGreyscaleIfNeeded(ImageJUtils.duplicate(img));
                    ImageJIterationUtils.forEachSlice(score, ip -> rankFilters.rank(ip, 1, RankFilters.VARIANCE), progressInfo);
                }
                break;
                default:
                    throw new UnsupportedOperationException("Unknown scoring method: " + fallbackScoringMethod);
            }
        }

        if(!ImageJUtils.imagesHaveSameSize(score, img)) {
            throw new RuntimeException("The input and score images do not have the same size.");
        }

        ImagePlus result;
        if (img.getStackSize() > 1) {
            switch (projectedAxis) {
                case Channel: {
                    result = processChannel(img, score, progressInfo);
                }
                break;
                case Depth: {
                    result = processDepth(img, score, progressInfo);
                }
                break;
                case Frame: {
                    result = processFrame(img, score, progressInfo);
                }
                break;
                default:
                    throw new UnsupportedOperationException();
            }
        } else {
            result = img;
        }

        ImageJUtils.copyAttributes(img, result);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    private ImagePlus processFrame(ImagePlus img, ImagePlus score, JIPipeProgressInfo progressInfo) {
        return null;
    }

    private ImagePlus processDepth(ImagePlus img, ImagePlus score, JIPipeProgressInfo progressInfo) {
        FloatProcessor bestScoreIp = new FloatProcessor(img.getWidth(), img.getHeight());
        Map<ImageSliceIndex, ImageProcessor> resultSlices = new HashMap<>();

        for (int c = 0; c < img.getNChannels(); c++) {
            for (int t = 0; t < img.getNFrames(); t++) {
                progressInfo.log("c=" + c + ", t=" + t);
                if(progressInfo.isCancelled()) {
                    return null;
                }
                resetScore(bestScoreIp);
                ImageProcessor resultIp = ImageJUtils.createProcessor(img.getWidth(), img.getHeight(), img.getBitDepth());

                for (int z = 0; z < img.getNSlices(); z++) {
                    ImageProcessor inputIp = ImageJUtils.getSliceZero(img, c, z, t);
                    ImageProcessor scoreIp = ImageJUtils.getSliceZero(score, c, z, t);
                    final int nPixels = inputIp.getWidth() * inputIp.getHeight();

                    for (int i = 0; i < nPixels; i++) {
                        float currentScore = scoreIp.getf(i);
                        float bestScore = bestScoreIp.getf(i);

                        if(selectionMethod == SelectionMethod.Maximum) {
                            if(currentScore > bestScore) {
                                bestScoreIp.setf(i, currentScore);
                                resultIp.set(i, inputIp.get(i));
                            }
                        }
                        else {
                            if(currentScore < bestScore) {
                                bestScoreIp.setf(i, currentScore);
                                resultIp.set(i, inputIp.get(i));
                            }
                        }
                    }
                }

                resultSlices.put(new ImageSliceIndex(c,0,t), resultIp);
            }
        }

        return ImageJUtils.mergeMappedSlices(resultSlices);
    }

    private void resetScore(FloatProcessor bestScore) {
        if(selectionMethod == SelectionMethod.Maximum) {
            Arrays.fill((float[]) bestScore.getPixels(), Float.NEGATIVE_INFINITY);
        }
        else {
            Arrays.fill((float[]) bestScore.getPixels(), Float.POSITIVE_INFINITY);
        }
    }

    private ImagePlus processChannel(ImagePlus img, ImagePlus score, JIPipeProgressInfo progressInfo) {
        return null;
    }


    @SetJIPipeDocumentation(name = "Fallback scoring method", description = "The scoring method that is applied per 2D plane if no custom score image is provided. Variance applies a local variance filter, while Tenengrad calculates the Sobel gradient.")
    @JIPipeParameter("fallback-scoring-method")
    public ScoringMethod getFallbackScoringMethod() {
        return fallbackScoringMethod;
    }

    @JIPipeParameter("fallback-scoring-method")
    public void setFallbackScoringMethod(ScoringMethod fallbackScoringMethod) {
        this.fallbackScoringMethod = fallbackScoringMethod;
    }

    @SetJIPipeDocumentation(name = "Pixel selection method", description = "Determines if the pixel with the minimum or the maximum score is chosen")
    @JIPipeParameter("selection-method")
    public SelectionMethod getSelectionMethod() {
        return selectionMethod;
    }

    @JIPipeParameter("selection-method")
    public void setSelectionMethod(SelectionMethod selectionMethod) {
        this.selectionMethod = selectionMethod;
    }

    @SetJIPipeDocumentation(name = "Projected axis", description = "The axis that should be projected")
    @JIPipeParameter("projected-axis")
    public HyperstackDimension getProjectedAxis() {
        return projectedAxis;
    }

    @JIPipeParameter("projected-axis")
    public void setProjectedAxis(HyperstackDimension projectedAxis) {
        this.projectedAxis = projectedAxis;
    }

    public enum SelectionMethod {
        Minimum,
        Maximum
    }

    public enum ScoringMethod {
        Variance,
        Tenengrad
    }
}
