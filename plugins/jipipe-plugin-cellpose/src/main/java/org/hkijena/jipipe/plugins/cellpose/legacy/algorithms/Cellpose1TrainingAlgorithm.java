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

package org.hkijena.jipipe.plugins.cellpose.legacy.algorithms;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
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
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.cellpose.Cellpose2PluginApplicationSettings;
import org.hkijena.jipipe.plugins.cellpose.legacy.PretrainedLegacyCellpose2TrainingModel;
import org.hkijena.jipipe.plugins.cellpose.legacy.datatypes.LegacyCellposeModelData;
import org.hkijena.jipipe.plugins.cellpose.legacy.datatypes.LegacyCellposeSizeModelData;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.Cellpose2GPUSettings;
import org.hkijena.jipipe.plugins.expressions.DataAnnotationQueryExpression;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.binary.ConnectedComponentsLabeling2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.binary.ConnectedComponentsLabeling3DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood2D;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood3D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale16UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonUtils;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Deprecated
@SetJIPipeDocumentation(name = "Cellpose training (Deprecated)", description = "Trains a model with Cellpose. You start from an existing model or train from scratch. " +
        "Incoming images are automatically converted to greyscale. Only 2D or 3D images are supported. For this node to work, you need to annotate a greyscale 16-bit or 8-bit label image column to each raw data input. " +
        "To do this, you can use the node 'Annotate with data'. By default, JIPipe will ensure that all connected components of this image are assigned a unique component. You can disable this feature via the parameters.")
@AddJIPipeInputSlot(value = ImagePlus3DData.class, name = "Training data", create = true)
@AddJIPipeInputSlot(value = ImagePlus3DData.class, name = "Test data", create = true, optional = true)
@AddJIPipeInputSlot(value = LegacyCellposeModelData.class)
@AddJIPipeOutputSlot(value = LegacyCellposeModelData.class, name = "Model", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
@LabelAsJIPipeHidden
public class Cellpose1TrainingAlgorithm extends JIPipeSingleIterationAlgorithm {


    private final Cellpose2GPUSettings gpuSettings;
    private PretrainedLegacyCellpose2TrainingModel pretrainedModel = PretrainedLegacyCellpose2TrainingModel.Cytoplasm;
    private int numEpochs = 500;
    private double learningRate = 0.2;
    private double weightDecay = 1e-05;
    private int batchSize = 8;
    private int minTrainMasks = 1;
    private boolean useResidualConnections = true;
    private boolean useStyleVector = true;
    private boolean concatenateDownsampledLayers = false;
    private boolean enable3DSegmentation = true;
    private boolean cleanUpAfterwards = true;
    private int diameter = 30;
    private boolean trainSizeModel = false;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();
    private DataAnnotationQueryExpression labelDataAnnotation = new DataAnnotationQueryExpression("\"Label\"");
    private boolean generateConnectedComponents = true;

    private OptionalIntegerParameter segmentedChannel = new OptionalIntegerParameter(false, 0);

    private OptionalIntegerParameter nuclearChannel = new OptionalIntegerParameter(false, 0);


    public Cellpose1TrainingAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.gpuSettings = new Cellpose2GPUSettings();
        updateSlots();

        registerSubParameter(gpuSettings);
    }

    public Cellpose1TrainingAlgorithm(Cellpose1TrainingAlgorithm other) {
        super(other);

        this.gpuSettings = new Cellpose2GPUSettings(other.gpuSettings);

        this.pretrainedModel = other.pretrainedModel;
        this.numEpochs = other.numEpochs;
        this.learningRate = other.learningRate;
        this.batchSize = other.batchSize;
        this.useResidualConnections = other.useResidualConnections;
        this.useStyleVector = other.useStyleVector;
        this.concatenateDownsampledLayers = other.concatenateDownsampledLayers;
        this.enable3DSegmentation = other.enable3DSegmentation;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.diameter = other.diameter;
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.trainSizeModel = other.trainSizeModel;
        this.labelDataAnnotation = new DataAnnotationQueryExpression(other.labelDataAnnotation);
        this.generateConnectedComponents = other.generateConnectedComponents;
        this.minTrainMasks = other.minTrainMasks;
        this.weightDecay = other.weightDecay;
        this.segmentedChannel = new OptionalIntegerParameter(other.segmentedChannel);
        this.nuclearChannel = new OptionalIntegerParameter(other.nuclearChannel);

        registerSubParameter(gpuSettings);

        updateSlots();
    }

    private void updateSlots() {
        if (pretrainedModel != PretrainedLegacyCellpose2TrainingModel.Custom) {
            if (getInputSlotMap().containsKey("Pretrained model")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.removeInputSlot("Pretrained model", false);
            }
        } else {
            if (!getInputSlotMap().containsKey("Pretrained model")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.addSlot("Pretrained model", new JIPipeDataSlotInfo(LegacyCellposeModelData.class, JIPipeSlotType.Input), false);
            }
        }
        if (!trainSizeModel) {
            if (getOutputSlotMap().containsKey("Size model")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.removeOutputSlot("Size model", false);
            }
        } else {
            if (!getOutputSlotMap().containsKey("Size model")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.addSlot("Size model", new JIPipeDataSlotInfo(LegacyCellposeSizeModelData.class, JIPipeSlotType.Output), false);
            }
        }
    }

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        super.getEnvironmentDependencies(target);
        if (overrideEnvironment.isEnabled()) {
            target.add(overrideEnvironment.getContent());
        } else {
            target.add(Cellpose2PluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment());
        }
    }

    @SetJIPipeDocumentation(name = "Segmented channel", description = "Channel to segment; 0: GRAY, 1: RED, 2: GREEN, 3: BLUE. Default: 0")
    @JIPipeParameter("segmented-channel")
    public OptionalIntegerParameter getSegmentedChannel() {
        return segmentedChannel;
    }

    @JIPipeParameter("segmented-channel")
    public void setSegmentedChannel(OptionalIntegerParameter segmentedChannel) {
        this.segmentedChannel = segmentedChannel;
    }

    @SetJIPipeDocumentation(name = "Nuclear channel", description = "Nuclear channel (only used by certain models); 0: NONE, 1: RED, 2: GREEN, 3: BLUE. Default: 0")
    @JIPipeParameter("nuclear-channel")
    public OptionalIntegerParameter getNuclearChannel() {
        return nuclearChannel;
    }

    @JIPipeParameter("nuclear-channel")
    public void setNuclearChannel(OptionalIntegerParameter nuclearChannel) {
        this.nuclearChannel = nuclearChannel;
    }

    @SetJIPipeDocumentation(name = "Weight decay", description = "The weight decay")
    @JIPipeParameter("weight-decay")
    public double getWeightDecay() {
        return weightDecay;
    }

    @JIPipeParameter("weight-decay")
    public void setWeightDecay(double weightDecay) {
        this.weightDecay = weightDecay;
    }

    @SetJIPipeDocumentation(name = "Generate connected components", description = "If enabled, JIPipe will apply a connected component labeling to the annotated masks. If disabled, Cellpose is provided with " +
            "the labels as-is, which might result in issues with the training.")
    @JIPipeParameter("generate-connected-components")
    public boolean isGenerateConnectedComponents() {
        return generateConnectedComponents;
    }

    @JIPipeParameter("generate-connected-components")
    public void setGenerateConnectedComponents(boolean generateConnectedComponents) {
        this.generateConnectedComponents = generateConnectedComponents;
    }

    @SetJIPipeDocumentation(name = "Minimum number of labels per image", description = "Minimum number of masks an image must have to use in training set. " +
            "This value is by default 5 in the original Cellpose tool.")
    @JIPipeParameter("min-train-masks")
    public int getMinTrainMasks() {
        return minTrainMasks;
    }

    @JIPipeParameter("min-train-masks")
    public void setMinTrainMasks(int minTrainMasks) {
        this.minTrainMasks = minTrainMasks;
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

    @SetJIPipeDocumentation(name = "Learning rate")
    @JIPipeParameter("learning-rate")
    public double getLearningRate() {
        return learningRate;
    }

    @JIPipeParameter("learning-rate")
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    @SetJIPipeDocumentation(name = "Batch size")
    @JIPipeParameter("batch-size")
    public int getBatchSize() {
        return batchSize;
    }

    @JIPipeParameter("batch-size")
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @SetJIPipeDocumentation(name = "Use residual connections")
    @JIPipeParameter("use-residual-connections")
    public boolean isUseResidualConnections() {
        return useResidualConnections;
    }

    @JIPipeParameter("use-residual-connections")
    public void setUseResidualConnections(boolean useResidualConnections) {
        this.useResidualConnections = useResidualConnections;
    }

    @SetJIPipeDocumentation(name = "Use style vector")
    @JIPipeParameter("use-style-vector")
    public boolean isUseStyleVector() {
        return useStyleVector;
    }

    @JIPipeParameter("use-style-vector")
    public void setUseStyleVector(boolean useStyleVector) {
        this.useStyleVector = useStyleVector;
    }

    @SetJIPipeDocumentation(name = "Concatenate downsampled layers",
            description = "Concatenate downsampled layers with upsampled layers (off by default which means they are added)")
    @JIPipeParameter("concatenate-downsampled-layers")
    public boolean isConcatenateDownsampledLayers() {
        return concatenateDownsampledLayers;
    }

    @JIPipeParameter("concatenate-downsampled-layers")
    public void setConcatenateDownsampledLayers(boolean concatenateDownsampledLayers) {
        this.concatenateDownsampledLayers = concatenateDownsampledLayers;
    }

    @SetJIPipeDocumentation(name = "Mean diameter", description = "The cell diameter. Depending on the model, you can choose following values: " +
            "<ul>" +
            "<li><b>Cytoplasm</b>: You need to rescale all your images that structures have a diameter of about 30 pixels.</li>" +
            "<li><b>Nuclei</b>: You need to rescale all your images that structures have a diameter of about 17 pixels.</li>" +
            "<li><b>Custom</b>: You need to rescale all your images that structures have a diameter appropriate for the model.</li>" +
            "<li><b>None</b>: This will train from scratch. You can freely set the diameter. You also can set the diameter to 0 to disable scaling.</li>" +
            "</ul>")
    @JIPipeParameter(value = "diameter", important = true)
    public int getDiameter() {
        return diameter;
    }

    @JIPipeParameter("diameter")
    public void setDiameter(int diameter) {
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

    @SetJIPipeDocumentation(name = "Override Python environment", description = "If enabled, a different Python environment is used for this Node. Otherwise " +
            "the one in the Project > Application settings > Extensions > Cellpose is used.")
    @JIPipeParameter("override-environment")
    public OptionalPythonEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalPythonEnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
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
    @JIPipeParameter(value = "output-parameters", collapsed = true, iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public Cellpose2GPUSettings getGpuSettings() {
        return gpuSettings;
    }

    @SetJIPipeDocumentation(name = "Model", description = "The pretrained model that should be used. You can either choose one of the models " +
            "provided by Cellpose, a custom model, or train from scratch. The pre-trained model has influence on the diameter and how the input images should be prepared:" +
            "<ul>" +
            "<li><b>Cytoplasm</b>: You need to rescale all your images that structures have a diameter of about 30 pixels.</li>" +
            "<li><b>Nuclei</b>: You need to rescale all your images that structures have a diameter of about 17 pixels.</li>" +
            "<li><b>Custom</b>: You need to rescale all your images that structures have a diameter appropriate for the model. Don't forget to set the diameter value.</li>" +
            "<li><b>None</b>: This will train from scratch. You can freely set the diameter. You also can set the diameter to 0 to disable scaling.</li>" +
            "</ul>")
    @JIPipeParameter("pretrained-model")
    public PretrainedLegacyCellpose2TrainingModel getPretrainedModel() {
        return pretrainedModel;
    }

    @JIPipeParameter("pretrained-model")
    public void setPretrainedModel(PretrainedLegacyCellpose2TrainingModel pretrainedModel) {
        this.pretrainedModel = pretrainedModel;

        // Update diameter
        switch (pretrainedModel) {
            case Cytoplasm:
                if (diameter != 30) {
                    ParameterUtils.setParameter(this, "diameter", 30.0);
                }
                break;
            case Nucleus:
                if (diameter != 17) {
                    ParameterUtils.setParameter(this, "diameter", 17.0);
                }
                break;
        }
        updateSlots();
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        // Prepare folders
        Path workDirectory = getNewScratch();
        Path trainingDir = workDirectory.resolve("training");
        Path testDir = workDirectory.resolve("test");
        try {
            Files.createDirectories(trainingDir);
            Files.createDirectories(testDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        progressInfo.log("Work directory is " + workDirectory);

        // Extract images
        JIPipeProgressInfo extractProgress = progressInfo.resolveAndLog("Extract input images");
        boolean dataIs3D = false;
        AtomicInteger imageCounter = new AtomicInteger(0);
        for (Integer row : iterationStep.getInputRows("Training data")) {
            JIPipeProgressInfo rowProgress = extractProgress.resolveAndLog("Row " + row);
            ImagePlus raw = getInputSlot("Training data")
                    .getData(row, ImagePlus3DData.class, rowProgress).getImage();
            ImagePlus mask = labelDataAnnotation.queryFirst(getInputSlot("Training data").getDataAnnotations(row))
                    .getData(ImagePlus3DGreyscale16UData.class, progressInfo).getImage();
            mask = ImageJUtils.ensureEqualSize(mask, raw, true);
            if (generateConnectedComponents)
                mask = applyConnectedComponents(mask, runContext, rowProgress.resolveAndLog("Connected components"));
            dataIs3D |= raw.getNDimensions() > 2 && enable3DSegmentation;

            saveImagesToPath(trainingDir, imageCounter, rowProgress, raw, mask);
        }
        for (Integer row : iterationStep.getInputRows("Test data")) {
            JIPipeProgressInfo rowProgress = extractProgress.resolveAndLog("Row " + row);
            ImagePlus raw = getInputSlot("Test data")
                    .getData(row, ImagePlus3DData.class, rowProgress).getImage();
            ImagePlus mask = labelDataAnnotation.queryFirst(getInputSlot("Test data").getDataAnnotations(row))
                    .getData(ImagePlus3DGreyscale16UData.class, progressInfo).getImage();
            if (generateConnectedComponents)
                mask = applyConnectedComponents(mask, runContext, rowProgress.resolveAndLog("Connected components"));
            mask = ImageJUtils.ensureEqualSize(mask, raw, true);

            saveImagesToPath(testDir, imageCounter, rowProgress, raw, mask);
        }

        // Extract model if custom
        Path customModelPath = null;
        if (pretrainedModel == PretrainedLegacyCellpose2TrainingModel.Custom) {
            Set<Integer> pretrainedModelRows = iterationStep.getInputRows("Pretrained model");
            if (pretrainedModelRows.size() != 1) {
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new GraphNodeValidationReportContext(this),
                        "Only one pretrained model is allowed",
                        "You can only provide one pretrained model per iteration step for training.",
                        "Ensure that only one pretrained model is in a iteration step."));
            }
            LegacyCellposeModelData modelData = iterationStep.getInputData("Pretrained model", LegacyCellposeModelData.class, progressInfo).get(0);
            customModelPath = workDirectory.resolve(modelData.getName());
            try {
                Files.write(customModelPath, modelData.getData());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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

        // Channels
        if (segmentedChannel.isEnabled()) {
            arguments.add("--chan");
            arguments.add(segmentedChannel.getContent() + "");
        } else {
            arguments.add("--chan");
            arguments.add("0");
        }
        if (nuclearChannel.isEnabled()) {
            arguments.add("--chan2");
            arguments.add(nuclearChannel.getContent() + "");
        }

        // GPU
        if (gpuSettings.isEnableGPU())
            arguments.add("--use_gpu");
        if (gpuSettings.getGpuDevice().isEnabled()) {
            arguments.add("--gpu_device");
            arguments.add(gpuSettings.getGpuDevice().getContent() + "");
        }
        if (dataIs3D)
            arguments.add("--do_3D");
        if (pretrainedModel == PretrainedLegacyCellpose2TrainingModel.Custom || pretrainedModel == PretrainedLegacyCellpose2TrainingModel.None) {
            arguments.add("--diameter");
            arguments.add(diameter + "");
            arguments.add("--diam_mean");
            arguments.add(diameter + "");
        }

        switch (pretrainedModel) {
            case Custom:
                arguments.add("--pretrained_model");
                arguments.add(customModelPath.toAbsolutePath().toString());
                break;
            case None:
                arguments.add("--pretrained_model");
                arguments.add("None");
                break;
            default:
                arguments.add("--pretrained_model");
                arguments.add(pretrainedModel.getId());
                break;
        }

        if (trainSizeModel)
            arguments.add("--train_size");

        arguments.add("--learning_rate");
        arguments.add(learningRate + "");

        arguments.add("--weight_decay");
        arguments.add(weightDecay + "");

        arguments.add("--n_epochs");
        arguments.add(numEpochs + "");

        arguments.add("--batch_size");
        arguments.add(batchSize + "");

        arguments.add("--residual_on");
        arguments.add(useResidualConnections ? "1" : "0");

        arguments.add("--style_on");
        arguments.add(useStyleVector ? "1" : "0");

        arguments.add("--concatenation");
        arguments.add(concatenateDownsampledLayers ? "1" : "0");

        arguments.add("--min_train_masks");
        arguments.add(minTrainMasks + "");

        // Run the module
        PythonUtils.runPython(arguments.toArray(new String[0]), overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() :
                Cellpose2PluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment(), Collections.emptyList(), Collections.emptyMap(), false, false, progressInfo);

        // Extract the model
        Path modelsPath = trainingDir.resolve("models");
        Path generatedModelFile = findModelFile(modelsPath);
        LegacyCellposeModelData modelData = new LegacyCellposeModelData(generatedModelFile);
        iterationStep.addOutputData("Model", modelData, progressInfo);

        // Extract size model
        if (trainSizeModel) {
            Path generatedSizeModelFile = findSizeModelFile(modelsPath);
            LegacyCellposeSizeModelData sizeModelData = new LegacyCellposeSizeModelData(generatedSizeModelFile);
            iterationStep.addOutputData("Size model", sizeModelData, progressInfo);
        }

        if (cleanUpAfterwards) {
            PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
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
                IJ.saveAs(maskSliceImage, "TIFF", imageFile.toString());
                IJ.saveAs(imageSliceImage, "TIFF", maskFile.toString());
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
