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

package org.hkijena.jipipe.plugins.cellpose.algorithms.cp3;

import com.fasterxml.jackson.databind.JsonNode;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSingleIterationAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.cellpose.CellposePlugin;
import org.hkijena.jipipe.plugins.cellpose.CellposeUtils;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.*;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp3.Cellpose3SegmentationTweaksSettings;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.nio.file.Path;
import java.util.*;


@SetJIPipeDocumentation(name = "Cellpose segmentation (3.x)", description =
        "Runs Cellpose on the input image with the given model(s). This node supports both segmentation in 3D and executing " +
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
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true, description = "The input images")
@AddJIPipeInputSlot(value = CellposeModelData.class, name = "Model", create = true, description = "The models (pretrained/custom). All workloads are repeated per model. To provide a pretrained model, use 'Pretrained Cellpose 3.x segmentation model'", role = JIPipeDataSlotRole.ParametersLooping)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Labels")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Flows XY")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Flows Z")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Flows d")
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Probabilities")
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "ROI")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
public class Cellpose3SegmentationInferenceAlgorithm extends JIPipeSingleIterationAlgorithm implements Cellpose3EnvironmentAccessNode {

    public static final JIPipeDataSlotInfo OUTPUT_LABELS = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Labels", "A grayscale image where each connected component is assigned a unique value");
    public static final JIPipeDataSlotInfo OUTPUT_FLOWS_XY = new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Output, "Flows XY", "An RGB image that indicates the x and y flow of each pixel");
    public static final JIPipeDataSlotInfo OUTPUT_FLOWS_Z = new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Output, "Flows Z", "Flows in Z direction (black for non-3D images)");
    public static final JIPipeDataSlotInfo OUTPUT_FLOWS_D = new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Output, "Flows d", "Multi-channel image that contains the flows [dZ, dY, dX, cell probability] (3D images) / [dY, dX, cell probability] (2D images)");
    public static final JIPipeDataSlotInfo OUTPUT_PROBABILITIES = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Probabilities", "An image indicating the cell probabilities for each pixel");
    public static final JIPipeDataSlotInfo OUTPUT_ROI = new JIPipeDataSlotInfo(ROI2DListData.class, JIPipeSlotType.Output, "ROI", "ROI of the segmented areas");

    private final Cellpose2GPUSettings gpuSettings;
    private final Cellpose3SegmentationTweaksSettings segmentationTweaksSettings;
    private final Cellpose2SegmentationThresholdSettings segmentationThresholdSettings;
    private final Cellpose2SegmentationOutputSettings segmentationOutputSettings;

    private final Cellpose2ChannelSettings channelSettings;
    private OptionalDoubleParameter diameter = new OptionalDoubleParameter(30.0, true);
    private boolean enable3DSegmentation = true;
    private OptionalTextAnnotationNameParameter diameterAnnotation = new OptionalTextAnnotationNameParameter("Diameter", true);
    private boolean cleanUpAfterwards = true;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();
    private boolean suppressLogs = false;

//    private OptionalDataAnnotationNameParameter sizeModelAnnotationName = new OptionalDataAnnotationNameParameter("Size model", true);

    public Cellpose3SegmentationInferenceAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.segmentationTweaksSettings = new Cellpose3SegmentationTweaksSettings();
        this.gpuSettings = new Cellpose2GPUSettings();
        this.segmentationThresholdSettings = new Cellpose2SegmentationThresholdSettings();
        this.segmentationOutputSettings = new Cellpose2SegmentationOutputSettings();
        this.channelSettings = new Cellpose2ChannelSettings();

        updateOutputSlots();

        registerSubParameter(segmentationTweaksSettings);
        registerSubParameter(segmentationThresholdSettings);
        registerSubParameter(segmentationOutputSettings);
        registerSubParameter(gpuSettings);
        registerSubParameter(channelSettings);
    }

    public Cellpose3SegmentationInferenceAlgorithm(Cellpose3SegmentationInferenceAlgorithm other) {
        super(other);
        this.gpuSettings = new Cellpose2GPUSettings(other.gpuSettings);
        this.segmentationTweaksSettings = new Cellpose3SegmentationTweaksSettings(other.segmentationTweaksSettings);
        this.segmentationThresholdSettings = new Cellpose2SegmentationThresholdSettings(other.segmentationThresholdSettings);
        this.segmentationOutputSettings = new Cellpose2SegmentationOutputSettings(other.segmentationOutputSettings);
        this.channelSettings = new Cellpose2ChannelSettings(other.channelSettings);
        this.suppressLogs = other.suppressLogs;
//        this.sizeModelAnnotationName = new OptionalDataAnnotationNameParameter(other.sizeModelAnnotationName);

        this.diameter = new OptionalDoubleParameter(other.diameter);
        this.diameterAnnotation = new OptionalTextAnnotationNameParameter(other.diameterAnnotation);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.enable3DSegmentation = other.enable3DSegmentation;
        this.cleanUpAfterwards = other.cleanUpAfterwards;

        updateOutputSlots();

        registerSubParameter(segmentationTweaksSettings);
        registerSubParameter(segmentationThresholdSettings);
        registerSubParameter(segmentationOutputSettings);
        registerSubParameter(gpuSettings);
        registerSubParameter(channelSettings);
    }

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        super.getEnvironmentDependencies(target);
        target.add(getConfiguredCellposeEnvironment());
    }

    @SetJIPipeDocumentation(name = "Suppress logs", description = "If enabled, the node will not log the status of the Cellpose operation. " +
            "Can be used to limit memory consumption of JIPipe if larger data sets are used.")
    @JIPipeParameter("suppress-logs")
    public boolean isSuppressLogs() {
        return suppressLogs;
    }

    @JIPipeParameter("suppress-logs")
    public void setSuppressLogs(boolean suppressLogs) {
        this.suppressLogs = suppressLogs;
    }

    @SetJIPipeDocumentation(name = "Enable 3D segmentation", description = "If enabled, Cellpose will segment in 3D. Otherwise, " +
            "any 3D image will be processed per-slice. Please note that 3D segmentation requires large amounts of memory.")
    @JIPipeParameter(value = "enable-3d-segmentation", important = true)
    public boolean isEnable3DSegmentation() {
        return enable3DSegmentation;
    }

    @JIPipeParameter("enable-3d-segmentation")
    public void setEnable3DSegmentation(boolean enable3DSegmentation) {
        this.enable3DSegmentation = enable3DSegmentation;
    }

    @SetJIPipeDocumentation(name = "Override Python environment", description = "If enabled, a different Python environment is used for this Node. Otherwise " +
            "the one in the Project > Application settings > Extensions > Cellpose 3.x is used.")
    @JIPipeParameter("override-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Cellpose 3", allowArtifact = true, artifactFilters = {"com.github.mouseland.cellpose3:*"})
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
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (!isPassThrough()) {
            reportConfiguredCellposeEnvironmentValidity(reportContext, report);
        }
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path workDirectory = getNewScratch();
        progressInfo.log("Work directory is " + workDirectory);

        progressInfo.log("Collecting models ...");
        List<CellposeModelInfo> modelInfos = new ArrayList<>();
        JIPipeInputDataSlot modelSlot = getInputSlot("Model");
        for (int modelRow : iterationStep.getInputRows(modelSlot)) {
            JIPipeProgressInfo modelProgress = progressInfo.resolve("Model row " + modelRow);
            CellposeModelData modelData = modelSlot.getData(modelRow, CellposeModelData.class, modelProgress);

            // Save the model out
            CellposeModelInfo modelInfo = new CellposeModelInfo();
            modelInfo.annotationList = modelSlot.getTextAnnotations(modelRow);
            if (modelData.isPretrained()) {
                modelInfo.modelPretrained = true;
                modelInfo.modelNameOrPath = modelData.getPretrainedModelName();
            } else {
                Path tempDirectory = PathUtils.createTempSubDirectory(workDirectory.resolve("models"), "model");
                modelData.exportData(new JIPipeFileSystemWriteDataStorage(modelProgress, tempDirectory), null, false, modelProgress);
                modelInfo.modelPretrained = false;
                modelInfo.modelNameOrPath = tempDirectory.resolve(modelData.getMetadata().getName()).toString();
            }
            modelInfos.add(modelInfo);
        }

        for (int i = 0; i < modelInfos.size(); i++) {
            CellposeModelInfo modelInfo = modelInfos.get(i);
            JIPipeProgressInfo modelProgress = progressInfo.resolve("Model", i, modelInfos.size());
            processModel(PathUtils.createTempSubDirectory(workDirectory, "run"), modelInfo, iterationStep, modelProgress);
        }

        // Cleanup
        if (cleanUpAfterwards) {
            PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
        }
    }

    private void processModel(Path workDirectory, CellposeModelInfo modelInfo, JIPipeMultiIterationStep iterationStep, JIPipeProgressInfo progressInfo) {
        // We need a 2D and a 3D branch due to incompatibilities on the side of Cellpose
        final Path io2DPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "io-2d");
        final Path io3DPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "io-3d");

        List<CellposeImageInfo> runWith2D = new ArrayList<>();
        List<CellposeImageInfo> runWith3D = new ArrayList<>();

        // Save input images
        saveInputImages(iterationStep, progressInfo.resolve("Export input images"), io2DPath, io3DPath, runWith2D, runWith3D);

        // Run Cellpose
        if (!runWith2D.isEmpty()) {
            runCellpose(progressInfo.resolve("Cellpose"), io2DPath, false, modelInfo.modelNameOrPath);
        }
        if (!runWith3D.isEmpty()) {
            runCellpose(progressInfo.resolve("Cellpose"), io3DPath, true, modelInfo.modelNameOrPath);
        }

        // Deploy and run extraction script
        progressInfo.log("Deploying script to extract Cellpose *.npy results ...");
        Path npyExtractorScript = workDirectory.resolve("extract-cellpose3-npy.py");
        CellposePlugin.RESOURCES.exportResourceToFile("extract-cellpose3-npy.py", npyExtractorScript);
        if (!runWith2D.isEmpty()) {
            List<String> arguments = new ArrayList<>();
            arguments.add(npyExtractorScript.toString());
            if (!segmentationOutputSettings.isOutputROI()) {
                arguments.add("--skip-roi");
            }
            arguments.add(io2DPath.toString());
            arguments.add(io2DPath.toString());
            PythonUtils.runPython(arguments.toArray(new String[0]),
                    getConfiguredCellposeEnvironment(),
                    Collections.emptyList(),
                    Collections.emptyMap(),
                    suppressLogs,
                    false, progressInfo.resolve("Extract Cellpose results (2D)"));
        }
        if (!runWith3D.isEmpty()) {
            List<String> arguments = new ArrayList<>();
            arguments.add(npyExtractorScript.toString());
            if (!segmentationOutputSettings.isOutputROI()) {
                arguments.add("--skip-roi");
            }
            arguments.add(io3DPath.toString());
            arguments.add(io3DPath.toString());
            PythonUtils.runPython(arguments.toArray(new String[0]),
                    getConfiguredCellposeEnvironment(),
                    Collections.emptyList(),
                    Collections.emptyMap(),
                    suppressLogs,
                    false, progressInfo.resolve("Extract Cellpose results (3D)"));
        }

        // Fetch the data from the directory
        for (CellposeImageInfo imageInfo : runWith2D) {
            extractDataFromInfo(modelInfo, iterationStep, imageInfo, io2DPath, progressInfo.resolve("Importing results row " + imageInfo.sourceRow));
        }
        for (CellposeImageInfo imageInfo : runWith3D) {
            extractDataFromInfo(modelInfo, iterationStep, imageInfo, io3DPath, progressInfo.resolve("Importing results row " + imageInfo.sourceRow));
        }
    }

    private void extractDataFromInfo(CellposeModelInfo modelInfo, JIPipeMultiIterationStep iterationStep, CellposeImageInfo imageInfo, Path ioPath, JIPipeProgressInfo progressInfo) {
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>(getInputSlot("Input").getTextAnnotations(imageInfo.sourceRow));
        annotationList.addAll(modelInfo.annotationList);
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
            ROI2DListData rois = new ROI2DListData();
            for (Map.Entry<ImageSliceIndex, String> entry : imageInfo.sliceBaseNames.entrySet()) {
                ROI2DListData sliceRoi = CellposeUtils.cellposeROIJsonToImageJ(ioPath.resolve(entry.getValue() + "_seg_roi.json"));
                if (imageInfo.sliceBaseNames.size() > 1) {
                    for (Roi roi : sliceRoi) {
                        roi.setPosition(0, entry.getKey().getZ() + 1, 0);
                    }
                }
                rois.addAll(sliceRoi);
            }
            iterationStep.addOutputData("ROI", rois, annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (segmentationOutputSettings.isOutputFlowsD()) {
            progressInfo.log("Reading Flows d ...");
            ImagePlus img = extractImageFromInfo(imageInfo, ioPath, "_seg_flows_dz_dy_dx.tif", true, progressInfo);
            iterationStep.addOutputData(OUTPUT_FLOWS_D.getName(), new ImagePlusData(img), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (segmentationOutputSettings.isOutputFlowsXY()) {
            progressInfo.log("Reading Flows XY ...");
            ImagePlus img = extractImageFromInfo(imageInfo, ioPath, "_seg_flows_rgb.tif", false, progressInfo);
            iterationStep.addOutputData(OUTPUT_FLOWS_XY.getName(), new ImagePlusData(img), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (segmentationOutputSettings.isOutputFlowsZ()) {
            progressInfo.log("Reading Flows Z ...");
            ImagePlus img = extractImageFromInfo(imageInfo, ioPath, "_seg_flows_z.tif", false, progressInfo);
            iterationStep.addOutputData(OUTPUT_FLOWS_Z.getName(), new ImagePlusData(img), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (segmentationOutputSettings.isOutputLabels()) {
            progressInfo.log("Reading labels ...");
            ImagePlus img = extractImageFromInfo(imageInfo, ioPath, "_seg_labels.tif", false, progressInfo);
            iterationStep.addOutputData(OUTPUT_LABELS.getName(), new ImagePlusGreyscaleData(img), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (segmentationOutputSettings.isOutputProbabilities()) {
            progressInfo.log("Reading probabilities ...");
            ImagePlus img = extractImageFromInfo(imageInfo, ioPath, "_seg_probabilities.tif", false, progressInfo);
            iterationStep.addOutputData(OUTPUT_PROBABILITIES.getName(), new ImagePlusGreyscale32FData(img), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
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

    private void runCellpose(JIPipeProgressInfo progressInfo, Path ioPath, boolean with3D, String modelNameOrPath) {
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

            // Special handling for macOS ARM (M1 etc)
            if(ProcessUtils.systemIsMacM1()) {
                arguments.add("mps");
            }
            else {
                arguments.add(gpuSettings.getGpuDevice().getContent() + "");
            }
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
        arguments.add("--pretrained_model");
        arguments.add(modelNameOrPath);

        // Tweaks
        if (!segmentationTweaksSettings.isNormalize()) {
            arguments.add("--no_norm");
        }
        if (segmentationTweaksSettings.getNormalizePercentile().isEnabled()) {
            arguments.add("--norm_percentile");
            arguments.add(segmentationTweaksSettings.getNormalizePercentile().getContent().getX() + "");
            arguments.add(segmentationTweaksSettings.getNormalizePercentile().getContent().getY() + "");
        }
        if (!segmentationTweaksSettings.isInterpolate()) {
            arguments.add("--no_interp");
        }
        if (segmentationTweaksSettings.getAnisotropy().isEnabled()) {
            arguments.add("--anisotropy");
            arguments.add(segmentationTweaksSettings.getAnisotropy().getContent() + "");
        }
        if (!segmentationTweaksSettings.isResample()) {
            arguments.add("--no_resample");
        }
        {
            arguments.add("--niter");
            arguments.add(String.valueOf(segmentationTweaksSettings.getNiter()));
        }
        {
            arguments.add("--flow3D_smooth");
            arguments.add(String.valueOf(segmentationTweaksSettings.getFlow3DSmoothing()));
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
        CellposeUtils.runCellpose(getConfiguredCellposeEnvironment(),
                arguments,
                suppressLogs,
                progressInfo);
    }

    private void saveInputImages(JIPipeMultiIterationStep iterationStep, JIPipeProgressInfo progressInfo, Path io2DPath, Path io3DPath, List<CellposeImageInfo> runWith2D, List<CellposeImageInfo> runWith3D) {
        for (int row : iterationStep.getInputRows("Input")) {
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

//    @SetJIPipeDocumentation(name = "Size model data annotation", description = "Name of the size model data annotation. Will be extracted from the inputs of the 'Model' input. ")
//    @JIPipeParameter("size-model-annotation-name")
//    public OptionalDataAnnotationNameParameter getSizeModelAnnotationName() {
//        return sizeModelAnnotationName;
//    }
//
//    @JIPipeParameter("size-model-annotation-name")
//    public void setSizeModelAnnotationName(OptionalDataAnnotationNameParameter sizeModelAnnotationName) {
//        this.sizeModelAnnotationName = sizeModelAnnotationName;
//    }

    @SetJIPipeDocumentation(name = "Clean up data after processing", description = "If enabled, data is deleted from temporary directories after " +
            "the processing was finished. Disable this to make it possible to debug your scripts. The directories are accessible via the logs (Tools &gt; Logs).")
    @JIPipeParameter("cleanup-afterwards")
    public boolean isCleanUpAfterwards() {
        return cleanUpAfterwards;
    }

    @JIPipeParameter("cleanup-afterwards")
    public void setCleanUpAfterwards(boolean cleanUpAfterwards) {
        this.cleanUpAfterwards = cleanUpAfterwards;
    }

    @SetJIPipeDocumentation(name = "Annotate with diameter", description = "If enabled, the diameter is attached as annotation. " +
            "Useful if you want to let Cellpose estimate the object diameters.")
    @JIPipeParameter("diameter-annotation")
    public OptionalTextAnnotationNameParameter getDiameterAnnotation() {
        return diameterAnnotation;
    }

    @JIPipeParameter("diameter-annotation")
    public void setDiameterAnnotation(OptionalTextAnnotationNameParameter diameterAnnotation) {
        this.diameterAnnotation = diameterAnnotation;
    }

    @SetJIPipeDocumentation(name = "Average object diameter", description = "If enabled, Cellpose will use the provided average diameter to find objects. " +
            "Otherwise, Cellpose will estimate the diameter by itself.")
    @JIPipeParameter(value = "diameter", important = true)
    public OptionalDoubleParameter getDiameter() {
        return diameter;
    }

    @JIPipeParameter("diameter")
    public void setDiameter(OptionalDoubleParameter diameter) {
        this.diameter = diameter;
    }

    @SetJIPipeDocumentation(name = "Cellpose: Channels", description = "Determines which channels are used for the segmentation")
    @JIPipeParameter(value = "channel-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public Cellpose2ChannelSettings getChannelSettings() {
        return channelSettings;
    }

    @SetJIPipeDocumentation(name = "Cellpose: Tweaks", description = "Advanced segmentation settings.")
    @JIPipeParameter(value = "enhancement-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png", collapsed = true)
    public Cellpose3SegmentationTweaksSettings getEnhancementParameters() {
        return segmentationTweaksSettings;
    }

    @SetJIPipeDocumentation(name = "Cellpose: Thresholds", description = "Parameters that control which objects are selected.")
    @JIPipeParameter(value = "threshold-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png", collapsed = true)
    public Cellpose2SegmentationThresholdSettings getThresholdParameters() {
        return segmentationThresholdSettings;
    }

    @SetJIPipeDocumentation(name = "Cellpose: Outputs", description = "The following settings allow you to select which outputs are generated.")
    @JIPipeParameter(value = "output-parameters", collapsed = true, iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public Cellpose2SegmentationOutputSettings getSegmentationOutputSettings() {
        return segmentationOutputSettings;
    }

    @SetJIPipeDocumentation(name = "Cellpose: GPU", description = "Controls how the graphics card is utilized.")
    @JIPipeParameter(value = "gpu-parameters", collapsed = true, iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public Cellpose2GPUSettings getGpuSettings() {
        return gpuSettings;
    }

    private void updateOutputSlots() {
        toggleSlot(OUTPUT_FLOWS_D, segmentationOutputSettings.isOutputFlowsD());
        toggleSlot(OUTPUT_PROBABILITIES, segmentationOutputSettings.isOutputProbabilities());
        toggleSlot(OUTPUT_FLOWS_XY, segmentationOutputSettings.isOutputFlowsXY());
        toggleSlot(OUTPUT_LABELS, segmentationOutputSettings.isOutputLabels());
        toggleSlot(OUTPUT_FLOWS_Z, segmentationOutputSettings.isOutputFlowsZ());
        toggleSlot(OUTPUT_ROI, segmentationOutputSettings.isOutputROI());
    }

    private static class CellposeModelInfo {
        private String modelNameOrPath;
        private String sizeModelNameOrPath;
        private boolean modelPretrained;
        private List<JIPipeTextAnnotation> annotationList;
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

