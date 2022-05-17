package org.hkijena.jipipe.extensions.ijweka.nodes;

import ij.IJ;
import ij.gui.Roi;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijweka.WekaUtils;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import trainableSegmentation.WekaSegmentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Train Weka model (2D)", description = "Trains a Weka model on 2D image data")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@JIPipeInputSlot(value = ImagePlus2DData.class, slotName = "Image", description = "Image on which the training should be applied", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", description = "The regions used for the training. " +
        "Please note that the ROI must be annotated according to the 'Class annotation' parameter", autoCreate = true)
@JIPipeOutputSlot(value = WekaModelData.class, slotName = "Trained model", description = "The model", autoCreate = true)
public class WekaTrainingROI2DAlgorithm extends JIPipeMergingAlgorithm {

    private OptionalAnnotationNameParameter classAnnotationName = new OptionalAnnotationNameParameter("Class", true);

    public WekaTrainingROI2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public WekaTrainingROI2DAlgorithm(WekaTrainingROI2DAlgorithm other) {
        super(other);
        this.classAnnotationName = new OptionalAnnotationNameParameter(other.classAnnotationName);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Map<Integer, ROIListData> groupedROIs = WekaUtils.groupROIByAnnotation(getInputSlot("ROI"), dataBatch, classAnnotationName, progressInfo);

        // Apply the training per image
        List<ImagePlus2DData> trainingImages = dataBatch.getInputData("Image", ImagePlus2DData.class, progressInfo);
        for (int i = 0; i < trainingImages.size(); i++) {
            JIPipeProgressInfo imageProgress = progressInfo.resolveAndLog("Image", i, trainingImages.size());
            WekaSegmentation wekaSegmentation = new WekaSegmentation(trainingImages.get(i).getImage());
            for (Map.Entry<Integer, ROIListData> entry : groupedROIs.entrySet()) {
                for (Roi roi : entry.getValue()) {
                    wekaSegmentation.addExample(entry.getKey(), roi, 1);
                }
            }

            if(!wekaSegmentation.trainClassifier()) {
                throw new RuntimeException("Weka training failed!");
            }

            dataBatch.addOutputData("Trained model", new WekaModelData(wekaSegmentation), imageProgress);
        }
    }

    @JIPipeDocumentation(name = "Class annotation", description = "The annotation of the ROI inputs that are used for determining the class. Ideally, annotations " +
            "should be numeric, beginning from zero (as required internally by the Weka training algorithm). Alternatively, JIPipe will assign classes automatically.")
    @JIPipeParameter(value = "class-annotation-name", important = true)
    public OptionalAnnotationNameParameter getClassAnnotationName() {
        return classAnnotationName;
    }

    @JIPipeParameter("class-annotation-name")
    public void setClassAnnotationName(OptionalAnnotationNameParameter classAnnotationName) {
        this.classAnnotationName = classAnnotationName;
    }
}
