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

import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeColumMatching;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.deeplearning.DeepLearningSettings;
import org.hkijena.jipipe.extensions.deeplearning.OptionalDeepLearningDeviceEnvironment;
import org.hkijena.jipipe.extensions.deeplearning.configs.DeepLearningTrainingConfiguration;
import org.hkijena.jipipe.extensions.deeplearning.datatypes.DeepLearningModelData;
import org.hkijena.jipipe.extensions.expressions.DataAnnotationQueryExpression;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
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

@JIPipeDocumentation(name = "Train model (labeled image files)", description = "Trains a Deep Learning model. This node accepts direct file inputs that can be useful if you " +
        "want to avoid loading data into JIPipe. Please note that the model needs to be able to train on images. For this node to work, you need to annotate a label data column (of a file data type) to each raw data input. " +
        "To do this, you can use the node 'Annotate with data'.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
@JIPipeInputSlot(value = FileData.class, slotName = "Labels", autoCreate = true)
@JIPipeInputSlot(value = DeepLearningModelData.class, slotName = "Model", autoCreate = true)
@JIPipeOutputSlot(value = DeepLearningModelData.class, slotName = "Trained model", autoCreate = true)
public class FileBasedTrainImageModelAlgorithm extends JIPipeMergingAlgorithm {

    private DeepLearningTrainingConfiguration trainingConfiguration = new DeepLearningTrainingConfiguration();
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();
    private boolean cleanUpAfterwards = true;
    private OptionalDeepLearningDeviceEnvironment overrideDevices = new OptionalDeepLearningDeviceEnvironment();
    private DataAnnotationQueryExpression labelDataAnnotation = new DataAnnotationQueryExpression("Label");

    public FileBasedTrainImageModelAlgorithm(JIPipeNodeInfo info) {
        super(info);
        getDataBatchGenerationSettings().setColumnMatching(JIPipeColumMatching.MergeAll);
        registerSubParameter(trainingConfiguration);
    }

    public FileBasedTrainImageModelAlgorithm(FileBasedTrainImageModelAlgorithm other) {
        super(other);
        this.trainingConfiguration = new DeepLearningTrainingConfiguration(other.trainingConfiguration);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        registerSubParameter(trainingConfiguration);
        this.overrideDevices = new OptionalDeepLearningDeviceEnvironment(other.overrideDevices);
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

    @JIPipeDocumentation(name = "Label data annotation", description = "Determines which data annotation contains the labels. Please ensure that " +
            "the appropriate label data (file or path data) is annotated to the raw input data.\n\n" + DataAnnotationQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("label-data-annotation")
    public DataAnnotationQueryExpression getLabelDataAnnotation() {
        return labelDataAnnotation;
    }

    @JIPipeParameter("label-data-annotation")
    public void setLabelDataAnnotation(DataAnnotationQueryExpression labelDataAnnotation) {
        this.labelDataAnnotation = labelDataAnnotation;
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot inputModelSlot = getInputSlot("Model");
        JIPipeDataSlot inputLabelsSlot = getInputSlot("Labels");
        int modelCounter = 0;
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
                FileData raw = inputLabelsSlot.getData(imageIndex, FileData.class, imageProgress);
                FileData label = labelDataAnnotation.queryFirst(inputLabelsSlot.getDataAnnotations(imageIndex)).getData(FileData.class, progressInfo);
                Path rawPath = rawsDirectory.resolve(imageCounter + "_img.tif");
                Path labelPath = labelsDirectory.resolve(imageCounter + "_img.tif");

                PathUtils.copyOrLink(raw.toPath(), rawPath, imageProgress);
                PathUtils.copyOrLink(label.toPath(), labelPath, imageProgress);
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
                try {
                    FileUtils.deleteDirectory(workDirectory.toFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
}
