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
import inra.ijpb.plugins.ExpandLabelsPlugin;
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

@JIPipeDocumentation(name = "Expand labels", description = "Adds space between labels to make them more easily distinguishable")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class ExpandLabelsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private float ratio = 20;

    public ExpandLabelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExpandLabelsAlgorithm(ExpandLabelsAlgorithm other) {
        super(other);
        this.ratio = other.ratio;
    }

    @JIPipeDocumentation(name = "Coefficient (%)", description = "Determines by how much % the label centers are shifted")
    @JIPipeParameter("ratio")
    public float getRatio() {
        return ratio;
    }

    @JIPipeParameter("ratio")
    public void setRatio(float ratio) {
        this.ratio = ratio;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus outputImage;
        if (inputImage.getStackSize() == 1) {
            outputImage = new ImagePlus("Boundaries", ExpandLabelsPlugin.expandLabels(inputImage.getProcessor(), ratio));
        } else {
            outputImage = new ImagePlus("Boundaries", ExpandLabelsPlugin.expandLabels(inputImage.getStack(), ratio));
        }
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        outputImage.copyScale(inputImage);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }
}
