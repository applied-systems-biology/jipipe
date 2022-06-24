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

package org.hkijena.jipipe.extensions.annotation.datasources;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.io.IOException;

/**
 * Imports {@link AnnotationTableData} from a file
 */
@JIPipeDocumentation(name = "Import annotation table")
@JIPipeInputSlot(value = FileData.class, slotName = "Files", autoCreate = true)
@JIPipeOutputSlot(value = AnnotationTableData.class, slotName = "Annotation table", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class AnnotationTableFromFile extends JIPipeSimpleIteratingAlgorithm {

    /**
     * @param info algorithm info
     */
    public AnnotationTableFromFile(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public AnnotationTableFromFile(AnnotationTableFromFile other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FileData fileData = dataBatch.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        ResultsTableData resultsTableData = ResultsTableData.fromCSV(fileData.toPath());
        dataBatch.addOutputData(getFirstOutputSlot(), new AnnotationTableData(resultsTableData), progressInfo);
    }
}
