package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.binary;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "Conditional invert", description = "Inverts the image if a condition (based on statistics) is met. Otherwise, the image is not inverted.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Binary")
@JIPipeInputSlot(slotName = "Input", value = ImagePlusGreyscaleMaskData.class, autoCreate = true)
@JIPipeInputSlot(slotName = "Output", value = ImagePlusGreyscaleMaskData.class, autoCreate = true)
public class ConditionalInverterAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter condition = new DefaultExpressionParameter("num_white > num_black");
    private boolean applyPerSlice = false;

    public ConditionalInverterAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConditionalInverterAlgorithm(ConditionalInverterAlgorithm other) {
        super(other);
        this.condition = new DefaultExpressionParameter(other.condition);
        this.applyPerSlice = other.applyPerSlice;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo).getDuplicateImage();
        ExpressionVariables variables = new ExpressionVariables();
        for (Map.Entry<String, JIPipeAnnotation> entry : dataBatch.getGlobalAnnotations().entrySet()) {
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
        if(applyPerSlice) {
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

                if(condition.test(variables)) {
                    ip.invert();
                }
            }, progressInfo);
        }
        else {
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

            if(condition.test(variables)) {
                ImageJUtils.forEachSlice(img, ImageProcessor::invert, progressInfo);
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Condition", description = "If the expression returns TRUE, the image will be inverted")
    @JIPipeParameter("condition")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public DefaultExpressionParameter getCondition() {
        return condition;
    }

    @JIPipeParameter("condition")
    public void setCondition(DefaultExpressionParameter condition) {
        this.condition = condition;
    }

    @JIPipeDocumentation(name = "Apply per slice", description = "If enabled, the operation is applied for each individual slice. Otherwise, statistics are extracted for the whole image.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        private final static Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(new ExpressionParameterVariable("Image width", "The width of the image", "width"));
            VARIABLES.add(new ExpressionParameterVariable("Image height", "The height of the image", "height"));
            VARIABLES.add(new ExpressionParameterVariable("Image depth", "The depth (number of slices) of the image", "depth"));
            VARIABLES.add(new ExpressionParameterVariable("Image channels", "The channels (number of channel slices) of the image", "num_channels"));
            VARIABLES.add(new ExpressionParameterVariable("Image frames", "The frames of the image", "num_frames"));
            VARIABLES.add(new ExpressionParameterVariable("Slice Z", "The Z index of the current slice (-1 if whole image)", "z"));
            VARIABLES.add(new ExpressionParameterVariable("Slice C", "The C index of the current slice (-1 if whole image)", "c"));
            VARIABLES.add(new ExpressionParameterVariable("Slice T", "The T index of the current slice (-1 if whole image)", "t"));
            VARIABLES.add(new ExpressionParameterVariable("Image title", "The title of the image", "title"));
            VARIABLES.add(new ExpressionParameterVariable("Number of white pixels", "Pixels with a value > 0", "num_white"));
            VARIABLES.add(new ExpressionParameterVariable("Number of black pixels", "Pixels with a value = 0", "num_black"));
            VARIABLES.add(new ExpressionParameterVariable("<Annotations>", "Annotations of the current image", ""));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
           return VARIABLES;
        }
    }
}
