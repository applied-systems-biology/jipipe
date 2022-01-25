package org.hkijena.jipipe.extensions.cellpose;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.extensions.cellpose.parameters.OutputParameters;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CellPoseUtils {
    private CellPoseUtils() {

    }

    /**
     * Converts a Cellpose ROI to {@link ROIListData} according to https://github.com/MouseLand/cellpose/blob/master/imagej_roi_converter.py
     *
     * @param file the Cellpose ROI
     * @return ImageJ ROI
     */
    public static ROIListData cellPoseROIToImageJ(Path file) {
        ROIListData rois = new ROIListData();
        try {
            for (String line : Files.readAllLines(file)) {
                TIntList xList = new TIntArrayList();
                TIntList yList = new TIntArrayList();
                List<Integer> xyList = Arrays.stream(line.trim().split(",")).map(Integer::parseInt).collect(Collectors.toList());
                for (int i = 0; i < xyList.size(); i++) {
                    if (i % 2 == 0) {
                        xList.add(xyList.get(i));
                    } else {
                        yList.add(xyList.get(i));
                    }
                }
                rois.add(new PolygonRoi(xList.toArray(), yList.toArray(), xList.size(), Roi.POLYGON));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return rois;
    }

    /**
     * Converts ROI in a custom Json format to {@link ROIListData}
     *
     * @param file the ROI file
     * @return ImageJ ROI
     */
    public static ROIListData cellPoseROIJsonToImageJ(Path file) {
        ROIListData rois = new ROIListData();
        try {
            JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(file.toFile());
            for (JsonNode roiItem : ImmutableList.copyOf(node.elements())) {
                TIntList xList = new TIntArrayList();
                TIntList yList = new TIntArrayList();

                for (JsonNode coordItem : ImmutableList.copyOf(roiItem.get("coords"))) {
                    int x = coordItem.get("x").asInt();
                    int y = coordItem.get("y").asInt();
                    xList.add(x);
                    yList.add(y);
                }

                if (xList.size() != yList.size()) {
                    System.err.println("Error: Different X and  Y array sizes");
                    continue;
                }
                if (xList.size() < 3) {
                    // Empty ROIs are not allowed
                    continue;
                }

                PolygonRoi roi = new PolygonRoi(xList.toArray(), yList.toArray(), xList.size(), Roi.POLYGON);
                int z = -1;
                JsonNode zEntry = roiItem.path("z");
                if (!zEntry.isMissingNode())
                    z = zEntry.asInt();
                roi.setPosition(0, z + 1, 0);
                rois.add(roi);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return rois;
    }

    public static void extractCellposeOutputs(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo, Path outputRoiOutline, Path outputLabels, Path outputFlows, Path outputProbabilities, Path outputStyles, List<JIPipeTextAnnotation> annotationList, OutputParameters outputParameters) {
        if (outputParameters.isOutputROI()) {
            ROIListData rois = cellPoseROIJsonToImageJ(outputRoiOutline);
            dataBatch.addOutputData("ROI", rois, annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputLabels()) {
            ImagePlus labels = IJ.openImage(outputLabels.toString());
            dataBatch.addOutputData("Labels", new ImagePlus3DGreyscaleData(labels), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputFlows()) {
            ImagePlus flows = IJ.openImage(outputFlows.toString());
            dataBatch.addOutputData("Flows", new ImagePlus3DColorRGBData(flows), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputProbabilities()) {
            ImagePlus probabilities = IJ.openImage(outputProbabilities.toString());
            dataBatch.addOutputData("Probabilities", new ImagePlus3DGreyscale32FData(probabilities), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputStyles()) {
            ImagePlus styles = IJ.openImage(outputStyles.toString());
            dataBatch.addOutputData("Styles", new ImagePlus3DGreyscale32FData(styles), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
    }

}
