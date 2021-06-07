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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.extensions.python.PythonExtensionSettings;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.JythonUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.nio.file.Path;
import java.util.Map;

/**
 * An algorithm that allows to run Python code
 */
@JIPipeDocumentation(name = "Python script (iterating)", description = "Runs a Python script that iterates through each data batch in the input slots. " +
        "This node uses an existing dedicated Python interpreter that must be set up in the application settings. ")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Python script")
public class IteratingPythonScriptAlgorithm extends JIPipeIteratingAlgorithm {

    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            PythonUtils.ALLOWED_PARAMETER_CLASSES);
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.Merge;
    private boolean cleanUpAfterwards = true;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();

    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public IteratingPythonScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().build());
        registerSubParameter(scriptParameters);
    }

    /**
     * Creates a copy
     *
     * @param other the info
     */
    public IteratingPythonScriptAlgorithm(IteratingPythonScriptAlgorithm other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        registerSubParameter(scriptParameters);
    }

    @JIPipeDocumentation(name = "Clean up data after processing", description = "If enabled, data is deleted from temporary directories after " +
            "the processing was finished. Disable this to make it possible to debug your scripts. The directories are accessible via the logs (Tools &gt; Logs).")
    @JIPipeParameter("cleanup-afterwards")
    public boolean isCleanUpAfterwards() {
        return cleanUpAfterwards;
    }

    @JIPipeParameter("cleanup-afterwards")
    public void setCleanUpAfterwards(boolean cleanUpAfterwards) {
        this.cleanUpAfterwards = cleanUpAfterwards;
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            slotConfiguration.clearInputSlots(true);
            slotConfiguration.clearOutputSlots(true);
            slotConfiguration.addSlot("Input", new JIPipeDataSlotInfo(ResultsTableData.class, JIPipeSlotType.Input, null), true);
            slotConfiguration.addSlot("Output", new JIPipeDataSlotInfo(ResultsTableData.class, JIPipeSlotType.Output, null), true);
            code.setCode("from jipipe.imagej import *\n" +
                    "\n" +
                    "# Get the input slot\n" +
                    "ds = jipipe_inputs[\"Input\"]\n" +
                    "\n" +
                    "# Get the first table from the slot\n" +
                    "table = load_table_file(data_slot=ds, row=0)\n" +
                    "\n" +
                    "print(table)\n" +
                    "\n" +
                    "# Get the output slot\n" +
                    "dso = jipipe_outputs[\"Output\"]\n" +
                    "\n" +
                    "# Add the table to the output slot\n" +
                    "add_table(table, dso)\n");
            getEventBus().post(new ParameterChangedEvent(this, "code"));
        }
    }

    @JIPipeDocumentation(name = "Override Python environment", description = "If enabled, a different Python environment is used for this Node.")
    @JIPipeParameter("override-environment")
    public OptionalPythonEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalPythonEnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        JythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
        if (!isPassThrough()) {
            if (overrideEnvironment.isEnabled()) {
                report.forCategory("Override Python environment").report(overrideEnvironment.getContent());
            } else {
                PythonExtensionSettings.checkPythonSettings(report.forCategory("Python"));
            }
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        StringBuilder code = new StringBuilder();

        // Install the adapter that provides the JIPipe API
        PythonUtils.installAdapterCodeIfNeeded(code);

        // Add user variables
        PythonUtils.parametersToPython(code, scriptParameters);

        // Add annotations
        PythonUtils.annotationsToPython(code, dataBatch.getAnnotations().values());

        // Install input slots
        Map<String, Path> inputSlotPaths = PythonUtils.installInputSlots(code, dataBatch, this, getEffectiveInputSlots(), progressInfo);

        // Install output slots
        Map<String, Path> outputSlotPaths = PythonUtils.installOutputSlots(code, getOutputSlots(), progressInfo);

        // Add main code
        code.append("\n").append(this.code.getCode()).append("\n");

        // Add postprocessor code
        PythonUtils.addPostprocessorCode(code, getOutputSlots());

        // Run Python
        PythonUtils.runPython(code.toString(),
                getOverrideEnvironment().isEnabled() ? getOverrideEnvironment().getContent() : PythonExtensionSettings.getInstance().getPythonEnvironment(),
                progressInfo);

        // Extract outputs
        PythonUtils.extractOutputs(dataBatch, outputSlotPaths, getOutputSlots(), annotationMergeStrategy, progressInfo);

        // Clean up
        if (cleanUpAfterwards) {
            PythonUtils.cleanup(inputSlotPaths, outputSlotPaths, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Script", description = "The Python script to be executed. " +
            "The script comes with various API functions and variables that allow to communicate with JIPipe: " +
            "<ul>" +
            "<li><code>jipipe_inputs</code> is a dict of input slots.</li>" +
            "<li><code>jipipe_outputs</code> is a dict of output slots.</li>" +
            "<li><code>jipipe_annotations</code> is a dict of annotation variables of the current data batch.</li>" +
            "<li><code>jipipe_variables</code> is a dict of variables passed from the script parameters.</li>" +
            "</ul>" +
            "The script is designed to be used with the JIPipe Python API (supplied automatically by default). " +
            "You can find the full API documentation here: https://www.jipipe.org/documentation/standard-library/python/api/")
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

    @JIPipeDocumentation(name = "Annotation merge strategy", description = "Determines how annotations that are added in the Python script are " +
            "merged into existing annotations.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
