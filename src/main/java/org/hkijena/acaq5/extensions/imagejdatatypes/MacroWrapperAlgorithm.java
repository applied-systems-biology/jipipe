package org.hkijena.acaq5.extensions.imagejdatatypes;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ConfigTraits;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.registries.ACAQImageJAdapterRegistry;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.utils.MacroUtils;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An algorithm that wraps around an ImageJ macro
 */
@ACAQDocumentation(name = "ImageJ Macro", description = "Runs a custom ImageJ macro")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(ImagePlusData.class)
@AlgorithmInputSlot(ROIData.class)
@AlgorithmInputSlot(ResultsTableData.class)
@AlgorithmOutputSlot(ImagePlusData.class)
@AlgorithmOutputSlot(ROIData.class)
@AlgorithmOutputSlot(ResultsTableData.class)
@ConfigTraits(allowModify = true)
public class MacroWrapperAlgorithm extends ACAQIteratingAlgorithm {
    public static Class<?>[] ALLOWED_PARAMETER_CLASSES = new Class[]{
            String.class,
            Byte.class,
            Short.class,
            Integer.class,
            Double.class,
            Float.class,
            Path.class
    };

    private MacroCode code = new MacroCode();
    private boolean strictMode = true;
    private boolean batchMode = false;
    private ACAQDynamicParameterCollection macroParameters = new ACAQDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);

    private List<ImagePlus> initiallyOpenedImages = new ArrayList<>();
    private List<Window> initiallyOpenedWindows = new ArrayList<>();

    /**
     * @param declaration the declaration
     */
    public MacroWrapperAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
                .allowOutputSlotInheritance(true)
                .restrictInputTo(getCompatibleTypes())
                .restrictOutputTo(getCompatibleTypes())
                .build());
        this.macroParameters.getEventBus().register(this);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public MacroWrapperAlgorithm(MacroWrapperAlgorithm other) {
        super(other);
        this.code = new MacroCode(other.code);
        this.macroParameters = new ACAQDynamicParameterCollection(other.macroParameters);
        this.macroParameters.getEventBus().register(this);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        backupWindows();
        prepareInputData(dataInterface);

        StringBuilder finalCode = new StringBuilder();
        // Inject parameters
        for (Map.Entry<String, ACAQParameterAccess> entry : macroParameters.getParameters().entrySet()) {
            if (!MacroUtils.isValidVariableName(entry.getKey()))
                throw new IllegalArgumentException("Invalid variable name " + entry.getKey());
            finalCode.append("var ").append(entry.getKey()).append(" = ");
            if (entry.getValue().getFieldClass() == Integer.class) {
                int value = 0;
                if (entry.getValue().get() != null)
                    value = entry.getValue().get();
                finalCode.append(value);
            } else if (entry.getValue().getFieldClass() == Double.class) {
                double value = 0;
                if (entry.getValue().get() != null)
                    value = entry.getValue().get();
                finalCode.append(value);
            } else if (entry.getValue().getFieldClass() == Float.class) {
                float value = 0;
                if (entry.getValue().get() != null)
                    value = entry.getValue().get();
                finalCode.append(value);
            } else {
                String value = "";
                if (entry.getValue().get() != null)
                    value = "" + entry.getValue().get();
                finalCode.append("\"").append(MacroUtils.escapeString(value)).append("\"");
            }
            finalCode.append(";\n");
        }

        finalCode.append("\n").append(code.getCode());

        Interpreter interpreter = new Interpreter();
        try {
            if (batchMode) {
                ImagePlus result = interpreter.runBatchMacro(finalCode.toString(), IJ.getImage());
                WindowManager.setTempCurrentImage(result);
            } else {
                interpreter.run(finalCode.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            passOutputData(dataInterface);
            clearData();
        }
    }

    private void passOutputData(ACAQDataInterface dataInterface) {
        for (ACAQDataSlot outputSlot : getOutputSlots()) {
            ImageJDatatypeAdapter adapter = ACAQImageJAdapterRegistry.getInstance().getAdapterForACAQData(outputSlot.getAcceptedDataType());
            ACAQData data;
            if (batchMode)
                data = adapter.importFromImageJ(null); // Fetch from current image only
            else
                data = adapter.importFromImageJ(outputSlot.getName());
            dataInterface.addOutputData(outputSlot, data);
        }
    }

    private void clearData() {
        RoiManager.getRoiManager().reset();
        RoiManager.getRoiManager().close();
        ResultsTable.getResultsTable().reset();
        for (int i = 0; i < WindowManager.getImageCount(); ++i) {
            ImagePlus image = WindowManager.getImage(i + 1);
            if (!initiallyOpenedImages.contains(image)) {
                image.changes = false;
                image.close();
            }
        }
        closeAdditionalWindows();
    }

    private void backupWindows() {
        initiallyOpenedWindows.clear();
        initiallyOpenedWindows.addAll(Arrays.asList(WindowManager.getAllNonImageWindows()));
    }

    private void closeAdditionalWindows() {
        for (Window window : WindowManager.getAllNonImageWindows()) {
            if (!initiallyOpenedWindows.contains(window)) {
                window.setVisible(false);
                window.dispose();
            }
        }
    }

    /**
     * Loads input data, so it can be discovered by ImageJ
     */
    private void prepareInputData(ACAQDataInterface dataInterface) {
//        long imageInputSlotCount = getInputSlots().stream().filter(slot -> ACAQMultichannelImageData.class.isAssignableFrom(slot.getAcceptedDataType())).count();
        initiallyOpenedImages.clear();
        for (int i = 0; i < WindowManager.getImageCount(); ++i) {
            initiallyOpenedImages.add(WindowManager.getImage(i + 1));
        }
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            ACAQData data = dataInterface.getInputData(inputSlot, ACAQData.class);
            ImageJDatatypeAdapter adapter = ACAQImageJAdapterRegistry.getInstance().getAdapterForACAQData(data);
            if (batchMode) {
                adapter.convertACAQToImageJ(data, true, true, inputSlot.getName());
            } else {
                adapter.convertACAQToImageJ(data, true, false, inputSlot.getName());
            }
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        long imageInputSlotCount = getInputSlots().stream().filter(slot -> ImagePlusData.class.isAssignableFrom(slot.getAcceptedDataType())).count();
        long imageOutputSlotCount = getOutputSlots().stream().filter(slot -> ImagePlusData.class.isAssignableFrom(slot.getAcceptedDataType())).count();
        long roiInputSlotCount = getInputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ROIData.class).count();
        long roiOutputSlotCount = getOutputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ROIData.class).count();
        long resultsTableInputSlotCount = getInputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ResultsTableData.class).count();
        long resultsTableOutputSlotCount = getOutputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ResultsTableData.class).count();
        if (batchMode && imageInputSlotCount != 1) {
            report.reportIsInvalid("Only exactly one input image is allowed! This requirement is caused by the batch mode setting.");
        }
        if (batchMode && imageOutputSlotCount != 1) {
            report.reportIsInvalid("Only exactly one output image is allowed! This requirement is caused by the batch mode setting.");
        }
        if (roiInputSlotCount > 1) {
            report.reportIsInvalid("Too many ROI inputs! Please make sure to only have at most one ROI data input.");
        }
        if (roiOutputSlotCount > 1) {
            report.reportIsInvalid("Too many ROI outputs! Please make sure to only have at most one ROI data output.");
        }
        if (resultsTableInputSlotCount > 1) {
            report.reportIsInvalid("Too many results table inputs! Please make sure to only have at most one results table data input.");
        }
        if (resultsTableOutputSlotCount > 1) {
            report.reportIsInvalid("Too many results table outputs! Please make sure to only have at most one results table data output.");
        }
        for (String key : macroParameters.getParameters().keySet()) {
            if (!MacroUtils.isValidVariableName(key)) {
                report.forCategory("Macro Parameters").forCategory(key).reportIsInvalid("'" + key + "' is an invalid ImageJ macro variable name! Please ensure that macro variables are compatible with the ImageJ macro language.");
            }
        }

        if (strictMode && !batchMode) {
            for (ACAQDataSlot inputSlot : getInputSlots()) {
                if (ImagePlusData.class.isAssignableFrom(inputSlot.getAcceptedDataType())) {
                    if (!code.getCode().contains("\"" + inputSlot.getName() + "\"")) {
                        report.reportIsInvalid("Input image '" + inputSlot.getName() + "' is not used! You can use selectWindow(\"" + inputSlot.getName() + "\"); to process the image. Disable strict mode to stop this message.");
                    }
                }
            }
            for (ACAQDataSlot outputSlot : getOutputSlots()) {
                if (ImagePlusData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                    if (!code.getCode().contains("\"" + outputSlot.getName() + "\"")) {
                        report.reportIsInvalid("Output image '" + outputSlot.getName() + "' is not used! You should rename an output image via rename(\"" + outputSlot.getName() + "\"); to allow ACAQ5 to find it. Disable strict mode to stop this message.");
                    }
                }
            }
        }
    }

    @ACAQDocumentation(name = "Code")
    @ACAQParameter("code")
    public MacroCode getCode() {
        return code;
    }

    @ACAQParameter("code")
    public void setCode(MacroCode code) {
        this.code = code;
    }

    @ACAQDocumentation(name = "Strict mode", description = "If enabled, macro code is scanned for common mistakes and an error is generated.")
    @ACAQParameter("strict-mode")
    public boolean isStrictMode() {
        return strictMode;
    }

    @ACAQParameter("strict-mode")
    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    @ACAQParameter("macro-parameters")
    @ACAQDocumentation(name = "Macro parameters", description = "The parameter are passed as variables to the macro.")
    public ACAQDynamicParameterCollection getMacroParameters() {
        return macroParameters;
    }

    /**
     * Triggered when the parameter structure of macro parameters is changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterStructureChanged(ParameterStructureChangedEvent event) {
        getEventBus().post(event);
    }

//    @ACAQDocumentation(name = "Batch mode", description = "If enabled, the macro might be able to run on servers.\n" +
//            "Requires that there is exactly one input and one output image.")
//    @ACAQParameter("batch-mode")
//    public boolean isBatchMode() {
//        return batchMode;
//    }
//
//    @ACAQParameter("batch-mode")
//    public void setBatchMode(boolean batchMode) {
//        this.batchMode = batchMode;
//    }

    public static Class[] getCompatibleTypes() {
        return ACAQImageJAdapterRegistry.getInstance().getSupportedACAQDataTypes().toArray(new Class[0]);
    }
}

