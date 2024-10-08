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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.generate;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Generate structure element", description = "Generates a structure element that is consistent with the ones used by the 2D morphological operation.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class, dataSourceMenuLocation = ImagePlusData.class)
@AddJIPipeOutputSlot(value = ImagePlus2DGreyscaleMaskData.class, name = "Kernel", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
public class GenerateStructureElement2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Strel.Shape element = Strel.Shape.DISK;
    private int radius = 1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GenerateStructureElement2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GenerateStructureElement2DAlgorithm(GenerateStructureElement2DAlgorithm other) {
        super(other);
        this.radius = other.radius;
        this.element = other.element;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // Size of the strel image (little bit larger than strel)
        Strel strel = element.fromRadius(radius);
        int[] dim = strel.getSize();
        int width = dim[0] + 20;
        int height = dim[1] + 20;

        // Creates strel image by dilating a point
        ImageProcessor strelImage = new ByteProcessor(width, height);
        strelImage.set(width / 2, height / 2, 255);
        strelImage = Morphology.dilation(strelImage, strel);

        // Display strel image
        ImagePlus strelDisplay = new ImagePlus("Structuring Element", strelImage);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscaleMaskData(strelDisplay), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Radius", description = "Radius of the filter kernel in pixels.")
    @JIPipeParameter("radius")
    public int getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(int radius) {
        this.radius = radius;
    }

    @SetJIPipeDocumentation(name = "Kernel", description = "The filter kernel.")
    @JIPipeParameter("element")
    public Strel.Shape getElement() {
        return element;
    }

    @JIPipeParameter("element")
    public void setElement(Strel.Shape element) {
        this.element = element;
    }
}
