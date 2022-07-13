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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math.local;

import ij.ImagePlus;
import ij.ImageStack;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood3D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

@JIPipeDocumentation(name = "Extended minima 3D", description = "Returns the extended minima.")
@JIPipeNode(menuPath = "Math\nLocal", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
@JIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nMinima and Maxima", aliasName = "Extended Min (3D)")
public class ExtendedMinima3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Neighborhood3D connectivity = Neighborhood3D.SixConnected;
    private double dynamic = 10;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ExtendedMinima3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ExtendedMinima3DAlgorithm(ExtendedMinima3DAlgorithm other) {
        super(other);
        this.connectivity = other.connectivity;
        this.dynamic = other.dynamic;
    }

    @JIPipeDocumentation(name = "Connectivity", description = "Determines the neighborhood around each pixel that is checked for connectivity")
    @JIPipeParameter("connectivity")
    public Neighborhood3D getConnectivity() {
        return connectivity;
    }

    @JIPipeParameter("connectivity")
    public void setConnectivity(Neighborhood3D connectivity) {
        this.connectivity = connectivity;
    }

    @JIPipeDocumentation(name = "Dynamic", description = "The minimal difference between a maxima and its boundary")
    @JIPipeParameter("dynamic")
    public double getDynamic() {
        return dynamic;
    }

    @JIPipeParameter("dynamic")
    public void setDynamic(double dynamic) {
        this.dynamic = dynamic;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImageStack resultStack = MinimaAndMaxima3D.extendedMinima(inputImage.getStack(), dynamic, connectivity.getNativeValue());
        ImagePlus outputImage = new ImagePlus("Regional maxima", resultStack);
        outputImage.copyScale(inputImage);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(outputImage), progressInfo);
    }
}
