package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.convert;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;

@JIPipeDocumentation(name = "Convert image", description = "Converts an image into another image type")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(menuPath = "Convert", nodeTypeCategory = ImagesNodeTypeCategory.class)
public class ConvertImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeDataInfoRef generatedImageType = new JIPipeDataInfoRef("imagej-imgplus-greyscale-8u");

    public ConvertImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertImageAlgorithm(ConvertImageAlgorithm other) {
        super(other);
        setGeneratedImageType(other.getGeneratedImageType());
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        iterationStep.addOutputData(getFirstOutputSlot(), JIPipe.createData(generatedImageType.getInfo().getDataClass(), image), progressInfo);
    }

    @JIPipeDocumentation(name = "Output image type", description = "The image type that is generated.")
    @JIPipeParameter("generated-image-type")
    @JIPipeDataParameterSettings(dataBaseClass = ImagePlusData.class)
    public JIPipeDataInfoRef getGeneratedImageType() {
        return generatedImageType;
    }

    @JIPipeParameter("generated-image-type")
    public void setGeneratedImageType(JIPipeDataInfoRef generatedImageType) {
        this.generatedImageType = generatedImageType;
        getFirstOutputSlot().setAcceptedDataType(generatedImageType.getInfo().getDataClass());
        emitNodeSlotsChangedEvent();
    }
}
