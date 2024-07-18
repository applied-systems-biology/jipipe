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
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.BitDepth;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.OptionalBitDepth;
import org.hkijena.jipipe.utils.scripting.MacroUtils;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Fast image arithmetics", description = "Applies standard arithmetic and logical operations including, addition, subtraction, division, multiplication, GAMMA, EXP, LOG, SQR, SQRT, ABS, AND, OR, XOR, minimum, maximum.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output")
public class FastImageArithmeticsAlgorithm extends JIPipeIteratingAlgorithm {

    private OptionalBitDepth bitDepth = OptionalBitDepth.Grayscale32f;
    private JIPipeExpressionParameter expression = new JIPipeExpressionParameter("I1 + I2");

    public FastImageArithmeticsAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputTo(ImagePlusGreyscaleData.class)
                .addInputSlot("I1", "", ImagePlusGreyscaleData.class, true)
                .addInputSlot("I2", "", ImagePlusGreyscaleData.class, true)
                .addOutputSlot("Output", "", ImagePlusGreyscaleData.class)
                .sealOutput()
                .build());
    }

    public FastImageArithmeticsAlgorithm(FastImageArithmeticsAlgorithm other) {
        super(other);
        this.bitDepth = other.bitDepth;
        this.expression = new JIPipeExpressionParameter(other.expression);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        // Collect all input images
        Map<String, ImagePlus> inputImagesMap = new HashMap<>();
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            ImagePlusData image = iterationStep.getInputData(inputSlot, ImagePlusGreyscaleData.class, progressInfo);
            if(image != null) {
                inputImagesMap.put(inputSlot.getName(), image.getImage());
            }
        }

        // Check if the images have the same size
        if(!ImageJUtils.imagesHaveSameSize(inputImagesMap.values())) {
            throw new IllegalArgumentException("Input images do not have the same size.");
        }

        // Convert inputs to target bit-depth
        {
            int targetBitDepth;

            if (bitDepth == OptionalBitDepth.None) {
                targetBitDepth = ImageJUtils.getConsensusBitDepth(inputImagesMap.values());
            } else {
                targetBitDepth = bitDepth.getBitDepth();
                if(targetBitDepth == 24) {
                    targetBitDepth = 32;
                }
            }

            progressInfo.log("Target bit-depth: " + targetBitDepth);

            for (String key : inputImagesMap.keySet()) {
                ImagePlus imagePlus = ImageJUtils.convertToBitDepthIfNeeded(inputImagesMap.get(key), targetBitDepth);
                inputImagesMap.put(key, imagePlus);
            }
        }

    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            if(!MacroUtils.isValidVariableName(inputSlot.getName())) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        reportContext,
                        "Invalid input name: " + inputSlot.getName(),
                        "The name of the input slot is not allowed.",
                        "Use only alphanumeric input slot names without spaces."));
            }
        }
    }

    @SetJIPipeDocumentation(name = "Bit depth", description = "Allows to force a specific bit depth. If 'None' is selected, JIPipe automatically chooses the highest bit depth based on the input. " +
            "Please note that 'RGB' will be handled as 32-bit float, as the operations do not support RGB pixel types.")
    @JIPipeParameter("bit-depth")
    public OptionalBitDepth getBitDepth() {
        return bitDepth;
    }

    @JIPipeParameter("bit-depth")
    public void setBitDepth(OptionalBitDepth bitDepth) {
        this.bitDepth = bitDepth;
    }

    @SetJIPipeDocumentation(name = "Expression", description = "The math expression that calculates the output. Applied per pixel. Please note that most standard JIPipe expression functions are not available. Available variables and operations: " +
            "<ul>" +
            "<li>Input slot names reference the pixel value at the current coordinate</li>" +
            "<li>x, y, z, c, t will point to the current location of the pixel</li>" +
            "<li>Numeric constants like 0.15 can be used</li>" +
            "<li>You can use brackets ( ) to ensure the correct order</li>" +
            "<li>[] + [] to add the pixel values</li>" +
            "<li>[] - [] to subtract the pixel values</li>" +
            "<li>[] * [] to multiply the pixel values</li>" +
            "<li>[] / [] to divide the pixel values</li>" +
            "<li>MIN([], []) to calculate the minimum of the operands</li>" +
            "<li>MAX([], []) to calculate the maximum of the operands</li>" +
            "<li>SQR([]) to calculate the square of the operand</li>" +
            "<li>SQRT([]) to calculate the square root of the operand</li>" +
            "<li>EXP([]) to calculate the e^operand</li>" +
            "<li>LN([]) to calculate the LN(operand)</li>" +
            "</ul>")
    @JIPipeParameter("expression")
    @JIPipeExpressionParameterSettings(withoutEditorButton = true)
    public JIPipeExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(JIPipeExpressionParameter expression) {
        this.expression = expression;
    }
}
