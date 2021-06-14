package org.hkijena.jipipe.extensions.cellpose.algorithms;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.cellpose.CellPosePretrainedModel;
import org.hkijena.jipipe.extensions.cellpose.CellPoseSettings;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseModelData;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseSizeModelData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.LabeledImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Cellpose training", description = "Trains a model with Cellpose. You start from an existing model or train from scratch. " +
        "Incoming images are automatically converted to greyscale. Only 2D or 3D images are supported.")
@JIPipeInputSlot(value = LabeledImagePlusData.class, slotName = "Training data", autoCreate = true)
@JIPipeInputSlot(value = LabeledImagePlusData.class, slotName = "Test data", autoCreate = true, optional = true)
@JIPipeInputSlot(value = CellPoseModelData.class)
@JIPipeOutputSlot(value = CellPoseModelData.class, slotName = "Model", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
public class CellPoseTrainingAlgorithm extends JIPipeMergingAlgorithm {

    private boolean enableGPU = true;
    private CellPosePretrainedModel pretrainedModel = CellPosePretrainedModel.Cytoplasm;
    private int numEpochs = 500;
    private double learningRate = 0.2;
    private int batchSize = 8;
    private boolean useResidualConnections = true;
    private boolean useStyleVector = true;
    private boolean concatenateDownsampledLayers = false;
    private boolean enable3DSegmentation = true;
    private boolean cleanUpAfterwards = true;
    private double diameter = 30;
    private boolean trainSizeModel = false;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();

    public CellPoseTrainingAlgorithm(JIPipeNodeInfo info) {
        super(info);
        updateSlots();
    }

    public CellPoseTrainingAlgorithm(CellPoseTrainingAlgorithm other) {
        super(other);
        this.enableGPU = other.enableGPU;
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
        updateSlots();
    }

    private void updateSlots() {
        if (pretrainedModel != CellPosePretrainedModel.Custom) {
            if (getInputSlotMap().containsKey("Pretrained model")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.removeInputSlot("Pretrained model", false);
            }
        } else {
            if (!getInputSlotMap().containsKey("Pretrained model")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.addSlot("Pretrained model", new JIPipeDataSlotInfo(CellPoseModelData.class, JIPipeSlotType.Input, null), false);
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
                slotConfiguration.addSlot("Size model", new JIPipeDataSlotInfo(CellPoseSizeModelData.class, JIPipeSlotType.Output, null), false);
            }
        }
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

    @JIPipeDocumentation(name = "Learning rate")
    @JIPipeParameter("learning-rate")
    public double getLearningRate() {
        return learningRate;
    }

    @JIPipeParameter("learning-rate")
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    @JIPipeDocumentation(name = "Batch size")
    @JIPipeParameter("batch-size")
    public int getBatchSize() {
        return batchSize;
    }

    @JIPipeParameter("batch-size")
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @JIPipeDocumentation(name = "Use residual connections")
    @JIPipeParameter("use-residual-connections")
    public boolean isUseResidualConnections() {
        return useResidualConnections;
    }

    @JIPipeParameter("use-residual-connections")
    public void setUseResidualConnections(boolean useResidualConnections) {
        this.useResidualConnections = useResidualConnections;
    }

    @JIPipeDocumentation(name = "Use style vector")
    @JIPipeParameter("use-style-vector")
    public boolean isUseStyleVector() {
        return useStyleVector;
    }

    @JIPipeParameter("use-style-vector")
    public void setUseStyleVector(boolean useStyleVector) {
        this.useStyleVector = useStyleVector;
    }

    @JIPipeDocumentation(name = "Concatenate downsampled layers",
            description = "Concatenate downsampled layers with upsampled layers (off by default which means they are added)")
    @JIPipeParameter("concatenate-downsampled-layers")
    public boolean isConcatenateDownsampledLayers() {
        return concatenateDownsampledLayers;
    }

    @JIPipeParameter("concatenate-downsampled-layers")
    public void setConcatenateDownsampledLayers(boolean concatenateDownsampledLayers) {
        this.concatenateDownsampledLayers = concatenateDownsampledLayers;
    }

    @JIPipeDocumentation(name = "Mean diameter", description = "The cell diameter. Depending on the model, you can choose following values: " +
            "<ul>" +
            "<li><b>Cytoplasm</b>: You need to rescale all your images that structures have a diameter of about 30 pixels.</li>" +
            "<li><b>Nuclei</b>: You need to rescale all your images that structures have a diameter of about 17 pixels.</li>" +
            "<li><b>Custom</b>: You need to rescale all your images that structures have a diameter appropriate for the model.</li>" +
            "<li><b>None</b>: This will train from scratch. You can freely set the diameter. You also can set the diameter to 0 to disable scaling.</li>" +
            "</ul>")
    @JIPipeParameter("diameter")
    public double getDiameter() {
        return diameter;
    }

    @JIPipeParameter("diameter")
    public void setDiameter(double diameter) {
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
            "the one in the Project > Application settings > Extensions > Cellpose is used.")
    @JIPipeParameter("override-environment")
    public OptionalPythonEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalPythonEnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
    }

    @JIPipeDocumentation(name = "Enable 3D segmentation", description = "If enabled, Cellpose will train in 3D. " +
            "Otherwise, JIPipe will prepare the data by splitting 3D data into planes.")
    @JIPipeParameter("enable-3d-segmentation")
    public boolean isEnable3DSegmentation() {
        return enable3DSegmentation;
    }

    @JIPipeParameter("enable-3d-segmentation")
    public void setEnable3DSegmentation(boolean enable3DSegmentation) {
        this.enable3DSegmentation = enable3DSegmentation;
    }

    @JIPipeDocumentation(name = "With GPU", description = "Utilize a GPU if available. Please note that you need to setup Cellpose " +
            "to allow usage of your GPU. Also ensure that enough memory is available.")
    @JIPipeParameter("enable-gpu")
    public boolean isEnableGPU() {
        return enableGPU;
    }

    @JIPipeParameter("enable-gpu")
    public void setEnableGPU(boolean enableGPU) {
        this.enableGPU = enableGPU;
    }

    @JIPipeDocumentation(name = "Model", description = "The pretrained model that should be used. You can either choose one of the models " +
            "provided by Cellpose, a custom model, or train from scratch. The pre-trained model has influence on the diameter and how the input images should be prepared:" +
            "<ul>" +
            "<li><b>Cytoplasm</b>: You need to rescale all your images that structures have a diameter of about 30 pixels.</li>" +
            "<li><b>Nuclei</b>: You need to rescale all your images that structures have a diameter of about 17 pixels.</li>" +
            "<li><b>Custom</b>: You need to rescale all your images that structures have a diameter appropriate for the model. Don't forget to set the diameter value.</li>" +
            "<li><b>None</b>: This will train from scratch. You can freely set the diameter. You also can set the diameter to 0 to disable scaling.</li>" +
            "</ul>")
    @JIPipeParameter("pretrained-model")
    public CellPosePretrainedModel getPretrainedModel() {
        return pretrainedModel;
    }

    @JIPipeParameter("pretrained-model")
    public void setPretrainedModel(CellPosePretrainedModel pretrainedModel) {
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
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        // Prepare folders
        Path workDirectory = RuntimeSettings.generateTempDirectory("cellpose-training");
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
            LabeledImagePlusData masked = getInputSlot("Training data")
                    .getData(row, LabeledImagePlusData.class, rowProgress);
            ImagePlus image = ImagePlusGreyscaleData.convertIfNeeded(masked.getImage());
            ImagePlus mask = ImageJUtils.getNormalizedMask(image, masked.getLabels());
            dataIs3D |= image.getNDimensions() > 2 && enable3DSegmentation;

            saveImagesToPath(trainingDir, imageCounter, rowProgress, image, mask);
        }
        for (Integer row : dataBatch.getInputRows("Test data")) {
            JIPipeProgressInfo rowProgress = extractProgress.resolveAndLog("Row " + row);
            LabeledImagePlusData masked = getInputSlot("Test data")
                    .getData(row, LabeledImagePlusData.class, rowProgress);
            ImagePlus image = ImagePlusGreyscaleData.convertIfNeeded(masked.getImage());
            ImagePlus mask = ImageJUtils.getNormalizedMask(image, masked.getLabels());
            dataIs3D |= image.getNDimensions() > 2 && enable3DSegmentation;

            saveImagesToPath(testDir, imageCounter, rowProgress, image, mask);
        }

        // Extract model if custom
        Path customModelPath = null;
        if (pretrainedModel == CellPosePretrainedModel.Custom) {
            Set<Integer> pretrainedModelRows = dataBatch.getInputRows("Pretrained model");
            if (pretrainedModelRows.size() != 1) {
                throw new UserFriendlyRuntimeException("Only one pretrained model is allowed",
                        "Only one pretrained model is allowed",
                        getDisplayName(),
                        "You can only provide one pretrained model per data batch for training.",
                        "Ensure that only one pretrained model is in a data batch.");
            }
            CellPoseModelData modelData = dataBatch.getInputData("Pretrained model", CellPoseModelData.class, progressInfo).get(0);
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
        arguments.add("mask");

        arguments.add("--chan");
        arguments.add("0");

        if (enableGPU)
            arguments.add("--use_gpu");
        if (dataIs3D)
            arguments.add("--do_3D");
        if (pretrainedModel == CellPosePretrainedModel.Custom || pretrainedModel == CellPosePretrainedModel.None) {
            arguments.add("--diameter");
            arguments.add(diameter + "");
        }

        switch (pretrainedModel) {
            case Cytoplasm:
            case Nucleus:
                arguments.add("--pretrained_model");
                arguments.add(pretrainedModel.getId());
                break;
            case Custom:
                arguments.add("--pretrained_model");
                arguments.add(customModelPath.toAbsolutePath().toString());
                break;
            case None:
                arguments.add("--pretrained_model");
                arguments.add("None");
                break;
        }

        if (trainSizeModel)
            arguments.add("--train_size");

        arguments.add("--learning_rate");
        arguments.add(learningRate + "");

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

        // Run the module
        PythonUtils.runPython(arguments.toArray(new String[0]), overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() :
                CellPoseSettings.getInstance().getPythonEnvironment(), Collections.emptyList(), progressInfo);

        // Extract the model
        Path modelsPath = trainingDir.resolve("models");
        Path generatedModelFile = findModelFile(modelsPath);
        CellPoseModelData modelData = new CellPoseModelData(generatedModelFile);
        dataBatch.addOutputData("Model", modelData, progressInfo);

        // Extract size model
        if (trainSizeModel) {
            Path generatedSizeModelFile = findSizeModelFile(modelsPath);
            CellPoseSizeModelData sizeModelData = new CellPoseSizeModelData(generatedSizeModelFile);
            dataBatch.addOutputData("Size model", sizeModelData, progressInfo);
        }

        if (cleanUpAfterwards) {
            try {
                FileUtils.deleteDirectory(workDirectory.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                Path maskFile = dir.resolve("i" + imageCounter + "_mask.tif");
                imageCounter.getAndIncrement();
                IJ.saveAs(maskSliceImage, "TIFF", imageFile.toString());
                IJ.saveAs(imageSliceImage, "TIFF", maskFile.toString());
            }, rowProgress);
        } else {
            // Save as-is
            Path imageFile = dir.resolve("i" + imageCounter + "_raw.tif");
            Path maskFile = dir.resolve("i" + imageCounter + "_mask.tif");
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
}
