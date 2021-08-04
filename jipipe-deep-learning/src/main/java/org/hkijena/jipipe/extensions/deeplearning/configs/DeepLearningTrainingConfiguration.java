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
import org.hkijena.jipipe.extensions.deeplearning.enums.NormalizationMethod;

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
    private String validationImagesPattern = "validation_raw/*.tif";
    private String validationLabelsPattern = "validation_raw/*.tif";
    private int maxEpochs = 100;
    private int batchSize = 32;
    private double validationSplit = 0.80;
    private int augmentationFactor = 3;
    private NormalizationMethod normalization = NormalizationMethod.zero_one;
    private boolean useElasticTransform = true;
    private boolean showPlots = false;

    public DeepLearningTrainingConfiguration() {
    }

    public DeepLearningTrainingConfiguration(DeepLearningTrainingConfiguration other) {
        this.inputModelPath = other.inputModelPath;
        this.outputModelPath = other.outputModelPath;
        this.outputModelJsonPath = other.outputModelJsonPath;
        this.inputImagesPattern = other.inputImagesPattern;
        this.inputLabelsPattern = other.inputLabelsPattern;
        this.validationImagesPattern = other.validationImagesPattern;
        this.validationLabelsPattern = other.validationLabelsPattern;
        this.maxEpochs = other.maxEpochs;
        this.batchSize = other.batchSize;
        this.validationSplit = other.validationSplit;
        this.augmentationFactor = other.augmentationFactor;
        this.normalization = other.normalization;
        this.logDir = other.logDir;
        this.useElasticTransform = other.useElasticTransform;
        this.showPlots = other.showPlots;
    }

    @JIPipeDocumentation(name = "Use elastic transform", description = "Use an elastic transformation for data augmentation purpose")
    @JIPipeParameter("use-use_elastic_transformation")
    @JsonGetter("use_elastic_transformation")
    public boolean isUseElasticTransform() {
        return useElasticTransform;
    }

    @JIPipeParameter("use-use_elastic_transformation")
    @JsonSetter("use_elastic_transformation")
    public void setUseElasticTransform(boolean useElasticTransform) {
        this.useElasticTransform = useElasticTransform;
    }

    @JsonGetter("log_dir")
    public Path getLogDir() {
        return logDir;
    }

    @JsonSetter("log_dir")
    public void setLogDir(Path logDir) {
        this.logDir = logDir;
    }

    @JsonGetter("show_plots")
    public boolean isShowPlots() {
        return showPlots;
    }

    @JsonSetter("show_plots")
    public void setShowPlots(boolean showPlots) {
        this.showPlots = showPlots;
    }

    @JIPipeDocumentation(name = "Normalization", description = "Normalize the input data to the specified interval: [0,255] => [0,1]")
    @JIPipeParameter("normalization")
    @JsonGetter("normalization")
    public NormalizationMethod getNormalization() {
        return normalization;
    }

    @JIPipeParameter("normalization")
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

    @JsonGetter("input_validation_dir")
    public String getValidationImagesPattern() {
        return validationImagesPattern;
    }

    @JsonSetter("input_validation_dir")
    public void setValidationImagesPattern(String validationImagesPattern) {
        this.validationImagesPattern = validationImagesPattern;
    }

    @JsonGetter("label_validation_dir")
    public String getValidationLabelsPattern() {
        return validationLabelsPattern;
    }

    @JsonSetter("label_validation_dir")
    public void setValidationLabelsPattern(String validationLabelsPattern) {
        this.validationLabelsPattern = validationLabelsPattern;
    }

    @JIPipeDocumentation(name = "Max epochs", description = "Maximum number of training epochs")
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

    @JIPipeDocumentation(name = "Batch size", description = "The batch size defines the number of samples that will be propagated through the network")
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

    @JIPipeDocumentation(name = "Validation split", description = "Fraction of the training data to be used as validation data (0.8 = 80% used for training)")
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

    @JIPipeDocumentation(name = "Augmentation factor", description = "Multiplier by which the whole data set for the training is augmented. Set to zero or one for no augmentation.")
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
