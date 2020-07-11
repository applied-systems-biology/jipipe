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
import ij.plugin.ImageCalculator;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotDefinition;
import org.hkijena.jipipe.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.parameters.collections.InputSlotMapParameterCollection;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;
import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.TO_GRAYSCALE32F_CONVERSION;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Image calculator 2D", description = "Applies a mathematical operation between two images. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Math", algorithmCategory = JIPipeAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input 1")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input 2")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class ImageCalculator2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Operation operation = Operation.Difference;
    private boolean floatingPointOutput = false;
    private InputSlotMapParameterCollection operands;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public ImageCalculator2DAlgorithm(JIPipeAlgorithmDeclaration declaration) {
        super(declaration, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input 1", ImagePlusData.class)
                .addInputSlot("Input 2", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input 1")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
        operands = new InputSlotMapParameterCollection(Operand.class, this, this::getNewOperand, false);
        operands.updateSlots();
        registerSubParameter(operands);
        setFloatingPointOutput(false);
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public ImageCalculator2DAlgorithm(ImageCalculator2DAlgorithm other) {
        super(other);
        this.operation = other.operation;
        operands = new InputSlotMapParameterCollection(Operand.class, this, this::getNewOperand, false);
        other.operands.copyTo(operands);
        registerSubParameter(operands);
        this.setFloatingPointOutput(other.floatingPointOutput);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    private Operand getNewOperand() {
        for (Operand value : Operand.values()) {
            if (operands.getParameters().values().stream()
                    .noneMatch(parameterAccess -> parameterAccess.get(Operand.class) == value)) {
                return value;
            }
        }
        return Operand.LeftOperand;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData leftOperand = null;
        ImagePlusData rightOperand = null;
        for (Map.Entry<String, JIPipeParameterAccess> entry : operands.getParameters().entrySet()) {
            Operand operand = entry.getValue().get(Operand.class);
            if (operand == Operand.LeftOperand) {
                leftOperand = dataInterface.getInputData(entry.getKey(), ImagePlusData.class);
            } else if (operand == Operand.RightOperand) {
                rightOperand = dataInterface.getInputData(entry.getKey(), ImagePlusData.class);
            }
        }

        if (leftOperand == null) {
            throw new NullPointerException("Left operand is null!");
        }
        if (rightOperand == null) {
            throw new NullPointerException("Right operand is null!");
        }

        // Make both of the inputs the same type
        rightOperand = (ImagePlusData) JIPipeDatatypeRegistry.getInstance().convert(rightOperand, leftOperand.getClass());

        ImageCalculator calculator = new ImageCalculator();
        ImagePlus img = calculator.run(operation.getId() + " stack create", leftOperand.getImage(), rightOperand.getImage());
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
        Set<Operand> existing = new HashSet<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : operands.getParameters().entrySet()) {
            Operand operand = entry.getValue().get(Operand.class);
            report.forCategory("Operands").forCategory(entry.getKey()).checkNonNull(operand, this);
            if (operand != null) {
                if (existing.contains(operand))
                    report.forCategory("Operands").forCategory(entry.getKey()).reportIsInvalid("Duplicate operand assignment!",
                            "Operand '" + operand + "' is already assigned.",
                            "Please assign the other operand.",
                            this);
                existing.add(operand);
            }
        }
    }

    @JIPipeDocumentation(name = "Function", description = "The function is applied to each pixel pair.")
    @JIPipeParameter("operation")
    public Operation getOperation() {
        return operation;
    }

    @JIPipeParameter("operation")
    public void setOperation(Operation operation) {
        this.operation = operation;

    }

    @JIPipeDocumentation(name = "Operands", description = "Determines which input image is which operand.")
    @JIPipeParameter("operand")
    public InputSlotMapParameterCollection getOperands() {
        return operands;
    }

    @JIPipeDocumentation(name = "Generate 32-bit floating point output", description = "Determines whether to keep the input data type or generate a 32-bit floating point output.")
    @JIPipeParameter("floating-point-output")
    public boolean isFloatingPointOutput() {
        return floatingPointOutput;
    }

    @JIPipeParameter("floating-point-output")
    public void setFloatingPointOutput(boolean floatingPointOutput) {
        this.floatingPointOutput = floatingPointOutput;

        JIPipeSlotDefinition definition = getFirstOutputSlot().getDefinition();
        if (floatingPointOutput) {
            getFirstOutputSlot().setAcceptedDataType(ImagePlusGreyscale32FData.class);
            definition.setInheritanceConversionsFromRaw(TO_GRAYSCALE32F_CONVERSION);
        } else {
            getFirstOutputSlot().setAcceptedDataType(ImagePlusData.class);
            definition.setInheritanceConversionsFromRaw(REMOVE_MASK_QUALIFIER);
        }
        getEventBus().post(new AlgorithmSlotsChangedEvent(this));
        updateSlotInheritance();
    }

    /**
     * Available math operations
     */
    public enum Operation {
        Add, Subtract, Multiply, Divide, AND, OR, XOR, Min, Max, Average, Difference, Copy, TransparentZero;

        public String getId() {
            switch (this) {
                case TransparentZero:
                    return "zero";
                case Difference:
                    return "diff";
                case Average:
                    return "ave";
                case Subtract:
                    return "sub";
                case Divide:
                    return "div";
                default:
                    return this.toString().toLowerCase();
            }
        }
    }

    /**
     * Determines the order of input images
     */
    public enum Operand {
        LeftOperand,
        RightOperand
    }
}
