package org.hkijena.jipipe.extensions.cellpose;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

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
                    if(i % 2 == 0) {
                        xList.add(xyList.get(i));
                    }
                    else {
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
}
