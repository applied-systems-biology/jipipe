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

package org.hkijena.jipipe.plugins.cellpose.parameters.cp3;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;

public class Cellpose3TrainingTweaksSettings extends AbstractJIPipeParameterCollection {

    private double learningRate = 0.2;
    private double weightDecay = 1e-05;
    private int batchSize = 8;
    private int minTrainMasks = 1;
    private OptionalIntegerParameter useSGD = new OptionalIntegerParameter(false, 1);

    private OptionalIntegerParameter numTrainingImagesPerEpoch = new OptionalIntegerParameter(false, 10);
    private OptionalIntegerParameter numTestImagesPerEpoch = new OptionalIntegerParameter(false, 10);

    private boolean generateConnectedComponents = true;

    public Cellpose3TrainingTweaksSettings() {
    }

    public Cellpose3TrainingTweaksSettings(Cellpose3TrainingTweaksSettings other) {
        this.learningRate = other.learningRate;
        this.batchSize = other.batchSize;
        this.minTrainMasks = other.minTrainMasks;
        this.weightDecay = other.weightDecay;
        this.generateConnectedComponents = other.generateConnectedComponents;
        this.useSGD = other.useSGD;
        this.numTrainingImagesPerEpoch = new OptionalIntegerParameter(other.numTrainingImagesPerEpoch);
        this.numTestImagesPerEpoch = new OptionalIntegerParameter(other.numTestImagesPerEpoch);
    }

    @SetJIPipeDocumentation(name = "Use SGD", description = "Enabling this parameter may be useful for re-training. It is recommended to train with more epoch (e.g. 300 instead of 100) with this parameter enabled.")
    @JIPipeParameter("use-sgd")
    public OptionalIntegerParameter isUseSGD() {
        return useSGD;
    }

    @JIPipeParameter("use-sgd")
    public void setUseSGD(OptionalIntegerParameter useSGD) {
        this.useSGD = useSGD;
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

    @SetJIPipeDocumentation(name = "Weight decay", description = "The weight decay")
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

    @SetJIPipeDocumentation(name = "Learning rate")
    @JIPipeParameter("learning-rate")
    public double getLearningRate() {
        return learningRate;
    }

    @JIPipeParameter("learning-rate")
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    @SetJIPipeDocumentation(name = "Batch size")
    @JIPipeParameter("batch-size")
    public int getBatchSize() {
        return batchSize;
    }

    @JIPipeParameter("batch-size")
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
