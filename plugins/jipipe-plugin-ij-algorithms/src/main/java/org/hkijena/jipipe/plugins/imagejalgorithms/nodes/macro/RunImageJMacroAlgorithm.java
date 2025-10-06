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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.macro;

import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.parameters.library.graph.InputSlotMapParameterCollection;
import org.hkijena.jipipe.plugins.parameters.library.graph.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.plugins.parameters.library.references.ImageJDataExportOperationRef;
import org.hkijena.jipipe.plugins.parameters.library.references.ImageJDataExporterRef;
import org.hkijena.jipipe.plugins.parameters.library.references.ImageJDataImportOperationRef;
import org.hkijena.jipipe.plugins.parameters.library.references.ImageJDataImporterRef;
import org.hkijena.jipipe.plugins.parameters.library.scripts.ImageJMacroParameter;
import org.hkijena.jipipe.plugins.strings.ImageJMacroData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import org.hkijena.jipipe.utils.scripting.ScriptUtils;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Run ImageJ Macro", description = "Runs a custom ImageJ macro. JIPipe will iterate through the iteration steps and execute operations to convert JIPipe data into their ImageJ equivalent (see JIPipe to ImageJ parameter). Then the macro code is executed, followed by operations to import " +
        "the result data into JIPipe data (see ImageJ to JIPipe parameter). Please feel free to click the 'Load example' button in the parameters to get started." +
        "\n\nPlease keep in mind the following remarks:\n\n" +
        "<ul>" +
        "<li>Input images are opened as windows named according to the input slot. You have to select windows with the select() function or comparable functions.</li>" +
        "<li>Output images are extracted by finding a window that is named according to the output slot. Ensure to rename() windows accordingly.</li>" +
        "<li>To extract the 'Results' table output, add an output of type 'Results table' and set the name to 'Results'. Alternatively, you can configure the output in 'JIPipe to ImageJ' and override the name to 'Results'</li>" +
        "<li>To import other tables, use a different slot name or set the appropriate configuration.</li>" +
        "<li>Please note that there is only one ROI manager. This is a restriction of ImageJ.</li>" +
        "<li>Annotations can also be accessed via a function getJIPipeAnnotation(key), which returns the string value of the annotation or an empty string if no value was set.</li>" +
        "<li>You can define variables that are passed from JIPipe to ImageJ. Variables are also created for incoming path-like data, named according to the slot name.</li>" +
        "</ul>")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(ImagePlusData.class)
@AddJIPipeInputSlot(ROI2DListData.class)
@AddJIPipeInputSlot(ResultsTableData.class)
@AddJIPipeInputSlot(PathData.class)
@AddJIPipeOutputSlot(ImagePlusData.class)
@AddJIPipeOutputSlot(ROI2DListData.class)
@AddJIPipeOutputSlot(ResultsTableData.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMacros", aliasName = "Run...")
public class RunImageJMacroAlgorithm extends JIPipeIteratingAlgorithm implements JIPipeScriptAlgorithm {
    public static final JIPipeDataSlotInfo SLOT_SCRIPT = JIPipeDataSlotInfo.builder().slotType(JIPipeSlotType.Input).dataClass(ImageJMacroData.class).name("Script").userModifiable(false).role(JIPipeDataSlotRole.Parameters).build();
    public static Class<?>[] ALLOWED_PARAMETER_CLASSES = new Class[]{
            String.class,
            Byte.class,
            Short.class,
            Integer.class,
            Double.class,
            Float.class,
            Path.class,
            Boolean.class
    };
    private final List<ImagePlus> initiallyOpenedImages = new ArrayList<>();
    private final List<Window> initiallyOpenedWindows = new ArrayList<>();
    private final InputSlotMapParameterCollection inputToImageJExporters;
    private final OutputSlotMapParameterCollection outputFromImageJImporters;
    private JIPipeDynamicParameterCollection macroParameters = new JIPipeDynamicParameterCollection(true, ALLOWED_PARAMETER_CLASSES);
    private int importDelay = 1000;
    private int exportDelay = 250;
     private ImageJMacroParameter code = new ImageJMacroParameter();
     private boolean externalCode = false;

    /**
     * @param info the info
     */
    public RunImageJMacroAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .build());
        registerSubParameter(macroParameters);

        // Importer settings
        inputToImageJExporters = new InputSlotMapParameterCollection(ImageJDataExportOperationRef.class, this, this::getDefaultExporterRef, false);
        inputToImageJExporters.updateSlots();
        registerSubParameter(inputToImageJExporters);

        // Exporter settings
        outputFromImageJImporters = new OutputSlotMapParameterCollection(ImageJDataImportOperationRef.class, this, this::getDefaultImporterRef, false);
        outputFromImageJImporters.updateSlots();
        registerSubParameter(outputFromImageJImporters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public RunImageJMacroAlgorithm(RunImageJMacroAlgorithm other) {
        super(other);
        this.importDelay = other.importDelay;
        this.exportDelay = other.exportDelay;
        this.macroParameters = new JIPipeDynamicParameterCollection(other.macroParameters);
        this.code = new ImageJMacroParameter(other.code);
        this.externalCode = other.externalCode;
        registerSubParameter(macroParameters);

        // Importer settings
        inputToImageJExporters = new InputSlotMapParameterCollection(ImageJDataExportOperationRef.class, this, this::getDefaultExporterRef, false);
        other.inputToImageJExporters.copyTo(inputToImageJExporters);
        registerSubParameter(inputToImageJExporters);

        // Exporter settings
        outputFromImageJImporters = new OutputSlotMapParameterCollection(ImageJDataImportOperationRef.class, this, this::getDefaultImporterRef, false);
        other.outputFromImageJImporters.copyTo(outputFromImageJImporters);
        registerSubParameter(outputFromImageJImporters);

        updateSlots();
    }

    private void updateSlots() {
        toggleSlot(SLOT_SCRIPT, externalCode);
        emitParameterUIChangedEvent();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if("code".equals(access.getKey()) && externalCode) {
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

    @SetJIPipeDocumentation(name = "Wait before importing", description = "Additional waiting time in milliseconds before results generated by ImageJ are imported back into JIPipe after the execution of the macro. Increase this delay if results are missing or are duplicated.")
    @JIPipeParameter("import-delay")
    public int getImportDelay() {
        return importDelay;
    }

    @JIPipeParameter("import-delay")
    public void setImportDelay(int importDelay) {
        this.importDelay = importDelay;
    }

    @SetJIPipeDocumentation(name = "Wait before executing", description = "Additional waiting time in milliseconds before the macro is executed. Increase this delay if the macro mis-behaves.")
    @JIPipeParameter("export-delay")
    public int getExportDelay() {
        return exportDelay;
    }

    @JIPipeParameter("export-delay")
    public void setExportDelay(int exportDelay) {
        this.exportDelay = exportDelay;
    }

    @SetJIPipeDocumentation(name = "JIPipe to ImageJ", description = "Use the following settings to change how inputs are converted from JIPipe to ImageJ.")
    @JIPipeParameter("input-to-imagej-exporters")
    public InputSlotMapParameterCollection getInputToImageJExporters() {
        return inputToImageJExporters;
    }

    @SetJIPipeDocumentation(name = "ImageJ to JIPipe", description = "Use the following settings to change how outputs are extracted from ImageJ to JIPipe.")
    @JIPipeParameter("output-from-imagej-importers")
    public OutputSlotMapParameterCollection getOutputFromImageJImporters() {
        return outputFromImageJImporters;
    }

    private Object getDefaultExporterRef(JIPipeDataSlotInfo info) {
        return new ImageJDataExportOperationRef(JIPipe.getImageJAdapters().getDefaultExporterFor(info.getDataClass()));
    }

    private Object getDefaultImporterRef(JIPipeDataSlotInfo info) {
        return new ImageJDataImportOperationRef(JIPipe.getImageJAdapters().getDefaultImporterFor(info.getDataClass()));
    }

    @SetJIPipeDocumentation(name = "Code", description = "The macro code. " + "Images are opened as windows named according to the input slot. You have to select windows with " +
            "the select() function or comparable functions. You have have one results table input which " +
            "can be addressed via the global functions. Input ROI are merged into one ROI manager.\n\n" +
            "You can define variables that are passed from JIPipe to ImageJ. Variables are also created for incoming path-like data, named according to the slot name. " +
            "Annotations can also be accessed via a function getJIPipeAnnotation(key) or getJIPipeTextAnnotation(key), which returns the string value of the annotation or an empty string if no value was set.")
    @JIPipeParameter("code")
    public ImageJMacroParameter getCode() {
        return code;
    }

    @JIPipeParameter("code")
    public void setCode(ImageJMacroParameter code) {
        this.code = code;
    }

    @Override
    public JIPipeParameterAccess getScriptParameterAccess() {
        return getParameterAccess("code");
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        backupWindows();
        try {
            prepareInputData(iterationStep, progressInfo);

            // Add delay
            if (exportDelay > 0) {
                try {
                    progressInfo.log("Waiting " + exportDelay + "ms before executing the code");
                    Thread.sleep(exportDelay);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            StringBuilder finalCode = new StringBuilder();
            // Inject annotations
            finalCode.append("function getJIPipeAnnotation(key) {\n");
            for (Map.Entry<String, JIPipeTextAnnotation> entry : iterationStep.getMergedTextAnnotations().entrySet()) {
                finalCode.append("if (key == \"").append(ScriptUtils.escapeString(entry.getKey())).append("\") { return \"").append(ScriptUtils.escapeString(entry.getValue().getValue())).append("\"; }\n");
            }
            finalCode.append("return \"\";\n");
            finalCode.append("}\n\n");
            // Inject annotations
            finalCode.append("function getJIPipeTextAnnotation(key) {\n");
            for (Map.Entry<String, JIPipeTextAnnotation> entry : iterationStep.getMergedTextAnnotations().entrySet()) {
                finalCode.append("if (key == \"").append(ScriptUtils.escapeString(entry.getKey())).append("\") { return \"").append(ScriptUtils.escapeString(entry.getValue().getValue())).append("\"; }\n");
            }
            finalCode.append("return \"\";\n");
            finalCode.append("}\n\n");

            // Inject parameters
            for (Map.Entry<String, JIPipeParameterAccess> entry : macroParameters.getParameters().entrySet()) {
                if (!ScriptUtils.isValidVariableName(entry.getKey()))
                    throw new IllegalArgumentException("Invalid variable name: " + entry.getKey());
                finalCode.append("var ").append(entry.getKey()).append(" = ");
                if (entry.getValue().getFieldClass() == Integer.class) {
                    int value = 0;
                    if (entry.getValue().get(Integer.class) != null)
                        value = entry.getValue().get(Integer.class);
                    finalCode.append(value);
                } else if (entry.getValue().getFieldClass() == Double.class) {
                    double value = 0;
                    if (entry.getValue().get(Double.class) != null)
                        value = entry.getValue().get(Double.class);
                    finalCode.append(value);
                } else if (entry.getValue().getFieldClass() == Float.class) {
                    float value = 0;
                    if (entry.getValue().get(Float.class) != null)
                        value = entry.getValue().get(Float.class);
                    finalCode.append(value);
                } else if (entry.getValue().getFieldClass() == Boolean.class) {
                    boolean value = false;
                    if (entry.getValue().get(Boolean.class) != null)
                        value = entry.getValue().get(Boolean.class);
                    finalCode.append(value);
                } else {
                    String value = "";
                    if (entry.getValue().get(String.class) != null)
                        value = "" + entry.getValue().get(String.class);
                    finalCode.append("\"").append(ScriptUtils.escapeString(value)).append("\"");
                }
                finalCode.append(";\n");
            }

            // Inject path data
            for (JIPipeDataSlot inputSlot : getNonParameterInputSlots()) {
                JIPipeData data = iterationStep.getInputData(inputSlot, JIPipeData.class, progressInfo);
                if (data instanceof PathData) {
                    if (!ScriptUtils.isValidVariableName(inputSlot.getName()))
                        throw new IllegalArgumentException("Invalid variable name " + inputSlot.getName());
                    finalCode.append("var ").append(inputSlot.getName()).append(" = ");
                    String value = "" + ((PathData) data).getPath();
                    finalCode.append("\"").append(ScriptUtils.escapeString(value)).append("\"");
                    finalCode.append(";\n");
                }
            }

            finalCode.append("\n").append(getMacroCode(iterationStep, getProjectDirectory(), progressInfo));


            try (IJLogToJIPipeProgressInfoPump ignored = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
                progressInfo.log("Executing macro: \n\n" + finalCode);

                Interpreter interpreter = new Interpreter();
                interpreter.run(finalCode.toString());

                if (importDelay > 0) {
                    try {
                        progressInfo.log("Waiting " + importDelay + "ms before importing results back into JIPipe");
                        Thread.sleep(importDelay);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                passOutputData(iterationStep, progressInfo);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            clearData(progressInfo.resolve("Cleanup"));
        }
    }

    private String getMacroCode(JIPipeSingleIterationStep iterationStep, Path projectDirectory, JIPipeProgressInfo progressInfo) {
        if(externalCode) {
            throw new RuntimeException("Not implemented yet");
        }
        else {
            return code.getCode();
        }
    }

    private void passOutputData(JIPipeSingleIterationStep iterationStep, JIPipeProgressInfo progressInfo) {
        for (JIPipeOutputDataSlot outputSlot : getOutputSlots()) {
            Object configuration = outputFromImageJImporters.get(outputSlot.getName()).get(Object.class);
            ImageJDataImporter importer;
            ImageJImportParameters parameters = new ImageJImportParameters(outputSlot.getName());

            if (configuration instanceof ImageJDataImporterRef) {
                importer = ((ImageJDataImporterRef) configuration).getInstance();
            } else if (configuration instanceof ImageJDataImportOperationRef operationRef) {
                importer = operationRef.getInstance();
                operationRef.configure(parameters);
            } else {
                throw new UnsupportedOperationException("Unknown parameter: " + parameters);
            }

            progressInfo.log("Importing data from ImageJ into " + outputSlot.getName() + " via " + parameters);
            JIPipeDataTable imported = importer.importData(null, parameters, progressInfo);
            for (int row = 0; row < imported.getRowCount(); row++) {
                iterationStep.addOutputData(outputSlot, imported.getData(row, JIPipeData.class, progressInfo).duplicate(progressInfo), progressInfo);
            }

            // Workaround bug: Not closing all outputs
            if (ImagePlusData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                ImagePlus image = WindowManager.getImage(outputSlot.getName());
                if (image != null) {
                    image.changes = false;
                    image.close();
                }
            }
        }
    }

    private void clearData(JIPipeProgressInfo progressInfo) {
        RoiManager.getRoiManager().reset();
        RoiManager.getRoiManager().close();
        ResultsTable.getResultsTable().reset();
        for (int i = 0; i < WindowManager.getImageCount(); ++i) {
            int id = WindowManager.getNthImageID(i + 1);
            ImagePlus image = WindowManager.getImage(id);
            if (!initiallyOpenedImages.contains(image)) {
                progressInfo.log("Closing image window: " + image);
                image.changes = false;
                image.close();
            }
        }
        closeAdditionalWindows(progressInfo);
    }

    private void backupWindows() {
        initiallyOpenedWindows.clear();
        initiallyOpenedWindows.addAll(Arrays.asList(WindowManager.getAllNonImageWindows()));
    }

    private void closeAdditionalWindows(JIPipeProgressInfo progressInfo) {
        for (Window window : WindowManager.getAllNonImageWindows()) {
            if (!initiallyOpenedWindows.contains(window)) {
                progressInfo.log("Closing window: " + window);
                window.setVisible(false);
                window.dispose();
            }
        }
    }

    /**
     * Loads input data, so it can be discovered by ImageJ
     */
    private void prepareInputData(JIPipeSingleIterationStep iterationStep, JIPipeProgressInfo progressInfo) {
//        long imageInputSlotCount = getInputSlots().stream().filter(slot -> JIPipeMultichannelImageData.class.isAssignableFrom(slot.getAcceptedDataType())).count();
        initiallyOpenedImages.clear();
        for (int i = 0; i < WindowManager.getImageCount(); ++i) {
            initiallyOpenedImages.add(WindowManager.getImage(i + 1));
        }
        for (JIPipeDataSlot inputSlot : getNonParameterInputSlots()) {
            JIPipeData data = iterationStep.getInputData(inputSlot, JIPipeData.class, progressInfo);
            Object configuration = inputToImageJExporters.get(inputSlot.getName()).get(Object.class);
            ImageJExportParameters parameters = new ImageJExportParameters(true, false, false, inputSlot.getName());
            ImageJDataExporter exporter;

            if (configuration instanceof ImageJDataExporterRef) {
                exporter = ((ImageJDataExporterRef) configuration).getInstance();
            } else if (configuration instanceof ImageJDataExportOperationRef) {
                ImageJDataExportOperationRef operationRef = (ImageJDataExportOperationRef) configuration;
                exporter = operationRef.getInstance();
                operationRef.configure(parameters);
            } else {
                throw new UnsupportedOperationException("Unknown parameter: " + configuration);
            }

            progressInfo.log("Converting " + data + " to ImageJ via " + parameters);
            exporter.exportData(data.duplicate(progressInfo), parameters, progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        long roiInputSlotCount = getNonParameterInputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ROI2DListData.class).count();
        long roiOutputSlotCount = getOutputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ROI2DListData.class).count();
        if (roiInputSlotCount > 1) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    reportContext,
                    "Too many ROI inputs!",
                    "ImageJ1 has no concept of multiple ROI Managers.",
                    "Please make sure to only have at most one ROI data input."));
        }
        if (roiOutputSlotCount > 1) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    reportContext,
                    "Too many ROI outputs!",
                    "ImageJ1 has no concept of multiple ROI Managers.",
                    "Please make sure to only have at most one ROI data outputs."));
        }
        for (String key : macroParameters.getParameters().keySet()) {
            if (!ScriptUtils.isValidVariableName(key)) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new ParameterValidationReportContext(reportContext, this, "Macro parameters", "macro-parameters"),
                        "'" + key + "' is an invalid ImageJ macro variable name!",
                        "Please ensure that macro variables are compatible with the ImageJ macro language."));
            }
        }
    }



    @JIPipeParameter(value = "macro-parameters", persistence = JIPipeParameterSerializationMode.Object)
    @SetJIPipeDocumentation(name = "Macro parameters", description = "The parameters are passed as variables to the macro.")
    public JIPipeDynamicParameterCollection getMacroParameters() {
        return macroParameters;
    }
}
