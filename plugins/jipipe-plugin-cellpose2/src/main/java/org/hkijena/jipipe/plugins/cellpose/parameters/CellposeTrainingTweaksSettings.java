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

package org.hkijena.jipipe.plugins.cellpose.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class CellposeTrainingTweaksSettings extends AbstractJIPipeParameterCollection {

    private double learningRate = 0.2;
    private double weightDecay = 1e-05;
    private int batchSize = 8;
    private int minTrainMasks = 1;
    private boolean useResidualConnections = true;
    private boolean useStyleVector = true;
    private boolean concatenateDownsampledLayers = false;

    private boolean generateConnectedComponents = true;

    public CellposeTrainingTweaksSettings() {
    }

    public CellposeTrainingTweaksSettings(CellposeTrainingTweaksSettings other) {
        this.learningRate = other.learningRate;
        this.batchSize = other.batchSize;
        this.useResidualConnections = other.useResidualConnections;
        this.useStyleVector = other.useStyleVector;
        this.concatenateDownsampledLayers = other.concatenateDownsampledLayers;
        this.minTrainMasks = other.minTrainMasks;
        this.weightDecay = other.weightDecay;
        this.generateConnectedComponents = other.generateConnectedComponents;
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

    @SetJIPipeDocumentation(name = "Use residual connections")
    @JIPipeParameter("use-residual-connections")
    public boolean isUseResidualConnections() {
        return useResidualConnections;
    }

    @JIPipeParameter("use-residual-connections")
    public void setUseResidualConnections(boolean useResidualConnections) {
        this.useResidualConnections = useResidualConnections;
    }

    @SetJIPipeDocumentation(name = "Use style vector")
    @JIPipeParameter("use-style-vector")
    public boolean isUseStyleVector() {
        return useStyleVector;
    }

    @JIPipeParameter("use-style-vector")
    public void setUseStyleVector(boolean useStyleVector) {
        this.useStyleVector = useStyleVector;
    }

    @SetJIPipeDocumentation(name = "Concatenate downsampled layers",
            description = "Concatenate downsampled layers with upsampled layers (off by default which means they are added)")
    @JIPipeParameter("concatenate-downsampled-layers")
    public boolean isConcatenateDownsampledLayers() {
        return concatenateDownsampledLayers;
    }

    @JIPipeParameter("concatenate-downsampled-layers")
    public void setConcatenateDownsampledLayers(boolean concatenateDownsampledLayers) {
        this.concatenateDownsampledLayers = concatenateDownsampledLayers;
    }
}
