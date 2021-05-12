package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.SourceWrapMode;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.PixelCoordinate5DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Local image calculator 2D (Expression)", description = "Applies a pixel-wise mathematical operation that produces a single output image. Available " +
        "are both the current pixel values, as well as the local areas around these pixels.")
@JIPipeInputSlot(value = ImagePlusGreyscale32FData.class)
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", autoCreate = true)
@JIPipeOrganization(menuPath = "Math\nLocal", nodeTypeCategory = ImagesNodeTypeCategory.class)
public class LocalImageCalculator2DExpression extends JIPipeIteratingAlgorithm {

    private DefaultExpressionParameter expression = new DefaultExpressionParameter("(MEDIAN(Local.I1) + I2) / 2");
    private int localWindowWidth = 3;
    private int localWindowHeight = 3;
    private SourceWrapMode sourceWrapMode = SourceWrapMode.Zero;

    public LocalImageCalculator2DExpression(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("I1", ImagePlusGreyscale32FData.class)
                .addInputSlot("I2", ImagePlusGreyscale32FData.class)
                .addOutputSlot("Output", ImagePlusGreyscale32FData.class, null)
                .sealOutput()
                .build());
    }

    public LocalImageCalculator2DExpression(LocalImageCalculator2DExpression other) {
        super(other);
        this.expression = new DefaultExpressionParameter(other.expression);
        this.localWindowWidth = other.localWindowWidth;
        this.localWindowHeight = other.localWindowHeight;
        this.sourceWrapMode = other.sourceWrapMode;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Map<String, ImagePlus> images = new HashMap<>();
        for (JIPipeDataSlot slot : getEffectiveInputSlots()) {
            images.put(slot.getName(), dataBatch.getInputData(slot, ImagePlusGreyscale32FData.class, progressInfo).getImage());
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
                throw new UserFriendlyRuntimeException("Input images do not have the same size!",
                        "Input images do not have the same size!",
                        getName(),
                        "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channes.",
                        "Please check the input images.");
            }
        }

        ExpressionParameters parameters = new ExpressionParameters();
        parameters.set("width", width);
        parameters.set("height", height);
        parameters.set("num_z", nZ);
        parameters.set("num_c", nC);
        parameters.set("num_t", nT);

        Map<String, ImageProcessor> processorMap = new HashMap<>();

        ImagePlus result = IJ.createHyperStack("Output", width, height, nC, nZ, nT, 32);
        ImageJUtils.forEachIndexedZCTSlice(referenceImage, (ip_, index) -> {
            parameters.set("z", index.getZ());
            parameters.set("c", index.getC());
            parameters.set("t", index.getT());

            for (Map.Entry<String, ImagePlus> entry : images.entrySet()) {
                ImageProcessor slice = ImageJUtils.getSliceZero(entry.getValue(), index);
                processorMap.put(entry.getKey(), slice);
            }

            ImageProcessor resultProcessor = ImageJUtils.getSliceZero(result, index);

            for (int y = 0; y < height; y++) {
                parameters.set("y", y);
                for (int x = 0; x < width; x++) {
                    parameters.set("x", x);
                    for (Map.Entry<String, ImageProcessor> entry : processorMap.entrySet()) {
                        parameters.set(entry.getKey(), entry.getValue().getf(x, y));

                        // Fetch local area
                        List<Double> localArea = getLocalArea(entry.getValue(), x, y);
                        parameters.set("Local." + entry.getKey(), localArea);
                    }

                    Number pixelResult = (Number) expression.evaluate(parameters);
                    resultProcessor.setf(x, y, pixelResult.floatValue());
                }
            }
        }, progressInfo);

        ImageJUtils.calibrate(result, ImageJCalibrationMode.AutomaticImageJ, 0, 0);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale32FData(result), progressInfo);
    }

    private List<Double> getLocalArea(ImageProcessor processor, int cx, int cy) {
        List<Double> result = new ArrayList<>();
        for (int i = 0; i < localWindowHeight; i++) {
            int y = cy - localWindowHeight / 2 + i;
            for (int j = 0; j < localWindowHeight; j++) {
                int x = cx - localWindowWidth / 2 + j;
                result.add(sourceWrapMode.getPixelFloat(processor, x, y) * 1.0);
            }
        }
        return result;
    }

    @JIPipeDocumentation(name = "Expression", description = "The mathematical expression that is applied to each pixel position in the input images. Additionally to the " +
            "positional variables, there are variables available that are named according to the input slots and contain the current pixel value of this slot. Arrays prefixed with " +
            "'Local.' are also available and contain the local image values (Example: Local.L1). The array contains the pixel values in Row-Major form (x0y0, x1y0, x2y0, x1y1, ...)")
    @JIPipeParameter("expression")
    @ExpressionParameterSettings(variableSource = PixelCoordinate5DExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(DefaultExpressionParameter expression) {
        this.expression = expression;
    }

    @JIPipeDocumentation(name = "Local window width", description = "Width of the local window")
    @JIPipeParameter("local-window-width")
    public int getLocalWindowWidth() {
        return localWindowWidth;
    }

    @JIPipeParameter("local-window-width")
    public void setLocalWindowWidth(int localWindowWidth) {
        this.localWindowWidth = localWindowWidth;
    }

    @JIPipeDocumentation(name = "Local window height", description = "Height of the local window")
    @JIPipeParameter("local-window-height")
    public int getLocalWindowHeight() {
        return localWindowHeight;
    }

    @JIPipeParameter("local-window-height")
    public void setLocalWindowHeight(int localWindowHeight) {
        this.localWindowHeight = localWindowHeight;
    }

    @JIPipeDocumentation(name = "Border pixel mode", description = "Determines how border pixels are handled. If set to 'Skip' border pixels are still processed by " +
            "the expression, but the local area variable will be missing pixels.")
    @JIPipeParameter("source-wrap-mode")
    public SourceWrapMode getSourceWrapMode() {
        return sourceWrapMode;
    }

    @JIPipeParameter("source-wrap-mode")
    public void setSourceWrapMode(SourceWrapMode sourceWrapMode) {
        this.sourceWrapMode = sourceWrapMode;
    }
}
