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

package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQMergingAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQMergingDataBatch;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.annotation.datatypes.AnnotationTableData;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.api.algorithm.ACAQMergingAlgorithm.MERGING_ALGORITHM_DESCRIPTION;

/**
 * Removes a specified annotation
 */
@ACAQDocumentation(name = "Convert to annotation table", description = "Converts data into an annotation table that contains " +
        "all annotations of the data row. The table contains a column 'data' that contains a string representation of the input data. " +
        "All other columns are generated based on the annotations. They have following structure: 'annotation:[annotation-id]' where the annotation id " +
        "is the unique identifier of this annotation type. You can find annotation types in the help menu." + "\n\n" + MERGING_ALGORITHM_DESCRIPTION)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Converter)
@AlgorithmInputSlot(value = ACAQData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = AnnotationTableData.class, slotName = "Output", autoCreate = true)
public class ConvertToAnnotationTable extends ACAQMergingAlgorithm {

    private boolean removeOutputAnnotations = true;
    private String generatedColumn = "data";

    /**
     * @param declaration algorithm declaration
     */
    public ConvertToAnnotationTable(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        setDataSetMatching(ACAQIteratingAlgorithm.ColumnMatching.Custom);
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
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Generated column").checkNonEmpty(generatedColumn, this);
    }

    @Override
    protected void runIteration(ACAQMergingDataBatch dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Set<Integer> inputDataRows = dataInterface.getInputRows(getFirstInputSlot());

        AnnotationTableData output = new AnnotationTableData();
        int dataColumn = output.addColumn(generatedColumn, true);

        int row = 0;
        for (int sourceRow : inputDataRows) {
            output.addRow();
            output.setValueAt("" + getFirstInputSlot().getData(sourceRow, ACAQData.class), row, dataColumn);
            for (ACAQAnnotation trait : getFirstInputSlot().getAnnotations(sourceRow)) {
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

    @ACAQDocumentation(name = "Remove output annotations", description = "If enabled, annotations are removed from the output.")
    @ACAQParameter("remove-output-annotations")
    public boolean isRemoveOutputAnnotations() {
        return removeOutputAnnotations;
    }

    @ACAQParameter("remove-output-annotations")
    public void setRemoveOutputAnnotations(boolean removeOutputAnnotations) {
        this.removeOutputAnnotations = removeOutputAnnotations;
    }

    @ACAQDocumentation(name = "Generated column", description = "The string representation of the data are stored in the column with this name")
    @ACAQParameter("generated-column")
    @StringParameterSettings(monospace = true)
    public String getGeneratedColumn() {
        return generatedColumn;
    }

    @ACAQParameter("generated-column")
    public void setGeneratedColumn(String generatedColumn) {
        this.generatedColumn = generatedColumn;
    }
}
