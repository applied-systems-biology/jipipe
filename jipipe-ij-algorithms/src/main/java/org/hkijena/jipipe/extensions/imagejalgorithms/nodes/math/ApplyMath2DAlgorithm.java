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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;


/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Math operation", description = "Applies a mathematical operation to each pixel. Supported operations are SET, ADD, SUBTRACT, MULTIPLY, DIVIDE, GAMMA, MINIMUM, MAXIMUM, LOGICAL OR, LOGICAL AND, and LOGICAL XOR")
@JIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nMath", aliasName = "Math operation (with scalar)")
public class ApplyMath2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Transformation transformation = Transformation.Set;
    private double value = 0;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ApplyMath2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ApplyMath2DAlgorithm(ApplyMath2DAlgorithm other) {
        super(other);
        this.transformation = other.transformation;
        this.value = other.value;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJUtils.forEachSlice(img, ip -> {
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
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Function", description = "The function that is applied to each pixel. " +
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

    @JIPipeDocumentation(name = "Value", description = "The second operand")
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
