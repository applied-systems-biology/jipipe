/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.PixelCoordinate5DExpressionParameterVariableSource;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Generate vector field from math expression", description = "Generates a vector field by utilizing a math expression. " +
        "The expression must return an array of numbers (or scalar if there are only 1 components).")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class GenerateVectorFromMathExpression extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter function = new DefaultExpressionParameter("ARRAY(x / 3, x + y)");
    private int width = 256;
    private int height = 256;
    private int sizeZ = 1;
    private int sizeC = 2;
    private int sizeT = 1;
    private HyperstackDimension componentDimension = HyperstackDimension.Channel;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GenerateVectorFromMathExpression(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GenerateVectorFromMathExpression(GenerateVectorFromMathExpression other) {
        super(other);
        this.function = new DefaultExpressionParameter(other.function);
        this.width = other.width;
        this.height = other.height;
        this.sizeZ = other.sizeZ;
        this.sizeC = other.sizeC;
        this.sizeT = other.sizeT;
        this.componentDimension = other.componentDimension;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = IJ.createHyperStack("Generated", width, height, sizeC, sizeZ, sizeT, 32);
        ExpressionVariables variableSet = new ExpressionVariables();
        variableSet.set("width", width);
        variableSet.set("height", height);
        variableSet.set("num_z", sizeZ);
        variableSet.set("num_c", sizeC);
        variableSet.set("num_t", sizeT);

        if (componentDimension == HyperstackDimension.Channel) {
            int iterationIndex = 0;
            int outputVectorSize = sizeC;
            List<ImageProcessor> resultProcessors = new ArrayList<>();
            for (int t = 0; t < img.getNFrames(); t++) {
                for (int z = 0; z < img.getNSlices(); z++) {

                    progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("z=" + z + ", t=" + t);
                    // Get result processor
                    resultProcessors.clear();
                    for (int i = 0; i < outputVectorSize; i++) {
                        resultProcessors.add(img.getStack().getProcessor(img.getStackIndex(i + 1, z + 1, t + 1)));
                    }

                    variableSet.set("z", z);
                    variableSet.set("c", 0);
                    variableSet.set("t", t);

                    for (int y = 0; y < img.getHeight(); y++) {
                        for (int x = 0; x < img.getWidth(); x++) {
                            // Generate result
                            variableSet.set("x", (double) x);
                            variableSet.set("y", (double) y);
                            generateAndWriteVectorResults(variableSet, resultProcessors, y, x);
                        }
                    }
                }
            }
        } else if (componentDimension == HyperstackDimension.Depth) {
            int iterationIndex = 0;
            int outputVectorSize = sizeZ;
            List<ImageProcessor> resultProcessors = new ArrayList<>();
            for (int t = 0; t < img.getNFrames(); t++) {
                for (int c = 0; c < img.getNChannels(); c++) {

                    progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("c=" + c + ", t=" + t);
                    // Get result processor
                    resultProcessors.clear();
                    for (int i = 0; i < outputVectorSize; i++) {
                        resultProcessors.add(img.getStack().getProcessor(img.getStackIndex(c + 1, i + 1, t + 1)));
                    }

                    variableSet.set("z", 0);
                    variableSet.set("c", c);
                    variableSet.set("t", t);

                    for (int y = 0; y < img.getHeight(); y++) {
                        for (int x = 0; x < img.getWidth(); x++) {
                            // Generate result
                            variableSet.set("x", (double) x);
                            variableSet.set("y", (double) y);
                            generateAndWriteVectorResults(variableSet, resultProcessors, y, x);
                        }
                    }
                }
            }
        } else if (componentDimension == HyperstackDimension.Frame) {
            int iterationIndex = 0;
            int outputVectorSize = sizeC;
            List<ImageProcessor> resultProcessors = new ArrayList<>();
            for (int c = 0; c < img.getNChannels(); c++) {
                for (int z = 0; z < img.getNSlices(); z++) {

                    progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("z=" + z + ", c=" + c);
                    // Get result processor
                    resultProcessors.clear();
                    for (int i = 0; i < outputVectorSize; i++) {
                        resultProcessors.add(img.getStack().getProcessor(img.getStackIndex(c + 1, z + 1, i + 1)));
                    }

                    variableSet.set("z", z);
                    variableSet.set("c", c);
                    variableSet.set("t", 0);

                    for (int y = 0; y < img.getHeight(); y++) {
                        for (int x = 0; x < img.getWidth(); x++) {
                            // Generate result
                            variableSet.set("x", (double) x);
                            variableSet.set("y", (double) y);
                            generateAndWriteVectorResults(variableSet, resultProcessors, y, x);
                        }
                    }
                }
            }
        }

        ImageJUtils.calibrate(img, ImageJCalibrationMode.AutomaticImageJ, 0, 0);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    private void generateAndWriteVectorResults(ExpressionVariables variableSet, List<ImageProcessor> resultProcessors, int y, int x) {
        Object expressionResult = function.evaluate(variableSet);
        int outputVectorSize = resultProcessors.size();
        if (expressionResult instanceof List) {
            List<?> collection = (List<?>) expressionResult;
            for (int i = 0; i < outputVectorSize; i++) {
                resultProcessors.get(i).setf(x, y, ((Number) collection.get(i)).floatValue());
            }
        } else {
            if (outputVectorSize > 1)
                throw new IndexOutOfBoundsException("Expression only generated a scalar, but expected array of size " + outputVectorSize);
            // Write result
            resultProcessors.get(0).setf(x, y, ((Number) expressionResult).floatValue());
        }
    }

    @JIPipeDocumentation(name = "Function", description = "Generates a vector. Must return an array of numbers with the required vector components. " +
            "If the vector has one component, it can also return a number.")
    @JIPipeParameter("function")
    @ExpressionParameterSettings(variableSource = PixelCoordinate5DExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getFunction() {
        return function;
    }

    @JIPipeParameter("function")
    public void setFunction(DefaultExpressionParameter transformation) {
        this.function = transformation;
    }

    @JIPipeDocumentation(name = "Width", description = "The width of the generated image")
    @JIPipeParameter("width")
    public int getWidth() {
        return width;
    }

    @JIPipeParameter("width")
    public void setWidth(int width) {
        this.width = width;
    }

    @JIPipeDocumentation(name = "Height", description = "The height of the generated image")
    @JIPipeParameter("height")
    public int getHeight() {
        return height;
    }

    @JIPipeParameter("height")
    public void setHeight(int height) {
        this.height = height;
    }

    @JIPipeDocumentation(name = "Number of slices (Z)", description = "Number of generated Z slices.")
    @JIPipeParameter("size-z")
    public int getSizeZ() {
        return sizeZ;
    }

    @JIPipeParameter("size-z")
    public void setSizeZ(int sizeZ) {
        this.sizeZ = sizeZ;
    }

    @JIPipeDocumentation(name = "Number of channels (C)", description = "Number of generated channel slices.")
    @JIPipeParameter("size-c")
    public int getSizeC() {
        return sizeC;
    }

    @JIPipeParameter("size-c")
    public void setSizeC(int sizeC) {
        this.sizeC = sizeC;
    }

    @JIPipeDocumentation(name = "Number of frames (T)", description = "Number of generated frame slices.")
    @JIPipeParameter("size-t")
    public int getSizeT() {
        return sizeT;
    }

    @JIPipeParameter("size-t")
    public void setSizeT(int sizeT) {
        this.sizeT = sizeT;
    }

    @JIPipeDocumentation(name = "Component dimension", description = "Determines which dimension (Z, C, or T) contains the vector components.")
    @JIPipeParameter("component-dimension")
    public HyperstackDimension getComponentDimension() {
        return componentDimension;
    }

    @JIPipeParameter("component-dimension")
    public void setComponentDimension(HyperstackDimension componentDimension) {
        this.componentDimension = componentDimension;
    }
}
