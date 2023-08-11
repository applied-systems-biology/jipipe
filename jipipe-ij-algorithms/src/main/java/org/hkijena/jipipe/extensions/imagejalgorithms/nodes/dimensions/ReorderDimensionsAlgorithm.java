package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.dimensions;

import com.google.common.collect.Sets;
import ij.ImagePlus;
import ij.ImageStack;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

/**
 * Algorithm that reorders Hyperstack dimensions
 */
@JIPipeDocumentation(name = "Reorder dimensions", description = "Reorders dimensions of hyperstacks. This for example allows you to " +
        "switch the depth and time axis. If a stack is provided, it is interpreted as hyperstack with depth and with one frame and one channel. " +
        "2D images are ignored and passed to the output without processing.")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nHyperstacks", aliasName = "Re-order Hyperstack...")
public class ReorderDimensionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private HyperstackDimension targetZ = HyperstackDimension.Depth;
    private HyperstackDimension targetC = HyperstackDimension.Channel;
    private HyperstackDimension targetT = HyperstackDimension.Frame;

    public ReorderDimensionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ReorderDimensionsAlgorithm(ReorderDimensionsAlgorithm other) {
        super(other);
        this.targetZ = other.targetZ;
        this.targetC = other.targetC;
        this.targetT = other.targetT;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus image = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();

        if (!image.hasImageStack()) {
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), progressInfo);
            return;
        }

        ImageStack newStack = new ImageStack(image.getWidth(), image.getHeight(), image.getStackSize());
        int depth = image.getNSlices();
        int channels = image.getNChannels();
        int frames = image.getNFrames();
        int newDepth;
        int newChannels;
        int newFrames;

        switch (targetZ) {
            case Channel:
                newDepth = channels;
                break;
            case Depth:
                newDepth = depth;
                break;
            case Frame:
                newDepth = frames;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        switch (targetC) {
            case Channel:
                newChannels = channels;
                break;
            case Depth:
                newChannels = depth;
                break;
            case Frame:
                newChannels = frames;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        switch (targetT) {
            case Channel:
                newFrames = channels;
                break;
            case Depth:
                newFrames = depth;
                break;
            case Frame:
                newFrames = frames;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        ImageJUtils.forEachIndexedZCTSlice(image, (ip, sourceIndex) -> {
            int z, c, t;
            switch (targetZ) {
                case Channel:
                    z = sourceIndex.getC();
                    break;
                case Depth:
                    z = sourceIndex.getZ();
                    break;
                case Frame:
                    z = sourceIndex.getT();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            switch (targetC) {
                case Channel:
                    c = sourceIndex.getC();
                    break;
                case Depth:
                    c = sourceIndex.getZ();
                    break;
                case Frame:
                    c = sourceIndex.getT();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            switch (targetT) {
                case Channel:
                    t = sourceIndex.getC();
                    break;
                case Depth:
                    t = sourceIndex.getZ();
                    break;
                case Frame:
                    t = sourceIndex.getT();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            int targetStackIndex = new ImageSliceIndex(c, z, t).zeroSliceIndexToOneStackIndex(newChannels, newDepth, newFrames);
            newStack.setProcessor(ip, targetStackIndex);
        }, progressInfo);


        ImagePlus reorganized = new ImagePlus(image.getTitle(), newStack);
        reorganized.setTitle(image.getTitle());
        reorganized.setDimensions(newChannels, newDepth, newFrames);
        reorganized.setCalibration(image.getCalibration());
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(reorganized), progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        if (Sets.newHashSet(targetC, targetT, targetZ).size() != 3) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    context,
                    "Duplicate target dimensions!",
                    "You cannot have duplicate target dimensions.",
                    "Check that all targe dimensions are only used once."));
        }
    }

    @JIPipeDocumentation(name = "Move Z to ...", description = "Determines how the Z dimension is re-mapped.")
    @JIPipeParameter("target-z")
    public HyperstackDimension getTargetZ() {
        return targetZ;
    }

    @JIPipeParameter("target-z")
    public void setTargetZ(HyperstackDimension targetZ) {
        this.targetZ = targetZ;
    }

    @JIPipeDocumentation(name = "Move C to ...", description = "Determines how the C (channel) dimension is re-mapped.")
    @JIPipeParameter("target-c")
    public HyperstackDimension getTargetC() {
        return targetC;
    }

    @JIPipeParameter("target-c")
    public void setTargetC(HyperstackDimension targetC) {
        this.targetC = targetC;
    }

    @JIPipeDocumentation(name = "Move T to ...", description = "Determines how the T (time) dimension is re-mapped.")
    @JIPipeParameter("target-t")
    public HyperstackDimension getTargetT() {
        return targetT;
    }

    @JIPipeParameter("target-t")
    public void setTargetT(HyperstackDimension targetT) {
        this.targetT = targetT;
    }
}
