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

import ij.*;
import ij.gui.*;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.MeasurementsWriter;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.text.TextPanel;

import java.awt.*;

/**
 * Copy of {@link ij.plugin.filter.Analyzer} that removes some of the GUI-dependent functions and also does not rely on static functions.
 */
public class CustomAnalyzer implements Measurements {

    // Order must agree with order of checkboxes in Set Measurements dialog box
    private static final int[] list = {AREA, MEAN, STD_DEV, MODE, MIN_MAX,
            CENTROID, CENTER_OF_MASS, PERIMETER, RECT, ELLIPSE, SHAPE_DESCRIPTORS, FERET,
            INTEGRATED_DENSITY, MEDIAN, SKEWNESS, KURTOSIS, AREA_FRACTION, STACK_POSITION,
            LIMIT, LABELS, INVERT_Y, SCIENTIFIC_NOTATION, ADD_TO_OVERLAY, NaN_EMPTY_CELLS};
    private final String MEASUREMENTS = "measurements";
    private final String MARK_WIDTH = "mark.width";
    private final String PRECISION = "precision";
    public Color darkBlue = new Color(0, 0, 160);
    public int markWidth;
    public int precision = Prefs.getInt(PRECISION, 3);
    private boolean drawLabels = true;
    private String arg;
    private ImagePlus imp;
    private ResultsTable rt;
    private int measurements;
    private StringBuffer min, max, mean, sd;
    private boolean disableReset;
    private boolean resultsUpdated;
    private boolean unsavedMeasurements;
    private int systemMeasurements = Prefs.getInt(MEASUREMENTS, AREA + MEAN + MIN_MAX);
    private float[] umeans = new float[MAX_STANDARDS];
    private int redirectTarget;
    private String redirectTitle = "";
    private ImagePlus redirectImage; // non-displayed images
    private int firstParticle, lastParticle;
    private boolean switchingModes;
    private boolean showMin = true;
    private boolean showAngle = true;

    public CustomAnalyzer() {
        rt = new ResultsTable();
        rt.setIsResultsTable(true);
        rt.showRowNumbers(true);
        rt.setPrecision((systemMeasurements & SCIENTIFIC_NOTATION) != 0 ? -precision : precision);
        rt.setNaNEmptyCells((systemMeasurements & NaN_EMPTY_CELLS) != 0);
        measurements = systemMeasurements;
    }

    /**
     * Constructs a new Analyzer using the specified ImagePlus object
     * and the current measurement options and default results table.
     */
    public CustomAnalyzer(ImagePlus imp) {
        this();
        this.imp = imp;
    }

    /**
     * Construct a new Analyzer using an ImagePlus object and a ResultsTable.
     */
    public CustomAnalyzer(ImagePlus imp, ResultsTable rt) {
        this(imp, ij.plugin.filter.Analyzer.getMeasurements(), rt);
    }

    /**
     * Construct a new Analyzer using an ImagePlus object and private
     * measurement options and a ResultsTable.
     */
    public CustomAnalyzer(ImagePlus imp, int measurements, ResultsTable rt) {
        this.imp = imp;
        this.measurements = measurements;
        if (rt == null)
            rt = new ResultsTable();
        rt.setPrecision((systemMeasurements & SCIENTIFIC_NOTATION) != 0 ? -precision : precision);
        rt.setNaNEmptyCells((systemMeasurements & NaN_EMPTY_CELLS) != 0);
        this.rt = rt;
    }

    private void addRoiToOverlay() {
        Roi roi = imp.getRoi();
        if (roi == null)
            return;
        roi = (Roi) roi.clone();
        if (imp.getStackSize() > 1) {
            if (imp.isHyperStack() || imp.isComposite())
                roi.setPosition(0, imp.getSlice(), imp.getFrame());
            else
                roi.setPosition(imp.getCurrentSlice());
        }
        if (roi.getName() == null)
            roi.setName("" + rt.size());
        //roi.setName(IJ.getString("Label:", "m"+rt.size()));
        roi.setIgnoreClipRect(true);
        Overlay overlay = imp.getOverlay();
        if (overlay == null)
            overlay = new Overlay();
        if (drawLabels)
            overlay.drawLabels(true);
        if (!overlay.getDrawNames())
            overlay.drawNames(true);
        overlay.setLabelColor(Color.white);
        overlay.drawBackgrounds(true);
        overlay.add(roi);
        imp.setOverlay(overlay);
        if (roi.getType() == Roi.COMPOSITE && Toolbar.getToolId() == Toolbar.OVAL && Toolbar.getBrushSize() > 0)
            imp.deleteRoi();  // delete ROIs created with the selection brush tool
    }

    /**
     * Measures the image or selection and adds the results to the default results table.
     */
    public void measure() {
        String lastHdr = rt.getColumnHeading(ResultsTable.LAST_HEADING);
        if (lastHdr == null || lastHdr.charAt(0) != 'M') {
            if (!reset()) return;
        }
        firstParticle = lastParticle = 0;
        Roi roi = imp.getRoi();
        if (roi != null && roi.getType() == Roi.POINT) {
            measurePoint(roi);
            return;
        }
        if (roi != null && roi.getType() == Roi.ANGLE) {
            measureAngle(roi);
            return;
        }
        if (roi != null && roi.isLine()) {
            measureLength(roi);
            return;
        }
        ImageStatistics stats;
        if (isRedirectImage()) {
            stats = getRedirectStats(measurements, roi);
            if (stats == null) return;
        } else
            stats = imp.getStatistics(measurements);
        if (!IJ.isResultsWindow() && IJ.getInstance() != null)
            reset();
        saveResults(stats, roi);
    }

	/*
	void showHeadings() {
		String[] headings = rt.getHeadings();
		int columns = headings.length;
		if (columns==0)
			return;
		IJ.log("Headings: "+headings.length+" "+rt.getColumnHeading(ResultsTable.LAST_HEADING));
		for (int i=0; i<columns; i++) {
			if (headings[i]!=null)
				IJ.log("   "+i+" "+headings[i]+" "+rt.getColumnIndex(headings[i]));
		}
	}
	*/

    public boolean reset() {
        boolean ok = true;
        if (rt.size() > 0 && !disableReset)
            ok = resetCounter();
        if (ok && rt.getColumnHeading(ResultsTable.LAST_HEADING) == null)
            rt.setDefaultHeadings();
        return ok;
    }

    /**
     * Returns <code>true</code> if an image is selected in the "Redirect To:"
     * popup menu of the Analyze/Set Measurements dialog box.
     */
    public boolean isRedirectImage() {
        return redirectTarget != 0;
    }

    /**
     * Set the "Redirect To" image. Pass 'null' as the
     * argument to disable redirected sampling.
     */
    public void setRedirectImage(ImagePlus imp) {
        if (imp == null) {
            redirectTarget = 0;
            redirectTitle = null;
            redirectImage = null;
        } else {
            redirectTarget = imp.getID();
            redirectTitle = imp.getTitle();
            if (imp.getWindow() == null)
                redirectImage = imp;
        }
    }

    private ImagePlus getRedirectImageOrStack(ImagePlus cimp) {
        ImagePlus rimp = getRedirectImage(cimp);
        if (rimp != null) {
            int depth = rimp.getStackSize();
            if (depth > 1 && depth == cimp.getStackSize() && rimp.getCurrentSlice() != cimp.getCurrentSlice())
                rimp.setSlice(cimp.getCurrentSlice());
        }
        return rimp;
    }

    /**
     * Returns the image selected in the "Redirect To:" popup
     * menu of the Analyze/Set Measurements dialog, or null
     * if "None" is selected, the image was not found or the
     * image is not the same size as <code>currentImage</code>.
     */
    public ImagePlus getRedirectImage(ImagePlus cimp) {
        ImagePlus rimp = WindowManager.getImage(redirectTarget);
        if (rimp == null)
            rimp = redirectImage;
        if (rimp == null) {
            IJ.error("Analyzer", "Redirect image (\"" + redirectTitle + "\")\n"
                    + "not found.");
            redirectTarget = 0;
            Macro.abort();
            return null;
        }
        if (rimp.getWidth() != cimp.getWidth() || rimp.getHeight() != cimp.getHeight()) {
            IJ.error("Analyzer", "Redirect image (\"" + redirectTitle + "\") \n"
                    + "is not the same size as the current image.");
            Macro.abort();
            return null;
        }
        return rimp;
    }

    public ImageStatistics getRedirectStats(int measurements, Roi roi) {
        ImagePlus redirectImp = getRedirectImageOrStack(imp);
        if (redirectImp == null)
            return null;
        ImageProcessor ip = redirectImp.getProcessor();
        if (imp.getTitle().equals("mask") && imp.getBitDepth() == 8) {
            ip.setMask(imp.getProcessor());
            ip.setRoi(0, 0, imp.getWidth(), imp.getHeight());
        } else
            ip.setRoi(roi);
        return ImageStatistics.getStatistics(ip, measurements, redirectImp.getCalibration());
    }

    private void measurePoint(Roi roi) {
        FloatPolygon p = roi.getFloatPolygon();
        ImagePlus imp2 = isRedirectImage() ? getRedirectImageOrStack(imp) : null;
        if (imp2 == null) imp2 = imp;
        ImageStack stack = null;
        if (imp2.getStackSize() > 1)
            stack = imp2.getStack();
        PointRoi pointRoi = roi instanceof PointRoi ? (PointRoi) roi : null;
        for (int i = 0; i < p.npoints; i++) {
            int position = 0;
            if (pointRoi != null && p.npoints > 1)
                position = pointRoi.getPointPosition(i);
            ImageProcessor ip = null;
            if (stack != null && position > 0 && position <= stack.size())
                ip = stack.getProcessor(position);
            else
                ip = imp2.getProcessor();
            ip.setRoi((int) Math.round(p.xpoints[i]), (int) Math.round(p.ypoints[i]), 1, 1);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, imp2.getCalibration());
            stats.xCenterOfMass = p.xpoints[i];
            stats.yCenterOfMass = p.ypoints[i];
            PointRoi point = new PointRoi(p.xpoints[i], p.ypoints[i]);
            point.setPosition(position);
            if (pointRoi != null && pointRoi.getNCounters() > 1) {
                int[] counters = pointRoi.getCounters();
                if (counters != null && i < counters.length) {
                    int counter = counters[i] & 0xff;
                    int count = pointRoi.getCount(counter);
                    int[] info = new int[2];
                    info[0] = counter;
                    info[1] = count;
                    point.setCounterInfo(info);
                }
            }
            saveResults(stats, point);
        }
    }

    void measureAngle(Roi roi) {
        ImageProcessor ip = imp.getProcessor();
        ip.setRoi(roi.getPolygon());
        ImageStatistics stats = new ImageStatistics();
        saveResults(stats, roi);
    }

    void measureLength(Roi roi) {
        ImagePlus imp2 = isRedirectImage() ? getRedirectImageOrStack(imp) : null;
        if (imp2 != null)
            imp2.setRoi(roi);
        else
            imp2 = imp;
        if ((measurements & (AREA + MEAN + STD_DEV + MODE + MIN_MAX + CENTROID + MEDIAN)) == 0) {
            incrementCounter();
            rt.addValue("Length", roi.getLength());
            if (roi.getType() == Roi.LINE && showAngle) {
                Line line = (Line) roi;
                rt.addValue("Angle", line.getFloatAngle(line.x1d, line.y1d, line.x2d, line.y2d));
            }
            if ((measurements & LABELS) != 0)
                rt.addLabel("Label", getFileName());
            return;
        }
        boolean straightLine = roi.getType() == Roi.LINE;
        int lineWidth = (int) Math.round(roi.getStrokeWidth());
        ImageProcessor ip2 = imp2.getProcessor();
        double minThreshold = ip2.getMinThreshold();
        double maxThreshold = ip2.getMaxThreshold();
        int limit = (ij.plugin.filter.Analyzer.getMeasurements() & LIMIT) != 0 ? LIMIT : 0;
        boolean calibrated = imp2.getCalibration().calibrated();
        Rectangle saveR = null;
        Calibration globalCal = calibrated ? imp2.getGlobalCalibration() : null;
        Calibration localCal = null;
        if (globalCal != null) {
            imp2.setGlobalCalibration(null);
            localCal = imp2.getCalibration().copy();
            imp2.setCalibration(globalCal);
        }
        if (lineWidth > 1) {
            saveR = ip2.getRoi();
            ip2.setRoi(Roi.convertLineToArea(roi));
        } else if (calibrated && limit != 0) {
            Calibration cal = imp2.getCalibration().copy();
            imp2.getCalibration().disableDensityCalibration();
            ProfilePlot profile = new ProfilePlot(imp2);
            imp2.setCalibration(cal);
            double[] values = profile.getProfile();
            if (values == null) return;
            ip2 = new FloatProcessor(values.length, 1, values);
            ip2 = convertToOriginalDepth(imp2, ip2);
            ip2.setCalibrationTable(cal.getCTable());
        } else {
            ProfilePlot profile = new ProfilePlot(imp2);
            double[] values = profile.getProfile();
            if (values == null) return;
            ip2 = new FloatProcessor(values.length, 1, values);
            if (limit != 0)
                ip2 = convertToOriginalDepth(imp2, ip2);
        }
        if (limit != 0 && minThreshold != ImageProcessor.NO_THRESHOLD)
            ip2.setThreshold(minThreshold, maxThreshold, ImageProcessor.NO_LUT_UPDATE);
        ImageStatistics stats = ImageStatistics.getStatistics(ip2, AREA + MEAN + STD_DEV + MODE + MIN_MAX + MEDIAN + limit, imp2.getCalibration());
        if (saveR != null)
            ip2.setRoi(saveR);
        if ((roi instanceof Line) && (measurements & CENTROID) != 0) {
            FloatPolygon p = ((Line) roi).getFloatPoints();
            stats.xCentroid = p.xpoints[0] + (p.xpoints[1] - p.xpoints[0]) / 2.0;
            stats.yCentroid = p.ypoints[0] + (p.ypoints[1] - p.ypoints[0]) / 2.0;
            if (imp2 != null) {
                Calibration cal = imp.getCalibration();
                stats.xCentroid = cal.getX(stats.xCentroid);
                stats.yCentroid = cal.getY(stats.yCentroid, imp2.getHeight());
            }
        }
        saveResults(stats, roi);
        if (globalCal != null && localCal != null) {
            imp2.setGlobalCalibration(globalCal);
            imp2.setCalibration(localCal);
        }
    }

    private ImageProcessor convertToOriginalDepth(ImagePlus imp, ImageProcessor ip) {
        if (imp.getBitDepth() == 8)
            ip = ip.convertToByte(false);
        else if (imp.getBitDepth() == 16)
            ip = ip.convertToShort(false);
        return ip;
    }


    /**
     * Saves the measurements specified in the "Set Measurements" dialog,
     * or by calling setMeasurements(), in the default results table.
     */
    public void saveResults(ImageStatistics stats, Roi roi) {
        if (rt.getColumnHeading(ResultsTable.LAST_HEADING) == null)
            reset();
        incrementCounter();
        int counter = rt.size();
        if (counter <= MAX_STANDARDS && !(stats.umean == 0.0 && counter == 1 && umeans != null && umeans[0] != 0f)) {
            if (umeans == null) umeans = new float[MAX_STANDARDS];
            umeans[counter - 1] = (float) stats.umean;
        }
        if ((measurements & LABELS) != 0)
            rt.addLabel("Label", getFileName());
        if ((measurements & AREA) != 0) rt.addValue(ResultsTable.AREA, stats.area);
        if ((measurements & MEAN) != 0) rt.addValue(ResultsTable.MEAN, stats.mean);
        if ((measurements & STD_DEV) != 0) rt.addValue(ResultsTable.STD_DEV, stats.stdDev);
        if ((measurements & MODE) != 0) rt.addValue(ResultsTable.MODE, stats.dmode);
        if ((measurements & MIN_MAX) != 0) {
            if (showMin) rt.addValue(ResultsTable.MIN, stats.min);
            rt.addValue(ResultsTable.MAX, stats.max);
        }
        if ((measurements & CENTROID) != 0) {
            rt.addValue(ResultsTable.X_CENTROID, stats.xCentroid);
            rt.addValue(ResultsTable.Y_CENTROID, stats.yCentroid);
        }
        if ((measurements & CENTER_OF_MASS) != 0) {
            rt.addValue(ResultsTable.X_CENTER_OF_MASS, stats.xCenterOfMass);
            rt.addValue(ResultsTable.Y_CENTER_OF_MASS, stats.yCenterOfMass);
        }
        if ((measurements & PERIMETER) != 0 || (measurements & SHAPE_DESCRIPTORS) != 0) {
            double perimeter;
            if (roi != null)
                perimeter = roi.getLength();
            else
                perimeter = imp != null ? imp.getWidth() * 2 + imp.getHeight() * 2 : 0.0;
            if ((measurements & PERIMETER) != 0)
                rt.addValue(ResultsTable.PERIMETER, perimeter);
            if ((measurements & SHAPE_DESCRIPTORS) != 0) {
                double circularity = perimeter == 0.0 ? 0.0 : 4.0 * Math.PI * (stats.area / (perimeter * perimeter));
                if (circularity > 1.0) circularity = 1.0;
                rt.addValue(ResultsTable.CIRCULARITY, circularity);
                Polygon ch = null;
                boolean isArea = roi == null || roi.isArea();
                double convexArea = roi != null ? getArea(roi.getConvexHull()) : stats.pixelCount;
                rt.addValue(ResultsTable.ASPECT_RATIO, isArea ? stats.major / stats.minor : 0.0);
                rt.addValue(ResultsTable.ROUNDNESS, isArea ? 4.0 * stats.area / (Math.PI * stats.major * stats.major) : 0.0);
                rt.addValue(ResultsTable.SOLIDITY, isArea ? stats.pixelCount / convexArea : Double.NaN);
                if (rt.size() == 1) {
                    rt.setDecimalPlaces(ResultsTable.CIRCULARITY, precision);
                    rt.setDecimalPlaces(ResultsTable.ASPECT_RATIO, precision);
                    rt.setDecimalPlaces(ResultsTable.ROUNDNESS, precision);
                    rt.setDecimalPlaces(ResultsTable.SOLIDITY, precision);
                }
                //rt.addValue(ResultsTable.CONVEXITY, getConvexPerimeter(roi, ch)/perimeter);
            }
        }
        if ((measurements & RECT) != 0) {
            if (roi != null && roi.isLine()) {
                Rectangle bounds = roi.getBounds();
                double rx = bounds.x;
                double ry = bounds.y;
                double rw = bounds.width;
                double rh = bounds.height;
                Calibration cal = imp != null ? imp.getCalibration() : null;
                if (cal != null) {
                    rx = cal.getX(rx);
                    ry = cal.getY(ry, imp.getHeight());
                    rw *= cal.pixelWidth;
                    rh *= cal.pixelHeight;
                }
                rt.addValue(ResultsTable.ROI_X, rx);
                rt.addValue(ResultsTable.ROI_Y, ry);
                rt.addValue(ResultsTable.ROI_WIDTH, rw);
                rt.addValue(ResultsTable.ROI_HEIGHT, rh);
            } else {
                rt.addValue(ResultsTable.ROI_X, stats.roiX);
                rt.addValue(ResultsTable.ROI_Y, stats.roiY);
                rt.addValue(ResultsTable.ROI_WIDTH, stats.roiWidth);
                rt.addValue(ResultsTable.ROI_HEIGHT, stats.roiHeight);
            }
        }
        if ((measurements & ELLIPSE) != 0) {
            rt.addValue(ResultsTable.MAJOR, stats.major);
            rt.addValue(ResultsTable.MINOR, stats.minor);
            rt.addValue(ResultsTable.ANGLE, stats.angle);
        }
        if ((measurements & FERET) != 0) {
            boolean extras = true;
            double FeretDiameter = Double.NaN, feretAngle = Double.NaN, minFeret = Double.NaN,
                    feretX = Double.NaN, feretY = Double.NaN;
            Roi roi2 = roi;
            if (roi2 == null && imp != null)
                roi2 = new Roi(0, 0, imp.getWidth(), imp.getHeight());
            if (roi2 != null) {
                double[] a = roi2.getFeretValues();
                if (a != null) {
                    FeretDiameter = a[0];
                    feretAngle = a[1];
                    minFeret = a[2];
                    feretX = a[3];
                    feretY = a[4];
                }
            }
            rt.addValue(ResultsTable.FERET, FeretDiameter);
            rt.addValue(ResultsTable.FERET_X, feretX);
            rt.addValue(ResultsTable.FERET_Y, feretY);
            rt.addValue(ResultsTable.FERET_ANGLE, feretAngle);
            rt.addValue(ResultsTable.MIN_FERET, minFeret);
        }
        if ((measurements & INTEGRATED_DENSITY) != 0) {
            rt.addValue(ResultsTable.INTEGRATED_DENSITY, stats.area * stats.mean);
            rt.addValue(ResultsTable.RAW_INTEGRATED_DENSITY, stats.pixelCount * stats.umean);
        }
        if ((measurements & MEDIAN) != 0) rt.addValue(ResultsTable.MEDIAN, stats.median);
        if ((measurements & SKEWNESS) != 0) rt.addValue(ResultsTable.SKEWNESS, stats.skewness);
        if ((measurements & KURTOSIS) != 0) rt.addValue(ResultsTable.KURTOSIS, stats.kurtosis);
        if ((measurements & AREA_FRACTION) != 0) rt.addValue(ResultsTable.AREA_FRACTION, stats.areaFraction);
        if ((measurements & STACK_POSITION) != 0) {
            if (imp != null && (imp.isHyperStack() || imp.isComposite())) {
                int[] position = imp.convertIndexToPosition(imp.getCurrentSlice());
                if (imp.getNChannels() > 1) {
                    int index = rt.getColumnIndex("Ch");
                    if (index < 0 || !rt.columnExists(index)) ;
                    rt.addValue("Ch", position[0]);
                }
                if (imp.getNSlices() > 1) {
                    int index = rt.getColumnIndex("Slice");
                    if (index < 0 || !rt.columnExists(index)) ;
                    rt.addValue("Slice", position[1]);
                }
                if (imp.getNFrames() > 1) {
                    int index = rt.getColumnIndex("Frame");
                    if (index < 0 || !rt.columnExists(index)) ;
                    rt.addValue("Frame", position[2]);
                }
            } else {
                int index = rt.getColumnIndex("Slice");
                if (index < 0 || !rt.columnExists(index)) ;
                rt.addValue("Slice", imp != null ? imp.getCurrentSlice() : 1.0);
            }
        }
        if (roi != null) {
            if (roi.getType() == Roi.ANGLE) {
                double angle = roi.getAngle();
                if (Prefs.reflexAngle) angle = 360.0 - angle;
                rt.addValue("Angle", angle);
            } else if (roi.isLine()) {
                rt.addValue("Length", roi.getLength());
                if (roi.getType() == Roi.LINE && showAngle) {
                    Line line = (Line) roi;
                    rt.addValue("Angle", line.getFloatAngle(line.x1d, line.y1d, line.x2d, line.y2d));
                }
            } else if (roi instanceof PointRoi)
                savePoints((PointRoi) roi);
        }
        if ((measurements & LIMIT) != 0 && imp != null && imp.getBitDepth() != 24) {
            rt.addValue(ResultsTable.MIN_THRESHOLD, stats.lowerThreshold);
            rt.addValue(ResultsTable.MAX_THRESHOLD, stats.upperThreshold);
        }
        if (roi instanceof RotatedRectRoi) {
            double[] p = ((RotatedRectRoi) roi).getParams();
            double dx = p[2] - p[0];
            double dy = p[3] - p[1];
            double length = Math.sqrt(dx * dx + dy * dy);
            Calibration cal = imp != null ? imp.getCalibration() : null;
            double pw = 1.0;
            if (cal != null && cal.pixelWidth == cal.pixelHeight)
                pw = cal.pixelWidth;
            rt.addValue("RRLength", length * pw);
            rt.addValue("RRWidth", p[4] * pw);
        }
        int group = roi != null ? roi.getGroup() : 0;
        if (group > 0) {
            rt.addValue("Group", group);
            String name = Roi.getGroupName(group);
            if (name != null)
                rt.addValue("GroupName", name);
        }
    }

    final double getArea(Polygon p) {
        if (p == null) return Double.NaN;
        int carea = 0;
        int iminus1;
        for (int i = 0; i < p.npoints; i++) {
            iminus1 = i - 1;
            if (iminus1 < 0) iminus1 = p.npoints - 1;
            carea += (p.xpoints[i] + p.xpoints[iminus1]) * (p.ypoints[i] - p.ypoints[iminus1]);
        }
        return (Math.abs(carea / 2.0));
    }

    void savePoints(PointRoi roi) {
        if (imp == null) {
            rt.addValue("X", 0.0);
            rt.addValue("Y", 0.0);
            return;
        }
        if ((measurements & AREA) != 0)
            rt.addValue(ResultsTable.AREA, 0);
        FloatPolygon p = roi.getFloatPolygon();
        ImageProcessor ip = imp.getProcessor();
        Calibration cal = imp.getCalibration();
        double x = p.xpoints[0];
        double y = p.ypoints[0];
        int ix = (int) x, iy = (int) y;
        double value = ip.getPixelValue(ix, iy);
        if (markWidth > 0 && !Toolbar.getMultiPointMode()) {
            ip.setColor(Toolbar.getForegroundColor());
            ip.setLineWidth(markWidth);
            ip.moveTo(ix, iy);
            ip.lineTo(ix, iy);
            imp.updateAndDraw();
            ip.setLineWidth(Line.getWidth());
        }
        rt.addValue("X", cal.getX(x));
        rt.addValue("Y", cal.getY(y, imp.getHeight()));
        int position = roi.getPosition();
        if (imp.isHyperStack() || imp.isComposite()) {
            int channel = imp.getChannel();
            int slice = imp.getSlice();
            int frame = imp.getFrame();
            if (position > 0) {
                int[] pos = imp.convertIndexToPosition(position);
                channel = pos[0];
                slice = pos[1];
                frame = pos[2];
            }
            if (imp.getNChannels() > 1)
                rt.addValue("Ch", channel);
            if (imp.getNSlices() > 1)
                rt.addValue("Slice", slice);
            if (imp.getNFrames() > 1)
                rt.addValue("Frame", frame);
        } else if (imp.getStackSize() > 1) {
            if (position == 0)
                position = imp.getCurrentSlice();
            rt.addValue("Slice", position);
        }
        int[] info = roi.getCounterInfo();
        if (info != null) {
            rt.addValue("Counter", info[0]);
            rt.addValue("Count", info[1]);
        }
        if (imp.getProperty("FHT") != null) {
            double center = imp.getWidth() / 2.0;
            y = imp.getHeight() - y - 1;
            double r = Math.sqrt((x - center) * (x - center) + (y - center) * (y - center));
            if (r < 1.0) r = 1.0;
            double theta = Math.atan2(y - center, x - center);
            theta = theta * 180.0 / Math.PI;
            if (theta < 0) theta = 360.0 + theta;
            rt.addValue("R", (imp.getWidth() / r) * cal.pixelWidth);
            rt.addValue("Theta", theta);
        }
    }

    String getFileName() {
        String s = "";
        if (imp != null) {
            if (redirectTarget != 0) {
                ImagePlus rImp = WindowManager.getImage(redirectTarget);
                if (rImp == null) rImp = redirectImage;
                if (rImp != null) s = rImp.getTitle();
            } else
                s = imp.getTitle();
            //int len = s.length();
            //if (len>4 && s.charAt(len-4)=='.' && !Character.isDigit(s.charAt(len-1)))
            //	s = s.substring(0,len-4);
            Roi roi = imp.getRoi();
            String roiName = roi != null ? roi.getName() : null;
            if (roiName != null && !roiName.contains(".")) {
                if (roiName.length() > 30)
                    roiName = roiName.substring(0, 27) + "...";
                s += ":" + roiName;
            }
            if (imp.getStackSize() > 1) {
                ImageStack stack = imp.getStack();
                int currentSlice = imp.getCurrentSlice();
                String label = stack.getShortSliceLabel(currentSlice);
                String colon = s.equals("") ? "" : ":";
                if (label != null && !label.equals(""))
                    s += colon + label;
                else
                    s += colon + currentSlice;
            }
        }
        return s;
    }

    /**
     * Converts a number to a formatted string with a tab at the end.
     */
    public String n(double n) {
        String s;
        if (Math.round(n) == n)
            s = ResultsTable.d2s(n, 0);
        else
            s = ResultsTable.d2s(n, precision);
        return s + "\t";
    }

    void incrementCounter() {
        rt.incrementCounter();
        unsavedMeasurements = true;
    }

    /**
     * Sets the measurement counter to zero. Displays a dialog that
     * allows the user to save any existing measurements. Returns
     * false if the user cancels the dialog.
     */
    public boolean resetCounter() {
        TextPanel tp = null;
        int counter = rt.size();
        int lineCount = tp != null ? IJ.getTextPanel().getLineCount() : 0;
        ImageJ ij = IJ.getInstance();
        boolean macro = (IJ.macroRunning() && !switchingModes) || Interpreter.isBatchMode();
        switchingModes = false;
        if (counter > 0 && lineCount > 0 && unsavedMeasurements && !macro && ij != null && !ij.quitting()) {
            YesNoCancelDialog d = new YesNoCancelDialog(ij, "ImageJ", "Save " + counter + " measurements?");
            if (d.cancelPressed())
                return false;
            else if (d.yesPressed()) {
                if (!(new MeasurementsWriter()).save(""))
                    return false;
            }
        }
        umeans = null;
        rt.reset();
        RoiManager.resetMultiMeasureResults();
        unsavedMeasurements = false;
        if (tp != null) tp.clear();
        return true;
    }

    public void setUnsavedMeasurements(boolean b) {
        unsavedMeasurements = b;
    }

    // Returns the measurement options defined in the Set Measurements dialog. */
    public int getMeasurements() {
        return systemMeasurements;
    }

    /**
     * Sets the system-wide measurement options.
     */
    public void setMeasurements(int measurements) {
        systemMeasurements = measurements;
    }

    /**
     * Sets the specified system-wide measurement option.
     */
    public void setMeasurement(int option, boolean state) {
        if (state) {
            systemMeasurements |= option;
            if ((option & ADD_TO_OVERLAY) != 0)
                drawLabels = true;
        } else
            systemMeasurements &= ~option;
    }

    /**
     * Returns an array containing the first 20 uncalibrated means.
     */
    public float[] getUMeans() {
        return umeans;
    }

    /**
     * Returns the default results table. This table should only
     * be displayed in a the "Results" window.
     */
    public ResultsTable getResultsTable() {
        return rt;
    }

    public void setResultsTable(ResultsTable rt) {
        TextPanel tp = IJ.isResultsWindow() ? IJ.getTextPanel() : null;
        if (tp != null)
            tp.clear();
        if (rt == null)
            rt = new ResultsTable();
        rt.setPrecision((systemMeasurements & SCIENTIFIC_NOTATION) != 0 ? -precision : precision);
        rt.setNaNEmptyCells((systemMeasurements & NaN_EMPTY_CELLS) != 0);
        umeans = null;
        unsavedMeasurements = false;
    }

    /**
     * Returns the number of digits displayed to the right of decimal point.
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * Sets the number of digits displayed to the right of decimal point.
     */
    public void setPrecision(int decimalPlaces) {
        if (decimalPlaces < 0) decimalPlaces = 0;
        if (decimalPlaces > 9) decimalPlaces = 9;
        precision = decimalPlaces;
    }

    /**
     * Returns an updated Y coordinate based on
     * the current "Invert Y Coordinates" flag.
     */
    public int updateY(int y, int imageHeight) {
        if ((systemMeasurements & INVERT_Y) != 0)
            y = imageHeight - y - 1;
        return y;
    }

    /**
     * Returns an updated Y coordinate based on
     * the current "Invert Y Coordinates" flag.
     */
    public double updateY(double y, int imageHeight) {
        if ((systemMeasurements & INVERT_Y) != 0)
            y = imageHeight - y - 1;
        return y;
    }

    public void setOption(String option, boolean b) {
        if (option.contains("min"))
            showMin = b;
        else if (option.contains("angle"))
            showAngle = b;
    }

    public boolean addToOverlay() {
        return ((getMeasurements() & ADD_TO_OVERLAY) != 0);
    }

    public void drawLabels(boolean b) {
        drawLabels = b;
    }

    /**
     * Used by RoiManager.multiMeasure() to suppress save as dialogs.
     */
    public void disableReset(boolean b) {
        disableReset = b;
    }

}

