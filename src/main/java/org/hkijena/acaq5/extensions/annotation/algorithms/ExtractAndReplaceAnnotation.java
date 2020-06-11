package org.hkijena.acaq5.extensions.annotation.algorithms;

import net.imagej.ops.morphology.thin.ThinningStrategy;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.functions.ACAQTraitPatternExtractionFunction;
import org.hkijena.acaq5.extensions.parameters.functions.FunctionParameter;
import org.hkijena.acaq5.extensions.parameters.patterns.StringPatternExtraction;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generates annotations from filenames
 */
@ACAQDocumentation(name = "Extract & replace annotations", description = "Algorithm that allows you to extract parts of an annotation and either " +
        "replace the existing annotation or put the results into a new one.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation, menuPath = "Modify")
@AlgorithmInputSlot(value = ACAQData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class ExtractAndReplaceAnnotation extends ACAQSimpleIteratingAlgorithm {

    private ACAQTraitPatternExtractionFunction.List functions = new ACAQTraitPatternExtractionFunction.List();

    /**
     * New instance
     *
     * @param declaration Algorithm declaration
     */
    public ExtractAndReplaceAnnotation(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        functions.addNewInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other Original algorithm
     */
    public ExtractAndReplaceAnnotation(ExtractAndReplaceAnnotation other) {
        super(other);
        this.functions = new ACAQTraitPatternExtractionFunction.List(other.functions);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (ACAQTraitPatternExtractionFunction function : functions) {
            ACAQTrait inputTrait = dataInterface.getAnnotationOfType(function.getInput().getDeclaration());
            if(inputTrait == null)
                continue;
            String newValue = function.getParameter().apply(inputTrait.getValue());
            if(newValue == null)
                continue;
            dataInterface.addGlobalAnnotation(function.getOutput().getDeclaration().newInstance(newValue));
        }
        dataInterface.addOutputData(getFirstOutputSlot(), dataInterface.getInputData(getFirstInputSlot(), ACAQData.class));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Functions").report(functions);
        for (int i = 0; i < functions.size(); i++) {
            ACAQValidityReport subReport = report.forCategory("Functions").forCategory("Item #" + (i + 1));
            subReport.forCategory("Input").checkNonNull(functions.get(i).getInput().getDeclaration(), this);
            subReport.forCategory("Output").checkNonNull(functions.get(i).getOutput().getDeclaration(), this);
        }
    }

    @ACAQDocumentation(name = "Functions", description = "The functions that allow you to extract and replace annotation values. " +
            "To extract values, you can split the incoming string into multiple components and then select the n-th component " +
            "or select the one that matches RegEx. Alternatively you can define a RegEx string that contains a matching group (brackets). " +
            "This matching group will then be picked.")
    @ACAQParameter("functions")
    public ACAQTraitPatternExtractionFunction.List getFunctions() {
        return functions;
    }

    @ACAQParameter("functions")
    public void setFunctions(ACAQTraitPatternExtractionFunction.List functions) {
        this.functions = functions;
    }
}
