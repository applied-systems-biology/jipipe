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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.PixelCoordinate5DExpressionParameterVariablesInfo;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Generate vector field from math expression", description = "Generates a vector field by utilizing a math expression. " +
        "The expression must return an array of numbers (or scalar if there are only 1 components).")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Annotations", create = true, optional = true, description = "Optional annotations that can be referenced in the expression")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
public class GenerateVectorFromMathExpression extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter function = new JIPipeExpressionParameter("ARRAY(x / 3, x + y)");
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
        this.function = new JIPipeExpressionParameter(other.function);
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = IJ.createHyperStack("Generated", width, height, sizeC, sizeZ, sizeT, 32);
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        variableSet.putAnnotations(iterationStep.getMergedTextAnnotations());
        variableSet.set("width", width);
        variableSet.set("height", height);
        variableSet.set("num_z", sizeZ);
        variableSet.set("num_c", sizeC);
        variableSet.set("num_t", sizeT);
        variableSet.putCustomVariables(getDefaultCustomExpressionVariables());

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

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    private void generateAndWriteVectorResults(JIPipeExpressionVariablesMap variableSet, List<ImageProcessor> resultProcessors, int y, int x) {
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

    @SetJIPipeDocumentation(name = "Function", description = "Generates a vector. Must return an array of numbers with the required vector components. " +
            "If the vector has one component, it can also return a number.")
    @JIPipeParameter("function")
    @JIPipeExpressionParameterSettings(hint = "per pixel")
    @AddJIPipeExpressionParameterVariable(fromClass = PixelCoordinate5DExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public JIPipeExpressionParameter getFunction() {
        return function;
    }

    @JIPipeParameter("function")
    public void setFunction(JIPipeExpressionParameter transformation) {
        this.function = transformation;
    }

    @SetJIPipeDocumentation(name = "Width", description = "The width of the generated image")
    @JIPipeParameter("width")
    public int getWidth() {
        return width;
    }

    @JIPipeParameter("width")
    public void setWidth(int width) {
        this.width = width;
    }

    @SetJIPipeDocumentation(name = "Height", description = "The height of the generated image")
    @JIPipeParameter("height")
    public int getHeight() {
        return height;
    }

    @JIPipeParameter("height")
    public void setHeight(int height) {
        this.height = height;
    }

    @SetJIPipeDocumentation(name = "Number of slices (Z)", description = "Number of generated Z slices.")
    @JIPipeParameter("size-z")
    public int getSizeZ() {
        return sizeZ;
    }

    @JIPipeParameter("size-z")
    public void setSizeZ(int sizeZ) {
        this.sizeZ = sizeZ;
    }

    @SetJIPipeDocumentation(name = "Number of channels (C)", description = "Number of generated channel slices.")
    @JIPipeParameter("size-c")
    public int getSizeC() {
        return sizeC;
    }

    @JIPipeParameter("size-c")
    public void setSizeC(int sizeC) {
        this.sizeC = sizeC;
    }

    @SetJIPipeDocumentation(name = "Number of frames (T)", description = "Number of generated frame slices.")
    @JIPipeParameter("size-t")
    public int getSizeT() {
        return sizeT;
    }

    @JIPipeParameter("size-t")
    public void setSizeT(int sizeT) {
        this.sizeT = sizeT;
    }

    @SetJIPipeDocumentation(name = "Component dimension", description = "Determines which dimension (Z, C, or T) contains the vector components.")
    @JIPipeParameter("component-dimension")
    public HyperstackDimension getComponentDimension() {
        return componentDimension;
    }

    @JIPipeParameter("component-dimension")
    public void setComponentDimension(HyperstackDimension componentDimension) {
        this.componentDimension = componentDimension;
    }
}
