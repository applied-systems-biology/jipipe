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
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.pairs.IntegerAndIntegerPair;
import org.hkijena.acaq5.extensions.parameters.pairs.StringAndStringPair;
import org.hkijena.acaq5.extensions.parameters.primitives.DoubleList;
import org.hkijena.acaq5.extensions.parameters.primitives.IntegerList;
import org.hkijena.acaq5.extensions.parameters.primitives.PathList;
import org.hkijena.acaq5.extensions.parameters.primitives.StringList;
import org.hkijena.acaq5.extensions.parameters.scripts.PythonCode;
import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.utils.MacroUtils;
import org.hkijena.acaq5.utils.PythonUtils;
import org.python.antlr.ast.Num;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.utils.PythonUtils.ALLOWED_PARAMETER_CLASSES;

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
    private PythonCode code = new PythonCode();
    private ACAQDynamicParameterCollection scriptParameters = new ACAQDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);

    /**
     * @param declaration the declaration
     */
    public ModifyTablesScript(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        code.setCode("# This script is executed for each table\n" +
                "# Tables are passed as dictionary 'table'\n" +
                "# Key are the column names\n" +
                "# Values are string/double arrays\n\n");
        registerSubParameter(scriptParameters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ModifyTablesScript(ModifyTablesScript other) {
        super(other);
        this.code = new PythonCode(other.code);
        this.scriptParameters = new ACAQDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        try {
            this.pythonInterpreter = new PythonInterpreter();
            pythonInterpreter.set("table", new PyDictionary());
            PythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
            if(pythonInterpreter.compile(code.getCode()) == null) {
                report.forCategory("Script").reportIsInvalid("The script is invalid!",
                        "The script could not be compiled.",
                        "Please check if your Python script is correct.",
                        this);
            }
            this.pythonInterpreter = null;
        }
        catch (Exception e) {
            report.forCategory("Script").reportIsInvalid("The script is invalid!",
                    "The script could not be compiled.",
                    "Please check if your Python script is correct.",
                    e);
        }
        for (String key : scriptParameters.getParameters().keySet()) {
            if (!MacroUtils.isValidVariableName(key)) {
                report.forCategory("Script Parameters").forCategory(key).reportIsInvalid("Invalid name!",
                        "'" + key + "' is an invalid Python variable name!",
                        "Please ensure that script variables are compatible with the Python language.",
                        this);
            }
        }
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
        PyDictionary tableDict = new PyDictionary();
        ResultsTableData inputData = dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class);
        for (int col = 0; col < inputData.getColumnCount(); col++) {
            String colName = inputData.getColumnName(col);
            List<Object> copy = new ArrayList<>();

            if(inputData.isNumeric(col)) {
                for (int row = 0; row < inputData.getRowCount(); row++) {
                    copy.add(inputData.getValueAsDouble(row, col));
                }
            }
            else {
                for (int row = 0; row < inputData.getRowCount(); row++) {
                    copy.add(inputData.getValueAsString(row, col));
                }
            }

            tableDict.put(colName, copy);
        }

        pythonInterpreter.set("table", tableDict);
        pythonInterpreter.set("nrow", inputData.getRowCount());
        pythonInterpreter.exec(code.getCode());

        Map<String, TableColumn> columns = new HashMap<>();
        for (Object key : tableDict.keys()) {
            String columnKey = "" + key;
            List<Object> rows = (List<Object>) tableDict.get(key);
            boolean isNumeric = true;
            for (Object row : rows) {
                isNumeric &= row instanceof Number;
            }
            if(isNumeric) {
                double[] data = new double[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    data[i] = ((Number)rows.get(i)).doubleValue();
                }
                columns.put(columnKey, new DoubleArrayTableColumn(data, columnKey));
            }
            else {
                String[] data = new String[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    data[i] = "" + rows.get(i);
                }
                columns.put(columnKey, new StringArrayTableColumn(data, columnKey));
            }
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ResultsTableData(columns));
    }

    @ACAQDocumentation(name = "Script", description = "Each table is passed as dictionary 'table' " +
            "with the column name as key and values being an array of strings or doubles. " +
            "The number of input rows can be accessed via the 'nrow' variable.")
    @ACAQParameter("code")
    public PythonCode getCode() {
        return code;
    }

    @ACAQParameter("code")
    public void setCode(PythonCode code) {
        this.code = code;
    }

     @ACAQDocumentation(name = "Script parameters", description = "The following parameters are prepended to the script code:")
     @ACAQParameter("script-parameters")
    public ACAQDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
