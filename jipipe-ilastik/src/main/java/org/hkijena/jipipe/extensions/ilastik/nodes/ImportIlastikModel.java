package org.hkijena.jipipe.extensions.ilastik.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.ilastik.datatypes.IlastikModelData;

@JIPipeDocumentation(name = "Import Ilastik project", description = "Imports an *.ilp file into the workflow")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "Project file", description = "The project file",autoCreate = true)
@JIPipeOutputSlot(value = IlastikModelData.class, slotName = "Project", description = "The Ilastik project", autoCreate = true)
public class ImportIlastikModel extends JIPipeSimpleIteratingAlgorithm {
    public ImportIlastikModel(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportIlastikModel(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        FileData fileData = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        IlastikModelData modelData = new IlastikModelData(fileData.toPath());
        iterationStep.addOutputData(getFirstOutputSlot(), modelData, progressInfo);
    }
}
