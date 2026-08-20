/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.cellpose.parameters.cp4;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;

public class Cellpose4SegmentationTrainingTweaksSettings extends AbstractJIPipeParameterCollection {

    private double learningRate = 0.00001;
    private double weightDecay = 0.1;
    private int batchSize = 1;
    private int minTrainMasks = 5;
    private OptionalIntegerParameter numTrainingImagesPerEpoch = new OptionalIntegerParameter(false, 10);
    private OptionalIntegerParameter numTestImagesPerEpoch = new OptionalIntegerParameter(false, 10);
    private boolean generateConnectedComponents = true;

    public Cellpose4SegmentationTrainingTweaksSettings() {
    }

    public Cellpose4SegmentationTrainingTweaksSettings(Cellpose4SegmentationTrainingTweaksSettings other) {
        this.learningRate = other.learningRate;
        this.batchSize = other.batchSize;
        this.minTrainMasks = other.minTrainMasks;
        this.weightDecay = other.weightDecay;
        this.generateConnectedComponents = other.generateConnectedComponents;
        this.numTrainingImagesPerEpoch = new OptionalIntegerParameter(other.numTrainingImagesPerEpoch);
        this.numTestImagesPerEpoch = new OptionalIntegerParameter(other.numTestImagesPerEpoch);
    }

    @SetJIPipeDocumentation(name = "Training images per epoch", description = "Allows to override the number of training images per epoch. Defaults to all images.")
    @JIPipeParameter("num-training-images-per-epoch")
    public OptionalIntegerParameter getNumTrainingImagesPerEpoch() {
        return numTrainingImagesPerEpoch;
    }

    @JIPipeParameter("num-training-images-per-epoch")
    public void setNumTrainingImagesPerEpoch(OptionalIntegerParameter numTrainingImagesPerEpoch) {
        this.numTrainingImagesPerEpoch = numTrainingImagesPerEpoch;
    }

    @SetJIPipeDocumentation(name = "Test images per epoch", description = "Allows to override the number of test images per epoch. Defaults to all images.")
    @JIPipeParameter("num-test-images-per-epoch")
    public OptionalIntegerParameter getNumTestImagesPerEpoch() {
        return numTestImagesPerEpoch;
    }

    @JIPipeParameter("num-test-images-per-epoch")
    public void setNumTestImagesPerEpoch(OptionalIntegerParameter numTestImagesPerEpoch) {
        this.numTestImagesPerEpoch = numTestImagesPerEpoch;
    }

    @SetJIPipeDocumentation(name = "Weight decay", description = "The weight decay. Default is 0.1 (recommended for Cellpose 4 transformer models).")
    @JIPipeParameter("weight-decay")
    public double getWeightDecay() {
        return weightDecay;
    }

    @JIPipeParameter("weight-decay")
    public void setWeightDecay(double weightDecay) {
        this.weightDecay = weightDecay;
    }

    @SetJIPipeDocumentation(name = "Generate connected components", description = "If enabled, JIPipe will apply a connected component labeling to the annotated masks. If disabled, Cellpose is provided with " +
            "the labels as-is, which might result in issues with the training.")
    @JIPipeParameter("generate-connected-components")
    public boolean isGenerateConnectedComponents() {
        return generateConnectedComponents;
    }

    @JIPipeParameter("generate-connected-components")
    public void setGenerateConnectedComponents(boolean generateConnectedComponents) {
        this.generateConnectedComponents = generateConnectedComponents;
    }

    @SetJIPipeDocumentation(name = "Minimum number of labels per image", description = "Minimum number of masks an image must have to use in training set. " +
            "This value is by default 5 in the original Cellpose tool.")
    @JIPipeParameter("min-train-masks")
    public int getMinTrainMasks() {
        return minTrainMasks;
    }

    @JIPipeParameter("min-train-masks")
    public void setMinTrainMasks(int minTrainMasks) {
        this.minTrainMasks = minTrainMasks;
    }

    @SetJIPipeDocumentation(name = "Learning rate", description = "Learning rate for training. Default is 1e-5 (recommended for fine-tuning Cellpose 4 transformer models). " +
            "Higher learning rates can destabilize the pre-trained transformer backbone.")
    @JIPipeParameter("learning-rate")
    public double getLearningRate() {
        return learningRate;
    }

    @JIPipeParameter("learning-rate")
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    @SetJIPipeDocumentation(name = "Batch size", description = "Training batch size. Default is 1. " +
            "Transformer models require significant GPU memory, so a batch size of 1 is recommended.")
    @JIPipeParameter("batch-size")
    public int getBatchSize() {
        return batchSize;
    }

    @JIPipeParameter("batch-size")
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
