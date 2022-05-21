package org.hkijena.jipipe.extensions.ijweka.nodes;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierParameter;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierSettings;
import org.hkijena.jipipe.extensions.ijweka.parameters.collections.WekaFeature3DSettings;
import org.hkijena.jipipe.extensions.ijweka.parameters.features.WekaFeature3D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;

import java.util.ArrayList;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Train Weka model from mask (3D)", description = "Trains a Weka model on 3D image data that classified into two classes via a mask.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@JIPipeInputSlot(value = ImagePlus3DData.class, slotName = "Image", description = "Image on which the training should be applied", autoCreate = true)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleMaskData.class, slotName = "Mask", description = "Mask that marks foreground objects via white pixels (255) and the background with black pixels (0)", autoCreate = true)
@JIPipeOutputSlot(value = WekaModelData.class, slotName = "Trained model", description = "The model", autoCreate = true)
public class WekaTrainingMask3DAlgorithm extends JIPipeIteratingAlgorithm {

    private WekaFeature3DSettings featureSettings = new WekaFeature3DSettings();

    private WekaClassifierSettings classifierSettings = new WekaClassifierSettings();


    public WekaTrainingMask3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(featureSettings);
        registerSubParameter(classifierSettings);
    }
    public WekaTrainingMask3DAlgorithm(WekaTrainingMask3DAlgorithm other) {
        super(other);
        this.featureSettings = other.featureSettings;
        registerSubParameter(featureSettings);
        this.classifierSettings = new WekaClassifierSettings(other.classifierSettings);
        registerSubParameter(classifierSettings);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
                // Setup parameters
        ArrayList<String> selectedFeatureNames = featureSettings.getTrainingFeatures().getValues().stream().map(WekaFeature3D::name).collect(Collectors.toCollection(ArrayList::new));
        Classifier classifier = (new WekaClassifierParameter(getClassifierSettings().getClassifier())).getClassifier(); // This will make a copy of the classifier

        // Apply the training
        ImagePlus trainingImage = dataBatch.getInputData("Image", ImagePlus3DData.class, progressInfo).getDuplicateImage();
        ImagePlus maskImage = dataBatch.getInputData("Mask", ImagePlus3DGreyscaleMaskData.class, progressInfo).getDuplicateImage();

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

        try(IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo.resolve("Weka"))) {
            if (!wekaSegmentation.trainClassifier()) {
                throw new RuntimeException("Weka training failed!");
            }
        }

        dataBatch.addOutputData("Trained model", new WekaModelData(wekaSegmentation), progressInfo);
    }

    @JIPipeDocumentation(name = "Classifier", description = "Settings for the classifier")
    @JIPipeParameter("classifier-settings")
    public WekaClassifierSettings getClassifierSettings() {
        return classifierSettings;
    }

    @JIPipeDocumentation(name = "Features", description = "Settings regarding the features used for training")
    @JIPipeParameter("feature-settings")
    public WekaFeature3DSettings getFeatureSettings() {
        return featureSettings;
    }

}
