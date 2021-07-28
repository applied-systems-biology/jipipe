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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.labels;

import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

@JIPipeDocumentation(name = "Dilate labels", description = "Applies a constrained dilation of labels without unwanted growing/shrinking bordering regions (dilation only over the background)")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class DilateLabelsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double maxRadius = 5;

    public DilateLabelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DilateLabelsAlgorithm(DilateLabelsAlgorithm other) {
        super(other);
        this.maxRadius = other.maxRadius;
    }

    @JIPipeDocumentation(name = "Maximum radius", description = "The maximum radius of the dilation")
    @JIPipeParameter("max-radius")
    public double getMaxRadius() {
        return maxRadius;
    }

    @JIPipeParameter("max-radius")
    public void setMaxRadius(double maxRadius) {
        this.maxRadius = maxRadius;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus outputImage;
        if (inputImage.getStackSize() == 1) {
            outputImage = new ImagePlus("Dilated", LabelImages.dilateLabels(inputImage.getProcessor(), maxRadius));
        } else {
            outputImage = new ImagePlus("Dilated", LabelImages.dilateLabels(inputImage.getStack(), maxRadius));
        }
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        outputImage.copyScale(inputImage);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }
}
