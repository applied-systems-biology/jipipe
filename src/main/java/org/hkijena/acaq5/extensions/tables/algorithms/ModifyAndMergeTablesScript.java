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

package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.scripts.PythonScript;
import org.hkijena.acaq5.utils.PythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.utils.PythonUtils.ALLOWED_PARAMETER_CLASSES;

/**
 * Algorithm that annotates all data with the same annotation
 */
@ACAQDocumentation(name = "Modify & merge tables (Script)", description = "Executes a Python-script that allows full control over the input data table. " +
        "In the Python script, there is an array 'tables' that contains all input rows as dictionary with following entries: 'data', 'nrow', and 'annotations'. " +
        "'data' is a dictionary with the column name as key and values being an array of strings or doubles. " +
        "'nrow' is an integer that contains the number of rows. " +
        "'annotations' is a dictionary from string to string containing all annotations.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ModifyAndMergeTablesScript extends ACAQAlgorithm {

    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private ACAQDynamicParameterCollection scriptParameters = new ACAQDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);

    /**
     * @param declaration the declaration
     */
    public ModifyAndMergeTablesScript(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        code.setCode("# This script is executed once\n" +
                "# All tables are passed as array 'tables'\n" +
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
    public ModifyAndMergeTablesScript(ModifyAndMergeTablesScript other) {
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
        if (isPassThrough() && canPassThrough()) {
            algorithmProgress.accept(subProgress.resolve("Data passed through to output"));
            runPassThrough();
            return;
        }
        this.pythonInterpreter = new PythonInterpreter();
        PythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);

        List<PyDictionary> rows = new ArrayList<>();
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            PyDictionary rowDictionary = new PyDictionary();
            ResultsTableData tableData = getFirstInputSlot().getData(row, ResultsTableData.class);

            rowDictionary.put("data", tableData.toPython());
            rowDictionary.put("nrow", tableData.getRowCount());
            rowDictionary.put("annotations", ACAQAnnotation.annotationListToPython(getFirstInputSlot().getAnnotations(row)));

            rows.add(rowDictionary);
        }

        pythonInterpreter.set("tables", rows);
        pythonInterpreter.exec(code.getCode());
        rows = (List<PyDictionary>) pythonInterpreter.get("tables").__tojava__(List.class);

        for (PyDictionary row : rows) {
            ResultsTableData data = ResultsTableData.fromPython((PyDictionary) row.get("data"));
            List<ACAQAnnotation> annotations = ACAQAnnotation.extractAnnotationsFromPython((PyDictionary) row.getOrDefault("annotations", new PyDictionary()));
            getFirstOutputSlot().addData(data, annotations);
        }

        this.pythonInterpreter = null;
    }

    @ACAQDocumentation(name = "Script", description = "There is an array 'tables' that contains all input rows as dictionary with following entries: 'data', 'nrow', and 'annotations'. " +
            "'data' is a dictionary with the column name as key and values being an array of strings or doubles. " +
            "'nrow' is an integer that contains the number of rows. " +
            "'annotations' is a dictionary from string to string containing all annotations")
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
