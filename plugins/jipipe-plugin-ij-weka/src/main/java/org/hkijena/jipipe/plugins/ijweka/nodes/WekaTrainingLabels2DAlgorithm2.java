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

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.DataAnnotationQueryExpression;
import org.hkijena.jipipe.plugins.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.plugins.ijweka.parameters.WekaClassifierParameter;
import org.hkijena.jipipe.plugins.ijweka.parameters.WekaClassifierSettings;
import org.hkijena.jipipe.plugins.ijweka.parameters.collections.WekaFeature2DSettings;
import org.hkijena.jipipe.plugins.ijweka.parameters.features.WekaFeature2D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Train Weka model from labels (multiple images, 2D)", description = "Trains a Weka model on 2D image data that classified by a label image.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@AddJIPipeInputSlot(value = ImagePlus2DData.class, name = "Labeled Images", description = "Images on which the training should be applied. " +
        "Each image should have a data annotation that contains the label data.", create = true)
@AddJIPipeOutputSlot(value = WekaModelData.class, name = "Trained model", description = "The model", create = true)
public class WekaTrainingLabels2DAlgorithm2 extends JIPipeMergingAlgorithm {

    private WekaFeature2DSettings featureSettings = new WekaFeature2DSettings();
    private WekaClassifierSettings classifierSettings = new WekaClassifierSettings();
    private DataAnnotationQueryExpression labelDataAnnotation = new DataAnnotationQueryExpression("\"Label\"");


    public WekaTrainingLabels2DAlgorithm2(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(featureSettings);
        registerSubParameter(classifierSettings);
    }

    public WekaTrainingLabels2DAlgorithm2(WekaTrainingLabels2DAlgorithm2 other) {
        super(other);
        this.featureSettings = other.featureSettings;
        this.labelDataAnnotation = new DataAnnotationQueryExpression(other.labelDataAnnotation);
        registerSubParameter(featureSettings);
        this.classifierSettings = new WekaClassifierSettings(other.classifierSettings);
        registerSubParameter(classifierSettings);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // Setup parameters
        ArrayList<String> selectedFeatureNames = featureSettings.getTrainingFeatures().getValues().stream().map(WekaFeature2D::name).collect(Collectors.toCollection(ArrayList::new));
        Classifier classifier = (new WekaClassifierParameter(getClassifierSettings().getClassifier())).getClassifier(); // This will make a copy of the classifier

        List<ImagePlus> trainingImages = new ArrayList<>();
        List<ImagePlus> labelImages = new ArrayList<>();

        for (Integer row : iterationStep.getInputRows(getFirstInputSlot())) {
            trainingImages.add(getFirstInputSlot().getData(row, ImagePlus2DData.class, progressInfo).getDuplicateImage());
            List<JIPipeDataAnnotation> dataAnnotations = getFirstInputSlot().getDataAnnotations(row);
            JIPipeDataAnnotation dataAnnotation = labelDataAnnotation.queryFirst(dataAnnotations);
            labelImages.add(dataAnnotation.getData(ImagePlus2DGreyscaleData.class, progressInfo).getDuplicateImage());
        }

        if (trainingImages.isEmpty()) {
            progressInfo.log("Nothing to train. Skipping.");
            return;
        }

        TIntSet labels = new TIntHashSet();
        for (ImagePlus labelImage : labelImages) {
            labels.addAll(LabelImages.findAllLabels(labelImage));
            if (labelImage.getProcessor().getStats().min == 0) {
                labels.add(0);
            }
        }
        int numLabels = labels.size();

        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo.resolve("Weka"))) {
            WekaSegmentation wekaSegmentation = new WekaSegmentation(trainingImages.get(0));
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

            for (int i = 0; i < trainingImages.size(); i++) {
                if (!ImageJUtils.imagesHaveSameSize(trainingImages.get(0), trainingImages.get(i), labelImages.get(i))) {
                    throw new JIPipeValidationRuntimeException(new GraphNodeValidationReportContext(this),
                            new IllegalArgumentException("Training images don't have the same size!"),
                            "Training images have different sizes!",
                            "Weka training images must have the same size",
                            "Check if the correct images are chosen or scale the images");
                }
                wekaSegmentation.addLabeledData(trainingImages.get(i), labelImages.get(i));
            }

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

    @SetJIPipeDocumentation(name = "Label data annotation", description = "Determines which data annotation contains the labels. Please ensure that " +
            "the appropriate label data is annotated to the raw input data.")
    @JIPipeParameter("label-data-annotation")
    public DataAnnotationQueryExpression getLabelDataAnnotation() {
        return labelDataAnnotation;
    }

    @JIPipeParameter("label-data-annotation")
    public void setLabelDataAnnotation(DataAnnotationQueryExpression labelDataAnnotation) {
        this.labelDataAnnotation = labelDataAnnotation;
    }

}
