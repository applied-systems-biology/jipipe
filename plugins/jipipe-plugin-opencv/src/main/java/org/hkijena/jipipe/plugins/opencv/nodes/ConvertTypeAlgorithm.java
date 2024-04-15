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
import org.hkijena.jipipe.plugins.opencv.utils.OpenCvImageUtils;
import org.hkijena.jipipe.plugins.opencv.utils.OpenCvType;

@SetJIPipeDocumentation(name = "OpenCV Convert Type", description = "Converts an OpenCV image to another type.")
@ConfigureJIPipeNode(menuPath = "Convert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = OpenCvImageData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = OpenCvImageData.class, slotName = "Output", create = true)
public class ConvertTypeAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OpenCvType type = OpenCvType.CV_8U;

    public ConvertTypeAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertTypeAlgorithm(ConvertTypeAlgorithm other) {
        super(other);
        this.type = other.type;
    }

    @SetJIPipeDocumentation(name = "Type", description = "The output type")
    @JIPipeParameter(value = "type", important = true)
    public OpenCvType getType() {
        return type;
    }

    @JIPipeParameter("type")
    public void setType(OpenCvType type) {
        this.type = type;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OpenCvImageData inputData = iterationStep.getInputData(getFirstInputSlot(), OpenCvImageData.class, progressInfo);
        OpenCvImageData outputData = OpenCvImageUtils.generateForEachIndexedZCTSlice(inputData, (src, index) -> OpenCvImageUtils.toType(src, type), progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
