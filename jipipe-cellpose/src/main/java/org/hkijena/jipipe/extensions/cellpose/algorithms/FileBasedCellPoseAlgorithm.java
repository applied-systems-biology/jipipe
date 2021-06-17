package org.hkijena.jipipe.extensions.cellpose.algorithms;

import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.cellpose.CellPoseModel;
import org.hkijena.jipipe.extensions.cellpose.CellPoseSettings;
import org.hkijena.jipipe.extensions.cellpose.CellPoseUtils;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseModelData;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseSizeModelData;
import org.hkijena.jipipe.extensions.cellpose.parameters.EnhancementParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.ModelParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.OutputParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.PerformanceParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.ThresholdParameters;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalDoubleParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalPathParameter;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.utils.MacroUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JIPipeDocumentation(name = "Cellpose (File based)", description = "Runs Cellpose on the input image. The image is provided as file. This node supports both segmentation in 3D and executing " +
        "Cellpose for each 2D image plane. " +
        "This node can generate a multitude of outputs, although only ROI is activated by default. " +
        "Go to the 'Outputs' parameter section to enable the other outputs." +
        "<ul>" +
        "<li><b>Labels:</b> A grayscale image where each connected component is assigned a unique value. " +
        "Please note that ImageJ will run into issues if too many objects are present and the labels are saved in int32. ImageJ would load them as float32.</li>" +
        "<li><b>Flows:</b> An RGB image that indicates the flow of each pixel. Convert it to HSB to extract information.</li>" +
        "<li><b>Probabilities:</b> An image indicating the probabilities for each pixel.</li>" +
        "<li><b>Styles:</b> A vector summarizing each image.</li>" +
        "<li><b>ROI:</b> ROI of the segmented areas.</li>" +
        "</ul>" +
        "All outputs except the labels are returned as files. " +
        "Please note that you need to setup a valid Python environment with Cellpose installed. You can find the setting in Project &gt; Application settings &gt; Extensions &gt; Cellpose.")
@JIPipeInputSlot(value = FileData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FileData.class, slotName = "Labels")
@JIPipeOutputSlot(value = FileData.class, slotName = "Flows")
@JIPipeOutputSlot(value = FileData.class, slotName = "Probabilities")
@JIPipeOutputSlot(value = FileData.class, slotName = "Styles")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
public class FileBasedCellPoseAlgorithm extends JIPipeMergingAlgorithm {

    private ModelParameters modelParameters = new ModelParameters();
    private PerformanceParameters performanceParameters = new PerformanceParameters();
    private EnhancementParameters enhancementParameters = new EnhancementParameters();
    private ThresholdParameters thresholdParameters = new ThresholdParameters();
    private OutputParameters outputParameters = new OutputParameters();
    private OptionalDoubleParameter diameter = new OptionalDoubleParameter(30.0, true);
    private boolean enable3DSegmentation = false;
    private OptionalAnnotationNameParameter diameterAnnotation = new OptionalAnnotationNameParameter("Diameter", true);
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();
    private OptionalPathParameter overrideOutputPath = new OptionalPathParameter();

    public FileBasedCellPoseAlgorithm(JIPipeNodeInfo info) {
        super(info);
        updateOutputSlots();
        updateInputSlots();
        registerSubParameter(modelParameters);
        registerSubParameter(performanceParameters);
        registerSubParameter(enhancementParameters);
        registerSubParameter(thresholdParameters);
        registerSubParameter(outputParameters);
    }

    public FileBasedCellPoseAlgorithm(FileBasedCellPoseAlgorithm other) {
        super(other);
        this.diameter = new OptionalDoubleParameter(other.diameter);
        this.diameterAnnotation = new OptionalAnnotationNameParameter(other.diameterAnnotation);
        this.modelParameters = new ModelParameters(other.modelParameters);
        this.outputParameters = new OutputParameters(other.outputParameters);
        this.performanceParameters = new PerformanceParameters(other.performanceParameters);
        this.enhancementParameters = new EnhancementParameters(other.enhancementParameters);
        this.thresholdParameters = new ThresholdParameters(other.thresholdParameters);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.enable3DSegmentation = other.enable3DSegmentation;
        this.overrideOutputPath = new OptionalPathParameter(other.overrideOutputPath);
        updateOutputSlots();
        updateInputSlots();
        registerSubParameter(modelParameters);
        registerSubParameter(performanceParameters);
        registerSubParameter(enhancementParameters);
        registerSubParameter(thresholdParameters);
        registerSubParameter(outputParameters);
    }

    @JIPipeDocumentation(name = "Enable 3D segmentation", description = "If enabled, Cellpose will segment in 3D. Otherwise, " +
            "any 3D image will be processed per-slice. Please note that 3D segmentation requires large amounts of memory.")
    @JIPipeParameter("enable-3d-segmentation")
    public boolean isEnable3DSegmentation() {
        return enable3DSegmentation;
    }

    @JIPipeParameter("enable-3d-segmentation")
    public void setEnable3DSegmentation(boolean enable3DSegmentation) {
        this.enable3DSegmentation = enable3DSegmentation;
    }

    @JIPipeDocumentation(name = "Override Python environment", description = "If enabled, a different Python environment is used for this Node. Otherwise " +
            "the one in the Project > Application settings > Extensions > Cellpose is used.")
    @JIPipeParameter("override-environment")
    public OptionalPythonEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalPythonEnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
    }

    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        super.onParameterChanged(event);
        if (event.getSource() == outputParameters) {
            updateOutputSlots();
        } else if (event.getSource() == modelParameters && event.getKey().equals("model")) {
            updateInputSlots();
        }
    }

    private void updateInputSlots() {
        if (modelParameters.getModel() != CellPoseModel.Custom) {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            if (getInputSlotMap().containsKey("Pretrained model")) {
                slotConfiguration.removeInputSlot("Pretrained model", false);
            }
            if (getInputSlotMap().containsKey("Size model")) {
                slotConfiguration.removeInputSlot("Size model", false);
            }
        } else {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            if (!getInputSlotMap().containsKey("Pretrained model")) {
                slotConfiguration.addSlot("Pretrained model", new JIPipeDataSlotInfo(CellPoseModelData.class, JIPipeSlotType.Input, null), false);
            }
            if (!getInputSlotMap().containsKey("Size model")) {
                JIPipeDataSlotInfo slotInfo = new JIPipeDataSlotInfo(CellPoseSizeModelData.class, JIPipeSlotType.Input, null);
                slotInfo.setOptional(true);
                slotConfiguration.addSlot("Size model", slotInfo, false);
            }
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        if (!isPassThrough()) {
            if (overrideEnvironment.isEnabled()) {
                report.forCategory("Override Python environment").report(overrideEnvironment.getContent());
            } else {
                CellPoseSettings.checkPythonSettings(report.forCategory("Python"));
            }
        }
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        Path workDirectory;

        // Create work directory
        if (overrideOutputPath.isEnabled()) {
            workDirectory = overrideOutputPath.getContent();
            try {
                if (Files.exists(workDirectory))
                    FileUtils.deleteDirectory(workDirectory.toFile());
                Files.createDirectories(workDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            workDirectory = getNewScratch();

        // Save models if needed
        List<Path> customModelPaths = new ArrayList<>();
        Path customSizeModelPath = null;
        if (modelParameters.getModel() == CellPoseModel.Custom) {
            List<CellPoseModelData> models = dataBatch.getInputData("Pretrained model", CellPoseModelData.class, progressInfo);
            for (int i = 0; i < models.size(); i++) {
                CellPoseModelData modelData = models.get(i);
                Path customModelPath = workDirectory.resolve(i + "_" + modelData.getName());
                try {
                    Files.write(customModelPath, modelData.getData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                customModelPaths.add(customModelPath);
            }

            List<CellPoseSizeModelData> sizeModels = dataBatch.getInputData("Size model", CellPoseSizeModelData.class, progressInfo);
            if (sizeModels.size() > 1) {
                throw new UserFriendlyRuntimeException("Only 1 size model supported!",
                        "Only one size model is supported!",
                        getDisplayName(),
                        "Currently, the node supports only one size model.",
                        "Remove or modify inputs so that there is only one size model.");
            }
            if (!sizeModels.isEmpty()) {
                CellPoseSizeModelData sizeModelData = sizeModels.get(0);
                Path customModelPath = workDirectory.resolve("sz" + sizeModelData.getName() + ".npy");
                try {
                    Files.write(customModelPath, sizeModelData.getData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                customSizeModelPath = customModelPath;
            }
        }

        for (Integer inputRow : dataBatch.getInputRows("Input")) {
            Path rowWorkDirectory = workDirectory.resolve(inputRow + "");
            try {
                Files.createDirectories(rowWorkDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Path inputImagePath = rowWorkDirectory.resolve("input.tif");
            Path outputRoiOutline = rowWorkDirectory.resolve("rois.json");
            Path outputDiameters = rowWorkDirectory.resolve("diameters.txt");
            Path outputLabels = rowWorkDirectory.resolve("labels.tif");
            Path outputFlows = rowWorkDirectory.resolve("flows.tif");
            Path outputProbabilities = rowWorkDirectory.resolve("probabilities.tif");
            Path outputStyles = rowWorkDirectory.resolve("styles.tif");

            // Save raw image
            progressInfo.log("Saving input image to " + inputImagePath);

            Path _inputImagePath = getInputSlot("Input").getData(inputRow, FileData.class, progressInfo).toPath();
            PathUtils.copyOrLink(_inputImagePath, inputImagePath, progressInfo);

            // Generate code
            StringBuilder code = new StringBuilder();
            CellPoseUtils.setupCellposeImports(code);

            code.append("enable_3d_segmentation = ").append(PythonUtils.objectToPython(enable3DSegmentation)).append("\n\n");

            // I we provide a custom model, we need to inject custom code (Why?)
            if (modelParameters.getModel() == CellPoseModel.Custom) {
                CellPoseUtils.setupCustomCellposeModel(code, customModelPaths, customSizeModelPath, getEnhancementParameters(), getModelParameters());
            } else {
                // We can use the combined Cellpose class
                CellPoseUtils.setupCombinedCellposeModel(code, getEnhancementParameters(), getModelParameters());
            }

            code.append("input_file = \"").append(MacroUtils.escapeString(inputImagePath.toString())).append("\"\n");
            code.append("img = io.imread(input_file)\n");
            code.append("data_is_3d = ").append("len(img.shape) > 2").append("\n");

            // Add split code for 3D data in 2D plane mode
            code.append("if data_is_3d and not enable_3d_segmentation:\n" +
                    "    imgs = []\n" +
                    "    for z in range(img.shape[0]):\n" +
                    "        imgs.append(img[z,:,:])\n" +
                    "    img = imgs\n\n");

            CellPoseUtils.setupModelEval(code, getDiameter(), getEnhancementParameters(), getPerformanceParameters(), getThresholdParameters());

            // Re-merge masks
            if (outputParameters.isOutputROI() || outputParameters.isOutputLabels()) {
                code.append("if data_is_3d and not enable_3d_segmentation:\n" +
                        "    masks = np.stack(masks, 0)\n");
            }

            // Generate ROI output
            if (outputParameters.isOutputROI()) {
                CellPoseUtils.setupGenerateOutputROI(outputRoiOutline, code);
            }
            if (outputParameters.isOutputLabels()) {
                CellPoseUtils.setupGenerateOutputLabels(outputLabels, code);
            }
            if (outputParameters.isOutputFlows()) {
                CellPoseUtils.setupGenerateOutputFlows(outputFlows, code);
            }
            if (outputParameters.isOutputProbabilities()) {
                CellPoseUtils.setupGenerateOutputProbabilities(outputProbabilities, code);
            }
            if (outputParameters.isOutputStyles()) {
                CellPoseUtils.setupGenerateOutputStyles(outputStyles, code);
            }

            // Write diameters
            if (diameterAnnotation.isEnabled()) {
                code.append(String.format("with open(\"%s\", \"w\") as f:\n", MacroUtils.escapeString(outputDiameters.toString())));
                code.append("    f.write(str(diams))\n");
            }

            // Run script
            PythonUtils.runPython(code.toString(), overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() :
                    CellPoseSettings.getInstance().getPythonEnvironment(), Collections.emptyList(), progressInfo);


            // Fetch original annotations and write them
            List<JIPipeAnnotation> annotationList = new ArrayList<>(getInputSlot("Input").getAnnotations(inputRow));

            // Read diameters
            if (diameterAnnotation.isEnabled()) {
                try {
                    String value = new String(Files.readAllBytes(outputDiameters), StandardCharsets.UTF_8);
                    diameterAnnotation.addAnnotationIfEnabled(annotationList, value);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Extract outputs
            CellPoseUtils.extractCellposeFileOutputs(dataBatch,
                    progressInfo,
                    outputRoiOutline,
                    outputLabels,
                    outputFlows,
                    outputProbabilities,
                    outputStyles,
                    annotationList,
                    getOutputParameters());
        }
    }

    @JIPipeDocumentation(name = "Override output path", description = "If enabled, overrides the path where the outputs and intermediate results.")
    @JIPipeParameter("override-output-path")
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.DirectoriesOnly)
    public OptionalPathParameter getOverrideOutputPath() {
        return overrideOutputPath;
    }

    @JIPipeParameter("override-output-path")
    public void setOverrideOutputPath(OptionalPathParameter overrideOutputPath) {
        this.overrideOutputPath = overrideOutputPath;
    }

    @JIPipeDocumentation(name = "Annotate with diameter", description = "If enabled, the diameter is attached as annotation. " +
            "Useful if you want to let Cellpose estimate the object diameters.")
    @JIPipeParameter("diameter-annotation")
    public OptionalAnnotationNameParameter getDiameterAnnotation() {
        return diameterAnnotation;
    }

    @JIPipeParameter("diameter-annotation")
    public void setDiameterAnnotation(OptionalAnnotationNameParameter diameterAnnotation) {
        this.diameterAnnotation = diameterAnnotation;
    }

    @JIPipeDocumentation(name = "Diameter", description = "If enabled, Cellpose will use the provided average diameter to find objects. " +
            "Otherwise, Cellpose will estimate the diameter by itself.")
    @JIPipeParameter("diameter")
    public OptionalDoubleParameter getDiameter() {
        return diameter;
    }

    @JIPipeParameter("diameter")
    public void setDiameter(OptionalDoubleParameter diameter) {
        this.diameter = diameter;
    }

    @JIPipeDocumentation(name = "Model", description = "The following settings are related to the model.")
    @JIPipeParameter("model-parameters")
    public ModelParameters getModelParameters() {
        return modelParameters;
    }

    @JIPipeDocumentation(name = "Performance", description = "The following settings are related to the performance of the operation (e.g., tiling).")
    @JIPipeParameter(value = "performance-parameters", collapsed = true)
    public PerformanceParameters getPerformanceParameters() {
        return performanceParameters;
    }

    @JIPipeDocumentation(name = "Tweaks", description = "Additional options like augmentation and averaging over multiple networks")
    @JIPipeParameter("enhancement-parameters")
    public EnhancementParameters getEnhancementParameters() {
        return enhancementParameters;
    }

    @JIPipeDocumentation(name = "Thresholds", description = "Parameters that control which objects are selected.")
    @JIPipeParameter("threshold-parameters")
    public ThresholdParameters getThresholdParameters() {
        return thresholdParameters;
    }

    @JIPipeDocumentation(name = "Outputs", description = "The following settings allow you to select which outputs are generated.")
    @JIPipeParameter(value = "output-parameters", collapsed = true)
    public OutputParameters getOutputParameters() {
        return outputParameters;
    }

    private void updateOutputSlots() {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
        if (outputParameters.isOutputLabels()) {
            if (!getOutputSlotMap().containsKey("Labels")) {
                slotConfiguration.addOutputSlot("Labels", FileData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Labels")) {
                slotConfiguration.removeOutputSlot("Labels", false);
            }
        }
        if (outputParameters.isOutputFlows()) {
            if (!getOutputSlotMap().containsKey("Flows")) {
                slotConfiguration.addOutputSlot("Flows", FileData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Flows")) {
                slotConfiguration.removeOutputSlot("Flows", false);
            }
        }
        if (outputParameters.isOutputProbabilities()) {
            if (!getOutputSlotMap().containsKey("Probabilities")) {
                slotConfiguration.addOutputSlot("Probabilities", FileData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Probabilities")) {
                slotConfiguration.removeOutputSlot("Probabilities", false);
            }
        }
        if (outputParameters.isOutputStyles()) {
            if (!getOutputSlotMap().containsKey("Styles")) {
                slotConfiguration.addOutputSlot("Styles", FileData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Styles")) {
                slotConfiguration.removeOutputSlot("Styles", false);
            }
        }
        if (outputParameters.isOutputROI()) {
            if (!getOutputSlotMap().containsKey("ROI")) {
                slotConfiguration.addOutputSlot("ROI", ROIListData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("ROI")) {
                slotConfiguration.removeOutputSlot("ROI", false);
            }
        }
    }
}

