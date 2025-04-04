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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.color;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.expressions.ColorPixel5DExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Custom color auto threshold", description = "Applies a mathematical operation to each pixel to convert the color " +
        "into a greyscale value.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold\nColor")
@AddJIPipeInputSlot(value = ImagePlusColorData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust")
public class ColorThresholdExpression2D extends JIPipeSimpleIteratingAlgorithm {

    private static ColorSpace COLOR_SPACE_RGB = new RGBColorSpace();
    private static ColorSpace COLOR_SPACE_HSB = new HSBColorSpace();
    private static ColorSpace COLOR_SPACE_LAB = new LABColorSpace();
    private JIPipeExpressionParameter expression = new JIPipeExpressionParameter("B > 50");

    public ColorThresholdExpression2D(JIPipeNodeInfo info) {
        super(info);
    }

    public ColorThresholdExpression2D(ColorThresholdExpression2D other) {
        super(other);
        this.expression = new JIPipeExpressionParameter(other.expression);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusColorData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusColorData.class, progressInfo);
        ImagePlus img = inputData.getImage();
        ImagePlus result = IJ.createHyperStack("Greyscale",
                img.getWidth(),
                img.getHeight(),
                img.getNChannels(),
                img.getNSlices(),
                img.getNFrames(),
                8);

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

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(result), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Expression", description = "This expression is executed for each pixel. It provides the pixel components in the " +
            "original color space, as well as in other color spaces. The expression must return a boolean value or a number (<=0 = background, >0 = foreground).")
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
