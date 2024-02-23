package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.convert;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;

@SetJIPipeDocumentation(name = "Convert image to RGB", description = "Converts an image into RGB colors")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output", create = true)
@ConfigureJIPipeNode(menuPath = "Convert", nodeTypeCategory = ImagesNodeTypeCategory.class)
public class ConvertImageToRGBAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public ConvertImageToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertImageToRGBAlgorithm(ConvertImageToRGBAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(image), progressInfo);
    }
}
