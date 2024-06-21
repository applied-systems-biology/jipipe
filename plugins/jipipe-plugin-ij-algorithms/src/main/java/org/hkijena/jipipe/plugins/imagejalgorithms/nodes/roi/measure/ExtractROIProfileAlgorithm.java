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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.measure;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import ij.ImagePlus;
import ij.gui.*;
import ij.measure.Calibration;
import ij.plugin.Straightener;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDataAnnotationNameParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SetJIPipeDocumentation(name = "Extract ROI profile", description = "Extracts the pixel intensities along the ROI if a straight or irregular line ROI are given. " +
        "If a rotated rectangle is processed, it is converted into a straight line." +
        "If any other ROI type is given, either the row or column average of the bounding rectangle is calculated.")
@AddJIPipeInputSlot(value = ROIListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Reference", create = true, description = "The profile(s) are created on this image")
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Measurements", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Measure")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze\nPlot profile")
public class ExtractROIProfileAlgorithm extends JIPipeIteratingAlgorithm {


    private boolean measureInPhysicalUnits = true;
    private RectangleMode rectangleMode = RectangleMode.Auto;
    private OptionalTextAnnotationNameParameter roiNameAnnotation = new OptionalTextAnnotationNameParameter("ROI Name", true);
    private OptionalTextAnnotationNameParameter roiIndexAnnotation = new OptionalTextAnnotationNameParameter("ROI Index", true);
    private OptionalDataAnnotationNameParameter roiDataAnnotation = new OptionalDataAnnotationNameParameter("ROI", true);

    public ExtractROIProfileAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractROIProfileAlgorithm(ExtractROIProfileAlgorithm other) {
        super(other);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.rectangleMode = other.rectangleMode;
        this.roiNameAnnotation = new OptionalTextAnnotationNameParameter(other.roiNameAnnotation);
        this.roiIndexAnnotation = new OptionalTextAnnotationNameParameter(other.roiIndexAnnotation);
        this.roiDataAnnotation = new OptionalDataAnnotationNameParameter(other.roiDataAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData rois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
        ImagePlus img = iterationStep.getInputData("Reference", ImagePlusGreyscaleData.class, progressInfo).getImage();
        Calibration calibration = measureInPhysicalUnits ? img.getCalibration() : null;

        for (int i = 0; i < rois.size(); i++) {
            Roi roi_ = rois.get(i);
            Roi roi = (Roi) roi_.clone();

            // Select the correct processor
            int c, z, t;
            if (roi.getCPosition() > 0) {
                c = roi.getCPosition() - 1;
            } else {
                c = 0;
            }
            if (roi.getZPosition() > 0) {
                z = roi.getZPosition() - 1;
            } else {
                z = 0;
            }
            if (roi.getTPosition() > 0) {
                t = roi.getTPosition() - 1;
            } else {
                t = 0;
            }

            ImageProcessor ip = ImageJUtils.getSliceZeroSafe(img, c, z, t);
            ImagePlus img2 = new ImagePlus("Slice", ip);
            img2.copyScale(img);
            ResultsTableData result;

            if (roi instanceof RotatedRectRoi) {
                // Replace with line ROI
                double[] p = ((RotatedRectRoi) roi).getParams();
                roi = new Line(p[0], p[1], p[2], p[3]);
                roi.setStrokeWidth(p[4]);
                roi.setImage(img2);
            }
            if (roi.getType() == Roi.LINE) {
                result = getStraightLineProfile(roi, calibration, ip);
            } else if (roi.getType() == Roi.POLYLINE || roi.getType() == Roi.FREELINE) {
                int lineWidth = Math.round(roi.getStrokeWidth());
                if (lineWidth <= 1) {
                    result = getIrregularProfile(roi, ip, calibration);
                } else {
                    result = getWideLineProfile(img2, lineWidth, calibration);
                }
            } else {
                Rectangle bounds = roi.getBounds();
                if (rectangleMode == RectangleMode.RowAverage) {
                    result = getRowAverageProfile(bounds, calibration, ip);
                } else if (rectangleMode == RectangleMode.ColumnAverage) {
                    result = getColumnAverageProfile(bounds, calibration, ip);
                } else if (rectangleMode == RectangleMode.Auto) {
                    if (bounds.getHeight() > bounds.getWidth()) {
                        result = getRowAverageProfile(bounds, calibration, ip);
                    } else {
                        result = getColumnAverageProfile(bounds, calibration, ip);
                    }
                } else {
                    progressInfo.log("Skipped " + roi_);
                    continue;
                }
            }

            List<JIPipeTextAnnotation> textAnnotationList = new ArrayList<>();
            List<JIPipeDataAnnotation> dataAnnotationList = new ArrayList<>();

            roiNameAnnotation.addAnnotationIfEnabled(textAnnotationList, StringUtils.nullToEmpty(roi.getName()));
            roiIndexAnnotation.addAnnotationIfEnabled(textAnnotationList, String.valueOf(i));

            if (roiDataAnnotation.isEnabled()) {
                dataAnnotationList.add(new JIPipeDataAnnotation(roiDataAnnotation.getContent(), new ROIListData(Collections.singletonList(roi))));
            }

            iterationStep.addOutputData(getFirstOutputSlot(),
                    result,
                    textAnnotationList,
                    JIPipeTextAnnotationMergeMode.Merge,
                    dataAnnotationList,
                    JIPipeDataAnnotationMergeMode.Merge,
                    progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Rectangle processing", description = "Determines how rectangular ROIs are handled. " +
            "RowAverage calculates the average intensity over all y values in the rectangle at each x value. " +
            "ColumnAverage calculates the average intensity over all x values in the rectangle at each y value. " +
            "Auto selects RowAverage or ColumnAverage based on the aspect ratio of the rectangle: RowAverage is chosen of the height of the rectangle is greater than its width.")
    @JIPipeParameter("rectangle-mode")
    public RectangleMode getRectangleMode() {
        return rectangleMode;
    }

    @JIPipeParameter("rectangle-mode")
    public void setRectangleMode(RectangleMode rectangleMode) {
        this.rectangleMode = rectangleMode;
    }

    @SetJIPipeDocumentation(name = "Annotate with ROI name", description = "If enabled, the resulting tables are annotated with the ROI name")
    @JIPipeParameter("roi-name-annotation")
    public OptionalTextAnnotationNameParameter getRoiNameAnnotation() {
        return roiNameAnnotation;
    }

    @JIPipeParameter("roi-name-annotation")
    public void setRoiNameAnnotation(OptionalTextAnnotationNameParameter roiNameAnnotation) {
        this.roiNameAnnotation = roiNameAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with ROI index", description = "If enabled, the resulting tables are annotated with the ROI index")
    @JIPipeParameter("roi-index-annotation")
    public OptionalTextAnnotationNameParameter getRoiIndexAnnotation() {
        return roiIndexAnnotation;
    }

    @JIPipeParameter("roi-index-annotation")
    public void setRoiIndexAnnotation(OptionalTextAnnotationNameParameter roiIndexAnnotation) {
        this.roiIndexAnnotation = roiIndexAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with ROI", description = "If enabled, the resulting tables are annotated with the ROI as data annotation")
    @JIPipeParameter("roi-data-annotation")
    public OptionalDataAnnotationNameParameter getRoiDataAnnotation() {
        return roiDataAnnotation;
    }

    @JIPipeParameter("roi-data-annotation")
    public void setRoiDataAnnotation(OptionalDataAnnotationNameParameter roiDataAnnotation) {
        this.roiDataAnnotation = roiDataAnnotation;
    }

    @SetJIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }

    private ResultsTableData getStraightLineProfile(Roi roi, Calibration cal, ImageProcessor ip) {
        double xInc = cal != null ? cal.pixelWidth : 1;
        ip.setInterpolate(PlotWindow.interpolate);
        Line line = (Line) roi;
        double[] values = line.getPixels();
        if (values == null) return null;
        if (cal != null && cal.pixelWidth != cal.pixelHeight) {
            FloatPolygon p = line.getFloatPoints();
            double dx = p.xpoints[1] - p.xpoints[0];
            double dy = p.ypoints[1] - p.ypoints[0];
            double pixelLength = Math.sqrt(dx * dx + dy * dy);
            dx = cal.pixelWidth * dx;
            dy = cal.pixelHeight * dy;
            double calibratedLength = Math.sqrt(dx * dx + dy * dy);
            xInc = calibratedLength / pixelLength;
        }

        // Calculate x value
        double[] lengthValues = new double[values.length];
        for (int i = 0; i < lengthValues.length; i++) {
            lengthValues[i] = i * xInc;
        }

        return new ResultsTableData(Arrays.asList(new DoubleArrayTableColumn(lengthValues, "Distance"),
                new DoubleArrayTableColumn(values, "Intensity")));
    }

    private ResultsTableData getRowAverageProfile(Rectangle rect, Calibration cal, ImageProcessor ip) {
        double xInc = cal != null ? cal.pixelWidth : 1;
        double[] values = new double[rect.height];
        int[] counts = new int[rect.height];
        double[] aLine;
        ip.setInterpolate(false);
        for (int x = rect.x; x < rect.x + rect.width; x++) {
            aLine = ip.getLine(x, rect.y, x, rect.y + rect.height - 1);
            for (int i = 0; i < rect.height; i++) {
                if (!Double.isNaN(aLine[i])) {
                    values[i] += aLine[i];
                    counts[i]++;
                }
            }
        }
        for (int i = 0; i < rect.height; i++)
            values[i] /= counts[i];
        if (cal != null) {
            xInc = cal.pixelHeight;
        }

        // Calculate x value
        double[] lengthValues = new double[values.length];
        for (int i = 0; i < lengthValues.length; i++) {
            lengthValues[i] = i * xInc;
        }

        return new ResultsTableData(Arrays.asList(new DoubleArrayTableColumn(lengthValues, "Distance"),
                new DoubleArrayTableColumn(values, "Intensity")));
    }

    private ResultsTableData getColumnAverageProfile(Rectangle rect, Calibration cal, ImageProcessor ip) {
        double[] values = new double[rect.width];
        double xInc = cal != null ? cal.pixelWidth : 1;
        int[] counts = new int[rect.width];
        double[] aLine;
        ip.setInterpolate(false);
        for (int y = rect.y; y < rect.y + rect.height; y++) {
            aLine = ip.getLine(rect.x, y, rect.x + rect.width - 1, y);
            for (int i = 0; i < rect.width; i++) {
                if (!Double.isNaN(aLine[i])) {
                    values[i] += aLine[i];
                    counts[i]++;
                }
            }
        }
        for (int i = 0; i < rect.width; i++) {
            values[i] /= counts[i];
        }

        // Calculate x value
        double[] lengthValues = new double[values.length];
        for (int i = 0; i < lengthValues.length; i++) {
            lengthValues[i] = i * xInc;
        }

        return new ResultsTableData(Arrays.asList(new DoubleArrayTableColumn(lengthValues, "Distance"),
                new DoubleArrayTableColumn(values, "Intensity")));
    }

    /**
     * Returns the profile for a polyline with single-pixel width.
     * If subpixel resolution is enabled (Plot options>subpixel resolution),
     * the line coordinates are interpreted as the roi line shown at high zoom level,
     * i.e., integer (x,y) is in the top left corner of pixel (x,y).
     * Thus, the coordinates of the pixel center are taken as (x+0.5, y+0.5).
     * If subpixel resolution is off, the coordinates of the pixel centers are taken
     * as integer (x,y).
     */
    private ResultsTableData getIrregularProfile(Roi roi, ImageProcessor ip, Calibration cal) {
        boolean interpolate = PlotWindow.interpolate;
        boolean calcXValues = cal != null && cal.pixelWidth != cal.pixelHeight;
        FloatPolygon p = roi.getFloatPolygon();
        int n = p.npoints;
        float[] xpoints = p.xpoints;
        float[] ypoints = p.ypoints;
        TDoubleList values = new TDoubleArrayList();
        TDoubleList lengthValues = new TDoubleArrayList();
        int n2;
        double inc = 0.01;
        double distance = 0.0, distance2 = 0.0, dx = 0.0, dy = 0.0, xinc, yinc;
        double x, y, lastx = 0.0, lasty = 0.0, x1, y1, x2 = xpoints[0], y2 = ypoints[0];
        double value;
        double coveredDistance = 0;
        for (int i = 1; i < n; i++) {
            x1 = x2;
            y1 = y2;
            x = x1;
            y = y1;
            x2 = xpoints[i];
            y2 = ypoints[i];
            dx = x2 - x1;
            dy = y2 - y1;
            distance = Math.sqrt(dx * dx + dy * dy);
            xinc = dx * inc / distance;
            yinc = dy * inc / distance;
            n2 = (int) (distance / inc);
            if (n == 2) n2++;
            do {
                dx = x - lastx;
                dy = y - lasty;
                distance2 = Math.sqrt(dx * dx + dy * dy);
                if (distance2 >= 1.0 - inc / 2.0) {
                    if (interpolate)
                        value = ip.getInterpolatedValue(x, y);
                    else
                        value = ip.getPixelValue((int) Math.round(x), (int) Math.round(y));
                    values.add(value);
                    coveredDistance += distance2;
                    lengthValues.add(coveredDistance);
                    lastx = x;
                    lasty = y;
                }
                x += xinc;
                y += yinc;
            } while (--n2 > 0);
        }

        // Calculate x value
        return new ResultsTableData(Arrays.asList(new DoubleArrayTableColumn(lengthValues.toArray(), "Distance"),
                new DoubleArrayTableColumn(values.toArray(), "Intensity")));
    }

    private ResultsTableData getWideLineProfile(ImagePlus imp, int lineWidth, Calibration cal) {
        double xInc = cal != null ? cal.pixelWidth : 1;
        Roi roi = imp.getRoi();
        if (roi == null) return null;    //roi may have changed asynchronously
        if ((roi instanceof PolygonRoi) && roi.getState() == Roi.CONSTRUCTING)
            return null;                //don't disturb roi under construction by spline fit
        roi = (Roi) roi.clone();
        ImageProcessor ip2 = (new Straightener()).straightenLine(imp, lineWidth);
        if (ip2 == null)
            return null;
        int width = ip2.getWidth();
        int height = ip2.getHeight();
        if (ip2 instanceof FloatProcessor) {
            return getColumnAverageProfile(new Rectangle(0, 0, width, height), cal, ip2);
        }
        double[] values = new double[width];
        double[] aLine;
        ip2.setInterpolate(false);
        for (int y = 0; y < height; y++) {
            aLine = ip2.getLine(0, y, width - 1, y);
            for (int i = 0; i < width; i++)
                values[i] += aLine[i];
        }
        for (int i = 0; i < width; i++)
            values[i] /= height;

        // Calculate x value
        double[] lengthValues = new double[values.length];
        for (int i = 0; i < lengthValues.length; i++) {
            lengthValues[i] = i * xInc;
        }

        return new ResultsTableData(Arrays.asList(new DoubleArrayTableColumn(lengthValues, "Distance"),
                new DoubleArrayTableColumn(values, "Intensity")));
    }

    public enum RectangleMode {
        Auto,
        RowAverage,
        ColumnAverage,
        Skip
    }
}
