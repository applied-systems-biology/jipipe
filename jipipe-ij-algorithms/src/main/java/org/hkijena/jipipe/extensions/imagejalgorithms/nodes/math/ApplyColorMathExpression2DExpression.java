package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.math;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.HSBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.LABColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ColorPixel5DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.List;

@JIPipeDocumentation(name = "Color math expression", description = "Applies a mathematical operation to each pixel. " +
        "The three available channels can be addressed individually.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math")
@JIPipeInputSlot(value = ImagePlusColorData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusColorData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nMath", aliasName = "Macro... (per pixel, color)")
public class ApplyColorMathExpression2DExpression extends JIPipeSimpleIteratingAlgorithm {

    private static final ColorSpace COLOR_SPACE_RGB = new RGBColorSpace();
    private static final ColorSpace COLOR_SPACE_HSB = new HSBColorSpace();
    private static final ColorSpace COLOR_SPACE_LAB = new LABColorSpace();
    private JIPipeExpressionParameter expression = new JIPipeExpressionParameter("ARRAY(255 - r, g, b)");
    private JIPipeDataInfoRef outputType = new JIPipeDataInfoRef(JIPipeDataInfo.getInstance(ImagePlusColorData.class));
    private final CustomExpressionVariablesParameter customExpressionVariables;

    public ApplyColorMathExpression2DExpression(JIPipeNodeInfo info) {
        super(info);
        this.customExpressionVariables = new CustomExpressionVariablesParameter();
        updateSlots();
    }

    public ApplyColorMathExpression2DExpression(ApplyColorMathExpression2DExpression other) {
        super(other);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(this);
        this.expression = new JIPipeExpressionParameter(other.expression);
        this.outputType = new JIPipeDataInfoRef(other.outputType);
        updateSlots();
    }

    private void updateSlots() {
        getFirstOutputSlot().setAcceptedDataType(outputType.getInfo().getDataClass());
        emitNodeSlotsChangedEvent();
    }

    @JIPipeDocumentation(name = "Output type", description = "Determines which data type is generated. Please note that channels are " +
            "re-interpreted, instead of converted.")
    @JIPipeParameter("output-type")
    @JIPipeDataParameterSettings(dataBaseClass = ImagePlusData.class)
    public JIPipeDataInfoRef getOutputType() {
        return outputType;
    }

    @JIPipeParameter("output-type")
    public void setOutputType(JIPipeDataInfoRef outputType) {
        this.outputType = outputType;
        updateSlots();
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlusColorData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusColorData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ExpressionVariables variableSet = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
            variableSet.set(annotation.getName(), annotation.getValue());
        }
        variableSet.set("width", inputData.getImage().getWidth());
        variableSet.set("height", inputData.getImage().getHeight());
        variableSet.set("num_z", inputData.getImage().getNSlices());
        variableSet.set("num_c", inputData.getImage().getNChannels());
        variableSet.set("num_t", inputData.getImage().getNFrames());
        customExpressionVariables.writeToVariables(variableSet, true, "custom", true, "custom");

        ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
            for (int y = 0; y < ip.getHeight(); y++) {
                for (int x = 0; x < ip.getWidth(); x++) {
                    int pixel = ip.get(x, y);
                    variableSet.set("z", index.getZ());
                    variableSet.set("c", index.getC());
                    variableSet.set("t", index.getT());
                    variableSet.set("x", (double) x);
                    variableSet.set("y", (double) y);

                    // Convert pixel
                    int as_rgb = COLOR_SPACE_RGB.convert(pixel, inputData.getColorSpace());
                    int as_hsb = COLOR_SPACE_HSB.convert(pixel, inputData.getColorSpace());
                    int as_lab = COLOR_SPACE_LAB.convert(pixel, inputData.getColorSpace());

                    int r = (as_rgb & 0xff0000) >> 16;
                    int g = ((as_rgb & 0xff00) >> 8);
                    int b = (as_rgb & 0xff);
                    int H = (as_hsb & 0xff0000) >> 16;
                    int S = ((as_hsb & 0xff00) >> 8);
                    int B = (as_hsb & 0xff);
                    int lL = (as_lab & 0xff0000) >> 16;
                    int la = ((as_lab & 0xff00) >> 8);
                    int lb = (as_lab & 0xff);

                    variableSet.set("r", r);
                    variableSet.set("g", g);
                    variableSet.set("b", b);
                    variableSet.set("H", H);
                    variableSet.set("S", S);
                    variableSet.set("B", B);
                    variableSet.set("LL", lL);
                    variableSet.set("La", la);
                    variableSet.set("Lb", lb);

                    Object result = expression.evaluate(variableSet);
                    int generatedPixel;
                    if (result instanceof List) {
                        List<?> list = (List<?>) result;
                        int c0 = Math.max(0, Math.min(255, ((Number) list.get(0)).intValue()));
                        int c1 = Math.max(0, Math.min(255, ((Number) list.get(1)).intValue()));
                        int c2 = Math.max(0, Math.min(255, ((Number) list.get(2)).intValue()));
                        generatedPixel = (c0 << 16) + (c1 << 8) + c2;
                    } else {
                        Number number = (Number) result;
                        generatedPixel = number.intValue();
                    }

                    ip.set(x, y, generatedPixel);
                }
            }
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), JIPipe.createData(outputType.getInfo().getDataClass(), img), progressInfo);
    }

    @JIPipeDocumentation(name = "Expression", description = "This expression is executed for each pixel. It provides the pixel components in the " +
            "original color space, as well as in other color spaces. The expression must return an array with three components, " +
            "each component ranging from 0-255 (otherwise the values are automatically clamped). " +
            "Alternatively, the expression can return a number that encodes the color components as integer.")
    @JIPipeParameter("expression")
    @ExpressionParameterSettings(variableSource = ColorPixel5DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    public JIPipeExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(JIPipeExpressionParameter expression) {
        this.expression = expression;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(custom, \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomExpressionVariables() {
        return customExpressionVariables;
    }
}
