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

import org.bytedeco.opencv.global.opencv_photo;
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
import org.hkijena.jipipe.plugins.opencv.datatypes.OpenCvImageData;
import org.hkijena.jipipe.plugins.opencv.utils.OpenCvImageUtils;

@SetJIPipeDocumentation(name = "Image inpainting", description = "Applies an algorithm for inpainting (content-aware fill) that attempts to fill in masked areas based on the surrounding image information")
@ConfigureJIPipeNode(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Restore")
@AddJIPipeInputSlot(value = OpenCvImageData.class, slotName = "Input", create = true)
@AddJIPipeInputSlot(value = OpenCvImageData.class, slotName = "Mask", create = true)
@AddJIPipeOutputSlot(value = OpenCvImageData.class, slotName = "Output", create = true)
@AddJIPipeCitation("https://docs.opencv.org/3.4/df/d3d/tutorial_py_inpainting.html")
public class InpaintingAlgorithm extends JIPipeIteratingAlgorithm {

    private int radius = 3;
    private Method method = Method.TeleaEtAl;

    public InpaintingAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public InpaintingAlgorithm(InpaintingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OpenCvImageData inputImage = iterationStep.getInputData("Input", OpenCvImageData.class, progressInfo);
        OpenCvImageData maskImage = iterationStep.getInputData("Mask", OpenCvImageData.class, progressInfo);
        OpenCvImageData outputImage = OpenCvImageUtils.generateForEachIndexedZCTSlice(inputImage, (input, index) -> {
            Mat mask = maskImage.getImageOrExpand(index);
            Mat dst = new Mat();
            opencv_photo.inpaint(input, mask, dst, radius, method.nativeValue);
            return dst;
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), outputImage, progressInfo);
    }

    public enum Method {
        TeleaEtAl(opencv_photo.INPAINT_TELEA),
        NavierStokesEtAl(opencv_photo.INPAINT_NS);

        private final int nativeValue;

        Method(int nativeValue) {

            this.nativeValue = nativeValue;
        }

        public int getNativeValue() {
            return nativeValue;
        }
    }
}
