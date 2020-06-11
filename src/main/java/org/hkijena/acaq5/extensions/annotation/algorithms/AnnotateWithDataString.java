package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generates annotations from filenames
 */
@ACAQDocumentation(name = "Annotate with data string", description = "Converts incoming data into its string representation and creates the a new annotation that " +
        "contains this generated string.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation, menuPath = "Generate")
@AlgorithmInputSlot(value = ACAQData.class, slotName = "Data", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQData.class, slotName = "Annotated data", inheritedSlot = "Data", autoCreate = true)
public class AnnotateWithDataString extends ACAQSimpleIteratingAlgorithm {

    private ACAQTraitDeclarationRef generatedAnnotation = new ACAQTraitDeclarationRef();

    /**
     * New instance
     *
     * @param declaration Algorithm declaration
     */
    public AnnotateWithDataString(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other Original algorithm
     */
    public AnnotateWithDataString(AnnotateWithDataString other) {
        super(other);
        this.generatedAnnotation = new ACAQTraitDeclarationRef(other.generatedAnnotation.getDeclaration());
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (generatedAnnotation.getDeclaration() != null) {
            ACAQData inputData = dataInterface.getInputData(getFirstInputSlot(), ACAQData.class);
            String discriminator = "" + inputData;
            dataInterface.addGlobalAnnotation(generatedAnnotation.getDeclaration().newInstance(discriminator));
            dataInterface.addOutputData(getFirstOutputSlot(), inputData);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Generated annotation").report(generatedAnnotation);
    }

    /**
     * @return Generated annotation type
     */
    @ACAQDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each data row")
    @ACAQParameter("generated-annotation")
    public ACAQTraitDeclarationRef getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    /**
     * Sets generated annotation type
     *
     * @param generatedAnnotation Annotation type
     */
    @ACAQParameter("generated-annotation")
    public void setGeneratedAnnotation(ACAQTraitDeclarationRef generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }
}
