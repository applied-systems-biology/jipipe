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
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
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
import org.hkijena.jipipe.extensions.deeplearning.enums.NormalizationMethod;
import org.hkijena.jipipe.extensions.deeplearning.environments.OptionalDeepLearningDeviceEnvironment;
import org.hkijena.jipipe.extensions.deeplearning.configs.DeepLearningTrainingConfiguration;
import org.hkijena.jipipe.extensions.deeplearning.datatypes.DeepLearningModelData;
import org.hkijena.jipipe.extensions.deeplearning.enums.ModelType;
import org.hkijena.jipipe.extensions.expressions.AnnotationQueryExpression;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.ScaleMode;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TransformScale2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Train model (classified images)", description = "Trains a Deep Learning model with classified images. The image classes are " +
        "extracted from an annotation column. Please note that the model must be able to be trained with classified images.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Training data", autoCreate = true)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Test data", autoCreate = true)
@JIPipeInputSlot(value = DeepLearningModelData.class, slotName = "Model", autoCreate = true)
@JIPipeOutputSlot(value = DeepLearningModelData.class, slotName = "Trained model", autoCreate = true)
public class TrainClassifierModelAlgorithm extends JIPipeSingleIterationAlgorithm {

    private TransformScale2DAlgorithm scale2DAlgorithm;
    private boolean scaleToModelSize = false;
    private DeepLearningTrainingConfiguration trainingConfiguration = new DeepLearningTrainingConfiguration();
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();
    private boolean cleanUpAfterwards = true;
    private OptionalDeepLearningDeviceEnvironment overrideDevices = new OptionalDeepLearningDeviceEnvironment();
    private AnnotationQueryExpression labelAnnotation = new AnnotationQueryExpression("Label");
    private NormalizationMethod normalization = NormalizationMethod.zero_one;

    public TrainClassifierModelAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(trainingConfiguration);
        scale2DAlgorithm = JIPipe.createNode(TransformScale2DAlgorithm.class);
        scale2DAlgorithm.setScaleMode(ScaleMode.Fit);
        registerSubParameter(scale2DAlgorithm);
    }

    public TrainClassifierModelAlgorithm(TrainClassifierModelAlgorithm other) {
        super(other);
        this.trainingConfiguration = new DeepLearningTrainingConfiguration(other.trainingConfiguration);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.scaleToModelSize = other.scaleToModelSize;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.labelAnnotation = new AnnotationQueryExpression(other.labelAnnotation);
        this.normalization = other.normalization;
        registerSubParameter(trainingConfiguration);
        this.scale2DAlgorithm = new TransformScale2DAlgorithm(other.scale2DAlgorithm);
        registerSubParameter(scale2DAlgorithm);
        this.overrideDevices = new OptionalDeepLearningDeviceEnvironment(other.overrideDevices);
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

    @JIPipeDocumentation(name = "Label annotation", description = "This annotation is used as label for the data. These labels should be non-empty and numeric. " +
            "If annotations do not comply to this format, use the 'Set annotations' node to set/convert the labels.")
    @JIPipeParameter("label-annotation")
    public AnnotationQueryExpression getLabelAnnotation() {
        return labelAnnotation;
    }

    @JIPipeParameter("label-annotation")
    public void setLabelAnnotation(AnnotationQueryExpression labelAnnotation) {
        this.labelAnnotation = labelAnnotation;
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot inputModelSlot = getInputSlot("Model");
        JIPipeDataSlot inputTrainingDataSlot = getInputSlot("Training data");
        JIPipeDataSlot inputTestDataSlot = getInputSlot("Test data");

        int modelCounter = 0;
        for (Integer modelIndex : dataBatch.getInputSlotRows().get(inputModelSlot)) {
            JIPipeProgressInfo modelProgress = progressInfo.resolveAndLog("Check model", modelCounter++, dataBatch.getInputSlotRows().get(inputModelSlot).size());
            DeepLearningModelData inputModel = inputModelSlot.getData(modelIndex, DeepLearningModelData.class, modelProgress);

            if (inputModel.getModelConfiguration().getModelType() != ModelType.classification) {
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
            Path labelFile = workDirectory.resolve("labels.csv");
            Path validationLabelsFile = workDirectory.resolve("labels-validation.csv");
            Path rawsDirectory = PathUtils.resolveAndMakeSubDirectory(workDirectory, "raw");
            Path validationRawsDirectory = PathUtils.resolveAndMakeSubDirectory(workDirectory, "validation-raw");

            saveLabelsAndImages(dataBatch, inputTrainingDataSlot, modelProgress, inputModel, labelFile, rawsDirectory);
            saveLabelsAndImages(dataBatch, inputTestDataSlot, modelProgress, inputModel, validationLabelsFile, validationRawsDirectory);

            // Save model according to standard interface
            inputModel.saveTo(workDirectory, "", false, modelProgress);

            // Modify and save configurations
            DeepLearningTrainingConfiguration trainingConfiguration = new DeepLearningTrainingConfiguration(this.trainingConfiguration);
            trainingConfiguration.setInputModelPath(workDirectory.resolve("model.hdf5"));
            trainingConfiguration.setOutputModelPath(workDirectory.resolve("trained_model.hdf5"));
            trainingConfiguration.setOutputModelJsonPath(workDirectory.resolve("trained_model.json"));
            trainingConfiguration.setLogDir(PathUtils.resolveAndMakeSubDirectory(workDirectory, "logs"));
            trainingConfiguration.setInputImagesPattern(rawsDirectory + "/*.tif");
            trainingConfiguration.setInputLabelsPattern(labelFile.toString());
            trainingConfiguration.setValidationImagesPattern(validationRawsDirectory + "/*.tif");
            trainingConfiguration.setValidationLabelsPattern(validationLabelsFile.toString());
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
            dataBatch.addOutputData(getFirstOutputSlot(), modelData, modelProgress);

            if (cleanUpAfterwards) {
                PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
            }
        }
    }

    private void saveLabelsAndImages(JIPipeMergingDataBatch dataBatch, JIPipeDataSlot inputImagesSlot, JIPipeProgressInfo modelProgress, DeepLearningModelData inputModel, Path labelFile, Path rawsDirectory) {
        ResultsTableData labels = new ResultsTableData();
        labels.addColumn("filename", true);
        labels.addColumn("label", true);

        int imageCounter = 0;
        Set<Integer> imageRows = dataBatch.getInputSlotRows().get(inputImagesSlot);
        for (Integer imageIndex : imageRows) {
            JIPipeProgressInfo imageProgress = modelProgress.resolveAndLog("Write images", imageCounter++, imageRows.size());
            ImagePlusData image = inputImagesSlot.getData(imageIndex, ImagePlus3DGreyscaleData.class, imageProgress);
            Path rawPath = rawsDirectory.resolve(imageCounter + "_img.tif");

            if (image.hasLoadedImage() || isScaleToModelSize()) {
                ImagePlus rawImage = isScaleToModelSize() ? DeepLearningUtils.scaleToModel(image.getImage(),
                        inputModel.getModelConfiguration(),
                        getScale2DAlgorithm(),
                        true,
                        true, modelProgress) : image.getImage();

                IJ.saveAsTiff(rawImage, rawPath.toString());
            } else {
                image.saveTo(rawsDirectory, imageCounter + "_img", true, imageProgress);
            }

            // Extract the label annotation + value
            JIPipeAnnotation annotation = labelAnnotation.queryFirst(inputImagesSlot.getAnnotations(imageIndex));
            int imageLabel = Integer.parseInt(annotation.getValue());

            // Insert into table
            labels.addRow();
            labels.setValueAt(rawPath.getFileName().toString(), labels.getRowCount() - 1, 0);
            labels.setValueAt(imageLabel, labels.getRowCount() - 1, 1);
        }

        // Save labels
        modelProgress.log("Saving labels table to " + labelFile);
        labels.saveAsCSV(labelFile);
    }

    @JIPipeDocumentation(name = "Training", description = "Use following settings to change the properties of the training.\n\n" + AnnotationQueryExpression.DOCUMENTATION_DESCRIPTION)
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
