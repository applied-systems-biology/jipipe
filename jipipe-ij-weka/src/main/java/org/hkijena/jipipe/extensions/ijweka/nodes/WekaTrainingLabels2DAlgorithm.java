package org.hkijena.jipipe.extensions.ijweka.nodes;

import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierParameter;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierSettings;
import org.hkijena.jipipe.extensions.ijweka.parameters.collections.WekaFeature2DSettings;
import org.hkijena.jipipe.extensions.ijweka.parameters.features.WekaFeature2D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;

import java.util.ArrayList;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Train Weka model from labels (2D)", description = "Trains a Weka model on 2D image data that classified by a label image. " +
        "Can only train on a single image. " +
        "Use the multi-image node if you want to train from multiple images.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@JIPipeInputSlot(value = ImagePlus2DData.class, slotName = "Image", description = "Image on which the training should be applied", autoCreate = true)
@JIPipeInputSlot(value = ImagePlus2DGreyscaleData.class, slotName = "Labels", description = "A labels image", autoCreate = true)
@JIPipeOutputSlot(value = WekaModelData.class, slotName = "Trained model", description = "The model", autoCreate = true)
public class WekaTrainingLabels2DAlgorithm extends JIPipeIteratingAlgorithm {

    private WekaFeature2DSettings featureSettings = new WekaFeature2DSettings();

    private WekaClassifierSettings classifierSettings = new WekaClassifierSettings();


    public WekaTrainingLabels2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(featureSettings);
        registerSubParameter(classifierSettings);
    }

    public WekaTrainingLabels2DAlgorithm(WekaTrainingLabels2DAlgorithm other) {
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
        ImagePlus labelImage = iterationStep.getInputData("Labels", ImagePlus2DGreyscaleData.class, progressInfo).getDuplicateImage();

        int numLabels = LabelImages.findAllLabels(labelImage).length;
        if (labelImage.getProcessor().getStats().min == 0) {
            numLabels += 1;
        }

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

            while (wekaSegmentation.getNumOfClasses() < numLabels) {
                wekaSegmentation.addClass();
            }

            wekaSegmentation.addLabeledData(trainingImage, labelImage);

            if (!wekaSegmentation.trainClassifier()) {
                throw new RuntimeException("Weka training failed!");
            }

            iterationStep.addOutputData("Trained model", new WekaModelData(wekaSegmentation), progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Classifier", description = "Settings for the classifier")
    @JIPipeParameter("classifier-settings")
    public WekaClassifierSettings getClassifierSettings() {
        return classifierSettings;
    }

    @JIPipeDocumentation(name = "Features", description = "Settings regarding the features used for training")
    @JIPipeParameter("feature-settings")
    public WekaFeature2DSettings getFeatureSettings() {
        return featureSettings;
    }

}
