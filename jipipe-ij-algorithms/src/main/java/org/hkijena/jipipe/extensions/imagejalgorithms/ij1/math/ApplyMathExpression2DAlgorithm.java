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

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.GreyscalePixel5DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Math expression", description = "Applies a mathematical operation to each pixel.")
@JIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class ApplyMathExpression2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter transformation = new DefaultExpressionParameter("x + y");

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
        this.transformation = new DefaultExpressionParameter(other.transformation);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ExpressionVariables variableSet = new ExpressionVariables();
        variableSet.set("width", img.getWidth());
        variableSet.set("height", img.getHeight());
        variableSet.set("num_z", inputData.getImage().getNSlices());
        variableSet.set("num_c", inputData.getImage().getNChannels());
        variableSet.set("num_t", inputData.getImage().getNFrames());
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
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Function", description = "The function that is applied to each pixel. The expression should return a number.")
    @JIPipeParameter("transformation-function")
    @ExpressionParameterSettings(variableSource = GreyscalePixel5DExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getTransformation() {
        return transformation;
    }

    @JIPipeParameter("transformation-function")
    public void setTransformation(DefaultExpressionParameter transformation) {
        this.transformation = transformation;

    }
}
