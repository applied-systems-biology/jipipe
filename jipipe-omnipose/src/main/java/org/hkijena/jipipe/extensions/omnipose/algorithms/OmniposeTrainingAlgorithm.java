package org.hkijena.jipipe.extensions.omnipose.algorithms;

import com.fasterxml.jackson.databind.JsonNode;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.causes.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellposeSizeModelData;
import org.hkijena.jipipe.extensions.cellpose.parameters.CellposeChannelSettings;
import org.hkijena.jipipe.extensions.cellpose.parameters.CellposeGPUSettings;
import org.hkijena.jipipe.extensions.expressions.DataAnnotationQueryExpression;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.Neighborhood2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.Neighborhood3D;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.binary.ConnectedComponentsLabeling2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.binary.ConnectedComponentsLabeling3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.omnipose.OmniposeExtension;
import org.hkijena.jipipe.extensions.omnipose.OmniposePretrainedModel;
import org.hkijena.jipipe.extensions.omnipose.OmniposeSettings;
import org.hkijena.jipipe.extensions.omnipose.parameters.OmniposeTrainingTweaksSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Omnipose training", description = "Trains a model with Omnipose. You start from an existing model or train from scratch. " +
        "Incoming images are automatically converted to greyscale. Only 2D or 3D images are supported. For this node to work, you need to annotate a greyscale 16-bit or 8-bit label image column to each raw data input. " +
        "To do this, you can use the node 'Annotate with data'. By default, JIPipe will ensure that all connected components of this image are assigned a unique component. You can disable this feature via the parameters.")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Training data", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Test data", autoCreate = true, optional = true)
@JIPipeInputSlot(value = CellposeModelData.class)
@JIPipeOutputSlot(value = CellposeModelData.class, slotName = "Model", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
public class OmniposeTrainingAlgorithm extends JIPipeSingleIterationAlgorithm {

    public static final JIPipeDataSlotInfo INPUT_PRETRAINED_MODEL = new JIPipeDataSlotInfo(CellposeModelData.class, JIPipeSlotType.Input, "Pretrained Model", "A custom pretrained model");

    public static final JIPipeDataSlotInfo OUTPUT_SIZE_MODEL = new JIPipeDataSlotInfo(CellposeSizeModelData.class, JIPipeSlotType.Output, "Size Model", "Generated size model", true);
    private final CellposeGPUSettings gpuSettings;
    private final OmniposeTrainingTweaksSettings tweaksSettings;
    private final CellposeChannelSettings channelSettings;
    private OmniposePretrainedModel pretrainedModel = OmniposePretrainedModel.BactOmni;
    private int numEpochs = 500;
    private boolean enable3DSegmentation = true;
    private boolean cleanUpAfterwards = true;
    private OptionalDoubleParameter diameter = new OptionalDoubleParameter(30, false);
    private boolean trainSizeModel = false;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();
    private DataAnnotationQueryExpression labelDataAnnotation = new DataAnnotationQueryExpression("Label");

    public OmniposeTrainingAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.gpuSettings = new CellposeGPUSettings();
        this.tweaksSettings = new OmniposeTrainingTweaksSettings();
        this.channelSettings = new CellposeChannelSettings();
        updateSlots();

        registerSubParameter(gpuSettings);
        registerSubParameter(tweaksSettings);
        registerSubParameter(channelSettings);
    }

    public OmniposeTrainingAlgorithm(OmniposeTrainingAlgorithm other) {
        super(other);

        this.gpuSettings = new CellposeGPUSettings(other.gpuSettings);
        this.tweaksSettings = new OmniposeTrainingTweaksSettings(other.tweaksSettings);
        this.channelSettings = new CellposeChannelSettings(other.channelSettings);

        this.pretrainedModel = other.pretrainedModel;
        this.numEpochs = other.numEpochs;
        this.enable3DSegmentation = other.enable3DSegmentation;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.diameter = new OptionalDoubleParameter(other.diameter);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.trainSizeModel = other.trainSizeModel;
        this.labelDataAnnotation = new DataAnnotationQueryExpression(other.labelDataAnnotation);

        registerSubParameter(gpuSettings);
        registerSubParameter(tweaksSettings);
        registerSubParameter(channelSettings);

        updateSlots();
    }

    private void updateSlots() {
        toggleSlot(INPUT_PRETRAINED_MODEL, pretrainedModel == OmniposePretrainedModel.Custom);
        toggleSlot(OUTPUT_SIZE_MODEL, trainSizeModel);
    }

    @JIPipeDocumentation(name = "Train size model", description = "If enabled, also train a size model")
    @JIPipeParameter("train-size-model")
    public boolean isTrainSizeModel() {
        return trainSizeModel;
    }

    @JIPipeParameter("train-size-model")
    public void setTrainSizeModel(boolean trainSizeModel) {
        this.trainSizeModel = trainSizeModel;
        updateSlots();
    }

    @JIPipeDocumentation(name = "Mean diameter", description = "The cell diameter. Depending on the model, you can choose following values: " +
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

    @JIPipeDocumentation(name = "Override Python environment", description = "If enabled, a different Python environment is used for this Node. Otherwise " +
            "the one in the Project > Application settings > Extensions > Omnipose is used.")
    @JIPipeParameter("override-environment")
    public OptionalPythonEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalPythonEnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
    }

    @JIPipeDocumentation(name = "Enable 3D segmentation", description = "If enabled, Omnipose will train in 3D. " +
            "Otherwise, JIPipe will prepare the data by splitting 3D data into planes.")
    @JIPipeParameter(value = "enable-3d-segmentation", important = true)
    public boolean isEnable3DSegmentation() {
        return enable3DSegmentation;
    }

    @JIPipeParameter("enable-3d-segmentation")
    public void setEnable3DSegmentation(boolean enable3DSegmentation) {
        this.enable3DSegmentation = enable3DSegmentation;
    }

    @JIPipeDocumentation(name = "Label data annotation", description = "Determines which data annotation contains the labels. Please ensure that " +
            "the appropriate label data is annotated to the raw input data.")
    @JIPipeParameter("label-data-annotation")
    public DataAnnotationQueryExpression getLabelDataAnnotation() {
        return labelDataAnnotation;
    }

    @JIPipeParameter("label-data-annotation")
    public void setLabelDataAnnotation(DataAnnotationQueryExpression labelDataAnnotation) {
        this.labelDataAnnotation = labelDataAnnotation;
    }

    @JIPipeDocumentation(name = "Omnipose: GPU", description = "Controls how the graphics card is utilized.")
    @JIPipeParameter(value = "gpu-settings", collapsed = true, resourceClass = OmniposeExtension.class, iconURL = "/org/hkijena/jipipe/extensions/omnipose/icons/omnipose.png")
    public CellposeGPUSettings getGpuSettings() {
        return gpuSettings;
    }

    @JIPipeDocumentation(name = "Omnipose: Tweaks", description = "Advanced settings for the training.")
    @JIPipeParameter(value = "tweaks-settings", collapsed = true, resourceClass = OmniposeExtension.class, iconURL = "/org/hkijena/jipipe/extensions/omnipose/icons/omnipose.png")
    public OmniposeTrainingTweaksSettings getTweaksSettings() {
        return tweaksSettings;
    }

    @JIPipeDocumentation(name = "Omnipose: Channels", description = "Determines which channels are used for the segmentation")
    @JIPipeParameter(value = "channel-parameters", resourceClass = OmniposeExtension.class, iconURL = "/org/hkijena/jipipe/extensions/omnipose/icons/omnipose.png")
    public CellposeChannelSettings getChannelSettings() {
        return channelSettings;
    }

    @JIPipeDocumentation(name = "Model", description = "The pretrained model that should be used. You can either choose one of the models " +
            "provided by Omnipose, a custom model, or train from scratch. The pre-trained model has influence on the diameter and how the input images should be prepared:" +
            "<ul>" +
            "<li><b>Cytoplasm</b>: You need to rescale all your images that structures have a diameter of about 30 pixels.</li>" +
            "<li><b>Nuclei</b>: You need to rescale all your images that structures have a diameter of about 17 pixels.</li>" +
            "<li><b>Custom</b>: You need to rescale all your images that structures have a diameter appropriate for the model. Don't forget to set the diameter value.</li>" +
            "<li><b>None</b>: This will train from scratch. You can freely set the diameter. You also can set the diameter to 0 to disable scaling.</li>" +
            "</ul>")
    @JIPipeParameter("pretrained-model")
    public OmniposePretrainedModel getPretrainedModel() {
        return pretrainedModel;
    }

    @JIPipeParameter("pretrained-model")
    public void setPretrainedModel(OmniposePretrainedModel pretrainedModel) {
        this.pretrainedModel = pretrainedModel;

        // Update diameter
//        switch (pretrainedModel) {
//            case Cytoplasm:
//                if (diameter.getContent() != 30) {
//                    ParameterUtils.setParameter(this, "diameter", 30.0);
//                }
//                break;
//            case Nucleus:
//                if (diameter.getContent() != 17) {
//                    ParameterUtils.setParameter(this, "diameter", 17.0);
//                }
//                break;
//        }
        updateSlots();
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

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
        for (Integer row : dataBatch.getInputRows("Training data")) {
            JIPipeProgressInfo rowProgress = extractProgress.resolveAndLog("Row " + row);
            ImagePlus raw = getInputSlot("Training data")
                    .getData(row, ImagePlusData.class, rowProgress).getImage();
            ImagePlus mask = labelDataAnnotation.queryFirst(getInputSlot("Training data").getDataAnnotations(row))
                    .getData(ImagePlus3DGreyscale16UData.class, progressInfo).getImage();
            mask = ImageJUtils.ensureEqualSize(mask, raw, true);
            if (tweaksSettings.isGenerateConnectedComponents())
                mask = applyConnectedComponents(mask, rowProgress.resolveAndLog("Connected components"));
            dataIs3D |= raw.getNDimensions() > 2 && enable3DSegmentation;

            saveImagesToPath(trainingDir, imageCounter, rowProgress, raw, mask);
        }
        for (Integer row : dataBatch.getInputRows("Test data")) {
            JIPipeProgressInfo rowProgress = extractProgress.resolveAndLog("Row " + row);
            ImagePlus raw = getInputSlot("Test data")
                    .getData(row, ImagePlusData.class, rowProgress).getImage();
            ImagePlus mask = labelDataAnnotation.queryFirst(getInputSlot("Test data").getDataAnnotations(row))
                    .getData(ImagePlus3DGreyscale16UData.class, progressInfo).getImage();
            if (tweaksSettings.isGenerateConnectedComponents())
                mask = applyConnectedComponents(mask, rowProgress.resolveAndLog("Connected components"));
            mask = ImageJUtils.ensureEqualSize(mask, raw, true);

            saveImagesToPath(testDir, imageCounter, rowProgress, raw, mask);
        }

        // Extract model if custom
        Path customModelPath = null;
        if (pretrainedModel == OmniposePretrainedModel.Custom) {
            Set<Integer> pretrainedModelRows = dataBatch.getInputRows(INPUT_PRETRAINED_MODEL.getName());
            if (pretrainedModelRows.size() != 1) {
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new GraphNodeValidationReportContext(this),
                        "Only one pretrained model is allowed",
                        "You can only provide one pretrained model per data batch for training.",
                        "Ensure that only one pretrained model is in a data batch."));
            }
            CellposeModelData modelData = dataBatch.getInputData(INPUT_PRETRAINED_MODEL.getName(), CellposeModelData.class, progressInfo).get(0);
            customModelPath = workDirectory.resolve(modelData.getName());
            try {
                Files.write(customModelPath, modelData.getData());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Setup arguments
        List<String> arguments = new ArrayList<>();
        Map<String, String> envVars = new HashMap<>();

        arguments.add("-m");
        arguments.add("cellpose");

        // Activate Omnipose
        arguments.add("--omni");

        arguments.add("--verbose");

        arguments.add("--train");

        arguments.add("--dir");
        arguments.add(trainingDir.toAbsolutePath().toString());

        if (!dataBatch.getInputRows("Test data").isEmpty()) {
            arguments.add("--test_dir");
            arguments.add(testDir.toAbsolutePath().toString());
        }

        arguments.add("--img_filter");
        arguments.add("raw");

        arguments.add("--mask_filter");
        arguments.add("masks");

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

        // GPU
        if (gpuSettings.isEnableGPU()) {
            arguments.add("--use_gpu");
            if (gpuSettings.getGpuDevice().isEnabled()) {
                envVars.put("CUDA_VISIBLE_DEVICES", gpuSettings.getGpuDevice().getContent().toString());
            }
        }
        if (dataIs3D)
            arguments.add("--do_3D");
        if (diameter.isEnabled()) {
            arguments.add("--diameter");
            arguments.add(diameter.getContent() + "");
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
        arguments.add(tweaksSettings.getLearningRate() + "");

        arguments.add("--n_epochs");
        arguments.add(numEpochs + "");

        arguments.add("--batch_size");
        arguments.add(tweaksSettings.getBatchSize() + "");

        arguments.add("--residual_on");
        arguments.add(tweaksSettings.isUseResidualConnections() ? "1" : "0");

        arguments.add("--style_on");
        arguments.add(tweaksSettings.isUseStyleVector() ? "1" : "0");

        arguments.add("--concatenation");
        arguments.add(tweaksSettings.isConcatenateDownsampledLayers() ? "1" : "0");

        arguments.add("--min_train_masks");
        arguments.add(tweaksSettings.getMinTrainMasks() + "");

        // Run the module
        PythonUtils.runPython(arguments.toArray(new String[0]), overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() :
                OmniposeSettings.getInstance().getPythonEnvironment(), Collections.emptyList(), envVars, progressInfo);

        // Extract the model
        Path modelsPath = trainingDir.resolve("models");
        Path generatedModelFile = findModelFile(modelsPath);
        CellposeModelData modelData = new CellposeModelData(generatedModelFile);
        dataBatch.addOutputData("Model", modelData, progressInfo);

        // Extract size model
        if (trainSizeModel) {
            Path generatedSizeModelFile = findSizeModelFile(modelsPath);
            CellposeSizeModelData sizeModelData = new CellposeSizeModelData(generatedSizeModelFile);
            dataBatch.addOutputData(OUTPUT_SIZE_MODEL.getName(), sizeModelData, progressInfo);
        }

        if (cleanUpAfterwards) {
            PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
        }
    }

    private ImagePlus applyConnectedComponents(ImagePlus mask, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Apply MorphoLibJ connected components labeling (8-connectivity, 16-bit) to " + mask);
        if (enable3DSegmentation) {
            ConnectedComponentsLabeling3DAlgorithm algorithm = JIPipe.createNode(ConnectedComponentsLabeling3DAlgorithm.class);
            algorithm.setConnectivity(Neighborhood3D.TwentySixConnected);
            algorithm.setOutputType(new JIPipeDataInfoRef(ImagePlusGreyscale16UData.class));
            algorithm.getFirstInputSlot().addData(new ImagePlus3DGreyscaleMaskData(mask), progressInfo);
            algorithm.run(progressInfo);
            return algorithm.getFirstOutputSlot().getData(0, ImagePlusGreyscale16UData.class, progressInfo).getImage();
        } else {
            ConnectedComponentsLabeling2DAlgorithm algorithm = JIPipe.createNode(ConnectedComponentsLabeling2DAlgorithm.class);
            algorithm.setConnectivity(Neighborhood2D.EightConnected);
            algorithm.setOutputType(new JIPipeDataInfoRef(ImagePlusGreyscale16UData.class));
            algorithm.getFirstInputSlot().addData(new ImagePlus3DGreyscaleMaskData(mask), progressInfo);
            algorithm.run(progressInfo);
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
            ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
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

    @JIPipeDocumentation(name = "Epochs", description = "Number of epochs that should be trained.")
    @JIPipeParameter("epochs")
    public int getNumEpochs() {
        return numEpochs;
    }

    @JIPipeParameter("epochs")
    public void setNumEpochs(int numEpochs) {
        this.numEpochs = numEpochs;
    }

    @Override
    protected void onDeserialized(JsonNode node, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {
        super.onDeserialized(node, issues, notifications);
        OmniposeExtension.createMissingPythonNotificationIfNeeded(notifications);
    }
}
