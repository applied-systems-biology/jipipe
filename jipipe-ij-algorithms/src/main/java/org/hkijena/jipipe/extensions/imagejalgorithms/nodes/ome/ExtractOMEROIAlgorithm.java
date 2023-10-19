package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.ome;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

@JIPipeDocumentation(name = "Extract OME ROI", description = "Extracts the ROI from an OME image")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = OMEImageData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class ExtractOMEROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ExtractOMEROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractOMEROIAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        OMEImageData inputData = iterationStep.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ROIListData(inputData.getRois()), progressInfo);
    }
}
