/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.strings.nodes.text;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.strings.StringData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Convert table to CSV text", description = "Converts a table to a text in CSV format")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = StringData.class, name = "Output", create = true)
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
