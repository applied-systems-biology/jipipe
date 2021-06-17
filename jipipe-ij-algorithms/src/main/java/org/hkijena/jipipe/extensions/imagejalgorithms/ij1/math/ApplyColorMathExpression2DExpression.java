package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.HSBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.LABColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ColoredImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ColorPixel5DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.editors.JIPipeDataParameterSettings;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeDataInfoRef;

import java.util.List;

@JIPipeDocumentation(name = "Color math expression", description = "Applies a mathematical operation to each pixel. " +
        "The three available channels can be addressed individually.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math")
@JIPipeInputSlot(value = ImagePlusColorData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusColorData.class, slotName = "Output", autoCreate = true)
public class ApplyColorMathExpression2DExpression extends JIPipeSimpleIteratingAlgorithm {

    private static ColorSpace COLOR_SPACE_RGB = new RGBColorSpace();
    private static ColorSpace COLOR_SPACE_HSB = new HSBColorSpace();
    private static ColorSpace COLOR_SPACE_LAB = new LABColorSpace();
    private DefaultExpressionParameter expression = new DefaultExpressionParameter("ARRAY(255 - r, g, b)");
    private JIPipeDataInfoRef outputType = new JIPipeDataInfoRef(JIPipeDataInfo.getInstance(ImagePlusColorData.class));

    public ApplyColorMathExpression2DExpression(JIPipeNodeInfo info) {
        super(info);
        updateSlots();
    }

    public ApplyColorMathExpression2DExpression(ApplyColorMathExpression2DExpression other) {
        super(other);
        this.expression = new DefaultExpressionParameter(other.expression);
        this.outputType = new JIPipeDataInfoRef(other.outputType);
        updateSlots();
    }

    private void updateSlots() {
        getFirstOutputSlot().setAcceptedDataType(outputType.getInfo().getDataClass());
        getEventBus().post(new JIPipeGraph.NodeSlotsChangedEvent(this));
    }

    @JIPipeDocumentation(name = "Output type", description = "Determines which data type is generated. Please note that channels are " +
            "re-interpreted, instead of converted.")
    @JIPipeParameter("output-type")
    @JIPipeDataParameterSettings(dataBaseClass = ColoredImagePlusData.class)
    public JIPipeDataInfoRef getOutputType() {
        return outputType;
    }

    @JIPipeParameter("output-type")
    public void setOutputType(JIPipeDataInfoRef outputType) {
        this.outputType = outputType;
        updateSlots();
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusColorData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusColorData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ExpressionParameters variableSet = new ExpressionParameters();
        variableSet.set("width", inputData.getImage().getWidth());
        variableSet.set("height", inputData.getImage().getHeight());
        variableSet.set("num_z", inputData.getImage().getNSlices());
        variableSet.set("num_c", inputData.getImage().getNChannels());
        variableSet.set("num_t", inputData.getImage().getNFrames());
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

        dataBatch.addOutputData(getFirstOutputSlot(), JIPipe.createData(outputType.getInfo().getDataClass(), img), progressInfo);
    }

    @JIPipeDocumentation(name = "Expression", description = "This expression is executed for each pixel. It provides the pixel components in the " +
            "original color space, as well as in other color spaces. The expression must return an array with three components, " +
            "each component ranging from 0-255 (otherwise the values are automatically clamped). " +
            "Alternatively, the expression can return a number that encodes the color components as integer.")
    @JIPipeParameter("expression")
    @ExpressionParameterSettings(variableSource = ColorPixel5DExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(DefaultExpressionParameter expression) {
        this.expression = expression;
    }
}
