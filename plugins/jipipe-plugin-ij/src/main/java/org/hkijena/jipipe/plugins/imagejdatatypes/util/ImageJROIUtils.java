package org.hkijena.jipipe.plugins.imagejdatatypes.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.measure.Measurements;
import ij.plugin.RoiScaler;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.SerializationUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class ImageJROIUtils {
    public static ResultsTableData measureROI(Roi roi, ImagePlus reference, boolean physicalUnits, Measurement... statistics) {
        ImageStatisticsSetParameter statisticsSetParameter = new ImageStatisticsSetParameter();
        if (statistics.length > 0) {
            statisticsSetParameter.getValues().clear();
            for (Measurement statistic : statistics) {
                statisticsSetParameter.getValues().add(statistic);
            }
        }
        ROI2DListData dummy = new ROI2DListData();
        dummy.add(roi);
        return dummy.measure(reference, statisticsSetParameter, true, physicalUnits);
    }

    public static Roi intersectROI(Roi roi1, Roi roi2) {
        ROI2DListData dummy = new ROI2DListData();
        dummy.add(roi1);
        dummy.add(roi2);
        dummy.logicalAnd();
        if (!dummy.isEmpty()) {
            return dummy.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns the properties of a {@link Roi} as map
     *
     * @param roi the roi
     * @return the properties
     */
    public static Map<String, String> getRoiProperties(Roi roi) {
        Properties props = new Properties();
        String properties = roi.getProperties();
        if (!StringUtils.isNullOrEmpty(properties)) {
            try {
                InputStream is = new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8));
                props.load(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        HashMap<String, String> map = new HashMap<>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            map.put("" + entry.getKey(), "" + entry.getValue());
        }
        return map;
    }

    /**
     * Sets the properties of a {@link Roi} from a map
     *
     * @param roi        the roi
     * @param properties the properties
     */
    public static void setRoiProperties(Roi roi, Map<String, String> properties) {
        Properties props = new Properties();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            props.setProperty(StringUtils.nullToEmpty(entry.getKey()).replace(' ', '_').replace('=', '_').replace(':', '_'), entry.getValue());
        }
        try {
            StringWriter writer = new StringWriter();
            props.store(writer, null);
            writer.flush();
            roi.setProperties(writer.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets ROI properties from a JSON string or
     *
     * @param roi        the ROI
     * @param properties the properties (must be JSON, or is set as value of fallbackKey). If empty, nothing happens.
     */
    public static void setRoiPropertiesFromString(Roi roi, String properties, String fallbackKey) {
        if (!StringUtils.isNullOrEmpty(properties)) {
            Map<String, String> map = new HashMap<>();
            try {
                JsonNode jsonNode = SerializationUtils.jsonStringToObject(properties, JsonNode.class);
                for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(jsonNode.fields())) {
                    map.put(entry.getKey(), entry.getValue().asText());
                }
            } catch (Exception ignored) {
                map.put(fallbackKey, properties);
            }
            if (!map.isEmpty()) {
                setRoiProperties(roi, map);
            }
        }
    }

    /**
     * Uses reflection to manually set the canvas of a {@link Roi}
     * Please note that the image of the Roi will be set.
     *
     * @param roi       the ROI
     * @param imagePlus the image
     * @param canvas    the canvas
     */
    public static void setRoiCanvas(Roi roi, ImagePlus imagePlus, ImageCanvas canvas) {
        // First set the image
        roi.setImage(imagePlus);
        // We have to set the canvas or overlay rendering will fail
        try {
            Field field = Roi.class.getDeclaredField("ic");
            field.setAccessible(true);
            field.set(roi, canvas);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyRoiAttributesAndLocation(Roi source, Roi target) {
        target.copyAttributes(source);
        target.setPosition(source.getPosition());
        target.setPosition(source.getPosition(), source.getZPosition(), source.getZPosition());
    }

    static PolygonRoi trimPolygon(PolygonRoi roi, double length) {
        int[] x = roi.getXCoordinates();
        int[] y = roi.getYCoordinates();
        int n = roi.getNCoordinates();
        x = ImageJMathUtils.lineSmooth(x, n);
        y = ImageJMathUtils.lineSmooth(y, n);
        float[] curvature = ImageJMathUtils.getCurvature(x, y, n, ImageJMathUtils.DEFAULT_CURVATURE_KERNEL);
        Rectangle r = roi.getBounds();
        double threshold = ImageJMathUtils.rodbard(length);
        //IJ.log("trim: "+length+" "+threshold);
        double distance = Math.sqrt((x[1] - x[0]) * (x[1] - x[0]) + (y[1] - y[0]) * (y[1] - y[0]));
        x[0] += r.x;
        y[0] += r.y;
        int i2 = 1;
        int x1, y1, x2 = 0, y2 = 0;
        for (int i = 1; i < n - 1; i++) {
            x1 = x[i];
            y1 = y[i];
            x2 = x[i + 1];
            y2 = y[i + 1];
            distance += Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)) + 1;
            distance += curvature[i] * 2;
            if (distance >= threshold) {
                x[i2] = x2 + r.x;
                y[i2] = y2 + r.y;
                i2++;
                distance = 0.0;
            }
        }
        int type = roi.getType() == Roi.FREELINE ? Roi.POLYLINE : Roi.POLYGON;
        if (type == Roi.POLYLINE && distance > 0.0) {
            x[i2] = x2 + r.x;
            y[i2] = y2 + r.y;
            i2++;
        }
        return new PolygonRoi(x, y, i2, type);
    }

    public static PolygonRoi trimFloatPolygon(PolygonRoi roi, double length) {
        FloatPolygon poly = roi.getFloatPolygon();
        float[] x = poly.xpoints;
        float[] y = poly.ypoints;
        int n = poly.npoints;
        x = ImageJMathUtils.lineSmooth(x, n);
        y = ImageJMathUtils.lineSmooth(y, n);
        float[] curvature = ImageJMathUtils.getCurvature(x, y, n, ImageJMathUtils.DEFAULT_CURVATURE_KERNEL);
        double threshold = ImageJMathUtils.rodbard(length);
        //IJ.log("trim: "+length+" "+threshold);
        double distance = Math.sqrt((x[1] - x[0]) * (x[1] - x[0]) + (y[1] - y[0]) * (y[1] - y[0]));
        int i2 = 1;
        double x1, y1, x2 = 0, y2 = 0;
        for (int i = 1; i < n - 1; i++) {
            x1 = x[i];
            y1 = y[i];
            x2 = x[i + 1];
            y2 = y[i + 1];
            distance += Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)) + 1;
            distance += curvature[i] * 2;
            if (distance >= threshold) {
                x[i2] = (float) x2;
                y[i2] = (float) y2;
                i2++;
                distance = 0.0;
            }
        }
        int type = roi.getType() == Roi.FREELINE ? Roi.POLYLINE : Roi.POLYGON;
        if (type == Roi.POLYLINE && distance > 0.0) {
            x[i2] = (float) x2;
            y[i2] = (float) y2;
            i2++;
        }
        return new PolygonRoi(x, y, i2, type);
    }

    /**
     * Port of the fitSpline function from {@link ij.plugin.Selection}
     *
     * @param roi input Roi
     * @return output Roi (null if no circle can be fit)
     */
    public static Roi fitSplineToRoi(Roi roi, boolean straighten, boolean remove) {
        int type = roi.getType();
        boolean segmentedSelection = type == Roi.POLYGON || type == Roi.POLYLINE;
        if (!(segmentedSelection || type == Roi.FREEROI || type == Roi.TRACED_ROI || type == Roi.FREELINE)) {
            return null;
        }
        if (roi instanceof EllipseRoi)
            return null;
        PolygonRoi p = (PolygonRoi) roi.clone();
        if (!segmentedSelection && p.getNCoordinates() > 3) {
            if (p.subPixelResolution())
                p = trimFloatPolygon(p, p.getUncalibratedLength());
            else
                p = trimPolygon(p, p.getUncalibratedLength());
        }
        if (straighten)
            p.fitSplineForStraightening();
        else if (remove)
            p.removeSplineFit();
        else
            p.fitSpline();

        return p;
    }

    /**
     * Port of the createEllipse function from {@link ij.plugin.Selection}
     *
     * @param roi input Roi
     * @return output Roi (null if no circle can be fit)
     */
    public static Roi fitEllipseToRoi(Roi roi) {
        if (roi.isLine()) {
            return null;
        }

        ROI2DListData listData = new ROI2DListData();
        listData.add(roi);
        ImagePlus dummyImage = listData.createDummyImage();
        ImageProcessor ip = dummyImage.getProcessor();
        ip.setRoi(roi);

        int options = Measurements.CENTROID + Measurements.ELLIPSE;
        ImageStatistics stats = ImageStatistics.getStatistics(ip, options, null);
        double dx = stats.major * Math.cos(stats.angle / 180.0 * Math.PI) / 2.0;
        double dy = -stats.major * Math.sin(stats.angle / 180.0 * Math.PI) / 2.0;
        double x1 = stats.xCentroid - dx;
        double x2 = stats.xCentroid + dx;
        double y1 = stats.yCentroid - dy;
        double y2 = stats.yCentroid + dy;
        double aspectRatio = stats.minor / stats.major;

        Roi roi2 = new EllipseRoi(x1, y1, x2, y2, aspectRatio);
        copyRoiAttributesAndLocation(roi, roi2);

        return roi2;
    }

    /**
     * Port of the fitCircle function from {@link ij.plugin.Selection}
     *
     * @param roi input Roi
     * @return output Roi (null if no circle can be fit)
     */
    public static Roi fitCircleToRoi(Roi roi) {
        if (roi.isArea()) {      //create circle with the same area and centroid
            ROI2DListData listData = new ROI2DListData();
            listData.add(roi);
            ImagePlus dummyImage = listData.createDummyImage();
            ImageProcessor ip = dummyImage.getProcessor();
            ip.setRoi(roi);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, Measurements.AREA + Measurements.CENTROID, null);
            double r = Math.sqrt(stats.pixelCount / Math.PI);
            int d = (int) Math.round(2.0 * r);
            Roi roi2 = new OvalRoi((int) Math.round(stats.xCentroid - r), (int) Math.round(stats.yCentroid - r), d, d);
            copyRoiAttributesAndLocation(roi, roi2);
            return roi2;
        }

        Polygon poly = roi.getPolygon();
        int n = poly.npoints;
        int[] x = poly.xpoints;
        int[] y = poly.ypoints;
        if (n < 3) {
            return null;
        }

        // calculate point centroid
        double sumx = 0, sumy = 0;
        for (int i = 0; i < n; i++) {
            sumx = sumx + poly.xpoints[i];
            sumy = sumy + poly.ypoints[i];
        }
        double meanx = sumx / n;
        double meany = sumy / n;

        // calculate moments
        double[] X = new double[n], Y = new double[n];
        double Mxx = 0, Myy = 0, Mxy = 0, Mxz = 0, Myz = 0, Mzz = 0;
        for (int i = 0; i < n; i++) {
            X[i] = x[i] - meanx;
            Y[i] = y[i] - meany;
            double Zi = X[i] * X[i] + Y[i] * Y[i];
            Mxy = Mxy + X[i] * Y[i];
            Mxx = Mxx + X[i] * X[i];
            Myy = Myy + Y[i] * Y[i];
            Mxz = Mxz + X[i] * Zi;
            Myz = Myz + Y[i] * Zi;
            Mzz = Mzz + Zi * Zi;
        }
        Mxx = Mxx / n;
        Myy = Myy / n;
        Mxy = Mxy / n;
        Mxz = Mxz / n;
        Myz = Myz / n;
        Mzz = Mzz / n;

        // calculate the coefficients of the characteristic polynomial
        double Mz = Mxx + Myy;
        double Cov_xy = Mxx * Myy - Mxy * Mxy;
        double Mxz2 = Mxz * Mxz;
        double Myz2 = Myz * Myz;
        double A2 = 4 * Cov_xy - 3 * Mz * Mz - Mzz;
        double A1 = Mzz * Mz + 4 * Cov_xy * Mz - Mxz2 - Myz2 - Mz * Mz * Mz;
        double A0 = Mxz2 * Myy + Myz2 * Mxx - Mzz * Cov_xy - 2 * Mxz * Myz * Mxy + Mz * Mz * Cov_xy;
        double A22 = A2 + A2;
        double epsilon = 1e-12;
        double ynew = 1e+20;
        int IterMax = 20;
        double xnew = 0;
        int iterations = 0;

        // Newton's method starting at x=0
        for (int iter = 1; iter <= IterMax; iter++) {
            iterations = iter;
            double yold = ynew;
            ynew = A0 + xnew * (A1 + xnew * (A2 + 4. * xnew * xnew));
            if (Math.abs(ynew) > Math.abs(yold)) {
                if (IJ.debugMode) IJ.log("Fit Circle: wrong direction: |ynew| > |yold|");
                xnew = 0;
                break;
            }
            double Dy = A1 + xnew * (A22 + 16 * xnew * xnew);
            double xold = xnew;
            xnew = xold - ynew / Dy;
            if (Math.abs((xnew - xold) / xnew) < epsilon)
                break;
            if (iter >= IterMax) {
                if (IJ.debugMode) IJ.log("Fit Circle: will not converge");
                xnew = 0;
            }
            if (xnew < 0) {
                if (IJ.debugMode) IJ.log("Fit Circle: negative root:  x = " + xnew);
                xnew = 0;
            }
        }
        if (IJ.debugMode) IJ.log("Fit Circle: n=" + n + ", xnew=" + IJ.d2s(xnew, 2) + ", iterations=" + iterations);

        // calculate the circle parameters
        double DET = xnew * xnew - xnew * Mz + Cov_xy;
        double CenterX = (Mxz * (Myy - xnew) - Myz * Mxy) / (2 * DET);
        double CenterY = (Myz * (Mxx - xnew) - Mxz * Mxy) / (2 * DET);
        double radius = Math.sqrt(CenterX * CenterX + CenterY * CenterY + Mz + 2 * xnew);
        if (Double.isNaN(radius)) {
            return null;
        }
        CenterX = CenterX + meanx;
        CenterY = CenterY + meany;

        Roi result = new OvalRoi((int) Math.round(CenterX - radius), (int) Math.round(CenterY - radius), (int) Math.round(2 * radius), (int) Math.round(2 * radius));
        copyRoiAttributesAndLocation(roi, result);
        return result;
    }

    public static Roi interpolateRoi(Roi roi, double interval, boolean smooth, boolean adjust) {
        if (roi.getType() == Roi.POINT)
            return null;
        int sign = adjust ? -1 : 1;
        Roi newRoi = null;
        if (roi instanceof ShapeRoi && ((ShapeRoi) roi).getRois().length > 1) {
            // handle composite roi, thanks to Michael Ellis
            Roi[] rois = ((ShapeRoi) roi).getRois();
            ShapeRoi newShapeRoi = null;
            for (Roi roi2 : rois) {
                FloatPolygon fPoly = roi2.getInterpolatedPolygon(interval, smooth);
                PolygonRoi polygon = new PolygonRoi(fPoly, PolygonRoi.POLYGON);
                if (newShapeRoi == null) // First Roi is the outer boundary
                    newShapeRoi = new ShapeRoi(polygon);
                else {
                    // Assume subsequent Rois are holes to be subtracted
                    ShapeRoi tempRoi = new ShapeRoi(polygon);
                    tempRoi.not(newShapeRoi);
                    newShapeRoi = tempRoi;
                }
            }
            newRoi = newShapeRoi;
        } else {
            FloatPolygon poly = roi.getInterpolatedPolygon(sign * interval, smooth);
            int t = roi.getType();
            int type = roi.isLine() ? Roi.FREELINE : Roi.FREEROI;
            if (t == Roi.POLYGON && interval > 1.0)
                type = Roi.POLYGON;
            if ((t == Roi.RECTANGLE || t == Roi.OVAL || t == Roi.FREEROI) && interval >= 8.0)
                type = Roi.POLYGON;
            if ((t == Roi.LINE || t == Roi.FREELINE) && interval >= 8.0)
                type = Roi.POLYLINE;
            if (t == Roi.POLYLINE && interval >= 8.0)
                type = Roi.POLYLINE;
//            ImageCanvas ic = null;
//            if (poly.npoints<=150 && ic!=null && ic.getMagnification()>=12.0)
//                type = roi.isLine()?Roi.POLYLINE:Roi.POLYGON;
            newRoi = new PolygonRoi(poly, type);
        }

        copyRoiAttributesAndLocation(roi, newRoi);
        return newRoi;
    }

    public static java.util.List<Point2D> getContourPoints(Roi roi) {
        List<Point2D> points = new ArrayList<>();

        if (roi == null) return points;

        if (roi instanceof PointRoi) {
            PointRoi pointRoi = (PointRoi) roi;
            float[] x = pointRoi.getFloatPolygon().xpoints;
            float[] y = pointRoi.getFloatPolygon().ypoints;
            for (int i = 0; i < pointRoi.getNCoordinates(); i++) {
                points.add(new Point2D.Float(x[i], y[i]));
            }

        } else if (roi instanceof PolygonRoi) {
            PolygonRoi polyRoi = (PolygonRoi) roi;
            float[] x = polyRoi.getFloatPolygon().xpoints;
            float[] y = polyRoi.getFloatPolygon().ypoints;
            for (int i = 0; i < polyRoi.getNCoordinates(); i++) {
                points.add(new Point2D.Float(x[i], y[i]));
            }

        } else if (roi instanceof ShapeRoi) {
            ShapeRoi shapeRoi = (ShapeRoi) roi;
            PathIterator it = shapeRoi.getShape().getPathIterator(null, 0.5);
            float[] coords = new float[6];
            while (!it.isDone()) {
                int type = it.currentSegment(coords);
                if (type != PathIterator.SEG_CLOSE) {
                    points.add(new Point2D.Float(coords[0], coords[1]));
                }
                it.next();
            }

        } else if (roi instanceof Line) {
            Line line = (Line) roi;
            points.add(new Point2D.Float((float) line.x1d, (float) line.y1d));
            points.add(new Point2D.Float((float) line.x2d, (float) line.y2d));

        } else {
            Polygon poly = roi.getPolygon();
            for (int i = 0; i < poly.npoints; i++) {
                points.add(new Point2D.Float(poly.xpoints[i], poly.ypoints[i]));
            }
        }

        return points;
    }

    public static Roi areaToLine(Roi roi) {
        if (roi == null || !roi.isArea()) {
            return null;
        }
        Polygon p = roi.getPolygon();
        FloatPolygon fp = (roi.subPixelResolution()) ? roi.getFloatPolygon() : null;
        if (p == null && fp == null)
            return null;
        int type1 = roi.getType();
        if (type1 == Roi.COMPOSITE) {
            return null;
        }
//        if (fp == null && type1 == Roi.TRACED_ROI) {
//            for (int i = 0; i < p.npoints; i++) {
//                if (p.xpoints[i] >= imp.getWidth()) {
//                    p.xpoints[i] = imp.getWidth() - 1;
//                }
//                if (p.ypoints[i] >= imp.getHeight()) {
//                    p.ypoints[i] = imp.getHeight() - 1;
//                }
//            }
//        }
        int type2 = Roi.POLYLINE;
        if (type1 == Roi.OVAL || type1 == Roi.FREEROI || type1 == Roi.TRACED_ROI
                || ((roi instanceof PolygonRoi) && ((PolygonRoi) roi).isSplineFit()))
            type2 = Roi.FREELINE;
        Roi roi2 = fp == null ? new PolygonRoi(p, type2) : new PolygonRoi(fp, type2);
        copyRoiAttributesAndLocation(roi, roi2);
        Rectangle2D.Double bounds = roi.getFloatBounds();
        roi2.setLocation(bounds.x - 0.5, bounds.y - 0.5);    //area and line roi coordinates are 0.5 pxl different
        return roi2;
    }

    public static Roi scaleRoiAroundOrigin(Roi roi, double sx, double sy, double originX, double originY) {
        // Step 1: Compute current position and offset
        Rectangle bounds = roi.getBounds();
        double dx = bounds.x - originX;
        double dy = bounds.y - originY;

        // Step 2: Move ROI so that origin becomes (0, 0) in local space
        roi.setLocation(originX, originY);

        // Step 3: Scale the ROI around the new origin (its new (0, 0))
        Roi scaled = RoiScaler.scale(roi, sx, sy, false);

        // Step 4: Move ROI back to compensate for the initial offset scaled by the scale factors
        double newX = originX + dx * sx;
        double newY = originY + dy * sy;
        scaled.setLocation((int) Math.round(newX), (int) Math.round(newY));

        return scaled;
    }
}
