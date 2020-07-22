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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Math operation", description = "Applies a mathematical operation to each pixel. ")
@JIPipeOrganization(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class ApplyMath2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Transformation transformation = Transformation.Set;
    private double value = 0;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ApplyMath2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class);
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
        });
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @JIPipeDocumentation(name = "Function", description = "The function that is applied to each pixel.")
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
