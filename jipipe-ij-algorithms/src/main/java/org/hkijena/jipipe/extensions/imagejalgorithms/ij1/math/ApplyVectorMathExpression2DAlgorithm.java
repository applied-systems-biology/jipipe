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

import com.fathzer.soft.javaluator.StaticVariableSet;
import gnu.trove.list.array.TDoubleArrayList;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.VectorPixel2DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Math expression (Vector)", description = "Applies a mathematical operation to each vector in the image. " +
        "One dimension is defined as the dimension that contains the vector components. For example, you can make the channel " +
        "the vector components. The algorithm then would iterate through all X, Y, Z, and T pixels and provide you with an array of " +
        "all channel pixel values for this position. The output is a scalar image.")
@JIPipeOrganization(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", autoCreate = true)
public class ApplyVectorMathExpression2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter transformation = new DefaultExpressionParameter("x + y");
    private HyperstackDimension componentDimension = HyperstackDimension.Channel;

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
        this.transformation = new DefaultExpressionParameter(other.transformation);
        this.componentDimension = other.componentDimension;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
        variableSet.set("width", inputData.getImage().getWidth());
        variableSet.set("height", inputData.getImage().getHeight());
        if (!inputData.getImage().isStack()) {
            ImagePlus img = inputData.getDuplicateImage();
            progressInfo.log("Info: Image is not a stack! Vector will contain the pixel value.");
            List<Double> vector = new ArrayList<>();
            ImageJUtils.forEachSlice(img, ip -> {
                for (int y = 0; y < ip.getHeight(); y++) {
                    for (int x = 0; x < ip.getWidth(); x++) {
                        vector.clear();
                        double value = ip.getf(x, y);
                        vector.add(value);
                        variableSet.set("x", (double) x);
                        variableSet.set("y", (double) y);
                        variableSet.set("vector", vector);
                        value = ((Number) transformation.evaluate(variableSet)).doubleValue();
                        ip.setf(x, y, (float) value);
                    }
                }
            }, progressInfo);
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
        } else {
            ImagePlus img = inputData.getImage();
            ImagePlus result;
            switch (componentDimension) {
                case Channel:
                    result = IJ.createHyperStack("Result", img.getWidth(), img.getHeight(), 1, img.getNSlices(), img.getNFrames(), img.getBitDepth());
                    break;
                case Depth:
                    result = IJ.createHyperStack("Result", img.getWidth(), img.getHeight(), img.getNChannels(), 1, img.getNFrames(), img.getBitDepth());
                    break;
                case Frame:
                    result = IJ.createHyperStack("Result", img.getWidth(), img.getHeight(), img.getNChannels(), img.getNSlices(), 1, img.getBitDepth());
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            if(componentDimension == HyperstackDimension.Channel) {
                int iterationIndex = 0;
                List<Double> vector = new ArrayList<>();
                for (int t = 0; t < img.getNFrames(); t++) {
                    for (int z = 0; z < img.getNSlices(); z++) {

                        progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("z=" + z + ", t=" + t);
                        // Get result processor
                        ImageProcessor resultProcessor = result.getStack().getProcessor(result.getStackIndex(1, z + 1, t + 1));

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
                                double value = ((Number) transformation.evaluate(variableSet)).doubleValue();

                                // Write result
                                resultProcessor.setf(x, y, (float)value);
                            }
                        }
                    }
                }
            }
            else if (componentDimension == HyperstackDimension.Depth) {
                int iterationIndex = 0;
                List<Double> vector = new ArrayList<>();
                for (int t = 0; t < img.getNFrames(); t++) {
                    for (int c = 0; c < img.getNChannels(); c++) {

                        progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("c=" + c + ", t=" + t);

                        // Get result processor
                        ImageProcessor resultProcessor = result.getStack().getProcessor(result.getStackIndex(c + 1, 1, t + 1));

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
                                double value = ((Number) transformation.evaluate(variableSet)).doubleValue();

                                // Write result
                                resultProcessor.setf(x, y, (float)value);
                            }
                        }
                    }
                }
            }
            else if(componentDimension == HyperstackDimension.Frame) {
                int iterationIndex = 0;
                List<Double> vector = new ArrayList<>();
                for (int z = 0; z < img.getNSlices(); z++) {
                    for (int c = 0; c < img.getNChannels(); c++) {

                        progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("z=" + z + ", c=" + c);

                        // Get result processor
                        ImageProcessor resultProcessor = result.getStack().getProcessor(result.getStackIndex(c + 1, z + 1, 1));

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
                                double value = ((Number) transformation.evaluate(variableSet)).doubleValue();

                                // Write result
                                resultProcessor.setf(x, y, (float)value);
                            }
                        }
                    }
                }
            }

            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Function", description = "The function that is applied to each vector. The expression should return a number.")
    @JIPipeParameter("transformation-function")
    @ExpressionParameterSettings(variableSource = VectorPixel2DExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getTransformation() {
        return transformation;
    }

    @JIPipeParameter("transformation-function")
    public void setTransformation(DefaultExpressionParameter transformation) {
        this.transformation = transformation;
    }

    @JIPipeDocumentation(name = "Vector component dimension", description = "The image dimension that contains the vector components.")
    @JIPipeParameter("component-dimension")
    public HyperstackDimension getComponentDimension() {
        return componentDimension;
    }

    @JIPipeParameter("component-dimension")
    public void setComponentDimension(HyperstackDimension componentDimension) {
        this.componentDimension = componentDimension;
    }
}
