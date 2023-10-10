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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.binary;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.Neighborhood2D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;

@JIPipeDocumentation(name = "Connected components labeling 2D", description = "Applies a connected components labeling on a mask. Each connected component is assigned a unique value in the output label image. " +
        "If the image is non-binary, the connected components are generated per-input value. If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
@JIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nBinary Images", aliasName = "Connected Components Labeling (2D)")
public class ConnectedComponentsLabeling2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Neighborhood2D connectivity = Neighborhood2D.EightConnected;
    private JIPipeDataInfoRef outputType = new JIPipeDataInfoRef(ImagePlusGreyscale16UData.class);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ConnectedComponentsLabeling2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        updateSlots();
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ConnectedComponentsLabeling2DAlgorithm(ConnectedComponentsLabeling2DAlgorithm other) {
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
        emitNodeSlotsChangedEvent();
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(inputImage.getWidth(), inputImage.getHeight(), inputImage.getStackSize());

        int bitDepth;
        if (outputType.getInfo().getDataClass() == ImagePlusGreyscale32FData.class)
            bitDepth = 32;
        else if (outputType.getInfo().getDataClass() == ImagePlusGreyscale16UData.class)
            bitDepth = 16;
        else
            bitDepth = 8;

        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            ImageProcessor processor = BinaryImages.componentsLabeling(ip, connectivity.getNativeValue(), bitDepth);
            stack.setProcessor(processor, index.zeroSliceIndexToOneStackIndex(inputImage));
        }, progressInfo);
        ImagePlus outputImage = new ImagePlus("Connected components", stack);
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        outputImage.copyScale(inputImage);
        dataBatch.addOutputData(getFirstOutputSlot(), JIPipe.createData(outputType.getInfo().getDataClass(), outputImage), progressInfo);
    }
}
