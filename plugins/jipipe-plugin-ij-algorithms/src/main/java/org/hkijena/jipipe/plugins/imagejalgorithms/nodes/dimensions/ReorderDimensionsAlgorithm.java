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

import com.google.common.collect.Sets;
import ij.ImagePlus;
import ij.ImageStack;
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
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;

/**
 * Algorithm that reorders Hyperstack dimensions
 */
@SetJIPipeDocumentation(name = "Reorder dimensions", description = "Reorders dimensions of hyperstacks. This for example allows you to " +
        "switch the depth and time axis. If a stack is provided, it is interpreted as hyperstack with depth and with one frame and one channel. " +
        "2D images are ignored and passed to the output without processing.")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nHyperstacks", aliasName = "Re-order Hyperstack...")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();

        if (!image.hasImageStack()) {
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), progressInfo);
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
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(reorganized), progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (Sets.newHashSet(targetC, targetT, targetZ).size() != 3) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    reportContext,
                    "Duplicate target dimensions!",
                    "You cannot have duplicate target dimensions.",
                    "Check that all targe dimensions are only used once."));
        }
    }

    @SetJIPipeDocumentation(name = "Move Z to ...", description = "Determines how the Z dimension is re-mapped.")
    @JIPipeParameter("target-z")
    public HyperstackDimension getTargetZ() {
        return targetZ;
    }

    @JIPipeParameter("target-z")
    public void setTargetZ(HyperstackDimension targetZ) {
        this.targetZ = targetZ;
    }

    @SetJIPipeDocumentation(name = "Move C to ...", description = "Determines how the C (channel) dimension is re-mapped.")
    @JIPipeParameter("target-c")
    public HyperstackDimension getTargetC() {
        return targetC;
    }

    @JIPipeParameter("target-c")
    public void setTargetC(HyperstackDimension targetC) {
        this.targetC = targetC;
    }

    @SetJIPipeDocumentation(name = "Move T to ...", description = "Determines how the T (time) dimension is re-mapped.")
    @JIPipeParameter("target-t")
    public HyperstackDimension getTargetT() {
        return targetT;
    }

    @JIPipeParameter("target-t")
    public void setTargetT(HyperstackDimension targetT) {
        this.targetT = targetT;
    }
}
