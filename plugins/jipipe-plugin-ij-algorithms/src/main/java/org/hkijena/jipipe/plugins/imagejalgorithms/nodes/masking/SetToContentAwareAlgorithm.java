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
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ContentAwareFill;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.SimpleImageAndRoiIteratingAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;

@SetJIPipeDocumentation(name = "Set to content aware 2D", description = "Sets pixels of the input image inside the masked area to a generated value. Please review the 'Methods' parameter to find out more about the methods.")
@ConfigureJIPipeNode(menuPath = "Masking", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Edit", aliasName = "Fill (whole image, greyscale)")
public class SetToContentAwareAlgorithm extends SimpleImageAndRoiIteratingAlgorithm {

    private Method method = Method.ClosestPixel;

    public SetToContentAwareAlgorithm(JIPipeNodeInfo info) {
        super(info, ImagePlusData.class, ImagePlusData.class);
        setTargetArea(ImageROITargetArea.InsideMask);
    }

    public SetToContentAwareAlgorithm(SetToContentAwareAlgorithm other) {
        super(other);
        this.method = other.method;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo).getDuplicateImage();
        ImageJIterationUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
            ImageProcessor mask = getMask(iterationStep, index, progressInfo);
            switch (method) {
                case ClosestPixel:
                    ContentAwareFill.fillClosestPixel(ip, mask);
                    break;
                case MeanValue:
                    ContentAwareFill.fillMean(ip, mask);
                    break;
                case MedianValue:
                    ContentAwareFill.fillMedian(ip, mask);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported method: " + method);
            }
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Method", description = "The fill method. Currently available are: " +
            "<ul>" +
            "<li>Closest pixel: finds the closest pixel to the each masked pixel and uses that value.</li>" +
            "<li>Mean pixel value: finds the mean pixel value outside the mask (per channel for color images)</li>" +
            "<li>Median pixel value: finds the median pixel value outside the mask (per channel for color images)</li>" +
            "</ul>")
    @JIPipeParameter("method")
    public Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(Method method) {
        this.method = method;
    }

    public enum Method {
        ClosestPixel("Closest pixel"),
        MeanValue("Mean pixel value"),
        MedianValue("Median pixel value");

        private final String label;

        Method(String label) {
            this.label = label;
        }


        @Override
        public String toString() {
            return label;
        }
    }
}
