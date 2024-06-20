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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.math;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.PixelCoordinate5DExpressionParameterVariablesInfo;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Image calculator 2D (Expression)", description = "Applies a pixel-wise mathematical operation that produces a single output image.")
@AddJIPipeInputSlot(value = ImagePlusGreyscale32FData.class)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Output", create = true)
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process", aliasName = "Image Calculator... (expression)")
public class ImageCalculator2DExpression extends JIPipeIteratingAlgorithm {

    private JIPipeExpressionParameter expression = new JIPipeExpressionParameter("(I1 + I2) / 2");

    public ImageCalculator2DExpression(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("I1", "The first input", ImagePlusGreyscale32FData.class)
                .addInputSlot("I2", "The second input", ImagePlusGreyscale32FData.class)
                .addOutputSlot("Output", "The output image", ImagePlusGreyscale32FData.class, null)
                .sealOutput()
                .build());
    }

    public ImageCalculator2DExpression(ImageCalculator2DExpression other) {
        super(other);
        this.expression = new JIPipeExpressionParameter(other.expression);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Map<String, ImagePlus> images = new HashMap<>();
        for (JIPipeDataSlot slot : getDataInputSlots()) {
            images.put(slot.getName(), iterationStep.getInputData(slot, ImagePlusGreyscale32FData.class, progressInfo).getImage());
        }

        if (images.isEmpty())
            return;

        ImagePlus referenceImage = images.values().iterator().next();

        int width = referenceImage.getWidth();
        int height = referenceImage.getHeight();
        int nZ = referenceImage.getNSlices();
        int nC = referenceImage.getNChannels();
        int nT = referenceImage.getNFrames();
        for (ImagePlus image : images.values()) {
            if (image.getWidth() != width || image.getHeight() != height ||
                    image.getNFrames() != nT || image.getNChannels() != nC || image.getNSlices() != nZ) {
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new GraphNodeValidationReportContext(this),
                        "Input images do not have the same size!",
                        "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
            }
        }

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.set("width", width);
        variables.set("height", height);
        variables.set("num_z", nZ);
        variables.set("num_c", nC);
        variables.set("num_t", nT);
        variables.putCustomVariables(getDefaultCustomExpressionVariables());

        Map<String, ImageProcessor> processorMap = new HashMap<>();

        ImagePlus result = IJ.createHyperStack("Output", width, height, nC, nZ, nT, 32);
        ImageJUtils.forEachIndexedZCTSlice(referenceImage, (ip_, index) -> {
            variables.set("z", index.getZ());
            variables.set("c", index.getC());
            variables.set("t", index.getT());

            for (Map.Entry<String, ImagePlus> entry : images.entrySet()) {
                ImageProcessor slice = ImageJUtils.getSliceZero(entry.getValue(), index);
                processorMap.put(entry.getKey(), slice);
            }

            ImageProcessor resultProcessor = ImageJUtils.getSliceZero(result, index);

            for (int y = 0; y < height; y++) {
                variables.set("y", y);
                for (int x = 0; x < width; x++) {
                    variables.set("x", x);
                    for (Map.Entry<String, ImageProcessor> entry : processorMap.entrySet()) {
                        variables.set(entry.getKey(), entry.getValue().getf(x, y));
                    }

                    Number pixelResult = (Number) expression.evaluate(variables);
                    resultProcessor.setf(x, y, pixelResult.floatValue());
                }
            }
        }, progressInfo);

        ImageJUtils.calibrate(result, ImageJCalibrationMode.AutomaticImageJ, 0, 0);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale32FData(result), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Expression", description = "The mathematical expression that is applied to each pixel position in the input images. Additionally to the " +
            "positional variables, there are variables available that are named according to the input slots and contain the current pixel value of this slot.")
    @JIPipeParameter("expression")
    @JIPipeExpressionParameterSettings(variableSource = PixelCoordinate5DExpressionParameterVariablesInfo.class, hint = "per pixel")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public JIPipeExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(JIPipeExpressionParameter expression) {
        this.expression = expression;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
