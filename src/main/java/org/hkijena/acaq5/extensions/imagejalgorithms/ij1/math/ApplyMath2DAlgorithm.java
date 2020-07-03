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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.math;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@ACAQDocumentation(name = "Math operation", description = "Applies a mathematical operation to each pixel. ")
@ACAQOrganization(menuPath = "Math", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class ApplyMath2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private Transformation transformation = Transformation.Set;
    private double value = 0;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public ApplyMath2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
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
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Function", description = "The function that is applied to each pixel.")
    @ACAQParameter("transformation-function")
    public Transformation getTransformation() {
        return transformation;
    }

    @ACAQParameter("transformation-function")
    public void setTransformation(Transformation transformation) {
        this.transformation = transformation;

    }

    @ACAQDocumentation(name = "Value", description = "The second operand")
    @ACAQParameter("value")
    public double getValue() {
        return value;
    }

    @ACAQParameter("value")
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
