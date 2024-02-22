package org.hkijena.jipipe.extensions.filesystem.datasources;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Run output folder", description = "Generates a path that points to the data output folder of the current run.")
@DefineJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = FolderData.class, slotName = "Output", create = true)
public class OutputFolderDataSource extends JIPipeAlgorithm {


    public OutputFolderDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public OutputFolderDataSource(OutputFolderDataSource other) {
        super(other);
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path storagePath = getFirstOutputSlot().getSlotStoragePath().getParent().getParent().getParent();
        getFirstOutputSlot().addData(new PathData(storagePath), JIPipeDataContext.create(this), progressInfo);
    }
}
