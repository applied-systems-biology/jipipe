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
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.extensions.ijweka.WekaUtils;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierParameter;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierSettings;
import org.hkijena.jipipe.extensions.ijweka.parameters.collections.WekaFeature3DSettings;
import org.hkijena.jipipe.extensions.ijweka.parameters.features.WekaFeature3D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.parameters.library.graph.InputSlotMapParameterCollection;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Train Weka model from ROI (3D)", description = "Trains a Weka model on 3D image data. The inputs are ROI that are assigned to the classes and trained on the input image."+
        "Can only train on a single image. Please convert ROI to labels/masks and use the appropriate nodes if you want to train on multiple images.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@JIPipeInputSlot(value = ImagePlus3DData.class, slotName = "Image", description = "Image on which the training should be applied", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class)
@JIPipeOutputSlot(value = WekaModelData.class, slotName = "Trained model", description = "The model", autoCreate = true)
public class WekaTrainingROI3DAlgorithm extends JIPipeIteratingAlgorithm {

    private final InputSlotMapParameterCollection classAssignment;
    private WekaFeature3DSettings featureSettings = new WekaFeature3DSettings();
    private WekaClassifierSettings classifierSettings = new WekaClassifierSettings();


    public WekaTrainingROI3DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Image", "Image on which the training should be applied", ImagePlus3DData.class, false, false)
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

    public WekaTrainingROI3DAlgorithm(WekaTrainingROI3DAlgorithm other) {
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Map<Integer, ROIListData> groupedROIs = new HashMap<>();
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            if (inputSlot.acceptsTrivially(ROIListData.class)) {
                int klass = classAssignment.getValue(inputSlot.getName(), Integer.class);
                ROIListData list = iterationStep.getInputData(inputSlot, ROIListData.class, progressInfo);

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
        ArrayList<String> selectedFeatureNames = featureSettings.getTrainingFeatures().getValues().stream().map(WekaFeature3D::name).collect(Collectors.toCollection(ArrayList::new));
        Classifier classifier = (new WekaClassifierParameter(getClassifierSettings().getClassifier())).getClassifier(); // This will make a copy of the classifier

        // Apply the training
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo.resolve("Weka"))) {
            ImagePlus trainingImage = iterationStep.getInputData("Image", ImagePlus3DData.class, progressInfo).getDuplicateImage();
            WekaSegmentation wekaSegmentation = new WekaSegmentation(true);
            wekaSegmentation.setTrainingImage(trainingImage);
            wekaSegmentation.setClassifier((AbstractClassifier) classifier);
            wekaSegmentation.setDoClassBalance(getClassifierSettings().isBalanceClasses());
            WekaUtils.set3DFeatures(wekaSegmentation, featureSettings.getTrainingFeatures().getValues());
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

            iterationStep.addOutputData("Trained model", new WekaModelData(wekaSegmentation), progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (getInputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ROIListData.class).count() < 2) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    reportContext,
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
    public WekaFeature3DSettings getFeatureSettings() {
        return featureSettings;
    }

    @JIPipeDocumentation(name = "Classes", description = "Assign the numeric classes to the input ROI slots")
    @JIPipeParameter("class-assignment")
    public InputSlotMapParameterCollection getClassAssignment() {
        return classAssignment;
    }
}
