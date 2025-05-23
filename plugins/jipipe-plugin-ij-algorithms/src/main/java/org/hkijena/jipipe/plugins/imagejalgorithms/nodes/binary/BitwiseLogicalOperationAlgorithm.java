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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.binary;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.util.LogicalOperation;

@SetJIPipeDocumentation(name = "Bitwise operation", description = "Combines two 8-bit images with a bitwise operation. You can use it to, for example combine two masks.")
@ConfigureJIPipeNode(menuPath = "Binary", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "Input 1", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "Input 2", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale8UData.class, name = "Output", create = true, inheritedSlot = "Input 1")
public class BitwiseLogicalOperationAlgorithm extends JIPipeIteratingAlgorithm {

    private LogicalOperation logicalOperation = LogicalOperation.LogicalOr;

    public BitwiseLogicalOperationAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public BitwiseLogicalOperationAlgorithm(BitwiseLogicalOperationAlgorithm other) {
        super(other);
        this.logicalOperation = other.logicalOperation;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData("Input 1", ImagePlusGreyscale8UData.class, progressInfo).getDuplicateImage();
        ImagePlus second = iterationStep.getInputData("Input 2", ImagePlusGreyscale8UData.class, progressInfo).getImage();
        if (!ImageJUtils.imagesHaveSameSize(img, second)) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }
        if (logicalOperation == LogicalOperation.LogicalAnd) {
            ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip1, index) -> {
                ImageProcessor ip2 = ImageJUtils.getSliceZero(second, index);
                byte[] pixels1 = (byte[]) ip1.getPixels();
                byte[] pixels2 = (byte[]) ip2.getPixels();
                for (int i = 0; i < pixels1.length; i++) {
                    pixels1[i] = (byte) ((pixels1[i] & 0xff) & (pixels2[i] & 0xff));
                }
            }, progressInfo);
        } else if (logicalOperation == LogicalOperation.LogicalOr) {
            ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip1, index) -> {
                ImageProcessor ip2 = ImageJUtils.getSliceZero(second, index);
                byte[] pixels1 = (byte[]) ip1.getPixels();
                byte[] pixels2 = (byte[]) ip2.getPixels();
                for (int i = 0; i < pixels1.length; i++) {
                    pixels1[i] = (byte) ((pixels1[i] & 0xff) | (pixels2[i] & 0xff));
                }
            }, progressInfo);
        } else if (logicalOperation == LogicalOperation.LogicalXor) {
            ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip1, index) -> {
                ImageProcessor ip2 = ImageJUtils.getSliceZero(second, index);
                byte[] pixels1 = (byte[]) ip1.getPixels();
                byte[] pixels2 = (byte[]) ip2.getPixels();
                for (int i = 0; i < pixels1.length; i++) {
                    pixels1[i] = (byte) ((pixels1[i] & 0xff) ^ (pixels2[i] & 0xff));
                }
            }, progressInfo);
        } else {
            throw new UnsupportedOperationException();
        }
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Operation", description = "The operation that is applied to each pair of bits.")
    @JIPipeParameter("operation")
    public LogicalOperation getLogicalOperation() {
        return logicalOperation;
    }

    @JIPipeParameter("operation")
    public void setLogicalOperation(LogicalOperation logicalOperation) {
        this.logicalOperation = logicalOperation;
    }
}
