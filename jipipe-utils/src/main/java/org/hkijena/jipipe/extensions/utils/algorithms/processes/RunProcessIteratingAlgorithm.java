package org.hkijena.jipipe.extensions.utils.algorithms.processes;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.processes.ProcessEnvironment;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ProcessUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

@SetJIPipeDocumentation(name = "Run process (Iterating)", description = "Executes a process.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Process")
public class RunProcessIteratingAlgorithm extends JIPipeIteratingAlgorithm {

    private ProcessEnvironment processEnvironment = new ProcessEnvironment();
    private OptionalJIPipeExpressionParameter overrideArguments = new OptionalJIPipeExpressionParameter(true, "ARRAY()");
    private boolean outputOutputFolder = false;
    private boolean cleanUpAfterwards = true;

    public RunProcessIteratingAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().build());
    }

    public RunProcessIteratingAlgorithm(RunProcessIteratingAlgorithm other) {
        super(other);
        this.processEnvironment = new ProcessEnvironment(other.processEnvironment);
        this.overrideArguments = new OptionalJIPipeExpressionParameter(other.overrideArguments);
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.setOutputOutputFolder(other.outputOutputFolder);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path workDirectory = getNewScratch();
        Path inputPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "inputs");
        Path outputPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "outputs");
        progressInfo.log("Inputs will be written to: " + inputPath);
        progressInfo.log("Outputs will be extracted from: " + outputPath);

        // Save all inputs
        for (JIPipeDataSlot slot : getDataInputSlots()) {
            JIPipeDataTable dummy = slot.slice(Collections.singletonList(iterationStep.getInputRow(slot)));
            dummy.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, inputPath.resolve(slot.getName())), progressInfo.resolve("Save inputs"));
        }

        // Run process
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        for (Map.Entry<String, JIPipeTextAnnotation> entry : iterationStep.getMergedTextAnnotations().entrySet()) {
            variables.set(entry.getKey(), entry.getValue().getValue());
        }
        variables.set("input_folder", inputPath.toString());
        variables.set("output_folder", outputPath.toString());

        ProcessEnvironment environment = new ProcessEnvironment(getProcessEnvironment());
        if (overrideArguments.isEnabled()) {
            environment.setArguments(overrideArguments.getContent());
        }

        ProcessUtils.runProcess(environment, variables, Collections.emptyMap(), true, progressInfo);

        // Extract outputs
        if (outputOutputFolder) {
            iterationStep.addOutputData("Output folder", new FolderData(outputPath), progressInfo);
        }

        for (JIPipeDataSlot slot : getOutputSlots()) {
            if (outputOutputFolder && "Output folder".equals(slot.getName()))
                continue;
            Path slotPath = outputPath.resolve(slot.getName());
            if (Files.exists(slotPath.resolve("data-table.json"))) {
                JIPipeDataTable loaded = JIPipeDataTable.importData(new JIPipeFileSystemReadDataStorage(progressInfo, slotPath), progressInfo.resolve("Extracting output '" + slot.getName() + "'"));
                for (int i = 0; i < loaded.getRowCount(); i++) {
                    iterationStep.addOutputData(slot.getName(), loaded.getData(i, slot.getAcceptedDataType(), progressInfo), progressInfo);
                }
            } else {
                progressInfo.log("Unable to load slot from " + slotPath + ": No data-table.json");
            }
        }

        // Clean up
        if (cleanUpAfterwards) {
            try {
                PathUtils.deleteDirectoryRecursively(inputPath,
                        progressInfo.resolve("Cleanup"));
                PathUtils.deleteDirectoryRecursively(outputPath,
                        progressInfo.resolve("Cleanup"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @SetJIPipeDocumentation(name = "Process", description = "The process that should be executed. Click 'Edit' to setup the process.")
    @JIPipeParameter(value = "process-environment", important = true)
    public ProcessEnvironment getProcessEnvironment() {
        return processEnvironment;
    }

    @JIPipeParameter("process-environment")
    public void setProcessEnvironment(ProcessEnvironment processEnvironment) {
        this.processEnvironment = processEnvironment;
    }

    @SetJIPipeDocumentation(name = "Override arguments", description = "If enabled, override arguments of the environment with the one provided by the expression. " +
            "Please note that this expression has access to annotations.")
    @JIPipeParameter("override-arguments")
    public OptionalJIPipeExpressionParameter getOverrideArguments() {
        return overrideArguments;
    }

    @JIPipeParameter("override-arguments")
    public void setOverrideArguments(OptionalJIPipeExpressionParameter overrideArguments) {
        this.overrideArguments = overrideArguments;
    }

    @SetJIPipeDocumentation(name = "Output output folder", description = "If enabled, the output folder designated to the current data batch is also output into a slot 'Output folder'. Please " +
            "note that any existing slot with this name is replaced by a directory output slot. Data will be not be extracted from the process output.")
    @JIPipeParameter("output-output-folder")
    public boolean isOutputOutputFolder() {
        return outputOutputFolder;
    }

    @JIPipeParameter("output-output-folder")
    public void setOutputOutputFolder(boolean outputOutputFolder) {
        if (this.outputOutputFolder != outputOutputFolder) {
            this.outputOutputFolder = outputOutputFolder;
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            if (outputOutputFolder) {
                if (!slotConfiguration.hasOutputSlot("Output folder") || getOutputSlot("Output folder").getAcceptedDataType() != FolderData.class) {
                    if (slotConfiguration.hasOutputSlot("Output folder")) {
                        slotConfiguration.removeOutputSlot("Output folder", false);
                    }
                    slotConfiguration.addOutputSlot("Output folder", "Output folder designated to the current data batch", FolderData.class, null, false);
                }
            } else {
                if (slotConfiguration.hasOutputSlot("Output folder")) {
                    slotConfiguration.removeOutputSlot("Output folder", false);
                }
            }
        }
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
}
