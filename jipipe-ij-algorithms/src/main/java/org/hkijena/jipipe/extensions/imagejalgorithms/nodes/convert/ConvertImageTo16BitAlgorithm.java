package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.convert;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;

@SetJIPipeDocumentation(name = "Convert image to 16-bit", description = "Converts an image into 16-bit")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale16UData.class, slotName = "Output", create = true)
@ConfigureJIPipeNode(menuPath = "Convert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nType", aliasName = "16-bit")
public class ConvertImageTo16BitAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public ConvertImageTo16BitAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertImageTo16BitAlgorithm(ConvertImageTo16BitAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale16UData(image), progressInfo);
    }
}
