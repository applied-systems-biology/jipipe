package org.hkijena.jipipe.extensions.ijweka.nodes;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierParameter;
import org.hkijena.jipipe.extensions.ijweka.parameters.features.WekaFeature2D;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaFeature2DSettings;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.parameters.library.graph.InputSlotMapParameterCollection;
import trainableSegmentation.WekaSegmentation;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Train Weka model from ROI (2D)", description = "Trains a Weka model on 2D image data. The inputs are ROI that are assigned to the classes and trained on the input image.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@JIPipeInputSlot(value = ImagePlus2DData.class, slotName = "Image", description = "Image on which the training should be applied", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class)
@JIPipeOutputSlot(value = WekaModelData.class, slotName = "Trained model", description = "The model", autoCreate = true)
public class WekaTrainingROI2DAlgorithm extends AbstractWekaTrainingAlgorithm {

    private WekaFeature2DSettings featureSettings = new WekaFeature2DSettings();
    private final InputSlotMapParameterCollection classAssignment;


    public WekaTrainingROI2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Image", "Image on which the training should be applied", ImagePlus2DData.class, false, false)
                .addInputSlot("Class 1", "", ROIListData.class)
                .addInputSlot("Class 2", "", ROIListData.class)
                .addOutputSlot("Trained model", "The model", WekaModelData.class, null)
                .sealOutput()
                .restrictInputTo(ROIListData.class)
                .build());
        registerSubParameter(featureSettings);
        classAssignment = new InputSlotMapParameterCollection(Integer.class, this, this::getNewClass, false);
        classAssignment.setSlotFilter(slot -> slot.acceptsTrivially(ROIListData.class));
        classAssignment.updateSlots();
        registerSubParameter(classAssignment);
    }
    public WekaTrainingROI2DAlgorithm(WekaTrainingROI2DAlgorithm other) {
        super(other);
        this.featureSettings = other.featureSettings;
        registerSubParameter(featureSettings);
        classAssignment = new InputSlotMapParameterCollection(Integer.class, this, this::getNewClass, false);
        classAssignment.setSlotFilter(slot -> slot.acceptsTrivially(ROIListData.class));
        other.classAssignment.copyTo(classAssignment);
        registerSubParameter(classAssignment);
    }

    private Object getNewClass(JIPipeDataSlotInfo slotInfo) {
        int newClass = 0;
        for (JIPipeParameterAccess access : classAssignment.getParameters().values()) {
            Integer klass = access.get(Integer.class);
            if(klass != null) {
                newClass = Math.max(newClass, klass + 1);
            }
        }
        return newClass;
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Map<Integer, ROIListData> groupedROIs =  new HashMap<>();
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            if(inputSlot.acceptsTrivially(ROIListData.class)) {
                int klass = classAssignment.getValue(inputSlot.getName(), Integer.class);
                List<ROIListData> list = dataBatch.getInputData(inputSlot, ROIListData.class, progressInfo);

                ROIListData target = groupedROIs.getOrDefault(klass, null);
                if(target == null) {
                    target = new ROIListData();
                    groupedROIs.put(klass, target);
                }

                for (ROIListData rois : list) {
                    target.addAll(rois);
                }

            }
        }


        // Setup parameters
        ArrayList<String> selectedFeatureNames = featureSettings.getTrainingFeatures().getValues().stream().map(WekaFeature2D::name).collect(Collectors.toCollection(ArrayList::new));
        Classifier classifier = (new WekaClassifierParameter(getClassifierSettings().getClassifier())).getClassifier(); // This will make a copy of the classifier

        // Apply the training per image
        List<ImagePlus2DData> trainingImages = dataBatch.getInputData("Image", ImagePlus2DData.class, progressInfo);
        for (int i = 0; i < trainingImages.size(); i++) {
            JIPipeProgressInfo imageProgress = progressInfo.resolveAndLog("Image", i, trainingImages.size());
            WekaSegmentation wekaSegmentation = new WekaSegmentation(trainingImages.get(i).getImage());
            wekaSegmentation.setClassifier((AbstractClassifier) classifier);
            wekaSegmentation.setDoClassBalance(getClassifierSettings().isBalanceClasses());
            wekaSegmentation.setFeatures(selectedFeatureNames);
            wekaSegmentation.setFeaturesDirty();
            wekaSegmentation.setMembraneThickness(featureSettings.getMembraneSize());
            wekaSegmentation.setMembranePatchSize(featureSettings.getMembranePatchSize());
            wekaSegmentation.setMinimumSigma(featureSettings.getMinSigma());
            wekaSegmentation.setMaximumSigma(featureSettings.getMaxSigma());
            wekaSegmentation.setUseNeighbors(featureSettings.isUseNeighbors());
            while(wekaSegmentation.getNumOfClasses() < groupedROIs.size()) {
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

            dataBatch.addOutputData("Trained model", new WekaModelData(wekaSegmentation), imageProgress);
        }
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        super.reportValidity(report);
        if(getInputSlots().stream().filter(slot -> slot.getAcceptedDataType() == ROIListData.class).count() < 2) {
            report.reportIsInvalid("Weka requires at least two classes!", "The Weka algorithm cannot be trained if you do not have at least two classes",
                    "Add at least two ROI List inputs", getDisplayName());
        }
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
