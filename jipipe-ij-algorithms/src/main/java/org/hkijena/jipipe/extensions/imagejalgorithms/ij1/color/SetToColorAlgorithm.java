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

import java.awt.*;
import java.util.Collections;

@JIPipeDocumentation(name = "Set to color", description = "Sets all pixels of the input image to the specified color. If the image is grayscale, the provided color is converted to its equivalent grayscale value.")
@JIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class SetToColorAlgorithm extends SimpleImageAndRoiIteratingAlgorithm {

    private Color color = Color.BLACK;

    public SetToColorAlgorithm(JIPipeNodeInfo info) {
        super(info, ImagePlusData.class, ImagePlusData.class, null, Collections.emptyMap());
    }

    public SetToColorAlgorithm(SetToColorAlgorithm other) {
        super(other);
        this.color = other.color;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus image = dataBatch.getInputData("Input", ImagePlusData.class, progressInfo).getDuplicateImage();
        image = ImageJUtils.channelsToRGB(image);
        if (image.getType() == ImagePlus.COLOR_RGB) {
            ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                ImageProcessor roi = getMask(dataBatch, index, progressInfo);
                ColorProcessor colorProcessor = (ColorProcessor) ip;
                ip.resetRoi();
                ip.setMask(roi);
                colorProcessor.setColor(color);
                ip.fill();
                ip.resetRoi();
            }, progressInfo);
        } else {
            double value = (color.getRed() + color.getGreen() + color.getBlue()) / 3.0;
            ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                ImageProcessor roi = getMask(dataBatch, index, progressInfo);
                ip.resetRoi();
                ip.setMask(roi);
                ip.setValue(value);
                ip.fill();
                ip.resetRoi();
            }, progressInfo);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), progressInfo);
    }

    @JIPipeDocumentation(name = "Color", description = "The color of each pixel.")
    @JIPipeParameter("color")
    public Color getColor() {
        return color;
    }

    @JIPipeParameter("color")
    public void setColor(Color color) {
        this.color = color;
    }
}