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

package org.hkijena.jipipe.extensions.deeplearning.configs;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.deeplearning.enums.MonitorLoss;
import org.hkijena.jipipe.extensions.deeplearning.enums.NormalizationMethod;
import org.hkijena.jipipe.extensions.parameters.primitives.NumberParameterSettings;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DeepLearningTrainingConfiguration implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private Path inputModelPath = Paths.get("");
    private Path outputModelPath = Paths.get("");
    private Path outputModelJsonPath = Paths.get("");
    private Path logDir = Paths.get("");
    private String inputImagesPattern = "raw/*.tif";
    private String inputLabelsPattern = "labels/*.tif";
    private int maxEpochs = 1000;
    private int batchSize = 32;
    private double learningRate = 0.0001;
    private MonitorLoss monitorLoss = MonitorLoss.val_loss;
    private double validationSplit = 0.85;
    private int augmentationFactor = 5;
    private NormalizationMethod normalization = NormalizationMethod.zero_one;

    public DeepLearningTrainingConfiguration() {
    }

    public DeepLearningTrainingConfiguration(DeepLearningTrainingConfiguration other) {
        this.inputModelPath = other.inputModelPath;
        this.outputModelPath = other.outputModelPath;
        this.outputModelJsonPath = other.outputModelJsonPath;
        this.inputImagesPattern = other.inputImagesPattern;
        this.inputLabelsPattern = other.inputLabelsPattern;
        this.maxEpochs = other.maxEpochs;
        this.batchSize = other.batchSize;
        this.learningRate = other.learningRate;
        this.monitorLoss = other.monitorLoss;
        this.validationSplit = other.validationSplit;
        this.augmentationFactor = other.augmentationFactor;
        this.normalization = other.normalization;
        this.logDir = other.logDir;
    }

    @JsonGetter("log_dir")
    public Path getLogDir() {
        return logDir;
    }

    @JsonSetter("log_dir")
    public void setLogDir(Path logDir) {
        this.logDir = logDir;
    }

    @JsonGetter("normalization")
    public NormalizationMethod getNormalization() {
        return normalization;
    }

    @JsonSetter("normalization")
    public void setNormalization(NormalizationMethod normalization) {
        this.normalization = normalization;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JsonGetter("input_model_path")
    public Path getInputModelPath() {
        return inputModelPath;
    }

    @JsonSetter("input_model_path")
    public void setInputModelPath(Path inputModelPath) {
        this.inputModelPath = inputModelPath;
    }

    @JsonGetter("output_model_path")
    public Path getOutputModelPath() {
        return outputModelPath;
    }

    @JsonSetter("output_model_path")
    public void setOutputModelPath(Path outputModelPath) {
        this.outputModelPath = outputModelPath;
    }

    @JsonGetter("input_dir")
    public String getInputImagesPattern() {
        return inputImagesPattern;
    }

    @JsonSetter("input_dir")
    public void setInputImagesPattern(String inputImagesPattern) {
        this.inputImagesPattern = inputImagesPattern;
    }

    @JsonGetter("label_dir")
    public String getInputLabelsPattern() {
        return inputLabelsPattern;
    }

    @JsonSetter("label_dir")
    public void setInputLabelsPattern(String inputLabelsPattern) {
        this.inputLabelsPattern = inputLabelsPattern;
    }

    @JIPipeDocumentation(name = "Max epochs", description = "The maximum number of epochs")
    @JsonGetter("max_epochs")
    @JIPipeParameter("max-epochs")
    public int getMaxEpochs() {
        return maxEpochs;
    }

    @JIPipeParameter("max-epochs")
    @JsonSetter("max_epochs")
    public void setMaxEpochs(int maxEpochs) {
        this.maxEpochs = maxEpochs;
    }

    @JIPipeDocumentation(name = "Batch size", description = "The batch size")
    @JIPipeParameter("batch-size")
    @JsonGetter("batch_size")
    public int getBatchSize() {
        return batchSize;
    }

    @JIPipeParameter("batch-size")
    @JsonSetter("batch_size")
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @JIPipeDocumentation(name = "Learning rate", description = "The learning rate")
    @JsonGetter("learning_rate")
    @NumberParameterSettings(step = 0.0001)
    @JIPipeParameter("learning-rate")
    public double getLearningRate() {
        return learningRate;
    }

    @JIPipeParameter("learning-rate")
    @JsonSetter("learning_rate")
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    @JIPipeDocumentation(name = "Monitor loss", description = "The monitor loss")
    @JIPipeParameter("monitor-loss")
    @JsonGetter("monitor_loss")
    public MonitorLoss getMonitorLoss() {
        return monitorLoss;
    }

    @JIPipeParameter("monitor-loss")
    @JsonSetter("monitor_loss")
    public void setMonitorLoss(MonitorLoss monitorLoss) {
        this.monitorLoss = monitorLoss;
    }

    @JIPipeDocumentation(name = "Validation split", description = "Ratio between training and validation data")
    @JIPipeParameter("validation-split")
    @JsonGetter("validation_split")
    public double getValidationSplit() {
        return validationSplit;
    }

    @JIPipeParameter("validation-split")
    @JsonSetter("validation_split")
    public void setValidationSplit(double validationSplit) {
        this.validationSplit = validationSplit;
    }

    @JIPipeDocumentation(name = "Augmentation factor", description = "Controls how much real-time augmentation is applied")
    @JIPipeParameter("augmentation-factor")
    @JsonGetter("augmentation_factor")
    public int getAugmentationFactor() {
        return augmentationFactor;
    }

    @JIPipeParameter("augmentation-factor")
    @JsonSetter("augmentation_factor")
    public void setAugmentationFactor(int augmentationFactor) {
        this.augmentationFactor = augmentationFactor;
    }

    @JsonGetter("output_model_json_path")
    public Path getOutputModelJsonPath() {
        return outputModelJsonPath;
    }

    @JsonSetter("output_model_json_path")
    public void setOutputModelJsonPath(Path outputModelJsonPath) {
        this.outputModelJsonPath = outputModelJsonPath;
    }
}
