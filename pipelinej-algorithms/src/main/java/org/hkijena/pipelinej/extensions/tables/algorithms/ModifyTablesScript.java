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

package org.hkijena.pipelinej.extensions.tables.algorithms;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQAnnotation;
import org.hkijena.pipelinej.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.tables.ResultsTableData;
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
@ACAQDocumentation(name = "Modify tables (Script)", description = "Executes a Python-script for each table. " +
        "Each table is passed as dictionary 'table' with the column name as key and values being an array of strings or doubles. The number of input rows can be accessed via the 'nrow' variable.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ModifyTablesScript extends ACAQSimpleIteratingAlgorithm {

    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private ACAQDynamicParameterCollection scriptParameters = new ACAQDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);

    /**
     * @param declaration the declaration
     */
    public ModifyTablesScript(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        code.setCode("# This script is executed for each table\n" +
                "# Tables are passed as dictionary 'table'\n" +
                "# Key are the column names\n" +
                "# Values are string/double arrays\n" +
                "# Annotations can be modified via a dict 'annotations'\n\n" +
                "areas = table[\"Area\"]\n" +
                "areas_sq = [ x * x for x in areas ]\n" +
                "table[\"Area2\"] = areas_sq");
        registerSubParameter(scriptParameters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ModifyTablesScript(ModifyTablesScript other) {
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
        ResultsTableData inputData = dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class);
        PyDictionary tableDict = inputData.toPython();

        PyDictionary annotationDict = ACAQAnnotation.annotationMapToPython(dataInterface.getAnnotations());
        pythonInterpreter.set("annotations", annotationDict);
        pythonInterpreter.set("table", tableDict);
        pythonInterpreter.set("nrow", inputData.getRowCount());
        pythonInterpreter.exec(code.getCode());
        tableDict = (PyDictionary) pythonInterpreter.get("table");
        annotationDict = (PyDictionary) pythonInterpreter.get("annotations");

        dataInterface.getAnnotations().clear();
        ACAQAnnotation.setAnnotationsFromPython(annotationDict, dataInterface.getAnnotations());

        dataInterface.addOutputData(getFirstOutputSlot(), ResultsTableData.fromPython(tableDict));
    }

    @ACAQDocumentation(name = "Script", description = "Each table is passed as dictionary 'table' " +
            "with the column name as key and values being an array of strings or doubles. " +
            "The number of input rows can be accessed via the 'nrow' variable.")
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
