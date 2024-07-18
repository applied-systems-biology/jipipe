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
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Legacy image calculator 2D (merge)", description = "Applies a mathematical operation between multiple images. The algorithm iteratively applies the operation to each image in the following order: " +
        "f( f( f( f(image 1, image 2), image 3), image 4), ... " +
        "The order is determined by how JIPipe calculates the merging and currently cannot be controlled." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true, description = "The input images")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true, description = "The calculation result")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process", aliasName = "Image Calculator... (multiple into one)")
@Deprecated
public class LegacyImageCalculator2DMergingAlgorithm extends JIPipeMergingAlgorithm {

    private LegacyImageCalculator2DAlgorithm.Operation operation = LegacyImageCalculator2DAlgorithm.Operation.Max;
    private boolean floatingPointOutput = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public LegacyImageCalculator2DMergingAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public LegacyImageCalculator2DMergingAlgorithm(LegacyImageCalculator2DMergingAlgorithm other) {
        super(other);
        this.operation = other.operation;
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<ImagePlus> operands = new ArrayList<>();

        for (ImagePlusData data : iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo)) {
            operands.add(data.getImage());
        }

        if (operands.isEmpty()) {
            return;
        }

        // Ensure same size
        if (!ImageJUtils.imagesHaveSameSize(operands)) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }

        // Make both of the inputs the same type
        if (floatingPointOutput) {
            for (int i = 0; i < operands.size(); i++) {
                operands.set(i, ImageJUtils.convertToGrayscale32FIfNeeded(operands.get(i)));
            }
        } else {
            operands = ImageJUtils.convertToConsensusBitDepthIfNeeded(operands);
        }

        ImagePlus result;

        if (operands.size() > 1) {
            result = operands.get(0);
            for (int i = 1; i < operands.size(); i++) {
                progressInfo.resolveAndLog("Merging", i - 1, operands.size() - 1);
                ImageCalculator calculator = new ImageCalculator();
                result = calculator.run(operation.getId() + " stack create", result, operands.get(i));
            }
        } else {
            result = operands.get(0);
        }

        result.copyScale(operands.get(0));
        result.setDimensions(operands.get(0).getNChannels(), operands.get(0).getNSlices(), operands.get(0).getNFrames());
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Function", description = "The function is applied to each pixel pair.")
    @JIPipeParameter("operation")
    public LegacyImageCalculator2DAlgorithm.Operation getOperation() {
        return operation;
    }

    @JIPipeParameter("operation")
    public void setOperation(LegacyImageCalculator2DAlgorithm.Operation operation) {
        this.operation = operation;

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


}
