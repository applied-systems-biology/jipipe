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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.masking;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.SimpleImageAndRoiIteratingAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

import java.awt.*;

@SetJIPipeDocumentation(name = "Set to value (grayscale)", description = "Sets all pixels of the input image to the specified grayscale value. If the image is RGB, the value is converted into an integer that is parsed as RGB.")
@ConfigureJIPipeNode(menuPath = "Masking", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Edit", aliasName = "Fill (whole image, greyscale)")
public class SetToValueAlgorithm extends SimpleImageAndRoiIteratingAlgorithm {

    private double value = 0;

    public SetToValueAlgorithm(JIPipeNodeInfo info) {
        super(info, ImagePlusData.class, ImagePlusData.class);
    }

    public SetToValueAlgorithm(SetToValueAlgorithm other) {
        super(other);
        this.value = other.value;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo).getDuplicateImage();
        image = ImageJUtils.channelsToRGB(image);
        if (image.getType() == ImagePlus.COLOR_RGB) {
            Color color = new Color((int) value);
            ImageJIterationUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                ImageProcessor roi = getMask(iterationStep, index, progressInfo);
                ColorProcessor colorProcessor = (ColorProcessor) ip;
                ip.resetRoi();
                colorProcessor.setColor(color);
                ip.fill(roi);
            }, progressInfo);
        } else {
            ImageJIterationUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                ImageProcessor roi = getMask(iterationStep, index, progressInfo);
                ip.resetRoi();
                ip.setValue(value);
                ip.fill(roi);
            }, progressInfo);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Value", description = "The value of each pixel.")
    @JIPipeParameter("value")
    public double getValue() {
        return value;
    }

    @JIPipeParameter("value")
    public void setValue(double value) {
        this.value = value;
    }
}
