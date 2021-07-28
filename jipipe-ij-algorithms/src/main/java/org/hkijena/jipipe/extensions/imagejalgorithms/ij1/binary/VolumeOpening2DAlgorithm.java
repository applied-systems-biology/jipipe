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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.binary;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Area opening 2D", description = "Removes all objects with an area smaller than the provided number of pixels. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Binary", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
public class VolumeOpening2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int minPixels = 50;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public VolumeOpening2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public VolumeOpening2DAlgorithm(VolumeOpening2DAlgorithm other) {
        super(other);
        this.minPixels = other.minPixels;
    }

    @JIPipeDocumentation(name = "Min number of pixels", description = "Minimum number of pixels a connected component should have.")
    @JIPipeParameter("min-pixels")
    public int getMinPixels() {
        return minPixels;
    }

    @JIPipeParameter("min-pixels")
    public void setMinPixels(int minPixels) {
        this.minPixels = minPixels;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(inputImage.getWidth(), inputImage.getHeight(), inputImage.getStackSize());

        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            ImageProcessor processor = BinaryImages.areaOpening(ip, minPixels);
            stack.setProcessor(processor, index.zeroSliceIndexToOneStackIndex(inputImage));
        }, progressInfo);
        ImagePlus outputImage = new ImagePlus("AreaOpening-" + minPixels, stack);
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        outputImage.copyScale(inputImage);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscaleMaskData(outputImage), progressInfo);
    }
}
