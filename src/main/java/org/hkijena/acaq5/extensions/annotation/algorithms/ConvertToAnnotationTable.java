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
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.annotation.datatypes.AnnotationTableData;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Removes a specified annotation
 */
@ACAQDocumentation(name = "Convert to annotation table", description = "Converts data into an annotation table that contains " +
        "all annotations of the data row. The table contains a column 'data' that contains a string representation of the input data. " +
        "All other columns are generated based on the annotations. They have following structure: 'annotation:[annotation-id]' where the annotation id " +
        "is the unique identifier of this annotation type. You can find annotation types in the help menu.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Converter)
@AlgorithmInputSlot(value = ACAQData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = AnnotationTableData.class, slotName = "Output", autoCreate = true)
public class ConvertToAnnotationTable extends ACAQSimpleIteratingAlgorithm {

    private boolean removeOutputAnnotations = true;

    /**
     * @param declaration algorithm declaration
     */
    public ConvertToAnnotationTable(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        updateSlotTraits();
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ConvertToAnnotationTable(ConvertToAnnotationTable other) {
        super(other);
        this.removeOutputAnnotations = other.removeOutputAnnotations;
        updateSlotTraits();
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        AnnotationTableData output = new AnnotationTableData();
        output.addRow();
        output.addColumn("data", true);
        output.setValueAt("" + dataInterface.getInputData(getFirstOutputSlot(), ACAQData.class), 0, 0);
        for (Map.Entry<ACAQTraitDeclaration, ACAQTrait> entry : dataInterface.getAnnotations().entrySet()) {
            int col = output.addAnnotationColumn(entry.getKey());
            ACAQTrait trait = entry.getValue();
            output.setValueAt(trait.getValue(), 0, col);
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
}
