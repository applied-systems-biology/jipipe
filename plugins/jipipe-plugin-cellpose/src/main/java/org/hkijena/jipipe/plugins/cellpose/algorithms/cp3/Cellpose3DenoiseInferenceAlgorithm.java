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
import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
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
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.cellpose.CellposePlugin;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.Cellpose2ChannelSettings;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.Cellpose2GPUSettings;
import org.hkijena.jipipe.plugins.cellpose.utils.CellposeImageInfo;
import org.hkijena.jipipe.plugins.cellpose.utils.CellposeModelInfo;
import org.hkijena.jipipe.plugins.cellpose.utils.CellposeUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@SetJIPipeDocumentation(name = "Cellpose image restoration (3.x)", description =
        "Runs Cellpose restoration on the input image with the given model(s). " +
                "Cellpose provides algorithms for denoising (similar to noise2void), deblurring, and upsampling. " +
                "This node supports operations in 3D and executing " +
                "Cellpose for each 2D image plane. " +
                "Please note that you need to setup a valid Python environment with Cellpose installed. You can find the setting in Project &gt; Application settings &gt; Extensions &gt; Cellpose.")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true, description = "The input images")
@AddJIPipeInputSlot(value = CellposeModelData.class, name = "Model", create = true, description = "The models (pretrained/custom). All workloads are repeated per model. To provide a pretrained model, use 'Pretrained Cellpose 3.x image restoration model'.", role = JIPipeDataSlotRole.ParametersLooping)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
public class Cellpose3DenoiseInferenceAlgorithm extends JIPipeSingleIterationAlgorithm implements Cellpose3EnvironmentAccessNode {

    private final Cellpose2GPUSettings gpuSettings;
    private final Cellpose2ChannelSettings channelSettings;

    private OptionalDoubleParameter diameter = new OptionalDoubleParameter(30.0, true);
    private boolean enable3DDenoising = true;
    private OptionalTextAnnotationNameParameter diameterAnnotation = new OptionalTextAnnotationNameParameter("Diameter", true);
    private boolean cleanUpAfterwards = true;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();
    private boolean suppressLogs = false;
    private boolean enableMultiChannel = true;

//    private OptionalDataAnnotationNameParameter sizeModelAnnotationName = new OptionalDataAnnotationNameParameter("Size model", true);

    public Cellpose3DenoiseInferenceAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.gpuSettings = new Cellpose2GPUSettings();
        this.channelSettings = new Cellpose2ChannelSettings();

        registerSubParameter(gpuSettings);
        registerSubParameter(channelSettings);
    }

    public Cellpose3DenoiseInferenceAlgorithm(Cellpose3DenoiseInferenceAlgorithm other) {
        super(other);
        this.gpuSettings = new Cellpose2GPUSettings(other.gpuSettings);
        this.channelSettings = new Cellpose2ChannelSettings(other.channelSettings);
        this.suppressLogs = other.suppressLogs;

        this.diameter = new OptionalDoubleParameter(other.diameter);
        this.diameterAnnotation = new OptionalTextAnnotationNameParameter(other.diameterAnnotation);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.enable3DDenoising = other.enable3DDenoising;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.enableMultiChannel = other.enableMultiChannel;

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

    @SetJIPipeDocumentation(name = "Enable 3D denoising", description = "If enabled, Cellpose will denoise in 3D. Otherwise, " +
            "any 3D image will be processed per-slice. Please note that 3D denoising requires large amounts of memory.")
    @JIPipeParameter(value = "enable-3d-denoising", important = true)
    public boolean isEnable3DDenoising() {
        return enable3DDenoising;
    }

    @JIPipeParameter("enable-3d-denoising")
    public void setEnable3DDenoising(boolean enable3DDenoising) {
        this.enable3DDenoising = enable3DDenoising;
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
            CellposeModelInfo modelInfo = CellposeUtils.createModelInfo(modelSlot.getTextAnnotations(modelRow), modelData, workDirectory, modelProgress);
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
        CellposeUtils.saveInputImages(getInputSlot("Input"),
                iterationStep.getInputRows("Input"),
                enable3DDenoising,
                enableMultiChannel, io2DPath, io3DPath, runWith2D, runWith3D, this, progressInfo.resolve("Export input images")
        );

        // Run Cellpose
        if (!runWith2D.isEmpty()) {
            runCellpose(progressInfo.resolve("Cellpose"), io2DPath, false, modelInfo.getModelNameOrPath());
        }
        if (!runWith3D.isEmpty()) {
            runCellpose(progressInfo.resolve("Cellpose"), io3DPath, true, modelInfo.getModelNameOrPath());
        }

        // Deploy and run extraction script
        progressInfo.log("Deploying script to extract Cellpose *.npy results ...");
        Path npyExtractorScript = workDirectory.resolve("extract-cellpose3-denoise-npy.py");
        CellposePlugin.RESOURCES.exportResourceToFile("extract-cellpose3-denoise-npy.py", npyExtractorScript);
        if (!runWith2D.isEmpty()) {
            List<String> arguments = new ArrayList<>();
            arguments.add(npyExtractorScript.toString());
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
            extractDataFromInfo(modelInfo, iterationStep, imageInfo, io2DPath, progressInfo.resolve("Importing results row " + imageInfo.getSourceRow()));
        }
        for (CellposeImageInfo imageInfo : runWith3D) {
            extractDataFromInfo(modelInfo, iterationStep, imageInfo, io3DPath, progressInfo.resolve("Importing results row " + imageInfo.getSourceRow()));
        }
    }

    private void extractDataFromInfo(CellposeModelInfo modelInfo, JIPipeMultiIterationStep iterationStep, CellposeImageInfo imageInfo, Path ioPath, JIPipeProgressInfo progressInfo) {
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>(getInputSlot("Input").getTextAnnotations(imageInfo.getSourceRow()));
        annotationList.addAll(modelInfo.getAnnotationList());
        if (diameterAnnotation.isEnabled()) {
            progressInfo.log("Reading info ...");
            List<JIPipeTextAnnotation> diameterAnnotations = new ArrayList<>();
            for (Map.Entry<ImageSliceIndex, String> entry : imageInfo.getSliceBaseNames().entrySet()) {
                JsonNode node = JsonUtils.readFromFile(ioPath.resolve(entry.getValue() + "_seg_info.json"), JsonNode.class);
                diameterAnnotation.addAnnotationIfEnabled(diameterAnnotations, node.get("est_diam").asText());
            }
            annotationList.addAll(JIPipeTextAnnotationMergeMode.Merge.merge(diameterAnnotations));
        }
        {
            progressInfo.log("Reading restored image ...");
            ImagePlus img = CellposeUtils.extractImageFromInfo(imageInfo, ioPath, "_seg_restored.tif", false, progressInfo);
            iterationStep.addOutputData("Output", new ImagePlusData(img), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
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
            if (ProcessUtils.systemIsMacM1()) {
                arguments.add("mps");
            } else {
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
        arguments.add("--restore_type");
        arguments.add(modelNameOrPath);

        // Input/output
        arguments.add("--dir");
        arguments.add(ioPath.toString());

        // Run the module
        CellposeUtils.runCellpose(getConfiguredCellposeEnvironment(),
                arguments,
                suppressLogs,
                progressInfo);
    }

    @SetJIPipeDocumentation(name = "Enable multichannel", description = "If enabled, multiple image channels are passed to Cellpose. " +
            "Otherwise, each channel will be processed individually.")
    @JIPipeParameter("enable-multichannel")
    public boolean isEnableMultiChannel() {
        return enableMultiChannel;
    }

    @JIPipeParameter("enable-multichannel")
    public void setEnableMultiChannel(boolean enableMultiChannel) {
        this.enableMultiChannel = enableMultiChannel;
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

    @SetJIPipeDocumentation(name = "Cellpose: Channels", description = "Determines which channels are denoised")
    @JIPipeParameter(value = "channel-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public Cellpose2ChannelSettings getChannelSettings() {
        return channelSettings;
    }


    @SetJIPipeDocumentation(name = "Cellpose: GPU", description = "Controls how the graphics card is utilized.")
    @JIPipeParameter(value = "gpu-parameters", collapsed = true, iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public Cellpose2GPUSettings getGpuSettings() {
        return gpuSettings;
    }

}

