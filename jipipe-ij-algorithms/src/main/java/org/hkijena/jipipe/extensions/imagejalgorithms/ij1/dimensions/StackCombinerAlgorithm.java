package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.ImageStack;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Combine stacks", description = "Combines the incoming stacks into one by adding the corresponding slices of the second stack to the first one.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Target", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Source", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Combined", autoCreate = true, inheritedSlot = "Target")
public class StackCombinerAlgorithm extends JIPipeIteratingAlgorithm {

    private HyperstackDimension targetDimension = HyperstackDimension.Depth;

    public StackCombinerAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public StackCombinerAlgorithm(StackCombinerAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData targetData = dataBatch.getInputData("Target", ImagePlusData.class, progressInfo);
        ImagePlus src = dataBatch.getInputData("Source", targetData.getClass(), progressInfo).getImage();
        ImagePlus target = targetData.getImage();

        switch (targetDimension) {
            case Depth:
                combineDepth(dataBatch, progressInfo, src, target);
                break;
            case Channel:
                combineChannel(dataBatch, progressInfo, src, target);
                break;
            case Frame:
                combineFrames(dataBatch, progressInfo, src, target);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void combineFrames(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo, ImagePlus src, ImagePlus target) {
        if(src.getNChannels() != target.getNChannels()) {
            throw new UserFriendlyRuntimeException("Source and target do not have the same number of channels",
                    "Cannot combine stacks with different number of channels!",
                    getName(),
                    "You tried to combine two image stacks by depth, but they have a different number of channels.",
                    "Choose another target dimension or check the input.");
        }
        if(src.getNSlices() != target.getNSlices()) {
            throw new UserFriendlyRuntimeException("Source and target do not have the same number of Z slices",
                    "Cannot combine stacks with different number of channels!",
                    getName(),
                    "You tried to combine two image stacks by channel, but they have a different number of Z slices.",
                    "Choose another target dimension or check the input.");
        }
        int nChannels = target.getNChannels();
        int nFrames = target.getNFrames()+ src.getNFrames();
        int nSlices = target.getNSlices();
        ImageStack stack = new ImageStack(target.getWidth(), target.getHeight(), src.getStackSize() + target.getStackSize());
        copyOriginalImage(progressInfo, target, nChannels, nFrames, nSlices, stack);
        ImageJUtils.forEachIndexedZCTSlice(src, (ip, index) -> {
            int targetIndex = ImageJUtils.getStackIndex(index.getC() + 1,
                    index.getZ() + 1,
                    index.getT() + target.getNFrames() + 1,
                    nChannels,
                    nSlices,
                    nFrames);
            stack.setProcessor(ip.duplicate(), targetIndex);
        }, progressInfo.resolve("Copying second image"));
        ImagePlus resultImage = new ImagePlus("Combined", stack);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), progressInfo);
    }

    private void combineDepth(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo, ImagePlus src, ImagePlus target) {
        if(src.getNChannels() != target.getNChannels()) {
            throw new UserFriendlyRuntimeException("Source and target do not have the same number of channels",
                    "Cannot combine stacks with different number of channels!",
                    getName(),
                    "You tried to combine two image stacks by depth, but they have a different number of channels.",
                    "Choose another target dimension or check the input.");
        }
        if(src.getNFrames() != target.getNFrames()) {
            throw new UserFriendlyRuntimeException("Source and target do not have the same number of frames",
                    "Cannot combine stacks with different number of frames!",
                    getName(),
                    "You tried to combine two image stacks by depth, but they have a different number of frames.",
                    "Choose another target dimension or check the input.");
        }
        int nChannels = target.getNChannels();
        int nFrames = target.getNFrames();
        int nSlices = target.getNSlices() + src.getNSlices();
        ImageStack stack = new ImageStack(target.getWidth(), target.getHeight(), src.getStackSize() + target.getStackSize());
        copyOriginalImage(progressInfo, target, nChannels, nFrames, nSlices, stack);
        ImageJUtils.forEachIndexedZCTSlice(src, (ip, index) -> {
            int targetIndex = ImageJUtils.getStackIndex(index.getC() + 1,
                    index.getZ() + target.getNSlices() + 1,
                    index.getT() + 1,
                    nChannels,
                    nSlices,
                    nFrames);
            stack.setProcessor(ip.duplicate(), targetIndex);
        }, progressInfo.resolve("Copying second image"));
        ImagePlus resultImage = new ImagePlus("Combined", stack);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), progressInfo);
    }

    private void combineChannel(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo, ImagePlus src, ImagePlus target) {
        if(src.getNSlices() != target.getNSlices()) {
            throw new UserFriendlyRuntimeException("Source and target do not have the same number of Z slices",
                    "Cannot combine stacks with different number of channels!",
                    getName(),
                    "You tried to combine two image stacks by channel, but they have a different number of Z slices.",
                    "Choose another target dimension or check the input.");
        }
        if(src.getNFrames() != target.getNFrames()) {
            throw new UserFriendlyRuntimeException("Source and target do not have the same number of frames",
                    "Cannot combine stacks with different number of frames!",
                    getName(),
                    "You tried to combine two image stacks by channel, but they have a different number of frames.",
                    "Choose another target dimension or check the input.");
        }
        int nChannels = target.getNChannels() + src.getNChannels();
        int nFrames = target.getNFrames();
        int nSlices = target.getNSlices();
        ImageStack stack = new ImageStack(target.getWidth(), target.getHeight(), src.getStackSize() + target.getStackSize());
        copyOriginalImage(progressInfo, target, nChannels, nFrames, nSlices, stack);
        ImageJUtils.forEachIndexedZCTSlice(src, (ip, index) -> {
            int targetIndex = ImageJUtils.getStackIndex(index.getC() + target.getNChannels() + 1,
                    index.getZ() + 1,
                    index.getT() + 1,
                    nChannels,
                    nSlices,
                    nFrames);
            stack.setProcessor(ip.duplicate(), targetIndex);
        }, progressInfo.resolve("Copying second image"));
        ImagePlus resultImage = new ImagePlus("Combined", stack);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), progressInfo);
    }

    private void copyOriginalImage(JIPipeProgressInfo progressInfo, ImagePlus target, int nChannels, int nFrames, int nSlices, ImageStack stack) {
        ImageJUtils.forEachIndexedZCTSlice(target, (ip, index) -> {
            int targetIndex = ImageJUtils.getStackIndex(index.getC() + 1,
                    index.getZ() + 1,
                    index.getT() + 1,
                    nChannels,
                    nSlices,
                    nFrames);
            stack.setProcessor(ip.duplicate(), targetIndex);
        }, progressInfo.resolve("Copying original image"));
    }

    @JIPipeDocumentation(name = "Combine ...", description = "Determines to which dimension the slices should be added to. The other dimensions must have exactly " +
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
