package org.hkijena.jipipe.extensions.cellpose.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseModelData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;

@JIPipeDocumentation(name = "Import Cellpose model", description = "Imports a Cellpose model from a file")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
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
