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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.morphology;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Morphological operation 3D", description = "Applies a morphological operation to greyscale images. " +
        "Following operations are included: Erosion, Dilation, Opening, Closing, White Top Hat, Black Top Hat, Gradient, Laplacian, Internal Gradient, and External Gradient. " +
        "More information (including examples) can be found at https://imagej.net/MorphoLibJ.html\n" +
        "If a multi-channel image is provided, the operation is applied to each channel.")
@JIPipeNode(menuPath = "Morphology", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nFiltering", aliasName = "Morphological Filters (3D)")
public class Morphology3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Morphology.Operation operation = Morphology.Operation.DILATION;
    private Strel3D.Shape element = Strel3D.Shape.BALL;
    private int radius = 1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public Morphology3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public Morphology3DAlgorithm(Morphology3DAlgorithm other) {
        super(other);
        this.operation = other.operation;
        this.radius = other.radius;
        this.element = other.element;
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo);
        ImagePlus img = inputData.getImage();
        Strel3D strel = element.fromRadius(radius);
        ImagePlus result = process(img, operation, strel);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    public ImagePlus process(ImagePlus image, Morphology.Operation op, Strel3D strel) {
        // Check validity of parameters
        if (image == null)
            return null;

        // extract the input stack
        ImageStack inputStack = image.getStack();

        // apply morphological operation
        ImageStack resultStack = op.apply(inputStack, strel);

        // create the new image plus from the processor
        String newName = image.getShortTitle() + "-" + op;
        ImagePlus resultPlus = new ImagePlus(newName, resultStack);
        resultPlus.copyScale(image);

        // return the created array
        return resultPlus;
    }

    @JIPipeDocumentation(name = "Operation", description = "The morphological operation. Following operations are supported: " +
            "<ul>" +
            "<li><b>Erosion: </b>A local minimum filter</li>" +
            "<li><b>Dilation: </b>A local maximum filter</li>" +
            "<li><b>Opening: </b>Erosion, followed by dilation</li>" +
            "<li><b>Closing: </b>Dilation, followed by erosion</li>" +
            "<li><b>White Top Hat: </b>Image - Opening</li>" +
            "<li><b>Black Top Hat (Black Hat): </b>Closing - Image</li>" +
            "<li><b>Gradient: </b>Dilation - Erosion</li>" +
            "<li><b>Laplacian: </b>(Dilation + Erosion) / 2 - Image</li>" +
            "<li><b>Internal gradient: </b>Image - Erosion</li>" +
            "<li><b>External gradient: </b>Dilation - Image</li>" +
            "</ul>")
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
    public Strel3D.Shape getElement() {
        return element;
    }

    @JIPipeParameter("element")
    public void setElement(Strel3D.Shape element) {
        this.element = element;
    }
}
