package org.hkijena.jipipe.extensions.cellpose;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.JsonUtils;

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

                if(xList.size() != yList.size()) {
                    System.err.println("Error: Different X and  Y array sizes");
                    continue;
                }
                if(xList.size() < 3) {
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
}
