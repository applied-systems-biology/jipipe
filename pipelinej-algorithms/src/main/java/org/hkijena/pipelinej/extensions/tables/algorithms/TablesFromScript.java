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
import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithm;
import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.pipelinej.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.pipelinej.api.data.ACAQAnnotation;
import org.hkijena.pipelinej.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.tables.ResultsTableData;
import org.hkijena.pipelinej.extensions.parameters.scripts.PythonScript;
import org.hkijena.pipelinej.utils.PythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.pipelinej.utils.PythonUtils.ALLOWED_PARAMETER_CLASSES;

/**
 * Algorithm that annotates all data with the same annotation
 */
@ACAQDocumentation(name = "Table from script", description = "Executes a Python-script that generates a table. " +
        "There must be an array 'tables' that contains all input rows as dictionary with following entries: 'data', 'nrow', and 'annotations'. " +
        "'data' is a dictionary with the column name as key and values being an array of strings or doubles. " +
        "'nrow' is an integer that contains the number of rows. " +
        "'annotations' is a dictionary from string to string containing all annotations")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class TablesFromScript extends ACAQAlgorithm {

    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private ACAQDynamicParameterCollection scriptParameters = new ACAQDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);

    /**
     * @param declaration the declaration
     */
    public TablesFromScript(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        code.setCode("# This script is executed once\n" +
                "# The results are extracted from an array 'tables'\n" +
                "# It contains dictionaries with following structure:\n" +
                "# { 'data' : {}, 'nrow': x, 'annotations': {} }\n" +
                "# 'data' is a dictionary from column name to a list of row data\n" +
                "# 'nrow' is the number of rows (str/float)\n" +
                "# 'annotations' is a dictionary from annotation name to value (str)" +
                "\n\n" +
                "tables = [\n" +
                "    { \"data\": { \"example\" : [1,2,3] } }\n" +
                "]");
        registerSubParameter(scriptParameters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public TablesFromScript(TablesFromScript other) {
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

        pythonInterpreter.exec(code.getCode());
        List<PyDictionary> rows = (List<PyDictionary>) pythonInterpreter.get("tables").__tojava__(List.class);

        for (PyDictionary row : rows) {
            ResultsTableData data = ResultsTableData.fromPython((PyDictionary) row.get("data"));
            List<ACAQAnnotation> annotations = ACAQAnnotation.extractAnnotationsFromPython((PyDictionary) row.getOrDefault("annotations", new PyDictionary()));
            getFirstOutputSlot().addData(data, annotations);
        }

        this.pythonInterpreter = null;
    }

    @ACAQDocumentation(name = "Script", description = "here must be an array 'tables' that contains all input rows as dictionary with following entries: 'data', 'nrow', and 'annotations'. " +
            "<ul><li>'data' is a dictionary with the column name as key and values being an array of strings or doubles.</li>" +
            "<li>'nrow' is an integer that contains the number of rows.</li>" +
            "<li>'annotations' is a dictionary from string to string containing all annotations</li></ul>")
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
