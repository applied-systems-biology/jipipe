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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.generate;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMultiDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMissingDataGeneratorAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.OptionalBitDepth;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Generate missing image", description = "Generates an empty (black) image if it cannot be matched to a reference")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
public class GenerateMissingZeroImage extends JIPipeMissingDataGeneratorAlgorithm {

    private OptionalIntegerParameter overwriteWidth = new OptionalIntegerParameter(false, 256);
    private OptionalIntegerParameter overwriteHeight = new OptionalIntegerParameter(false, 256);
    private OptionalIntegerParameter overwriteSizeZ = new OptionalIntegerParameter(false, 1);
    private OptionalIntegerParameter overwriteSizeC = new OptionalIntegerParameter(false, 1);
    private OptionalIntegerParameter overwriteSizeT = new OptionalIntegerParameter(false, 1);
    private OptionalBitDepth overwriteOutputBitDepth = OptionalBitDepth.None;
    private boolean generateOnePerBatch = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GenerateMissingZeroImage(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GenerateMissingZeroImage(GenerateMissingZeroImage other) {
        super(other);
        this.overwriteWidth = new OptionalIntegerParameter(other.overwriteWidth);
        this.overwriteHeight = new OptionalIntegerParameter(other.overwriteHeight);
        this.overwriteSizeZ = new OptionalIntegerParameter(other.overwriteSizeZ);
        this.overwriteSizeC = new OptionalIntegerParameter(other.overwriteSizeC);
        this.overwriteSizeT = new OptionalIntegerParameter(other.overwriteSizeT);
        this.overwriteOutputBitDepth = other.overwriteOutputBitDepth;
        this.generateOnePerBatch = other.generateOnePerBatch;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runGenerator(JIPipeMultiDataBatch dataBatch, JIPipeInputDataSlot inputSlot, JIPipeOutputDataSlot outputSlot, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot referenceSlot = getInputSlot("Reference");
        for (Integer inputRow : dataBatch.getInputRows(referenceSlot)) {
            progressInfo.log("Row " + inputRow);
            ImagePlus referenceImage = referenceSlot.getData(inputRow, ImagePlusData.class, progressInfo).getImage();
            int width = referenceImage.getWidth();
            int height = referenceImage.getHeight();
            int sizeC = referenceImage.getNChannels();
            int sizeZ = referenceImage.getNSlices();
            int sizeT = referenceImage.getNFrames();
            int bitDepth = referenceImage.getBitDepth();
            if (overwriteWidth.isEnabled())
                width = overwriteWidth.getContent();
            if (overwriteHeight.isEnabled())
                height = overwriteHeight.getContent();
            if (overwriteSizeC.isEnabled())
                sizeC = overwriteSizeC.getContent();
            if (overwriteSizeZ.isEnabled())
                sizeZ = overwriteSizeZ.getContent();
            if (overwriteSizeT.isEnabled())
                sizeT = overwriteSizeT.getContent();
            if (overwriteOutputBitDepth != OptionalBitDepth.None)
                bitDepth = overwriteOutputBitDepth.getBitDepth();

            ImagePlus img = IJ.createHyperStack("Generated", width, height, sizeC, sizeZ, sizeT, bitDepth);

            dataBatch.addOutputData(outputSlot, new ImagePlusData(img), progressInfo);
            if (generateOnePerBatch)
                break;
        }
    }

    @JIPipeDocumentation(name = "Generate one per batch", description = "If enabled, only one image per batch is generated. " +
            "The first image is used as reference.")
    @JIPipeParameter("generate-one-per-batch")
    public boolean isGenerateOnePerBatch() {
        return generateOnePerBatch;
    }

    @JIPipeParameter("generate-one-per-batch")
    public void setGenerateOnePerBatch(boolean generateOnePerBatch) {
        this.generateOnePerBatch = generateOnePerBatch;
    }

    @JIPipeDocumentation(name = "Overwrite bit depth", description = "If none is selected, the output type is the same as the type " +
            "of the input image. Otherwise, the bit depth is set according to the selection.")
    @JIPipeParameter("overwrite-output-bit-depth")
    public OptionalBitDepth getOverwriteOutputBitDepth() {
        return overwriteOutputBitDepth;
    }

    @JIPipeParameter("overwrite-output-bit-depth")
    public void setOverwriteOutputBitDepth(OptionalBitDepth overwriteOutputBitDepth) {
        this.overwriteOutputBitDepth = overwriteOutputBitDepth;
    }

    @JIPipeDocumentation(name = "Overwrite Width", description = "The width of the generated image")
    @JIPipeParameter("width")
    public OptionalIntegerParameter getOverwriteWidth() {
        return overwriteWidth;
    }

    @JIPipeParameter("width")
    public void setOverwriteWidth(OptionalIntegerParameter overwriteWidth) {
        this.overwriteWidth = overwriteWidth;
    }

    @JIPipeDocumentation(name = "Overwrite Height", description = "The height of the generated image")
    @JIPipeParameter("height")
    public OptionalIntegerParameter getOverwriteHeight() {
        return overwriteHeight;
    }

    @JIPipeParameter("height")
    public void setOverwriteHeight(OptionalIntegerParameter overwriteHeight) {
        this.overwriteHeight = overwriteHeight;
    }

    @JIPipeDocumentation(name = "Overwrite number of slices (Z)", description = "Number of generated Z slices.")
    @JIPipeParameter("size-z")
    public OptionalIntegerParameter getOverwriteSizeZ() {
        return overwriteSizeZ;
    }

    @JIPipeParameter("size-z")
    public void setOverwriteSizeZ(OptionalIntegerParameter overwriteSizeZ) {
        this.overwriteSizeZ = overwriteSizeZ;
    }

    @JIPipeDocumentation(name = "Overwrite number of channels (C)", description = "Number of generated channel slices.")
    @JIPipeParameter("size-c")
    public OptionalIntegerParameter getOverwriteSizeC() {
        return overwriteSizeC;
    }

    @JIPipeParameter("size-c")
    public void setOverwriteSizeC(OptionalIntegerParameter overwriteSizeC) {
        this.overwriteSizeC = overwriteSizeC;
    }

    @JIPipeDocumentation(name = "Overwrite number of frames (T)", description = "Number of generated frame slices.")
    @JIPipeParameter("size-t")
    public OptionalIntegerParameter getOverwriteSizeT() {
        return overwriteSizeT;
    }

    @JIPipeParameter("size-t")
    public void setOverwriteSizeT(OptionalIntegerParameter overwriteSizeT) {
        this.overwriteSizeT = overwriteSizeT;
    }
}
