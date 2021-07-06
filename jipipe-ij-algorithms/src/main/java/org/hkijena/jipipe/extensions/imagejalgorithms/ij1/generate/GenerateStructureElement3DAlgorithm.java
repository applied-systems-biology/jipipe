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

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.Strel3D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Generate structure element", description = "Generates a structure element that is consistent with the ones used by the 3D morphological operation.")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Kernel")
public class GenerateStructureElement3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Strel3D.Shape element = Strel3D.Shape.BALL;
    private int radius = 1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GenerateStructureElement3DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addOutputSlot("Kernel", ImagePlus2DGreyscaleMaskData.class, null).seal().build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GenerateStructureElement3DAlgorithm(GenerateStructureElement3DAlgorithm other) {
        super(other);
        this.radius = other.radius;
        this.element = other.element;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        // Size of the strel image (little bit larger than strel)
        Strel3D strel = element.fromRadius(radius);
        int[] dim = strel.getSize();
        int width = dim[0] + 20;
        int height = dim[1] + 20;
        int depth = dim[2] + 20;

        // Creates strel image by dilating a point
        ImageStack strelImageStack = new ImageStack(width, height, depth);
        ImageProcessor strelImage = new ByteProcessor(width, height);
        strelImage.set(width / 2, height / 2, 255);

        for (int i = 0; i < depth; i++) {
            strelImageStack.setProcessor(new ByteProcessor(width, height), i + 1);
        }
        strelImageStack.setProcessor(strelImage, depth / 2);
        strelImageStack = Morphology.dilation(strelImageStack, strel);

        // Display strel image
        ImagePlus strelDisplay = new ImagePlus("Structuring Element", strelImageStack);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscaleMaskData(strelDisplay), progressInfo);
    }


    @Override
    public void reportValidity(JIPipeIssueReport report) {
        report.resolve("Radius").checkIfWithin(this, radius, 0, Double.POSITIVE_INFINITY, false, true);
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

    @JIPipeDocumentation(name = "Kernel", description = "The filter kernel.")
    @JIPipeParameter("element")
    public Strel3D.Shape getElement() {
        return element;
    }

    @JIPipeParameter("element")
    public void setElement(Strel3D.Shape element) {
        this.element = element;
    }
}
