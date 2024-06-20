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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.segment;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import inra.ijpb.watershed.Watershed;
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
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood2D3D;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Seeded watershed", description = "Performs segmentation via watershed on a 2D or 3D image using flooding simulations. Please note that this node returns labels instead of masks. " +
        "The markers need to be labels, so apply a connected components labeling if you only have masks.")
@AddJIPipeCitation("\"Determining watersheds in digital pictures via flooding simulations.\" Lausanne-DL tentative. International Society for Optics and Photonics, 1990")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Segment")
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, name = "Image", create = true)
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, name = "Markers", create = true)
@AddJIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, name = "Labels", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nSegmentation", aliasName = "Marker-controlled Watershed")
public class SeededWatershedSegmentationAlgorithm extends JIPipeIteratingAlgorithm {

    private Neighborhood2D3D connectivity = Neighborhood2D3D.NoDiagonals;
    private ImageROITargetArea targetArea = ImageROITargetArea.WholeImage;
    private boolean applyPerSlice = false;
    private boolean getDams = false;

    public SeededWatershedSegmentationAlgorithm(JIPipeNodeInfo info) {
        super(info);
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    public SeededWatershedSegmentationAlgorithm(SeededWatershedSegmentationAlgorithm other) {
        super(other);
        this.connectivity = other.connectivity;
        this.getDams = other.getDams;
        this.targetArea = other.targetArea;
        this.applyPerSlice = other.applyPerSlice;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData("Image", ImagePlus3DGreyscaleData.class, progressInfo).getImage();
        ImagePlus seedImage = iterationStep.getInputData("Markers", ImagePlus3DGreyscaleData.class, progressInfo).getImage();
        if (applyPerSlice) {
            ImagePlus resultImage = ImageJUtils.generateForEachIndexedZCTSlice(inputImage, (ip, index) -> {
                ImageProcessor mask = ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(targetArea, iterationStep, index, progressInfo);
                ImageProcessor seeds = ImageJUtils.getSliceZero(seedImage, index);
                return Watershed.computeWatershed(new ImagePlus("raw", ip), new ImagePlus("marker", seeds), new ImagePlus("mask", mask), connectivity.getNativeValue2D(), getDams, false).getProcessor();
            }, progressInfo);
            resultImage.copyScale(inputImage);
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscaleData(resultImage), progressInfo);
        } else {
            ImagePlus mask = ImageJAlgorithmUtils.getMaskFromMaskOrROI(targetArea, iterationStep, "Image", progressInfo);
            ImagePlus resultImage = Watershed.computeWatershed(inputImage, seedImage, mask, inputImage.getStackSize() == 1 ? connectivity.getNativeValue2D() : connectivity.getNativeValue3D(), getDams, false);
            resultImage.copyScale(inputImage);
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscaleData(resultImage), progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Get dams", description = "If enabled, the dams instead of the wells are returned")
    @JIPipeParameter("get-dams")
    public boolean isGetDams() {
        return getDams;
    }

    @JIPipeParameter("get-dams")
    public void setGetDams(boolean getDams) {
        this.getDams = getDams;
    }

    @SetJIPipeDocumentation(name = "Connectivity", description = "Determines the pixel neighborhood for the flood fill algorithm.")
    @JIPipeParameter("connectivity")
    public Neighborhood2D3D getConnectivity() {
        return connectivity;
    }

    @JIPipeParameter("connectivity")
    public void setConnectivity(Neighborhood2D3D connectivity) {
        this.connectivity = connectivity;
    }

    @SetJIPipeDocumentation(name = "Only apply to ...", description = "Determines where the watershed is applied.")
    @JIPipeParameter("target-area")
    public ImageROITargetArea getTargetArea() {
        return targetArea;
    }

    @JIPipeParameter("target-area")
    public void setTargetArea(ImageROITargetArea targetArea) {
        this.targetArea = targetArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    @SetJIPipeDocumentation(name = "Apply per slice", description = "If enabled, 3D data is split into 2D slices and the watershed algorithm is applied per slice.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }
}
