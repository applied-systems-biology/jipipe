package org.hkijena.jipipe.extensions.cellpose.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseSizeModelData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;

@JIPipeDocumentation(name = "Import Cellpose size model", description = "Imports a Cellpose size model from a file")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
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
