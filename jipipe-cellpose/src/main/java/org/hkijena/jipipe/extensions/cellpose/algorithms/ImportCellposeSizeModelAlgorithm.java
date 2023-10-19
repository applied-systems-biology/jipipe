package org.hkijena.jipipe.extensions.cellpose.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellposeSizeModelData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;

@JIPipeDocumentation(name = "Import Cellpose size model", description = "Imports a Cellpose size model from a file")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = CellposeSizeModelData.class, slotName = "Output", autoCreate = true)
public class ImportCellposeSizeModelAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ImportCellposeSizeModelAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportCellposeSizeModelAlgorithm(ImportCellposeSizeModelAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        FileData fileData = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new CellposeSizeModelData(fileData.toPath()), progressInfo);
    }
}
