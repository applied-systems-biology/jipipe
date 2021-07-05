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
import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.deeplearning.enums.DeepLearningModelType;
import org.hkijena.jipipe.extensions.deeplearning.DeepLearningSettings;
import org.hkijena.jipipe.extensions.deeplearning.DeepLearningUtils;
import org.hkijena.jipipe.extensions.deeplearning.OptionalDeepLearningDeviceEnvironment;
import org.hkijena.jipipe.extensions.deeplearning.configs.DeepLearningTrainingConfiguration;
import org.hkijena.jipipe.extensions.deeplearning.datatypes.DeepLearningModelData;
import org.hkijena.jipipe.extensions.expressions.DataAnnotationQueryExpression;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.ScaleMode;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TransformScale2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Train model (labeled images)", description = "Trains a Deep Learning model with images. Please note the the model must be able to be trained with labeled images.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Labels", autoCreate = true)
@JIPipeInputSlot(value = DeepLearningModelData.class, slotName = "Model", autoCreate = true)
@JIPipeOutputSlot(value = DeepLearningModelData.class, slotName = "Trained model", autoCreate = true)
public class TrainImageModelAlgorithm extends JIPipeSingleIterationAlgorithm {

    private TransformScale2DAlgorithm scale2DAlgorithm;
    private boolean scaleToModelSize = false;
    private DeepLearningTrainingConfiguration trainingConfiguration = new DeepLearningTrainingConfiguration();
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();
    private boolean cleanUpAfterwards = true;
    private OptionalDeepLearningDeviceEnvironment overrideDevices = new OptionalDeepLearningDeviceEnvironment();
    private DataAnnotationQueryExpression labelDataAnnotation = new DataAnnotationQueryExpression("Label");

    public TrainImageModelAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(trainingConfiguration);
        scale2DAlgorithm = JIPipe.createNode(TransformScale2DAlgorithm.class);
        scale2DAlgorithm.setScaleMode(ScaleMode.Fit);
        registerSubParameter(scale2DAlgorithm);
    }

    public TrainImageModelAlgorithm(TrainImageModelAlgorithm other) {
        super(other);
        this.trainingConfiguration = new DeepLearningTrainingConfiguration(other.trainingConfiguration);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.scaleToModelSize = other.scaleToModelSize;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        registerSubParameter(trainingConfiguration);
        this.scale2DAlgorithm = new TransformScale2DAlgorithm(other.scale2DAlgorithm);
        registerSubParameter(scale2DAlgorithm);
        this.overrideDevices = new OptionalDeepLearningDeviceEnvironment(other.overrideDevices);
        this.labelDataAnnotation = new DataAnnotationQueryExpression(other.labelDataAnnotation);
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
        JIPipeDataSlot inputLabelsSlot = getInputSlot("Labels");
        int modelCounter = 0;
        for (Integer modelIndex : dataBatch.getInputSlotRows().get(inputModelSlot)) {
            JIPipeProgressInfo modelProgress = progressInfo.resolveAndLog("Check model", modelCounter++, dataBatch.getInputSlotRows().get(inputModelSlot).size());
            DeepLearningModelData inputModel = inputModelSlot.getData(modelIndex, DeepLearningModelData.class, modelProgress);

            if(inputModel.getModelConfiguration().getModelType() == DeepLearningModelType.classification) {
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
            Path labelsDirectory = workDirectory.resolve("labels");
            Path rawsDirectory = workDirectory.resolve("raw");
            try {
                Files.createDirectories(labelsDirectory);
                Files.createDirectories(rawsDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int imageCounter = 0;
            Set<Integer> labelRows = dataBatch.getInputSlotRows().get(inputLabelsSlot);
            for (Integer imageIndex : labelRows) {
                JIPipeProgressInfo imageProgress = modelProgress.resolveAndLog("Write labels", imageCounter++, labelRows.size());
                ImagePlusData raw = inputLabelsSlot.getData(imageIndex, ImagePlusData.class, imageProgress);
                ImagePlusData label = labelDataAnnotation.queryFirst(inputLabelsSlot.getDataAnnotations(imageIndex)).getData(ImagePlusData.class, progressInfo);
                Path rawPath = rawsDirectory.resolve(imageCounter + "_img.tif");
                Path labelPath = labelsDirectory.resolve(imageCounter + "_img.tif");

                if(raw.hasLoadedImage() || isScaleToModelSize()) {
                    ImagePlus rawImage = isScaleToModelSize() ? DeepLearningUtils.scaleToModel(raw.getImage(),
                            inputModel.getModelConfiguration(),
                            getScale2DAlgorithm(),
                            imageProgress) : raw.getImage();
                    IJ.saveAsTiff(rawImage, rawPath.toString());
                }
                else {
                    raw.saveTo(rawsDirectory, imageCounter + "_img", true, imageProgress);
                }
                if(label.hasLoadedImage() || isScaleToModelSize()) {
                    ImagePlus labelImage = isScaleToModelSize() ? DeepLearningUtils.scaleToModel(label.getImage(),
                            inputModel.getModelConfiguration(),
                            getScale2DAlgorithm(),
                            imageProgress) : label.getImage();
                    IJ.saveAsTiff(labelImage, labelPath.toString());
                }
                else {
                    label.saveTo(labelsDirectory, imageCounter + "_img", true, imageProgress);
                }
            }

            // Save model according to standard interface
            inputModel.saveTo(workDirectory, "", false, modelProgress);

            // Modify and save configurations
            DeepLearningTrainingConfiguration trainingConfiguration = new DeepLearningTrainingConfiguration(this.trainingConfiguration);
            trainingConfiguration.setInputModelPath(workDirectory.resolve("model.hdf5"));
            trainingConfiguration.setOutputModelPath(workDirectory.resolve("trained_model.hdf5"));
            trainingConfiguration.setOutputModelJsonPath(workDirectory.resolve("trained_model.json"));
            trainingConfiguration.setInputImagesPattern(rawsDirectory + "/*.tif");
            trainingConfiguration.setInputLabelsPattern(labelsDirectory + "/*.tif");
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
        if ("axis".equals(access.getKey()) && access.getSource() == getScale2DAlgorithm()) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }
}
