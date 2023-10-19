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

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Generate structure element (3D)", description = "Generates a structure element that is consistent with the ones used by the 3D morphological operation.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class, dataSourceMenuLocation = ImagePlusData.class)
@JIPipeOutputSlot(value = ImagePlus3DGreyscaleMaskData.class, slotName = "Kernel", autoCreate = true)
@JIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
public class GenerateStructureElement3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Strel3D.Shape element = Strel3D.Shape.BALL;
    private int radius = 1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GenerateStructureElement3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
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
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscaleMaskData(strelDisplay), progressInfo);
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
