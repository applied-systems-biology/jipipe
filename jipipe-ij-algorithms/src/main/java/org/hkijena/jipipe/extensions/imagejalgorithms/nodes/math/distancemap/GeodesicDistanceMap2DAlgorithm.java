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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.math.distancemap;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.binary.ChamferWeights;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.binary.Image_8_16_32_Filter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;

@SetJIPipeDocumentation(name = "Geodesic Distance Map 2D", description = "Computes the geodesic distance map from a binary image. If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@DefineJIPipeNode(menuPath = "Math\nDistance map", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Marker", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nBinary Images", aliasName = "Geodesic Distance Map (2D)")
public class GeodesicDistanceMap2DAlgorithm extends JIPipeIteratingAlgorithm {

    private ChamferWeights chamferWeights = ChamferWeights.BORGEFORS;
    private boolean normalize = true;
    private JIPipeDataInfoRef outputType = new JIPipeDataInfoRef(ImagePlusGreyscale32FData.class);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GeodesicDistanceMap2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        updateSlots();
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GeodesicDistanceMap2DAlgorithm(GeodesicDistanceMap2DAlgorithm other) {
        super(other);
        this.chamferWeights = other.chamferWeights;
        this.normalize = other.normalize;
        this.outputType = new JIPipeDataInfoRef(other.outputType);
    }

    @SetJIPipeDocumentation(name = "Chamfer weights", description = "Determines the Chamfer weights for this distance transform")
    @JIPipeParameter("chamfer-weights")
    public ChamferWeights getChamferWeights() {
        return chamferWeights;
    }

    @JIPipeParameter("chamfer-weights")
    public void setChamferWeights(ChamferWeights chamferWeights) {
        this.chamferWeights = chamferWeights;
    }

    @SetJIPipeDocumentation(name = "Normalize weights", description = "Indicates whether the resulting distance map should be normalized (divide distances by the first chamfer weight)")
    @JIPipeParameter("normalize")
    public boolean isNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    @SetJIPipeDocumentation(name = "Output type", description = "Determines the output type. You can choose between an  16-bit or 32-bit.")
    @JIPipeParameter("output-type")
    @JIPipeDataParameterSettings(dataClassFilter = Image_8_16_32_Filter.class)
    public JIPipeDataInfoRef getOutputType() {
        return outputType;
    }

    @JIPipeParameter("output-type")
    public void setOutputType(JIPipeDataInfoRef outputType) {
        this.outputType = outputType;
        updateSlots();
    }

    private void updateSlots() {
        getFirstOutputSlot().setAcceptedDataType(outputType.getInfo().getDataClass());
        emitNodeSlotsChangedEvent();
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus markerImage = iterationStep.getInputData(getInputSlot("Marker"), ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
        ImagePlus maskImage = iterationStep.getInputData(getInputSlot("Mask"), ImagePlusGreyscaleMaskData.class, progressInfo).getImage();

        ImageStack stack = new ImageStack(markerImage.getWidth(), markerImage.getHeight(), markerImage.getStackSize());

        int bitDepth;
        if (outputType.getInfo().getDataClass() == ImagePlusGreyscale32FData.class)
            bitDepth = 32;
        else
            bitDepth = 16;

        ImageJUtils.forEachIndexedZCTSlice(markerImage, (ipMarker, index) -> {
            ImageProcessor ipMask = ImageJUtils.getSliceZero(maskImage, index);
            ImageProcessor processor;

            if (bitDepth == 16)
                processor = BinaryImages.geodesicDistanceMap(ipMarker, ipMask, chamferWeights.getShortWeights(), normalize);
            else
                processor = BinaryImages.geodesicDistanceMap(ipMarker, ipMask, chamferWeights.getFloatWeights(), normalize);

            stack.setProcessor(processor, index.zeroSliceIndexToOneStackIndex(markerImage));
        }, progressInfo);
        ImagePlus outputImage = new ImagePlus("GDM", stack);
        outputImage.setDimensions(markerImage.getNChannels(), markerImage.getNSlices(), markerImage.getNFrames());
        outputImage.copyScale(markerImage);
        iterationStep.addOutputData(getFirstOutputSlot(), JIPipe.createData(outputType.getInfo().getDataClass(), outputImage), progressInfo);
    }
}
