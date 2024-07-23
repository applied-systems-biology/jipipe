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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.VectorPixel5DExpressionParameterVariablesInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Apply expression per pixel (vector)", description = "Applies a mathematical operation to each vector in the image. " +
        "One dimension is defined as the dimension that contains the vector components. For example, you can make the channel " +
        "the vector components. The algorithm then would iterate through all X, Y, Z, and T pixels and provide you with an array of " +
        "all channel pixel values for this position. The output is a scalar image.")
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nMath", aliasName = "Macro... (per pixel, vector)")
public class ApplyVectorMathExpression2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter transformation = new JIPipeExpressionParameter("x + y");
    private HyperstackDimension componentDimension = HyperstackDimension.Channel;
    private int outputVectorSize = 1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ApplyVectorMathExpression2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ApplyVectorMathExpression2DAlgorithm(ApplyVectorMathExpression2DAlgorithm other) {
        super(other);
        this.transformation = new JIPipeExpressionParameter(other.transformation);
        this.componentDimension = other.componentDimension;
        this.outputVectorSize = other.outputVectorSize;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        variableSet.set("width", inputData.getImage().getWidth());
        variableSet.set("height", inputData.getImage().getHeight());
        variableSet.set("num_z", inputData.getImage().getNSlices());
        variableSet.set("num_c", inputData.getImage().getNChannels());
        variableSet.set("num_t", inputData.getImage().getNFrames());
        variableSet.set("num_output_components", outputVectorSize);
        {
            ImagePlus img = inputData.getImage();
            ImagePlus result;
            switch (componentDimension) {
                case Channel:
                    result = IJ.createHyperStack("Result", img.getWidth(), img.getHeight(), outputVectorSize, img.getNSlices(), img.getNFrames(), img.getBitDepth());
                    break;
                case Depth:
                    result = IJ.createHyperStack("Result", img.getWidth(), img.getHeight(), img.getNChannels(), outputVectorSize, img.getNFrames(), img.getBitDepth());
                    break;
                case Frame:
                    result = IJ.createHyperStack("Result", img.getWidth(), img.getHeight(), img.getNChannels(), img.getNSlices(), outputVectorSize, img.getBitDepth());
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            if (componentDimension == HyperstackDimension.Channel) {
                int iterationIndex = 0;
                List<Double> vector = new ArrayList<>();
                List<ImageProcessor> resultProcessors = new ArrayList<>();
                for (int t = 0; t < img.getNFrames(); t++) {
                    for (int z = 0; z < img.getNSlices(); z++) {

                        progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("z=" + z + ", t=" + t);
                        // Get result processor
                        resultProcessors.clear();
                        for (int i = 0; i < outputVectorSize; i++) {
                            resultProcessors.add(result.getStack().getProcessor(result.getStackIndex(i + 1, z + 1, t + 1)));
                        }

                        variableSet.set("z", z);
                        variableSet.set("c", 0);
                        variableSet.set("t", t);

                        for (int y = 0; y < img.getHeight(); y++) {
                            for (int x = 0; x < img.getWidth(); x++) {

                                // Collect vector for this pixel
                                vector.clear();
                                for (int c = 0; c < img.getNChannels(); c++) {
                                    int index = img.getStackIndex(c + 1, z + 1, t + 1);

                                    ImageProcessor ip = img.getImageStack().getProcessor(index);
                                    double value = ip.getf(x, y);
                                    vector.add(value);
                                }

                                // Generate result
                                variableSet.set("x", (double) x);
                                variableSet.set("y", (double) y);
                                variableSet.set("vector", vector);
                                generateAndWriteVectorResults(variableSet, resultProcessors, y, x);
                            }
                        }
                    }
                }
            } else if (componentDimension == HyperstackDimension.Depth) {
                int iterationIndex = 0;
                List<Double> vector = new ArrayList<>();
                List<ImageProcessor> resultProcessors = new ArrayList<>();
                for (int t = 0; t < img.getNFrames(); t++) {
                    for (int c = 0; c < img.getNChannels(); c++) {

                        progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("c=" + c + ", t=" + t);

                        // Get result processor
                        resultProcessors.clear();
                        for (int i = 0; i < outputVectorSize; i++) {
                            resultProcessors.add(result.getStack().getProcessor(result.getStackIndex(c + 1, i + 1, t + 1)));
                        }

                        variableSet.set("z", 0);
                        variableSet.set("c", c);
                        variableSet.set("t", t);

                        for (int y = 0; y < img.getHeight(); y++) {
                            for (int x = 0; x < img.getWidth(); x++) {

                                // Collect vector for this pixel
                                vector.clear();
                                for (int z = 0; z < img.getNSlices(); z++) {
                                    int index = img.getStackIndex(c + 1, z + 1, t + 1);
                                    ImageProcessor ip = img.getImageStack().getProcessor(index);
                                    double value = ip.getf(x, y);
                                    vector.add(value);
                                }

                                // Generate result
                                variableSet.set("x", (double) x);
                                variableSet.set("y", (double) y);
                                variableSet.set("vector", vector);
                                generateAndWriteVectorResults(variableSet, resultProcessors, y, x);
                            }
                        }
                    }
                }
            } else if (componentDimension == HyperstackDimension.Frame) {
                int iterationIndex = 0;
                List<Double> vector = new ArrayList<>();
                List<ImageProcessor> resultProcessors = new ArrayList<>();
                for (int z = 0; z < img.getNSlices(); z++) {
                    for (int c = 0; c < img.getNChannels(); c++) {

                        progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("z=" + z + ", c=" + c);

                        // Get result processor
                        resultProcessors.clear();
                        for (int i = 0; i < outputVectorSize; i++) {
                            resultProcessors.add(result.getStack().getProcessor(result.getStackIndex(c + 1, z + 1, i + 1)));
                        }

                        variableSet.set("z", z);
                        variableSet.set("c", c);
                        variableSet.set("t", 0);

                        for (int y = 0; y < img.getHeight(); y++) {
                            for (int x = 0; x < img.getWidth(); x++) {

                                // Collect vector for this pixel
                                vector.clear();
                                for (int t = 0; t < img.getNFrames(); t++) {
                                    int index = img.getStackIndex(c + 1, z + 1, t + 1);
                                    ImageProcessor ip = img.getImageStack().getProcessor(index);
                                    double value = ip.getf(x, y);
                                    vector.add(value);
                                }

                                // Generate result
                                variableSet.set("x", (double) x);
                                variableSet.set("y", (double) y);
                                variableSet.set("vector", vector);
                                generateAndWriteVectorResults(variableSet, resultProcessors, y, x);
                            }
                        }
                    }
                }
            }

            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        }
    }

    private void generateAndWriteVectorResults(JIPipeExpressionVariablesMap variableSet, List<ImageProcessor> resultProcessors, int y, int x) {
        Object expressionResult = transformation.evaluate(variableSet);
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

    @SetJIPipeDocumentation(name = "Output vector size", description = "Determines how many slices in the selected component dimension are generated as output. " +
            "The expression must generate exactly as many vector components as this number.")
    @JIPipeParameter("output-vector-size")
    public int getOutputVectorSize() {
        return outputVectorSize;
    }

    @JIPipeParameter("output-vector-size")
    public void setOutputVectorSize(int outputVectorSize) {
        this.outputVectorSize = outputVectorSize;
    }

    @SetJIPipeDocumentation(name = "Function", description = "The function that is applied to each vector. The expression should return a number.")
    @JIPipeParameter("transformation-function")
    @JIPipeExpressionParameterSettings(variableSource = VectorPixel5DExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getTransformation() {
        return transformation;
    }

    @JIPipeParameter("transformation-function")
    public void setTransformation(JIPipeExpressionParameter transformation) {
        this.transformation = transformation;
    }

    @SetJIPipeDocumentation(name = "Vector component dimension", description = "The image dimension that contains the vector components.")
    @JIPipeParameter("component-dimension")
    public HyperstackDimension getComponentDimension() {
        return componentDimension;
    }

    @JIPipeParameter("component-dimension")
    public void setComponentDimension(HyperstackDimension componentDimension) {
        this.componentDimension = componentDimension;
    }
}
