package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.ome;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.strings.XMLData;

@JIPipeDocumentation(name = "Extract OME XML", description = "Extracts the OME metadata XML from an OME image")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = OMEImageData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = XMLData.class, slotName = "Output", autoCreate = true)
public class ExtractOMEXMLAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ExtractOMEXMLAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractOMEXMLAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        OMEImageData inputData = dataBatch.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new XMLData(inputData.getMetadata().dumpXML()), progressInfo);
    }
}
