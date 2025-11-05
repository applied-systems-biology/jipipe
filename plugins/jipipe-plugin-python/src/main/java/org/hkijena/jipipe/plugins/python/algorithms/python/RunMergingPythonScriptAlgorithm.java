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

package org.hkijena.jipipe.plugins.python.algorithms.python;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.environments.RegisterJIPipeEnvironmentUsage;
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
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.python.adapter.JIPipePythonAdapterLibraryEnvironment;
import org.hkijena.jipipe.plugins.python.utils.PythonUtils;
import org.hkijena.jipipe.plugins.strings.PythonScriptData;
import org.hkijena.jipipe.utils.scripting.JythonUtils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An algorithm that allows to run Python code
 */
@SetJIPipeDocumentation(name = "Run Python script (merging)", description = "Runs a Python script that iterates through each iteration step in the input slots. " +
        "This node uses an existing dedicated Python interpreter that must be set up in the application settings.\n\nTo learn more about the JIPipe Python API, visit https://jipipe.hki-jena.de/apidocs/python-current/index.html")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Python script")
@RegisterJIPipeEnvironmentUsage(PythonEnvironment.class)
@RegisterJIPipeEnvironmentUsage(JIPipePythonAdapterLibraryEnvironment.class)
public class RunMergingPythonScriptAlgorithm extends JIPipeMergingAlgorithm implements JIPipeScriptAlgorithm {

    public static final JIPipeDataSlotInfo SLOT_SCRIPT = JIPipeDataSlotInfo.builder().slotType(JIPipeSlotType.Input).dataClass(PythonScriptData.class).name("Script").userModifiable(false).role(JIPipeDataSlotRole.Parameters).build();

    private PythonScriptParameter code = new PythonScriptParameter();
    private boolean externalCode = false;
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            PythonUtils.ALLOWED_PARAMETER_CLASSES);
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.Merge;
    private boolean cleanUpAfterwards = true;
    private boolean suppressLogs = false;

    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public RunMergingPythonScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().build());
        registerSubParameter(scriptParameters);
        updateSlots();
    }

    /**
     * Creates a copy
     *
     * @param other the info
     */
    public RunMergingPythonScriptAlgorithm(RunMergingPythonScriptAlgorithm other) {
        super(other);
        this.code = new PythonScriptParameter(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.suppressLogs = other.suppressLogs;
        this.externalCode = other.externalCode;
        registerSubParameter(scriptParameters);
        updateSlots();
    }

    @SetJIPipeDocumentation(name = "Clean up data after processing", description = "If enabled, data is deleted from temporary directories after " +
            "the processing was finished. Disable this to make it possible to debug your scripts. The directories are accessible via the logs (Tools &gt; Logs).")
    @JIPipeParameter("cleanup-afterwards")
    public boolean isCleanUpAfterwards() {
        return cleanUpAfterwards;
    }

    @JIPipeParameter("cleanup-afterwards")
    public void setCleanUpAfterwards(boolean cleanUpAfterwards) {
        this.cleanUpAfterwards = cleanUpAfterwards;
    }

    @SetJIPipeDocumentation(name = "Suppress logs", description = "If enabled, the node will not log the status of the Python operation. " +
            "Can be used to limit memory consumption of JIPipe if larger data sets are used.")
    @JIPipeParameter("suppress-logs")
    public boolean isSuppressLogs() {
        return suppressLogs;
    }

    @JIPipeParameter("suppress-logs")
    public void setSuppressLogs(boolean suppressLogs) {
        this.suppressLogs = suppressLogs;
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
        StringBuilder code = new StringBuilder();

        // Get the environments
        JIPipePythonAdapterLibraryEnvironment adapterLibraryEnvironment = getEnvironment(JIPipePythonAdapterLibraryEnvironment.class, runContext, progressInfo);
        PythonEnvironment pythonEnvironment = getEnvironment(PythonEnvironment.class, runContext, progressInfo);

        // Install the adapter that provides the JIPipe API
        PythonUtils.installAdapterCodeIfNeeded(adapterLibraryEnvironment, code);

        // Add user variables
        PythonUtils.parametersToPython(code, scriptParameters);

        // Add annotations
        PythonUtils.annotationsToPython(code, iterationStep.getMergedTextAnnotations().values());

        Path workDirectory = getNewScratch();

        // Install input slots
        Map<String, Path> inputSlotPaths = PythonUtils.installInputSlots(code, iterationStep, this, getDataInputSlots(), workDirectory, progressInfo);

        // Install output slots
        Map<String, Path> outputSlotPaths = PythonUtils.installOutputSlots(code, getOutputSlots(), workDirectory, progressInfo);

        // Add main code
        code.append("\n").append(getScriptCode(iterationStep, progressInfo)).append("\n");

        // Add postprocessor code
        PythonUtils.addPostprocessorCode(code, getOutputSlots());

        // Run code
        PythonUtils.runPython(code.toString(),
                pythonEnvironment,
                Collections.emptyList(), suppressLogs, progressInfo);

        // Extract outputs
        PythonUtils.extractOutputs(iterationStep, outputSlotPaths, getOutputSlots(), annotationMergeStrategy, progressInfo);

        // Clean up
        if (cleanUpAfterwards) {
            PythonUtils.cleanup(inputSlotPaths, outputSlotPaths, progressInfo);
        }
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);
    }

    @SetJIPipeDocumentation(name = "Script", description = "The Python script to be executed. " +
            "The script comes with various API functions and variables that allow to communicate with JIPipe: " +
            "<ul>" +
            "<li><code>jipipe_inputs</code> is a dict of input slots.</li>" +
            "<li><code>jipipe_outputs</code> is a dict of output slots.</li>" +
            "<li><code>jipipe_annotations</code> is a dict of annotation variables of the current iteration step.</li>" +
            "<li><code>jipipe_variables</code> is a dict of variables passed from the script parameters.</li>" +
            "</ul>" +
            "The script is designed to be used with the JIPipe Python API (supplied automatically by default). " +
            "You can find the full API documentation here: https://jipipe.hki-jena.de/apidocs/python-current/index.html")
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

    @SetJIPipeDocumentation(name = "Annotation merge strategy", description = "Determines how annotations that are added in the Python script are " +
            "merged into existing annotations.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
