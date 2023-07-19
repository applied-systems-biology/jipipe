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
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.library.scripts.PythonScript;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import org.hkijena.jipipe.utils.scripting.JythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * An algorithm that allows to run Python code
 */
@JIPipeDocumentation(name = "Jython script (simple iterating)", description = "Runs a Python script that iterates through each data batch in one input slot. " +
        "This node uses Jython, a Java interpreter for Python that currently does not support native functions (e.g. Numpy), but can access all Java types." +
        "Access to the data batch is done via a variable 'data_batch' that provides access to all input and output data, as well as annotations. " +
        "Input slots can be accessed from variables 'input_slots' (array), 'input_slots_map' (map from name to slot). " +
        "The first (and only) input slot is also accessible via the 'input_slot' variable. " +
        "Output slots can be accessed from variables 'output_slots' (array), 'output_slots_map' (map from name to slot).")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Python script")
public class SimpleIteratingJythonScriptAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipe.getParameterTypes().getRegisteredParameters().values());

    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public SimpleIteratingJythonScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputSlotCount(1)
                .build());
        registerSubParameter(scriptParameters);
    }

    /**
     * Creates a copy
     *
     * @param other the info
     */
    public SimpleIteratingJythonScriptAlgorithm(SimpleIteratingJythonScriptAlgorithm other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        JythonUtils.checkScriptValidity(code.getCode(getProjectDirectory()), scriptParameters, new ParameterValidationReportContext(context,this, "Script", "script"), report);
        JythonUtils.checkScriptParametersValidity(scriptParameters,  new ParameterValidationReportContext(context,this, "Script parameters", "script-parameters"), report);
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);
        code.makeExternalScriptFileRelative(baseDirectory);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        JythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        pythonInterpreter.set("data_batch", dataBatch);
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
        if (!getNonParameterInputSlots().isEmpty()) {
            pythonInterpreter.set("input_slot", getFirstInputSlot());
        }
        pythonInterpreter.set("progress_info", progressInfo);
        try (IJLogToJIPipeProgressInfoPump ignored = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
            pythonInterpreter.exec(code.getCode(getProjectDirectory()));
        }
    }

    @JIPipeDocumentation(name = "Script", description = "Access to the data batch is done via a variable 'data_batch' that provides access to all input and output data, as well as annotations." +
            "Input slots can be accessed from variables 'input_slots' (array), 'input_slots_map' (map from name to slot). " +
            "The first (and only) input slot is also accessible via the 'input_slot' variable. " +
            "Output slots can be accessed from variables 'output_slots' (array), 'output_slots_map' (map from name to slot). " +
            "A variable 'progress_info' provides the current progress logger instance.")
    @JIPipeParameter("code")
    public PythonScript getCode() {
        return code;
    }

    @JIPipeParameter("code")
    public void setCode(PythonScript code) {
        this.code = code;
    }

    @JIPipeDocumentation(name = "Script parameters", description = "The following parameters will be passed to the Python script. The variable name is equal to the unique parameter identifier.")
    @JIPipeParameter(value = "script-parameters", persistence = JIPipeParameterPersistence.NestedCollection)
    public JIPipeDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
