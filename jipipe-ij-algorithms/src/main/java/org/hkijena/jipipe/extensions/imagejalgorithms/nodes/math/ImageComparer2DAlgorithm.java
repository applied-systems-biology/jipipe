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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.math;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.graph.InputSlotMapParameterCollection;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Compare pixels 2D (where)", description = "Compares each pixel position and returns a 255 where the condition applies and 0 where the condition does not apply." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input 1", create = true, description = "The first operand")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input 2", create = true, description = "The second operand")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", create = true, description = "The calculation result")
public class ImageComparer2DAlgorithm extends JIPipeIteratingAlgorithm {

    private Operation operation = Operation.Equals;
    private boolean invert = false;
    private InputSlotMapParameterCollection operands;


    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ImageComparer2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        operands = new InputSlotMapParameterCollection(Operand.class, this, this::getNewOperand, false);
        operands.updateSlots();
        registerSubParameter(operands);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ImageComparer2DAlgorithm(ImageComparer2DAlgorithm other) {
        super(other);
        this.invert = other.invert;
        this.operation = other.operation;
        operands = new InputSlotMapParameterCollection(Operand.class, this, this::getNewOperand, false);
        other.operands.copyTo(operands);
        registerSubParameter(operands);
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

        ImagePlus result = IJ.createHyperStack("Result", leftOperand.getWidth(), leftOperand.getHeight(), leftOperand.getNChannels(), leftOperand.getNSlices(), leftOperand.getNFrames(), 8);

        ImagePlus finalLeftOperand = leftOperand;
        ImagePlus finalRightOperand = rightOperand;
        ImageJUtils.forEachIndexedZCTSlice(result, (resultIp, index) -> {
            ImageProcessor leftIp = ImageJUtils.getSliceZero(finalLeftOperand, index);
            ImageProcessor rightIp = ImageJUtils.getSliceZero(finalRightOperand, index);
            for (int y = 0; y < resultIp.getHeight(); y++) {
                for (int x = 0; x < resultIp.getWidth(); x++) {
                    boolean matches;
                    switch (operation) {
                        case Equals:
                            matches = leftIp.getf(x, y) == rightIp.getf(x, y);
                            break;
                        case LessThan:
                            matches = leftIp.getf(x, y) < rightIp.getf(x, y);
                            break;
                        case GreaterThan:
                            matches = leftIp.getf(x, y) > rightIp.getf(x, y);
                            break;
                        case LessThanOrEquals:
                            matches = leftIp.getf(x, y) <= rightIp.getf(x, y);
                            break;
                        case GreaterThanOrEquals:
                            matches = leftIp.getf(x, y) >= rightIp.getf(x, y);
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                    if (invert)
                        matches = !matches;
                    if (matches) {
                        resultIp.set(x, y, 255);
                    }
                }
            }
        }, progressInfo);

        result.copyAttributes(leftOperand);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result), progressInfo);
    }


    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
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

    @SetJIPipeDocumentation(name = "Invert results", description = "If enabled, the results are inverted")
    @JIPipeParameter("invert")
    public boolean isInvert() {
        return invert;
    }

    @JIPipeParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;
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

    /**
     * Available math operations
     */
    public enum Operation {
        Equals,
        LessThan,
        GreaterThan,
        LessThanOrEquals,
        GreaterThanOrEquals;


        @Override
        public String toString() {
            switch (this) {
                case Equals:
                    return "Equals (x = y)";
                case LessThan:
                    return "Less than (x < y)";
                case GreaterThan:
                    return "Greater than (x > y)";
                case LessThanOrEquals:
                    return "Less than or equals (x ≤ y)";
                case GreaterThanOrEquals:
                    return "Greater than or equals (x ≥ y)";
                default:
                    throw new UnsupportedOperationException();
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
