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

package org.hkijena.jipipe.extensions.python.algorithms;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterSerializationMode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.library.scripts.PythonScript;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonPlugin;
import org.hkijena.jipipe.extensions.python.PythonExtensionSettings;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.extensions.python.adapter.PythonAdapterExtensionSettings;
import org.hkijena.jipipe.utils.scripting.JythonUtils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An algorithm that allows to run Python code
 */
@SetJIPipeDocumentation(name = "Python script (merging)", description = "Runs a Python script that iterates through each data batch in the input slots. " +
        "This node uses an existing dedicated Python interpreter that must be set up in the application settings.\n\nTo learn more about the JIPipe Python API, visit https://jipipe.hki-jena.de/apidocs/python-current/index.html")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Python script")
public class MergingPythonScriptAlgorithm extends JIPipeMergingAlgorithm {

    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            PythonUtils.ALLOWED_PARAMETER_CLASSES);
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.Merge;
    private boolean cleanUpAfterwards = true;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();

    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public MergingPythonScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().build());
        registerSubParameter(scriptParameters);
    }

    /**
     * Creates a copy
     *
     * @param other the info
     */
    public MergingPythonScriptAlgorithm(MergingPythonScriptAlgorithm other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        registerSubParameter(scriptParameters);
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

    @SetJIPipeDocumentation(name = "Override Python environment", description = "If enabled, a different Python environment is used for this Node.")
    @JIPipeParameter("override-environment")
    public OptionalPythonEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalPythonEnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
    }
    @Override
    public void getExternalEnvironments(List<JIPipeEnvironment> target) {
        super.getExternalEnvironments(target);
        if(overrideEnvironment.isEnabled()) {
            target.add(overrideEnvironment.getContent());
        }
        else {
            target.add(PythonExtensionSettings.getInstance().getPythonEnvironment());
        }
        target.add(PythonAdapterExtensionSettings.getInstance().getPythonAdapterLibraryEnvironment());
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        JythonUtils.checkScriptParametersValidity(scriptParameters, new ParameterValidationReportContext(reportContext, this, "Script parameters", "script-parameters"), report);
        if (!isPassThrough()) {
            if (overrideEnvironment.isEnabled()) {
                report.report(new ParameterValidationReportContext(reportContext, this, "Override Python environment", "override-python-environment"), overrideEnvironment.getContent());
            } else {
                PythonExtensionSettings.checkPythonSettings(reportContext, report);
            }
        }
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        StringBuilder code = new StringBuilder();

        // Install the adapter that provides the JIPipe API
        PythonUtils.installAdapterCodeIfNeeded(code);

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
        code.append("\n").append(this.code.getCode(getProjectDirectory())).append("\n");

        // Add postprocessor code
        PythonUtils.addPostprocessorCode(code, getOutputSlots());

        // Run code
        PythonUtils.runPython(code.toString(),
                getOverrideEnvironment().isEnabled() ? getOverrideEnvironment().getContent() : PythonExtensionSettings.getInstance().getPythonEnvironment(),
                Collections.emptyList(), progressInfo);

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
        code.makeExternalScriptFileRelative(baseDirectory);
    }

    @SetJIPipeDocumentation(name = "Script", description = "The Python script to be executed. " +
            "The script comes with various API functions and variables that allow to communicate with JIPipe: " +
            "<ul>" +
            "<li><code>jipipe_inputs</code> is a dict of input slots.</li>" +
            "<li><code>jipipe_outputs</code> is a dict of output slots.</li>" +
            "<li><code>jipipe_annotations</code> is a dict of annotation variables of the current data batch.</li>" +
            "<li><code>jipipe_variables</code> is a dict of variables passed from the script parameters.</li>" +
            "</ul>" +
            "The script is designed to be used with the JIPipe Python API (supplied automatically by default). " +
            "You can find the full API documentation here: https://jipipe.hki-jena.de/apidocs/python-current/index.html")
    @JIPipeParameter("code")
    public PythonScript getCode() {
        return code;
    }

    @JIPipeParameter("code")
    public void setCode(PythonScript code) {
        this.code = code;
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

    @Override
    protected void onDeserialized(JsonNode node, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {
        super.onDeserialized(node, issues, notifications);
        PythonPlugin.createMissingPythonNotificationIfNeeded(notifications);
        PythonPlugin.createMissingLibJIPipePythonNotificationIfNeeded(notifications);
    }
}
