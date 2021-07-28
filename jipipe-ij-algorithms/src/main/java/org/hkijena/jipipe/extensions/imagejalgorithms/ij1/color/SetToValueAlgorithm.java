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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.color;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.SimpleImageAndRoiIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.awt.Color;
import java.util.Collections;

@JIPipeDocumentation(name = "Set to color (grayscale)", description = "Sets all pixels of the input image to the specified grayscale value. If the image is RGB, the value is converted into an integer that is parsed as RGB.")
@JIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class SetToValueAlgorithm extends SimpleImageAndRoiIteratingAlgorithm {

    private double value = 0;

    public SetToValueAlgorithm(JIPipeNodeInfo info) {
        super(info, ImagePlusData.class, ImagePlusData.class, null, Collections.emptyMap());
    }

    public SetToValueAlgorithm(SetToValueAlgorithm other) {
        super(other);
        this.value = other.value;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus image = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        image = ImageJUtils.channelsToRGB(image);
        if (image.getType() == ImagePlus.COLOR_RGB) {
            Color color = new Color((int) value);
            ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                ImageProcessor roi = getMask(dataBatch, index, progressInfo);
                ColorProcessor colorProcessor = (ColorProcessor) ip;
                ip.resetRoi();
                colorProcessor.setColor(color);
                ip.fill(roi);
            }, progressInfo);
        } else {
            ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                ImageProcessor roi = getMask(dataBatch, index, progressInfo);
                ip.resetRoi();
                ip.setValue(value);
                ip.fill(roi);
            }, progressInfo);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), progressInfo);
    }

    @JIPipeDocumentation(name = "Value", description = "The value of each pixel.")
    @JIPipeParameter("value")
    public double getValue() {
        return value;
    }

    @JIPipeParameter("value")
    public void setValue(double value) {
        this.value = value;
    }
}
