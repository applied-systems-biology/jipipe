package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Reconstruction;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood2D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.primitives.BooleanParameterSettings;

@JIPipeDocumentation(name = "Morphological reconstruction 2D", description = "Geodesic reconstruction repeats conditional dilations or erosions until idempotence. " +
        "Two images are required: the marker image, used to initialize the reconstruction, an the mask image, used to constrain the reconstruction. " +
        "More information: https://imagej.net/plugins/morpholibj")
@JIPipeCitation("https://imagej.net/plugins/morpholibj")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Morphology")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Marker", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class MorphologicalReconstruction2DAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean applyDilation = true;
    private Neighborhood2D connectivity = Neighborhood2D.FourConnected;

    public MorphologicalReconstruction2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MorphologicalReconstruction2DAlgorithm(MorphologicalReconstruction2DAlgorithm other) {
        super(other);
        this.applyDilation = other.applyDilation;
        this.connectivity = other.connectivity;
    }

    @JIPipeDocumentation(name = "Type of reconstruction", description = "Determines which type of reconstruction is applied")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "By dilation", falseLabel = "By erosion")
    @JIPipeParameter("apply-dilation")
    public boolean isApplyDilation() {
        return applyDilation;
    }

    @JIPipeParameter("apply-dilation")
    public void setApplyDilation(boolean applyDilation) {
        this.applyDilation = applyDilation;
    }

    @JIPipeDocumentation(name = "Connectivity", description = "Determines the neighborhood around each pixel that is checked for connectivity")
    @JIPipeParameter("connectivity")
    public Neighborhood2D getConnectivity() {
        return connectivity;
    }

    @JIPipeParameter("connectivity")
    public void setConnectivity(Neighborhood2D connectivity) {
        this.connectivity = connectivity;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus markerImage = dataBatch.getInputData("Marker", ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus maskImage = dataBatch.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
        maskImage = ImageJUtils.ensureEqualSize(maskImage, markerImage, true);

        ImagePlus finalMaskImage = maskImage;
        ImagePlus resultImage = ImageJUtils.generateForEachIndexedZCTSlice(markerImage, (markerProcessor, index) -> {
            ImageProcessor maskProcessor = ImageJUtils.getSliceZero(finalMaskImage, index);
            if (applyDilation)
                return Reconstruction.reconstructByDilation(markerProcessor, maskProcessor, connectivity.getNativeValue());
            else
                return Reconstruction.reconstructByErosion(markerProcessor, maskProcessor, connectivity.getNativeValue());
        }, progressInfo);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(resultImage), progressInfo);
    }
}