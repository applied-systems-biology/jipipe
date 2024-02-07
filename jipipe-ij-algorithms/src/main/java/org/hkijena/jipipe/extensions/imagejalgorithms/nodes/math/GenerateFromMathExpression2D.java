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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.math;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.PixelCoordinate5DExpressionParameterVariablesInfo;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.ResourceUtils;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Generate image from math expression", description = "Applies a mathematical operation to each pixel. The value is written into the image.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Annotations", autoCreate = true, optional = true, description = "Optional annotations that can be referenced in the expression")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class GenerateFromMathExpression2D extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter function = new JIPipeExpressionParameter("x + y");
    private final CustomExpressionVariablesParameter customExpressionVariables;
    private int width = 256;
    private int height = 256;
    private int sizeZ = 1;
    private int sizeC = 1;
    private int sizeT = 1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GenerateFromMathExpression2D(JIPipeNodeInfo info) {
        super(info);
        this.customExpressionVariables = new CustomExpressionVariablesParameter();
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GenerateFromMathExpression2D(GenerateFromMathExpression2D other) {
        super(other);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(this);
        this.function = new JIPipeExpressionParameter(other.function);
        this.width = other.width;
        this.height = other.height;
        this.sizeZ = other.sizeZ;
        this.sizeC = other.sizeC;
        this.sizeT = other.sizeT;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = IJ.createHyperStack("Generated", width, height, sizeC, sizeZ, sizeT, 32);
        ExpressionVariables variableSet = new ExpressionVariables();
        variableSet.putAnnotations(iterationStep.getMergedTextAnnotations());
        variableSet.set("width", width);
        variableSet.set("height", height);
        variableSet.set("num_z", sizeZ);
        variableSet.set("num_c", sizeC);
        variableSet.set("num_t", sizeT);
        customExpressionVariables.writeToVariables(variableSet, true, "custom", true, "custom");

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

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Function", description = "The function that is applied to each pixel. The expression should return a number.")
    @JIPipeParameter("function")
    @JIPipeExpressionParameterSettings(hint = "per pixel")
    @JIPipeExpressionParameterVariable(fromClass = PixelCoordinate5DExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @JIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    public JIPipeExpressionParameter getFunction() {
        return function;
    }

    @JIPipeParameter("function")
    public void setFunction(JIPipeExpressionParameter transformation) {
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

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(custom, \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomExpressionVariables() {
        return customExpressionVariables;
    }
}
