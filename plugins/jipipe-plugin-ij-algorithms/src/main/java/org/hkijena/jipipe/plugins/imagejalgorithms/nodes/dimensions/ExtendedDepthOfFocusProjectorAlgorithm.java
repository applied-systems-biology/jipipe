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
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.HyperstackDimension;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerRange;


@SetJIPipeDocumentation(name = "Custom extended depth of focus (Z/C/T)", description = "Performs a Z, C, or T-Projection ")
@ConfigureJIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Score", create = true, description = "Optional image that scores each pixel. Must have the exact same size as the input. If not provided, one of the fallback functions is used.", optional = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
public class ExtendedDepthOfFocusProjectorAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ScoringMethod fallbackScoringMethod = ScoringMethod.Tenengrad;
    private SelectionMethod selectionMethod = SelectionMethod.Maximum;
    private HyperstackDimension projectedAxis = HyperstackDimension.Depth;

    private OptionalIntegerRange restrictToIndices = new OptionalIntegerRange();

    public ExtendedDepthOfFocusProjectorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtendedDepthOfFocusProjectorAlgorithm(ExtendedDepthOfFocusProjectorAlgorithm other) {
        super(other);
        this.projectedAxis = other.projectedAxis;
        this.selectionMethod = other.selectionMethod;
        this.fallbackScoringMethod = other.fallbackScoringMethod;
        this.restrictToIndices = new OptionalIntegerRange(other.restrictToIndices);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

//        ImagePlus result;
//        if (img.getStackSize() > 1) {
//            switch (projectedAxis) {
//                case Channel: {
//                    result = processChannel(img, variables, progressInfo);
//                }
//                break;
//                case Depth: {
//                    result = processDepth(img, variables, progressInfo);
//                }
//                break;
//                case Frame: {
//                    result = processFrame(img, variables, progressInfo);
//                }
//                break;
//                default:
//                    throw new UnsupportedOperationException();
//            }
//        } else {
//            result = img;
//        }

//        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

//    private ImagePlus processFrame(ImagePlus img, JIPipeExpressionVariablesMap variables, JIPipeProgressInfo progressInfo) {
//        final int newDepth = img.getNSlices();
//        final int newFrames = 1;
//        final int newChannels = img.getNChannels();
//        final ImageStack resultStack = new ImageStack(img.getWidth(), img.getHeight(), newDepth * newChannels * newFrames);
//
//        // Generate the source slice indices
//        List<Integer> sourceIndices;
//        if (restrictToIndices.isEnabled()) {
//            sourceIndices = restrictToIndices.getContent().getIntegers(0, img.getNFrames() - 1, variables);
//        } else {
//            sourceIndices = new ArrayList<>();
//            for (int i = 0; i < img.getNFrames(); i++) {
//                sourceIndices.add(i);
//            }
//        }
//
//        for (int z = 0; z < newDepth; z++) {
//            for (int c = 0; c < newChannels; c++) {
//                ImageSliceIndex firstSliceIndex = new ImageSliceIndex(c, z, 0);
//                progressInfo.log(firstSliceIndex.toString());
//
//                // Extract the T slices into the stack
//                ImageStack extracted = new ImageStack(img.getWidth(), img.getHeight());
//                for (int t : sourceIndices) {
//                    ImageProcessor processor = ImageJUtils.getSliceZero(img, c, z, t);
//                    extracted.addSlice(processor);
//                }
//
//                // C-project the stack
//                ImagePlus extractedImp = new ImagePlus("stack", extracted);
//                ImagePlus projected = ZProjector.run(extractedImp, method.getNativeValue());
//
//                resultStack.setProcessor(projected.getProcessor(), ImageJUtils.zeroSliceIndexToOneStackIndex(c, z, 0, newChannels, newDepth, newFrames));
//            }
//        }
//
//        ImagePlus projected = new ImagePlus(img.getTitle() + " projected " + projectedAxis, resultStack);
//        projected.copyScale(img);
//        projected.setDimensions(newChannels, newDepth, newFrames);
//        return projected;
//    }
//
//    private ImagePlus processDepth(ImagePlus img, JIPipeExpressionVariablesMap variables, JIPipeProgressInfo progressInfo) {
//        final int newDepth = 1;
//        final int newFrames = img.getNFrames();
//        final int newChannels = img.getNChannels();
//        final ImageStack resultStack = new ImageStack(img.getWidth(), img.getHeight(), newDepth * newChannels * newFrames);
//
//        // Generate the source slice indices
//        List<Integer> sourceIndices;
//        if (restrictToIndices.isEnabled()) {
//            sourceIndices = restrictToIndices.getContent().getIntegers(0, img.getNSlices() - 1, variables);
//        } else {
//            sourceIndices = new ArrayList<>();
//            for (int i = 0; i < img.getNSlices(); i++) {
//                sourceIndices.add(i);
//            }
//        }
//
//        for (int c = 0; c < newChannels; c++) {
//            for (int t = 0; t < newFrames; t++) {
//                ImageSliceIndex firstSliceIndex = new ImageSliceIndex(c, 0, t);
//                progressInfo.log(firstSliceIndex.toString());
//
//                // Extract the Z slices into the stack
//                ImageStack extracted = new ImageStack(img.getWidth(), img.getHeight());
//                for (int z : sourceIndices) {
//                    ImageProcessor processor = ImageJUtils.getSliceZero(img, c, z, t);
//                    extracted.addSlice(processor);
//                }
//
//                // Z-project the stack
//                ImagePlus extractedImp = new ImagePlus("stack", extracted);
//                ImagePlus projected = ZProjector.run(extractedImp, method.getNativeValue());
//
//                resultStack.setProcessor(projected.getProcessor(), ImageJUtils.zeroSliceIndexToOneStackIndex(c, 0, t, newChannels, newDepth, newFrames));
//            }
//        }
//
//        ImagePlus projected = new ImagePlus(img.getTitle() + " projected " + projectedAxis, resultStack);
//        projected.copyScale(img);
//        projected.setDimensions(newChannels, newDepth, newFrames);
//        return projected;
//    }
//
//    private ImagePlus processChannel(ImagePlus img, JIPipeExpressionVariablesMap variables, JIPipeProgressInfo progressInfo) {
//        final int newDepth = img.getNSlices();
//        final int newFrames = img.getNFrames();
//        final int newChannels = 1;
//        final ImageStack resultStack = new ImageStack(img.getWidth(), img.getHeight(), newDepth * newChannels * newFrames);
//
//        // Generate the source slice indices
//        List<Integer> sourceIndices;
//        if (restrictToIndices.isEnabled()) {
//            sourceIndices = restrictToIndices.getContent().getIntegers(0, img.getNChannels() - 1, variables);
//        } else {
//            sourceIndices = new ArrayList<>();
//            for (int i = 0; i < img.getNChannels(); i++) {
//                sourceIndices.add(i);
//            }
//        }
//
//        for (int z = 0; z < newDepth; z++) {
//            for (int t = 0; t < newFrames; t++) {
//                ImageSliceIndex firstSliceIndex = new ImageSliceIndex(0, z, t);
//                progressInfo.log(firstSliceIndex.toString());
//
//                // Extract the C slices into the stack
//                ImageStack extracted = new ImageStack(img.getWidth(), img.getHeight());
//                for (int c : sourceIndices) {
//                    ImageProcessor processor = ImageJUtils.getSliceZero(img, c, z, t);
//                    extracted.addSlice(processor);
//                }
//
//                // C-project the stack
//                ImagePlus extractedImp = new ImagePlus("stack", extracted);
//                ImagePlus projected = ZProjector.run(extractedImp, method.getNativeValue());
//
//                resultStack.setProcessor(projected.getProcessor(), ImageJUtils.zeroSliceIndexToOneStackIndex(0, z, t, newChannels, newDepth, newFrames));
//            }
//        }
//
//        ImagePlus projected = new ImagePlus(img.getTitle() + " projected " + projectedAxis, resultStack);
//        projected.copyScale(img);
//        projected.setDimensions(newChannels, newDepth, newFrames);
//        return projected;
//    }

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

    @SetJIPipeDocumentation(name = "Restrict to indices", description = "If enabled, allows to restrict to specific projected indices")
    @JIPipeParameter("restrict-to-indices")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public OptionalIntegerRange getRestrictToIndices() {
        return restrictToIndices;
    }

    @JIPipeParameter("restrict-to-indices")
    public void setRestrictToIndices(OptionalIntegerRange restrictToIndices) {
        this.restrictToIndices = restrictToIndices;
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
