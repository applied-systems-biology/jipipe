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

package org.hkijena.jipipe.plugins.cellpose;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.plugins.cellpose.parameters.deprecated.CellposeSegmentationOutputSettings_Old;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CellposeUtils {

    private static String CELLPOSE_CUSTOM_CODE;

    private CellposeUtils() {

    }

    public static String getCellposeCustomCode() {
        if (CELLPOSE_CUSTOM_CODE == null) {
            try {
                CELLPOSE_CUSTOM_CODE = Resources.toString(CellposeUtils.class.getResource("/org/hkijena/jipipe/plugins/cellpose/cellpose-custom.py"), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return CELLPOSE_CUSTOM_CODE;
    }

    /**
     * Converts a Cellpose ROI to {@link ROIListData} according to <a href="https://github.com/MouseLand/cellpose/blob/master/imagej_roi_converter.py">...</a>
     *
     * @param file the Cellpose ROI
     * @return ImageJ ROI
     */
    public static ROIListData cellposeROIToImageJ(Path file) {
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
    public static ROIListData cellposeROIJsonToImageJ(Path file) {
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

    public static void extractCellposeOutputs(JIPipeMultiIterationStep iterationStep, JIPipeProgressInfo progressInfo, Path outputRoiOutline, Path outputLabels, Path outputFlows, Path outputProbabilities, Path outputStyles, List<JIPipeTextAnnotation> annotationList, CellposeSegmentationOutputSettings_Old outputParameters) {
        if (outputParameters.isOutputROI()) {
            ROIListData rois = cellposeROIJsonToImageJ(outputRoiOutline);
            iterationStep.addOutputData("ROI", rois, annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputLabels()) {
            ImagePlus labels = IJ.openImage(outputLabels.toString());
            iterationStep.addOutputData("Labels", new ImagePlus3DGreyscaleData(labels), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputFlows()) {
            ImagePlus flows = IJ.openImage(outputFlows.toString());
            iterationStep.addOutputData("Flows", new ImagePlus3DColorRGBData(flows), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputProbabilities()) {
            ImagePlus probabilities = IJ.openImage(outputProbabilities.toString());
            iterationStep.addOutputData("Probabilities", new ImagePlus3DGreyscale32FData(probabilities), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputStyles()) {
            ImagePlus styles = IJ.openImage(outputStyles.toString());
            iterationStep.addOutputData("Styles", new ImagePlus3DGreyscale32FData(styles), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
    }

}
