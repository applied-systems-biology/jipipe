package org.hkijena.jipipe.extensions.cellpose.algorithms;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;

@SetJIPipeDocumentation(name = "Import Cellpose model", description = "Imports a Cellpose model from a file")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = CellposeModelData.class, slotName = "Output", create = true)
public class ImportCellposeModelAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ImportCellposeModelAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportCellposeModelAlgorithm(ImportCellposeModelAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        FileData fileData = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new CellposeModelData(fileData.toPath()), progressInfo);
    }
}
