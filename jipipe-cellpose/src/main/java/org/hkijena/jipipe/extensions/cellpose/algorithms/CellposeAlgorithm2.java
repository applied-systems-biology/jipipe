package org.hkijena.jipipe.extensions.cellpose.algorithms;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.cellpose.CellposeModel;
import org.hkijena.jipipe.extensions.cellpose.CellposeSettings;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellposeSizeModelData;
import org.hkijena.jipipe.extensions.cellpose.parameters.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Improved version of {@link CellposeAlgorithm} that utilizes the Cellpose CLI
 */
@JIPipeDocumentation(name = "Cellpose", description = "Runs Cellpose on the input image. This node supports both segmentation in 3D and executing " +
        "Cellpose for each 2D image plane. " +
        "This node can generate a multitude of outputs, although only ROI is activated by default. " +
        "Go to the 'Outputs' parameter section to enable the other outputs." +
        "<ul>" +
        "<li><b>Labels:</b> A grayscale image where each connected component is assigned a unique value.</li>" +
        "<li><b>Flows XY:</b> An RGB image that indicates the x and y flow of each pixel</li>" +
        "<li><b>Flows Z:</b> Flows in Z direction (black for non-3D images)</li>" +
        "<li><b>Flows d:</b> Multi-channel image that contains the flows [dZ, dY, dX, cell probability] (3D images) / [dY, dX, cell probability] (2D images)</li>" +
        "<li><b>Probabilities:</b> An image indicating the cell probabilities for each pixel</li>" +
        "<li><b>ROI:</b> ROI of the segmented areas.</li>" +
        "</ul>" +
        "Please note that you need to setup a valid Python environment with Cellpose installed. You can find the setting in Project &gt; Application settings &gt; Extensions &gt; Cellpose.")
@JIPipeInputSlot(value = ImagePlus3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels")
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Flows XY")
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Flows Z")
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Flows d")
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Probabilities")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
public class CellposeAlgorithm2 extends JIPipeSingleIterationAlgorithm {

    public static final JIPipeDataSlotInfo INPUT_PRETRAINED_MODEL = new JIPipeDataSlotInfo(CellposeModelData.class, JIPipeSlotType.Input, "Pretrained Model", "A custom pretrained model");
    public static final JIPipeDataSlotInfo INPUT_SIZE_MODEL = new JIPipeDataSlotInfo(CellposeSizeModelData.class, JIPipeSlotType.Input, "Size Model", "A custom size model", null, true);
    public static final JIPipeDataSlotInfo OUTPUT_LABELS = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Labels", "A grayscale image where each connected component is assigned a unique value");
    public static final JIPipeDataSlotInfo OUTPUT_FLOWS_XY = new JIPipeDataSlotInfo(ImagePlusColorRGBData.class, JIPipeSlotType.Output, "Flows XY", "An RGB image that indicates the x and y flow of each pixel");
    public static final JIPipeDataSlotInfo OUTPUT_FLOWS_Z = new JIPipeDataSlotInfo(ImagePlusColorRGBData.class, JIPipeSlotType.Output, "Flows Z", "Flows in Z direction (black for non-3D images)");
    public static final JIPipeDataSlotInfo OUTPUT_FLOWS_D = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Flows d", "Multi-channel image that contains the flows [dZ, dY, dX, cell probability] (3D images) / [dY, dX, cell probability] (2D images)");
    public static final JIPipeDataSlotInfo OUTPUT_PROBABILITIES = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Probabilities", "An image indicating the cell probabilities for each pixel");
    public static final JIPipeDataSlotInfo OUTPUT_ROI = new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Output, "ROI", "ROI of the segmented areas");

    private final SegmentationModelSettings segmentationModelSettings;
    private final GeneralGPUSettings gpuSettings;
    private final SegmentationPerformanceSettings segmentationPerformanceSettings;
    private final SegmentationEnhancementSettings segmentationEnhancementSettings;
    private final SegmentationThresholdSettings segmentationThresholdSettings;
    private final SegmentationOutputSettings segmentationOutputSettings;
    private OptionalDoubleParameter diameter = new OptionalDoubleParameter(30.0, true);
    private boolean enable3DSegmentation = true;
    private OptionalAnnotationNameParameter diameterAnnotation = new OptionalAnnotationNameParameter("Diameter", true);
    private boolean cleanUpAfterwards = true;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();

    private OptionalIntegerParameter segmentedChannel = new OptionalIntegerParameter(false, 0);

    private OptionalIntegerParameter nuclearChannel = new OptionalIntegerParameter(false, 0);

    public CellposeAlgorithm2(JIPipeNodeInfo info) {
        super(info);
        this.segmentationModelSettings = new SegmentationModelSettings();
        this.segmentationEnhancementSettings = new SegmentationEnhancementSettings();
        this.segmentationPerformanceSettings = new SegmentationPerformanceSettings();
        this.gpuSettings = new GeneralGPUSettings();
        this.segmentationThresholdSettings = new SegmentationThresholdSettings();
        this.segmentationOutputSettings = new SegmentationOutputSettings();

        updateOutputSlots();
        updateInputSlots();

        registerSubParameter(segmentationModelSettings);
        registerSubParameter(segmentationPerformanceSettings);
        registerSubParameter(segmentationEnhancementSettings);
        registerSubParameter(segmentationThresholdSettings);
        registerSubParameter(segmentationOutputSettings);
        registerSubParameter(gpuSettings);
    }

    public CellposeAlgorithm2(CellposeAlgorithm2 other) {
        super(other);
        this.gpuSettings = new GeneralGPUSettings(other.gpuSettings);
        this.segmentationPerformanceSettings = new SegmentationPerformanceSettings(other.segmentationPerformanceSettings);
        this.segmentationEnhancementSettings = new SegmentationEnhancementSettings(other.segmentationEnhancementSettings);
        this.segmentationThresholdSettings = new SegmentationThresholdSettings(other.segmentationThresholdSettings);
        this.segmentationModelSettings = new SegmentationModelSettings(other.segmentationModelSettings);
        this.segmentationOutputSettings = new SegmentationOutputSettings(other.segmentationOutputSettings);

        this.diameter = new OptionalDoubleParameter(other.diameter);
        this.diameterAnnotation = new OptionalAnnotationNameParameter(other.diameterAnnotation);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.segmentedChannel = new OptionalIntegerParameter(other.segmentedChannel);
        this.nuclearChannel = new OptionalIntegerParameter(other.nuclearChannel);
        this.enable3DSegmentation = other.enable3DSegmentation;
        this.cleanUpAfterwards = other.cleanUpAfterwards;

        updateOutputSlots();
        updateInputSlots();

        registerSubParameter(segmentationModelSettings);
        registerSubParameter(segmentationPerformanceSettings);
        registerSubParameter(segmentationEnhancementSettings);
        registerSubParameter(segmentationThresholdSettings);
        registerSubParameter(segmentationOutputSettings);
        registerSubParameter(gpuSettings);
    }

    @JIPipeDocumentation(name = "Segmented channel", description = "Channel to segment; 0: GRAY, 1: RED, 2: GREEN, 3: BLUE. Default: 0")
    @JIPipeParameter("segmented-channel")
    public OptionalIntegerParameter getSegmentedChannel() {
        return segmentedChannel;
    }

    @JIPipeParameter("segmented-channel")
    public void setSegmentedChannel(OptionalIntegerParameter segmentedChannel) {
        this.segmentedChannel = segmentedChannel;
    }

    @JIPipeDocumentation(name = "Nuclear channel", description = "Nuclear channel (only used by certain models); 0: NONE, 1: RED, 2: GREEN, 3: BLUE. Default: 0")
    @JIPipeParameter("nuclear-channel")
    public OptionalIntegerParameter getNuclearChannel() {
        return nuclearChannel;
    }

    @JIPipeParameter("nuclear-channel")
    public void setNuclearChannel(OptionalIntegerParameter nuclearChannel) {
        this.nuclearChannel = nuclearChannel;
    }

    @JIPipeDocumentation(name = "Enable 3D segmentation", description = "If enabled, Cellpose will segment in 3D. Otherwise, " +
            "any 3D image will be processed per-slice. Please note that 3D segmentation requires large amounts of memory.")
    @JIPipeParameter(value = "enable-3d-segmentation", important = true)
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
        if (event.getSource() == segmentationOutputSettings) {
            updateOutputSlots();
        } else if (event.getSource() == segmentationModelSettings && event.getKey().equals("model")) {
            updateInputSlots();
        }
    }

    private void updateInputSlots() {
        toggleSlot(INPUT_PRETRAINED_MODEL, segmentationModelSettings.getModel() == CellposeModel.Custom);
        toggleSlot(INPUT_SIZE_MODEL, segmentationModelSettings.getModel() == CellposeModel.Custom);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        super.reportValidity(report);
        if (!isPassThrough()) {
            if (overrideEnvironment.isEnabled()) {
                report.resolve("Override Python environment").report(overrideEnvironment.getContent());
            } else {
                CellposeSettings.checkPythonSettings(report.resolve("Python"));
            }
        }
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path workDirectory = getNewScratch();
        progressInfo.log("Work directory is " + workDirectory);

        // Save models if needed
        List<Path> customModelPaths = new ArrayList<>();
        Path customSizeModelPath = null;
        if (segmentationModelSettings.getModel() == CellposeModel.Custom) {
            List<CellposeModelData> models = dataBatch.getInputData("Pretrained model", CellposeModelData.class, progressInfo);
            for (int i = 0; i < models.size(); i++) {
                CellposeModelData modelData = models.get(i);
                Path customModelPath = workDirectory.resolve(i + "_" + modelData.getName());
                try {
                    Files.write(customModelPath, modelData.getData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                customModelPaths.add(customModelPath);
            }

            List<CellposeSizeModelData> sizeModels = dataBatch.getInputData("Size model", CellposeSizeModelData.class, progressInfo);
            if (sizeModels.size() > 1) {
                throw new UserFriendlyRuntimeException("Only 1 size model supported!",
                        "Only one size model is supported!",
                        getDisplayName(),
                        "Currently, the node supports only one size model.",
                        "Remove or modify inputs so that there is only one size model.");
            }
            if (!sizeModels.isEmpty()) {
                CellposeSizeModelData sizeModelData = sizeModels.get(0);
                Path customModelPath = workDirectory.resolve("sz" + sizeModelData.getName() + ".npy");
                try {
                    Files.write(customModelPath, sizeModelData.getData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                customSizeModelPath = customModelPath;
            }
        }

        // We need a 2D and a 3D branch due to incompatibilities on the side of Cellpose
        final Path io2DPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "io-2d");
        final Path io3DPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "io-3d");

        List<CellposeImageInfo> runWith2D = new ArrayList<>();
        List<CellposeImageInfo> runWith3D = new ArrayList<>();

        // Save input images
        saveInputImages(dataBatch, progressInfo.resolve("Export input images"), io2DPath, io3DPath, runWith2D, runWith3D);

        // Run Cellpose


        // Cleanup
        if (cleanUpAfterwards) {
            PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
        }
    }

    private void runCellpose(JIPipeProgressInfo progressInfo, Path ioPath, boolean with3D, Path customModelPath, Path customSizeModelPath) {
        List<String> arguments = new ArrayList<>();
        arguments.add("-m");
        arguments.add("cellpose");

        arguments.add("--verbose");
        if (segmentationModelSettings.isEnableGPU())
            arguments.add("--use_gpu");

        arguments.add("--dir");
        arguments.add(ioPath.toString());
    }

    private void saveInputImages(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo, Path io2DPath, Path io3DPath, List<CellposeImageInfo> runWith2D, List<CellposeImageInfo> runWith3D) {
        for (int row : dataBatch.getInputRows("Input")) {
            JIPipeProgressInfo rowProgress = progressInfo.resolve("Data row " + row);

            ImagePlus img = getInputSlot("Input").getData(row, ImagePlus3DData.class, rowProgress).getImage();
            if(img.getNFrames() > 1) {
                throw new UserFriendlyRuntimeException("Cellpose does not support time series!",
                        "Cellpose does not support time series!",
                        getDisplayName(),
                        "Please ensure that the image dimensions are correctly assigned.",
                        "Remove the frames or reorder the dimensions before applying Cellpose");
            }
            if(img.getNSlices() == 1) {
                // Output the image as-is
                String baseName = row + "_";
                Path outputPath = io2DPath.resolve(baseName + ".tif");
                IJ.saveAsTiff(img, outputPath.toString());

                // Create info
                CellposeImageInfo info = new CellposeImageInfo(row);
                info.sliceBaseNames.put(new ImageSliceIndex(-1,-1,-1), baseName);
                runWith2D.add(info);
            }
            else {
                if(enable3DSegmentation) {
                    // Cannot have channels AND RGB
                    if(img.getNChannels() > 1 && img.getType() == ImagePlus.COLOR_RGB) {
                        throw new UserFriendlyRuntimeException("Cellpose does not support 3D multichannel images with RGB!",
                                "Cellpose does not support 3D multichannel images with RGB!",
                                getDisplayName(),
                                "Python will convert the RGB channels into greyscale slices, thus conflicting with the channel slices defined in the input image",
                                "Convert the image from RGB to greyscale or remove the additional channel slices.");
                    }

                    // Output the image as-is
                    String baseName = row + "_";
                    Path outputPath = io3DPath.resolve(baseName + ".tif");
                    IJ.saveAsTiff(img, outputPath.toString());

                    // Create info
                    CellposeImageInfo info = new CellposeImageInfo(row);
                    info.sliceBaseNames.put(new ImageSliceIndex(-1,-1,-1), baseName);
                    runWith3D.add(info);
                }
                else {
                    rowProgress.log("3D mode not active, but 3D image detected -> Image will be split into 2D slices.");

                    CellposeImageInfo info = new CellposeImageInfo(row);

                    // Split the 3D image into slices
                    ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                        ImagePlus sliceImage = new ImagePlus(index.toString(), ip);
                        String baseName = row + "_z" + index.getZ() + "_c" + index.getC() + "_t" + index.getT() + "_";
                        Path outputPath = io2DPath.resolve(baseName + ".tif");
                        IJ.saveAsTiff(sliceImage, outputPath.toString());

                        // Create info
                        info.sliceBaseNames.put(index, baseName);
                    }, rowProgress);

                    runWith2D.add(info);
                }
            }
        }
    }

    @JIPipeDocumentation(name = "Clean up data after processing", description = "If enabled, data is deleted from temporary directories after " +
            "the processing was finished. Disable this to make it possible to debug your scripts. The directories are accessible via the logs (Tools &gt; Logs).")
    @JIPipeParameter("cleanup-afterwards")
    public boolean isCleanUpAfterwards() {
        return cleanUpAfterwards;
    }

    @JIPipeParameter("cleanup-afterwards")
    public void setCleanUpAfterwards(boolean cleanUpAfterwards) {
        this.cleanUpAfterwards = cleanUpAfterwards;
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

    @JIPipeDocumentation(name = "Average object diameter", description = "If enabled, Cellpose will use the provided average diameter to find objects. " +
            "Otherwise, Cellpose will estimate the diameter by itself.")
    @JIPipeParameter(value = "diameter", important = true)
    public OptionalDoubleParameter getDiameter() {
        return diameter;
    }

    @JIPipeParameter("diameter")
    public void setDiameter(OptionalDoubleParameter diameter) {
        this.diameter = diameter;
    }

    @JIPipeDocumentation(name = "Cellpose: Model", description = "The following settings are related to the model.")
    @JIPipeParameter(value = "model-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public SegmentationModelSettings getModelParameters() {
        return segmentationModelSettings;
    }

    @JIPipeDocumentation(name = "Cellpose: Performance", description = "The following settings are related to the performance of the operation (e.g., tiling).")
    @JIPipeParameter(value = "performance-parameters", collapsed = true, iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public SegmentationPerformanceSettings getPerformanceParameters() {
        return segmentationPerformanceSettings;
    }

    @JIPipeDocumentation(name = "Cellpose: Tweaks", description = "Additional options like augmentation and averaging over multiple networks")
    @JIPipeParameter(value = "enhancement-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png", collapsed = true)
    public SegmentationEnhancementSettings getEnhancementParameters() {
        return segmentationEnhancementSettings;
    }

    @JIPipeDocumentation(name = "Cellpose: Thresholds", description = "Parameters that control which objects are selected.")
    @JIPipeParameter(value = "threshold-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png", collapsed = true)
    public SegmentationThresholdSettings getThresholdParameters() {
        return segmentationThresholdSettings;
    }

    @JIPipeDocumentation(name = "Cellpose: Outputs", description = "The following settings allow you to select which outputs are generated.")
    @JIPipeParameter(value = "output-parameters", collapsed = true, iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public SegmentationOutputSettings getSegmentationOutputSettings() {
        return segmentationOutputSettings;
    }

    @JIPipeDocumentation(name = "Cellpose: GPU", description = "Controls how the graphics card is utilized.")
    @JIPipeParameter(value = "output-parameters", collapsed = true, iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public GeneralGPUSettings getGpuSettings() {
        return gpuSettings;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getSource() == segmentationModelSettings && "mean-diameter".equals(access.getKey())) {
            // This is never needed
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    private void updateOutputSlots() {
        toggleSlot(OUTPUT_FLOWS_D, segmentationOutputSettings.isOutputFlowsD());
        toggleSlot(OUTPUT_PROBABILITIES, segmentationOutputSettings.isOutputProbabilities());
        toggleSlot(OUTPUT_FLOWS_XY, segmentationOutputSettings.isOutputFlowsXY());
        toggleSlot(OUTPUT_LABELS, segmentationOutputSettings.isOutputLabels());
        toggleSlot(OUTPUT_FLOWS_Z, segmentationOutputSettings.isOutputFlowsZ());
        toggleSlot(OUTPUT_ROI, segmentationOutputSettings.isOutputROI());
    }

    private static class CellposeImageInfo {
        private final int sourceRow;
        private final Map<ImageSliceIndex, String> sliceBaseNames;

        private CellposeImageInfo(int sourceRow) {
            this.sourceRow = sourceRow;
            this.sliceBaseNames = new HashMap<>();
        }

        public int getSourceRow() {
            return sourceRow;
        }

        public Map<ImageSliceIndex, String> getSliceBaseNames() {
            return sliceBaseNames;
        }
    }
}

