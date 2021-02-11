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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Morphological operation 2D", description = "Applies a morphological operation to greyscale images. " +
        "Following operations are included: Erosion, Dilation, Opening, Closing, White Top Hat, Black Top Hat, Gradient, Laplacian, Internal Gradient, and External Gradient. " +
        "If a multi-channel image is provided, the operation is applied to each channel. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Morphology", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class Morphology2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Morphology.Operation operation = Morphology.Operation.DILATION;
    private Strel.Shape element = Strel.Shape.DISK;
    private int radius = 1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public Morphology2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public Morphology2DAlgorithm(Morphology2DAlgorithm other) {
        super(other);
        this.operation = other.operation;
        this.radius = other.radius;
        this.element = other.element;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        Strel strel = element.fromRadius(radius);
        ImageStack outputStack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
        ImageJUtils.forEachSlice(img, ip -> {
            // apply morphological operation
            ImageProcessor resultProcessor = operation.apply(ip, strel);

            // Keep same color model
            resultProcessor.setColorModel(ip.getColorModel());

            outputStack.addSlice(resultProcessor);
        }, progressInfo);
        ImagePlus result = new ImagePlus(operation.toString(), outputStack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Radius").checkIfWithin(this, radius, 0, Double.POSITIVE_INFINITY, false, true);
    }

    @JIPipeDocumentation(name = "Operation", description = "The morphological operation")
    @JIPipeParameter("operation")
    public Morphology.Operation getOperation() {
        return operation;
    }

    @JIPipeParameter("operation")
    public void setOperation(Morphology.Operation operation) {
        this.operation = operation;

    }

    @JIPipeDocumentation(name = "Radius", description = "Radius of the filter kernel in pixels.")
    @JIPipeParameter("radius")
    public int getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(int radius) {
        this.radius = radius;
    }

    @JIPipeDocumentation(name = "Structure element", description = "The structure element.")
    @JIPipeParameter("element")
    public Strel.Shape getElement() {
        return element;
    }

    @JIPipeParameter("element")
    public void setElement(Strel.Shape element) {
        this.element = element;
    }
}
