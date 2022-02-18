package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.color;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.HSBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.LABColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ColorPixel5DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.editors.JIPipeDataParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

@JIPipeDocumentation(name = "Color to greyscale (Expression)", description = "Applies a mathematical operation to each pixel to convert the color " +
        "into a greyscale value.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors\nConvert")
@JIPipeInputSlot(value = ImagePlusColorData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class ColorToGreyscaleExpression2D extends JIPipeSimpleIteratingAlgorithm {

    private static ColorSpace COLOR_SPACE_RGB = new RGBColorSpace();
    private static ColorSpace COLOR_SPACE_HSB = new HSBColorSpace();
    private static ColorSpace COLOR_SPACE_LAB = new LABColorSpace();
    private DefaultExpressionParameter expression = new DefaultExpressionParameter("(r + g + b) / 3");
    private JIPipeDataInfoRef outputType = new JIPipeDataInfoRef(JIPipeDataInfo.getInstance(ImagePlusGreyscale32FData.class));

    public ColorToGreyscaleExpression2D(JIPipeNodeInfo info) {
        super(info);
        updateSlots();
    }

    public ColorToGreyscaleExpression2D(ColorToGreyscaleExpression2D other) {
        super(other);
        this.expression = new DefaultExpressionParameter(other.expression);
        this.outputType = new JIPipeDataInfoRef(other.outputType);
        updateSlots();
    }

    private void updateSlots() {
        getFirstOutputSlot().setAcceptedDataType(outputType.getInfo().getDataClass());
        getEventBus().post(new JIPipeGraph.NodeSlotsChangedEvent(this));
    }

    @JIPipeDocumentation(name = "Output type", description = "Determines which data type is generated. Please note that the generic greyscale output " +
            "is 32-bit float.")
    @JIPipeParameter("output-type")
    @JIPipeDataParameterSettings(dataBaseClass = ImagePlusGreyscaleData.class)
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
        ImagePlus img = inputData.getImage();
        ImagePlus result;
        if (ImagePlusGreyscale8UData.class.isAssignableFrom(outputType.getInfo().getDataClass())) {
            result = IJ.createHyperStack("Greyscale", img.getWidth(), img.getHeight(), img.getNChannels(), img.getNSlices(), img.getNFrames(), 8);
        } else if (ImagePlusGreyscale16UData.class.isAssignableFrom(outputType.getInfo().getDataClass())) {
            result = IJ.createHyperStack("Greyscale", img.getWidth(), img.getHeight(), img.getNChannels(), img.getNSlices(), img.getNFrames(), 16);
        } else {
            result = IJ.createHyperStack("Greyscale", img.getWidth(), img.getHeight(), img.getNChannels(), img.getNSlices(), img.getNFrames(), 32);
        }

        ExpressionVariables variableSet = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : dataBatch.getMergedAnnotations().values()) {
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
                    resultProcessor.setf(x, y, ((Number) evaluationResult).floatValue());
                }
            }
        }, progressInfo);

        ImageJUtils.calibrate(result, ImageJCalibrationMode.AutomaticImageJ, 0, 0);
        dataBatch.addOutputData(getFirstOutputSlot(), JIPipe.createData(outputType.getInfo().getDataClass(), result), progressInfo);
    }

    @JIPipeDocumentation(name = "Expression", description = "This expression is executed for each pixel. It provides the pixel components in the " +
            "original color space, as well as in other color spaces. The expression must return a number that will be stored as greyscale value.")
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
