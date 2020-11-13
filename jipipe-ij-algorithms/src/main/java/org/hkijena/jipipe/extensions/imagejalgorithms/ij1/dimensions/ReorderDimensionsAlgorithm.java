package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnableInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.HyperstackDimensionPairParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.HashSet;
import java.util.Set;

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

    private HyperstackDimensionPairParameter.List hyperstackReassignments = new HyperstackDimensionPairParameter.List();

    public ReorderDimensionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        hyperstackReassignments.add(new HyperstackDimensionPairParameter(HyperstackDimension.Depth, HyperstackDimension.Depth));
        hyperstackReassignments.add(new HyperstackDimensionPairParameter(HyperstackDimension.Channel, HyperstackDimension.Channel));
        hyperstackReassignments.add(new HyperstackDimensionPairParameter(HyperstackDimension.Frame, HyperstackDimension.Frame));
    }

    public ReorderDimensionsAlgorithm(ReorderDimensionsAlgorithm other) {
        super(other);
        this.hyperstackReassignments = new HyperstackDimensionPairParameter.List(other.hyperstackReassignments);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnableInfo progress) {
        ImagePlus image = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class).getImage();

        if (!image.isStack() && !image.isHyperStack()) {
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(image));
            return;
        }

        ImagePlus reorganized = image.duplicate();
        reorganized.setTitle(image.getTitle());

        if (image.isStack() && !image.isHyperStack()) {
            int z = image.getStackSize();
            reorganized.setDimensions(1, z, 1);
        }

        int depth = reorganized.getNSlices();
        int channels = reorganized.getNChannels();
        int frames = reorganized.getNFrames();
        int newDepth = depth;
        int newChannels = channels;
        int newFrames = frames;

        for (HyperstackDimensionPairParameter reassignment : hyperstackReassignments) {
            int source;
            switch (reassignment.getKey()) {
                case Depth:
                    source = depth;
                    break;
                case Frame:
                    source = frames;
                    break;
                case Channel:
                    source = channels;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            switch (reassignment.getValue()) {
                case Depth:
                    newDepth = source;
                    break;
                case Frame:
                    newFrames = source;
                    break;
                case Channel:
                    newChannels = channels;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        reorganized.setDimensions(newChannels, newDepth, newFrames);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(reorganized));
    }

    @JIPipeDocumentation(name = "Reassignments", description = "Determines which channel is assigned to which other channel. To switch two channels, add two entries " +
            "into the list and assign the switching. For example A -> B and B -> A. Only having A -> B is not sufficient and will generate an error message.")
    @JIPipeParameter("hyperstack-reassignments")
    public HyperstackDimensionPairParameter.List getHyperstackReassignments() {
        return hyperstackReassignments;
    }

    @JIPipeParameter("hyperstack-reassignments")
    public void setHyperstackReassignments(HyperstackDimensionPairParameter.List hyperstackReassignments) {
        this.hyperstackReassignments = hyperstackReassignments;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        if (!hyperstackReassignments.isEmpty()) {
            Set<HyperstackDimension> sources = new HashSet<>();
            Set<HyperstackDimension> targets = new HashSet<>();
            for (HyperstackDimensionPairParameter reassignment : hyperstackReassignments) {
                sources.add(reassignment.getKey());
                targets.add(reassignment.getValue());
            }
            if (sources.size() != hyperstackReassignments.size()) {
                report.forCategory("Reassignments").reportIsInvalid("Invalid dimension reassignments!",
                        "You have duplicate assignment sources.",
                        "Please remove duplicate assignments",
                        this);
            }
            if (targets.size() != hyperstackReassignments.size()) {
                report.forCategory("Reassignments").reportIsInvalid("Invalid dimension reassignments!",
                        "You have duplicate assignment targets.",
                        "Please remove duplicate assignments",
                        this);
            }
            if (!sources.equals(targets)) {
                report.forCategory("Reassignments").reportIsInvalid("Invalid dimension reassignments!",
                        "You have incomplete assignments.",
                        "Please check that no dimensions are lost during reassignment.",
                        this);
            }
        }
    }
}
