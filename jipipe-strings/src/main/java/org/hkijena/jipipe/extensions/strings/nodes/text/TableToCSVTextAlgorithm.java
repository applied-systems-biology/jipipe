package org.hkijena.jipipe.extensions.strings.nodes.text;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.strings.StringData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Convert table to CSV text", description = "Converts a table to a text in CSV format")
@DefineJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = StringData.class, slotName = "Output", create = true)
public class TableToCSVTextAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public TableToCSVTextAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TableToCSVTextAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputData = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        try {
            Path tempFile = Files.createTempFile("table-to-csv", ".csv");
            inputData.saveAsCSV(tempFile);
            String result = new String(Files.readAllBytes(tempFile));
            Files.delete(tempFile);
            iterationStep.addOutputData(getFirstOutputSlot(), new StringData(result), progressInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
