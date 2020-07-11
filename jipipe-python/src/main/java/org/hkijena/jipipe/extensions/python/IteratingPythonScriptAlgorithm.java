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

package org.hkijena.jipipe.extensions.python;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.api.registries.JIPipeParameterTypeRegistry;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.PythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An algorithm that allows to run Python code
 */
@JIPipeDocumentation(name = "Python script (iterating)", description = "Runs a Python script that iterates through each data batch in the input slots. " +
        "Access to the data batch is done via a variable 'data_batch' that provides access to all input and output data, as well as annotations. " +
        "Input slots can be accessed from variables 'input_slots' (array), 'input_slots_map' (map from name to slot). " +
        "Output slots can be accessed from variables 'output_slots' (array), 'output_slots_map' (map from name to slot).")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Miscellaneous, menuPath = "Python script")
public class IteratingPythonScriptAlgorithm extends JIPipeIteratingAlgorithm {

    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipeParameterTypeRegistry.getInstance().getRegisteredParameters().values());

    /**
     * Creates a new instance
     * @param declaration the declaration
     */
    public IteratingPythonScriptAlgorithm(JIPipeAlgorithmDeclaration declaration) {
        super(declaration, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Table", ResultsTableData.class)
                .addOutputSlot("Table", ResultsTableData.class, null)
                .build());
        registerSubParameter(scriptParameters);

        code.setCode("from org.hkijena.jipipe.extensions.tables.datatypes import ResultsTableData\n" +
                "\n" +
                "# Fetch the input table from the first input slot\n" +
                "input_table = data_batch.getInputData(input_slot, ResultsTableData)\n" +
                "\n" +
                "table = ResultsTableData()\n" +
                "\n" +
                "for col in range(input_table.getColumnCount()):\n" +
                "\ttable.addColumn(\"MEAN(\" + input_table.getColumnName(col) + \")\", True)\n" +
                "\n" +
                "table.addRow()\n" +
                "\n" +
                "for col in range(input_table.getColumnCount()):\n" +
                "\tcolumn = input_table.getColumnReference(col)\n" +
                "\tcolumn_data = column.getDataAsDouble(column.getRows())\n" +
                "\ttable.setValueAt(sum(column_data) / column.getRows(), 0, col)\n" +
                "\n" +
                "# Write the generated data\n" +
                "# Annotations are automatically transferred\n" +
                "data_batch.addOutputData(output_slots[0], table)\n");
    }

    /**
     * Creates a copy
     * @param other the declaration
     */
    public IteratingPythonScriptAlgorithm(IteratingPythonScriptAlgorithm other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        PythonUtils.checkScriptValidity(code.getCode(), scriptParameters, report.forCategory("Script"));
        PythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        PythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        pythonInterpreter.set("data_batch", dataInterface);
        PyDictionary inputSlotMap = new PyDictionary();
        PyDictionary outputSlotMap = new PyDictionary();
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            inputSlotMap.put(inputSlot.getName(), inputSlot);
        }
        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            outputSlotMap.put(outputSlot.getName(), outputSlot);
        }
        pythonInterpreter.set("input_slots", new ArrayList<>(getInputSlots()));
        pythonInterpreter.set("output_slots", new ArrayList<>(getOutputSlots()));
        pythonInterpreter.set("input_slot_map", inputSlotMap);
        pythonInterpreter.set("output_slot_map", outputSlotMap);
        pythonInterpreter.exec(code.getCode());
    }

    @JIPipeDocumentation(name = "Script", description = "Access to the data batch is done via a variable 'data_batch' that provides access to all input and output data, as well as annotations." +
            "Input slots can be accessed from variables 'input_slots' (array), 'input_slots_map' (map from name to slot). " +
            "Output slots can be accessed from variables 'output_slots' (array), 'output_slots_map' (map from name to slot).")
    @JIPipeParameter("code")
    public PythonScript getCode() {
        return code;
    }

    @JIPipeParameter("code")
    public void setCode(PythonScript code) {
        this.code = code;
    }

    @JIPipeDocumentation(name = "Script parameters", description = "The following parameters will be passed to the Python script. The variable name is equal to the unique parameter identifier.")
    @JIPipeParameter(value = "script-parameters", persistence = JIPipeParameterPersistence.Object)
    public JIPipeDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
