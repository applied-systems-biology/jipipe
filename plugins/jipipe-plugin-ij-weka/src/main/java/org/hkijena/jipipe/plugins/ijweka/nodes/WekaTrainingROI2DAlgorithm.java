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
import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.plugins.ijweka.parameters.WekaClassifierParameter;
import org.hkijena.jipipe.plugins.ijweka.parameters.WekaClassifierSettings;
import org.hkijena.jipipe.plugins.ijweka.parameters.collections.WekaFeature2DSettings;
import org.hkijena.jipipe.plugins.ijweka.parameters.features.WekaFeature2D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.plugins.parameters.library.graph.InputSlotMapParameterCollection;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Train Weka model from ROI (2D)", description = "Trains a Weka model on 2D image data. The inputs are ROI that are assigned to the classes and trained on the input image. " +
        "Can only train on a single image. Please convert ROI to labels/masks and use the appropriate nodes if you want to train on multiple images.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@AddJIPipeInputSlot(value = ImagePlus2DData.class, name = "Image", description = "Image on which the training should be applied", create = true)
@AddJIPipeInputSlot(value = ROI2DListData.class)
@AddJIPipeOutputSlot(value = WekaModelData.class, name = "Trained model", description = "The model", create = true)
public class WekaTrainingROI2DAlgorithm extends JIPipeIteratingAlgorithm {

    private final InputSlotMapParameterCollection classAssignment;
    private WekaFeature2DSettings featureSettings = new WekaFeature2DSettings();
    private WekaClassifierSettings classifierSettings = new WekaClassifierSettings();


    public WekaTrainingROI2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Image", "Image on which the training should be applied", ImagePlus2DData.class, false, false)
                .addInputSlot("Class 1", "", ROI2DListData.class)
                .addInputSlot("Class 2", "", ROI2DListData.class)
                .addOutputSlot("Trained model", "The model", WekaModelData.class)
                .sealOutput()
                .restrictInputTo(ROI2DListData.class)
                .build());
        registerSubParameter(featureSettings);
        classAssignment = new InputSlotMapParameterCollection(Integer.class, this, this::getNewClass, false);
        classAssignment.setSlotFilter(slot -> slot.acceptsTrivially(ROI2DListData.class));
        classAssignment.updateSlots();
        registerSubParameter(classAssignment);
        registerSubParameter(classifierSettings);
    }

    public WekaTrainingROI2DAlgorithm(WekaTrainingROI2DAlgorithm other) {
        super(other);
        this.featureSettings = other.featureSettings;
        registerSubParameter(featureSettings);
        this.classifierSettings = new WekaClassifierSettings(other.classifierSettings);
        registerSubParameter(classifierSettings);
        classAssignment = new InputSlotMapParameterCollection(Integer.class, this, this::getNewClass, false);
        classAssignment.setSlotFilter(slot -> slot.acceptsTrivially(ROI2DListData.class));
        other.classAssignment.copyTo(classAssignment);
        registerSubParameter(classAssignment);
    }

    private Object getNewClass(JIPipeDataSlotInfo slotInfo) {
        int newClass = 0;
        for (JIPipeParameterAccess access : classAssignment.getParameters().values()) {
            Integer klass = access.get(Integer.class);
            if (klass != null) {
                newClass = Math.max(newClass, klass + 1);
            }
        }
        return newClass;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Map<Integer, ROI2DListData> groupedROIs = new HashMap<>();
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            if (inputSlot.acceptsTrivially(ROI2DListData.class)) {
                int klass = classAssignment.getValue(inputSlot.getName(), Integer.class);
                ROI2DListData list = iterationStep.getInputData(inputSlot, ROI2DListData.class, progressInfo);

                if (list == null)
                    continue;

                ROI2DListData target = groupedROIs.getOrDefault(klass, null);
                if (target == null) {
                    target = new ROI2DListData();
                    groupedROIs.put(klass, target);
                }

                target.addAll(list);
            }
        }


        // Setup parameters
        ArrayList<String> selectedFeatureNames = featureSettings.getTrainingFeatures().getValues().stream().map(WekaFeature2D::name).collect(Collectors.toCollection(ArrayList::new));
        Classifier classifier = (new WekaClassifierParameter(getClassifierSettings().getClassifier())).getClassifier(); // This will make a copy of the classifier

        // Apply the training
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo.resolve("Weka"))) {
            ImagePlus trainingImage = iterationStep.getInputData("Image", ImagePlus2DData.class, progressInfo).getDuplicateImage();
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
            while (wekaSegmentation.getNumOfClasses() < groupedROIs.size()) {
                wekaSegmentation.addClass();
            }

            for (Map.Entry<Integer, ROI2DListData> entry : groupedROIs.entrySet()) {
                for (Roi roi : entry.getValue()) {
                    wekaSegmentation.addExample(entry.getKey(), roi, 1);
                }
            }


            if (!wekaSegmentation.trainClassifier()) {
                throw new RuntimeException("Weka training failed!");
            }


            iterationStep.addOutputData("Trained model", new WekaModelData(wekaSegmentation), progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (getInputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ROI2DListData.class).count() < 2) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    reportContext,
                    "Weka requires at least two classes!", "The Weka algorithm cannot be trained if you do not have at least two classes",
                    "Add at least two ROI List inputs"));
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

    @SetJIPipeDocumentation(name = "Classes", description = "Assign the numeric classes to the input ROI slots")
    @JIPipeParameter("class-assignment")
    public InputSlotMapParameterCollection getClassAssignment() {
        return classAssignment;
    }
}
