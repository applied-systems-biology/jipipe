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
import ij.ImageStack;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.HyperstackDimension;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerRange;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link ZProjector}
 */
@SetJIPipeDocumentation(name = "Z-Project", description = "Performs a Z-Projection. Also supports other axes C-Project/T-Project.")
@ConfigureJIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nStacks", aliasName = "Z Project... (alternative)")
public class NewZProjectorAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ZProjectorAlgorithm.Method method = ZProjectorAlgorithm.Method.MaxIntensity;
    private HyperstackDimension projectedAxis = HyperstackDimension.Depth;

    private OptionalIntegerRange restrictToIndices = new OptionalIntegerRange();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public NewZProjectorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public NewZProjectorAlgorithm(NewZProjectorAlgorithm other) {
        super(other);
        this.method = other.method;
        this.projectedAxis = other.projectedAxis;
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

        ImagePlus result;
        if (img.getStackSize() > 1) {
            switch (projectedAxis) {
                case Channel: {
                    result = processChannel(img, variables, progressInfo);
                }
                break;
                case Depth: {
                    result = processDepth(img, variables, progressInfo);
                }
                break;
                case Frame: {
                    result = processFrame(img, variables, progressInfo);
                }
                break;
                default:
                    throw new UnsupportedOperationException();
            }
        } else {
            result = img;
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    private ImagePlus processFrame(ImagePlus img, JIPipeExpressionVariablesMap variables, JIPipeProgressInfo progressInfo) {
        final int newDepth = img.getNSlices();
        final int newFrames = 1;
        final int newChannels = img.getNChannels();
        final ImageStack resultStack = new ImageStack(img.getWidth(), img.getHeight(), newDepth * newChannels * newFrames);

        // Generate the source slice indices
        List<Integer> sourceIndices;
        if (restrictToIndices.isEnabled()) {
            sourceIndices = restrictToIndices.getContent().getIntegers(0, img.getNFrames() - 1, variables);
        } else {
            sourceIndices = new ArrayList<>();
            for (int i = 0; i < img.getNFrames(); i++) {
                sourceIndices.add(i);
            }
        }

        for (int z = 0; z < newDepth; z++) {
            for (int c = 0; c < newChannels; c++) {
                ImageSliceIndex firstSliceIndex = new ImageSliceIndex(c, z, 0);
                progressInfo.log(firstSliceIndex.toString());

                // Extract the T slices into the stack
                ImageStack extracted = new ImageStack(img.getWidth(), img.getHeight());
                for (int t : sourceIndices) {
                    ImageProcessor processor = ImageJUtils.getSliceZero(img, c, z, t);
                    extracted.addSlice(processor);
                }

                // C-project the stack
                ImagePlus extractedImp = new ImagePlus("stack", extracted);
                ImagePlus projected = ZProjector.run(extractedImp, method.getNativeValue());

                resultStack.setProcessor(projected.getProcessor(), ImageJUtils.zeroSliceIndexToOneStackIndex(c, z, 0, newChannels, newDepth, newFrames));
            }
        }

        ImagePlus projected = new ImagePlus(img.getTitle() + " projected " + projectedAxis, resultStack);
        projected.copyScale(img);
        projected.setDimensions(newChannels, newDepth, newFrames);
        projected = ImageJUtils.copyLUTsIfNeeded(img, projected);
        return projected;
    }

    private ImagePlus processDepth(ImagePlus img, JIPipeExpressionVariablesMap variables, JIPipeProgressInfo progressInfo) {
        final int newDepth = 1;
        final int newFrames = img.getNFrames();
        final int newChannels = img.getNChannels();
        final ImageStack resultStack = new ImageStack(img.getWidth(), img.getHeight(), newDepth * newChannels * newFrames);

        // Generate the source slice indices
        List<Integer> sourceIndices;
        if (restrictToIndices.isEnabled()) {
            sourceIndices = restrictToIndices.getContent().getIntegers(0, img.getNSlices() - 1, variables);
        } else {
            sourceIndices = new ArrayList<>();
            for (int i = 0; i < img.getNSlices(); i++) {
                sourceIndices.add(i);
            }
        }

        for (int c = 0; c < newChannels; c++) {
            for (int t = 0; t < newFrames; t++) {
                ImageSliceIndex firstSliceIndex = new ImageSliceIndex(c, 0, t);
                progressInfo.log(firstSliceIndex.toString());

                // Extract the Z slices into the stack
                ImageStack extracted = new ImageStack(img.getWidth(), img.getHeight());
                for (int z : sourceIndices) {
                    ImageProcessor processor = ImageJUtils.getSliceZero(img, c, z, t);
                    extracted.addSlice(processor);
                }

                // Z-project the stack
                ImagePlus extractedImp = new ImagePlus("stack", extracted);
                ImagePlus projected = ZProjector.run(extractedImp, method.getNativeValue());

                resultStack.setProcessor(projected.getProcessor(), ImageJUtils.zeroSliceIndexToOneStackIndex(c, 0, t, newChannels, newDepth, newFrames));
            }
        }

        ImagePlus projected = new ImagePlus(img.getTitle() + " projected " + projectedAxis, resultStack);
        projected.copyScale(img);
        projected.setDimensions(newChannels, newDepth, newFrames);
        projected = ImageJUtils.copyLUTsIfNeeded(img, projected);
        return projected;
    }

    private ImagePlus processChannel(ImagePlus img, JIPipeExpressionVariablesMap variables, JIPipeProgressInfo progressInfo) {
        final int newDepth = img.getNSlices();
        final int newFrames = img.getNFrames();
        final int newChannels = 1;
        final ImageStack resultStack = new ImageStack(img.getWidth(), img.getHeight(), newDepth * newChannels * newFrames);

        // Generate the source slice indices
        List<Integer> sourceIndices;
        if (restrictToIndices.isEnabled()) {
            sourceIndices = restrictToIndices.getContent().getIntegers(0, img.getNChannels() - 1, variables);
        } else {
            sourceIndices = new ArrayList<>();
            for (int i = 0; i < img.getNChannels(); i++) {
                sourceIndices.add(i);
            }
        }

        for (int z = 0; z < newDepth; z++) {
            for (int t = 0; t < newFrames; t++) {
                ImageSliceIndex firstSliceIndex = new ImageSliceIndex(0, z, t);
                progressInfo.log(firstSliceIndex.toString());

                // Extract the C slices into the stack
                ImageStack extracted = new ImageStack(img.getWidth(), img.getHeight());
                for (int c : sourceIndices) {
                    ImageProcessor processor = ImageJUtils.getSliceZero(img, c, z, t);
                    extracted.addSlice(processor);
                }

                // C-project the stack
                ImagePlus extractedImp = new ImagePlus("stack", extracted);
                ImagePlus projected = ZProjector.run(extractedImp, method.getNativeValue());

                resultStack.setProcessor(projected.getProcessor(), ImageJUtils.zeroSliceIndexToOneStackIndex(0, z, t, newChannels, newDepth, newFrames));
            }
        }

        ImagePlus projected = new ImagePlus(img.getTitle() + " projected " + projectedAxis, resultStack);
        projected.copyScale(img);
        projected.setDimensions(newChannels, newDepth, newFrames);
        return projected;
    }

    @SetJIPipeDocumentation(name = "Method", description = "The function that is applied to each stack of pixels.")
    @JIPipeParameter("method")
    public ZProjectorAlgorithm.Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(ZProjectorAlgorithm.Method method) {
        this.method = method;
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
}
