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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math.distancemap;

import ij.ImagePlus;
import ij.ImageStack;
import inra.ijpb.binary.ChamferWeights3D;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.binary.Image_8_16_32_Filter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;

@JIPipeDocumentation(name = "Chamfer Distance Map 3D (labels)", description = "Computes the distance map from a label image. If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Math\nDistance map", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class LabelChamferDistanceMap3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ChamferWeights3D chamferWeights = ChamferWeights3D.WEIGHTS_3_4_5_7;
    private boolean normalize = true;
    private JIPipeDataInfoRef outputType = new JIPipeDataInfoRef(ImagePlusGreyscale32FData.class);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public LabelChamferDistanceMap3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        updateSlots();
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public LabelChamferDistanceMap3DAlgorithm(LabelChamferDistanceMap3DAlgorithm other) {
        super(other);
        this.chamferWeights = other.chamferWeights;
        this.normalize = other.normalize;
        this.outputType = new JIPipeDataInfoRef(other.outputType);
    }

    @JIPipeDocumentation(name = "Chamfer weights", description = "Determines the Chamfer weights for this distance transform")
    @JIPipeParameter("chamfer-weights")
    public ChamferWeights3D getChamferWeights() {
        return chamferWeights;
    }

    @JIPipeParameter("chamfer-weights")
    public void setChamferWeights(ChamferWeights3D chamferWeights) {
        this.chamferWeights = chamferWeights;
    }

    @JIPipeDocumentation(name = "Normalize weights", description = "Indicates whether the resulting distance map should be normalized (divide distances by the first chamfer weight)")
    @JIPipeParameter("normalize")
    public boolean isNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    @JIPipeDocumentation(name = "Output type", description = "Determines the output type. You can choose between an  16-bit or 32-bit.")
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
        getEventBus().post(new JIPipeGraph.NodeSlotsChangedEvent(this));
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlus3DGreyscaleData.class, progressInfo).getImage();

        int bitDepth;
        if (outputType.getInfo().getDataClass() == ImagePlusGreyscale32FData.class)
            bitDepth = 32;
        else
            bitDepth = 16;

        ImageStack outputImage;

        if (bitDepth == 16)
            outputImage = LabelImages.distanceMap(inputImage.getStack(), chamferWeights.getShortWeights(), normalize);
        else
            outputImage = LabelImages.distanceMap(inputImage.getStack(), chamferWeights.getFloatWeights(), normalize);

        ImagePlus outputImagePlus = new ImagePlus("CDM", outputImage);
        outputImagePlus.copyScale(inputImage);
        dataBatch.addOutputData(getFirstOutputSlot(), JIPipe.createData(outputType.getInfo().getDataClass(), outputImagePlus), progressInfo);
    }
}
