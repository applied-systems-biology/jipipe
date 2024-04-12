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

package org.hkijena.jipipe.plugins.opencv.nodes;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_photo;
import org.bytedeco.opencv.global.opencv_xphoto;
import org.bytedeco.opencv.opencv_core.Mat;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.opencv.datatypes.OpenCvImageData;
import org.hkijena.jipipe.plugins.opencv.utils.OpenCvImageUtils;
import org.hkijena.jipipe.plugins.opencv.utils.OpenCvType;

@SetJIPipeDocumentation(name = "Image inpainting", description = "Applies an algorithm for inpainting (content-aware fill) that attempts to fill in masked areas based on the surrounding image information")
@ConfigureJIPipeNode(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Restore")
@AddJIPipeInputSlot(value = OpenCvImageData.class, slotName = "Input", create = true, description = "Input 8-bit, 16-bit unsigned or 32-bit float 1-channel or 8-bit 3-channel image. ")
@AddJIPipeInputSlot(value = OpenCvImageData.class, slotName = "Mask", create = true, description = "Inpainting mask, 8-bit 1-channel image. Non-zero pixels indicate the area that needs to be inpainted.")
@AddJIPipeOutputSlot(value = OpenCvImageData.class, slotName = "Output", create = true)
@AddJIPipeCitation("https://docs.opencv.org/3.4/df/d3d/tutorial_py_inpainting.html")
@AddJIPipeCitation("https://docs.opencv.org/4.x/dc/d2f/tutorial_xphoto_inpainting.html")
public class InpaintingAlgorithm extends JIPipeIteratingAlgorithm {

    private int radius = 3;
    private Method method = Method.ShiftMap;

    public InpaintingAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public InpaintingAlgorithm(InpaintingAlgorithm other) {
        super(other);
        this.radius = other.radius;
        this.method = other.method;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OpenCvImageData inputImage = iterationStep.getInputData("Input", OpenCvImageData.class, progressInfo);
        OpenCvImageData maskImage = iterationStep.getInputData("Mask", OpenCvImageData.class, progressInfo);
        OpenCvImageData outputImage = OpenCvImageUtils.generateForEachIndexedZCTSlice(inputImage, (src_, index) -> {
            Mat src = OpenCvImageUtils.toType(src_, OpenCvType.CV_8UC3, OpenCvType.CV_8U, OpenCvType.CV_16U, OpenCvType.CV_32F);
            Mat mask = OpenCvImageUtils.toMask(maskImage.getImageOrExpand(index));

            Mat dst = new Mat();
            if(method.isxPhoto()) {
                Mat invertedMask = new Mat();
                opencv_core.bitwise_not(mask, invertedMask);
                opencv_xphoto.inpaint(src, invertedMask, dst, method.nativeValue);
            }
            else {
                opencv_photo.inpaint(src, mask, dst, radius, method.nativeValue);
            }
            return dst;
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), outputImage, progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @SetJIPipeDocumentation(name = "Radius", description = "Radius of a circular neighborhood of each point inpainted that is considered by the algorithm.")
     @JIPipeParameter("radius")
    public int getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(int radius) {
        this.radius = radius;
    }

    @SetJIPipeDocumentation(name = "Method", description = "The inpainting method")
    @JIPipeParameter("method")
    public Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(Method method) {
        this.method = method;
        emitParameterUIChangedEvent();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if("radius".equals(access.getKey()) && method.isxPhoto()) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    public enum Method {
        TeleaEtAl(opencv_photo.INPAINT_TELEA, false),
        NavierStokesEtAl(opencv_photo.INPAINT_NS, false),
        ShiftMap(opencv_xphoto.INPAINT_SHIFTMAP, true),
        FSRFast(opencv_xphoto.INPAINT_FSR_FAST, true),
        FSRBest(opencv_xphoto.INPAINT_FSR_BEST, true);

        private final int nativeValue;
        private final boolean xPhoto;

        Method(int nativeValue, boolean xPhoto) {

            this.nativeValue = nativeValue;
            this.xPhoto = xPhoto;
        }

        public int getNativeValue() {
            return nativeValue;
        }

        public boolean isxPhoto() {
            return xPhoto;
        }
    }
}
