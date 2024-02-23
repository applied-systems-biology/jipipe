package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.ome;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.strings.XMLData;

@SetJIPipeDocumentation(name = "Extract OME XML", description = "Extracts the OME metadata XML from an OME image")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = OMEImageData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = XMLData.class, slotName = "Output", create = true)
public class ExtractOMEXMLAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ExtractOMEXMLAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractOMEXMLAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEImageData inputData = iterationStep.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new XMLData(inputData.getMetadata().dumpXML()), progressInfo);
    }
}
