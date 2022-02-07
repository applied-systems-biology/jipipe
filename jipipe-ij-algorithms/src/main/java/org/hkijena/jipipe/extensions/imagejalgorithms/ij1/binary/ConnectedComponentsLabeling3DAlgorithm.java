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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.binary;

import ij.ImagePlus;
import inra.ijpb.binary.BinaryImages;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood2D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.parameters.library.editors.JIPipeDataParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;

@JIPipeDocumentation(name = "Connected components labeling 3D", description = "Applies a connected components labeling on a mask. Each connected component is assigned a unique value in the output label image. " +
        "If the image is non-binary, the connected components are generated per-input value. If 3D data is supplied, the connected components are extracted in 3D.")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class ConnectedComponentsLabeling3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Neighborhood2D connectivity = Neighborhood2D.EightConnected;
    private JIPipeDataInfoRef outputType = new JIPipeDataInfoRef(ImagePlusGreyscale16UData.class);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ConnectedComponentsLabeling3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        updateSlots();
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ConnectedComponentsLabeling3DAlgorithm(ConnectedComponentsLabeling3DAlgorithm other) {
        super(other);
        this.connectivity = other.connectivity;
        this.outputType = new JIPipeDataInfoRef(other.outputType);
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

    @JIPipeDocumentation(name = "Output type", description = "Determines the output label type. You can choose between an 8-bit, 16-bit, and 32-bit label.")
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
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo).getImage();

        int bitDepth;
        if (outputType.getInfo().getDataClass() == ImagePlusGreyscale32FData.class)
            bitDepth = 32;
        else if (outputType.getInfo().getDataClass() == ImagePlusGreyscale16UData.class)
            bitDepth = 16;
        else
            bitDepth = 8;

        ImagePlus outputImage = BinaryImages.componentsLabeling(inputImage, connectivity.getNativeValue(), bitDepth);
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        dataBatch.addOutputData(getFirstOutputSlot(), JIPipe.createData(outputType.getInfo().getDataClass(), outputImage), progressInfo);
    }
}
