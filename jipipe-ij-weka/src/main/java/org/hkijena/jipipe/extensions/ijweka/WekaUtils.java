package org.hkijena.jipipe.extensions.ijweka;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import trainableSegmentation.FeatureStack3D;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.WekaSegmentation;
import weka.classifiers.AbstractClassifier;
import weka.core.Instances;

import java.util.*;
import java.util.stream.Collectors;

public class WekaUtils {
    public static Map<Integer, ROIListData> groupROIByAnnotation(JIPipeInputDataSlot roiSlot, JIPipeMergingDataBatch dataBatch,
                                                                 OptionalAnnotationNameParameter classAnnotationName, JIPipeProgressInfo progressInfo) {
        // Collect the ROI classes
        Map<Integer, ROIListData> groupedROIs = new HashMap<>();
        if(classAnnotationName.isEnabled() && !StringUtils.isNullOrEmpty(classAnnotationName.getContent())) {
            int maxClass = Integer.MIN_VALUE;
            Set<Integer> failed = new HashSet<>();

            // First read the classes from annotations
            for (int row : dataBatch.getInputRows("ROI")) {
                JIPipeTextAnnotation classAnnotation = roiSlot.getTextAnnotation(row, classAnnotationName.getContent());
                String annotationValue = classAnnotation != null ? classAnnotation.getValue() : "";
                try {
                    // Attempt to parse as integer. If success, then choose it as class
                    int klass = Integer.parseInt(annotationValue);
                    maxClass = Math.max(klass, maxClass);

                    ROIListData target = groupedROIs.getOrDefault(klass, null);
                    if(target == null) {
                        target = new ROIListData();
                        groupedROIs.put(klass, target);
                    }
                    target.addAll(roiSlot.getData(row, ROIListData.class, progressInfo));
                }
                catch (NumberFormatException e) {
                    progressInfo.log("Unable to convert class annotation ROI at row " + row + " into a number. Assigning classes automatically!");
                    failed.add(row);
                }
            }

            maxClass = Math.max(0, maxClass + 1);

            // Handle all failed instances
            Map<String, List<Integer>> failedGroupedByAnnotation = failed.stream().collect(Collectors.groupingBy(row -> {
                JIPipeTextAnnotation classAnnotation = roiSlot.getTextAnnotation(row, classAnnotationName.getContent());
                String annotationValue = classAnnotation != null ? classAnnotation.getValue() : "";
                return annotationValue;
            }));
            ArrayList<Map.Entry<String, List<Integer>>> failedEntries = new ArrayList<>(failedGroupedByAnnotation.entrySet());
            for (int i = 0; i < failedEntries.size(); i++) {
                Map.Entry<String, List<Integer>> entry = failedEntries.get(i);
                int klass = maxClass + i;
                ROIListData target = groupedROIs.getOrDefault(klass, null);
                if(target == null) {
                    target = new ROIListData();
                    groupedROIs.put(klass, target);
                }
                for (Integer row : entry.getValue()) {
                    progressInfo.log("Assigning " + row + " to class = " + row + ". If you do not want this, set numeric class annotations.");
                    target.addAll(roiSlot.getData(row, ROIListData.class, progressInfo));
                }
            }

        }
        else {
            progressInfo.log("No class annotation name set. All ROIs will be assigned the same class = 1.");
            // Dump all in one class
            ROIListData merged = new ROIListData();
            for (ROIListData roi : dataBatch.getInputData("ROI", ROIListData.class, progressInfo)) {
                merged.addAll(roi);
            }
            groupedROIs.put(1, merged);
        }
        return groupedROIs;
    }
}
