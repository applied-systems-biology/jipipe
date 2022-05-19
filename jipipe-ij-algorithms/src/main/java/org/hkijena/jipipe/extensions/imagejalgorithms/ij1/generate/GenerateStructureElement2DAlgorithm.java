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
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
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

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Generate structure element", description = "Generates a structure element that is consistent with the ones used by the 2D morphological operation.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Kernel")
@JIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
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
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addOutputSlot("Kernel", "", ImagePlus2DGreyscaleMaskData.class, null).seal().build());
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
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
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscaleMaskData(strelDisplay), progressInfo);
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
    public Strel.Shape getElement() {
        return element;
    }

    @JIPipeParameter("element")
    public void setElement(Strel.Shape element) {
        this.element = element;
    }
}
