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
import org.hkijena.jipipe.extensions.parameters.functions.StringPatternExtractionFunction;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generates annotations from filenames
 */
@JIPipeDocumentation(name = "Extract & replace annotations", description = "Algorithm that allows you to extract parts of an annotation and either " +
        "replace the existing annotation or put the results into a new one.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Annotation, menuPath = "Modify")
@AlgorithmInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class ExtractAndReplaceAnnotation extends JIPipeSimpleIteratingAlgorithm {

    private StringPatternExtractionFunction.List functions = new StringPatternExtractionFunction.List();

    /**
     * New instance
     *
     * @param declaration Algorithm declaration
     */
    public ExtractAndReplaceAnnotation(JIPipeAlgorithmDeclaration declaration) {
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
        this.functions = new StringPatternExtractionFunction.List(other.functions);
    }

    @Override
    protected void runIteration(JIPipeDataInterface dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (StringPatternExtractionFunction function : functions) {
            JIPipeAnnotation inputTrait = dataInterface.getAnnotationOfType(function.getInput());
            if (inputTrait == null)
                continue;
            String newValue = function.getParameter().apply(inputTrait.getValue());
            if (newValue == null)
                continue;
            dataInterface.addGlobalAnnotation(new JIPipeAnnotation(function.getOutput(), newValue));
        }
        dataInterface.addOutputData(getFirstOutputSlot(), dataInterface.getInputData(getFirstInputSlot(), JIPipeData.class));
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Functions").report(functions);
        for (int i = 0; i < functions.size(); i++) {
            JIPipeValidityReport subReport = report.forCategory("Functions").forCategory("Item #" + (i + 1));
            subReport.forCategory("Input").checkNonEmpty(functions.get(i).getInput(), this);
            subReport.forCategory("Output").checkNonEmpty(functions.get(i).getOutput(), this);
        }
    }

    @JIPipeDocumentation(name = "Functions", description = "The functions that allow you to extract and replace annotation values. " +
            "To extract values, you can split the incoming string into multiple components and then select the n-th component " +
            "or select the one that matches RegEx. Alternatively you can define a RegEx string that contains a matching group (brackets). " +
            "This matching group will then be picked.")
    @JIPipeParameter("functions")
    @StringParameterSettings(monospace = true)
    public StringPatternExtractionFunction.List getFunctions() {
        return functions;
    }

    @JIPipeParameter("functions")
    public void setFunctions(StringPatternExtractionFunction.List functions) {
        this.functions = functions;
    }
}
