package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.color;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;

@JIPipeDocumentation(name = "To RGB", description = "Converts the incoming image to RGB. This applies the LUT if any is set.")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors")
public class ToRGBAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusColorRGBData.class, "Input", ImageJAlgorithmsExtension.TO_COLOR_RGB_CONVERSION)
                .seal()
                .build());
    }

    public ToRGBAlgorithm(ToRGBAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo)
                .getDuplicateImage()), progressInfo);
    }
}
