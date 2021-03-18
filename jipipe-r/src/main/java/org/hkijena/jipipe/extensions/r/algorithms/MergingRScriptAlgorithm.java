package org.hkijena.jipipe.extensions.r.algorithms;

import com.github.rcaller.rstuff.RCaller;
import com.github.rcaller.rstuff.RCode;
import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.r.RExtensionSettings;
import org.hkijena.jipipe.extensions.r.RUtils;
import org.hkijena.jipipe.extensions.r.parameters.RScriptParameter;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "R script (merging)", description = "Allows to execute a custom R script. " +
        "The script is repeated for each data batch. Please note the each data batch can contain multiple items per slot. " +
        "The script comes with various API functions and variables that allow to communicate with JIPipe: " +
        "<ul>" +
        "<li><code>JIPipe.InputSlotRowCounts</code> contains named row counts for each slot</li>" +
        "<li><code>JIPipe.Annotations</code> contains the list of annotations (named strings)</li>" +
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
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "R script")
@JIPipeInputSlot(ResultsTableData.class)
@JIPipeOutputSlot(ImagePlusColorRGBData.class)
@JIPipeOutputSlot(ResultsTableData.class)
public class MergingRScriptAlgorithm extends JIPipeMergingAlgorithm {

    private RScriptParameter script = new RScriptParameter();
    private RCaller rCaller;
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.Merge;
    private JIPipeDynamicParameterCollection variables = new JIPipeDynamicParameterCollection(RUtils.ALLOWED_PARAMETER_CLASSES);

    public MergingRScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().build());
        registerSubParameter(variables);
    }

    public MergingRScriptAlgorithm(MergingRScriptAlgorithm other) {
        super(other);
        this.script = new RScriptParameter(other.script);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.variables = new JIPipeDynamicParameterCollection(other.variables);
        registerSubParameter(variables);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        if(!isPassThrough()) {
            RExtensionSettings.checkRSettings(report.forCategory("R"));
        }
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        try {
            if (!isPassThrough()) {
                RExtensionSettings.checkRSettings();
                rCaller = RCaller.create(RExtensionSettings.createRCallerOptions());
            }
            super.run(progressInfo);
        } finally {
            rCaller = null;
        }
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        RCode code = RCode.create();
        code.getCode().setLength(0);

        // RCaller provides its own methods for serialization, but
        // we don't need them (not compatible to data types)

        // Add user variables
        RUtils.parametersToR(code, variables);

        // Add annotations
        RUtils.annotationsToR(code, dataBatch.getAnnotations().values());

        Map<String, Path> inputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : getEffectiveInputSlots()) {
            Path tempPath = RuntimeSettings.generateTempDirectory("r-input");
            progressInfo.log("Input slot '" + slot.getName() + "' is stored in " + tempPath);
            JIPipeDataSlot dummy = dataBatch.toDummySlot(slot.getInfo(), this, slot);
            dummy.save(tempPath, null, progressInfo);
            inputSlotPaths.put(slot.getName(), tempPath);
        }

        RUtils.inputSlotsToR(code, inputSlotPaths, slotName -> getInputSlot(slotName).getRowCount());
        RUtils.installInputLoaderCode(code);

        Map<String, Path> outputSlotPaths = new HashMap<>();
        for (JIPipeDataSlot slot : getOutputSlots()) {
            Path tempPath = RuntimeSettings.generateTempDirectory("r-output");
            progressInfo.log("Output slot '" + slot.getName() + "' is stored in " + tempPath);
            outputSlotPaths.put(slot.getName(), tempPath);
        }
        RUtils.outputSlotsToR(code, getOutputSlots(), outputSlotPaths);
        RUtils.installOutputGeneratorCode(code);

        code.addRCode(script.getCode());
        rCaller.setRCode(code);
        RUtils.installPostprocessorCode(code);

        progressInfo.log(code.getCode().toString());
        rCaller.runOnly();

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

    @JIPipeDocumentation(name = "Script", description = "The script that contains the R commands. " +
            "The script comes with various API functions and variables that allow to communicate with JIPipe: " +
            "<ul>" +
            "<li><code>JIPipe.InputSlotRowCounts</code> contains named row counts for each slot</li>" +
            "<li><code>JIPipe.Annotations</code> contains the list of annotations (named strings)</li>" +
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

    @JIPipeDocumentation(name = "Annotation merge strategy", description = "Determines how annotations that are added in the R script are " +
            "merged into existing annotations.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            Object result = JOptionPane.showInputDialog(parent.getWindow(), "Please select the example:",
                    "Load example", JOptionPane.PLAIN_MESSAGE, null, Examples.values(), Examples.LoadIris);
            if (result instanceof Examples) {
                ((Examples) result).apply(this);
            }
        }
    }

    @JIPipeParameter("variables")
    @JIPipeDocumentation(name = "Script variables", description = "The parameters are passed as variables to the R script. The variables are named according to the " +
            "unique name (if valid variable names) and are also stored in a list 'JIPipe.Variables'.")
    public JIPipeDynamicParameterCollection getVariables() {
        return variables;
    }

    private enum Examples {
        LoadIris("Load IRIS data set", "library(datasets)\n\nJIPipe.AddOutputDataFrame(slot=\"Table\", data=iris)",
                new JIPipeInputSlot[0], new JIPipeOutputSlot[]{
                new DefaultJIPipeOutputSlot(ResultsTableData.class, "Table", null, false)
        }),
        PlotIris("Plot IRIS data set", "library(datasets)\n" +
                "\n" +
                "# Generate the output file name\n" +
                "png.file.name <- JIPipe.AddOutputPNGImagePath(slot=\"Plot\")\n\n" +
                "# Use standard R functions. Write into this file.\n" +
                "png(png.file.name, width = 800, height = 600)\n" +
                "plot(iris$Petal.Length, iris$Petal.Width, pch=21, bg=c(\"red\",\"green3\",\"blue\")[unclass(iris$Species)], main=\"Edgar Anderson's Iris Data\")\n" +
                "dev.off()\n" +
                "\n" +
                "# JIPipe will automatically load the data",
                new JIPipeInputSlot[0], new JIPipeOutputSlot[]{
                new DefaultJIPipeOutputSlot(ImagePlusColorRGBData.class, "Plot", null, false)
        });

        private final String name;
        private final String code;
        private final JIPipeInputSlot[] inputSlots;
        private final JIPipeOutputSlot[] outputSlots;

        Examples(String name, String code, JIPipeInputSlot[] inputSlots, JIPipeOutputSlot[] outputSlots) {
            this.name = name;
            this.code = code;
            this.inputSlots = inputSlots;
            this.outputSlots = outputSlots;
        }

        public void apply(MergingRScriptAlgorithm algorithm) {
            JIPipeParameterCollection.setParameter(algorithm, "script", new RScriptParameter(code));
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) algorithm.getSlotConfiguration();
            slotConfiguration.clearInputSlots(true);
            slotConfiguration.clearOutputSlots(true);
            for (JIPipeInputSlot inputSlot : inputSlots) {
                slotConfiguration.addInputSlot(inputSlot.slotName(), inputSlot.value(), true);
            }
            for (JIPipeOutputSlot outputSlot : outputSlots) {
                slotConfiguration.addOutputSlot(outputSlot.slotName(), outputSlot.value(), null, true);
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
