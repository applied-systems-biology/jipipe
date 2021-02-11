/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.datasources;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.io.IOException;

/**
 * Imports {@link ResultsTableData} from a file
 */
@JIPipeDocumentation(name = "Results table from file")
@JIPipeInputSlot(value = FileData.class, slotName = "Files", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Results table", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ResultsTableFromFile extends JIPipeSimpleIteratingAlgorithm {

    /**
     * @param info algorithm info
     */
    public ResultsTableFromFile(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ResultsTableFromFile(ResultsTableFromFile other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FileData fileData = dataBatch.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        try {
            ResultsTable resultsTable = ResultsTable.open(fileData.getPath().toString());
            dataBatch.addOutputData(getFirstOutputSlot(), new ResultsTableData(resultsTable), progressInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
