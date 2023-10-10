/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.dimensions;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.color.MergeChannelsAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.library.graph.InputSlotMapParameterCollection;

import java.util.*;


/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Merge hyperstack C/Z/T", description = "Merges multiple stacks that miss a dimension into the target dimension. " +
        "Requires that all incoming stacks have the same size and that there is only one slice in the created dimension. " +
        "The type of the output image is determined by the consensus bit depth. " +
        "This node has similar functionality to the 'Combine stacks' node. " +
        "If no order (ascending) is given for each input, the order of input images is determined by the order in the slot list.")
@JIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nHyperstacks")
public class StackToDimensionMerger2Algorithm extends JIPipeIteratingAlgorithm {

    private final InputSlotMapParameterCollection orderAssignment;
    private HyperstackDimension createdDimension = HyperstackDimension.Channel;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public StackToDimensionMerger2Algorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputTo(ImagePlusData.class)
                .addOutputSlot("Output", "The output", ImagePlusData.class).sealOutput().build());
        orderAssignment = new InputSlotMapParameterCollection(Integer.class, this, slotInfo -> 0, false);
        orderAssignment.updateSlots();
        registerSubParameter(orderAssignment);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public StackToDimensionMerger2Algorithm(StackToDimensionMerger2Algorithm other) {
        super(other);
        orderAssignment = new InputSlotMapParameterCollection(MergeChannelsAlgorithm.ChannelColor.class, this, slotInfo -> 0, false);
        other.orderAssignment.copyTo(orderAssignment);
        registerSubParameter(orderAssignment);
        this.createdDimension = other.createdDimension;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @JIPipeDocumentation(name = "Created dimension", description = "The dimension that is created by merging the " +
            "incoming stacks.")
    @JIPipeParameter("created-dimension")
    public HyperstackDimension getCreatedDimension() {
        return createdDimension;
    }

    @JIPipeParameter("created-dimension")
    public void setCreatedDimension(HyperstackDimension createdDimension) {
        this.createdDimension = createdDimension;
    }

    @JIPipeDocumentation(name = "Input order", description = "The following settings allow you to customize the order of the input images. The inputs are ordered ascending.")
    @JIPipeParameter("order")
    public InputSlotMapParameterCollection getOrderAssignment() {
        return orderAssignment;
    }

    private List<ImagePlus> getOrderedInputImages(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<ImagePlus> inputImages = new ArrayList<>();
        Map<JIPipeDataSlot, Integer> orderMap = new HashMap<>();
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            orderMap.put(inputSlot, orderAssignment.get(inputSlot.getName()).get(Integer.class));
        }
        orderMap.values().stream().distinct().sorted(Comparator.naturalOrder()).forEach(orderIndex -> {
            for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
                if (orderMap.get(inputSlot).equals(orderIndex)) {
                    inputImages.add(dataBatch.getInputData(inputSlot, ImagePlusData.class, progressInfo).getImage());
                }
            }
        });
        return inputImages;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        List<ImagePlus> inputImages = getOrderedInputImages(dataBatch, progressInfo);
        inputImages = ImageJUtils.convertToConsensusBitDepthIfNeeded(inputImages);

        if (inputImages.isEmpty())
            return;
        if (inputImages.size() == 1) {
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(inputImages.get(0)), progressInfo);
            return;
        }
        if (!ImageJUtils.imagesHaveSameSize(inputImages)) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }

        // Collect
        Map<ImageSliceIndex, ImageProcessor> indexMappings = new HashMap<>();
        int dc = 0;
        int dz = 0;
        int dt = 0;
        for (ImagePlus inputImage : inputImages) {
            for (int c = 0; c < inputImage.getNChannels(); c++) {
                for (int z = 0; z < inputImage.getNSlices(); z++) {
                    for (int t = 0; t < inputImage.getNFrames(); t++) {
                        indexMappings.put(new ImageSliceIndex(c + dc, z + dz, t + dt), ImageJUtils.getSliceZero(inputImage, c, z, t));
                    }
                }
            }
            switch (createdDimension) {
                case Channel:
                    dc += inputImage.getNChannels();
                    break;
                case Depth:
                    dz += inputImage.getNSlices();
                    break;
                case Frame:
                    dt += inputImage.getNFrames();
                    break;
            }
        }

        // Merge
        ImagePlus resultImage = ImageJUtils.combineSlices(indexMappings);
        resultImage.copyScale(inputImages.get(0));
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), progressInfo);
    }
}
