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

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.GreyscalePixel5DExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Math expression", description = "Applies a mathematical operation to each pixel.")
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nMath", aliasName = "Macro... (per pixel, greyscale)")
public class ApplyMathExpression2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter transformation = new JIPipeExpressionParameter("x + y");

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ApplyMathExpression2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ApplyMathExpression2DAlgorithm(ApplyMathExpression2DAlgorithm other) {
        super(other);
        this.transformation = new JIPipeExpressionParameter(other.transformation);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
            variableSet.set(annotation.getName(), annotation.getValue());
        }
        variableSet.set("width", img.getWidth());
        variableSet.set("height", img.getHeight());
        variableSet.set("num_z", inputData.getImage().getNSlices());
        variableSet.set("num_c", inputData.getImage().getNChannels());
        variableSet.set("num_t", inputData.getImage().getNFrames());
        variableSet.putCustomVariables(getDefaultCustomExpressionVariables());

        ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
            for (int y = 0; y < ip.getHeight(); y++) {
                for (int x = 0; x < ip.getWidth(); x++) {
                    double value = ip.getf(x, y);
                    variableSet.set("z", index.getZ());
                    variableSet.set("c", index.getC());
                    variableSet.set("t", index.getT());
                    variableSet.set("x", (double) x);
                    variableSet.set("y", (double) y);
                    variableSet.set("value", value);
                    value = ((Number) transformation.evaluate(variableSet)).doubleValue();
                    ip.setf(x, y, (float) value);
                }
            }
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Function", description = "The function that is applied to each pixel. The expression should return a number.")
    @JIPipeParameter("transformation-function")
    @JIPipeExpressionParameterSettings(variableSource = GreyscalePixel5DExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public JIPipeExpressionParameter getTransformation() {
        return transformation;
    }

    @JIPipeParameter("transformation-function")
    public void setTransformation(JIPipeExpressionParameter transformation) {
        this.transformation = transformation;

    }
    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
