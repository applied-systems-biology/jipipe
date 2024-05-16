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

package org.hkijena.jipipe.plugins.r.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadata;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterSerializationMode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.r.OptionalREnvironment;
import org.hkijena.jipipe.plugins.r.REnvironmentAccessNode;
import org.hkijena.jipipe.plugins.r.RPluginApplicationSettings;
import org.hkijena.jipipe.plugins.r.RUtils;
import org.hkijena.jipipe.plugins.r.parameters.RScriptParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "R script (iterating)", description = "Allows to execute a custom R script. " +
        "The script is repeated for each data batch. Please note the each data batch only contains one item (row) per slot. " +
        "The script comes with various API functions and variables that allow to communicate with JIPipe: " +
        "<ul>" +
        "<li><code>JIPipe.InputSlotRowCounts</code> contains named row counts for each slot. Is always 1 for each slot.</li>" +
        "<li><code>JIPipe.TextAnnotations</code> contains the list of annotations (named strings)</li>" +
        "<li><code>JIPipe.Variables</code> contains the list of variables defined by parameters (named values). " +
        "If a parameter's unique key is a valid variable name, it will also be available as variable.</li>" +
        "<li><code>JIPipe.GetInputFolder(slot, row=0)</code> returns the data folder of the specified slot. " +
        "The data folder contains the input row stored in standardized JIPipe format.</li>" +
        "<li><code>JIPipe.GetInputAsDataFrame(slot, row=0)</code> reads the specified input data row as data frame.</li>" +
        "<li><code>JIPipe.AddOutputFolder(slot, annotations=list())</code> adds a new output folder into the specified" +
        " output slot and returns the folder path. Optionally, you can assign annotations to add as list of named strings. " +
        "Please note that the folder must contain data according to the slot's data type (an image file or a CSV file respectively)</li>" +
        "<li><code>JIPipe.AddOutputDataFrame(data, slot, annotations=list())</code> adds the specified data frame into the specified output slot. " +
        "Optionally, you can add annotations to the data.</li>" +
        "<li><code>JIPipe.AddOutputPNGImagePath(data, slot, annotations=list())</code> generates an image file path to be added as output and return the path. " +
        "Please note that you must use png() or other functions to actually write this file." +
        "Optionally, you can add annotations to the data.</li>" +
        "<li><code>JIPipe.AddOutputTIFFImagePath(data, slot, annotations=list())</code> generates an image file path to be added as output and return the path. " +
        "Please note that you must use tiff() or other functions to actually write this file." +
        "Optionally, you can add annotations to the data.</li>" +
        "</ul>")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "R script")
@AddJIPipeInputSlot(ResultsTableData.class)
@AddJIPipeOutputSlot(ImagePlusColorRGBData.class)
@AddJIPipeOutputSlot(ResultsTableData.class)
public class IteratingRScriptAlgorithm extends JIPipeIteratingAlgorithm implements REnvironmentAccessNode {

    private RScriptParameter script = new RScriptParameter();
    private JIPipeDynamicParameterCollection variables = new JIPipeDynamicParameterCollection(true, RUtils.ALLOWED_PARAMETER_CLASSES);
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.Merge;
    private boolean cleanUpAfterwards = true;
    private OptionalREnvironment overrideEnvironment = new OptionalREnvironment();

    public IteratingRScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().build());
        registerSubParameter(variables);
    }

    public IteratingRScriptAlgorithm(IteratingRScriptAlgorithm other) {
        super(other);
        this.script = new RScriptParameter(other.script);
        this.variables = new JIPipeDynamicParameterCollection(other.variables);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.overrideEnvironment = new OptionalREnvironment(other.overrideEnvironment);
        registerSubParameter(variables);
    }

    @Override
    public void getExternalEnvironments(List<JIPipeEnvironment> target) {
        super.getExternalEnvironments(target);
       target.add(getConfiguredREnvironment());
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

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (!isPassThrough()) {
           report.report(reportContext, getConfiguredREnvironment());
        }
    }

    @SetJIPipeDocumentation(name = "Override R environment", description = "If enabled, a different R environment is used for this Node.")
    @JIPipeParameter("override-environment")
    public OptionalREnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalREnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        StringBuilder code = new StringBuilder();

        // Add user variables
        RUtils.parametersToR(code, variables);

        // Add annotations
        RUtils.textAnnotationsToR(code, iterationStep.getMergedTextAnnotations().values());

        Path workDirectory = getNewScratch();

        Map<String, Path> inputSlotPaths = new HashMap<>();
        List<JIPipeInputDataSlot> dummySlots = new ArrayList<>();
        for (JIPipeDataSlot slot : getDataInputSlots()) {
            Path tempPath = workDirectory.resolve("inputs").resolve(slot.getName());
            try {
                Files.createDirectories(tempPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            progressInfo.log("Input slot '" + slot.getName() + "' is stored in " + tempPath);
            JIPipeInputDataSlot dummy = (JIPipeInputDataSlot) iterationStep.toDummySlot(slot.getInfo(), this, slot, progressInfo);
            dummy.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, tempPath), progressInfo);
            inputSlotPaths.put(slot.getName(), tempPath);
            dummySlots.add(dummy);
        }

        RUtils.inputSlotsToR(code, inputSlotPaths, dummySlots);
        RUtils.installInputLoaderCode(code);

        Map<String, Path> outputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : getOutputSlots()) {
            Path tempPath = workDirectory.resolve("outputs").resolve(slot.getName());
            try {
                Files.createDirectories(tempPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            progressInfo.log("Output slot '" + slot.getName() + "' is stored in " + tempPath);
            outputSlotPaths.put(slot.getName(), tempPath);
        }
        RUtils.outputSlotsToR(code, getOutputSlots(), outputSlotPaths);
        RUtils.installOutputGeneratorCode(code);

        code.append("\n").append(script.getCode(getProjectDirectory())).append("\n");
        RUtils.installPostprocessorCode(code);

        progressInfo.log(code.toString());

        // Export as script and run it
        RUtils.runR(code.toString(),
                getConfiguredREnvironment(),
                progressInfo);

        for (JIPipeOutputDataSlot outputSlot : getOutputSlots()) {
            Path storagePath = outputSlotPaths.get(outputSlot.getName());
            JIPipeDataTableMetadata table = JIPipeDataTableMetadata.loadFromJson(outputSlotPaths.get(outputSlot.getName()).resolve("data-table.json"));
            for (int row = 0; row < table.getRowCount(); row++) {
                JIPipeDataInfo dataInfo = table.getDataTypeOf(row);
                Path rowStoragePath = table.getRowStoragePath(storagePath, row);
                JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, rowStoragePath), dataInfo.getDataClass(), progressInfo);
                iterationStep.addOutputData(outputSlot, data, table.getRowList().get(row).getTextAnnotations(), annotationMergeStrategy, progressInfo);
            }
        }

        // Clean up
        if (cleanUpAfterwards) {
            PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
        }
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);
        script.makeExternalScriptFileRelative(baseDirectory);
    }

    @JIPipeParameter(value = "variables", persistence = JIPipeParameterSerializationMode.Object)
    @SetJIPipeDocumentation(name = "Script variables", description = "The parameters are passed as variables to the R script. The variables are named according to the " +
            "unique name (if valid variable names) and are also stored in a list 'JIPipe.Variables'.")
    public JIPipeDynamicParameterCollection getVariables() {
        return variables;
    }

    @SetJIPipeDocumentation(name = "Script", description = "The script that contains the R commands. " +
            "The script comes with various API functions and variables that allow to communicate with JIPipe: " +
            "<ul>" +
            "<li><code>JIPipe.InputSlotRowCounts</code> contains named row counts for each slot. Is always 1 for each slot.</li>" +
            "<li><code>JIPipe.TextAnnotations</code> contains the list of text annotations (named strings)</li>" +
            "<li><code>JIPipe.Variables</code> contains the list of variables defined by parameters (named values). " +
            "If a parameter's unique key is a valid variable name, it will also be available as variable.</li>" +
            "<li><code>JIPipe.GetInputFolder(slot, row=0)</code> returns the data folder of the specified slot. " +
            "The data folder contains the input row stored in standardized JIPipe format.</li>" +
            "<li><code>JIPipe.GetInputAsDataFrame(slot, row=0)</code> reads the specified input data row as data frame.</li>" +
            "<li><code>JIPipe.AddOutputFolder(slot, annotations=list())</code> adds a new output folder into the specified" +
            " output slot and returns the folder path. Optionally, you can assign annotations to add as list of named strings. " +
            "Please note that the folder must contain data according to the slot's data type (an image file or a CSV file respectively)</li>" +
            "<li><code>JIPipe.AddOutputDataFrame(data, slot, annotations=list())</code> adds the specified data frame into the specified output slot. " +
            "Optionally, you can add annotations to the data.</li>" +
            "<li><code>JIPipe.AddOutputPNGImagePath(data, slot, annotations=list())</code> generates an image file path to be added as output and return the path. " +
            "Please note that you must use png() or other functions to actually write this file." +
            "Optionally, you can add annotations to the data.</li>" +
            "<li><code>JIPipe.AddOutputTIFFImagePath(data, slot, annotations=list())</code> generates an image file path to be added as output and return the path. " +
            "Please note that you must use tiff() or other functions to actually write this file." +
            "Optionally, you can add annotations to the data.</li>" +
            "</ul>")
    @JIPipeParameter("script")
    public RScriptParameter getScript() {
        return script;
    }

    @JIPipeParameter("script")
    public void setScript(RScriptParameter script) {
        this.script = script;
    }

    @SetJIPipeDocumentation(name = "Annotation merge strategy", description = "Determines how annotations that are added in the R script are " +
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
