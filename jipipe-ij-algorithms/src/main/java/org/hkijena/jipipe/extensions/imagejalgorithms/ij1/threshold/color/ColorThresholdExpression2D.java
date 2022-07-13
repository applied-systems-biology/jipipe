package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.color;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.HSBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.LABColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ColorPixel5DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Custom color auto threshold", description = "Applies a mathematical operation to each pixel to convert the color " +
        "into a greyscale value.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold\nColor")
@JIPipeInputSlot(value = ImagePlusColorData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust")
public class ColorThresholdExpression2D extends JIPipeSimpleIteratingAlgorithm {

    private static ColorSpace COLOR_SPACE_RGB = new RGBColorSpace();
    private static ColorSpace COLOR_SPACE_HSB = new HSBColorSpace();
    private static ColorSpace COLOR_SPACE_LAB = new LABColorSpace();
    private DefaultExpressionParameter expression = new DefaultExpressionParameter("B > 50");

    public ColorThresholdExpression2D(JIPipeNodeInfo info) {
        super(info);
    }

    public ColorThresholdExpression2D(ColorThresholdExpression2D other) {
        super(other);
        this.expression = new DefaultExpressionParameter(other.expression);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusColorData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusColorData.class, progressInfo);
        ImagePlus img = inputData.getImage();
        ImagePlus result = IJ.createHyperStack("Greyscale",
                img.getWidth(),
                img.getHeight(),
                img.getNChannels(),
                img.getNSlices(),
                img.getNFrames(),
                8);

        ExpressionVariables variableSet = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : dataBatch.getMergedTextAnnotations().values()) {
            variableSet.set(annotation.getName(), annotation.getValue());
        }
        variableSet.set("width", img.getWidth());
        variableSet.set("height", img.getHeight());
        variableSet.set("num_z", img.getNSlices());
        variableSet.set("num_c", img.getNChannels());
        variableSet.set("num_t", img.getNFrames());
        ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
            for (int y = 0; y < ip.getHeight(); y++) {
                for (int x = 0; x < ip.getWidth(); x++) {

                    ImageProcessor resultProcessor = ImageJUtils.getSliceZero(result, index);

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

                    Object evaluationResult = expression.evaluate(variableSet);
                    if (evaluationResult instanceof Boolean) {
                        if ((boolean) evaluationResult) {
                            resultProcessor.set(x, y, 255);
                        }
                    } else {
                        int number = ((Number) evaluationResult).intValue();
                        if (number > 0) {
                            resultProcessor.set(x, y, 255);
                        }
                    }
                }
            }
        }, progressInfo);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(result), progressInfo);
    }

    @JIPipeDocumentation(name = "Expression", description = "This expression is executed for each pixel. It provides the pixel components in the " +
            "original color space, as well as in other color spaces. The expression must return a boolean value or a number (<=0 = background, >0 = foreground).")
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
