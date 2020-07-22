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

package org.hkijena.jipipe.extensions.tables.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Add annotations as columns", description = "Adds column annotations to the table as new columns.")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Append")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class AddAnnotationColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private String annotationPrefix = "annotation:";

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public AddAnnotationColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public AddAnnotationColumnsAlgorithm(AddAnnotationColumnsAlgorithm other) {
        super(other);
        this.annotationPrefix = other.annotationPrefix;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData resultsTableData = new ResultsTableData(dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class));
        for (Map.Entry<String, JIPipeAnnotation> entry : dataBatch.getAnnotations().entrySet()) {
            int col = resultsTableData.addColumn(annotationPrefix + entry.getKey(), true);
            for (int row = 0; row < resultsTableData.getRowCount(); row++) {
                resultsTableData.setValueAt(entry.getValue() != null ? "" + entry.getValue().getValue() : "", row, col);
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), resultsTableData);
    }

    @JIPipeDocumentation(name = "Annotation prefix", description = "Prefix added to columns generated from data annotations.")
    @JIPipeParameter("annotation-prefix")
    public String getAnnotationPrefix() {
        return annotationPrefix;
    }

    @JIPipeParameter("annotation-prefix")
    public void setAnnotationPrefix(String annotationPrefix) {
        this.annotationPrefix = annotationPrefix;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }
}
