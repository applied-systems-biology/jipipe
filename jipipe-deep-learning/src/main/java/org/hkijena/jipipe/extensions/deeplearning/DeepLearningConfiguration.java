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

package org.hkijena.jipipe.extensions.deeplearning;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * POJO class for storing the configuration
 */
public class DeepLearningConfiguration {
    private Path modelPath = null;
    private Path inputDir = null;
    private Path labelDir = null;
    private Path dataInfo = null;
    private Path checkpointDir = null;
    private int maxEpochs = 1000;
    private int kernelSize = 7;
    private RegularizationMethod regularizationMethod = RegularizationMethod.GaussianDropout;
    private double regularizationLambda = 0.1;
    private int batchSize = 32;
    private int numFilters = 64;
    private int imgSize = 512;
    private int augmentationFactor = 5;
    private double learningRate = 0.0001;
    private double beta1 = 0.5;
    private MonitorLoss monitorLoss = MonitorLoss.val_loss;
    private int numClasses = 2;
    private double validationSplit = 0.85;
    private double segmentationThreshold = 0.9;
    private int numKFolders = 9;

    @JsonGetter("model_path")
    public Path getModelPath() {
        return modelPath;
    }

    @JsonSetter("model_path")
    public void setModelPath(Path modelPath) {
        this.modelPath = modelPath;
    }

    @JsonGetter("input_dir")
    public Path getInputDir() {
        return inputDir;
    }

    @JsonSetter("input_dir")
    public void setInputDir(Path inputDir) {
        this.inputDir = inputDir;
    }

    @JsonGetter("label_dir")
    public Path getLabelDir() {
        return labelDir;
    }

    @JsonSetter("label_dir")
    public void setLabelDir(Path labelDir) {
        this.labelDir = labelDir;
    }

    @JsonGetter("data_info")
    public Path getDataInfo() {
        return dataInfo;
    }

    @JsonSetter("data_info")
    public void setDataInfo(Path dataInfo) {
        this.dataInfo = dataInfo;
    }

    @JsonGetter("checkpoint_dir")
    public Path getCheckpointDir() {
        return checkpointDir;
    }

    @JsonSetter("checkpoint_dir")
    public void setCheckpointDir(Path checkpointDir) {
        this.checkpointDir = checkpointDir;
    }

    @JsonGetter("max_epochs")
    public int getMaxEpochs() {
        return maxEpochs;
    }

    @JsonSetter("max_epochs")
    public void setMaxEpochs(int maxEpochs) {
        this.maxEpochs = maxEpochs;
    }

    @JsonGetter("kernel_size")
    public int getKernelSize() {
        return kernelSize;
    }

    @JsonSetter("kernel_size")
    public void setKernelSize(int kernelSize) {
        this.kernelSize = kernelSize;
    }

    @JsonGetter("regularization_method")
    public RegularizationMethod getRegularizationMethod() {
        return regularizationMethod;
    }

    @JsonSetter("regularization_method")
    public void setRegularizationMethod(RegularizationMethod regularizationMethod) {
        this.regularizationMethod = regularizationMethod;
    }

    @JsonGetter("regularization_lambda")
    public double getRegularizationLambda() {
        return regularizationLambda;
    }

    @JsonSetter("regularization_lambda")
    public void setRegularizationLambda(double regularizationLambda) {
        this.regularizationLambda = regularizationLambda;
    }

    @JsonGetter("batch_size")
    public int getBatchSize() {
        return batchSize;
    }

    @JsonSetter("batch_size")
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @JsonGetter("n_filters")
    public int getNumFilters() {
        return numFilters;
    }

    @JsonSetter("n_filters")
    public void setNumFilters(int numFilters) {
        this.numFilters = numFilters;
    }

    @JsonGetter("img_size")
    public int getImgSize() {
        return imgSize;
    }

    @JsonSetter("img_size")
    public void setImgSize(int imgSize) {
        this.imgSize = imgSize;
    }

    @JsonGetter("augmentation_factor")
    public int getAugmentationFactor() {
        return augmentationFactor;
    }

    @JsonSetter("augmentation_factor")
    public void setAugmentationFactor(int augmentationFactor) {
        this.augmentationFactor = augmentationFactor;
    }

    @JsonGetter("learning_rate")
    public double getLearningRate() {
        return learningRate;
    }

    @JsonSetter("learning_rate")
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    @JsonGetter("beta1")
    public double getBeta1() {
        return beta1;
    }

    @JsonSetter("beta1")
    public void setBeta1(double beta1) {
        this.beta1 = beta1;
    }

    @JsonGetter("monitor_loss")
    public MonitorLoss getMonitorLoss() {
        return monitorLoss;
    }

    @JsonSetter("monitor_loss")
    public void setMonitorLoss(MonitorLoss monitorLoss) {
        this.monitorLoss = monitorLoss;
    }

    @JsonGetter("n_classes")
    public int getNumClasses() {
        return numClasses;
    }

    @JsonSetter("n_classes")
    public void setNumClasses(int numClasses) {
        this.numClasses = numClasses;
    }

    @JsonGetter("validation_split")
    public double getValidationSplit() {
        return validationSplit;
    }

    @JsonSetter("validation_split")
    public void setValidationSplit(double validationSplit) {
        this.validationSplit = validationSplit;
    }

    @JsonGetter("segmentation_threshold")
    public double getSegmentationThreshold() {
        return segmentationThreshold;
    }

    @JsonSetter("segmentation_threshold")
    public void setSegmentationThreshold(double segmentationThreshold) {
        this.segmentationThreshold = segmentationThreshold;
    }

    @JsonGetter("num_K_folds")
    public int getNumKFolders() {
        return numKFolders;
    }

    @JsonSetter("num_K_folds")
    public void setNumKFolders(int numKFolders) {
        this.numKFolders = numKFolders;
    }
}
