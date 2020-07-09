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

package org.hkijena.pipelinej.extensions.annotation.algorithms;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQAnnotation;
import org.hkijena.pipelinej.api.data.ACAQData;
import org.hkijena.pipelinej.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.parameters.scripts.PythonScript;
import org.hkijena.pipelinej.utils.PythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.pipelinej.utils.PythonUtils.ALLOWED_PARAMETER_CLASSES;

/**
 * Algorithm that annotates all data with the same annotation
 */
@ACAQDocumentation(name = "Modify annotation rows (Script)", description = "Executes a Python-script for each annotation row. " +
        "All annotations are passed as dictionary 'annotations' that can be modified using Python functions. The values are then extracted and " +
        "converted into their respective ACAQ5 types.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation, menuPath = "Modify")
@AlgorithmInputSlot(value = ACAQData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class ModifyAnnotationScript extends ACAQSimpleIteratingAlgorithm {

    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private ACAQDynamicParameterCollection scriptParameters = new ACAQDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);

    /**
     * @param declaration the declaration
     */
    public ModifyAnnotationScript(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        code.setCode("# This script is executed for each row\n" +
                "# Annotations are passed as dictionary 'annotations'\n" +
                "# Modifications are copied into ACAQ5\n\n" +
                "annotations[\"condition\"] = \"example\"");
        registerSubParameter(scriptParameters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ModifyAnnotationScript(ModifyAnnotationScript other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new ACAQDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        PythonUtils.checkScriptValidity(code.getCode(), scriptParameters, report.forCategory("Script"));
        PythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        this.pythonInterpreter = new PythonInterpreter();
        PythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        super.run(subProgress, algorithmProgress, isCancelled);
        this.pythonInterpreter = null;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        PyDictionary annotationDict = ACAQAnnotation.annotationMapToPython(dataInterface.getAnnotations());
        pythonInterpreter.set("annotations", annotationDict);
        pythonInterpreter.exec(code.getCode());
        annotationDict = (PyDictionary) pythonInterpreter.get("annotations");

        // Convert the results back into ACAQ5
        dataInterface.getAnnotations().clear();
        ACAQAnnotation.setAnnotationsFromPython(annotationDict, dataInterface.getAnnotations());

        dataInterface.addOutputData(getFirstOutputSlot(), dataInterface.getInputData(getFirstInputSlot(), ACAQData.class));
    }

    @ACAQDocumentation(name = "Script", description = "All annotations are passed as dictionary 'annotations' that can be modified using Python functions. The values are then extracted and " +
            "converted into their respective ACAQ5 types.")
    @ACAQParameter("code")
    public PythonScript getCode() {
        return code;
    }

    @ACAQParameter("code")
    public void setCode(PythonScript code) {
        this.code = code;
    }

    @ACAQDocumentation(name = "Script parameters", description = "The following parameters will be passed to the Python script. The variable name is equal to the unique parameter identifier.")
    @ACAQParameter("script-parameters")
    public ACAQDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
