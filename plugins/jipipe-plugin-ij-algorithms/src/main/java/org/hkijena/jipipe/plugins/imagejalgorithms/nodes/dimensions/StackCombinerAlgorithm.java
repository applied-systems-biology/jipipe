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
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Combine stacks", description = "Combines the incoming stacks into one by adding the corresponding slices of the second stack to the first one. " +
        "For example, this allows to combine two one-channel stacks with the same number of slices into one with two channels. " +
        "This node has similar functionality to the 'Merge stacks into dimension' node.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Target", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Source", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Combined", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nStacks\nTools", aliasName = "Combine...")
public class StackCombinerAlgorithm extends JIPipeIteratingAlgorithm {

    private HyperstackDimension targetDimension = HyperstackDimension.Depth;

    public StackCombinerAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public StackCombinerAlgorithm(StackCombinerAlgorithm other) {
        super(other);
        this.targetDimension = other.targetDimension;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData targetData = iterationStep.getInputData("Target", ImagePlusData.class, progressInfo);
        ImagePlus src = iterationStep.getInputData("Source", targetData.getClass(), progressInfo).getImage();
        ImagePlus target = targetData.getImage();

        switch (targetDimension) {
            case Depth:
                combineDepth(iterationStep, progressInfo, src, target);
                break;
            case Channel:
                combineChannel(iterationStep, progressInfo, src, target);
                break;
            case Frame:
                combineFrames(iterationStep, progressInfo, src, target);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void combineFrames(JIPipeSingleIterationStep iterationStep, JIPipeProgressInfo progressInfo, ImagePlus src, ImagePlus target) {
        if (src.getNChannels() != target.getNChannels()) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Source and target do not have the same number of channels",
                    "You tried to combine two image stacks by depth, but they have a different number of channels.",
                    "Choose another target dimension or check the input."));
        }
        if (src.getNSlices() != target.getNSlices()) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Source and target do not have the same number of Z slices",
                    "You tried to combine two image stacks by channel, but they have a different number of Z slices.",
                    "Choose another target dimension or check the input."));
        }
        int nChannels = target.getNChannels();
        int nFrames = target.getNFrames() + src.getNFrames();
        int nSlices = target.getNSlices();
        ImageStack stack = new ImageStack(target.getWidth(), target.getHeight(), src.getStackSize() + target.getStackSize());
        copyOriginalImage(progressInfo, target, nChannels, nFrames, nSlices, stack);
        ImageJUtils.forEachIndexedZCTSlice(src, (ip, index) -> {
            int targetIndex = ImageJUtils.oneSliceIndexToOneStackIndex(index.getC() + 1,
                    index.getZ() + 1,
                    index.getT() + target.getNFrames() + 1,
                    nChannels,
                    nSlices,
                    nFrames);
            stack.setProcessor(ip.duplicate(), targetIndex);
        }, progressInfo.resolve("Copying second image"));
        ImagePlus resultImage = new ImagePlus("Combined", stack);
        resultImage.setDimensions(nChannels, nSlices, nFrames);
        resultImage.copyScale(src);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), progressInfo);
    }

    private void combineDepth(JIPipeSingleIterationStep iterationStep, JIPipeProgressInfo progressInfo, ImagePlus src, ImagePlus target) {
        if (src.getNChannels() != target.getNChannels()) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Source and target do not have the same number of channels",
                    "You tried to combine two image stacks by depth, but they have a different number of channels.",
                    "Choose another target dimension or check the input."));
        }
        if (src.getNFrames() != target.getNFrames()) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Source and target do not have the same number of frames",
                    "You tried to combine two image stacks by frame, but they have a different number of frames.",
                    "Choose another target dimension or check the input."));
        }
        int nChannels = target.getNChannels();
        int nFrames = target.getNFrames();
        int nSlices = target.getNSlices() + src.getNSlices();
        ImageStack stack = new ImageStack(target.getWidth(), target.getHeight(), src.getStackSize() + target.getStackSize());
        copyOriginalImage(progressInfo, target, nChannels, nFrames, nSlices, stack);
        ImageJUtils.forEachIndexedZCTSlice(src, (ip, index) -> {
            int targetIndex = ImageJUtils.oneSliceIndexToOneStackIndex(index.getC() + 1,
                    index.getZ() + target.getNSlices() + 1,
                    index.getT() + 1,
                    nChannels,
                    nSlices,
                    nFrames);
            stack.setProcessor(ip.duplicate(), targetIndex);
        }, progressInfo.resolve("Copying second image"));
        ImagePlus resultImage = new ImagePlus("Combined", stack);
        resultImage.setDimensions(nChannels, nSlices, nFrames);
        resultImage.copyScale(src);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), progressInfo);
    }

    private void combineChannel(JIPipeSingleIterationStep iterationStep, JIPipeProgressInfo progressInfo, ImagePlus src, ImagePlus target) {
        if (src.getNSlices() != target.getNSlices()) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Source and target do not have the same number of Z slices",
                    "You tried to combine two image stacks by channel, but they have a different number of Z slices.",
                    "Choose another target dimension or check the input."));
        }
        if (src.getNFrames() != target.getNFrames()) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Source and target do not have the same number of frames",
                    "You tried to combine two image stacks by frame, but they have a different number of frames.",
                    "Choose another target dimension or check the input."));
        }
        int nChannels = target.getNChannels() + src.getNChannels();
        int nFrames = target.getNFrames();
        int nSlices = target.getNSlices();
        ImageStack stack = new ImageStack(target.getWidth(), target.getHeight(), src.getStackSize() + target.getStackSize());
        copyOriginalImage(progressInfo, target, nChannels, nFrames, nSlices, stack);
        ImageJUtils.forEachIndexedZCTSlice(src, (ip, index) -> {
            int targetIndex = ImageJUtils.oneSliceIndexToOneStackIndex(index.getC() + target.getNChannels() + 1,
                    index.getZ() + 1,
                    index.getT() + 1,
                    nChannels,
                    nSlices,
                    nFrames);
            stack.setProcessor(ip.duplicate(), targetIndex);
        }, progressInfo.resolve("Copying second image"));
        ImagePlus resultImage = new ImagePlus("Combined", stack);
        resultImage.setDimensions(nChannels, nSlices, nFrames);
        resultImage.copyScale(src);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), progressInfo);
    }

    private void copyOriginalImage(JIPipeProgressInfo progressInfo, ImagePlus target, int nChannels, int nFrames, int nSlices, ImageStack stack) {
        ImageJUtils.forEachIndexedZCTSlice(target, (ip, index) -> {
            int targetIndex = ImageJUtils.oneSliceIndexToOneStackIndex(index.getC() + 1,
                    index.getZ() + 1,
                    index.getT() + 1,
                    nChannels,
                    nSlices,
                    nFrames);
            stack.setProcessor(ip.duplicate(), targetIndex);
        }, progressInfo.resolve("Copying original image"));
    }

    @SetJIPipeDocumentation(name = "Combine ...", description = "Determines to which dimension the slices should be added to. The other dimensions must have exactly " +
            "the same size. Example: If Z is selected, the number of channels and frames must be equal in both input stacks.")
    @JIPipeParameter("target-dimension")
    public HyperstackDimension getTargetDimension() {
        return targetDimension;
    }

    @JIPipeParameter("target-dimension")
    public void setTargetDimension(HyperstackDimension targetDimension) {
        this.targetDimension = targetDimension;
    }
}
