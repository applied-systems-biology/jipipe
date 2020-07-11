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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1;

import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.compat.ImageJDatatypeAdapter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.api.registries.JIPipeImageJAdapterRegistry;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.scripts.ImageJMacro;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.MacroUtils;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.api.algorithm.JIPipeIteratingAlgorithm.ITERATING_ALGORITHM_DESCRIPTION;

/**
 * An algorithm that wraps around an ImageJ macro
 */
@JIPipeDocumentation(name = "ImageJ Macro", description = "Runs a custom ImageJ macro. " +
        "Images are opened as windows named according to the input slot. You have to select windows with " +
        "the select() function or comparable functions. You have have one results table input which " +
        "can be adressed via the global functions. Input ROI are merged into one ROI manager.\n\n" +
        "You can define variables that are passed from JIPipe to ImageJ. Variables are also created for incoming path-like data." + "\n\n" + ITERATING_ALGORITHM_DESCRIPTION)
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor)
@AlgorithmInputSlot(ImagePlusData.class)
@AlgorithmInputSlot(ROIListData.class)
@AlgorithmInputSlot(ResultsTableData.class)
@AlgorithmInputSlot(PathData.class)
@AlgorithmOutputSlot(ImagePlusData.class)
@AlgorithmOutputSlot(ROIListData.class)
@AlgorithmOutputSlot(ResultsTableData.class)
public class MacroWrapperAlgorithm extends JIPipeIteratingAlgorithm {
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

    private ImageJMacro code = new ImageJMacro();
    private boolean strictMode = true;
    private JIPipeDynamicParameterCollection macroParameters = new JIPipeDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);

    private List<ImagePlus> initiallyOpenedImages = new ArrayList<>();
    private List<Window> initiallyOpenedWindows = new ArrayList<>();

    /**
     * @param declaration the declaration
     */
    public MacroWrapperAlgorithm(JIPipeAlgorithmDeclaration declaration) {
        super(declaration, JIPipeDefaultMutableSlotConfiguration.builder()
                .allowOutputSlotInheritance(true)
                .restrictInputTo(getCompatibleTypes())
                .restrictOutputTo(getCompatibleTypes())
                .addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, null)
                .build());
        this.code.setCode("// To add variables, click the [+] button below.\n" +
                "// They will be created automatically before this code fragment.\n\n" +
                "// Each input image slot creates a window with its name.\n" +
                "// You have to select it, first\n" +
                "selectWindow(\"Input\");\n\n" +
                "// Apply your operations here\n\n" +
                "// JIPipe extracts output images based on their window name\n" +
                "rename(\"Output\");\n");
        this.macroParameters.getEventBus().register(this);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public MacroWrapperAlgorithm(MacroWrapperAlgorithm other) {
        super(other);
        this.code = new ImageJMacro(other.code);
        this.macroParameters = new JIPipeDynamicParameterCollection(other.macroParameters);
        this.macroParameters.getEventBus().register(this);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        backupWindows();
        prepareInputData(dataInterface);

        StringBuilder finalCode = new StringBuilder();
        // Inject parameters
        for (Map.Entry<String, JIPipeParameterAccess> entry : macroParameters.getParameters().entrySet()) {
            if (!MacroUtils.isValidVariableName(entry.getKey()))
                throw new IllegalArgumentException("Invalid variable name " + entry.getKey());
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
                finalCode.append("\"").append(MacroUtils.escapeString(value)).append("\"");
            }
            finalCode.append(";\n");
        }

        // Inject path data
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            JIPipeData data = dataInterface.getInputData(inputSlot, JIPipeData.class);
            if (data instanceof PathData) {
                if (!MacroUtils.isValidVariableName(inputSlot.getName()))
                    throw new IllegalArgumentException("Invalid variable name " + inputSlot.getName());
                finalCode.append("var ").append(inputSlot.getName()).append(" = ");
                String value = "" + ((PathData) data).getPath();
                finalCode.append("\"").append(MacroUtils.escapeString(value)).append("\"");
                finalCode.append(";\n");
            }
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

    private void passOutputData(JIPipeDataBatch dataInterface) {
        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            ImageJDatatypeAdapter adapter = JIPipeImageJAdapterRegistry.getInstance().getAdapterForJIPipeData(outputSlot.getAcceptedDataType());
            JIPipeData data = adapter.importFromImageJ(outputSlot.getName());
            dataInterface.addOutputData(outputSlot, data);

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

    private void clearData() {
        RoiManager.getRoiManager().reset();
        RoiManager.getRoiManager().close();
        ResultsTable.getResultsTable().reset();
        for (int i = 0; i < WindowManager.getImageCount(); ++i) {
            int id = WindowManager.getNthImageID(i + 1);
            ImagePlus image = WindowManager.getImage(id);
            System.out.println("Encounter: " + image);
            if (!initiallyOpenedImages.contains(image)) {
                image.changes = false;
                System.out.println("Close: " + image);
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
    private void prepareInputData(JIPipeDataBatch dataInterface) {
//        long imageInputSlotCount = getInputSlots().stream().filter(slot -> JIPipeMultichannelImageData.class.isAssignableFrom(slot.getAcceptedDataType())).count();
        initiallyOpenedImages.clear();
        for (int i = 0; i < WindowManager.getImageCount(); ++i) {
            initiallyOpenedImages.add(WindowManager.getImage(i + 1));
        }
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            JIPipeData data = dataInterface.getInputData(inputSlot, JIPipeData.class);
            if (data instanceof PathData)
                continue;
            ImageJDatatypeAdapter adapter = JIPipeImageJAdapterRegistry.getInstance().getAdapterForJIPipeData(data);
            adapter.convertJIPipeToImageJ(data, true, false, inputSlot.getName());
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        long roiInputSlotCount = getInputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ROIListData.class).count();
        long roiOutputSlotCount = getOutputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ROIListData.class).count();
        long resultsTableInputSlotCount = getInputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ResultsTableData.class).count();
        long resultsTableOutputSlotCount = getOutputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ResultsTableData.class).count();
        if (roiInputSlotCount > 1) {
            report.reportIsInvalid("Too many ROI inputs!",
                    "ImageJ1 has no concept of multiple ROI Managers.",
                    "Please make sure to only have at most one ROI data input.",
                    this);
        }
        if (roiOutputSlotCount > 1) {
            report.reportIsInvalid("Too many ROI outputs!",
                    "ImageJ1 has no concept of multiple ROI Managers.",
                    "Please make sure to only have at most one ROI data output.",
                    this);
        }
        if (resultsTableInputSlotCount > 1) {
            report.reportIsInvalid("Too many results table inputs!",
                    "ImageJ1 has no concept of multiple result tables.",
                    "Please make sure to only have at most one results table data input.",
                    this);
        }
        if (resultsTableOutputSlotCount > 1) {
            report.reportIsInvalid("Too many results table outputs!",
                    "ImageJ1 has no concept of multiple result tables.",
                    "Please make sure to only have at most one results table data output.",
                    this);
        }
        for (String key : macroParameters.getParameters().keySet()) {
            if (!MacroUtils.isValidVariableName(key)) {
                report.forCategory("Macro Parameters").forCategory(key).reportIsInvalid("Invalid name!",
                        "'" + key + "' is an invalid ImageJ macro variable name!",
                        "Please ensure that macro variables are compatible with the ImageJ macro language.",
                        this);
            }
        }

        if (strictMode) {
            for (JIPipeDataSlot inputSlot : getInputSlots()) {
                if (ImagePlusData.class.isAssignableFrom(inputSlot.getAcceptedDataType())) {
                    if (!code.getCode().contains("\"" + inputSlot.getName() + "\"")) {
                        report.reportIsInvalid("Strict mode: Unused input image",
                                "Input image '" + inputSlot.getName() + "' is not used!",
                                "You can use selectWindow(\"" + inputSlot.getName() + "\"); to process the image. Disable strict mode to stop this message.",
                                this);
                    }
                }
            }
            for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                if (ImagePlusData.class.isAssignableFrom(outputSlot.getAcceptedDataType())) {
                    if (!code.getCode().contains("\"" + outputSlot.getName() + "\"")) {
                        report.reportIsInvalid("Strict mode: Unused output image",
                                "Output image '" + outputSlot.getName() + "' is not used!",
                                "You should rename an output image via rename(\"" + outputSlot.getName() + "\"); to allow JIPipe to find it. Disable strict mode to stop this message.",
                                this);
                    }
                }
            }
        }
    }

    @JIPipeDocumentation(name = "Code")
    @JIPipeParameter("code")
    public ImageJMacro getCode() {
        return code;
    }

    @JIPipeParameter("code")
    public void setCode(ImageJMacro code) {
        this.code = code;
    }

    @JIPipeDocumentation(name = "Strict mode", description = "If enabled, macro code is scanned for common mistakes and an error is generated.")
    @JIPipeParameter("strict-mode")
    public boolean isStrictMode() {
        return strictMode;
    }

    @JIPipeParameter("strict-mode")
    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    @JIPipeParameter(value = "macro-parameters", persistence = JIPipeParameterPersistence.Object)
    @JIPipeDocumentation(name = "Macro parameters", description = "The parameter are passed as variables to the macro.")
    public JIPipeDynamicParameterCollection getMacroParameters() {
        return macroParameters;
    }

    /**
     * Returns all types compatible with the {@link MacroWrapperAlgorithm}
     *
     * @return compatible data types
     */
    public static Class[] getCompatibleTypes() {
        return JIPipeImageJAdapterRegistry.getInstance().getSupportedJIPipeDataTypes().toArray(new Class[0]);
    }
}

