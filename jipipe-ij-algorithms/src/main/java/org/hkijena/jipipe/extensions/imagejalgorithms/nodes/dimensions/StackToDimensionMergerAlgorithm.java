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
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMultiDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Merge hyperstack C/Z/T (single input)", description = "Merges multiple stacks that miss a dimension into the target dimension. " +
        "Requires that all incoming stacks have the same size and that there is only one slice in the created dimension. " +
        "The type of the output image is determined by consensus bit depth. " +
        "This node has similar functionality to the 'Combine stacks' node.")
@JIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nHyperstacks")
public class StackToDimensionMergerAlgorithm extends JIPipeMergingAlgorithm {

    private HyperstackDimension createdDimension = HyperstackDimension.Channel;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public StackToDimensionMergerAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public StackToDimensionMergerAlgorithm(StackToDimensionMergerAlgorithm other) {
        super(other);
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

    @Override
    protected void runIteration(JIPipeMultiDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        List<ImagePlus> inputImages = new ArrayList<>();
        for (ImagePlusData data : dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo)) {
            inputImages.add(data.getImage());
        }
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
