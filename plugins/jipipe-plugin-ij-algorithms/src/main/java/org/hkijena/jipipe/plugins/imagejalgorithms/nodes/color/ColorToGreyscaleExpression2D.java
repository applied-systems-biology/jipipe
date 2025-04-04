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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.color;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.HSBColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.LABColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.RGBColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.expressions.ColorPixel5DExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

@SetJIPipeDocumentation(name = "Color to greyscale (Expression)", description = "Applies a mathematical operation to each pixel to convert the color " +
        "into a greyscale value.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors\nConvert")
@AddJIPipeInputSlot(value = ImagePlusColorData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output", create = true)
public class ColorToGreyscaleExpression2D extends JIPipeSimpleIteratingAlgorithm {

    private static ColorSpace COLOR_SPACE_RGB = new RGBColorSpace();
    private static ColorSpace COLOR_SPACE_HSB = new HSBColorSpace();
    private static ColorSpace COLOR_SPACE_LAB = new LABColorSpace();
    private JIPipeExpressionParameter expression = new JIPipeExpressionParameter("(r + g + b) / 3");
    private JIPipeDataInfoRef outputType = new JIPipeDataInfoRef(JIPipeDataInfo.getInstance(ImagePlusGreyscale32FData.class));

    public ColorToGreyscaleExpression2D(JIPipeNodeInfo info) {
        super(info);
        updateSlots();
    }

    public ColorToGreyscaleExpression2D(ColorToGreyscaleExpression2D other) {
        super(other);
        this.expression = new JIPipeExpressionParameter(other.expression);
        this.outputType = new JIPipeDataInfoRef(other.outputType);
        updateSlots();
    }

    private void updateSlots() {
        getFirstOutputSlot().setAcceptedDataType(outputType.getInfo().getDataClass());
        emitNodeSlotsChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Output type", description = "Determines which data type is generated. Please note that the generic greyscale output " +
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusColorData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusColorData.class, progressInfo);
        ImagePlus img = inputData.getImage();
        ImagePlus result;
        if (ImagePlusGreyscale8UData.class.isAssignableFrom(outputType.getInfo().getDataClass())) {
            result = IJ.createHyperStack("Greyscale", img.getWidth(), img.getHeight(), img.getNChannels(), img.getNSlices(), img.getNFrames(), 8);
        } else if (ImagePlusGreyscale16UData.class.isAssignableFrom(outputType.getInfo().getDataClass())) {
            result = IJ.createHyperStack("Greyscale", img.getWidth(), img.getHeight(), img.getNChannels(), img.getNSlices(), img.getNFrames(), 16);
        } else {
            result = IJ.createHyperStack("Greyscale", img.getWidth(), img.getHeight(), img.getNChannels(), img.getNSlices(), img.getNFrames(), 32);
        }

        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap(iterationStep);
        variableSet.set("width", img.getWidth());
        variableSet.set("height", img.getHeight());
        variableSet.set("num_z", img.getNSlices());
        variableSet.set("num_c", img.getNChannels());
        variableSet.set("num_t", img.getNFrames());
        ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
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
        iterationStep.addOutputData(getFirstOutputSlot(), JIPipe.createData(outputType.getInfo().getDataClass(), result), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Expression", description = "This expression is executed for each pixel. It provides the pixel components in the " +
            "original color space, as well as in other color spaces. The expression must return a number that will be stored as greyscale value.")
    @JIPipeParameter("expression")
    @JIPipeExpressionParameterSettings(variableSource = ColorPixel5DExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(JIPipeExpressionParameter expression) {
        this.expression = expression;
    }
}
