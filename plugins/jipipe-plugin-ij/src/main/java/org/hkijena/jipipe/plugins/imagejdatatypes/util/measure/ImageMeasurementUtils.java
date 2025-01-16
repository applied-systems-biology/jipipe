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
import org.scijava.vecmath.Point2f;

/**
 * Custom implementation of {@link ij.plugin.filter.Analyzer}
 */
public class ImageMeasurementUtils {

//    public static void measure(ImageProcessor ip, ImageStatisticsSetParameter measurements, ImageStatistics stats, ImagePlus imp, Roi roi, ResultsTableData resultsTable) {
//
//    }
//
//    public static void measure(ImageStatisticsSetParameter measurements, ImageStatistics stats, ImagePlus imp, Roi roi, ResultsTableData resultsTable) {
//
//        final int precision = 3;
//
//        // Add the row and get the internal table
//        resultsTable.addRow();
//        ResultsTable rt = resultsTable.getTable();
//
////        int counter = rt.size();
////        if (counter<=MAX_STANDARDS && !(stats.umean==0.0&&counter==1&&umeans!=null && umeans[0]!=0f)) {
////            if (umeans==null) umeans = new float[MAX_STANDARDS];
////            umeans[counter-1] = (float)stats.umean;
////        }
////        if ((measurements&LABELS)!=0) {
////            rt.addLabel("Label", getFileName());
////        }
//        if (measurements.contains(Measurement.Area)) {
//            rt.addValue(ResultsTable.AREA,stats.area);
//        }
//        if (measurements.contains(Measurement.PixelValueMean)) {
//            rt.addValue(ResultsTable.MEAN,stats.mean);
//        }
//        if (measurements.contains(Measurement.PixelValueStandardDeviation)) {
//            rt.addValue(ResultsTable.STD_DEV,stats.stdDev);
//        }
//        if (measurements.contains(Measurement.PixelValueModal)) {
//            rt.addValue(ResultsTable.MODE, stats.dmode);
//        }
//        if (measurements.contains(Measurement.PixelValueMinMax)) {
//            rt.addValue(ResultsTable.MIN,stats.min);
//            rt.addValue(ResultsTable.MAX,stats.max);
//        }
//        if (measurements.contains(Measurement.Centroid)) {
//            rt.addValue(ResultsTable.X_CENTROID,stats.xCentroid);
//            rt.addValue(ResultsTable.Y_CENTROID,stats.yCentroid);
//        }
//        if (measurements.contains(Measurement.CenterOfMass)) {
//            rt.addValue(ResultsTable.X_CENTER_OF_MASS,stats.xCenterOfMass);
//            rt.addValue(ResultsTable.Y_CENTER_OF_MASS,stats.yCenterOfMass);
//        }
//        if (measurements.contains(Measurement.Perimeter) || measurements.contains(Measurement.ShapeDescriptors)) {
//            double perimeter;
//            if (roi!=null)
//                perimeter = roi.getLength();
//            else
//                perimeter = imp!=null?imp.getWidth()*2+imp.getHeight()*2:0.0;
//            if (measurements.contains(Measurement.Perimeter)) {
//                rt.addValue(ResultsTable.PERIMETER,perimeter);
//            }
//            if (measurements.contains(Measurement.ShapeDescriptors)) {
//                double circularity = perimeter==0.0?0.0:4.0*Math.PI*(stats.area/(perimeter*perimeter));
//                if (circularity>1.0) circularity = 1.0;
//                rt.addValue(ResultsTable.CIRCULARITY, circularity);
//                Polygon ch = null;
//                boolean isArea = roi==null || roi.isArea();
//                double convexArea = roi!=null?getArea(roi.getConvexHull()):stats.pixelCount;
//                rt.addValue(ResultsTable.ASPECT_RATIO, isArea?stats.major/stats.minor:0.0);
//                rt.addValue(ResultsTable.ROUNDNESS, isArea?4.0*stats.area/(Math.PI*stats.major*stats.major):0.0);
//                rt.addValue(ResultsTable.SOLIDITY, isArea?stats.pixelCount/convexArea:Double.NaN);
//                if (rt.size()==1) {
//                    rt.setDecimalPlaces(ResultsTable.CIRCULARITY, precision);
//                    rt.setDecimalPlaces(ResultsTable.ASPECT_RATIO, precision);
//                    rt.setDecimalPlaces(ResultsTable.ROUNDNESS, precision);
//                    rt.setDecimalPlaces(ResultsTable.SOLIDITY, precision);
//                }
//                //rt.addValue(ResultsTable.CONVEXITY, getConvexPerimeter(roi, ch)/perimeter);
//            }
//        }
//        if (measurements.contains(Measurement.BoundingRectangle)) {
//            if (roi!=null && roi.isLine()) {
//                Rectangle bounds = roi.getBounds();
//                double rx = bounds.x;
//                double ry = bounds.y;
//                double rw = bounds.width;
//                double rh = bounds.height;
//                Calibration cal = imp!=null?imp.getCalibration():null;
//                if (cal!=null) {
//                    rx = cal.getX(rx);
//                    ry = cal.getY(ry, imp.getHeight());
//                    rw *= cal.pixelWidth;
//                    rh *= cal.pixelHeight;
//                }
//                rt.addValue(ResultsTable.ROI_X, rx);
//                rt.addValue(ResultsTable.ROI_Y, ry);
//                rt.addValue(ResultsTable.ROI_WIDTH, rw);
//                rt.addValue(ResultsTable.ROI_HEIGHT, rh);
//            } else {
//                rt.addValue(ResultsTable.ROI_X,stats.roiX);
//                rt.addValue(ResultsTable.ROI_Y,stats.roiY);
//                rt.addValue(ResultsTable.ROI_WIDTH,stats.roiWidth);
//                rt.addValue(ResultsTable.ROI_HEIGHT,stats.roiHeight);
//            }
//        }
//        if (measurements.contains(Measurement.FitEllipse)) {
//            rt.addValue(ResultsTable.MAJOR,stats.major);
//            rt.addValue(ResultsTable.MINOR,stats.minor);
//            rt.addValue(ResultsTable.ANGLE,stats.angle);
//        }
//        if (measurements.contains(Measurement.FeretDiameter)) {
//            boolean extras = true;
//            double FeretDiameter=Double.NaN, feretAngle=Double.NaN, minFeret=Double.NaN,
//                    feretX=Double.NaN, feretY=Double.NaN;
//            Roi roi2 = roi;
//            if (roi2==null && imp!=null)
//                roi2 = new Roi(0, 0, imp.getWidth(), imp.getHeight());
//            if (roi2!=null) {
//                double[] a = roi2.getFeretValues();
//                if (a!=null) {
//                    FeretDiameter = a[0];
//                    feretAngle = a[1];
//                    minFeret = a[2];
//                    feretX = a[3];
//                    feretY = a[4];
//                }
//            }
//            rt.addValue(ResultsTable.FERET, FeretDiameter);
//            rt.addValue(ResultsTable.FERET_X, feretX);
//            rt.addValue(ResultsTable.FERET_Y, feretY);
//            rt.addValue(ResultsTable.FERET_ANGLE, feretAngle);
//            rt.addValue(ResultsTable.MIN_FERET, minFeret);
//        }
//        if (measurements.contains(Measurement.IntegratedDensity)) {
//            rt.addValue(ResultsTable.INTEGRATED_DENSITY,stats.area*stats.mean);
//            rt.addValue(ResultsTable.RAW_INTEGRATED_DENSITY,stats.pixelCount*stats.umean);
//        }
//        if (measurements.contains(Measurement.PixelValueMedian)) {
//            rt.addValue(ResultsTable.MEDIAN, stats.median);
//        }
//        if (measurements.contains(Measurement.PixelValueSkewness)) {
//            rt.addValue(ResultsTable.SKEWNESS, stats.skewness);
//        }
//        if (measurements.contains(Measurement.PixelValueKurtosis)) {
//            rt.addValue(ResultsTable.KURTOSIS, stats.kurtosis);
//        }
//        if (measurements.contains(Measurement.AreaFraction)) {
//            rt.addValue(ResultsTable.AREA_FRACTION, stats.areaFraction);
//        }
//        if (measurements.contains(Measurement.StackPosition)) {
//            boolean update = false;
//            if (imp!=null && (imp.isHyperStack()||imp.isComposite())) {
//                int[] position = imp.convertIndexToPosition(imp.getCurrentSlice());
//                if (imp.getNChannels()>1) {
//                    int index = rt.getColumnIndex("Ch");
//                    if (index<0 || !rt.columnExists(index)) update=true;
//                    rt.addValue("Ch", position[0]);
//                }
//                if (imp.getNSlices()>1) {
//                    int index = rt.getColumnIndex("Slice");
//                    if (index<0 || !rt.columnExists(index)) update=true;
//                    rt.addValue("Slice", position[1]);
//                }
//                if (imp.getNFrames()>1) {
//                    int index = rt.getColumnIndex("Frame");
//                    if (index<0 || !rt.columnExists(index)) update=true;
//                    rt.addValue("Frame", position[2]);
//                }
//            } else {
//                int index = rt.getColumnIndex("Slice");
//                if (index<0 || !rt.columnExists(index)) update=true;
//                rt.addValue("Slice", imp!=null?imp.getCurrentSlice():1.0);
//            }
//        }
//        if (roi!=null) {
//            if (roi.getType()==Roi.ANGLE) {
//                double angle = roi.getAngle();
//                if (Prefs.reflexAngle) angle = 360.0-angle;
//                rt.addValue("Angle", angle);
//            } else if (roi.isLine()) {
//                rt.addValue("Length", roi.getLength());
//                if (roi.getType()==Roi.LINE) {
//                    Line line = (Line)roi;
//                    rt.addValue("Angle", line.getFloatAngle(line.x1d,line.y1d,line.x2d,line.y2d));
//                }
//            } else if (roi instanceof PointRoi) {
//                savePoints(measurements, imp, rt, (PointRoi)roi);
//            }
//        }
////        if ((measurements&LIMIT)!=0 && imp!=null && imp.getBitDepth()!=24) {
////            rt.addValue(ResultsTable.MIN_THRESHOLD, stats.lowerThreshold);
////            rt.addValue(ResultsTable.MAX_THRESHOLD, stats.upperThreshold);
////        }
//        if (roi instanceof RotatedRectRoi) {
//            double[] p = ((RotatedRectRoi)roi).getParams();
//            double dx = p[2] - p[0];
//            double dy = p[3] - p[1];
//            double length = Math.sqrt(dx*dx+dy*dy);
//            Calibration cal = imp!=null?imp.getCalibration():null;
//            double pw = 1.0;
//            if (cal!=null && cal.pixelWidth==cal.pixelHeight)
//                pw = cal.pixelWidth;
//            rt.addValue("RRLength", length*pw);
//            rt.addValue("RRWidth", p[4]*pw);
//        }
//        int group = roi!=null?roi.getGroup():0;
//        if (group>0) {
//            rt.addValue("Group", group);
//            String name = Roi.getGroupName(group);
//            if (name!=null)
//                rt.addValue("GroupName", name);
//        }
//    }
//
//    public static void savePoints(ImageStatisticsSetParameter measurements, ImageProcessor ip, ImageSliceIndex sliceIndex, Calibration calibration, ResultsTable rt, PointRoi roi) {
//        if (measurements.contains(Measurement.Area)) {
//            rt.addValue(ResultsTable.AREA,0);
//        }
//        FloatPolygon p = roi.getFloatPolygon();
//        double x = p.xpoints[0];
//        double y = p.ypoints[0];
//        rt.addValue("X", calibration != null ? calibration.getX(x) : 0);
//        rt.addValue("Y", calibration != null ? calibration.getY(y, ip.getHeight()) : 0);
//        rt.addValue("Slice", sliceIndex.getZ() + 1);
//        rt.addValue("Ch", sliceIndex.getC() + 1);
//        rt.addValue("Frame", sliceIndex.getT() + 1);
//        int[] info = roi.getCounterInfo();
//        if (info!=null) {
//            rt.addValue("Counter", info[0]);
//            rt.addValue("Count", info[1]);
//        }

    /// /        if (imp.getProperty("FHT")!=null) {
    /// /            double center = imp.getWidth()/2.0;
    /// /            y = imp.getHeight()-y-1;
    /// /            double r = Math.sqrt((x-center)*(x-center) + (y-center)*(y-center));
    /// /            if (r<1.0) r = 1.0;
    /// /            double theta = Math.atan2(y-center, x-center);
    /// /            theta = theta*180.0/Math.PI;
    /// /            if (theta<0) theta = 360.0+theta;
    /// /            rt.addValue("R", (imp.getWidth()/r)*cal.pixelWidth);
    /// /            rt.addValue("Theta", theta);
    /// /        }
//    }
//
//    public static double getArea(Polygon p) {
//        if (p==null) return Double.NaN;
//        int carea = 0;
//        int iminus1;
//        for (int i=0; i<p.npoints; i++) {
//            iminus1 = i-1;
//            if (iminus1<0) iminus1=p.npoints-1;
//            carea += (p.xpoints[i]+p.xpoints[iminus1])*(p.ypoints[i]-p.ypoints[iminus1]);
//        }
//        return (Math.abs(carea/2.0));
//    }
//
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
