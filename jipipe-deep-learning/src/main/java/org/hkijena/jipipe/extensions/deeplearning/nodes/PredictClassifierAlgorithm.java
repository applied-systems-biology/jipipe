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
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataAnnotationMergeStrategy;
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
import org.hkijena.jipipe.extensions.deeplearning.environments.OptionalDeepLearningDeviceEnvironment;
import org.hkijena.jipipe.extensions.deeplearning.configs.DeepLearningPredictionConfiguration;
import org.hkijena.jipipe.extensions.deeplearning.datatypes.DeepLearningModelData;
import org.hkijena.jipipe.extensions.deeplearning.enums.ModelType;
import org.hkijena.jipipe.extensions.deeplearning.enums.NormalizationMethod;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.ScaleMode;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TransformScale2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalDataAnnotationNameParameter;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "Predict (classified images)", description = "Applies a prediction via a Deep learning model. The prediction returns classified images. " +
        "Please note that the model needs to be able to classify images.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = DeepLearningModelData.class, slotName = "Model", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Prediction", autoCreate = true)
public class PredictClassifierAlgorithm extends JIPipeSingleIterationAlgorithm {

    private TransformScale2DAlgorithm scale2DAlgorithm;
    private boolean cleanUpAfterwards = true;
    private boolean scaleToModelSize = true;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();
    private OptionalDeepLearningDeviceEnvironment overrideDevices = new OptionalDeepLearningDeviceEnvironment();
    private NormalizationMethod normalization = NormalizationMethod.zero_one;
    private OptionalAnnotationNameParameter predictedLabelsAnnotation = new OptionalAnnotationNameParameter("Predicted class", true);
    private OptionalDataAnnotationNameParameter labelProbabilitiesAnnotation = new OptionalDataAnnotationNameParameter("Label probabilities", true);

    public PredictClassifierAlgorithm(JIPipeNodeInfo info) {
        super(info);
        scale2DAlgorithm = JIPipe.createNode(TransformScale2DAlgorithm.class);
        scale2DAlgorithm.setScaleMode(ScaleMode.Fit);
        registerSubParameter(scale2DAlgorithm);
    }

    public PredictClassifierAlgorithm(PredictClassifierAlgorithm other) {
        super(other);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.scale2DAlgorithm = new TransformScale2DAlgorithm(other.scale2DAlgorithm);
        this.overrideDevices = new OptionalDeepLearningDeviceEnvironment(other.overrideDevices);
        this.scaleToModelSize = other.scaleToModelSize;
        this.normalization = other.normalization;
        this.predictedLabelsAnnotation = new OptionalAnnotationNameParameter(other.predictedLabelsAnnotation);
        this.labelProbabilitiesAnnotation = new OptionalDataAnnotationNameParameter(other.labelProbabilitiesAnnotation);
        registerSubParameter(scale2DAlgorithm);
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

    @JIPipeDocumentation(name = "Predicted labels annotation", description = "If enabled, the incoming images are annotated with the best (highest probability) predicted label.")
    @JIPipeParameter("predicted-labels-annotation")
    public OptionalAnnotationNameParameter getPredictedLabelsAnnotation() {
        return predictedLabelsAnnotation;
    }

    @JIPipeParameter("predicted-labels-annotation")
    public void setPredictedLabelsAnnotation(OptionalAnnotationNameParameter predictedLabelsAnnotation) {
        this.predictedLabelsAnnotation = predictedLabelsAnnotation;
    }

    @JIPipeDocumentation(name = "Label probabilities annotation", description = "If enabled, the output will be annotated with a table of the label probabilities")
    @JIPipeParameter("label-probabilities-annotation")
    public OptionalDataAnnotationNameParameter getLabelProbabilitiesAnnotation() {
        return labelProbabilitiesAnnotation;
    }

    @JIPipeParameter("label-probabilities-annotation")
    public void setLabelProbabilitiesAnnotation(OptionalDataAnnotationNameParameter labelProbabilitiesAnnotation) {
        this.labelProbabilitiesAnnotation = labelProbabilitiesAnnotation;
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot inputModelSlot = getInputSlot("Model");
        JIPipeDataSlot inputRawImageSlot = getInputSlot("Input");
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

            Path predictionsDirectory = workDirectory.resolve("predict");
            Path rawsDirectory = workDirectory.resolve("raw");
            try {
                Files.createDirectories(predictionsDirectory);
                Files.createDirectories(rawsDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Map<String, Integer> inputDataToRowMapping = new HashMap<>();
            {
                int imageCounter = 0;
                Set<Integer> inputRows = dataBatch.getInputSlotRows().get(inputRawImageSlot);
                for (Integer imageIndex : inputRows) {
                    JIPipeProgressInfo imageProgress = modelProgress.resolveAndLog("Write inputs", imageCounter++, inputRows.size());
                    ImagePlusData raw = inputRawImageSlot.getData(imageIndex, ImagePlus3DGreyscaleData.class, imageProgress);

                    if (raw.hasLoadedImage() || isScaleToModelSize()) {
                        Path rawPath = rawsDirectory.resolve(imageCounter + "_img.tif");

                        ImagePlus rawImage = isScaleToModelSize() ? DeepLearningUtils.scaleToModel(raw.getImage(),
                                inputModel.getModelConfiguration(),
                                getScale2DAlgorithm(),
                                true,
                                true,
                                modelProgress) : raw.getImage();

                        IJ.saveAsTiff(rawImage, rawPath.toString());
                        inputDataToRowMapping.put(rawPath.toString(), imageIndex);
                    } else {
                        raw.saveTo(rawsDirectory, imageCounter + "_img", true, imageProgress);
                        inputDataToRowMapping.put(rawsDirectory.resolve(imageCounter + "_img").toString(), imageIndex);
                    }
                }
            }

            // Save model according to standard interface
            inputModel.saveTo(workDirectory, "", false, modelProgress);

            // Generate configuration
            DeepLearningPredictionConfiguration predictionConfiguration = new DeepLearningPredictionConfiguration();
            predictionConfiguration.setOutputPath(predictionsDirectory);
            predictionConfiguration.setInputImagesPattern(rawsDirectory + "/*.tif");
            predictionConfiguration.setInputModelPath(workDirectory.resolve("model.hdf5"));
            predictionConfiguration.setNormalization(normalization);
            try {
                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(workDirectory.resolve("predict-config.json").toFile(), predictionConfiguration);
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
            arguments.add("predict");
            arguments.add("--config");
            arguments.add(workDirectory.resolve("predict-config.json").toString());
            arguments.add("--model-config");
            arguments.add(workDirectory.resolve("model-config.json").toString());
            arguments.add("--device-config");
            arguments.add(deviceConfigurationPath.toString());

            if (DeepLearningSettings.getInstance().getDeepLearningToolkit().needsInstall())
                DeepLearningSettings.getInstance().getDeepLearningToolkit().install(modelProgress);
            PythonUtils.runPython(arguments.toArray(new String[0]), overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() :
                            DeepLearningSettings.getInstance().getPythonEnvironment(),
                    Collections.singletonList(DeepLearningSettings.getInstance().getDeepLearningToolkit().getLibraryDirectory().toAbsolutePath()), modelProgress);

            // Fetch prediction result
            Path predictionResultTableFile = PathUtils.findFileByExtensionIn(predictionsDirectory, ".csv");
            ResultsTableData predictionResult = ResultsTableData.fromCSV(predictionResultTableFile);

            for (int row = 0; row < predictionResult.getRowCount(); row++) {
                String inputImagePath = predictionResult.getValueAsString(row, "sample");
                int inputRow = inputDataToRowMapping.get(inputImagePath);

                double bestClassProbability = -1;
                int bestClass = -1;

                ResultsTableData rowMetadata = new ResultsTableData();
                rowMetadata.addRow();
                for (JIPipeAnnotation annotation : getInputSlot("Input").getAnnotations(inputRow)) {
                    rowMetadata.setValueAt(annotation.getValue(), 0, annotation.getName());
                }
                for (int i = 0; i < inputModel.getModelConfiguration().getNumClasses(); i++) {
                    String columnName = "probability_class_" + i;
                    double probability;
                    if(predictionResult.getColumnNames().contains(columnName)) {
                        probability = predictionResult.getValueAsDouble(row, columnName);
                    }
                    else {
                        probability = 0;
                    }
                    rowMetadata.setValueAt(probability, 0, "probability_class_" + i);
                    if(probability > bestClassProbability) {
                        bestClassProbability = probability;
                        bestClass = i;
                    }
                }

                List<JIPipeAnnotation> annotations = new ArrayList<>(getInputSlot("Input").getAnnotations(inputRow));
                List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();

                predictedLabelsAnnotation.addAnnotationIfEnabled(annotations, "" + bestClass);
                labelProbabilitiesAnnotation.addAnnotationIfEnabled(dataAnnotations, rowMetadata);

                dataBatch.addOutputData(getFirstOutputSlot(),
                        getInputSlot("Input").getVirtualData(inputRow),
                        annotations,
                        JIPipeAnnotationMergeStrategy.OverwriteExisting,
                        dataAnnotations,
                        JIPipeDataAnnotationMergeStrategy.OverwriteExisting);
            }

            if (cleanUpAfterwards) {
                PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
            }
        }
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

    @JIPipeDocumentation(name = "Scaling", description = "The following settings determine how the image is scaled in 2D if it does not fit to the size the model is designed for.")
    @JIPipeParameter(value = "scale-algorithm",
            collapsed = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/transform-scale.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/transform-scale.png")
    public TransformScale2DAlgorithm getScale2DAlgorithm() {
        return scale2DAlgorithm;
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
