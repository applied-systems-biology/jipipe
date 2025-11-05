/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.python.algorithms.jython;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeScriptAlgorithm;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.scripts.PythonScriptParameter;
import org.hkijena.jipipe.plugins.strings.PythonScriptData;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import org.hkijena.jipipe.utils.scripting.JythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * An algorithm that allows to run Python code
 */
@SetJIPipeDocumentation(name = "Run Jython script (merging)", description = "Runs a Python script that iterates through each iteration step in the input slots. " +
        "This node uses Jython, a Java interpreter for Python that currently does not support native functions (e.g. Numpy), but can access all Java types." +
        "Each iteration step contains multiple input and output data items." +
        "Access to the iteration step is done via a variable 'data_batch' that provides access to all input and output data, as well as annotations. " +
        "Input slots can be accessed from variables 'input_slots' (array), 'input_slots_map' (map from name to slot). " +
        "Output slots can be accessed from variables 'output_slots' (array), 'output_slots_map' (map from name to slot).")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Python script")
public class RunMergingJythonScriptAlgorithm extends JIPipeMergingAlgorithm implements JIPipeScriptAlgorithm {

    public static final JIPipeDataSlotInfo SLOT_SCRIPT = JIPipeDataSlotInfo.builder().slotType(JIPipeSlotType.Input).dataClass(PythonScriptData.class).name("Script").userModifiable(false).role(JIPipeDataSlotRole.Parameters).build();

    private PythonScriptParameter code = new PythonScriptParameter();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipe.getParameterTypes().getRegisteredParameters().values());
    private boolean externalCode = false;

    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public RunMergingJythonScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().build());
        registerSubParameter(scriptParameters);
        updateSlots();
    }

    /**
     * Creates a copy
     *
     * @param other the info
     */
    public RunMergingJythonScriptAlgorithm(RunMergingJythonScriptAlgorithm other) {
        super(other);
        this.code = new PythonScriptParameter(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        this.externalCode = other.externalCode;
        registerSubParameter(scriptParameters);
        updateSlots();
    }

    private void updateSlots() {
        toggleSlot(SLOT_SCRIPT, externalCode);
        emitParameterUIChangedEvent();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if ("code".equals(access.getKey()) && externalCode) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    @SetJIPipeDocumentation(name = "External code", description = "If enabled, run code from an input slot")
    @JIPipeParameter(value = "external-code", important = true)
    public boolean isExternalCode() {
        return externalCode;
    }

    @JIPipeParameter("external-code")
    public void setExternalCode(boolean externalCode) {
        this.externalCode = externalCode;
        updateSlots();
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        super.reportValidity(reportContext, reportSettings, report, progressInfo);
        JythonUtils.checkScriptParametersValidity(scriptParameters, new ParameterValidationReportContext(reportContext, this, "Script parameters", "script-parameters"), report);
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);
    }

    private String getScriptCode(JIPipeMultiIterationStep iterationStep, JIPipeProgressInfo progressInfo) {
        if (externalCode) {
            List<PythonScriptData> inputData = iterationStep.getInputData(SLOT_SCRIPT.getName(), PythonScriptData.class, progressInfo);
            if (inputData.size() > 1) {
                progressInfo.warn("Multiple external scripts were provided. Running only the first one!");
            }
            return inputData.getFirst().getData();
        } else {
            return code.getCode();
        }
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        JythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        pythonInterpreter.set("data_batch", iterationStep);
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
        pythonInterpreter.set("progress_info", progressInfo);
        try (IJLogToJIPipeProgressInfoPump ignored = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
            pythonInterpreter.exec(getScriptCode(iterationStep, progressInfo));
        }
    }

    @SetJIPipeDocumentation(name = "Script", description = "Access to the iteration step is done via a variable 'data_batch' that provides access to all input and output data, as well as annotations." +
            "Input slots can be accessed from variables 'input_slots' (array), 'input_slots_map' (map from name to slot). " +
            "Output slots can be accessed from variables 'output_slots' (array), 'output_slots_map' (map from name to slot). " +
            "A variable 'progress_info' provides the current progress logger instance.")
    @JIPipeParameter("code")
    public PythonScriptParameter getCode() {
        return code;
    }

    @JIPipeParameter("code")
    public void setCode(PythonScriptParameter code) {
        this.code = code;
    }

    @Override
    public JIPipeParameterAccess getScriptParameterAccess() {
        return getParameterAccess("code");
    }

    @SetJIPipeDocumentation(name = "Script parameters", description = "The following parameters will be passed to the Python script. The variable name is equal to the unique parameter identifier.")
    @JIPipeParameter(value = "script-parameters", persistence = JIPipeParameterSerializationMode.Object)
    public JIPipeDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
