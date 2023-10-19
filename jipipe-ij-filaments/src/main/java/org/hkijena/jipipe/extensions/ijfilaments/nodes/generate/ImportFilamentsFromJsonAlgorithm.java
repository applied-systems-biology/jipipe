package org.hkijena.jipipe.extensions.ijfilaments.nodes.generate;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.nio.file.Path;

@JIPipeDocumentation(name = "Import filaments from JSON", description = "Imports filaments from a JSON file")
@JIPipeInputSlot(value = FileData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ImportFilamentsFromJsonAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ImportFilamentsFromJsonAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportFilamentsFromJsonAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        Path path = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo).toPath();
        Filaments3DData graph = JsonUtils.readFromFile(path, Filaments3DData.class);
        iterationStep.addOutputData(getFirstOutputSlot(), graph, progressInfo);
    }
}
