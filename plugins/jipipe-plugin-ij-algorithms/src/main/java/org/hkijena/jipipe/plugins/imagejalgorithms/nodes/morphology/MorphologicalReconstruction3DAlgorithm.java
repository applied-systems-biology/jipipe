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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.morphology;

import ij.ImagePlus;
import ij.ImageStack;
import inra.ijpb.morphology.Reconstruction3D;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood2D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.BooleanParameterSettings;

@SetJIPipeDocumentation(name = "Morphological reconstruction 3D", description = "Geodesic reconstruction repeats conditional dilations or erosions until idempotence. " +
        "Two images are required: the marker image, used to initialize the reconstruction, an the mask image, used to constrain the reconstruction. " +
        "More information: https://imagej.net/plugins/morpholibj")
@AddJIPipeCitation("https://imagej.net/plugins/morpholibj")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Morphology")
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, name = "Marker", create = true)
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleMaskData.class, name = "Mask", create = true)
@AddJIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, name = "Output", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nFiltering", aliasName = "Morphological Reconstruction 3D")
public class MorphologicalReconstruction3DAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean applyDilation = true;
    private Neighborhood2D connectivity = Neighborhood2D.FourConnected;

    public MorphologicalReconstruction3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MorphologicalReconstruction3DAlgorithm(MorphologicalReconstruction3DAlgorithm other) {
        super(other);
        this.applyDilation = other.applyDilation;
        this.connectivity = other.connectivity;
    }

    @SetJIPipeDocumentation(name = "Type of reconstruction", description = "Determines which type of reconstruction is applied")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "By dilation", falseLabel = "By erosion")
    @JIPipeParameter("apply-dilation")
    public boolean isApplyDilation() {
        return applyDilation;
    }

    @JIPipeParameter("apply-dilation")
    public void setApplyDilation(boolean applyDilation) {
        this.applyDilation = applyDilation;
    }

    @SetJIPipeDocumentation(name = "Connectivity", description = "Determines the neighborhood around each pixel that is checked for connectivity")
    @JIPipeParameter("connectivity")
    public Neighborhood2D getConnectivity() {
        return connectivity;
    }

    @JIPipeParameter("connectivity")
    public void setConnectivity(Neighborhood2D connectivity) {
        this.connectivity = connectivity;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus markerImage = iterationStep.getInputData("Marker", ImagePlus3DGreyscaleData.class, progressInfo).getImage();
        ImagePlus maskImage = iterationStep.getInputData("Mask", ImagePlus3DGreyscaleMaskData.class, progressInfo).getImage();
        maskImage = ImageJUtils.ensureEqualSize(maskImage, markerImage, true);

        ImageStack resultImage;
        if (applyDilation)
            resultImage = Reconstruction3D.reconstructByDilation(markerImage.getImageStack(), maskImage.getImageStack(), connectivity.getNativeValue());
        else
            resultImage = Reconstruction3D.reconstructByErosion(markerImage.getImageStack(), maskImage.getImageStack(), connectivity.getNativeValue());

        ImagePlus outputImage = new ImagePlus("Reconstructed", resultImage);
        outputImage.copyScale(markerImage);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscaleData(outputImage), progressInfo);
    }
}
