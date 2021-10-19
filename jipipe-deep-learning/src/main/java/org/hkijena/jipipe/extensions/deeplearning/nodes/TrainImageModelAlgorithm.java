/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.deeplearning.nodes;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSingleIterationAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.deeplearning.DeepLearningSettings;
import org.hkijena.jipipe.extensions.deeplearning.DeepLearningUtils;
import org.hkijena.jipipe.extensions.deeplearning.configs.DeepLearningTrainingConfiguration;
import org.hkijena.jipipe.extensions.deeplearning.datatypes.DeepLearningModelData;
import org.hkijena.jipipe.extensions.deeplearning.enums.ModelType;
import org.hkijena.jipipe.extensions.deeplearning.enums.NormalizationMethod;
import org.hkijena.jipipe.extensions.deeplearning.environments.OptionalDeepLearningDeviceEnvironment;
import org.hkijena.jipipe.extensions.expressions.DataAnnotationQueryExpression;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.InterpolationMethod;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.ScaleMode;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TransformScale2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Train model (labeled images)", description = "Trains a Deep Learning model with images. Please note the the model must be able to be trained with labeled images. " +
        "For this node to work, you need to annotate a greyscale 16-bit or 8-bit label image column to each raw data input. To do this, you can use the node 'Annotate with data'.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Training data", autoCreate = true)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Validation data", autoCreate = true)
@JIPipeInputSlot(value = DeepLearningModelData.class, slotName = "Model", autoCreate = true)
@JIPipeOutputSlot(value = DeepLearningModelData.class, slotName = "Trained model", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "History")
public class TrainImageModelAlgorithm extends JIPipeSingleIterationAlgorithm {

    public static final JIPipeDataSlotInfo SLOT_HISTORY = new JIPipeDataSlotInfo(ResultsTableData.class, JIPipeSlotType.Output, "History");

    private TransformScale2DAlgorithm scale2DAlgorithm;
    private boolean scaleToModelSize = true;
    private DeepLearningTrainingConfiguration trainingConfiguration = new DeepLearningTrainingConfiguration();
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();
    private boolean cleanUpAfterwards = true;
    private OptionalDeepLearningDeviceEnvironment overrideDevices = new OptionalDeepLearningDeviceEnvironment();
    private DataAnnotationQueryExpression labelDataAnnotation = new DataAnnotationQueryExpression("Label");
    private NormalizationMethod normalization = NormalizationMethod.zero_one;
    private boolean outputHistory = false;

    public TrainImageModelAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(trainingConfiguration);
        scale2DAlgorithm = JIPipe.createNode(TransformScale2DAlgorithm.class);
        scale2DAlgorithm.setScaleMode(ScaleMode.Fit);
        scale2DAlgorithm.setInterpolationMethod(InterpolationMethod.None);
        registerSubParameter(scale2DAlgorithm);
    }

    public TrainImageModelAlgorithm(TrainImageModelAlgorithm other) {
        super(other);
        this.trainingConfiguration = new DeepLearningTrainingConfiguration(other.trainingConfiguration);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.scaleToModelSize = other.scaleToModelSize;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.normalization = other.normalization;
        registerSubParameter(trainingConfiguration);
        this.scale2DAlgorithm = new TransformScale2DAlgorithm(other.scale2DAlgorithm);
        registerSubParameter(scale2DAlgorithm);
        this.overrideDevices = new OptionalDeepLearningDeviceEnvironment(other.overrideDevices);
        this.labelDataAnnotation = new DataAnnotationQueryExpression(other.labelDataAnnotation);
        setOutputHistory(other.isOutputHistory());
    }

    @JIPipeDocumentation(name = "Output history", description = "If enabled, the training history (loss, TP, FN, ...) is written to the output.")
    @JIPipeParameter("output-history")
    public boolean isOutputHistory() {
        return outputHistory;
    }

    @JIPipeParameter("output-history")
    public void setOutputHistory(boolean outputHistory) {
        this.outputHistory = outputHistory;
        toggleSlot(SLOT_HISTORY, outputHistory);
    }

    @JIPipeDocumentation(name = "Normalization", description = "The normalization method used for preprocessing the images.")
    @JIPipeParameter("normalization")
    public NormalizationMethod getNormalization() {
        return normalization;
    }

    @JIPipeParameter("normalization")
    public void setNormalization(NormalizationMethod normalization) {
        this.normalization = normalization;
    }

    @JIPipeDocumentation(name = "Label data annotation", description = "Determines which data annotation contains the labels. Please ensure that " +
            "the appropriate label data is annotated to the raw input data.\n\n" + DataAnnotationQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("label-data-annotation")
    public DataAnnotationQueryExpression getLabelDataAnnotation() {
        return labelDataAnnotation;
    }

    @JIPipeParameter("label-data-annotation")
    public void setLabelDataAnnotation(DataAnnotationQueryExpression labelDataAnnotation) {
        this.labelDataAnnotation = labelDataAnnotation;
    }

    @JIPipeDocumentation(name = "Override device configuration", description = "If enabled, this nodes provides a custom device configuration, " +
            "different to the one inside the application settings")
    @JIPipeParameter("override-devices")
    public OptionalDeepLearningDeviceEnvironment getOverrideDevices() {
        return overrideDevices;
    }

    @JIPipeParameter("override-devices")
    public void setOverrideDevices(OptionalDeepLearningDeviceEnvironment overrideDevices) {
        this.overrideDevices = overrideDevices;
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot inputModelSlot = getInputSlot("Model");
        JIPipeDataSlot inputTrainingDataSlot = getInputSlot("Training data");
        JIPipeDataSlot inputValidationDataSlot = getInputSlot("Validation data");
        int modelCounter = 0;
        for (Integer modelIndex : dataBatch.getInputSlotRows().get(inputModelSlot)) {
            JIPipeProgressInfo modelProgress = progressInfo.resolveAndLog("Check model", modelCounter++, dataBatch.getInputSlotRows().get(inputModelSlot).size());
            DeepLearningModelData inputModel = inputModelSlot.getData(modelIndex, DeepLearningModelData.class, modelProgress);

            if (inputModel.getModelConfiguration().getModelType() == ModelType.classification) {
                throw new UserFriendlyRuntimeException("Model " + inputModel + " is not supported by this node!",
                        "Unsupported model",
                        getDisplayName(),
                        "The input model '" + inputModel + "' is not supported by this node.",
                        "Use a different predict node.");
            }
        }
        modelCounter = 0;
        for (Integer modelIndex : dataBatch.getInputSlotRows().get(inputModelSlot)) {
            JIPipeProgressInfo modelProgress = progressInfo.resolveAndLog("Model", modelCounter++, dataBatch.getInputSlotRows().get(inputModelSlot).size());
            DeepLearningModelData inputModel = inputModelSlot.getData(modelIndex, DeepLearningModelData.class, modelProgress);

            Path workDirectory = getNewScratch();

            // Save labels & raw images
            Path labelsDirectory = PathUtils.resolveAndMakeSubDirectory(workDirectory, "labels");
            Path validationLabelsDirectory = PathUtils.resolveAndMakeSubDirectory(workDirectory, "validation-labels");
            Path rawsDirectory = PathUtils.resolveAndMakeSubDirectory(workDirectory, "raw");
            Path validationRawsDirectory = PathUtils.resolveAndMakeSubDirectory(workDirectory, "validation-raw");

            writeImages(dataBatch, progressInfo, inputTrainingDataSlot, modelProgress, inputModel, labelsDirectory, rawsDirectory, "train");
            writeImages(dataBatch, progressInfo, inputValidationDataSlot, modelProgress, inputModel, validationLabelsDirectory, validationRawsDirectory, "val");

            // Save model according to standard interface
            inputModel.saveTo(workDirectory, "", false, modelProgress);

            // Modify and save configurations
            DeepLearningTrainingConfiguration trainingConfiguration = new DeepLearningTrainingConfiguration(this.trainingConfiguration);
            trainingConfiguration.setInputModelPath(workDirectory.resolve("model.hdf5"));
            trainingConfiguration.setOutputModelPath(workDirectory.resolve("trained_model.hdf5"));
            trainingConfiguration.setOutputModelJsonPath(workDirectory.resolve("trained_model.json"));
            trainingConfiguration.setLogDir(PathUtils.resolveAndMakeSubDirectory(workDirectory, "logs"));
            trainingConfiguration.setInputImagesPattern(rawsDirectory + "/*.tif");
            trainingConfiguration.setInputLabelsPattern(labelsDirectory + "/*.tif");
            trainingConfiguration.setValidationImagesPattern(validationRawsDirectory + "/*.tif");
            trainingConfiguration.setValidationLabelsPattern(validationLabelsDirectory + "/*.tif");
            trainingConfiguration.setNormalization(normalization);
            try {
                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(workDirectory.resolve("training-config.json").toFile(), trainingConfiguration);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Path deviceConfigurationPath = workDirectory.resolve("device-configuration.json");
            if (getOverrideDevices().isEnabled())
                getOverrideDevices().getContent().saveAsJson(deviceConfigurationPath);
            else
                DeepLearningSettings.getInstance().getDeepLearningDevice().saveAsJson(deviceConfigurationPath);

            // Run Python
            List<String> arguments = new ArrayList<>();
            arguments.add("-m");
            arguments.add("dltoolbox");
            arguments.add("--operation");
            arguments.add("train");
            arguments.add("--config");
            arguments.add(workDirectory.resolve("training-config.json").toString());
            arguments.add("--model-config");
            arguments.add(workDirectory.resolve("model-config.json").toString());
            arguments.add("--device-config");
            arguments.add(deviceConfigurationPath.toString());

            if (DeepLearningSettings.getInstance().getDeepLearningToolkit().needsInstall())
                DeepLearningSettings.getInstance().getDeepLearningToolkit().install(modelProgress);
            PythonUtils.runPython(arguments.toArray(new String[0]), overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() :
                            DeepLearningSettings.getInstance().getPythonEnvironment(),
                    Collections.singletonList(DeepLearningSettings.getInstance().getDeepLearningToolkit().getLibraryDirectory().toAbsolutePath()), modelProgress);

            DeepLearningModelData modelData = new DeepLearningModelData(workDirectory.resolve("trained_model.hdf5"),
                    workDirectory.resolve("model-config.json"),
                    workDirectory.resolve("trained_model.json"));
            dataBatch.addOutputData("Trained model", modelData, modelProgress);

            if (outputHistory) {
                Path historyFile = workDirectory.resolve("logs/training.log");
                if (Files.isRegularFile(historyFile)) {
                    ResultsTableData tableData = ResultsTableData.fromCSV(historyFile);
                    dataBatch.addOutputData("History", tableData, progressInfo);
                }
            }

            if (cleanUpAfterwards) {
                PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
            }
        }
    }

    private void writeImages(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo, JIPipeDataSlot inputValidationDataSlot, JIPipeProgressInfo modelProgress, DeepLearningModelData inputModel, Path labelsDirectory, Path rawsDirectory, String prefix) {
        int imageCounter = 0;
        Set<Integer> labelRows = dataBatch.getInputSlotRows().get(inputValidationDataSlot);
        for (Integer imageIndex : labelRows) {
            JIPipeProgressInfo imageProgress = modelProgress.resolveAndLog("Write labeled images", imageCounter++, labelRows.size());
            ImagePlusData raw = inputValidationDataSlot.getData(imageIndex, ImagePlus3DGreyscaleData.class, imageProgress);
            ImagePlusData label = labelDataAnnotation.queryFirst(inputValidationDataSlot.getDataAnnotations(imageIndex)).getData(ImagePlus3DGreyscaleData.class, progressInfo);
            Path rawPath = rawsDirectory.resolve(imageCounter + "_" + prefix + ".tif");
            Path labelPath = labelsDirectory.resolve(imageCounter + "_" + prefix + ".tif");

            if (raw.hasLoadedImage() || isScaleToModelSize()) {
                ImagePlus rawImage = isScaleToModelSize() ? DeepLearningUtils.scaleToModel(raw.getImage(),
                        inputModel.getModelConfiguration(),
                        getScale2DAlgorithm(),
                        true,
                        true,
                        imageProgress) : raw.getImage();
                IJ.saveAsTiff(rawImage, rawPath.toString());
            } else {
                raw.saveTo(rawsDirectory, imageCounter + "_" + prefix, true, imageProgress);
            }
            if (label.hasLoadedImage() || isScaleToModelSize()) {
                ImagePlus labelImage = isScaleToModelSize() ? DeepLearningUtils.scaleToModel(label.getImage(),
                        inputModel.getModelConfiguration(),
                        getScale2DAlgorithm(),
                        true,
                        true, imageProgress) : label.getImage();
                IJ.saveAsTiff(labelImage, labelPath.toString());
            } else {
                label.saveTo(labelsDirectory, imageCounter + "_" + prefix, true, imageProgress);
            }
        }
    }

    @JIPipeDocumentation(name = "Training", description = "Use following settings to change the properties of the training")
    @JIPipeParameter(value = "training",
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/dl-model.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/dl-model.png")
    public DeepLearningTrainingConfiguration getTrainingConfiguration() {
        return trainingConfiguration;
    }

    @JIPipeDocumentation(name = "Override Python environment", description = "If enabled, a different Python environment is used for this Node. Otherwise " +
            "the one in the Project > Application settings > Extensions > Deep Learning is used.")
    @JIPipeParameter("override-environment")
    public OptionalPythonEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalPythonEnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
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

    @JIPipeDocumentation(name = "Scaling", description = "The following settings determine how the image is scaled in 2D if it does not fit to the size the model is designed for.")
    @JIPipeParameter(value = "scale-algorithm",
            collapsed = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/transform-scale.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/transform-scale.png")
    public TransformScale2DAlgorithm getScale2DAlgorithm() {
        return scale2DAlgorithm;
    }

    @JIPipeDocumentation(name = "Scale images to model size", description = "If enabled, images are automatically scaled to fit to the model size. Otherwise, " +
            "Keras will apply tiling automatically if you provide images of an unsupported size.")
    @JIPipeParameter("scale-to-model-size")
    public boolean isScaleToModelSize() {
        return scaleToModelSize;
    }

    @JIPipeParameter("scale-to-model-size")
    public void setScaleToModelSize(boolean scaleToModelSize) {
        this.scaleToModelSize = scaleToModelSize;
        triggerParameterUIChange();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if (!scaleToModelSize && subParameter == getScale2DAlgorithm()) {
            return false;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if ("x-axis".equals(access.getKey()) && access.getSource() == getScale2DAlgorithm()) {
            return false;
        }
        if ("y-axis".equals(access.getKey()) && access.getSource() == getScale2DAlgorithm()) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }
}
