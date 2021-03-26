package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.binary;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.util.LogicalOperation;

@JIPipeDocumentation(name = "Bitwise operation", description = "Combines two 8-bit images with a bitwise operation. You can use it to, for example combine two masks.")
@JIPipeOrganization(menuPath = "Binary", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input 1", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input 2", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input 1")
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData("Input 1", ImagePlusGreyscale8UData.class, progressInfo).getDuplicateImage();
        ImagePlus second = dataBatch.getInputData("Input 2", ImagePlusGreyscale8UData.class, progressInfo).getImage();
        if(!ImageJUtils.imagesHaveSameSize(img, second)) {
            throw new UserFriendlyRuntimeException("Input images do not have the same size!",
                    "Input images do not have the same size!",
                    getName(),
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channes.",
                    "Please check the input images.");
        }
        if(logicalOperation == LogicalOperation.LogicalAnd) {
            ImageJUtils.forEachIndexedZCTSlice(img, (ip1, index) -> {
                ImageProcessor ip2 = ImageJUtils.getSliceZero(second, index);
                byte[] pixels1 = (byte[]) ip1.getPixels();
                byte[] pixels2 = (byte[]) ip2.getPixels();
                for (int i = 0; i < pixels1.length; i++) {
                    pixels1[i] = (byte)((pixels1[i] & 0xff) & (pixels2[i] & 0xff));
                }
            }, progressInfo);
        }
        else if(logicalOperation == LogicalOperation.LogicalOr) {
            ImageJUtils.forEachIndexedZCTSlice(img, (ip1, index) -> {
                ImageProcessor ip2 = ImageJUtils.getSliceZero(second, index);
                byte[] pixels1 = (byte[]) ip1.getPixels();
                byte[] pixels2 = (byte[]) ip2.getPixels();
                for (int i = 0; i < pixels1.length; i++) {
                    pixels1[i] = (byte)((pixels1[i] & 0xff) | (pixels2[i] & 0xff));
                }
            }, progressInfo);
        }
        else if(logicalOperation == LogicalOperation.LogicalXor) {
            ImageJUtils.forEachIndexedZCTSlice(img, (ip1, index) -> {
                ImageProcessor ip2 = ImageJUtils.getSliceZero(second, index);
                byte[] pixels1 = (byte[]) ip1.getPixels();
                byte[] pixels2 = (byte[]) ip2.getPixels();
                for (int i = 0; i < pixels1.length; i++) {
                    pixels1[i] = (byte)((pixels1[i] & 0xff) ^ (pixels2[i] & 0xff));
                }
            }, progressInfo);
        }
        else {
            throw new UnsupportedOperationException();
        }
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Operation", description = "The operation that is applied to each pair of bits.")
    @JIPipeParameter("operation")
    public LogicalOperation getLogicalOperation() {
        return logicalOperation;
    }

    @JIPipeParameter("operation")
    public void setLogicalOperation(LogicalOperation logicalOperation) {
        this.logicalOperation = logicalOperation;
    }
}
