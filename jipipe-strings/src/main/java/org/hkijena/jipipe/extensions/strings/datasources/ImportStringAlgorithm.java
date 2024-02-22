package org.hkijena.jipipe.extensions.strings.datasources;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.strings.StringData;

import java.io.IOException;
import java.nio.file.Files;

@SetJIPipeDocumentation(name = "Import text", description = "Imports a text/string from a file")
@DefineJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, slotName = "File", create = true)
@AddJIPipeOutputSlot(value = StringData.class, slotName = "Text", create = true)
public class ImportStringAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ImportStringAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportStringAlgorithm(ImportStringAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        FileData fileData = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        try {
            String data = new String(Files.readAllBytes(fileData.toPath()));
            iterationStep.addOutputData(getFirstOutputSlot(), new StringData(data), progressInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
