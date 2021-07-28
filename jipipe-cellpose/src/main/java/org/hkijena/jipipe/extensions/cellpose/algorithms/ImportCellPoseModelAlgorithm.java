package org.hkijena.jipipe.extensions.cellpose.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseModelData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;

@JIPipeDocumentation(name = "Import Cellpose model", description = "Imports a Cellpose model from a file")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = CellPoseModelData.class, slotName = "Output", autoCreate = true)
public class ImportCellPoseModelAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ImportCellPoseModelAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportCellPoseModelAlgorithm(ImportCellPoseModelAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FileData fileData = dataBatch.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new CellPoseModelData(fileData.toPath()), progressInfo);
    }
}
