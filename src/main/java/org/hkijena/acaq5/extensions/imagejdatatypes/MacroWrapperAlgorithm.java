package org.hkijena.acaq5.extensions.imagejdatatypes;

import com.google.common.eventbus.Subscribe;
import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ConfigTraits;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterHolder;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQSubParameters;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ACAQROIData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ACAQResultsTableData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.utils.MacroUtils;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An algorithm that wraps around an ImageJ macro
 */
@ACAQDocumentation(name = "ImageJ Macro", description = "Runs a custom ImageJ macro")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Miscellaneous)
@AlgorithmInputSlot(ImagePlusData.class)
@AlgorithmInputSlot(ACAQROIData.class)
@AlgorithmInputSlot(ACAQResultsTableData.class)
@AlgorithmOutputSlot(ImagePlusData.class)
@AlgorithmOutputSlot(ACAQROIData.class)
@AlgorithmOutputSlot(ACAQResultsTableData.class)
@ConfigTraits(allowModify = true)
public class MacroWrapperAlgorithm extends ACAQIteratingAlgorithm {

    public static List<Class<? extends ACAQData>> IMAGEJ_DATA_CLASSES = new ArrayList<>();
    public static Class<?>[] ALLOWED_PARAMETER_CLASSES = new Class[]{
            String.class,
            Integer.class,
            Double.class,
            Float.class,
            Path.class
    };

    private MacroCode code = new MacroCode();
    private boolean strictMode = true;
    private ACAQDynamicParameterHolder macroParameters = new ACAQDynamicParameterHolder(ALLOWED_PARAMETER_CLASSES);

    private List<ImagePlus> initiallyOpenedImages = new ArrayList<>();
    private List<Window> initiallyOpenedWindows = new ArrayList<>();

    public MacroWrapperAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
                .allowOutputSlotInheritance(true)
                .restrictInputTo(IMAGEJ_DATA_CLASSES.toArray(new Class[0]))
                .restrictOutputTo(IMAGEJ_DATA_CLASSES.toArray(new Class[0]))
                .build());
        this.macroParameters.getEventBus().register(this);
    }

    public MacroWrapperAlgorithm(MacroWrapperAlgorithm other) {
        super(other);
        this.code = new MacroCode(other.code);
        this.macroParameters = new ACAQDynamicParameterHolder(other.macroParameters);
        this.macroParameters.getEventBus().register(this);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        backupWindows();
        prepareInputData(dataInterface);

        StringBuilder finalCode = new StringBuilder();
        // Inject parameters
        for (Map.Entry<String, ACAQParameterAccess> entry : macroParameters.getCustomParameters().entrySet()) {
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
            interpreter.run(finalCode.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            passOutputData(dataInterface);
            clearData();
        }
    }

    private void passOutputData(ACAQDataInterface dataInterface) {
        for (ACAQDataSlot outputSlot : getOutputSlots()) {
            if (ImagePlusData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                ImagePlus image = WindowManager.getImage(outputSlot.getName());
                try {
                    dataInterface.addOutputData(outputSlot, outputSlot.getAcceptedDataType().getConstructor(ImagePlus.class).newInstance(image.duplicate()));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            } else if (ACAQROIData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                dataInterface.addOutputData(outputSlot, new ACAQROIData(RoiManager.getRoiManager()));
            } else if (ACAQResultsTableData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                ResultsTable table = ResultsTable.getResultsTable();
                dataInterface.addOutputData(outputSlot, new ACAQResultsTableData((ResultsTable) table.clone()));
            }
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
            ACAQData data = dataInterface.getInputData(inputSlot);
            if (data instanceof ImagePlusData) {
                ImagePlus img = ((ImagePlusData) data).getImage().duplicate();
                img.setTitle(inputSlot.getName());
                img.show();
                WindowManager.setTempCurrentImage(img);
            } else if (data instanceof ACAQROIData) {
                RoiManager.getRoiManager().reset();
                ((ACAQROIData) data).addToRoiManager(RoiManager.getRoiManager());
            } else if (data instanceof ACAQResultsTableData) {
                ResultsTable.getResultsTable().reset();
                ((ACAQResultsTableData) data).addToTable(ResultsTable.getResultsTable());
            } else {
                throw new RuntimeException("Unsupported data: " + data);
            }
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        long roiInputSlotCount = getInputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ACAQROIData.class).count();
        long roiOutputSlotCount = getOutputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ACAQROIData.class).count();
        long resultsTableInputSlotCount = getInputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ACAQResultsTableData.class).count();
        long resultsTableOutputSlotCount = getOutputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ACAQResultsTableData.class).count();
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
        for (String key : macroParameters.getCustomParameters().keySet()) {
            if (!MacroUtils.isValidVariableName(key)) {
                report.forCategory("Macro Parameters").forCategory(key).reportIsInvalid("'" + key + "' is an invalid ImageJ macro variable name! Please ensure that macro variables are compatible with the ImageJ macro language.");
            }
        }

        if (strictMode) {
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

    @ACAQSubParameters("macro-parameters")
    @ACAQDocumentation(name = "Macro parameters", description = "Parameters that are passed as variables to the macro")
    public ACAQDynamicParameterHolder getMacroParameters() {
        return macroParameters;
    }

    @Subscribe
    public void onParameterStructureChanged(ParameterStructureChangedEvent event) {
        getEventBus().post(event);
    }
}

