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

package org.hkijena.jipipe.extensions.python.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.JythonUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * An algorithm that allows to run Python code
 */
@JIPipeDocumentation(name = "Jython script (multi-parameter capable)", description = "Runs a Python script that has direct access to all input data slots. " +
        "This node uses Jython, a Java interpreter for Python that currently does not support native functions (e.g. Numpy), but can access all Java types." +
        "Input slots can be accessed from variables 'input_slots' (array), 'input_slots_map' (map from name to slot). " +
        "Output slots can be accessed from variables 'output_slots' (array), 'output_slots_map' (map from name to slot)." +
        "Slots are of their respective JIPipe types (JIPipeDataSlot) and are fully accessible from within Python. " +
        "This algorithm is capable of running over multiple parameter sets via an additional slot. Automatically generated annotations generated based on " +
        "the parameters are available as variable 'parameter_annotations'. Please do not forget to pass the annotations to the output if you want to want this.")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Python script")
public class JythonScriptAlgorithm extends JIPipeParameterSlotAlgorithm {

    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipe.getParameterTypes().getRegisteredParameters().values());

    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public JythonScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addOutputSlot("Table", ResultsTableData.class, null)
                .build());
        registerSubParameter(scriptParameters);
    }

    /**
     * Creates a copy
     *
     * @param other the info
     */
    public JythonScriptAlgorithm(JythonScriptAlgorithm other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            slotConfiguration.clearInputSlots(true);
            slotConfiguration.clearOutputSlots(true);
            slotConfiguration.addSlot("Table", new JIPipeDataSlotInfo(ResultsTableData.class, JIPipeSlotType.Output, null), true);
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
            getEventBus().post(new ParameterChangedEvent(this, "code"));
        }
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations) {
        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        JythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        pythonInterpreter.set("parameter_annotations", parameterAnnotations);
        PyDictionary inputSlotMap = new PyDictionary();
        PyDictionary outputSlotMap = new PyDictionary();
        for (JIPipeDataSlot inputSlot : getNonParameterInputSlots()) {
            inputSlotMap.put(inputSlot.getName(), inputSlot);
        }
        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            outputSlotMap.put(outputSlot.getName(), outputSlot);
        }
        pythonInterpreter.set("input_slots", new ArrayList<>(getNonParameterInputSlots()));
        pythonInterpreter.set("output_slots", new ArrayList<>(getOutputSlots()));
        pythonInterpreter.set("input_slot_map", inputSlotMap);
        pythonInterpreter.set("output_slot_map", outputSlotMap);

        pythonInterpreter.exec(code.getCode());
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        JythonUtils.checkScriptValidity(code.getCode(), scriptParameters, report.forCategory("Script"));
        JythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
    }

    @JIPipeDocumentation(name = "Script", description = "Access to the data batch is done via a variable 'data_batch' that provides access to all input and output data, as well as annotations.")
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
