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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.api.algorithm.JIPipeMergingAlgorithm.MERGING_ALGORITHM_DESCRIPTION;

/**
 * Removes a specified annotation
 */
@JIPipeDocumentation(name = "Convert to annotation table", description = "Converts data into an annotation table that contains " +
        "all annotations of the data row. The table contains a column 'data' that contains a string representation of the input data. " +
        "All other columns are generated based on the annotations. They have following structure: 'annotation:[annotation-id]' where the annotation id " +
        "is the unique identifier of this annotation type. You can find annotation types in the help menu." + "\n\n" + MERGING_ALGORITHM_DESCRIPTION)
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Converter)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = AnnotationTableData.class, slotName = "Output", autoCreate = true)
public class ConvertToAnnotationTable extends JIPipeMergingAlgorithm {

    private boolean removeOutputAnnotations = true;
    private String generatedColumn = "data";

    /**
     * @param info algorithm info
     */
    public ConvertToAnnotationTable(JIPipeNodeInfo info) {
        super(info);
        getDataBatchGenerationSettings().setDataSetMatching(JIPipeIteratingAlgorithm.ColumnMatching.Custom);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ConvertToAnnotationTable(ConvertToAnnotationTable other) {
        super(other);
        this.removeOutputAnnotations = other.removeOutputAnnotations;
        this.generatedColumn = other.generatedColumn;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Generated column").checkNonEmpty(generatedColumn, this);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Set<Integer> inputDataRows = dataInterface.getInputRows(getFirstInputSlot());

        AnnotationTableData output = new AnnotationTableData();
        int dataColumn = output.addColumn(generatedColumn, true);

        int row = 0;
        for (int sourceRow : inputDataRows) {
            output.addRow();
            output.setValueAt("" + getFirstInputSlot().getData(sourceRow, JIPipeData.class), row, dataColumn);
            for (JIPipeAnnotation trait : getFirstInputSlot().getAnnotations(sourceRow)) {
                if (trait != null) {
                    int col = output.addAnnotationColumn(trait.getName());
                    output.setValueAt(trait.getValue(), row, col);
                }
            }
            ++row;
        }

        if (removeOutputAnnotations)
            dataInterface.getAnnotations().clear();

        dataInterface.addOutputData(getFirstOutputSlot(), output);

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

    @JIPipeDocumentation(name = "Generated column", description = "The string representation of the data are stored in the column with this name")
    @JIPipeParameter("generated-column")
    @StringParameterSettings(monospace = true)
    public String getGeneratedColumn() {
        return generatedColumn;
    }

    @JIPipeParameter("generated-column")
    public void setGeneratedColumn(String generatedColumn) {
        this.generatedColumn = generatedColumn;
    }
}
