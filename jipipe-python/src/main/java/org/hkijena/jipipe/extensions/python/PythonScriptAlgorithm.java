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
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.MacroUtils;
import org.hkijena.jipipe.utils.PythonUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.python.util.PythonInterpreter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.utils.PythonUtils.ALLOWED_PARAMETER_CLASSES;

/**
 * An algorithm that allows to run Python code
 */
@JIPipeDocumentation(name = "Python script", description = "Runs a Python script that has direct access to all input data slots. " +
        "All slots are available as variables 'input_[slot name]' and 'output_[slot name]'. Slot names with spaces or other invalid variable names " +
        "are automatically converted by replacing the offending characters with '_'. Duplicate names are avoided by adding counters to the variable names. " +
        "Slots are of their respective JIPipe types (JIPipeDataSlot) and are fully accessible from within Python.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Miscellaneous)
public class PythonScriptAlgorithm extends JIPipeAlgorithm {

    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);

    /**
     * Creates a new instance
     * @param declaration the declaration
     */
    public PythonScriptAlgorithm(JIPipeAlgorithmDeclaration declaration) {
        super(declaration, JIPipeDefaultMutableSlotConfiguration.builder()
                .addOutputSlot("Table", ResultsTableData.class, null)
                .build());
        registerSubParameter(scriptParameters);

        code.setCode("from org.hkijena.jipipe.extensions.tables.datatypes import ResultsTableData\n" +
                "from org.hkijena.jipipe.api.data import JIPipeAnnotation\n" +
                "from random import random\n" +
                "\n" +
                "# We generate a table of 10 values\n" +
                "table = ResultsTableData()\n" +
                "table.addColumn(\"Area\", True)\n" +
                "\n" +
                "for row in range(10):\n" +
                "\ttable.addRow()\n" +
                "\ttable.setValueAt(random(), row, 0)\n" +
                "\n" +
                "# The output is written into the output slot\n" +
                "# You can add annotations via an overload of addData()\n" +
                "output_Table.addData(table, [JIPipeAnnotation(\"Dataset\", \"Generated\")])\n");
    }

    /**
     * Creates a copy
     * @param other the declaration
     */
    public PythonScriptAlgorithm(PythonScriptAlgorithm other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (isPassThrough() && canAutoPassThrough()) {
            algorithmProgress.accept(subProgress.resolve("Data passed through to output"));
            runPassThrough();
            return;
        }

        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        // Pass slots
        Set<String> existingSlotVariableNames = new HashSet<>();
        for (JIPipeDataSlot slot : getInputSlots()) {
            String variableName = StringUtils.makeUniqueString("input_" + MacroUtils.makeMacroCompatible(slot.getName()),
                    "_", existingSlotVariableNames);
            existingSlotVariableNames.add(variableName);
            pythonInterpreter.set(variableName, slot);
        }
        for (JIPipeDataSlot slot : getOutputSlots()) {
            String variableName = StringUtils.makeUniqueString("output_" + MacroUtils.makeMacroCompatible(slot.getName()),
                    "_", existingSlotVariableNames);
            existingSlotVariableNames.add(variableName);
            pythonInterpreter.set(variableName, slot);
        }

        pythonInterpreter.exec(code.getCode());
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        PythonUtils.checkScriptValidity(code.getCode(), scriptParameters, report.forCategory("Script"));
        PythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
    }

    @JIPipeDocumentation(name = "Script", description = "")
    @JIPipeParameter("code")
    public PythonScript getCode() {
        return code;
    }

    @JIPipeParameter("code")
    public void setCode(PythonScript code) {
        this.code = code;

    }

    @JIPipeDocumentation(name = "Script parameters", description = "All slots are available as variables 'input_[slot name]' and 'output_[slot name]'. Slot names with spaces or other invalid variable names " +
            "are automatically converted by replacing the offending characters with '_'. Duplicate names are avoided by adding counters to the variable names. " +
            "Slots are of their respective JIPipe types (JIPipeDataSlot) and are fully accessible from within Python.")
    @JIPipeParameter(value = "script-parameters", persistence = JIPipeParameterPersistence.Object)
    public JIPipeDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
