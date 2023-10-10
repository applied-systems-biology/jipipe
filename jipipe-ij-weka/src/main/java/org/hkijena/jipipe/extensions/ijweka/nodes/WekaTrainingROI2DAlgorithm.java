package org.hkijena.jipipe.extensions.ijweka.nodes;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierParameter;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierSettings;
import org.hkijena.jipipe.extensions.ijweka.parameters.collections.WekaFeature2DSettings;
import org.hkijena.jipipe.extensions.ijweka.parameters.features.WekaFeature2D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.parameters.library.graph.InputSlotMapParameterCollection;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Train Weka model from ROI (2D)", description = "Trains a Weka model on 2D image data. The inputs are ROI that are assigned to the classes and trained on the input image.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@JIPipeInputSlot(value = ImagePlus2DData.class, slotName = "Image", description = "Image on which the training should be applied", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class)
@JIPipeOutputSlot(value = WekaModelData.class, slotName = "Trained model", description = "The model", autoCreate = true)
public class WekaTrainingROI2DAlgorithm extends JIPipeIteratingAlgorithm {

    private final InputSlotMapParameterCollection classAssignment;
    private WekaFeature2DSettings featureSettings = new WekaFeature2DSettings();
    private WekaClassifierSettings classifierSettings = new WekaClassifierSettings();


    public WekaTrainingROI2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Image", "Image on which the training should be applied", ImagePlus2DData.class, false, false)
                .addInputSlot("Class 1", "", ROIListData.class)
                .addInputSlot("Class 2", "", ROIListData.class)
                .addOutputSlot("Trained model", "The model", WekaModelData.class)
                .sealOutput()
                .restrictInputTo(ROIListData.class)
                .build());
        registerSubParameter(featureSettings);
        classAssignment = new InputSlotMapParameterCollection(Integer.class, this, this::getNewClass, false);
        classAssignment.setSlotFilter(slot -> slot.acceptsTrivially(ROIListData.class));
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
        classAssignment.setSlotFilter(slot -> slot.acceptsTrivially(ROIListData.class));
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Map<Integer, ROIListData> groupedROIs = new HashMap<>();
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            if (inputSlot.acceptsTrivially(ROIListData.class)) {
                int klass = classAssignment.getValue(inputSlot.getName(), Integer.class);
                ROIListData list = dataBatch.getInputData(inputSlot, ROIListData.class, progressInfo);

                if (list == null)
                    continue;

                ROIListData target = groupedROIs.getOrDefault(klass, null);
                if (target == null) {
                    target = new ROIListData();
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
            ImagePlus trainingImage = dataBatch.getInputData("Image", ImagePlus2DData.class, progressInfo).getDuplicateImage();
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

            for (Map.Entry<Integer, ROIListData> entry : groupedROIs.entrySet()) {
                for (Roi roi : entry.getValue()) {
                    wekaSegmentation.addExample(entry.getKey(), roi, 1);
                }
            }


            if (!wekaSegmentation.trainClassifier()) {
                throw new RuntimeException("Weka training failed!");
            }


            dataBatch.addOutputData("Trained model", new WekaModelData(wekaSegmentation), progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        if (getInputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ROIListData.class).count() < 2) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    context,
                    "Weka requires at least two classes!", "The Weka algorithm cannot be trained if you do not have at least two classes",
                    "Add at least two ROI List inputs"));
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

    @JIPipeDocumentation(name = "Classes", description = "Assign the numeric classes to the input ROI slots")
    @JIPipeParameter("class-assignment")
    public InputSlotMapParameterCollection getClassAssignment() {
        return classAssignment;
    }
}
