package org.hkijena.jipipe.extensions.strings.nodes.text;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.strings.StringData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@JIPipeDocumentation(name = "Convert table to CSV text", description = "Converts a table to a text in CSV format")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = StringData.class, slotName = "Output", autoCreate = true)
public class TableToCSVTextAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public TableToCSVTextAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TableToCSVTextAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputData = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        try {
            Path tempFile = Files.createTempFile("table-to-csv", ".csv");
            inputData.saveAsCSV(tempFile);
            String result = new String(Files.readAllBytes(tempFile));
            Files.delete(tempFile);
            dataBatch.addOutputData(getFirstOutputSlot(), new StringData(result), progressInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
