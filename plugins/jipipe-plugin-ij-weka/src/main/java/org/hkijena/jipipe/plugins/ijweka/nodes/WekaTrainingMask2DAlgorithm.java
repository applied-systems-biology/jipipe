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

package org.hkijena.jipipe.plugins.ijweka.nodes;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.plugins.ijweka.parameters.WekaClassifierParameter;
import org.hkijena.jipipe.plugins.ijweka.parameters.WekaClassifierSettings;
import org.hkijena.jipipe.plugins.ijweka.parameters.collections.WekaFeature2DSettings;
import org.hkijena.jipipe.plugins.ijweka.parameters.features.WekaFeature2D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;

import java.util.ArrayList;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Train Weka model from mask (2D)", description = "Trains a Weka model on 2D image data that classified into two classes via a mask. " +
        "Can only train on a single image. " +
        "Use the multi-image node if you want to train from multiple images.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@AddJIPipeInputSlot(value = ImagePlus2DData.class, slotName = "Image", description = "Image on which the training should be applied", create = true)
@AddJIPipeInputSlot(value = ImagePlus2DGreyscaleMaskData.class, slotName = "Mask", description = "Mask that marks foreground objects via white pixels (255) and the background with black pixels (0)", create = true)
@AddJIPipeOutputSlot(value = WekaModelData.class, slotName = "Trained model", description = "The model", create = true)
public class WekaTrainingMask2DAlgorithm extends JIPipeIteratingAlgorithm {

    private WekaFeature2DSettings featureSettings = new WekaFeature2DSettings();

    private WekaClassifierSettings classifierSettings = new WekaClassifierSettings();


    public WekaTrainingMask2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(featureSettings);
        registerSubParameter(classifierSettings);
    }

    public WekaTrainingMask2DAlgorithm(WekaTrainingMask2DAlgorithm other) {
        super(other);
        this.featureSettings = other.featureSettings;
        registerSubParameter(featureSettings);
        this.classifierSettings = new WekaClassifierSettings(other.classifierSettings);
        registerSubParameter(classifierSettings);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // Setup parameters
        ArrayList<String> selectedFeatureNames = featureSettings.getTrainingFeatures().getValues().stream().map(WekaFeature2D::name).collect(Collectors.toCollection(ArrayList::new));
        Classifier classifier = (new WekaClassifierParameter(getClassifierSettings().getClassifier())).getClassifier(); // This will make a copy of the classifier

        // Apply the training
        ImagePlus trainingImage = iterationStep.getInputData("Image", ImagePlus2DData.class, progressInfo).getDuplicateImage();
        ImagePlus maskImage = iterationStep.getInputData("Mask", ImagePlus2DGreyscaleMaskData.class, progressInfo).getDuplicateImage();

        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo.resolve("Weka"))) {
            WekaSegmentation wekaSegmentation = new WekaSegmentation(trainingImage);
            wekaSegmentation.setClassifier((AbstractClassifier) classifier);
            wekaSegmentation.setDoClassBalance(getClassifierSettings().isBalanceClasses());
            wekaSegmentation.setFeatures(selectedFeatureNames);
            wekaSegmentation.setFeaturesDirty();
            wekaSegmentation.setMembraneThickness(featureSettings.getMembraneSize());
            wekaSegmentation.setMembranePatchSize(featureSettings.getMembranePatchSize());
            wekaSegmentation.setMinimumSigma(featureSettings.getMinSigma());
            wekaSegmentation.setMaximumSigma(featureSettings.getMaxSigma());
            wekaSegmentation.setUseNeighbors(featureSettings.isUseNeighbors());

            wekaSegmentation.addBinaryData(maskImage, 0, "class 2", "class 1");


            if (!wekaSegmentation.trainClassifier()) {
                throw new RuntimeException("Weka training failed!");
            }

            iterationStep.addOutputData("Trained model", new WekaModelData(wekaSegmentation), progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Classifier", description = "Settings for the classifier")
    @JIPipeParameter("classifier-settings")
    public WekaClassifierSettings getClassifierSettings() {
        return classifierSettings;
    }

    @SetJIPipeDocumentation(name = "Features", description = "Settings regarding the features used for training")
    @JIPipeParameter("feature-settings")
    public WekaFeature2DSettings getFeatureSettings() {
        return featureSettings;
    }

}
