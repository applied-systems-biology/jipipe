package org.hkijena.acaq5.extension.api.algorithms.macro;

import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ConfigTraits;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMultichannelImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQROIData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQResultsTableData;
import org.hkijena.acaq5.extension.api.macro.MacroCode;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An algorithm that wraps around an ImageJ macro
 */
@ACAQDocumentation(name = "ImageJ Macro", description = "Runs a custom ImageJ macro")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Miscellaneous)
@AlgorithmInputSlot(ACAQMultichannelImageData.class)
@AlgorithmInputSlot(ACAQMaskData.class)
@AlgorithmInputSlot(ACAQROIData.class)
@AlgorithmInputSlot(ACAQResultsTableData.class)
@AlgorithmOutputSlot(ACAQMultichannelImageData.class)
@AlgorithmOutputSlot(ACAQMaskData.class)
@AlgorithmOutputSlot(ACAQROIData.class)
@AlgorithmOutputSlot(ACAQResultsTableData.class)
@ConfigTraits(allowModify = true)
public class MacroWrapperAlgorithm extends ACAQIteratingAlgorithm {

    private MacroCode code = new MacroCode();
    private boolean strictMode = true;

    private List<ImagePlus> initiallyOpenedImages = new ArrayList<>();

    public static List<Class<? extends ACAQData>> IMAGEJ_DATA_CLASSES = Arrays.asList(ACAQMultichannelImageData.class,
            ACAQGreyscaleImageData.class,
            ACAQMaskData.class,
            ACAQROIData.class,
            ACAQResultsTableData.class);

    public MacroWrapperAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().restrictInputTo(IMAGEJ_DATA_CLASSES.toArray(new Class[0]))
                .restrictOutputTo(IMAGEJ_DATA_CLASSES.toArray(new Class[0])).build());
    }

    public MacroWrapperAlgorithm(MacroWrapperAlgorithm other) {
        super(other);
        this.code = new MacroCode(other.code);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        prepareInputData(dataInterface);

        Interpreter interpreter = new Interpreter();
        interpreter.run(code.getCode());

        passOutputData(dataInterface);
        clearData(dataInterface);
    }

    private void passOutputData(ACAQDataInterface dataInterface) {
        for (ACAQDataSlot outputSlot : getOutputSlots()) {
            if(ACAQMultichannelImageData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                ImagePlus image = WindowManager.getImage(outputSlot.getName());
                try {
                    dataInterface.addOutputData(outputSlot, outputSlot.getAcceptedDataType().getConstructor(ImagePlus.class).newInstance(image.duplicate()));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
            else if(ACAQROIData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                dataInterface.addOutputData(outputSlot, new ACAQROIData(RoiManager.getRoiManager()));
            }
            else if(ACAQResultsTableData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                ResultsTable table = ResultsTable.getResultsTable();
                dataInterface.addOutputData(outputSlot, new ACAQResultsTableData((ResultsTable) table.clone()));
            }
        }
    }

    private void clearData(ACAQDataInterface dataInterface) {
        RoiManager.getRoiManager().reset();
        RoiManager.getRoiManager().close();
        ResultsTable.getResultsTable().reset();
        for(int i = 0; i < WindowManager.getImageCount(); ++i) {
            ImagePlus image = WindowManager.getImage(i + 1);
            if(!initiallyOpenedImages.contains(image)) {
                image.close();
            }
        }
    }

    /**
     * Loads input data, so it can be discovered by ImageJ
     */
    private void prepareInputData(ACAQDataInterface dataInterface) {
//        long imageInputSlotCount = getInputSlots().stream().filter(slot -> ACAQMultichannelImageData.class.isAssignableFrom(slot.getAcceptedDataType())).count();
        initiallyOpenedImages.clear();
        for(int i = 0; i < WindowManager.getImageCount(); ++i) {
            initiallyOpenedImages.add(WindowManager.getImage(i + 1));
        }
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            ACAQData data = dataInterface.getInputData(inputSlot);
            if(data instanceof ACAQMultichannelImageData) {
                ImagePlus img = ((ACAQMultichannelImageData) data).getImage().duplicate();
                img.setTitle(inputSlot.getName());
                img.show();
                WindowManager.setTempCurrentImage(img);
            }
            else if(data instanceof ACAQROIData) {
                RoiManager.getRoiManager().reset();
                ((ACAQROIData) data).addToRoiManager(RoiManager.getRoiManager());
            }
            else if(data instanceof ACAQResultsTableData) {
                ResultsTable.getResultsTable().reset();
                ((ACAQResultsTableData)data).addToTable(ResultsTable.getResultsTable());
            }
            else {
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
        if(roiInputSlotCount > 1) {
            report.reportIsInvalid("Too many ROI inputs! Please make sure to only have at most one ROI data input.");
        }
        if(roiOutputSlotCount > 1) {
            report.reportIsInvalid("Too many ROI outputs! Please make sure to only have at most one ROI data output.");
        }
        if(resultsTableInputSlotCount > 1) {
            report.reportIsInvalid("Too many results table inputs! Please make sure to only have at most one results table data input.");
        }
        if(resultsTableOutputSlotCount > 1) {
            report.reportIsInvalid("Too many results table outputs! Please make sure to only have at most one results table data output.");
        }
        if(strictMode) {
            for (ACAQDataSlot inputSlot : getInputSlots()) {
                if (ACAQMultichannelImageData.class.isAssignableFrom(inputSlot.getAcceptedDataType())) {
                    if (!code.getCode().contains("\"" + inputSlot.getName() + "\"")) {
                        report.reportIsInvalid("Input image '" + inputSlot.getName() + "' is not used! You can use selectWindow(\"" + inputSlot.getName() + "\"); to process the image. Disable strict mode to stop this message.");
                    }
                }
            }
            for (ACAQDataSlot outputSlot : getOutputSlots()) {
                if (ACAQMultichannelImageData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
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
}
