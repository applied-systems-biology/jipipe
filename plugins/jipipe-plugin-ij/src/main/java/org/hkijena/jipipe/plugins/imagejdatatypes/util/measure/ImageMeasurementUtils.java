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

package org.hkijena.jipipe.plugins.imagejdatatypes.util.measure;

import ij.gui.Roi;
import ij.process.FloatPolygon;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.jogamp.vecmath.Point2f;

/**
 * Custom implementation of {@link ij.plugin.filter.Analyzer}
 */
public class ImageMeasurementUtils {

    public static void calculateAdditionalMeasurements(ImageStatisticsSetParameter measurements, boolean addNameToTable, Roi roi, ResultsTableData forRoi) {
        if (measurements.getValues().contains(Measurement.BoundingRectangle) || measurements.getValues().contains(Measurement.ShapeDescriptors)) {
            // Calculate fitted rotated rectangle
            Roi mbr = ROI2DListData.calculateMinimumBoundingRectangle(roi);
            FloatPolygon fp = mbr.getFloatPolygon();
            Point2f p1 = new Point2f(fp.xpoints[0], fp.ypoints[0]);
            Point2f p2 = new Point2f(fp.xpoints[1], fp.ypoints[1]);
            Point2f p3 = new Point2f(fp.xpoints[2], fp.ypoints[2]);
            Point2f p4 = new Point2f(fp.xpoints[3], fp.ypoints[3]);
            float major = Math.max(p1.distance(p2), p2.distance(p3));
            float minor = Math.min(p1.distance(p2), p2.distance(p3));
            if (measurements.getValues().contains(Measurement.BoundingRectangle)) {
                int columnRBWidth = forRoi.getOrCreateColumnIndex("RBWidth", false);
                int columnRBHeight = forRoi.getOrCreateColumnIndex("RBHeight", false);
                int columnRBX1 = forRoi.getOrCreateColumnIndex("RBX1", false);
                int columnRBX2 = forRoi.getOrCreateColumnIndex("RBX2", false);
                int columnRBX3 = forRoi.getOrCreateColumnIndex("RBX3", false);
                int columnRBX4 = forRoi.getOrCreateColumnIndex("RBX4", false);
                int columnRBY1 = forRoi.getOrCreateColumnIndex("RBY1", false);
                int columnRBY2 = forRoi.getOrCreateColumnIndex("RBY2", false);
                int columnRBY3 = forRoi.getOrCreateColumnIndex("RBY3", false);
                int columnRBY4 = forRoi.getOrCreateColumnIndex("RBY4", false);
                for (int row = 0; row < forRoi.getRowCount(); row++) {
                    forRoi.setValueAt(major, row, columnRBWidth);
                    forRoi.setValueAt(minor, row, columnRBHeight);
                    forRoi.setValueAt(p1.x, row, columnRBX1);
                    forRoi.setValueAt(p2.x, row, columnRBX2);
                    forRoi.setValueAt(p3.x, row, columnRBX3);
                    forRoi.setValueAt(p4.x, row, columnRBX4);
                    forRoi.setValueAt(p1.y, row, columnRBY1);
                    forRoi.setValueAt(p2.y, row, columnRBY2);
                    forRoi.setValueAt(p3.y, row, columnRBY3);
                    forRoi.setValueAt(p4.y, row, columnRBY4);
                }
            }
            if (measurements.getValues().contains(Measurement.ShapeDescriptors)) {
                float ar = major / minor;
                int column = forRoi.getOrCreateColumnIndex("rAR", false);
                for (int row = 0; row < forRoi.getRowCount(); row++) {
                    forRoi.setValueAt(ar, row, column);
                }
            }
        }
        if (measurements.getValues().contains(Measurement.StackPosition)) {
            int columnChannel = forRoi.getOrCreateColumnIndex("Ch", false);
            int columnStack = forRoi.getOrCreateColumnIndex("Slice", false);
            int columnFrame = forRoi.getOrCreateColumnIndex("Frame", false);
            for (int row = 0; row < forRoi.getRowCount(); row++) {
                forRoi.setValueAt(roi.getCPosition(), row, columnChannel);
                forRoi.setValueAt(roi.getZPosition(), row, columnStack);
                forRoi.setValueAt(roi.getTPosition(), row, columnFrame);
            }
        }
        if (addNameToTable) {
            int columnName = forRoi.getOrCreateColumnIndex("Name", true);
            for (int row = 0; row < forRoi.getRowCount(); row++) {
                forRoi.setValueAt(roi.getName(), row, columnName);
            }
        }
    }
}
