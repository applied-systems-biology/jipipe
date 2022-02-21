package org.hkijena.jipipe.extensions.cellpose.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseSizeModelData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;

@JIPipeDocumentation(name = "Import Cellpose size model", description = "Imports a Cellpose size model from a file")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = CellPoseSizeModelData.class, slotName = "Output", autoCreate = true)
public class ImportCellPoseSizeModelAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ImportCellPoseSizeModelAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportCellPoseSizeModelAlgorithm(ImportCellPoseSizeModelAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FileData fileData = dataBatch.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new CellPoseSizeModelData(fileData.toPath()), progressInfo);
    }
}