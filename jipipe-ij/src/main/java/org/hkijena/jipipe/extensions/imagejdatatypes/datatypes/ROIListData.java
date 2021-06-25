/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes;

import com.google.common.collect.ImmutableList;
import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.Filler;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.LUT;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeCacheSlotDataSource;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedImagePlusDataViewerWindow;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedROIListDataViewerWindow;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.RoiOutline;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.parameters.roi.Margin;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Contains {@link Roi}
 */
@JIPipeDocumentation(name = "ROI list", description = "Collection of ROI")
@JIPipeDataStorageDocumentation("Contains one file in *.roi or *.zip format. " +
        "*.roi is a single ImageJ ROI. *.zip contains multiple ImageJ ROI. Please note that if multiple *.roi/*.zip are present, only " +
        "one will be loaded.")
public class ROIListData extends ArrayList<Roi> implements JIPipeData {

    /**
     * Creates an empty set of ROI
     */
    public ROIListData() {
    }

    /**
     * Creates a deep copy
     *
     * @param other the original
     */
    public ROIListData(List<Roi> other) {
        for (Roi roi : other) {
            Roi clone = (Roi) roi.clone();
            // Keep the image reference
            clone.setImage(roi.getImage());
            add(clone);
        }
    }

    /**
     * Initializes from a RoiManager
     *
     * @param roiManager the ROI manager
     */
    public ROIListData(RoiManager roiManager) {
        this.addAll(Arrays.asList(roiManager.getRoisAsArray()));
    }

    /**
     * Groups the ROI by their image positions
     *
     * @param perSlice   group per slice
     * @param perChannel group per channel
     * @param perFrame   group per frame
     * @return groups
     */
    public Map<ImageSliceIndex, List<Roi>> groupByPosition(boolean perSlice, boolean perChannel, boolean perFrame) {
        return this.stream().collect(Collectors.groupingBy(roi -> {
            ImageSliceIndex index = new ImageSliceIndex();
            if (perSlice)
                index.setZ(roi.getZPosition() - 1);
            if (perFrame)
                index.setT(roi.getTPosition() - 1);
            if (perChannel)
                index.setC(roi.getCPosition() - 1);
            return index;
        }));
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        // Code adapted from ImageJ RoiManager class
        if (size() == 1) {
            try {
                FileOutputStream out = new FileOutputStream(storageFilePath.resolve(name + ".roi").toFile());
                RoiEncoder re = new RoiEncoder(out);
                Roi roi = this.get(0);
                re.write(roi);
                out.flush();
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(storageFilePath.resolve(name + ".zip").toFile())));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
                RoiEncoder re = new RoiEncoder(out);
                for (int i = 0; i < this.size(); i++) {
                    String label = name + "-" + i;
                    Roi roi = this.get(i);
                    if (roi == null) continue;
                    if (!label.endsWith(".roi")) label += ".roi";
                    zos.putNextEntry(new ZipEntry(label));
                    re.write(roi);
                    out.flush();
                }
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String toString() {
        if (size() == 1) {
            return "ROI list (" + size() + " items) " + get(0);
        }
        return "ROI list (" + size() + " items)";
    }

    @Override
    public JIPipeData duplicate() {
        return new ROIListData(this);
    }

    @Override
    public Component preview(int width, int height) {
        ImagePlus mask;
        if (isEmpty()) {
            mask = IJ.createImage("empty", "8-bit", width, height, 1);
        } else {
            ROIListData copy = new ROIListData(this);
            copy.flatten();
            copy.crop(true, false, false, false);
            mask = copy.toMask(new Margin(), false, true, 1);
            mask.setLut(LUT.createLutFromColor(Color.RED));
        }
        return new ImagePlusData(mask).preview(width, height);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        if (source instanceof JIPipeCacheSlotDataSource) {
            CachedROIListDataViewerWindow window = new CachedROIListDataViewerWindow(workbench, (JIPipeCacheSlotDataSource) source, displayName);
            window.setVisible(true);
        } else {
            ImagePlus mask;
            if (isEmpty()) {
                mask = IJ.createImage("empty", "8-bit", 128, 128, 1);
            } else {
                ROIListData copy = new ROIListData(this);
                copy.flatten();
                copy.crop(true, false, false, false);
                mask = copy.toMask(new Margin(), false, true, 1);
                mask.setLut(LUT.createLutFromColor(Color.RED));
            }
            mask.show();
        }
    }

    /**
     * Groups the ROI by their reference image
     *
     * @return map of reference image to ROI
     */
    public Map<Optional<ImagePlus>, ROIListData> groupByReferenceImage() {
        Map<Optional<ImagePlus>, ROIListData> byImage = new HashMap<>();
        for (Roi roi : this) {
            Optional<ImagePlus> key = Optional.ofNullable(roi.getImage());
            ROIListData rois = byImage.getOrDefault(key, null);
            if (rois == null) {
                rois = new ROIListData();
                byImage.put(key, rois);
            }
            rois.add(roi);
        }
        return byImage;
    }

    /**
     * Moves the ROI, so the bounding rectangle is at 0,0
     */
    public void crop(boolean cropXY, boolean cropZ, boolean cropC, boolean cropT) {
        Rectangle bounds = getBounds();
        int sz = Integer.MAX_VALUE;
        int sc = Integer.MAX_VALUE;
        int st = Integer.MAX_VALUE;
        for (Roi roi : this) {
            int z = roi.getZPosition();
            int c = roi.getCPosition();
            int t = roi.getTPosition();
            if (roi.getZPosition() > 0)
                sz = Math.min(sz, z);
            if (roi.getZPosition() > 0)
                sc = Math.min(sc, c);
            if (roi.getZPosition() > 0)
                st = Math.min(st, t);
        }
        if (!cropZ)
            sz = Integer.MAX_VALUE;
        if (!cropC)
            sc = Integer.MAX_VALUE;
        if (!cropT)
            st = Integer.MAX_VALUE;
        for (Roi roi : this) {
            if (cropXY)
                roi.setLocation(roi.getXBase() - bounds.x, roi.getYBase() - bounds.y);
            int z = roi.getZPosition();
            int c = roi.getCPosition();
            int t = roi.getTPosition();
            if (z > 0 && sz != Integer.MAX_VALUE)
                z = z - sz;
            if (c > 0 && sc != Integer.MAX_VALUE)
                c = c - sc;
            if (t > 0 && st != Integer.MAX_VALUE)
                t = t - st;
            roi.setPosition(z, c, t);
        }
    }

    /**
     * Makes all ROI visible on all stacks
     */
    public void flatten() {
        for (Roi roi : this) {
            roi.setPosition(0, 0, 0);
        }
    }

    /**
     * Creates a 2D 8-bit image that covers the region of all ROI
     *
     * @return the image
     */
    public ImagePlus createDummyImage() {
        Rectangle bounds = getBounds();
        int width = Math.max(0, bounds.x) + bounds.width;
        int height = Math.max(0, bounds.y) + bounds.height;
        return IJ.createImage("empty", "8-bit", width, height, 1);
    }

    /**
     * Generates a mask image from pure ROI data.
     * The ROI's reference images are ignored.
     *
     * @param imageArea         modifications for the image area
     * @param drawOutline       whether to draw an outline
     * @param drawFilledOutline whether to fill the area
     * @param lineThickness     line thickness for drawing
     * @return the image
     */
    public ImagePlus toMask(Margin imageArea, boolean drawOutline, boolean drawFilledOutline, int lineThickness) {
        // Find the bounds and future stack position
        Rectangle bounds = imageArea.apply(this.getBounds());
        int sx = bounds.width + bounds.x;
        int sy = bounds.height + bounds.y;
        int sz = 1;
        int sc = 1;
        int st = 1;
        for (Roi roi : this) {
            int z = roi.getZPosition();
            int c = roi.getCPosition();
            int t = roi.getTPosition();
            sz = Math.max(sz, z);
            sc = Math.max(sc, c);
            st = Math.max(st, t);
        }

        ImagePlus result = IJ.createImage("ROIs", "8-bit", sx, sy, sc, sz, st);
        drawMask(drawOutline, drawFilledOutline, lineThickness, result);
        return result;
    }

    /**
     * Generates a mask image from pure ROI data.
     * The ROI's reference images are ignored.
     *
     * @param width             the image width
     * @param height            the image height
     * @param drawOutline       whether to draw an outline
     * @param drawFilledOutline whether to fill the area
     * @param lineThickness     line thickness for drawing
     * @return the image
     */
    public ImagePlus toMask(int width, int height, boolean drawOutline, boolean drawFilledOutline, int lineThickness) {
        // Find the bounds and future stack position
        int sz = 1;
        int sc = 1;
        int st = 1;
        for (Roi roi : this) {
            int z = roi.getZPosition();
            int c = roi.getCPosition();
            int t = roi.getTPosition();
            sz = Math.max(sz, z);
            sc = Math.max(sc, c);
            st = Math.max(st, t);
        }

        ImagePlus result = IJ.createImage("ROIs", "8-bit", width, height, sc, sz, st);
        drawMask(drawOutline, drawFilledOutline, lineThickness, result);
        return result;
    }

    /**
     * Generates a mask image from pure ROI data.
     * The ROI's reference images are ignored.
     *
     * @param width             the image width
     * @param height            the image height
     * @param drawOutline       whether to draw an outline
     * @param drawFilledOutline whether to fill the area
     * @param lineThickness     line thickness for drawing
     * @param sliceIndex        zero-based slice index
     * @return the image
     */
    public ImagePlus getMaskForSlice(int width, int height, boolean drawOutline, boolean drawFilledOutline, int lineThickness, ImageSliceIndex sliceIndex) {
        // Find the bounds and future stack position
        int sz = 1;
        int sc = 1;
        int st = 1;
        for (Roi roi : this) {
            int z = roi.getZPosition();
            int c = roi.getCPosition();
            int t = roi.getTPosition();
            sz = Math.max(sz, z);
            sc = Math.max(sc, c);
            st = Math.max(st, t);
        }

        ImagePlus result = IJ.createImage("ROIs", "8-bit", width, height, 1, 1, 1);
        drawMaskForSliceIndex(drawOutline, drawFilledOutline, lineThickness, result, sliceIndex);
        return result;
    }

    public void draw(ImageProcessor processor, ImageSliceIndex currentIndex, boolean ignoreZ, boolean ignoreC, boolean ignoreT, boolean drawOutline, boolean fillOutline, boolean drawLabel, int defaultLineThickness, Color defaultFillColor, Color defaultLineColor, Collection<Roi> highlighted) {
        ImagePlus tmp = new ImagePlus("tmp", processor);
        final int z = currentIndex.getZ();
        final int c = currentIndex.getC();
        final int t = currentIndex.getT();
        final Filler roiFiller = new Filler();
        for (int i = 0; i < size(); i++) {
            Roi roi = get(i);
            int rz = roi.getZPosition();
            int rc = roi.getCPosition();
            int rt = roi.getTPosition();
            if (!ignoreZ && rz != 0 && rz != (z + 1))
                continue;
            if (!ignoreC && rc != 0 && rc != (c + 1))
                continue;
            if (!ignoreT && rt != 0 && rt != (t + 1))
                continue;
            if (fillOutline) {
                Color color = roi.getFillColor() != null ? roi.getFillColor() : defaultFillColor;
                if (!highlighted.isEmpty()) {
                    if (!highlighted.contains(roi)) {
                        color = ColorUtils.scaleHSV(color, 0.8f, 1, 0.5f);
                    } else {
                        continue;
                    }
                }
                processor.setColor(color);
                processor.fill(roi);
            }
            if (drawOutline) {
                Color color = roi.getStrokeColor() != null ? roi.getStrokeColor() : defaultLineColor;
                int width = (int) roi.getStrokeWidth() <= 0 ? defaultLineThickness : (int) roi.getStrokeWidth();
                if (!highlighted.isEmpty()) {
                    if (!highlighted.contains(roi)) {
                        color = ColorUtils.scaleHSV(color, 0.8f, 1, 0.5f);
                    } else {
                        continue;
                    }
                }
                processor.setLineWidth(width);
                processor.setColor(color);
                roi.drawPixels(processor);
            }
            if (drawLabel) {
                Point centroid = getCentroid(roi);
                roiFiller.drawLabel(tmp, processor, i, new Rectangle(centroid.x, centroid.y, 0, 0));
            }
        }
        for (Roi roi : highlighted) {
            int i = indexOf(roi);
            int rz = roi.getZPosition();
            int rc = roi.getCPosition();
            int rt = roi.getTPosition();
            if (!ignoreZ && rz != 0 && rz != (z + 1))
                continue;
            if (!ignoreC && rc != 0 && rc != (c + 1))
                continue;
            if (!ignoreT && rt != 0 && rt != (t + 1))
                continue;
            if (fillOutline) {
                Color color = roi.getFillColor() != null ? roi.getFillColor() : defaultFillColor;
                processor.setColor(color);
                processor.fill(roi);
            }
            if (drawOutline) {
                Color color = roi.getStrokeColor() != null ? roi.getStrokeColor() : defaultLineColor;
                int width = (int) roi.getStrokeWidth() <= 0 ? defaultLineThickness : (int) roi.getStrokeWidth();
                processor.setLineWidth(width);
                processor.setColor(color);
                roi.drawPixels(processor);
            }
            if (drawLabel) {
                Point centroid = getCentroid(roi);
                roiFiller.drawLabel(tmp, processor, i, new Rectangle(centroid.x, centroid.y, 0, 0));
            }
        }

    }

    /**
     * Draws the ROI over an existing mask image
     *
     * @param drawOutline       whether to draw an outline
     * @param drawFilledOutline whether to fill the area
     * @param lineThickness     line thickness for drawing
     * @param result            the target image
     * @param sliceIndex        zero-based slice index
     */
    public void drawMaskForSliceIndex(boolean drawOutline, boolean drawFilledOutline, int lineThickness, ImagePlus result, ImageSliceIndex sliceIndex) {
        ImageProcessor processor = result.getProcessor();
        processor.setLineWidth(lineThickness);
        processor.setColor(255);

        int z = sliceIndex.getZ();
        int c = sliceIndex.getC();
        int t = sliceIndex.getT();

        for (Roi roi : this) {
            int rz = roi.getZPosition();
            int rc = roi.getCPosition();
            int rt = roi.getTPosition();
            if (rz != 0 && rz != (z + 1))
                continue;
            if (rc != 0 && rc != (c + 1))
                continue;
            if (rt != 0 && rt != (t + 1))
                continue;
            if (drawFilledOutline)
                processor.fill(roi);
            if (drawOutline)
                roi.drawPixels(processor);
        }
    }

    /**
     * Draws the ROI over an existing mask image
     *
     * @param drawOutline       whether to draw an outline
     * @param drawFilledOutline whether to fill the area
     * @param lineThickness     line thickness for drawing
     * @param result            the target image
     */
    public void drawMask(boolean drawOutline, boolean drawFilledOutline, int lineThickness, ImagePlus result) {
        for (int z = 0; z < result.getNSlices(); z++) {
            for (int c = 0; c < result.getNChannels(); c++) {
                for (int t = 0; t < result.getNFrames(); t++) {
                    int stackIndex = result.getStackIndex(c + 1, z + 1, t + 1);
                    ImageProcessor processor = result.getStack().getProcessor(stackIndex);
                    processor.setLineWidth(lineThickness);
                    processor.setColor(255);

                    for (Roi roi : this) {
                        int rz = roi.getZPosition();
                        int rc = roi.getCPosition();
                        int rt = roi.getTPosition();
                        if (rz != 0 && rz != (z + 1))
                            continue;
                        if (rc != 0 && rc != (c + 1))
                            continue;
                        if (rt != 0 && rt != (t + 1))
                            continue;
                        if (drawFilledOutline)
                            processor.fill(roi);
                        if (drawOutline)
                            roi.drawPixels(processor);
                    }
                }
            }
        }
    }

    /**
     * Adds the ROI to an existing ROI manager instance
     *
     * @param roiManager the ROI manager
     */
    public void addToRoiManager(RoiManager roiManager) {
        for (Roi roi : this) {
            roiManager.add(roi, -1);
        }
    }

    /**
     * Merges the ROI from another data into this one.
     * Creates copies of the merged {@link Roi}
     *
     * @param other the other data. The entries are copied.
     */
    public void mergeWith(ROIListData other) {
        for (Roi item : other) {
            add((Roi) item.clone());
        }
    }

    /**
     * Outlines all {@link Roi} in this list by the specified algorithm.
     * All {@link Roi} are replaced by their outline.
     *
     * @param outline the method
     */
    public void outline(RoiOutline outline) {
        ImmutableList<Roi> input = ImmutableList.copyOf(this);
        clear();
        for (Roi roi : input) {
            Roi outlined;
            switch (outline) {
                case Polygon:
                    outlined = new PolygonRoi(roi.getFloatPolygon(), Roi.POLYGON);
                    break;
                case ClosedPolygon:
                    outlined = new PolygonRoi(roi.getFloatPolygon("close"), Roi.POLYGON);
                    break;
                case ConvexHull:
                    outlined = new PolygonRoi(roi.getConvexHull(), Roi.POLYGON);
                    break;
                case BoundingRectangle: {
                    Rectangle b = roi.getBounds();
                    outlined = new PolygonRoi(new int[]{b.x, b.x + b.width, b.x + b.width, b.x},
                            new int[]{b.y, b.y, b.y + b.height, b.y + b.height},
                            4,
                            Roi.POLYGON);
                }
                break;
                default:
                    throw new UnsupportedOperationException("Unsupported: " + outline);
            }

            // Restore information
            outlined.copyAttributes(roi);
            outlined.setPosition(roi.getPosition());
            outlined.setPosition(roi.getCPosition(), roi.getZPosition(), roi.getTPosition());

            // Add to list
            add(outlined);
        }
    }

    /**
     * Splits all {@link ij.gui.ShapeRoi} that consist of multiple sub-Roi into their individual sub-Roi.
     * The original {@link ShapeRoi} is removed.
     */
    public void splitAll() {
        for (Roi target : ImmutableList.copyOf(this)) {
            if (target instanceof ShapeRoi) {
                ShapeRoi shapeRoi = (ShapeRoi) target;
                if (shapeRoi.getRois().length > 1) {
                    remove(shapeRoi);
                    this.addAll(Arrays.asList(shapeRoi.getRois()));
                }
            }
        }
    }

    /**
     * Returns the bounds of area described by the {@link Roi}
     *
     * @return the bounds. if no {@link Roi} are present, a Rectangle with zero size is returned
     */
    public Rectangle getBounds() {
        if (isEmpty())
            return new Rectangle(0, 0, 0, 0);
        double x0 = Double.POSITIVE_INFINITY;
        double x1 = Double.NEGATIVE_INFINITY;
        double y0 = Double.POSITIVE_INFINITY;
        double y1 = Double.NEGATIVE_INFINITY;
        for (Roi roi : this) {
            double x = roi.getXBase();
            double y = roi.getYBase();
            double w = roi.getFloatWidth();
            double h = roi.getFloatHeight();
            x0 = Math.min(x, x0);
            x1 = Math.max(x + w, x1);
            y0 = Math.min(y, y0);
            y1 = Math.max(y + h, y1);
        }
        return new Rectangle((int) x0, (int) y0, (int) (x1 - x0), (int) (y1 - y0));
    }

    /**
     * Applies a logical OR operation to all {@link Roi} in this list. This means that all {@link Roi} are combined into a single {@link Roi}
     */
    public void logicalOr() {
        if (containsOnlyRoisOfType(Roi.POINT)) {
            FloatPolygon fp = new FloatPolygon();
            for (Roi roi : this) {
                FloatPolygon fpi = roi.getFloatPolygon();
                for (int i = 0; i < fpi.npoints; i++)
                    fp.addPoint(fpi.xpoints[i], fpi.ypoints[i]);
            }
            clear();
            add(new PointRoi(fp));
        } else {
            ShapeRoi s1 = null, s2 = null;
            for (Roi roi : this) {
                if (!roi.isArea() && roi.getType() != Roi.POINT)
                    roi = Roi.convertLineToArea(roi);
                if (s1 == null) {
                    if (roi instanceof ShapeRoi)
                        s1 = (ShapeRoi) roi;
                    else
                        s1 = new ShapeRoi(roi);
                    if (s1 == null) return;
                } else {
                    if (roi instanceof ShapeRoi)
                        s2 = (ShapeRoi) roi;
                    else
                        s2 = new ShapeRoi(roi);
                    if (s2 == null) continue;
                    s1.or(s2);
                }
            }
            if (s1 != null) {
                s1.trySimplify();
                clear();
                add(s1);
            }
        }
    }

    /**
     * Applies a logical AND operation to all {@link Roi} in this list. This means that the output is one {@link Roi} containing the intersection.
     */
    public void logicalAnd() {
        int nPointRois = countRoisOfType(Roi.POINT);
        ShapeRoi s1 = null;
        PointRoi pointRoi = null;
        for (Roi roi : this) {
            if (roi == null)
                continue;
            if (s1 == null) {
                if (nPointRois == 1 && roi.getType() == Roi.POINT) {
                    pointRoi = (PointRoi) roi;
                    continue;  //PointRoi will be handled at the end
                }
                if (roi instanceof ShapeRoi)
                    s1 = (ShapeRoi) roi.clone();
                else
                    s1 = new ShapeRoi(roi);
                if (s1 == null) continue;
            } else {
                if (nPointRois == 1 && roi.getType() == Roi.POINT) {
                    pointRoi = (PointRoi) roi;
                    continue;  //PointRoi will be handled at the end
                }
                ShapeRoi s2 = null;
                if (roi instanceof ShapeRoi)
                    s2 = (ShapeRoi) roi.clone();
                else
                    s2 = new ShapeRoi(roi);
                if (s2 == null) continue;
                s1.and(s2);
            }
        }
        if (s1 == null) return;
        if (pointRoi != null) {
            clear();
            add(pointRoi.containedPoints(s1));
        } else {
            s1.trySimplify();
            clear();
            add(s1);
        }
    }

    /**
     * Applies a logical XOR operation on the list of {@link Roi}.
     */
    public void logicalXor() {
        ShapeRoi s1 = null, s2 = null;
        for (Roi roi : this) {
            if (roi == null)
                continue;
            if (s1 == null) {
                if (roi instanceof ShapeRoi)
                    s1 = (ShapeRoi) roi.clone();
                else
                    s1 = new ShapeRoi(roi);
                if (s1 == null) return;
            } else {
                if (roi instanceof ShapeRoi)
                    s2 = (ShapeRoi) roi.clone();
                else
                    s2 = new ShapeRoi(roi);
                if (s2 == null) continue;
                s1.xor(s2);
            }
        }
        if (s1 != null) {
            s1.trySimplify();
            clear();
            add(s1);
        }
    }

    /**
     * Returns if this ROI list only contains ROI of given type
     *
     * @param type the ROI type. Can be RECTANGLE=0, OVAL=1, POLYGON=2, FREEROI=3, TRACED_ROI=4, LINE=5, POLYLINE=6, FREELINE=7, ANGLE=8, COMPOSITE=9, or POINT=10
     * @return if this ROI list only contains ROI of given type
     */
    public boolean containsOnlyRoisOfType(int type) {
        return size() == countRoisOfType(type);
    }

    /**
     * Counts all ROIs of given type
     *
     * @param type the ROI type. Can be RECTANGLE=0, OVAL=1, POLYGON=2, FREEROI=3, TRACED_ROI=4, LINE=5, POLYLINE=6, FREELINE=7, ANGLE=8, COMPOSITE=9, or POINT=10
     * @return number of ROI with type Roi.POINT
     */
    public int countRoisOfType(int type) {
        int nPointRois = 0;
        for (Roi roi : this)
            if (roi.getType() == type)
                nPointRois++;
        return nPointRois;
    }

    /**
     * Generates ROI statistics
     *
     * @param imp            the reference image. Can be null to measure on a black image
     * @param measurements   which measurements to extract
     * @param addNameToTable if true, add the ROI's name to the table
     * @return the measurements
     */
    public ResultsTableData measure(ImagePlus imp, ImageStatisticsSetParameter measurements, boolean addNameToTable) {
        ResultsTableData result = new ResultsTableData(new ResultsTable());
        if (imp != null) {
            measurements.updateAnalyzer();
            Analyzer aSys = new Analyzer(imp); // System Analyzer
            ResultsTable rtSys = Analyzer.getResultsTable();
            rtSys.reset();
            for (int z = 0; z < imp.getNSlices(); z++) {
                for (int c = 0; c < imp.getNChannels(); c++) {
                    for (int t = 0; t < imp.getNFrames(); t++) {
                        imp.setSliceWithoutUpdate(imp.getStackIndex(c + 1, z + 1, t + 1));
                        for (Roi roi : this) {
                            if ((roi.getZPosition() == 0 || roi.getZPosition() == z + 1) &&
                                    (roi.getCPosition() == 0 || roi.getCPosition() == c + 1) &&
                                    (roi.getTPosition() == 0 || roi.getTPosition() == t + 1)) {
                                imp.setRoi(roi);
                                rtSys.reset();
                                aSys.measure();
                                ResultsTableData forRoi = new ResultsTableData(rtSys);
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
                                    int columnName = result.getOrCreateColumnIndex("Name", true);
                                    for (int row = 0; row < result.getRowCount(); row++) {
                                        result.setValueAt(roi.getName(), row, columnName);
                                    }
                                }
                                result.addRows(forRoi);
                            }
                        }
                    }
                }
            }
        } else {
            imp = createDummyImage();
            measurements.updateAnalyzer();
            Analyzer aSys = new Analyzer(imp); // System Analyzer
            ResultsTable rtSys = Analyzer.getResultsTable();
            rtSys.reset();
            for (Roi roi : this) {
                imp.setRoi(roi);
                rtSys.reset();
                aSys.measure();
                ResultsTableData forRoi = new ResultsTableData(rtSys);
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
                    int columnName = result.getOrCreateColumnIndex("Name", true);
                    for (int row = 0; row < result.getRowCount(); row++) {
                        result.setValueAt(roi.getName(), row, columnName);
                    }
                }
                result.addRows(forRoi);
            }
        }

//        int currentSlice = imp.getCurrentSlice();
//        for (int slice = 1; slice <= nSlices; slice++) {
//            int sliceUse = slice;
//            if (nSlices == 1) sliceUse = currentSlice;
//            imp.setSliceWithoutUpdate(sliceUse);
//            for (Roi roi0 : this) {
//                imp.setRoi(roi0);
//                aSys.measure();
//                ResultsTableData forRoi = new ResultsTableData(rtSys);
//                result.mergeWith(forRoi);
//            }
//        }
        return result;
    }

    /**
     * The overall channel position of the ROIs.
     * If the position is not exactly defined, this function returns 0
     * Returns 0 if no ROIs are in the list
     *
     * @return the overall channel position. 1-based index. 0 equals that the ROIs are at multiple locations
     */
    public int getChannelPosition() {
        int result = -1;
        for (Roi roi : this) {
            if (result == -1)
                result = roi.getCPosition();
            else if (result != 0 && result != roi.getCPosition()) {
                result = 0;
                break;
            }
        }
        return Math.max(0, result);
    }

    /**
     * The overall stack position of the ROIs.
     * If the position is not exactly defined, this function returns 0
     * Returns 0 if no ROIs are in the list
     *
     * @return the overall stack position. 1-based index. 0 equals that the ROIs are at multiple locations
     */
    public int getStackPosition() {
        int result = -1;
        for (Roi roi : this) {
            if (result == -1)
                result = roi.getZPosition();
            else if (result != 0 && result != roi.getZPosition()) {
                result = 0;
                break;
            }
        }
        return Math.max(0, result);
    }

    /**
     * The overall frame position of the ROIs.
     * If the position is not exactly defined, this function returns 0
     * Returns 0 if no ROIs are in the list
     *
     * @return the overall frame position. 1-based index. 0 equals that the ROIs are at multiple locations
     */
    public int getFramePosition() {
        int result = -1;
        for (Roi roi : this) {
            if (result == -1)
                result = roi.getTPosition();
            else if (result != 0 && result != roi.getTPosition()) {
                result = 0;
                break;
            }
        }
        return Math.max(0, result);
    }

    /**
     * Creates a 2D 8-bit black image that covers the region of all provided ROI
     *
     * @param rois the rois
     * @return the image. 1x1 pixel if no ROI or empty roi are provided
     */
    public static ImagePlus createDummyImageFor(Collection<ROIListData> rois) {
        int width = 1;
        int height = 1;
        for (ROIListData data : rois) {
            Rectangle bounds = data.getBounds();
            int w = Math.max(0, bounds.x) + bounds.width;
            int h = Math.max(0, bounds.y) + bounds.height;
            width = Math.max(w, width);
            height = Math.max(h, height);
        }
        return IJ.createImage("empty", "8-bit", width, height, 1);
    }

    /**
     * Loads {@link Roi} from a path that contains a zip/roi file
     *
     * @param storageFilePath path that contains a zip/roi file
     */
    public static ROIListData importFrom(Path storageFilePath) {
        ROIListData result = new ROIListData();
        Path zipFile = PathUtils.findFileByExtensionIn(storageFilePath, ".zip");
        Path roiFile = PathUtils.findFileByExtensionIn(storageFilePath, ".roi");
        if (zipFile != null) {
            result.addAll(loadRoiListFromFile(zipFile));
        } else if (roiFile != null) {
            result.addAll(loadRoiListFromFile(roiFile));
        } else {
            throw new RuntimeException(new FileNotFoundException("Could not find a .roi or .zip file in " + storageFilePath));
        }
        return result;
    }

    /**
     * Loads a set of ROI from a zip file
     *
     * @param fileName the zip file
     * @return the Roi list
     */
    public static List<Roi> loadRoiListFromFile(Path fileName) {
        // Code adapted from ImageJ RoiManager
        List<Roi> result = new ArrayList<>();

        if (fileName.toString().toLowerCase().endsWith(".roi")) {
            try {
                Roi roi = new RoiDecoder(fileName.toString()).getRoi();
                if (roi != null)
                    result.add(roi);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            ZipInputStream in = null;
            ByteArrayOutputStream out = null;
            int nRois = 0;
            try {
                in = new ZipInputStream(new FileInputStream(fileName.toFile()));
                byte[] buf = new byte[1024];
                int len;
                ZipEntry entry = in.getNextEntry();
                while (entry != null) {
                    String name = entry.getName();
                    if (name.endsWith(".roi")) {
                        out = new ByteArrayOutputStream();
                        while ((len = in.read(buf)) > 0)
                            out.write(buf, 0, len);
                        out.close();
                        byte[] bytes = out.toByteArray();
                        RoiDecoder rd = new RoiDecoder(bytes, name);
                        Roi roi = rd.getRoi();
                        if (roi != null) {
                            name = name.substring(0, name.length() - 4);
                            result.add(roi);
                            nRois++;
                        }
                    }
                    entry = in.getNextEntry();
                }
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                if (out != null)
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
            }
        }

        return result;
    }

    /**
     * Gets the centroid of a ROI
     *
     * @param roi the roi
     * @return the centroid
     */
    public static Point getCentroid(Roi roi) {
        return new Point((int) roi.getContourCentroid()[0], (int) roi.getContourCentroid()[1]);
    }

    /**
     * Returns true if the ROI is visible at given slice index
     *
     * @param roi      the roi
     * @param location slice index, zero-based
     * @param ignoreZ  ignore Z constraint
     * @param ignoreC  ignore C constraint
     * @param ignoreT  ignore T constraint
     * @return if the ROI is visible
     */
    public static boolean isVisibleIn(Roi roi, ImageSliceIndex location, boolean ignoreZ, boolean ignoreC, boolean ignoreT) {
        if (!ignoreZ && roi.getZPosition() > 0 && roi.getZPosition() != (location.getZ() + 1))
            return false;
        if (!ignoreC && roi.getCPosition() > 0 && roi.getCPosition() != (location.getC() + 1))
            return false;
        if (!ignoreT && roi.getTPosition() > 0 && roi.getTPosition() != (location.getT() + 1))
            return false;
        return true;
    }
}
