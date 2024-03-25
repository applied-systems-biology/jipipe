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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.binary;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SetJIPipeDocumentation(name = "Conditional invert", description = "Inverts the image if a condition (based on statistics) is met. Otherwise, the image is not inverted.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Binary")
@AddJIPipeInputSlot(slotName = "Input", value = ImagePlusGreyscaleMaskData.class, create = true)
@AddJIPipeOutputSlot(slotName = "Output", value = ImagePlusGreyscaleMaskData.class, create = true)
public class ConditionalInverterAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter condition = new JIPipeExpressionParameter("num_white > num_black");
    private boolean applyPerSlice = false;

    public ConditionalInverterAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConditionalInverterAlgorithm(ConditionalInverterAlgorithm other) {
        super(other);
        this.condition = new JIPipeExpressionParameter(other.condition);
        this.applyPerSlice = other.applyPerSlice;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo).getDuplicateImage();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        for (Map.Entry<String, JIPipeTextAnnotation> entry : iterationStep.getMergedTextAnnotations().entrySet()) {
            variables.set(entry.getKey(), entry.getValue().getValue());
        }
        variables.set("width", img.getWidth());
        variables.set("height", img.getHeight());
        variables.set("depth", img.getNSlices());
        variables.set("channels", img.getNChannels());
        variables.set("frames", img.getNFrames());
        variables.set("z", -1);
        variables.set("c", -1);
        variables.set("t", -1);
        variables.set("title", img.getTitle());
        if (applyPerSlice) {
            ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                double[] nums = new double[2];
                byte[] arr = (byte[]) ip.getPixels();
                for (byte b : arr) {
                    int val = Byte.toUnsignedInt(b);
                    if (val > 0) {
                        nums[1]++;
                    } else {
                        nums[0]++;
                    }
                }
                variables.set("num_white", nums[1]);
                variables.set("num_black", nums[0]);

                if (condition.test(variables)) {
                    ip.invert();
                }
            }, progressInfo);
        } else {
            double[] nums = new double[2];
            ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                byte[] arr = (byte[]) ip.getPixels();
                for (byte b : arr) {
                    int val = Byte.toUnsignedInt(b);
                    if (val > 0) {
                        nums[1]++;
                    } else {
                        nums[0]++;
                    }
                }
            }, progressInfo);
            variables.set("num_white", nums[1]);
            variables.set("num_black", nums[0]);

            if (condition.test(variables)) {
                ImageJUtils.forEachSlice(img, ImageProcessor::invert, progressInfo);
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Condition", description = "If the expression returns TRUE, the image will be inverted")
    @JIPipeParameter("condition")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class)
    public JIPipeExpressionParameter getCondition() {
        return condition;
    }

    @JIPipeParameter("condition")
    public void setCondition(JIPipeExpressionParameter condition) {
        this.condition = condition;
    }

    @SetJIPipeDocumentation(name = "Apply per slice", description = "If enabled, the operation is applied for each individual slice. Otherwise, statistics are extracted for the whole image.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {

        private final static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("width", "Image width", "The width of the image"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("height", "Image height", "The height of the image"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("depth", "Image depth", "The depth (number of slices) of the image"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_channels", "Image channels", "The channels (number of channel slices) of the image"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_frames", "Image frames", "The frames of the image"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("z", "Slice Z", "The Z index of the current slice (-1 if whole image)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("c", "Slice C", "The C index of the current slice (-1 if whole image)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("t", "Slice T", "The T index of the current slice (-1 if whole image)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("title", "Image title", "The title of the image"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_white", "Number of white pixels", "Pixels with a value > 0"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_black", "Number of black pixels", "Pixels with a value = 0"));
            VARIABLES.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
