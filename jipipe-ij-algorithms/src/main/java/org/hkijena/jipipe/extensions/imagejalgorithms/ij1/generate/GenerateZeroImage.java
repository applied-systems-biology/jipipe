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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.generate;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.BitDepth;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Create empty image", description = "Creates a new image that is black.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class GenerateZeroImage extends JIPipeSimpleIteratingAlgorithm {

    private int width = 256;
    private int height = 256;
    private int sizeZ = 1;
    private int sizeC = 1;
    private int sizeT = 1;
    private BitDepth bitDepth = BitDepth.Grayscale8u;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GenerateZeroImage(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GenerateZeroImage(GenerateZeroImage other) {
        super(other);
        this.bitDepth = other.bitDepth;
        this.width = other.width;
        this.height = other.height;
        this.sizeZ = other.sizeZ;
        this.sizeC = other.sizeC;
        this.sizeT = other.sizeT;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = IJ.createHyperStack("Generated", width, height, sizeC, sizeZ, sizeT, bitDepth.getBitDepth());
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Bit depth", description = "Determines the bit depth of the image")
    @JIPipeParameter("bit-depth")
    public BitDepth getBitDepth() {
        return bitDepth;
    }

    @JIPipeParameter("bit-depth")
    public void setBitDepth(BitDepth bitDepth) {
        this.bitDepth = bitDepth;
    }

    @JIPipeDocumentation(name = "Width", description = "The width of the generated image")
    @JIPipeParameter(value = "width", uiOrder = -20)
    public int getWidth() {
        return width;
    }

    @JIPipeParameter("width")
    public void setWidth(int width) {
        this.width = width;
    }

    @JIPipeDocumentation(name = "Height", description = "The height of the generated image")
    @JIPipeParameter(value = "height", uiOrder = -15)
    public int getHeight() {
        return height;
    }

    @JIPipeParameter("height")
    public void setHeight(int height) {
        this.height = height;
    }

    @JIPipeDocumentation(name = "Number of slices (Z)", description = "Number of generated Z slices.")
    @JIPipeParameter("size-z")
    public int getSizeZ() {
        return sizeZ;
    }

    @JIPipeParameter("size-z")
    public void setSizeZ(int sizeZ) {
        this.sizeZ = sizeZ;
    }

    @JIPipeDocumentation(name = "Number of channels (C)", description = "Number of generated channel slices.")
    @JIPipeParameter("size-c")
    public int getSizeC() {
        return sizeC;
    }

    @JIPipeParameter("size-c")
    public void setSizeC(int sizeC) {
        this.sizeC = sizeC;
    }

    @JIPipeDocumentation(name = "Number of frames (T)", description = "Number of generated frame slices.")
    @JIPipeParameter("size-t")
    public int getSizeT() {
        return sizeT;
    }

    @JIPipeParameter("size-t")
    public void setSizeT(int sizeT) {
        this.sizeT = sizeT;
    }
}
