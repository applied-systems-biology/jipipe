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
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.LUT;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.RoiOutline;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.SliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.parameters.roi.Margin;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
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
public class ROIListData extends ArrayList<Roi> implements JIPipeData {

    /**
     * Creates an empty set of ROI
     */
    public ROIListData() {
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
    public Map<SliceIndex, List<Roi>> groupByPosition(boolean perSlice, boolean perChannel, boolean perFrame) {
        return this.stream().collect(Collectors.groupingBy(roi -> {
            SliceIndex index = new SliceIndex();
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
    public void saveTo(Path storageFilePath, String name, boolean forceName) {
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
        return "ROI list (" + size() + " items)";
    }

    @Override
    public JIPipeData duplicate() {
        return new ROIListData(this);
    }

    @Override
    public Component preview(int width, int height) {
        ROIListData copy = new ROIListData(this);
        copy.flatten();
        copy.crop(true, false, false, false);
        ImagePlus mask = copy.toMask(new Margin(), false, true, 1);
        mask.setLut(LUT.createLutFromColor(Color.RED));
        return new ImagePlusData(mask).preview(width, height);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        Map<Optional<ImagePlus>, ROIListData> byImage = groupByReferenceImage();

        RoiManager roiManager = null;
        if (roiManager == null) {
            if (Macro.getOptions() != null && Interpreter.isBatchMode())
                roiManager = Interpreter.getBatchModeRoiManager();
            if (roiManager == null) {
                Frame frame = WindowManager.getFrame("ROI Manager");
                if (frame == null)
                    IJ.run("ROI Manager...");
                frame = WindowManager.getFrame("ROI Manager");
                if (frame == null || !(frame instanceof RoiManager)) {
                    return;
                }
                roiManager = (RoiManager) frame;
            }
        }

        ImagePlus fallbackImage = WindowManager.getCurrentImage();
        Margin margin = new Margin();
        margin.getWidth().setUseExactValue(false);
        margin.getHeight().setUseExactValue(false);

        for (Map.Entry<Optional<ImagePlus>, ROIListData> entry : byImage.entrySet()) {
            if (!entry.getKey().isPresent()) {
                if (fallbackImage == null) {
                    fallbackImage = entry.getValue().toMask(margin, false, true, 1);
                    fallbackImage.setTitle("Auto-generated based on ROI");
                    fallbackImage.show();
                }
                for (Roi roi : entry.getValue()) {
                    roiManager.add(fallbackImage, (Roi) roi.clone(), -1);
                }
            } else {
                ImagePlus target = entry.getKey().get().duplicate();
                target.setTitle(entry.getKey().get().getTitle());
                target.show();
                for (Roi roi : entry.getValue()) {
                    roiManager.add(target, (Roi) roi.clone(), -1);
                }
            }
        }

        roiManager.runCommand("show all with labels");
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
     * @param imp          the reference image
     * @param measurements which measurements to extract
     * @return the measurements
     */
    public ResultsTableData measure(ImagePlus imp, ImageStatisticsSetParameter measurements) {
        measurements.updateAnalyzer();
        Analyzer aSys = new Analyzer(imp); // System Analyzer
        ResultsTable rtSys = Analyzer.getResultsTable();
        ResultsTableData result = new ResultsTableData(new ResultsTable());
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
                            aSys.measure();
                            ResultsTableData forRoi = new ResultsTableData(rtSys);
                            result.mergeWith(forRoi);
                        }
                    }
                }
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
}
