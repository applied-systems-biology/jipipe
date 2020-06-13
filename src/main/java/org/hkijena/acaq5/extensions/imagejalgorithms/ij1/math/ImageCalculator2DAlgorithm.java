package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.math;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.acaq5.extensions.parameters.collections.InputSlotMapParameterCollection;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;
import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.TO_GRAYSCALE32F_CONVERSION;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@ACAQDocumentation(name = "Image calculator 2D", description = "Applies a mathematical operation between two images. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Math", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input 1")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input 2")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class ImageCalculator2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private Operation operation = Operation.Difference;
    private boolean floatingPointOutput = false;
    private InputSlotMapParameterCollection operands;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public ImageCalculator2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData leftOperand = null;
        ImagePlusData rightOperand = null;
        for (Map.Entry<String, ACAQParameterAccess> entry : operands.getParameters().entrySet()) {
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
        rightOperand = (ImagePlusData) ACAQDatatypeRegistry.getInstance().convert(rightOperand, leftOperand.getClass());

        ImageCalculator calculator = new ImageCalculator();
        ImagePlus img = calculator.run(operation.getId() + " stack create", leftOperand.getImage(), rightOperand.getImage());
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        Set<Operand> existing = new HashSet<>();
        for (Map.Entry<String, ACAQParameterAccess> entry : operands.getParameters().entrySet()) {
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

    @ACAQDocumentation(name = "Function", description = "The function is applied to each pixel pair.")
    @ACAQParameter("operation")
    public Operation getOperation() {
        return operation;
    }

    @ACAQParameter("operation")
    public void setOperation(Operation operation) {
        this.operation = operation;
        getEventBus().post(new ParameterChangedEvent(this, "operation"));
    }

    @ACAQDocumentation(name = "Operands", description = "Determines which input image is which operand.")
    @ACAQParameter("operand")
    public InputSlotMapParameterCollection getOperands() {
        return operands;
    }

    @ACAQDocumentation(name = "Generate 32-bit floating point output", description = "Determines whether to keep the input data type or generate a 32-bit floating point output.")
    @ACAQParameter("floating-point-output")
    public boolean isFloatingPointOutput() {
        return floatingPointOutput;
    }

    @ACAQParameter("floating-point-output")
    public void setFloatingPointOutput(boolean floatingPointOutput) {
        this.floatingPointOutput = floatingPointOutput;
        getEventBus().post(new ParameterChangedEvent(this, "floating-point-output"));
        ACAQSlotDefinition definition = getFirstOutputSlot().getDefinition();
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
