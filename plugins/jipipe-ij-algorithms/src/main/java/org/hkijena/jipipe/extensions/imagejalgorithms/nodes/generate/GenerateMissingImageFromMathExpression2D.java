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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.generate;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMissingDataGeneratorAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.OptionalBitDepth;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.PixelCoordinate5DExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Generate missing from math expression", description = "Generates an image if it cannot be matched to a reference " +
        "in a data batch according to the mathematical operation." +
        " Applies a mathematical operation to each pixel. The value is written into the image. ")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", create = true, optional = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Image", create = true)
public class GenerateMissingImageFromMathExpression2D extends JIPipeMissingDataGeneratorAlgorithm {

    private JIPipeExpressionParameter function = new JIPipeExpressionParameter("0");
    private OptionalIntegerParameter overwriteWidth = new OptionalIntegerParameter(false, 256);
    private OptionalIntegerParameter overwriteHeight = new OptionalIntegerParameter(false, 256);
    private OptionalIntegerParameter overwriteSizeZ = new OptionalIntegerParameter(false, 1);
    private OptionalIntegerParameter overwriteSizeC = new OptionalIntegerParameter(false, 1);
    private OptionalIntegerParameter overwriteSizeT = new OptionalIntegerParameter(false, 1);
    private OptionalBitDepth overwriteOutputBitDepth = OptionalBitDepth.None;
    private boolean generateOnePerBatch = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GenerateMissingImageFromMathExpression2D(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GenerateMissingImageFromMathExpression2D(GenerateMissingImageFromMathExpression2D other) {
        super(other);
        this.function = new JIPipeExpressionParameter(other.function);
        this.overwriteWidth = new OptionalIntegerParameter(other.overwriteWidth);
        this.overwriteHeight = new OptionalIntegerParameter(other.overwriteHeight);
        this.overwriteSizeZ = new OptionalIntegerParameter(other.overwriteSizeZ);
        this.overwriteSizeC = new OptionalIntegerParameter(other.overwriteSizeC);
        this.overwriteSizeT = new OptionalIntegerParameter(other.overwriteSizeT);
        this.overwriteOutputBitDepth = other.overwriteOutputBitDepth;
        this.generateOnePerBatch = other.generateOnePerBatch;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runGenerator(JIPipeMultiIterationStep iterationStep, JIPipeInputDataSlot inputSlot, JIPipeOutputDataSlot outputSlot, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot referenceSlot = getInputSlot("Reference");
        for (Integer inputRow : iterationStep.getInputRows(referenceSlot)) {
            progressInfo.log("Row " + inputRow);
            ImagePlus referenceImage = referenceSlot.getData(inputRow, ImagePlusData.class, progressInfo).getImage();
            int width = referenceImage.getWidth();
            int height = referenceImage.getHeight();
            int sizeC = referenceImage.getNChannels();
            int sizeZ = referenceImage.getNSlices();
            int sizeT = referenceImage.getNFrames();
            int bitDepth = referenceImage.getBitDepth();
            if (overwriteWidth.isEnabled())
                width = overwriteWidth.getContent();
            if (overwriteHeight.isEnabled())
                height = overwriteHeight.getContent();
            if (overwriteSizeC.isEnabled())
                sizeC = overwriteSizeC.getContent();
            if (overwriteSizeZ.isEnabled())
                sizeZ = overwriteSizeZ.getContent();
            if (overwriteSizeT.isEnabled())
                sizeT = overwriteSizeT.getContent();
            if (overwriteOutputBitDepth != OptionalBitDepth.None)
                bitDepth = overwriteOutputBitDepth.getBitDepth();

            ImagePlus img = IJ.createHyperStack("Generated", width, height, sizeC, sizeZ, sizeT, bitDepth);
            JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
            variableSet.set("width", overwriteWidth);
            variableSet.set("height", overwriteHeight);
            variableSet.set("num_z", overwriteSizeZ);
            variableSet.set("num_c", overwriteSizeC);
            variableSet.set("num_t", overwriteSizeT);

            ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                for (int y = 0; y < ip.getHeight(); y++) {
                    for (int x = 0; x < ip.getWidth(); x++) {
                        variableSet.set("z", (double) index.getZ());
                        variableSet.set("c", (double) index.getC());
                        variableSet.set("t", (double) index.getT());
                        variableSet.set("x", (double) x);
                        variableSet.set("y", (double) y);
                        float value = ((Number) function.evaluate(variableSet)).floatValue();
                        ip.setf(x, y, value);
                    }
                }
            }, progressInfo);

            ImageJUtils.calibrate(img, ImageJCalibrationMode.AutomaticImageJ, 0, 0);

            iterationStep.addOutputData(outputSlot, new ImagePlusData(img), progressInfo);
            if (generateOnePerBatch)
                break;
        }
    }

    @SetJIPipeDocumentation(name = "Generate one per batch", description = "If enabled, only one image per batch is generated. " +
            "The first image is used as reference.")
    @JIPipeParameter("generate-one-per-batch")
    public boolean isGenerateOnePerBatch() {
        return generateOnePerBatch;
    }

    @JIPipeParameter("generate-one-per-batch")
    public void setGenerateOnePerBatch(boolean generateOnePerBatch) {
        this.generateOnePerBatch = generateOnePerBatch;
    }

    @SetJIPipeDocumentation(name = "Overwrite bit depth", description = "If none is selected, the output type is the same as the type " +
            "of the input image. Otherwise, the bit depth is set according to the selection.")
    @JIPipeParameter("overwrite-output-bit-depth")
    public OptionalBitDepth getOverwriteOutputBitDepth() {
        return overwriteOutputBitDepth;
    }

    @JIPipeParameter("overwrite-output-bit-depth")
    public void setOverwriteOutputBitDepth(OptionalBitDepth overwriteOutputBitDepth) {
        this.overwriteOutputBitDepth = overwriteOutputBitDepth;
    }

    @SetJIPipeDocumentation(name = "Function", description = "The function that is applied to each pixel. The expression should return a number.")
    @JIPipeParameter("function")
    @JIPipeExpressionParameterSettings(variableSource = PixelCoordinate5DExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getFunction() {
        return function;
    }

    @JIPipeParameter("function")
    public void setFunction(JIPipeExpressionParameter transformation) {
        this.function = transformation;
    }

    @SetJIPipeDocumentation(name = "Overwrite Width", description = "The width of the generated image")
    @JIPipeParameter("width")
    public OptionalIntegerParameter getOverwriteWidth() {
        return overwriteWidth;
    }

    @JIPipeParameter("width")
    public void setOverwriteWidth(OptionalIntegerParameter overwriteWidth) {
        this.overwriteWidth = overwriteWidth;
    }

    @SetJIPipeDocumentation(name = "Overwrite Height", description = "The height of the generated image")
    @JIPipeParameter("height")
    public OptionalIntegerParameter getOverwriteHeight() {
        return overwriteHeight;
    }

    @JIPipeParameter("height")
    public void setOverwriteHeight(OptionalIntegerParameter overwriteHeight) {
        this.overwriteHeight = overwriteHeight;
    }

    @SetJIPipeDocumentation(name = "Overwrite number of slices (Z)", description = "Number of generated Z slices.")
    @JIPipeParameter("size-z")
    public OptionalIntegerParameter getOverwriteSizeZ() {
        return overwriteSizeZ;
    }

    @JIPipeParameter("size-z")
    public void setOverwriteSizeZ(OptionalIntegerParameter overwriteSizeZ) {
        this.overwriteSizeZ = overwriteSizeZ;
    }

    @SetJIPipeDocumentation(name = "Overwrite number of channels (C)", description = "Number of generated channel slices.")
    @JIPipeParameter("size-c")
    public OptionalIntegerParameter getOverwriteSizeC() {
        return overwriteSizeC;
    }

    @JIPipeParameter("size-c")
    public void setOverwriteSizeC(OptionalIntegerParameter overwriteSizeC) {
        this.overwriteSizeC = overwriteSizeC;
    }

    @SetJIPipeDocumentation(name = "Overwrite number of frames (T)", description = "Number of generated frame slices.")
    @JIPipeParameter("size-t")
    public OptionalIntegerParameter getOverwriteSizeT() {
        return overwriteSizeT;
    }

    @JIPipeParameter("size-t")
    public void setOverwriteSizeT(OptionalIntegerParameter overwriteSizeT) {
        this.overwriteSizeT = overwriteSizeT;
    }
}
