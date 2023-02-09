package org.hkijena.jipipe.extensions.ijfilaments.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.nio.file.Path;

@JIPipeDocumentation(name = "Import filaments from JSON", description = "Imports filaments from a JSON file")
@JIPipeInputSlot(value = FileData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FilamentsData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ImportFilamentsFromJsonAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ImportFilamentsFromJsonAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportFilamentsFromJsonAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path path = dataBatch.getInputData(getFirstInputSlot(), FileData.class, progressInfo).toPath();
        FilamentsData graph = JsonUtils.readFromFile(path, FilamentsData.class);
        dataBatch.addOutputData(getFirstOutputSlot(), graph, progressInfo);
    }
}
