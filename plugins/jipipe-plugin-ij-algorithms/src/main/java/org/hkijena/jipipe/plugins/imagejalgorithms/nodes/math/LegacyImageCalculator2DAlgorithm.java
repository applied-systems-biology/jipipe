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
import ij.plugin.ImageCalculator;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.graph.InputSlotMapParameterCollection;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Legacy image calculator 2D", description = "Applies a mathematical operation between two images. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice. " +
        "Old implementation that directly uses the one provided by ImageJ. We recommend to use 'Fast image arithmetics' instead.")
@Deprecated
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input 1", create = true, description = "The first operand")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input 2", create = true, description = "The second operand")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true, description = "The calculation result")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process", aliasName = "Image Calculator...")
public class LegacyImageCalculator2DAlgorithm extends JIPipeIteratingAlgorithm {

    private Operation operation = Operation.Difference;
    private boolean floatingPointOutput = false;
    private InputSlotMapParameterCollection operands;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public LegacyImageCalculator2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        operands = new InputSlotMapParameterCollection(Operand.class, this, this::getNewOperand, false);
        operands.updateSlots();
        registerSubParameter(operands);
        setFloatingPointOutput(false);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public LegacyImageCalculator2DAlgorithm(LegacyImageCalculator2DAlgorithm other) {
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

    private Operand getNewOperand(JIPipeDataSlotInfo info) {
        for (Operand value : Operand.values()) {
            if (operands.getParameters().values().stream()
                    .noneMatch(parameterAccess -> parameterAccess.get(Operand.class) == value)) {
                return value;
            }
        }
        return Operand.LeftOperand;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus leftOperand = null;
        ImagePlus rightOperand = null;
        for (Map.Entry<String, JIPipeParameterAccess> entry : operands.getParameters().entrySet()) {
            Operand operand = entry.getValue().get(Operand.class);
            if (operand == Operand.LeftOperand) {
                leftOperand = iterationStep.getInputData(entry.getKey(), ImagePlusData.class, progressInfo).getImage();
            } else if (operand == Operand.RightOperand) {
                rightOperand = iterationStep.getInputData(entry.getKey(), ImagePlusData.class, progressInfo).getImage();
            }
        }

        if (leftOperand == null) {
            throw new NullPointerException("Left operand is null!");
        }
        if (rightOperand == null) {
            throw new NullPointerException("Right operand is null!");
        }

        // Ensure same size
        if (!ImageJUtils.imagesHaveSameSize(leftOperand, rightOperand)) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }

        // Make both of the inputs the same type
        if (floatingPointOutput) {
            leftOperand = ImageJUtils.convertToGrayscale32FIfNeeded(leftOperand);
            rightOperand = ImageJUtils.convertToGrayscale32FIfNeeded(rightOperand);
        } else {
            rightOperand = ImageJUtils.convertToBitDepthIfNeeded(rightOperand, leftOperand.getBitDepth());
        }

        ImageCalculator calculator = new ImageCalculator();
        ImagePlus img = calculator.run(operation.getId() + " stack create", leftOperand, rightOperand);
        img.copyScale(leftOperand);
        img.setDimensions(leftOperand.getNChannels(), leftOperand.getNSlices(), leftOperand.getNFrames());
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }


    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        Set<Operand> existing = new HashSet<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : operands.getParameters().entrySet()) {
            Operand operand = entry.getValue().get(Operand.class);
            if (operand == null) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new ParameterValidationReportContext(reportContext, this, "Operands", "operands"),
                        "Operand not selected!",
                        "Please ensure that all operands are selected"));
            }
            if (operand != null) {
                if (existing.contains(operand)) {
                    report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                            new ParameterValidationReportContext(reportContext, this, "Operands", "operands"),
                            "Duplicate operand assignment!",
                            "Operand '" + operand + "' is already assigned.",
                            "Please assign the other operand."));
                }
                existing.add(operand);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Function", description = "The function is applied to each pixel pair.")
    @JIPipeParameter("operation")
    public Operation getOperation() {
        return operation;
    }

    @JIPipeParameter("operation")
    public void setOperation(Operation operation) {
        this.operation = operation;

    }

    @SetJIPipeDocumentation(name = "Operands", description = "Determines which input image is which operand.")
    @JIPipeParameter("operand")
    public InputSlotMapParameterCollection getOperands() {
        return operands;
    }

    @SetJIPipeDocumentation(name = "Generate 32-bit floating point output", description = "Determines whether to keep the input data type or generate a 32-bit floating point output.")
    @JIPipeParameter(value = "floating-point-output", important = true)
    public boolean isFloatingPointOutput() {
        return floatingPointOutput;
    }

    @JIPipeParameter("floating-point-output")
    public void setFloatingPointOutput(boolean floatingPointOutput) {
        this.floatingPointOutput = floatingPointOutput;
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
                    return this.toString().toLowerCase(Locale.ROOT);
            }
        }
    }

    /**
     * Determines the order of input images
     */
    public enum Operand {
        LeftOperand,
        RightOperand;


        @Override
        public String toString() {
            switch (this) {
                case LeftOperand:
                    return "Left operand ([x] OP y)";
                case RightOperand:
                    return "Right operand (x OP [y])";
                default:
                    return super.toString();
            }
        }
    }
}
