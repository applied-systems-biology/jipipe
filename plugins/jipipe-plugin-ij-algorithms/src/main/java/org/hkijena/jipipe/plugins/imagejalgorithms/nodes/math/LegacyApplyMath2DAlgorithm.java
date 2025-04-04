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

import ij.ImagePlus;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;


/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Legacy image math operation (scalar)", description = "Applies a mathematical operation to each pixel. Supported operations are SET, ADD, SUBTRACT, MULTIPLY, DIVIDE, GAMMA, MINIMUM, MAXIMUM, LOGICAL OR, LOGICAL AND, and LOGICAL XOR. " +
        "We recommend to use 'Fast image arithmetics' instead.")
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nMath", aliasName = "Math operation (with scalar)")
@Deprecated
public class LegacyApplyMath2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Transformation transformation = Transformation.Set;
    private double value = 0;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public LegacyApplyMath2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public LegacyApplyMath2DAlgorithm(LegacyApplyMath2DAlgorithm other) {
        super(other);
        this.transformation = other.transformation;
        this.value = other.value;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJIterationUtils.forEachSlice(img, ip -> {
            switch (transformation) {
                case Set:
                    ip.set(value);
                    break;
                case Add:
                    ip.add(value);
                    break;
                case Subtract:
                    ip.subtract(value);
                    break;
                case Multiply:
                    ip.multiply(value);
                    break;
                case Divide:
                    ip.multiply(1.0f / value);
                    break;
                case Gamma:
                    ip.gamma(value);
                    break;
                case Minimum:
                    ip.min(value);
                    break;
                case Maximum:
                    ip.max(value);
                    break;
                case LogicalOr:
                    ip.or((int) value);
                    break;
                case LogicalAnd:
                    ip.and((int) value);
                    break;
                case LogicalXOr:
                    ip.xor((int) value);
                    break;
            }
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Function", description = "The function that is applied to each pixel. " +
            "<ul>" +
            "<li>Set: Set all pixel values to the provided one</li>" +
            "<li>Add: Add the provided value to all pixel values</li>" +
            "<li>Subtract: Subtract the provided value from all pixel values</li>" +
            "<li>Multiply: Multiply all pixel values with the provided value</li>" +
            "<li>Divide: Divide all pixel values by the provided value</li>" +
            "<li>Gamma: Perform gamma correction (value is gamma)</li>" +
            "<li>Minimum: Pixel values less than the provided value are set to the value</li>" +
            "<li>Maximum: Pixel values larger than the provided value are set to the value</li>" +
            "<li>Logical OR: Apply binary OR operation on pixels with value (value is converted to integer)</li>" +
            "<li>Logical AND: Apply binary AND operation on pixels with value (value is converted to integer)</li>" +
            "<li>Logical XOR: Apply binary XOR operation on pixels with value (value is converted to integer)</li>" +
            "</ul>")
    @JIPipeParameter("transformation-function")
    public Transformation getTransformation() {
        return transformation;
    }

    @JIPipeParameter("transformation-function")
    public void setTransformation(Transformation transformation) {
        this.transformation = transformation;

    }

    @SetJIPipeDocumentation(name = "Value", description = "The second operand")
    @JIPipeParameter("value")
    public double getValue() {
        return value;
    }

    @JIPipeParameter("value")
    public void setValue(double value) {
        this.value = value;

    }

    /**
     * Available transformation functions
     */
    public enum Transformation {
        Set,
        Add,
        Subtract,
        Multiply,
        Divide,
        Gamma,
        Minimum,
        Maximum,
        LogicalAnd,
        LogicalOr,
        LogicalXOr
    }
}
