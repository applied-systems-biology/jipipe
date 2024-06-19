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

import org.bytedeco.javacpp.opencv_core;
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
import org.hkijena.jipipe.plugins.opencv.utils.OpenCvDepth;
import org.hkijena.jipipe.plugins.opencv.utils.OpenCvImageUtils;

@SetJIPipeDocumentation(name = "OpenCV Convert Depth", description = "Converts an OpenCV image to another depth. Applied per channel.")
@ConfigureJIPipeNode(menuPath = "Convert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = OpenCvImageData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = OpenCvImageData.class, slotName = "Output", create = true)
public class ConvertDepthAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OpenCvDepth depth = OpenCvDepth.CV_8U;

    public ConvertDepthAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertDepthAlgorithm(ConvertDepthAlgorithm other) {
        super(other);
        this.depth = other.depth;
    }

    @SetJIPipeDocumentation(name = "Depth", description = "The output bit depth")
    @JIPipeParameter(value = "depth", important = true)
    public OpenCvDepth getDepth() {
        return depth;
    }

    @JIPipeParameter("depth")
    public void setDepth(OpenCvDepth depth) {
        this.depth = depth;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OpenCvImageData inputData = iterationStep.getInputData(getFirstInputSlot(), OpenCvImageData.class, progressInfo);
        OpenCvImageData outputData = OpenCvImageUtils.generateForEachIndexedZCTSlice(inputData, (src, index) -> {
            opencv_core.Mat dst = new opencv_core.Mat();
            src.convertTo(dst, depth.getNativeValue());
            return dst;
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
