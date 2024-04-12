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

package org.hkijena.jipipe.plugins.opencv.nodes.filters;

import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.opencv.datatypes.OpenCvImageData;
import org.hkijena.jipipe.plugins.opencv.utils.OpenCvBorderType;
import org.hkijena.jipipe.plugins.opencv.utils.OpenCvImageUtils;

@SetJIPipeDocumentation(name = "Bilateral Filter (OpenCV)", description = "Applies the bilateral filter to an image.\n" +
        "\n" +
        "The function applies bilateral filtering to the input image, as described in http://www.dai.ed.ac.uk/CVonline/LOCAL_COPIES/MANDUCHI1/Bilateral_Filtering.html bilateralFilter can reduce unwanted noise very well while keeping edges fairly sharp. However, it is very slow compared to most filters.\n" +
        "\n" +
        "Sigma values: For simplicity, you can set the 2 sigma values to be the same. If they are small (< 10), the filter will not have much effect, whereas if they are large (> 150), they will have a very strong effect, making the image look \"cartoonish\".\n" +
        "\n" +
        "Filter size: Large filters (d > 5) are very slow, so it is recommended to use d=5 for real-time applications, and perhaps d=9 for offline applications that need heavy noise filtering.")
@ConfigureJIPipeNode(menuPath = "Blur", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = OpenCvImageData.class, slotName = "Input", create = true, description = "Source 8-bit or floating-point, 1-channel or 3-channel image.")
@AddJIPipeOutputSlot(value = OpenCvImageData.class, slotName = "Output", create = true)
public class BilateralFilterAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double sigmaColor = 75;
    private double sigmaSpace = 75;
    private OpenCvBorderType borderType = OpenCvBorderType.Constant;
    private int diameter = 15;

    public BilateralFilterAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public BilateralFilterAlgorithm(BilateralFilterAlgorithm other) {
        super(other);
        this.sigmaColor = other.sigmaColor;
        this.sigmaSpace = other.sigmaSpace;
        this.borderType = other.borderType;
        this.diameter = other.diameter;
    }

    @SetJIPipeDocumentation(name = "Sigma (Color)", description = "Filter sigma in the color space. A larger value of the parameter means that farther colors within the pixel " +
            "neighborhood (see sigmaSpace) will be mixed together, resulting in larger areas of semi-equal color. ")
    @JIPipeParameter("sigma-color")
    public double getSigmaColor() {
        return sigmaColor;
    }

    @JIPipeParameter("sigma-color")
    public void setSigmaColor(double sigmaColor) {
        this.sigmaColor = sigmaColor;
    }

    @SetJIPipeDocumentation(name = "Sigma (Space)", description = "Filter sigma in the coordinate space. A larger value of the parameter means that farther pixels will influence each other as long as their colors are close enough (see sigmaColor ). " +
            "When d>0, it specifies the neighborhood size regardless of sigmaSpace. Otherwise, d is proportional to sigmaSpace. ")
    @JIPipeParameter("sigma-space")
    public double getSigmaSpace() {
        return sigmaSpace;
    }

    @JIPipeParameter("sigma-space")
    public void setSigmaSpace(double sigmaSpace) {
        this.sigmaSpace = sigmaSpace;
    }

    @SetJIPipeDocumentation(name = "Border type", description = "Border mode used to extrapolate pixels outside of the image")
    @JIPipeParameter("border-type")
    public OpenCvBorderType getBorderType() {
        return borderType;
    }

    @JIPipeParameter("border-type")
    public void setBorderType(OpenCvBorderType borderType) {
        this.borderType = borderType;
    }

    @SetJIPipeDocumentation(name = "Diameter", description = "Diameter of each pixel neighborhood that is used during filtering. If it is non-positive, it is computed from sigmaSpace.")
    @JIPipeParameter("diameter")
    public int getDiameter() {
        return diameter;
    }

    @JIPipeParameter("diameter")
    public void setDiameter(int diameter) {
        this.diameter = diameter;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OpenCvImageData inputData = iterationStep.getInputData(getFirstInputSlot(), OpenCvImageData.class, progressInfo);
        OpenCvImageData outputData = OpenCvImageUtils.generateForEachIndexedZCTSlice(inputData, (src, index) -> {
            Mat dst = new Mat();
            opencv_imgproc.bilateralFilter(src, dst, diameter, sigmaColor, sigmaSpace, borderType.getNativeValue());
            return dst;
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
