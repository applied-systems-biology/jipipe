package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import com.google.common.collect.Sets;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;

/**
 * Algorithm that reorders Hyperstack dimensions
 */
@JIPipeDocumentation(name = "Reorder dimensions", description = "Reorders dimensions of hyperstacks. This for example allows you to " +
        "switch the depth and time axis. If a stack is provided, it is interpreted as hyperstack with depth and with one frame and one channel. " +
        "2D images are ignored and passed to the output without processing.")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
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

        if (!image.isStack()) {
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), progressInfo);
            return;
        }

        ImagePlus reorganized = image.duplicate();
        reorganized.setTitle(image.getTitle());

        int depth = reorganized.getNSlices();
        int channels = reorganized.getNChannels();
        int frames = reorganized.getNFrames();
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

        reorganized.setDimensions(newChannels, newDepth, newFrames);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(reorganized), progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        if (Sets.newHashSet(targetC, targetT, targetZ).size() != 3) {
            report.forCategory("Dimensions").reportIsInvalid("Duplicate target dimensions!",
                    "You cannot have duplicate target dimensions.",
                    "Check that all targe dimensions are only used once.",
                    this);
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
