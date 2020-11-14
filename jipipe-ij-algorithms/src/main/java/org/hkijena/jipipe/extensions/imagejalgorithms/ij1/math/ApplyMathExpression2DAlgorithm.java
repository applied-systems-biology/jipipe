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
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Math expression", description = "Applies a mathematical operation to each pixel. " +
        "This node uses JIPipe's expression parameter instead of the functions integrated in ImageJ, which might have a lower performance.")
@JIPipeOrganization(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getDuplicateImage();
        StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
        variableSet.set("width", img.getWidth());
        variableSet.set("height", img.getHeight());
        ImageJUtils.forEachSlice(img, ip -> {
            for (int y = 0; y < ip.getHeight(); y++) {
                for (int x = 0; x < ip.getWidth(); x++) {
                    double value = ip.getf(x, y);
                    variableSet.set("x", (double) x);
                    variableSet.set("y", (double) y);
                    variableSet.set("value", value);
                    value = ((Number) transformation.evaluate(variableSet)).doubleValue();
                    ip.setf(x, y, (float) value);
                }
            }
        });
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }

    @JIPipeDocumentation(name = "Function", description = "The function that is applied to each pixel. The expression should return a number.")
    @JIPipeParameter("transformation-function")
    public DefaultExpressionParameter getTransformation() {
        return transformation;
    }

    @JIPipeParameter("transformation-function")
    public void setTransformation(DefaultExpressionParameter transformation) {
        this.transformation = transformation;

    }
}
