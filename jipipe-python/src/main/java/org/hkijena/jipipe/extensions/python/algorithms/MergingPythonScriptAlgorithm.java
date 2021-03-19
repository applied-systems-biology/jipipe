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

import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.extensions.python.PythonExtensionSettings;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.JythonUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * An algorithm that allows to run Python code
 */
@JIPipeDocumentation(name = "Python script (merging)", description = "Runs a Python script that iterates through each data batch in the input slots. " +
        "This node uses an existing dedicated Python interpreter that must be set up in the application settings. ")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Python script")
public class MergingPythonScriptAlgorithm extends JIPipeMergingAlgorithm {

    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            PythonUtils.ALLOWED_PARAMETER_CLASSES);
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.Merge;

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
        registerSubParameter(scriptParameters);
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            slotConfiguration.clearInputSlots(true);
            slotConfiguration.clearOutputSlots(true);
            slotConfiguration.addSlot("Table", new JIPipeDataSlotInfo(ResultsTableData.class, JIPipeSlotType.Input, null), true);
            slotConfiguration.addSlot("Table", new JIPipeDataSlotInfo(ResultsTableData.class, JIPipeSlotType.Output, null), true);
            code.setCode("from org.hkijena.jipipe.extensions.tables.datatypes import ResultsTableData\n" +
                    "\n" +
                    "# Fetch the input table from the first input slot\n" +
                    "input_table = data_batch.getInputData(input_slots[0], ResultsTableData)\n" +
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
            getEventBus().post(new ParameterChangedEvent(this, "code"));
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        JythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
        if(!isPassThrough()) {
            PythonExtensionSettings.checkPythonSettings(report.forCategory("Python"));
        }
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        StringBuilder code = new StringBuilder();

        // Install the adapter that provides the JIPipe API
        PythonUtils.installAdapterCodeIfNeeded(code);

        // Add user variables
        PythonUtils.parametersToPython(code, scriptParameters);

        // Add annotations
        PythonUtils.annotationsToPython(code, dataBatch.getAnnotations().values());

        // Install input slots
        Map<String, Path> inputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : getEffectiveInputSlots()) {
            Path tempPath = RuntimeSettings.generateTempDirectory("py-input");
            progressInfo.log("Input slot '" + slot.getName() + "' is stored in " + tempPath);
            JIPipeDataSlot dummy = dataBatch.toDummySlot(slot.getInfo(), this, slot);
            dummy.save(tempPath, null, progressInfo);
            inputSlotPaths.put(slot.getName(), tempPath);
        }
        PythonUtils.inputSlotsToPython(code, inputSlotPaths);

        // Install output slots
        Map<String, Path> outputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : getOutputSlots()) {
            Path tempPath = RuntimeSettings.generateTempDirectory("py-output");
            progressInfo.log("Output slot '" + slot.getName() + "' is stored in " + tempPath);
            outputSlotPaths.put(slot.getName(), tempPath);
        }
        PythonUtils.outputSlotsToPython(code, getOutputSlots(), outputSlotPaths);
        code.append("\n").append(this.code.getCode()).append("\n");
        PythonUtils.addPostprocessorCode(code, getOutputSlots());

        String finalCode = code.toString();
        progressInfo.log(finalCode);
        Path codeFilePath = RuntimeSettings.generateTempFile("py", ".py");
        try {
            Files.write(codeFilePath, finalCode.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PythonUtils.runPython(codeFilePath, progressInfo);

        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            Path storagePath = outputSlotPaths.get(outputSlot.getName());
            JIPipeExportedDataTable table = JIPipeExportedDataTable.loadFromJson(outputSlotPaths.get(outputSlot.getName()).resolve("data-table.json"));
            for (int row = 0; row < table.getRowCount(); row++) {
                JIPipeDataInfo dataInfo = table.getDataTypeOf(row);
                Path rowStoragePath = table.getRowStoragePath(storagePath, row);
                JIPipeData data = JIPipe.importData(rowStoragePath, dataInfo.getDataClass());
                dataBatch.addOutputData(outputSlot, data, table.getRowList().get(row).getAnnotations(), annotationMergeStrategy, progressInfo);
            }
        }

        // Clean up
        progressInfo.log("Cleaning up ...");
        for (Map.Entry<String, Path> entry : inputSlotPaths.entrySet()) {
            try {
                FileUtils.deleteDirectory(entry.getValue().toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (Map.Entry<String, Path> entry : outputSlotPaths.entrySet()) {
            try {
                FileUtils.deleteDirectory(entry.getValue().toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
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
