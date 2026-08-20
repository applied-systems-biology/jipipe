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

package org.hkijena.jipipe.plugins.cellpose.algorithms.cp4;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurationCache;
import org.hkijena.jipipe.api.environments.RegisterJIPipeEnvironmentUsage;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSingleIterationAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.desktop.commons.components.project.ArtifactUpgrade;
import org.hkijena.jipipe.desktop.commons.components.project.ArtifactUpgradeUtils;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeSizeModelData;
import org.hkijena.jipipe.plugins.cellpose.environments.cp4.Cellpose4Environment;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.Cellpose2GPUSettings;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp3.Cellpose3SegmentationTrainingTweaksSettings;
import org.hkijena.jipipe.plugins.cellpose.utils.CellposeModelInfo;
import org.hkijena.jipipe.plugins.cellpose.utils.CellposeUtils;
import org.hkijena.jipipe.plugins.cellpose.utils.CellposeVersionUtils;
import org.hkijena.jipipe.plugins.expressions.DataAnnotationQueryExpression;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.binary.ConnectedComponentsLabeling2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.binary.ConnectedComponentsLabeling3DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood2D;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood3D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale16UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.VersionUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Cellpose segmentation training (4.x)", description =
        "Trains a segmentation model with Cellpose 4.2+. You start from the cpsam model or train from scratch. " +
                "Incoming images are automatically converted to greyscale. Only 2D or 3D images are supported. For this node to work, you need to annotate a greyscale 16-bit or 8-bit label image column to each raw data input. " +
                "To do this, you can use the node 'Annotate with data'. By default, JIPipe will ensure that all connected components of this image are assigned a unique component. You can disable this feature via the parameters. " +
                "Does not support the training of image restoration models.")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Training data", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Test data", create = true, optional = true)
@AddJIPipeInputSlot(value = CellposeModelData.class, name = "Pretrained model", create = true, description = "The pretrained model. If you want to train from scratch, provide a pretrained model 'None'. Only cpsam is recommended for training.", role = JIPipeDataSlotRole.ParametersLooping)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
@AddJIPipeOutputSlot(value = CellposeModelData.class, name = "Model", create = true, description = "The trained model")
@AddJIPipeOutputSlot(value = CellposeSizeModelData.class)
@RegisterJIPipeEnvironmentUsage(Cellpose4Environment.class)
public class Cellpose4SegmentationTraining2Algorithm extends JIPipeSingleIterationAlgorithm {

    public static final JIPipeDataSlotInfo OUTPUT_SIZE_MODEL = new JIPipeDataSlotInfo(CellposeSizeModelData.class, JIPipeSlotType.Output, "Size Model", "Generated size model", true);

    private final Cellpose2GPUSettings gpuSettings;
    private final Cellpose3SegmentationTrainingTweaksSettings tweaksSettings;
    private int numEpochs = 100;
    private boolean enable3DSegmentation = true;
    private boolean cleanUpAfterwards = true;
    private OptionalDoubleParameter diameter = new OptionalDoubleParameter(30, false);
    private boolean trainSizeModel = false;
    private DataAnnotationQueryExpression labelDataAnnotation = new DataAnnotationQueryExpression("\"Label\"");
    private boolean suppressLogs = false;
    private boolean clearLabelDataAnnotation = true;
    private OptionalIntegerParameter saveEvery = new OptionalIntegerParameter(true, 100);
    private String modelNameOut = "";

    public Cellpose4SegmentationTraining2Algorithm(JIPipeNodeInfo info) {
        super(info);
        this.gpuSettings = new Cellpose2GPUSettings();
        this.tweaksSettings = new Cellpose3SegmentationTrainingTweaksSettings();
        this.tweaksSettings.setLearningRate(0.00001);
        this.tweaksSettings.setWeightDecay(0.1);
        this.tweaksSettings.setBatchSize(1);
        updateSlots();

        registerSubParameter(gpuSettings);
        registerSubParameter(tweaksSettings);
    }

    public Cellpose4SegmentationTraining2Algorithm(Cellpose4SegmentationTraining2Algorithm other) {
        super(other);

        this.gpuSettings = new Cellpose2GPUSettings(other.gpuSettings);
        this.tweaksSettings = new Cellpose3SegmentationTrainingTweaksSettings(other.tweaksSettings);
        this.suppressLogs = other.suppressLogs;

        this.numEpochs = other.numEpochs;
        this.enable3DSegmentation = other.enable3DSegmentation;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.diameter = new OptionalDoubleParameter(other.diameter);
        this.trainSizeModel = other.trainSizeModel;
        this.labelDataAnnotation = new DataAnnotationQueryExpression(other.labelDataAnnotation);
        this.clearLabelDataAnnotation = other.clearLabelDataAnnotation;
        this.saveEvery = new OptionalIntegerParameter(other.saveEvery);
        this.modelNameOut = other.modelNameOut;

        registerSubParameter(gpuSettings);
        registerSubParameter(tweaksSettings);

        updateSlots();
    }

    @Override
    public void applyProjectUpgrade(String fromVersion, JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.applyProjectUpgrade(fromVersion, context, report);

        // Coming from older JIPipe versions we will disable label clearing to ensure the same behavior
        if (VersionUtils.isOlderThanOrEqual(fromVersion, "5.3.0")) {
            clearLabelDataAnnotation = false;
        }
    }

    @SetJIPipeDocumentation(name = "Remove label data annotation from outputs", description = "If enabled, outputs will not have the label data annotation, " +
            "which is often not needed anymore at this stage. Other data annotations will be left alone.")
    @JIPipeParameter("clear-label-data-annotation")
    public boolean isClearLabelDataAnnotation() {
        return clearLabelDataAnnotation;
    }

    @JIPipeParameter("clear-label-data-annotation")
    public void setClearLabelDataAnnotation(boolean clearLabelDataAnnotation) {
        this.clearLabelDataAnnotation = clearLabelDataAnnotation;
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

    private void updateSlots() {
        toggleSlot(OUTPUT_SIZE_MODEL, trainSizeModel);
    }

    @SetJIPipeDocumentation(name = "Train size model", description = "If enabled, also train a size model")
    @JIPipeParameter("train-size-model")
    public boolean isTrainSizeModel() {
        return trainSizeModel;
    }

    @JIPipeParameter("train-size-model")
    public void setTrainSizeModel(boolean trainSizeModel) {
        this.trainSizeModel = trainSizeModel;
        updateSlots();
    }

    @SetJIPipeDocumentation(name = "Mean diameter", description = "The cell diameter. Depending on the model, you can choose following values: " +
            "<ul>" +
            "<li><b>Cytoplasm</b>: You need to rescale all your images that structures have a diameter of about 30 pixels.</li>" +
            "<li><b>Nuclei</b>: You need to rescale all your images that structures have a diameter of about 17 pixels.</li>" +
            "<li><b>Custom</b>: You need to rescale all your images that structures have a diameter appropriate for the model.</li>" +
            "<li><b>None</b>: This will train from scratch. You can freely set the diameter. You also can set the diameter to 0 to disable scaling.</li>" +
            "</ul>")
    @JIPipeParameter(value = "diameter", important = true)
    public OptionalDoubleParameter getDiameter() {
        return diameter;
    }

    @JIPipeParameter("diameter")
    public void setDiameter(OptionalDoubleParameter diameter) {
        this.diameter = diameter;
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

    @SetJIPipeDocumentation(name = "Enable 3D segmentation", description = "If enabled, Cellpose will train in 3D. " +
            "Otherwise, JIPipe will prepare the data by splitting 3D data into planes.")
    @JIPipeParameter(value = "enable-3d-segmentation", important = true)
    public boolean isEnable3DSegmentation() {
        return enable3DSegmentation;
    }

    @JIPipeParameter("enable-3d-segmentation")
    public void setEnable3DSegmentation(boolean enable3DSegmentation) {
        this.enable3DSegmentation = enable3DSegmentation;
    }

    @SetJIPipeDocumentation(name = "Label data annotation", description = "Determines which data annotation contains the labels. Please ensure that " +
            "the appropriate label data is annotated to the raw input data.")
    @JIPipeParameter("label-data-annotation")
    public DataAnnotationQueryExpression getLabelDataAnnotation() {
        return labelDataAnnotation;
    }

    @JIPipeParameter("label-data-annotation")
    public void setLabelDataAnnotation(DataAnnotationQueryExpression labelDataAnnotation) {
        this.labelDataAnnotation = labelDataAnnotation;
    }

    @SetJIPipeDocumentation(name = "Cellpose: GPU", description = "Controls how the graphics card is utilized.")
    @JIPipeParameter(value = "gpu-settings", collapsed = true, icon = "apps/cellpose.png")
    public Cellpose2GPUSettings getGpuSettings() {
        return gpuSettings;
    }

    @SetJIPipeDocumentation(name = "Cellpose: Tweaks", description = "Advanced settings for the training.")
    @JIPipeParameter(value = "tweaks-settings", collapsed = true, icon = "apps/cellpose.png")
    public Cellpose3SegmentationTrainingTweaksSettings getTweaksSettings() {
        return tweaksSettings;
    }

    @SetJIPipeDocumentation(name = "Save every", description = "Save the model every N epochs. If enabled, the model is saved at the specified interval during training.")
    @JIPipeParameter("save-every")
    public OptionalIntegerParameter getSaveEvery() {
        return saveEvery;
    }

    @JIPipeParameter("save-every")
    public void setSaveEvery(OptionalIntegerParameter saveEvery) {
        this.saveEvery = saveEvery;
    }

    @SetJIPipeDocumentation(name = "Model name output", description = "If set, the trained model will be saved with this name.")
    @JIPipeParameter("model-name-out")
    public String getModelNameOut() {
        return modelNameOut;
    }

    @JIPipeParameter("model-name-out")
    public void setModelNameOut(String modelNameOut) {
        this.modelNameOut = modelNameOut;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        super.reportValidity(reportContext, reportSettings, report, progressInfo);

        // Non-cpsam model warning
        JIPipeInputDataSlot modelSlot = getInputSlot("Pretrained model");
        for (int row = 0; row < modelSlot.getRowCount(); row++) {
            CellposeModelData modelData = modelSlot.getData(row, CellposeModelData.class, progressInfo);
            String modelId = modelData.getPretrainedModelName();
            if (modelId != null && !modelId.equals("cpsam") && !modelId.equals("None")) {
                reportContext.warning()
                        .title("Non-cpsam model for training")
                        .explanation("Only the 'cpsam' model is recommended for training. Using '" + modelId + "' may produce suboptimal results.")
                        .solution("Use the 'cpsam' model as the pretrained starting model.")
                        .report(report);
            }
        }

        // Version check
        JIPipeEnvironmentConfigurationCache configurationCache = new JIPipeEnvironmentConfigurationCache();
        Cellpose4Environment environment = getEnvironment(Cellpose4Environment.class, configurationCache, progressInfo);
        String version = CellposeVersionUtils.getInstalledVersion(environment);
        if (version != null && StringUtils.compareVersions(version, "4.2") < 0) {
            List<ArtifactUpgrade> upgrades = ArtifactUpgradeUtils.findAvailableUpgrades(environment);
            JIPipeValidationReportContext context = new GraphNodeValidationReportContext(this);
            if (!upgrades.isEmpty()) {
                context.warning()
                        .title("Cellpose version may be too old")
                        .explanation("This training node requires Cellpose 4.2 or later, but the current environment uses Cellpose " + version + ".")
                        .solution("Update to Cellpose 4.2 or later by clicking the 'Update Cellpose' button.")
                        .action(new JIPipeNotificationAction("Update Cellpose", "Update to a newer Cellpose version",
                                JIPipe.RESOURCES.getIcon16("actions/list-check.png"),
                                wb -> ArtifactUpgradeUtils.showUpgradeDialog((org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench) wb, upgrades)))
                        .report(report);
            } else {
                context.warning()
                        .title("Cellpose version may be too old")
                        .explanation("This training node requires Cellpose 4.2 or later, but the current environment uses Cellpose " + version + ".")
                        .solution("Please install a newer Cellpose artifact manually.")
                        .report(report);
            }
        }
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // Get environment
        Cellpose4Environment environment = getEnvironment(Cellpose4Environment.class, runContext, progressInfo);

        // Runtime version guard
        String version = CellposeVersionUtils.getInstalledVersion(environment);
        if (version != null && StringUtils.compareVersions(version, "4.2") < 0) {
            List<ArtifactUpgrade> upgrades = ArtifactUpgradeUtils.findAvailableUpgrades(environment);
            GraphNodeValidationReportContext context = new GraphNodeValidationReportContext(this);
            if (!upgrades.isEmpty()) {
                throw new JIPipeValidationRuntimeException(context.error()
                        .title("Cellpose version too old for training")
                        .explanation("This training node requires Cellpose 4.2 or later, but the current environment uses Cellpose " + version + ". Execution was aborted.")
                        .solution("Update to Cellpose 4.2 or later.")
                        .action(new JIPipeNotificationAction("Update Cellpose", "Update to a newer Cellpose version",
                                JIPipe.RESOURCES.getIcon16("actions/list-check.png"),
                                wb -> ArtifactUpgradeUtils.showUpgradeDialog((org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench) wb, upgrades)))
                        .build());
            } else {
                throw new JIPipeValidationRuntimeException(context.error()
                        .title("Cellpose version too old for training")
                        .explanation("This training node requires Cellpose 4.2 or later, but the current environment uses Cellpose " + version + ". Execution was aborted.")
                        .solution("Please install a newer Cellpose artifact manually.")
                        .build());
            }
        }

        // Prepare folders
        Path workDirectory = getNewScratch();
        progressInfo.log("Work directory is " + workDirectory);

        progressInfo.log("Collecting models ...");
        List<CellposeModelInfo> modelInfos = new ArrayList<>();
        JIPipeInputDataSlot modelSlot = getInputSlot("Pretrained model");
        for (int modelRow : iterationStep.getInputRows(modelSlot)) {
            JIPipeProgressInfo modelProgress = progressInfo.resolve("Model row " + modelRow);
            CellposeModelData modelData = modelSlot.getData(modelRow, CellposeModelData.class, modelProgress);

            // Save the model out
            CellposeModelInfo modelInfo = CellposeUtils.createModelInfo(modelSlot.getTextAnnotations(modelRow), modelData, workDirectory, modelProgress);
            modelInfos.add(modelInfo);
        }

        // Aggressive error for non-cpsam models
        for (CellposeModelInfo modelInfo : modelInfos) {
            if (modelInfo.getModelNameOrPath() != null && !modelInfo.getModelNameOrPath().equals("cpsam") && !modelInfo.getModelNameOrPath().equals("None")) {
                progressInfo.aggressiveError("WARNING: Only the 'cpsam' model is recommended for training. Using '" + modelInfo.getModelNameOrPath() + "' may produce suboptimal results.");
            }
        }

        if (clearLabelDataAnnotation) {
            progressInfo.warn("Clearing label data annotation '" + labelDataAnnotation.getExpression() + "' as requested.");
            Map<String, JIPipeDataAnnotation> mergedDataAnnotations = iterationStep.getMergedDataAnnotations();
            JIPipeDataAnnotation queried = labelDataAnnotation.queryFirst(mergedDataAnnotations.values());
            if (queried != null) {
                mergedDataAnnotations.remove(queried.getName());
            }
        }

        for (int i = 0; i < modelInfos.size(); i++) {
            CellposeModelInfo modelInfo = modelInfos.get(i);
            JIPipeProgressInfo modelProgress = progressInfo.resolve("Model", i, modelInfos.size());
            processModel(PathUtils.createTempSubDirectory(workDirectory, "run"), environment, modelInfo, iterationStep, runContext, modelProgress);
        }


        if (cleanUpAfterwards) {
            PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
        }
    }

    private void processModel(Path workDirectory, Cellpose4Environment environment, CellposeModelInfo modelInfo, JIPipeMultiIterationStep iterationStep, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path trainingDir = workDirectory.resolve("training");
        Path testDir = workDirectory.resolve("test");
        try {
            Files.createDirectories(trainingDir);
            Files.createDirectories(testDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Extract images
        JIPipeProgressInfo extractProgress = progressInfo.resolveAndLog("Extract input images");
        boolean dataIs3D = false;
        AtomicInteger imageCounter = new AtomicInteger(0);
        for (Integer row : iterationStep.getInputRows("Training data")) {
            JIPipeProgressInfo rowProgress = extractProgress.resolveAndLog("Row " + row);
            ImagePlus raw = getInputSlot("Training data")
                    .getData(row, ImagePlusData.class, rowProgress).getImage();
            ImagePlus mask = labelDataAnnotation.queryFirst(getInputSlot("Training data").getDataAnnotations(row))
                    .getData(ImagePlus3DGreyscale16UData.class, progressInfo).getImage();
            mask = ImageJUtils.ensureEqualSize(mask, raw, true);
            if (tweaksSettings.isGenerateConnectedComponents())
                mask = applyConnectedComponents(mask, runContext, rowProgress.resolveAndLog("Connected components"));
            dataIs3D |= raw.getNDimensions() > 2 && enable3DSegmentation;

            saveImagesToPath(trainingDir, imageCounter, rowProgress, raw, mask);
        }
        for (Integer row : iterationStep.getInputRows("Test data")) {
            JIPipeProgressInfo rowProgress = extractProgress.resolveAndLog("Row " + row);
            ImagePlus raw = getInputSlot("Test data")
                    .getData(row, ImagePlusData.class, rowProgress).getImage();
            ImagePlus mask = labelDataAnnotation.queryFirst(getInputSlot("Test data").getDataAnnotations(row))
                    .getData(ImagePlus3DGreyscale16UData.class, progressInfo).getImage();
            if (tweaksSettings.isGenerateConnectedComponents())
                mask = applyConnectedComponents(mask, runContext, rowProgress.resolveAndLog("Connected components"));
            mask = ImageJUtils.ensureEqualSize(mask, raw, true);

            saveImagesToPath(testDir, imageCounter, rowProgress, raw, mask);
        }

        // Setup arguments
        List<String> arguments = new ArrayList<>();
        arguments.add("-m");
        arguments.add("cellpose");

        arguments.add("--verbose");

        arguments.add("--train");

        arguments.add("--dir");
        arguments.add(trainingDir.toAbsolutePath().toString());

        if (!iterationStep.getInputRows("Test data").isEmpty()) {
            arguments.add("--test_dir");
            arguments.add(testDir.toAbsolutePath().toString());
        }

        arguments.add("--img_filter");
        arguments.add("raw");

        arguments.add("--mask_filter");
        arguments.add("masks");

        // GPU
        if (gpuSettings.isEnableGPU())
            arguments.add("--use_gpu");
        if (gpuSettings.getGpuDevice().isEnabled()) {
            arguments.add("--gpu_device");
            arguments.add(gpuSettings.getGpuDevice().getContent() + "");
        }
        if (dataIs3D)
            arguments.add("--do_3D");
        if (diameter.isEnabled()) {
            arguments.add("--diameter");
            arguments.add(diameter.getContent() + "");
        }

        arguments.add("--bsize");
        arguments.add("256");

        if (modelInfo.getModelNameOrPath() != null) {
            arguments.add("--pretrained_model");
            arguments.add(modelInfo.getModelNameOrPath());
        } else {
            arguments.add("--pretrained_model");
            arguments.add("None");
        }

        arguments.add("--learning_rate");
        arguments.add(tweaksSettings.getLearningRate() + "");

        arguments.add("--weight_decay");
        arguments.add(tweaksSettings.getWeightDecay() + "");

        arguments.add("--n_epochs");
        arguments.add(numEpochs + "");

        arguments.add("--batch_size");
        arguments.add(tweaksSettings.getBatchSize() + "");

        arguments.add("--min_train_masks");
        arguments.add(tweaksSettings.getMinTrainMasks() + "");

        if (tweaksSettings.getNumTrainingImagesPerEpoch().isEnabled()) {
            arguments.add("--nimg_per_epoch");
            arguments.add(tweaksSettings.getNumTrainingImagesPerEpoch().getContent() + "");
        }

        if (tweaksSettings.getNumTestImagesPerEpoch().isEnabled()) {
            arguments.add("--nimg_test_per_epoch");
            arguments.add(tweaksSettings.getNumTestImagesPerEpoch().getContent() + "");
        }

        if (saveEvery.isEnabled()) {
            arguments.add("--save_every");
            arguments.add(saveEvery.getContent() + "");
        }

        if (!modelNameOut.isEmpty()) {
            arguments.add("--model_name_out");
            arguments.add(modelNameOut);
        }

        // Run the module
        CellposeUtils.runCellpose(environment,
                arguments,
                suppressLogs,
                progressInfo);

        // Collect annotations
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>(modelInfo.getAnnotationList());

        // Extract the model
        Path modelsPath = trainingDir.resolve("models");
        Path generatedModelFile = findModelFile(modelsPath);
        CellposeModelData modelData = new CellposeModelData(generatedModelFile);
        iterationStep.addOutputData("Model", modelData, annotationList, JIPipeTextAnnotationMergeMode.Merge, progressInfo);

        // Extract size model
        if (trainSizeModel) {
            Path generatedSizeModelFile = findSizeModelFile(modelsPath);
            CellposeSizeModelData sizeModelData = new CellposeSizeModelData(generatedSizeModelFile);
            iterationStep.addOutputData(OUTPUT_SIZE_MODEL.getName(), sizeModelData, annotationList, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    private ImagePlus applyConnectedComponents(ImagePlus mask, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Apply MorphoLibJ connected components labeling (8-connectivity, 16-bit) to " + mask);
        if (enable3DSegmentation) {
            ConnectedComponentsLabeling3DAlgorithm algorithm = JIPipe.createNode(ConnectedComponentsLabeling3DAlgorithm.class);
            algorithm.setConnectivity(Neighborhood3D.TwentySixConnected);
            algorithm.setOutputType(new JIPipeDataInfoRef(ImagePlusGreyscale16UData.class));
            algorithm.getFirstInputSlot().addData(new ImagePlus3DGreyscaleMaskData(mask), progressInfo);
            algorithm.run(runContext, progressInfo);
            return algorithm.getFirstOutputSlot().getData(0, ImagePlusGreyscale16UData.class, progressInfo).getImage();
        } else {
            ConnectedComponentsLabeling2DAlgorithm algorithm = JIPipe.createNode(ConnectedComponentsLabeling2DAlgorithm.class);
            algorithm.setConnectivity(Neighborhood2D.EightConnected);
            algorithm.setOutputType(new JIPipeDataInfoRef(ImagePlusGreyscale16UData.class));
            algorithm.getFirstInputSlot().addData(new ImagePlus3DGreyscaleMaskData(mask), progressInfo);
            algorithm.run(runContext, progressInfo);
            return algorithm.getFirstOutputSlot().getData(0, ImagePlusGreyscale16UData.class, progressInfo).getImage();
        }
    }

    private Path findModelFile(Path modelsPath) {
        for (Path path : PathUtils.findFilesByExtensionIn(modelsPath).stream().sorted(Comparator.comparing(path -> {
            try {
                return Files.getLastModifiedTime((Path) path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).reversed()).collect(Collectors.toList())) {
            String name = path.getFileName().toString();
            if (!name.startsWith("cellpose"))
                continue;
            if (!name.endsWith(".npy")) {
                return path;
            }
        }
        throw new RuntimeException("Could not find model in " + modelsPath);
    }

    private Path findSizeModelFile(Path modelsPath) {
        List<Path> list = PathUtils.findFilesByExtensionIn(modelsPath, ".npy").stream().sorted(Comparator.comparing(path -> {
            try {
                return Files.getLastModifiedTime((Path) path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).reversed()).collect(Collectors.toList());
        return list.get(0);
    }

    private void saveImagesToPath(Path dir, AtomicInteger imageCounter, JIPipeProgressInfo rowProgress, ImagePlus image, ImagePlus mask) {
        if (image.getStackSize() > 1 && !enable3DSegmentation) {
            ImageJIterationUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                ImageProcessor maskSlice = ImageJUtils.getSliceZero(mask, index);
                ImagePlus maskSliceImage = new ImagePlus("slice", maskSlice);
                ImagePlus imageSliceImage = new ImagePlus("slice", ip);
                Path imageFile = dir.resolve("i" + imageCounter + "_raw.tif");
                Path maskFile = dir.resolve("i" + imageCounter + "_masks.tif");
                imageCounter.getAndIncrement();
                IJ.saveAs(imageSliceImage, "TIFF", imageFile.toString());
                IJ.saveAs(maskSliceImage, "TIFF", maskFile.toString());
            }, rowProgress);
        } else {
            // Save as-is
            Path imageFile = dir.resolve("i" + imageCounter + "_raw.tif");
            Path maskFile = dir.resolve("i" + imageCounter + "_masks.tif");
            imageCounter.getAndIncrement();
            IJ.saveAs(image, "TIFF", imageFile.toString());
            IJ.saveAs(mask, "TIFF", maskFile.toString());
        }
    }

    @SetJIPipeDocumentation(name = "Epochs", description = "Number of epochs that should be trained.")
    @JIPipeParameter("epochs")
    public int getNumEpochs() {
        return numEpochs;
    }

    @JIPipeParameter("epochs")
    public void setNumEpochs(int numEpochs) {
        this.numEpochs = numEpochs;
    }
}
