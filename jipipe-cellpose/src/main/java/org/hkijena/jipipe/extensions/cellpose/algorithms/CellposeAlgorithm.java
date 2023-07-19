package org.hkijena.jipipe.extensions.cellpose.algorithms;

import com.fasterxml.jackson.databind.JsonNode;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.cellpose.CellposeExtension;
import org.hkijena.jipipe.extensions.cellpose.CellposeModel;
import org.hkijena.jipipe.extensions.cellpose.CellposeSettings;
import org.hkijena.jipipe.extensions.cellpose.CellposeUtils;
import org.hkijena.jipipe.extensions.cellpose.algorithms.deprecated.CellposeAlgorithm_Old;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.extensions.cellpose.parameters.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Improved version of {@link CellposeAlgorithm_Old} that utilizes the Cellpose CLI
 */
@JIPipeDocumentation(name = "Cellpose (2.x)", description = "Runs Cellpose on the input image. This node supports both segmentation in 3D and executing " +
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
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Flows XY")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Flows Z")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Flows d")
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Probabilities")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
public class CellposeAlgorithm extends JIPipeSingleIterationAlgorithm {

    public static final JIPipeDataSlotInfo INPUT_PRETRAINED_MODEL = new JIPipeDataSlotInfo(CellposeModelData.class, JIPipeSlotType.Input, "Pretrained Model", "A custom pretrained model");
    //    public static final JIPipeDataSlotInfo INPUT_SIZE_MODEL = new JIPipeDataSlotInfo(CellposeSizeModelData.class, JIPipeSlotType.Input, "Size Model", "A custom size model", null, true);
    public static final JIPipeDataSlotInfo OUTPUT_LABELS = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Labels", "A grayscale image where each connected component is assigned a unique value");
    public static final JIPipeDataSlotInfo OUTPUT_FLOWS_XY = new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Output, "Flows XY", "An RGB image that indicates the x and y flow of each pixel");
    public static final JIPipeDataSlotInfo OUTPUT_FLOWS_Z = new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Output, "Flows Z", "Flows in Z direction (black for non-3D images)");
    public static final JIPipeDataSlotInfo OUTPUT_FLOWS_D = new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Output, "Flows d", "Multi-channel image that contains the flows [dZ, dY, dX, cell probability] (3D images) / [dY, dX, cell probability] (2D images)");
    public static final JIPipeDataSlotInfo OUTPUT_PROBABILITIES = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Probabilities", "An image indicating the cell probabilities for each pixel");
    public static final JIPipeDataSlotInfo OUTPUT_ROI = new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Output, "ROI", "ROI of the segmented areas");

    private final CellposeGPUSettings gpuSettings;
    private final CellposeSegmentationTweaksSettings segmentationTweaksSettings;
    private final CellposeSegmentationThresholdSettings segmentationThresholdSettings;
    private final CellposeSegmentationOutputSettings segmentationOutputSettings;

    private final CellposeChannelSettings channelSettings;

    private CellposeModel model = CellposeModel.Cytoplasm;
    private OptionalDoubleParameter diameter = new OptionalDoubleParameter(30.0, true);
    private boolean enable3DSegmentation = true;
    private OptionalAnnotationNameParameter diameterAnnotation = new OptionalAnnotationNameParameter("Diameter", true);
    private boolean cleanUpAfterwards = true;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();

    public CellposeAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.segmentationTweaksSettings = new CellposeSegmentationTweaksSettings();
        this.gpuSettings = new CellposeGPUSettings();
        this.segmentationThresholdSettings = new CellposeSegmentationThresholdSettings();
        this.segmentationOutputSettings = new CellposeSegmentationOutputSettings();
        this.channelSettings = new CellposeChannelSettings();

        updateOutputSlots();
        updateInputSlots();

        registerSubParameter(segmentationTweaksSettings);
        registerSubParameter(segmentationThresholdSettings);
        registerSubParameter(segmentationOutputSettings);
        registerSubParameter(gpuSettings);
        registerSubParameter(channelSettings);
    }

    public CellposeAlgorithm(CellposeAlgorithm other) {
        super(other);
        this.gpuSettings = new CellposeGPUSettings(other.gpuSettings);
        this.segmentationTweaksSettings = new CellposeSegmentationTweaksSettings(other.segmentationTweaksSettings);
        this.segmentationThresholdSettings = new CellposeSegmentationThresholdSettings(other.segmentationThresholdSettings);
        this.segmentationOutputSettings = new CellposeSegmentationOutputSettings(other.segmentationOutputSettings);
        this.channelSettings = new CellposeChannelSettings(other.channelSettings);

        this.model = other.model;
        this.diameter = new OptionalDoubleParameter(other.diameter);
        this.diameterAnnotation = new OptionalAnnotationNameParameter(other.diameterAnnotation);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.enable3DSegmentation = other.enable3DSegmentation;
        this.cleanUpAfterwards = other.cleanUpAfterwards;

        updateOutputSlots();
        updateInputSlots();

        registerSubParameter(segmentationTweaksSettings);
        registerSubParameter(segmentationThresholdSettings);
        registerSubParameter(segmentationOutputSettings);
        registerSubParameter(gpuSettings);
        registerSubParameter(channelSettings);
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
        } else if (event.getKey().equals("model")) {
            updateInputSlots();
        }
    }

    private void updateInputSlots() {
        toggleSlot(INPUT_PRETRAINED_MODEL, getModel() == CellposeModel.Custom);
//        toggleSlot(INPUT_SIZE_MODEL, segmentationModelSettings.getModel() == CellposeModel.Custom);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        if (!isPassThrough()) {
            if (overrideEnvironment.isEnabled()) {
                report.report(context, overrideEnvironment.getContent());
            } else {
                CellposeSettings.checkPythonSettings(context, report);
            }
        }
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path workDirectory = getNewScratch();
        progressInfo.log("Work directory is " + workDirectory);

        // Save models if needed
        List<Path> customModelPaths = new ArrayList<>();
//        Path customSizeModelPath = null;
        if (getModel() == CellposeModel.Custom) {
            List<CellposeModelData> models = dataBatch.getInputData(INPUT_PRETRAINED_MODEL.getName(), CellposeModelData.class, progressInfo);
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

//            List<CellposeSizeModelData> sizeModels = dataBatch.getInputData("Size model", CellposeSizeModelData.class, progressInfo);
//            if (sizeModels.size() > 1) {
//                throw new UserFriendlyRuntimeException("Only 1 size model supported!",
//                        "Only one size model is supported!",
//                        getDisplayName(),
//                        "Currently, the node supports only one size model.",
//                        "Remove or modify inputs so that there is only one size model.");
//            }
//            if (!sizeModels.isEmpty()) {
//                CellposeSizeModelData sizeModelData = sizeModels.get(0);
//                Path customModelPath = workDirectory.resolve("sz" + sizeModelData.getName() + ".npy");
//                try {
//                    Files.write(customModelPath, sizeModelData.getData());
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//                customSizeModelPath = customModelPath;
//            }
        }

        // We need a 2D and a 3D branch due to incompatibilities on the side of Cellpose
        final Path io2DPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "io-2d");
        final Path io3DPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "io-3d");

        List<CellposeImageInfo> runWith2D = new ArrayList<>();
        List<CellposeImageInfo> runWith3D = new ArrayList<>();

        // Save input images
        saveInputImages(dataBatch, progressInfo.resolve("Export input images"), io2DPath, io3DPath, runWith2D, runWith3D);

        // Run Cellpose
        if (!runWith2D.isEmpty()) {
            if (!customModelPaths.isEmpty()) {
                for (Path customModelPath : customModelPaths) {
                    runCellpose(progressInfo.resolve("Cellpose"), io2DPath, false, customModelPath);
                }
            } else {
                runCellpose(progressInfo.resolve("Cellpose"), io2DPath, false, null);
            }
        }
        if (!runWith3D.isEmpty()) {
            if (!customModelPaths.isEmpty()) {
                for (Path customModelPath : customModelPaths) {
                    runCellpose(progressInfo.resolve("Cellpose"), io3DPath, true, customModelPath);
                }
            } else {
                runCellpose(progressInfo.resolve("Cellpose"), io3DPath, true, null);
            }
        }

        // Deploy and run extraction script
        progressInfo.log("Deploying script to extract Cellpose *.npy results ...");
        Path npyExtractorScript = workDirectory.resolve("extract-cellpose-npy.py");
        CellposeExtension.RESOURCES.exportResourceToFile("extract-cellpose-npy.py", npyExtractorScript);
        if (!runWith2D.isEmpty()) {
            PythonUtils.runPython(new String[]{npyExtractorScript.toString(), io2DPath.toString(), io2DPath.toString()}, overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() :
                    CellposeSettings.getInstance().getPythonEnvironment(), Collections.emptyList(), Collections.emptyMap(), progressInfo.resolve("Extract Cellpose results (2D)"));
        }
        if (!runWith3D.isEmpty()) {
            PythonUtils.runPython(new String[]{npyExtractorScript.toString(), io3DPath.toString(), io3DPath.toString()}, overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() :
                    CellposeSettings.getInstance().getPythonEnvironment(), Collections.emptyList(), Collections.emptyMap(), progressInfo.resolve("Extract Cellpose results (3D)"));
        }

        // Fetch the data from the directory
        for (CellposeImageInfo imageInfo : runWith2D) {
            extractDataFromInfo(dataBatch, imageInfo, io2DPath, progressInfo.resolve("Importing results row " + imageInfo.sourceRow));
        }
        for (CellposeImageInfo imageInfo : runWith3D) {
            extractDataFromInfo(dataBatch, imageInfo, io3DPath, progressInfo.resolve("Importing results row " + imageInfo.sourceRow));
        }

        // Cleanup
        if (cleanUpAfterwards) {
            PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
        }
    }

    private void extractDataFromInfo(JIPipeMergingDataBatch dataBatch, CellposeImageInfo imageInfo, Path ioPath, JIPipeProgressInfo progressInfo) {
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>(getInputSlot("Input").getTextAnnotations(imageInfo.sourceRow));
        if (diameterAnnotation.isEnabled()) {
            progressInfo.log("Reading info ...");
            List<JIPipeTextAnnotation> diameterAnnotations = new ArrayList<>();
            for (Map.Entry<ImageSliceIndex, String> entry : imageInfo.sliceBaseNames.entrySet()) {
                JsonNode node = JsonUtils.readFromFile(ioPath.resolve(entry.getValue() + "_seg_info.json"), JsonNode.class);
                diameterAnnotation.addAnnotationIfEnabled(diameterAnnotations, node.get("est_diam").asText());
            }
            annotationList.addAll(JIPipeTextAnnotationMergeMode.Merge.merge(diameterAnnotations));
        }
        if (segmentationOutputSettings.isOutputROI()) {
            progressInfo.log("Reading ROI ...");
            ROIListData rois = new ROIListData();
            for (Map.Entry<ImageSliceIndex, String> entry : imageInfo.sliceBaseNames.entrySet()) {
                ROIListData sliceRoi = CellposeUtils.cellposeROIJsonToImageJ(ioPath.resolve(entry.getValue() + "_seg_roi.json"));
                if (imageInfo.sliceBaseNames.size() > 1) {
                    for (Roi roi : sliceRoi) {
                        roi.setPosition(0, entry.getKey().getZ() + 1, 0);
                    }
                }
                rois.addAll(sliceRoi);
            }
            dataBatch.addOutputData("ROI", rois, annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (segmentationOutputSettings.isOutputFlowsD()) {
            progressInfo.log("Reading Flows d ...");
            ImagePlus img = extractImageFromInfo(imageInfo, ioPath, "_seg_flows_dz_dy_dx.tif", true, progressInfo);
            dataBatch.addOutputData(OUTPUT_FLOWS_D.getName(), new ImagePlusData(img), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (segmentationOutputSettings.isOutputFlowsXY()) {
            progressInfo.log("Reading Flows XY ...");
            ImagePlus img = extractImageFromInfo(imageInfo, ioPath, "_seg_flows_rgb.tif", false, progressInfo);
            dataBatch.addOutputData(OUTPUT_FLOWS_XY.getName(), new ImagePlusData(img), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (segmentationOutputSettings.isOutputFlowsZ()) {
            progressInfo.log("Reading Flows Z ...");
            ImagePlus img = extractImageFromInfo(imageInfo, ioPath, "_seg_flows_z.tif", false, progressInfo);
            dataBatch.addOutputData(OUTPUT_FLOWS_Z.getName(), new ImagePlusData(img), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (segmentationOutputSettings.isOutputLabels()) {
            progressInfo.log("Reading labels ...");
            ImagePlus img = extractImageFromInfo(imageInfo, ioPath, "_seg_labels.tif", false, progressInfo);
            dataBatch.addOutputData(OUTPUT_LABELS.getName(), new ImagePlusGreyscaleData(img), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (segmentationOutputSettings.isOutputProbabilities()) {
            progressInfo.log("Reading probabilities ...");
            ImagePlus img = extractImageFromInfo(imageInfo, ioPath, "_seg_probabilities.tif", false, progressInfo);
            dataBatch.addOutputData(OUTPUT_PROBABILITIES.getName(), new ImagePlusGreyscale32FData(img), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
    }

    private ImagePlus extractImageFromInfo(CellposeImageInfo imageInfo, Path ioPath, String basePathSuffix, boolean useBioFormats, JIPipeProgressInfo progressInfo) {
        Map<ImageSliceIndex, ImagePlus> sliceMap = new HashMap<>();
        for (Map.Entry<ImageSliceIndex, String> entry : imageInfo.sliceBaseNames.entrySet()) {
            Path imageFile = ioPath.resolve(entry.getValue() + basePathSuffix);
            progressInfo.log("Reading: " + imageFile);
            ImagePlus slice;
            if (useBioFormats) {
                OMEImageData omeImageData = OMEImageData.simpleOMEImport(imageFile);
                slice = omeImageData.getImage();
            } else {
                slice = IJ.openImage(imageFile.toString());
            }
            if (slice == null) {
                throw new NullPointerException("Unable to read image from " + imageFile + "! Bio-Formats: " + useBioFormats);
            }
            sliceMap.put(entry.getKey(), slice);
        }
        if (sliceMap.size() == 1) {
            return sliceMap.values().iterator().next();
        } else {
            int width = 0;
            int height = 0;
            int sizeZ = 0;
            int sizeC = 0;
            int sizeT = 0;
            for (Map.Entry<ImageSliceIndex, ImagePlus> entry : sliceMap.entrySet()) {
                width = Math.max(entry.getValue().getWidth(), width);
                height = Math.max(entry.getValue().getHeight(), height);
                sizeZ = Math.max(entry.getKey().getZ() + 1, sizeZ);
                sizeC = Math.max(Math.max(entry.getKey().getC() + 1, sizeC), entry.getValue().getStackSize()); // Map slices to channels
                sizeT = Math.max(entry.getKey().getT() + 1, sizeT);
            }
            ImageStack stack = new ImageStack(width, height, sizeZ * sizeT * sizeC);
            for (Map.Entry<ImageSliceIndex, ImagePlus> entry : sliceMap.entrySet()) {
                // Map all slices to channels
                for (int i = 0; i < entry.getValue().getStackSize(); i++) {
                    ImageProcessor processor = entry.getValue().getStack().getProcessor(i + 1);
                    int targetIndex = (new ImageSliceIndex(i, entry.getKey().getZ(), entry.getKey().getT())).zeroSliceIndexToOneStackIndex(sizeC, sizeZ, sizeT);
                    stack.setProcessor(processor, targetIndex);
                }
            }
            ImagePlus img = new ImagePlus(basePathSuffix, stack);
            img.setDimensions(sizeC, sizeZ, sizeT);
            return img;
        }
    }

    private void runCellpose(JIPipeProgressInfo progressInfo, Path ioPath, boolean with3D, Path customModelPath) {
        List<String> arguments = new ArrayList<>();
        arguments.add("-m");
        arguments.add("cellpose");

        arguments.add("--verbose");

        // Inputs
        if (diameter.isEnabled()) {
            arguments.add("--diameter");
            arguments.add(diameter.getContent() + "");
        } else {
            arguments.add("--diameter");
            arguments.add("0");
        }
        if (with3D) {
            arguments.add("--do_3D");
        }

        // GPU
        if (gpuSettings.isEnableGPU())
            arguments.add("--use_gpu");
        if (gpuSettings.getGpuDevice().isEnabled()) {
            arguments.add("--gpu_device");
            arguments.add(gpuSettings.getGpuDevice().getContent() + "");
        }

        // Channels
        if (channelSettings.getSegmentedChannel().isEnabled()) {
            arguments.add("--chan");
            arguments.add(channelSettings.getSegmentedChannel().getContent() + "");
        } else {
            arguments.add("--chan");
            arguments.add("0");
        }
        if (channelSettings.getNuclearChannel().isEnabled()) {
            arguments.add("--chan2");
            arguments.add(channelSettings.getNuclearChannel().getContent() + "");
        }
        if (channelSettings.isAllChannels()) {
            arguments.add("--all_channels");
        }
        if (channelSettings.isInvert()) {
            arguments.add("--invert");
        }

        // Model
        if (getModel() == CellposeModel.Custom) {
            arguments.add("--pretrained_model");
            arguments.add(customModelPath.toString());
        } else {
            arguments.add("--pretrained_model");
            arguments.add(getModel().getId());
        }

        // Tweaks
        if (!segmentationTweaksSettings.isNormalize()) {
            arguments.add("--no_norm");
        }
        if (segmentationTweaksSettings.isNetAverage()) {
            arguments.add("--net_avg");
        }
        if (!segmentationTweaksSettings.isInterpolate()) {
            arguments.add("--no_interp");
        }
        if (segmentationTweaksSettings.getAnisotropy().isEnabled()) {
            arguments.add("--anisotropy");
            arguments.add(segmentationTweaksSettings.getAnisotropy().getContent() + "");
        }
        if (segmentationTweaksSettings.isDisableResample()) {
            arguments.add("--no_resample");
        }

        // Segmentation
        if (segmentationThresholdSettings.isExcludeOnEdges()) {
            arguments.add("--exclude_on_edges");
        }
        if (segmentationThresholdSettings.getStitchThreshold().isEnabled()) {
            arguments.add("--stitch_threshold");
            arguments.add("" + segmentationThresholdSettings.getStitchThreshold().getContent());
        }
        {
            arguments.add("--flow_threshold");
            arguments.add(segmentationThresholdSettings.getFlowThreshold() + "");
        }
        {
            arguments.add("--cellprob_threshold");
            arguments.add(segmentationThresholdSettings.getCellProbabilityThreshold() + "");
        }

        // Input/output
        arguments.add("--dir");
        arguments.add(ioPath.toString());

        // Run the module
        PythonUtils.runPython(arguments.toArray(new String[0]), overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() :
                CellposeSettings.getInstance().getPythonEnvironment(), Collections.emptyList(), Collections.emptyMap(), progressInfo);
    }

    private void saveInputImages(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo, Path io2DPath, Path io3DPath, List<CellposeImageInfo> runWith2D, List<CellposeImageInfo> runWith3D) {
        for (int row : dataBatch.getInputRows("Input")) {
            JIPipeProgressInfo rowProgress = progressInfo.resolve("Data row " + row);

            ImagePlus img = getInputSlot("Input").getData(row, ImagePlusData.class, rowProgress).getImage();
            if (img.getNFrames() > 1) {
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new GraphNodeValidationReportContext(this),
                        "Cellpose does not support time series!",
                        "Please ensure that the image dimensions are correctly assigned.",
                        "Remove the frames or reorder the dimensions before applying Cellpose"));
            }
            if (img.getNSlices() == 1) {
                // Output the image as-is
                String baseName = row + "_";
                Path outputPath = io2DPath.resolve(baseName + ".tif");
                IJ.saveAsTiff(img, outputPath.toString());

                // Create info
                CellposeImageInfo info = new CellposeImageInfo(row);
                info.sliceBaseNames.put(new ImageSliceIndex(-1, -1, -1), baseName);
                runWith2D.add(info);
            } else {
                if (enable3DSegmentation) {
                    // Cannot have channels AND RGB
                    if (img.getNChannels() > 1 && img.getType() == ImagePlus.COLOR_RGB) {
                        throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                                new GraphNodeValidationReportContext(this),
                                "Cellpose does not support 3D multichannel images with RGB!",
                                "Python will convert the RGB channels into greyscale slices, thus conflicting with the channel slices defined in the input image",
                                "Convert the image from RGB to greyscale or remove the additional channel slices."));
                    }

                    // Output the image as-is
                    String baseName = row + "_";
                    Path outputPath = io3DPath.resolve(baseName + ".tif");
                    IJ.saveAsTiff(img, outputPath.toString());

                    // Create info
                    CellposeImageInfo info = new CellposeImageInfo(row);
                    info.sliceBaseNames.put(new ImageSliceIndex(-1, -1, -1), baseName);
                    runWith3D.add(info);
                } else {
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

    @JIPipeDocumentation(name = "Cellpose: Channels", description = "Determines which channels are used for the segmentation")
    @JIPipeParameter(value = "channel-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public CellposeChannelSettings getChannelSettings() {
        return channelSettings;
    }

    @JIPipeDocumentation(name = "Cellpose: Tweaks", description = "Additional options like augmentation and averaging over multiple networks")
    @JIPipeParameter(value = "enhancement-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png", collapsed = true)
    public CellposeSegmentationTweaksSettings getEnhancementParameters() {
        return segmentationTweaksSettings;
    }

    @JIPipeDocumentation(name = "Cellpose: Thresholds", description = "Parameters that control which objects are selected.")
    @JIPipeParameter(value = "threshold-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png", collapsed = true)
    public CellposeSegmentationThresholdSettings getThresholdParameters() {
        return segmentationThresholdSettings;
    }

    @JIPipeDocumentation(name = "Cellpose: Outputs", description = "The following settings allow you to select which outputs are generated.")
    @JIPipeParameter(value = "output-parameters", collapsed = true, iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public CellposeSegmentationOutputSettings getSegmentationOutputSettings() {
        return segmentationOutputSettings;
    }

    @JIPipeDocumentation(name = "Cellpose: GPU", description = "Controls how the graphics card is utilized.")
    @JIPipeParameter(value = "gpu-parameters", collapsed = true, iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public CellposeGPUSettings getGpuSettings() {
        return gpuSettings;
    }

    @JIPipeDocumentation(name = "Model", description = "The model type that should be used.")
    @JIPipeParameter("model")
    public CellposeModel getModel() {
        return model;
    }

    @JIPipeParameter("model")
    public void setModel(CellposeModel model) {
        if (this.model != model) {
            this.model = model;
            updateInputSlots();
        }
    }

    private void updateOutputSlots() {
        toggleSlot(OUTPUT_FLOWS_D, segmentationOutputSettings.isOutputFlowsD());
        toggleSlot(OUTPUT_PROBABILITIES, segmentationOutputSettings.isOutputProbabilities());
        toggleSlot(OUTPUT_FLOWS_XY, segmentationOutputSettings.isOutputFlowsXY());
        toggleSlot(OUTPUT_LABELS, segmentationOutputSettings.isOutputLabels());
        toggleSlot(OUTPUT_FLOWS_Z, segmentationOutputSettings.isOutputFlowsZ());
        toggleSlot(OUTPUT_ROI, segmentationOutputSettings.isOutputROI());
    }

    @Override
    protected void onDeserialized(JsonNode node, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {
        super.onDeserialized(node, issues, notifications);
        CellposeExtension.createMissingPythonNotificationIfNeeded(notifications);
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

