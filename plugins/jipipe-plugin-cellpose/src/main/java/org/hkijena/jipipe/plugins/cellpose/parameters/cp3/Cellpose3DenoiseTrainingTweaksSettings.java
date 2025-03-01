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

public class Cellpose3DenoiseTrainingTweaksSettings extends AbstractJIPipeParameterCollection {

    private double learningRate = 0.2;
    private double weightDecay = 1e-05;

    private double lambdaPerceptual = 1;
    private double lambdaSegmentation = 1.5;
    private double lambdaReconstruction = 0;

    private OptionalIntegerParameter numTrainingImagesPerEpoch = new OptionalIntegerParameter(false, 10);
    private OptionalIntegerParameter numTestImagesPerEpoch = new OptionalIntegerParameter(false, 10);

    private boolean generateConnectedComponents = true;

    private PretrainedCellpose3SegmentationModel segmentationModel = PretrainedCellpose3SegmentationModel.cyto2;

    public Cellpose3DenoiseTrainingTweaksSettings() {
    }

    public Cellpose3DenoiseTrainingTweaksSettings(Cellpose3DenoiseTrainingTweaksSettings other) {
        this.learningRate = other.learningRate;
        this.weightDecay = other.weightDecay;
        this.lambdaPerceptual = other.lambdaPerceptual;
        this.lambdaSegmentation = other.lambdaSegmentation;
        this.lambdaReconstruction = other.lambdaReconstruction;
        this.numTrainingImagesPerEpoch = new OptionalIntegerParameter(other.numTrainingImagesPerEpoch);
        this.numTestImagesPerEpoch = new OptionalIntegerParameter(other.numTestImagesPerEpoch);
        this.generateConnectedComponents = other.generateConnectedComponents;
        this.segmentationModel = other.segmentationModel;
    }

    @SetJIPipeDocumentation(name = "Segmentation model", description = "Pretrained segmentation model used for calculating the segmentation loss. Cannot be None.")
    @JIPipeParameter("segmentation-model")
    public PretrainedCellpose3SegmentationModel getSegmentationModel() {
        return segmentationModel;
    }

    @JIPipeParameter("segmentation-model")
    public void setSegmentationModel(PretrainedCellpose3SegmentationModel segmentationModel) {
        this.segmentationModel = segmentationModel;
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


    @SetJIPipeDocumentation(name = "Learning rate")
    @JIPipeParameter("learning-rate")
    public double getLearningRate() {
        return learningRate;
    }

    @JIPipeParameter("learning-rate")
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    @SetJIPipeDocumentation(name = "Loss weight: perceptual", description = "Weighting of perceptual loss")
    @JIPipeParameter("lambda-perceptual")
    public double getLambdaPerceptual() {
        return lambdaPerceptual;
    }

    @JIPipeParameter("lambda-perceptual")
    public void setLambdaPerceptual(double lambdaPerceptual) {
        this.lambdaPerceptual = lambdaPerceptual;
    }

    @SetJIPipeDocumentation(name = "Loss weight: segmentation", description = "Weighting of segmentation loss")
    @JIPipeParameter("lambda-segmentation")
    public double getLambdaSegmentation() {
        return lambdaSegmentation;
    }

    @JIPipeParameter("lambda-segmentation")
    public void setLambdaSegmentation(double lambdaSegmentation) {
        this.lambdaSegmentation = lambdaSegmentation;
    }

    @SetJIPipeDocumentation(name = "Loss weight: reconstruction", description = "Weighting of reconstruction loss")
    @JIPipeParameter("lambda-reconstruction")
    public double getLambdaReconstruction() {
        return lambdaReconstruction;
    }

    @JIPipeParameter("lambda-reconstruction")
    public void setLambdaReconstruction(double lambdaReconstruction) {
        this.lambdaReconstruction = lambdaReconstruction;
    }
}
