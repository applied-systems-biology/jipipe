package org.hkijena.jipipe.extensions.r.algorithms;

import com.github.rcaller.rstuff.RCaller;
import com.github.rcaller.rstuff.RCallerOptions;
import com.github.rcaller.rstuff.RCode;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.r.RExtensionSettings;
import org.hkijena.jipipe.extensions.r.parameters.RScriptParameter;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.MacroUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "R script (iterating)", description = "Allows to execute a custom R script. " +
        "The inputs are made available as CSV files accessible via variables named according to the slot name. Additionally, variables 'Input.[Input name].File'" +
        " are made available. Outputs must be written into the files according to 'Output.[Input name].File' (done automatically by default for data frames). Plots should be written in a standard pixel format like PNG. " +
        "SVG/PS/PDF is not supported. " +
        "Annotations are available as variables and inside an 'Annotations' list variable.")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "R script")
public class IteratingRScriptAlgorithm extends JIPipeIteratingAlgorithm {

    private RScriptParameter script = new RScriptParameter();
    private RCaller rCaller;
    private boolean annotationsAsVariables = true;
    private boolean customWriteCSV = false;
    private boolean customReadCSV = false;

    public IteratingRScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
        .restrictInputTo(ResultsTableData.class)
        .restrictOutputTo(ResultsTableData.class, ImagePlusData.class)
        .build());
    }

    public IteratingRScriptAlgorithm(IteratingRScriptAlgorithm other) {
        super(other);
        this.script = new RScriptParameter(other.script);
        this.customWriteCSV = other.customWriteCSV;
        this.customReadCSV = other.customReadCSV;
        this.annotationsAsVariables = other.annotationsAsVariables;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        try {
            if(!isPassThrough()) {
                if(!RExtensionSettings.checkRSettings()) {
                    throw new UserFriendlyRuntimeException("The R installation is invalid!\n" +
                            "R=" + RExtensionSettings.getInstance().getRExecutable() + "\n" +
                            "RScript=" + RExtensionSettings.getInstance().getRScriptExecutable(),
                            "R is not configured!",
                            getName(),
                            "This node requires an installation of R. Either R is not installed or JIPipe cannot find R.",
                            "Please install R from https://www.r-project.org/. If R is installed, go to Project > Application settings > Extensions > R  integration and " +
                                    "manually override R executable and RScript executable (please refer to the documentation in the settings page).");
                }
                rCaller = RCaller.create(RExtensionSettings.createRCallerOptions());
            }
            super.run(progressInfo);
        }
        finally {
            rCaller = null;
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        RCode code = RCode.create();

        // RCaller provides its own methods for serialization, but
        // we don't need them (not compatible to data types)

        // Generate files for each output
        Map<String, Path> generatedTables = new HashMap<>();
        Map<String, Path> generatedPlots = new HashMap<>();

        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            if(ResultsTableData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                Path tempFile = RuntimeSettings.generateTempFile("jipipe-r", ".csv");
                generatedTables.put(outputSlot.getName(), tempFile);
            }
            else if(ImagePlusData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                Path tempFile = RuntimeSettings.generateTempFile("jipipe-r", ".png");
                generatedPlots.put(outputSlot.getName(), tempFile);
            }
        }

        // Add annotations
        if(annotationsAsVariables) {
            code.addRCode("Annotations <- list()");
            for (JIPipeAnnotation annotation : dataBatch.getAnnotations().values()) {
                if(MacroUtils.isValidVariableName(annotation.getName())) {
                    code.addString(annotation.getName(), annotation.getValue());
                }
                else {
                    progressInfo.log("Info: Tried to add annotation '" + annotation.getName() + "' as variable, but the name is not a valid variable name. " +
                            "Only accessible via the 'Annotations' list.");
                }
                code.addRCode(String.format("Annotations$\"%s\" <- \"%s\"", MacroUtils.escapeString(annotation.getName()),
                        MacroUtils.escapeString(annotation.getValue())));
            }
        }

        // Add I/O variables
        for (Map.Entry<String, Path> entry : generatedTables.entrySet()) {
            code.addString("Output." + entry.getKey() + ".File", MacroUtils.escapeString(entry.getValue().toAbsolutePath().toString()));
        }
        for (Map.Entry<String, Path> entry : generatedPlots.entrySet()) {
            code.addString("Output." + entry.getKey() + ".File", MacroUtils.escapeString(entry.getValue().toAbsolutePath().toString()));
        }
        for (JIPipeDataSlot inputSlot : getEffectiveInputSlots()) {
            ResultsTableData resultsTableData = dataBatch.getInputData(inputSlot, ResultsTableData.class, progressInfo);
            Path tempFile = RuntimeSettings.generateTempFile("jipipe-r", ".csv");
            resultsTableData.saveAsCSV(tempFile);

            code.addString("Input." + inputSlot.getName() + ".File", MacroUtils.escapeString(tempFile.toAbsolutePath().toString()));
            if(!customReadCSV) {
                code.addRCode(inputSlot.getName() + " <- read.csv(file=\"" + MacroUtils.escapeString(tempFile.toAbsolutePath().toString()) + "\")");
            }
        }

        code.addRCode(script.getCode());
        rCaller.setRCode(code);

        // Add code after main code
        if(!customWriteCSV) {
            for (Map.Entry<String, Path> entry : generatedTables.entrySet()) {
                code.addRCode("write.csv(" + entry.getKey() + ", file=\"" + MacroUtils.escapeString(entry.getValue().toAbsolutePath().toString()) + "\")");
            }
        }

        rCaller.runOnly();

        // Extract outputs
        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            if (ResultsTableData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                Path tableFile = generatedTables.get(outputSlot.getName());
                try {
                    ResultsTableData resultsTableData = ResultsTableData.fromCSV(tableFile);
                    dataBatch.addOutputData(outputSlot, resultsTableData, progressInfo);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else  if(ImagePlusData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                Path imageFile = generatedPlots.get(outputSlot.getName());
                ImagePlus image =  IJ.openImage(imageFile.toString());
                if(image.getBitDepth() != 24) {
                    // R saved indexed colors that break apart easily, so we enforce RGB conversion
                    ColorProcessor colorProcessor = new ColorProcessor(image.getProcessor().getBufferedImage());
                    image.setProcessor(colorProcessor);
                }
                dataBatch.addOutputData(outputSlot, new ImagePlusData(image), progressInfo);
            }
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            if(!MacroUtils.isValidVariableName(outputSlot.getName())) {
                report.forCategory("Output slots").reportIsInvalid("Invalid output slot name!",
                        "All output slots must have valid R variable names. Following name is not valid: " + outputSlot.getName(),
                        "Change the slot name.",
                        this);
            }
        }
        for (JIPipeDataSlot inputSlot : getEffectiveInputSlots()) {
            if(!MacroUtils.isValidVariableName(inputSlot.getName())) {
                report.forCategory("Input slots").reportIsInvalid("Invalid input slot name!",
                        "All input slots must have valid R variable names. Following name is not valid: " + inputSlot.getName(),
                        "Change the slot name.",
                        this);
            }
        }
    }

    @JIPipeDocumentation(name = "Script", description = "The script that contains the R commands. " +
            "You have access to variables 'Input.[Input name].File' and 'Output.[Output name].File' that point to the input/output files for " +
            "the specified slot. By default, CSV tables will be read and written automatically into variables named according to the slot. " +
            "Annotations are also available as variables, and within the 'Annotations' list variable that also contains annotation names " +
            "that cannot be written into variables.")
    @JIPipeParameter("script")
    public RScriptParameter getScript() {
        return script;
    }

    @JIPipeParameter("script")
    public void setScript(RScriptParameter script) {
        this.script = script;
    }

    @JIPipeDocumentation(name = "Custom write CSV", description = "If enabled, " +
            "the script is provided with variables 'Output.[Output name].File' that will contain the CSV file for the corresponding table output. " +
            "Must be manually written via write.csv or other methods.")
    @JIPipeParameter("custom-write-csv")
    public boolean isCustomWriteCSV() {
        return customWriteCSV;
    }

    @JIPipeParameter("custom-write-csv")
    public void setCustomWriteCSV(boolean customWriteCSV) {
        this.customWriteCSV = customWriteCSV;
    }

    @JIPipeDocumentation(name = "Custom read CSV", description = "If enabled, the script is only provided with variables 'Input.[Input name].File' " +
            "that will contain the CSV file for the corresponding table input. You must manually read the file via read.csv or other methods.")
    @JIPipeParameter("custom-read-csv")
    public boolean isCustomReadCSV() {
        return customReadCSV;
    }

    @JIPipeParameter("custom-read-csv")
    public void setCustomReadCSV(boolean customReadCSV) {
        this.customReadCSV = customReadCSV;
    }

    @JIPipeDocumentation(name = "Add annotations as variables", description = "If enabled, annotations are added as variables (if compatible to code). " +
            "Alternatively, all annotations are available in a list 'Annotations'")
    @JIPipeParameter("annotations-as-variables")
    public boolean isAnnotationsAsVariables() {
        return annotationsAsVariables;
    }

    @JIPipeParameter("annotations-as-variables")
    public void setAnnotationsAsVariables(boolean annotationsAsVariables) {
        this.annotationsAsVariables = annotationsAsVariables;
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            Object result = JOptionPane.showInputDialog(parent.getWindow(), "Please select the example:",
                    "Load example", JOptionPane.PLAIN_MESSAGE, null, Examples.values(), Examples.LoadIris);
            if(result instanceof Examples) {
                ((Examples) result).apply(this);
            }
        }
    }

    private enum Examples {
        LoadIris("Load IRIS data set", "library(datasets)\n\nTable <- iris",
                new JIPipeInputSlot[0], new JIPipeOutputSlot[] {
                new DefaultJIPipeOutputSlot(ResultsTableData.class, "Table", null, false)
        }),
        PlotIris("Plot IRIS data set", "library(datasets)\n" +
                "\n" +
                "# You must write into the provided file\n" +
                "png(Output.Plot.File, width = 800, height = 600)\n" +
                "plot(iris$Petal.Length, iris$Petal.Width, pch=21, bg=c(\"red\",\"green3\",\"blue\")[unclass(iris$Species)], main=\"Edgar Anderson's Iris Data\")\n" +
                "dev.off()\n" +
                "\n" +
                "# JIPipe will automatically load the data in Output.Plot.File",
                new JIPipeInputSlot[0], new JIPipeOutputSlot[] {
                new DefaultJIPipeOutputSlot(ImagePlusData.class, "Plot", null, false)
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

        public void apply(IteratingRScriptAlgorithm algorithm) {
            JIPipeParameterCollection.setParameter(algorithm, "script", new RScriptParameter(code));
            JIPipeParameterCollection.setParameter(algorithm, "custom-write-csv", false);
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
