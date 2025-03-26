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

package org.hkijena.jipipe.plugins.omnipose.algorithms;

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
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeSizeModelData;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.Cellpose2ChannelSettings;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.Cellpose2GPUSettings;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.Cellpose2SegmentationOutputSettings;
import org.hkijena.jipipe.plugins.cellpose.utils.CellposeUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.omnipose.OmniposeEnvironmentAccessNode;
import org.hkijena.jipipe.plugins.omnipose.OmniposePlugin;
import org.hkijena.jipipe.plugins.omnipose.parameters.OmniposeSegmentationThresholdSettings;
import org.hkijena.jipipe.plugins.omnipose.parameters.OmniposeSegmentationTweaksSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.nio.file.Path;
import java.util.*;

@SetJIPipeDocumentation(name = "Omnipose prediction (0.x)", description = "Runs Omnipose on the input image. This node supports both segmentation in 3D and executing " +
        "Omnipose for each 2D image plane. " +
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
        "Please note that you need to setup a valid Python environment with Omnipose installed. You can find the setting in Project &gt; Application settings &gt; Extensions &gt; Omnipose.")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true, description = "The input images")
@AddJIPipeInputSlot(value = CellposeModelData.class, name = "Model", create = true, description = "The models (pretrained/custom). All workloads are repeated per model.", role = JIPipeDataSlotRole.ParametersLooping)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Labels")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Flows XY")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Flows Z")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Flows d")
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Probabilities")
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "ROI")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
public class Omnipose0InferenceAlgorithm extends JIPipeSingleIterationAlgorithm implements OmniposeEnvironmentAccessNode {

    public static final JIPipeDataSlotInfo OUTPUT_LABELS = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Labels", "A grayscale image where each connected component is assigned a unique value");
    public static final JIPipeDataSlotInfo OUTPUT_FLOWS_XY = new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Output, "Flows XY", "An RGB image that indicates the x and y flow of each pixel");
    public static final JIPipeDataSlotInfo OUTPUT_FLOWS_Z = new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Output, "Flows Z", "Flows in Z direction (black for non-3D images)");
    public static final JIPipeDataSlotInfo OUTPUT_FLOWS_D = new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Output, "Flows d", "Multi-channel image that contains the flows [dZ, dY, dX, cell probability] (3D images) / [dY, dX, cell probability] (2D images)");
    public static final JIPipeDataSlotInfo OUTPUT_PROBABILITIES = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Probabilities", "An image indicating the cell probabilities for each pixel");
    public static final JIPipeDataSlotInfo OUTPUT_ROI = new JIPipeDataSlotInfo(ROI2DListData.class, JIPipeSlotType.Output, "ROI", "ROI of the segmented areas");

    private final Cellpose2GPUSettings gpuSettings;
    private final OmniposeSegmentationTweaksSettings segmentationTweaksSettings;
    private final OmniposeSegmentationThresholdSettings segmentationThresholdSettings;
    private final Cellpose2SegmentationOutputSettings segmentationOutputSettings;
    private final Cellpose2ChannelSettings channelSettings;

    private OptionalDoubleParameter diameter = new OptionalDoubleParameter(30.0, true);
    private boolean enable3DSegmentation = true;
    private OptionalTextAnnotationNameParameter diameterAnnotation = new OptionalTextAnnotationNameParameter("Diameter", true);
    private boolean cleanUpAfterwards = true;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();
    private boolean suppressLogs = false;

    public Omnipose0InferenceAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.segmentationTweaksSettings = new OmniposeSegmentationTweaksSettings();
        this.gpuSettings = new Cellpose2GPUSettings();
        this.segmentationThresholdSettings = new OmniposeSegmentationThresholdSettings();
        this.segmentationOutputSettings = new Cellpose2SegmentationOutputSettings();
        this.channelSettings = new Cellpose2ChannelSettings();

        updateOutputSlots();

        registerSubParameter(segmentationTweaksSettings);
        registerSubParameter(segmentationThresholdSettings);
        registerSubParameter(segmentationOutputSettings);
        registerSubParameter(gpuSettings);
        registerSubParameter(channelSettings);
    }

    public Omnipose0InferenceAlgorithm(Omnipose0InferenceAlgorithm other) {
        super(other);
        this.gpuSettings = new Cellpose2GPUSettings(other.gpuSettings);
        this.segmentationTweaksSettings = new OmniposeSegmentationTweaksSettings(other.segmentationTweaksSettings);
        this.segmentationThresholdSettings = new OmniposeSegmentationThresholdSettings(other.segmentationThresholdSettings);
        this.segmentationOutputSettings = new Cellpose2SegmentationOutputSettings(other.segmentationOutputSettings);
        this.channelSettings = new Cellpose2ChannelSettings(other.channelSettings);

        this.diameter = new OptionalDoubleParameter(other.diameter);
        this.diameterAnnotation = new OptionalTextAnnotationNameParameter(other.diameterAnnotation);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.enable3DSegmentation = other.enable3DSegmentation;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.suppressLogs = other.suppressLogs;

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
        target.add(getConfiguredOmniposeEnvironment());
    }

    @SetJIPipeDocumentation(name = "Suppress logs", description = "If enabled, the node will not log the status of the Python operation. " +
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
            "the one in the Project > Application settings > Extensions > Cellpose is used.")
    @JIPipeParameter("override-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Omnipose", allowArtifact = true, artifactFilters = {"com.github.kevinjohncutler.omnipose:*"})
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
            reportConfiguredOmniposeEnvironmentValidity(reportContext, report);
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
            CellposeSizeModelData sizeModelData = null;
//            if(sizeModelAnnotationName.isEnabled() && !StringUtils.isNullOrEmpty(sizeModelAnnotationName.getContent())) {
//                JIPipeDataAnnotation dataAnnotation = getInputSlot("Model").getDataAnnotation(modelRow, sizeModelAnnotationName.getContent());
//                if(dataAnnotation != null) {
//                    sizeModelData = dataAnnotation.getData(CellposeSizeModelData.class, modelProgress);
//                }
//            }

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
//            if(sizeModelData != null) {
//                Path tempDirectory = PathUtils.createTempDirectory(workDirectory.resolve("models"), "size-model");
//                sizeModelData.exportData(new JIPipeFileSystemWriteDataStorage(modelProgress, tempDirectory), null, false, modelProgress);
//                modelInfo.sizeModelNameOrPath = tempDirectory.resolve(sizeModelData.getMetadata().getName()).toString();
//            }

            modelInfos.add(modelInfo);
        }

        for (int i = 0; i < modelInfos.size(); i++) {
            CellposeModelInfo modelInfo = modelInfos.get(i);
            JIPipeProgressInfo modelProgress = progressInfo.resolve("Model", i, modelInfos.size());
            processModel(PathUtils.createTempSubDirectory(workDirectory, "run"), modelInfo, iterationStep, iterationContext, runContext, modelProgress);
        }

        // Cleanup
        if (cleanUpAfterwards) {
            PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
        }
    }

    private void processModel(Path workDirectory, CellposeModelInfo modelInfo, JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // We need a 2D and a 3D branch due to incompatibilities on the side of Cellpose
        final Path io2DPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "io-2d");
        final Path io3DPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "io-3d");

        List<CellposeImageInfo> runWith2D = new ArrayList<>();
        List<CellposeImageInfo> runWith3D = new ArrayList<>();

        // Save input images
        saveInputImages(iterationStep, progressInfo.resolve("Export input images"), io2DPath, io3DPath, runWith2D, runWith3D);

        // Run Cellpose
        if (!runWith2D.isEmpty()) {
            runOmnipose(progressInfo.resolve("Omnipose"), io2DPath, false, modelInfo.modelNameOrPath, null);
        }
        if (!runWith3D.isEmpty()) {
            runOmnipose(progressInfo.resolve("Omnipose"), io3DPath, false, modelInfo.modelNameOrPath, null);
        }

        // Deploy and run extraction script
        progressInfo.log("Deploying script to extract Omnipose *.npy results ...");
        Path npyExtractorScript = workDirectory.resolve("extract-cellpose2-npy.py");
        CellposePlugin.RESOURCES.exportResourceToFile("extract-cellpose2-npy.py", npyExtractorScript);
        if (!runWith2D.isEmpty()) {
            List<String> arguments = new ArrayList<>();
            arguments.add(npyExtractorScript.toString());
            if (!segmentationOutputSettings.isOutputROI())
                arguments.add("--skip-roi");
            arguments.add(io2DPath.toString());
            arguments.add(io2DPath.toString());
            PythonUtils.runPython(arguments.toArray(new String[0]),
                    getConfiguredOmniposeEnvironment(),
                    Collections.emptyList(),
                    Collections.emptyMap(),
                    suppressLogs,
                    false, progressInfo.resolve("Extract Omnipose results (2D)"));
        }
        if (!runWith3D.isEmpty()) {
            List<String> arguments = new ArrayList<>();
            arguments.add(npyExtractorScript.toString());
            if (!segmentationOutputSettings.isOutputROI())
                arguments.add("--skip-roi");
            arguments.add(io3DPath.toString());
            arguments.add(io3DPath.toString());
            PythonUtils.runPython(arguments.toArray(new String[0]),
                    getConfiguredOmniposeEnvironment(),
                    Collections.emptyList(),
                    Collections.emptyMap(),
                    suppressLogs,
                    false, progressInfo.resolve("Extract Omnipose results (3D)"));
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

    private void runOmnipose(JIPipeProgressInfo progressInfo, Path ioPath, boolean with3D, String modelNameOrPath, String sizeModelNameOrPath) {
        List<String> arguments = new ArrayList<>();
        Map<String, String> envVars = new HashMap<>();

        arguments.add("-m");
        arguments.add("cellpose");

        // Activate Omnipose
        arguments.add("--omni");

        // Full log
        arguments.add("--verbose");

        // Suppress input
        arguments.add("--testing");

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
        if (gpuSettings.isEnableGPU()) {
            arguments.add("--use_gpu");
            if (gpuSettings.getGpuDevice().isEnabled()) {
                envVars.put("CUDA_VISIBLE_DEVICES", gpuSettings.getGpuDevice().getContent().toString());
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
        if (segmentationTweaksSettings.isCluster()) {
            arguments.add("--cluster");
        }
        if (segmentationTweaksSettings.isFastMode()) {
            arguments.add("--fast_mode");
        }
        if (!segmentationTweaksSettings.isNetAverage()) {
            arguments.add("--no_net_avg");
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
            arguments.add("--mask_threshold");
            arguments.add(segmentationThresholdSettings.getMaskThreshold() + "");
        }
        {
            arguments.add("--diam_threshold");
            arguments.add(segmentationThresholdSettings.getDiamThreshold() + "");
        }

        // Input/output
        arguments.add("--dir");
        arguments.add(ioPath.toString());

        // Run the module
        CellposeUtils.runCellpose(getConfiguredOmniposeEnvironment(),
                arguments,
                suppressLogs,
                progressInfo);
    }

    private void saveInputImages(JIPipeMultiIterationStep iterationStep, JIPipeProgressInfo progressInfo, Path io2DPath, Path io3DPath, List<CellposeImageInfo> runWith2D, List<CellposeImageInfo> runWith3D) {
        for (int row : iterationStep.getInputRows("Input")) {
            JIPipeProgressInfo rowProgress = progressInfo.resolve("Data row " + row);

            ImagePlus img = getInputSlot("Input").getData(row, ImagePlusData.class, rowProgress).getImage();
            if (img.getNFrames() > 1) {
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(this),
                        "Omnipose does not support time series!",
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
                        throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(this),
                                "Omnipose does not support 3D multichannel images with RGB!",
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
                    ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
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

    @SetJIPipeDocumentation(name = "Omnipose: Channels", description = "Determines which channels are used for the segmentation")
    @JIPipeParameter(value = "channel-parameters", resourceClass = OmniposePlugin.class, iconURL = "/org/hkijena/jipipe/plugins/omnipose/icons/omnipose.png")
    public Cellpose2ChannelSettings getChannelSettings() {
        return channelSettings;
    }

    @SetJIPipeDocumentation(name = "Omnipose: Tweaks", description = "Additional options like augmentation and averaging over multiple networks")
    @JIPipeParameter(value = "enhancement-parameters", resourceClass = OmniposePlugin.class, iconURL = "/org/hkijena/jipipe/plugins/omnipose/icons/omnipose.png", collapsed = true)
    public OmniposeSegmentationTweaksSettings getEnhancementParameters() {
        return segmentationTweaksSettings;
    }

    @SetJIPipeDocumentation(name = "Omnipose: Thresholds", description = "Parameters that control which objects are selected.")
    @JIPipeParameter(value = "threshold-parameters", resourceClass = OmniposePlugin.class, iconURL = "/org/hkijena/jipipe/plugins/omnipose/icons/omnipose.png", collapsed = true)
    public OmniposeSegmentationThresholdSettings getThresholdParameters() {
        return segmentationThresholdSettings;
    }

    @SetJIPipeDocumentation(name = "Omnipose: Outputs", description = "The following settings allow you to select which outputs are generated.")
    @JIPipeParameter(value = "output-parameters", collapsed = true, resourceClass = OmniposePlugin.class, iconURL = "/org/hkijena/jipipe/plugins/omnipose/icons/omnipose.png")
    public Cellpose2SegmentationOutputSettings getSegmentationOutputSettings() {
        return segmentationOutputSettings;
    }

    @SetJIPipeDocumentation(name = "Omnipose: GPU", description = "Controls how the graphics card is utilized.")
    @JIPipeParameter(value = "gpu-parameters", collapsed = true, resourceClass = OmniposePlugin.class, iconURL = "/org/hkijena/jipipe/plugins/omnipose/icons/omnipose.png")
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

