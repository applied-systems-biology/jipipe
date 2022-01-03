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

package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeColumMatching;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;

import java.util.Set;

import static org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm.MERGING_ALGORITHM_DESCRIPTION;

/**
 * Removes a specified annotation
 */
@JIPipeDocumentation(name = "Convert to annotation table", description = "Converts data into an annotation table that contains " +
        "all annotations of the data row. You can also add a string representation of the data." + "\n\n" + MERGING_ALGORITHM_DESCRIPTION)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = AnnotationTableData.class, slotName = "Output", autoCreate = true)
public class ConvertToAnnotationTable extends JIPipeMergingAlgorithm {

    private boolean removeOutputAnnotations = false;
    private OptionalAnnotationNameParameter addDataToString = new OptionalAnnotationNameParameter("data_as_string", false);

    /**
     * @param info algorithm info
     */
    public ConvertToAnnotationTable(JIPipeNodeInfo info) {
        super(info);
        getDataBatchGenerationSettings().setColumnMatching(JIPipeColumMatching.Custom);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ConvertToAnnotationTable(ConvertToAnnotationTable other) {
        super(other);
        this.removeOutputAnnotations = other.removeOutputAnnotations;
        this.addDataToString = other.addDataToString;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        report.resolve("Add data as string").report(addDataToString);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Set<Integer> inputDataRows = dataBatch.getInputRows(getFirstInputSlot());

        AnnotationTableData output = new AnnotationTableData();
        int dataColumn = addDataToString.isEnabled() ? output.addColumn(addDataToString.getContent(), true) : -1;

        int row = 0;
        for (int sourceRow : inputDataRows) {
            output.addRow();
            if (dataColumn >= 0)
                output.setValueAt(getFirstInputSlot().getVirtualData(sourceRow).getStringRepresentation(), row, dataColumn);
            for (JIPipeAnnotation annotation : getFirstInputSlot().getAnnotations(sourceRow)) {
                if (annotation != null) {
                    int col = output.addAnnotationColumn(annotation.getName());
                    output.setValueAt(annotation.getValue(), row, col);
                }
            }
            ++row;
        }

        if (removeOutputAnnotations)
            dataBatch.getMergedAnnotations().clear();

        dataBatch.addOutputData(getFirstOutputSlot(), output, progressInfo);

    }

    @JIPipeDocumentation(name = "Remove output annotations", description = "If enabled, annotations are removed from the output.")
    @JIPipeParameter("remove-output-annotations")
    public boolean isRemoveOutputAnnotations() {
        return removeOutputAnnotations;
    }

    @JIPipeParameter("remove-output-annotations")
    public void setRemoveOutputAnnotations(boolean removeOutputAnnotations) {
        this.removeOutputAnnotations = removeOutputAnnotations;
    }

    @JIPipeDocumentation(name = "Add data as string", description = "Adds the string representation of data as string")
    @JIPipeParameter("add-data-as-string")
    public OptionalAnnotationNameParameter getAddDataToString() {
        return addDataToString;
    }

    @JIPipeParameter("add-data-as-string")
    public void setAddDataToString(OptionalAnnotationNameParameter addDataToString) {
        this.addDataToString = addDataToString;
    }
}
