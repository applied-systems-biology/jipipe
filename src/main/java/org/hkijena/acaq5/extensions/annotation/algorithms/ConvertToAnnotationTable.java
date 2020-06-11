package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.extensions.annotation.datatypes.AnnotationTableData;

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
        updateSlotTraits();
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
        updateSlotTraits();
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Generated column").checkNonEmpty(generatedColumn, this);
    }

    @Override
    protected void runIteration(ACAQMultiDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Set<Integer> inputDataRows = dataInterface.getInputRows(getFirstInputSlot());

        AnnotationTableData output = new AnnotationTableData();
        int dataColumn = output.addColumn(generatedColumn, true);

        int row = 0;
        for (int sourceRow : inputDataRows) {
            output.addRow();
            output.setValueAt("" + getFirstInputSlot().getData(sourceRow, ACAQData.class), row, dataColumn);
            for (ACAQTrait trait : getFirstInputSlot().getAnnotations(sourceRow)) {
                if(trait != null) {
                    int col = output.addAnnotationColumn(trait.getDeclaration());
                    output.setValueAt(trait.getValue(), row, col);
                }
            }
            ++row;
        }

        if (removeOutputAnnotations)
            dataInterface.getAnnotations().clear();

        dataInterface.addOutputData(getFirstOutputSlot(), output);

    }

    private void updateSlotTraits() {
        ACAQDefaultMutableTraitConfiguration traitConfiguration = (ACAQDefaultMutableTraitConfiguration) getTraitConfiguration();
        traitConfiguration.setTransferAllToAll(!removeOutputAnnotations);
        traitConfiguration.postChangedEvent();
    }

    @ACAQDocumentation(name = "Remove output annotations", description = "If enabled, annotations are removed from the output.")
    @ACAQParameter("remove-output-annotations")
    public boolean isRemoveOutputAnnotations() {
        return removeOutputAnnotations;
    }

    @ACAQParameter("remove-output-annotations")
    public void setRemoveOutputAnnotations(boolean removeOutputAnnotations) {
        this.removeOutputAnnotations = removeOutputAnnotations;
        updateSlotTraits();
    }

    @ACAQDocumentation(name = "Generated column", description = "The string representation of the data are stored in the column with this name")
    @ACAQParameter("generated-column")
    public String getGeneratedColumn() {
        return generatedColumn;
    }

    @ACAQParameter("generated-column")
    public void setGeneratedColumn(String generatedColumn) {
        this.generatedColumn = generatedColumn;
    }
}
