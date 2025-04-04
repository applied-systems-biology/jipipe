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

package org.hkijena.jipipe.plugins.imagejdatatypes.datatypes;

import com.google.common.collect.ImmutableList;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.RoiRotator;
import ij.plugin.RoiScaler;
import ij.plugin.filter.Filler;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeCommonData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.*;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.BitDepth;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.CustomAnalyzer;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageMeasurementUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.plugins.napari.NapariOverlay;
import org.hkijena.jipipe.plugins.parameters.library.roi.Margin;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Contains {@link Roi}
 */
@SetJIPipeDocumentation(name = "ImageJ 2D ROI list", description = "Collection of ROI")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one file in *.roi or *.zip format. " +
        "*.roi is a single ImageJ ROI. *.zip contains multiple ImageJ ROI. Please note that if multiple *.roi/*.zip are present, only " +
        "one will be loaded.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/roi-list-data.schema.json")
@LabelAsJIPipeCommonData
public class ROI2DListData extends ArrayList<Roi> implements JIPipeData, NapariOverlay {

    /**
     * Creates an empty set of ROI
     */
    public ROI2DListData() {
    }

    /**
     * Creates a deep copy
     *
     * @param other the original
     */
    public ROI2DListData(List<Roi> other) {
        for (Roi roi : other) {
            String properties = roi.getProperties();
            Roi clone = (Roi) roi.clone();
            if (properties != null) {
                // We have to force the props variable to null, because Roi does not make a deep copy of it
                try {
                    Field field = Roi.class.getDeclaredField("props");
                    field.setAccessible(true);
                    field.set(clone, null);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }
            // Keep the image reference
            clone.setImage(roi.getImage());
            // Roi clone does not copy properties for some reason (keeps reference)
            if (properties != null)
                clone.setProperties(properties);
            add(clone);
        }
    }

    /**
     * Initializes from a RoiManager
     *
     * @param roiManager the ROI manager
     */
    public ROI2DListData(RoiManager roiManager) {
        this.addAll(Arrays.asList(roiManager.getRoisAsArray()));
    }

    /**
     * Creates a 2D 8-bit black image that covers the region of all provided ROI
     *
     * @param rois the rois
     * @return the image. 1x1 pixel if no ROI or empty roi are provided
     */
    public static ImagePlus createDummyImageFor(Collection<ROI2DListData> rois) {
        int width = 1;
        int height = 1;
        for (ROI2DListData data : rois) {
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
     * @param storage path that contains a zip/roi file
     */
    public static ROI2DListData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        ROI2DListData result = new ROI2DListData();
        Path zipFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".zip");
        Path roiFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".roi");
        if (zipFile != null) {
            result.addAll(loadRoiListFromFile(zipFile));
        } else if (roiFile != null) {
            result.addAll(loadRoiListFromFile(roiFile));
        } else {
            throw new RuntimeException(new FileNotFoundException("Could not find a .roi or .zip file in " + storage));
        }
        return result;
    }

    /**
     * Loads a set of ROI from a zip file
     *
     * @param fileName the zip file
     * @return the Roi list
     */
    public static ROI2DListData loadRoiListFromFile(Path fileName) {
        // Code adapted from ImageJ RoiManager
        ROI2DListData result = new ROI2DListData();

        if (fileName.toString().toLowerCase(Locale.ROOT).endsWith(".roi")) {
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
     * Gets the centroid of a ROI
     *
     * @param roi the roi
     * @return the centroid
     */
    public static Point2D getCentroidDouble(Roi roi) {
        return new Point2D.Double(roi.getContourCentroid()[0], roi.getContourCentroid()[1]);
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

    /**
     * Saves a single ROI to a file
     *
     * @param roi        the ROI
     * @param outputFile the file
     */
    public static void saveSingleRoi(Roi roi, Path outputFile) {
        try {
            FileOutputStream out = new FileOutputStream(outputFile.toFile());
            RoiEncoder re = new RoiEncoder(out);
            re.write(roi);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method based on <a href="https://github.com/ndefrancesco/macro-frenzy/blob/master/geometry/fitting/fitMinRectangle.ijm">fitMinRectangle</a>
     *
     * @param roi the roi
     * @return the mbr
     */
    public static Roi calculateMinimumBoundingRectangle(Roi roi) {
        try {
            roi = new PolygonRoi(roi.getConvexHull(), Roi.POLYGON);

            // Iterate a few times to converge
            for (int iteration = 0; iteration < 2; iteration++) {
                int np = roi.getFloatPolygon().npoints;
                float[] xp = roi.getFloatPolygon().xpoints;
                float[] yp = roi.getFloatPolygon().ypoints;

                double minArea = 2 * roi.getBounds().getWidth() * roi.getBounds().getHeight();
                double minFD = roi.getBounds().getWidth() + roi.getBounds().getHeight(); // FD now stands for first diameter :)
                int imin = -1;
                int i2min = -1;
                int jmin = -1;
                double min_hmin = 0;
                double min_hmax = 0;
                for (int i = 0; i < np; i++) {
                    double maxLD = 0;
                    int imax = -1;
                    int i2max = -1;
                    int jmax = -1;
                    int i2;
                    if (i < np - 1) i2 = i + 1;
                    else i2 = 0;

                    for (int j = 0; j < np; j++) {
                        double d = Math.abs(perpDist(xp[i], yp[i], xp[i2], yp[i2], xp[j], yp[j]));
                        if (maxLD < d) {
                            maxLD = d;
                            imax = i;
                            jmax = j;
                            i2max = i2;
                        }
                    }

                    double hmin = 0;
                    double hmax = 0;

                    for (int k = 0; k < np; k++) { // rotating calipers
                        double hd = parDist(xp[imax], yp[imax], xp[i2max], yp[i2max], xp[k], yp[k]);
                        hmin = Math.min(hmin, hd);
                        hmax = Math.max(hmax, hd);
                    }

                    double area = maxLD * (hmax - hmin);

                    if (minArea > area) {

                        minArea = area;
                        minFD = maxLD;
                        min_hmin = hmin;
                        min_hmax = hmax;

                        imin = imax;
                        i2min = i2max;
                        jmin = jmax;
                    }
                }

                double pd = perpDist(xp[imin], yp[imin], xp[i2min], yp[i2min], xp[jmin], yp[jmin]); // signed feret diameter
                double pairAngle = Math.atan2(yp[i2min] - yp[imin], xp[i2min] - xp[imin]);
                double minAngle = pairAngle + Math.PI / 2;

                float[] nxp = new float[4];
                float[] nyp = new float[4];

                nxp[0] = (float) (xp[imin] + Math.cos(pairAngle) * min_hmax);
                nyp[0] = (float) (yp[imin] + Math.sin(pairAngle) * min_hmax);

                nxp[1] = (float) (nxp[0] + Math.cos(minAngle) * pd);
                nyp[1] = (float) (nyp[0] + Math.sin(minAngle) * pd);

                nxp[2] = (float) (nxp[1] + Math.cos(pairAngle) * (min_hmin - min_hmax));
                nyp[2] = (float) (nyp[1] + Math.sin(pairAngle) * (min_hmin - min_hmax));

                nxp[3] = (float) (nxp[2] + Math.cos(minAngle) * -pd);
                nyp[3] = (float) (nyp[2] + Math.sin(minAngle) * -pd);

                roi = new PolygonRoi(nxp, nyp, 4, Roi.POLYGON);
            }

            return roi;
        } catch (Exception ignored) {
            // Fallback case: standard bounding
            Rectangle bounds = roi.getBounds();
            return new PolygonRoi(new float[]{bounds.x, bounds.x + bounds.width, bounds.x + bounds.width, bounds.x},
                    new float[]{bounds.y, bounds.y, bounds.y + bounds.height, bounds.y + bounds.height}, 4, Roi.POLYGON);
        }
    }

    /**
     * Method based on <a href="https://github.com/ndefrancesco/macro-frenzy/blob/master/geometry/fitting/fitMinRectangle.ijm">fitMinRectangle</a>
     *
     * @param x1 x1
     * @param y1 y1
     * @param x2 x2
     * @param y2 y2
     * @return dist2
     */
    private static double dist2(double x1, double y1, double x2, double y2) {
        return Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2);
    }

    /**
     * Method based on <a href="https://github.com/ndefrancesco/macro-frenzy/blob/master/geometry/fitting/fitMinRectangle.ijm">fitMinRectangle</a>
     *
     * @param p1x p1x
     * @param p1y p1y
     * @param p2x p2x
     * @param p2y p2y
     * @param x   x
     * @param y   y
     * @return signed distance from a point (x,y) to a line passing through p1 and p2
     */
    private static double perpDist(double p1x, double p1y, double p2x, double p2y, double x, double y) {
        // signed distance from a point (x,y) to a line passing through p1 and p2
        return ((p2x - p1x) * (y - p1y) - (x - p1x) * (p2y - p1y)) / Math.sqrt(dist2(p1x, p1y, p2x, p2y));
    }

    /**
     * Method based on <a href="https://github.com/ndefrancesco/macro-frenzy/blob/master/geometry/fitting/fitMinRectangle.ijm">fitMinRectangle</a>
     *
     * @param p1x p1x
     * @param p1y p1y
     * @param p2x p2x
     * @param p2y p2y
     * @param x   x
     * @param y   y
     * @return signed projection of vector (x,y)-p1 into a line passing through p1 and p2
     */
    private static double parDist(double p1x, double p1y, double p2x, double p2y, double x, double y) {
        // signed projection of vector (x,y)-p1 into a line passing through p1 and p2
        return ((p2x - p1x) * (x - p1x) + (y - p1y) * (p2y - p1y)) / Math.sqrt(dist2(p1x, p1y, p2x, p2y));
    }

    /**
     * Creates a shallow copy of this list
     *
     * @return shallow copy
     */
    public ROI2DListData shallowClone() {
        ROI2DListData result = new ROI2DListData();
        result.addAll(this);
        return result;
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

    /**
     * Saves the ROI list into the selected path
     *
     * @param path the path
     * @return the real path generated by the method. The reason behind this are restrictions of ImageJ ROIs.
     */
    public Path save(Path path) {
        String fileName = path.getFileName().toString();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".roi") && !fileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            if (size() == 1) {
                fileName += ".roi";
            } else {
                fileName += ".zip";
            }
        }
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".roi") && size() != 1) {
            fileName += ".zip";
        }
        path = path.getParent().resolve(fileName);
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".roi")) {
            try {
                FileOutputStream out = new FileOutputStream(path.toFile());
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
                ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(path.toFile())));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
                RoiEncoder re = new RoiEncoder(out);
                Set<String> existing = new HashSet<>();
                for (int i = 0; i < this.size(); i++) {
                    String label = "" + i;
                    Roi roi = this.get(i);
                    if (roi == null) continue;
                    if (roi.getName() != null) {
                        label = roi.getName();
                    }
                    while (label.endsWith(".roi")) {
                        label = label.substring(0, label.length() - 4);
                    }
                    label = StringUtils.makeUniqueString(label, " ", existing);
                    existing.add(label);
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
        return path;
    }

    /**
     * Saves all ROIs in this list as ZIP file
     *
     * @param outputFile the output file
     */
    public void saveToZip(Path outputFile) {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile.toFile()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            RoiEncoder re = new RoiEncoder(out);
            Set<String> existing = new HashSet<>();
            for (int i = 0; i < this.size(); i++) {
                String label = "" + i;
                Roi roi = this.get(i);
                if (roi == null) continue;
                if (roi.getName() != null) {
                    label = roi.getName();
                }
                while (label.endsWith(".roi")) {
                    label = label.substring(0, label.length() - 4);
                }
                label = StringUtils.makeUniqueString(label, " ", existing);
                existing.add(label);
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

    /**
     * Saves all ROIs in this list as *.roi or *.zip file depending on how many ROIs are stored in the list
     * If the extension of the output file is .zip and there is one ROI in the list, the output will still be saved as zip!
     *
     * @param outputFile the output file. without extension.
     * @return the output file with extension
     */
    public Path saveToRoiOrZip(Path outputFile) {
        if (size() == 1 && !outputFile.toString().endsWith(".zip")) {
            outputFile = PathUtils.ensureExtension(outputFile, ".roi");
            saveSingleRoi(get(0), outputFile);
        } else {
            outputFile = PathUtils.ensureExtension(outputFile, ".zip");
            saveToZip(outputFile);
        }
        return outputFile;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        // Code adapted from ImageJ RoiManager class
        if (size() == 1) {
            try {
                FileOutputStream out = new FileOutputStream(storage.getFileSystemPath().resolve(name + ".roi").toFile());
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
                ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(storage.getFileSystemPath().resolve(name + ".zip").toFile())));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
                RoiEncoder re = new RoiEncoder(out);
                Set<String> existing = new HashSet<>();
                for (int i = 0; i < this.size(); i++) {
                    String label = name + "-" + i;
                    Roi roi = this.get(i);
                    if (roi == null) continue;
                    if (roi.getName() != null) {
                        label = roi.getName();
                    }
                    while (label.endsWith(".roi")) {
                        label = label.substring(0, label.length() - 4);
                    }
                    label = StringUtils.makeUniqueString(label, " ", existing);
                    existing.add(label);
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
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new ROI2DListData(this);
    }

    @Override
    public Component preview(int width, int height) {
        ImagePlus mask;
        if (isEmpty()) {
            mask = IJ.createImage("empty", "8-bit", width, height, 1);
        } else {
            ROI2DListData copy = new ROI2DListData(this);
            copy.flatten();
            copy.crop(true, false, false, false);
            Margin margin = new Margin();
            mask = copy.toRGBImage(margin, ROIElementDrawingMode.Always, ROIElementDrawingMode.IfAvailable, 1, Color.RED, Color.RED);
//            mask.setLut(LUT.createLutFromColor(Color.RED));
        }
        if (mask.getWidth() * mask.getHeight() == 0)
            return null;
        return new ImagePlusData(mask).preview(width, height);
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        ImagePlus mask;
        if (isEmpty()) {
            mask = IJ.createImage("empty", "8-bit", width, height, 1);
        } else {
            ROI2DListData copy = new ROI2DListData(this);
            copy.flatten();
            copy.crop(true, false, false, false);
            Margin margin = new Margin();
            mask = copy.toRGBImage(margin, ROIElementDrawingMode.Always, ROIElementDrawingMode.IfAvailable, 1, Color.RED, Color.RED);
//            mask.setLut(LUT.createLutFromColor(Color.RED));
        }
        if (mask.getWidth() * mask.getHeight() == 0)
            return null;
        return new ImagePlusData(mask).createThumbnail(width, height, progressInfo);
    }

    /**
     * Groups the ROI by their reference image
     *
     * @return map of reference image to ROI
     */
    public Map<Optional<ImagePlus>, ROI2DListData> groupByReferenceImage() {
        Map<Optional<ImagePlus>, ROI2DListData> byImage = new HashMap<>();
        for (Roi roi : this) {
            Optional<ImagePlus> key = Optional.ofNullable(roi.getImage());
            ROI2DListData rois = byImage.getOrDefault(key, null);
            if (rois == null) {
                rois = new ROI2DListData();
                byImage.put(key, rois);
            }
            rois.add(roi);
        }
        return byImage;
    }

    /**
     * Scales the ROI in this list and returns a new list containing the scaled instances
     * Does not change the ROIs in this list
     *
     * @param scaleX   the x-scale
     * @param scaleY   the y-scale
     * @param centered if the scaling expands from the ROI center
     */
    public ROI2DListData scale(double scaleX, double scaleY, boolean centered) {
        ROI2DListData result = new ROI2DListData();
        for (Roi roi : this) {
            result.add(RoiScaler.scale(roi, scaleX, scaleY, centered));
        }
        return result;
    }

    /**
     * Rotates the ROI around the center
     * Does not change the ROIs in this list
     *
     * @param angle  the angle
     * @param center the center point
     * @return the rotated ROIs
     */
    public ROI2DListData rotate(double angle, Point2D center) {
        ROI2DListData result = new ROI2DListData();
        for (Roi roi : this) {
            result.add(RoiRotator.rotate(roi, angle, center.getX(), center.getY()));
        }
        return result;
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
        Rectangle bounds = imageArea.getInsideArea(this.getBounds(), new JIPipeExpressionVariablesMap());
        if (bounds == null) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new UnspecifiedValidationReportContext(),
                    "Invalid margin:" + imageArea,
                    "The provided margin is invalid.",
                    "Please check any margin parameters. Set them to Center/Center and all values to zero to be sure."));
        }
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
     * Generates an RGB image from pure ROI data.
     * The ROI's reference images are ignored.
     *
     * @param imageArea            modifications for the image area
     * @param drawOutline          whether to draw an outline
     * @param drawFilledOutline    whether to fill the area
     * @param defaultLineThickness line thickness for drawing
     * @param defaultFillColor     the default fill color
     * @param defaultLineColor     the default line color
     * @return the image
     */
    public ImagePlus toRGBImage(Margin imageArea, ROIElementDrawingMode drawOutline, ROIElementDrawingMode drawFilledOutline, int defaultLineThickness, Color defaultFillColor, Color defaultLineColor) {
        // Find the bounds and future stack position
        Rectangle bounds = imageArea.getInsideArea(this.getBounds(), new JIPipeExpressionVariablesMap());
        if (bounds == null) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new UnspecifiedValidationReportContext(),
                    "Invalid margin:" + imageArea,
                    "The provided margin is invalid.",
                    "Please check any margin parameters. Set them to Center/Center and all values to zero to be sure."));
        }
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

        ImagePlus result = IJ.createImage("ROIs", "RGB", sx, sy, sc, sz, st);
        draw(result, true, true, true, drawOutline, drawFilledOutline, defaultLineThickness, defaultFillColor, defaultLineColor);
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
     * Converts this ROI list into a mask that has the same size as the reference image
     *
     * @param reference         the reference image
     * @param drawFilledOutline fill the ROI
     * @param drawOutline       draw the ROI outlines
     * @param lineThickness     the ROI line thickness
     * @return the mask
     */
    public ImagePlus toMask(ImagePlus reference, boolean drawFilledOutline, boolean drawOutline, int lineThickness) {
        int sx = reference.getWidth();
        int sy = reference.getHeight();
        int sz = reference.getNSlices();
        int sc = reference.getNChannels();
        int st = reference.getNFrames();

        return toMask(drawFilledOutline, drawOutline, lineThickness, sx, sy, sz, sc, st);
    }

    /**
     * Converts this ROI list into a mask that has the same size as the reference image
     *
     * @param drawFilledOutline fill the ROI
     * @param drawOutline       draw the ROI outlines
     * @param lineThickness     the ROI line thickness
     * @param width             output image width
     * @param height            output image height
     * @param numSlices         output image Z slices
     * @param numChannels       output image channel slices
     * @param numFrames         output image frame slices
     * @return the mask
     */
    public ImagePlus toMask(boolean drawFilledOutline, boolean drawOutline, int lineThickness, int width, int height, int numSlices, int numChannels, int numFrames) {
        ImagePlus result = IJ.createImage("ROIs", "8-bit", width, height, numChannels, numSlices, numFrames);
        for (int z = 0; z < numSlices; z++) {
            for (int c = 0; c < numChannels; c++) {
                for (int t = 0; t < numFrames; t++) {
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

    /**
     * Draw on top of an RGB image
     *
     * @param rgbImage             the target image
     * @param drawOutline          draw an outline
     * @param fillOutline          fill the outline
     * @param ignoreZ              do not constrain Z
     * @param ignoreC              do not constrain channel
     * @param ignoreT              do not constrain frame
     * @param defaultLineThickness default line thickness
     * @param defaultFillColor     default fill color
     * @param defaultLineColor     default line color
     */
    public void draw(ImagePlus rgbImage, boolean ignoreZ, boolean ignoreC, boolean ignoreT, ROIElementDrawingMode drawOutline, ROIElementDrawingMode fillOutline, int defaultLineThickness, Color defaultFillColor, Color defaultLineColor) {
        ImageJIterationUtils.forEachIndexedZCTSlice(rgbImage, (processor, index) -> {
            for (Roi roi : this) {
                int rz = ignoreZ ? 0 : roi.getZPosition();
                int rc = ignoreC ? 0 : roi.getCPosition();
                int rt = ignoreT ? 0 : roi.getTPosition();
                if (rz != 0 && rz != (index.getZ() + 1))
                    continue;
                if (rc != 0 && rc != (index.getC() + 1))
                    continue;
                if (rt != 0 && rt != (index.getT() + 1))
                    continue;
                if (fillOutline.shouldDraw(roi.getFillColor(), defaultFillColor)) {
                    Color color = (roi.getFillColor() == null) ? defaultFillColor : roi.getFillColor();
                    processor.setColor(color);
                    processor.fill(roi);
                }
                if (drawOutline.shouldDraw(roi.getStrokeColor(), defaultLineColor)) {
                    Color color = (roi.getStrokeColor() == null) ? defaultLineColor : roi.getStrokeColor();
                    int width = (roi.getStrokeWidth() <= 0) ? defaultLineThickness : (int) roi.getStrokeWidth();
                    processor.setLineWidth(width);
                    processor.setColor(color);
                    roi.drawPixels(processor);
                }
            }
        }, new JIPipeProgressInfo());
    }

    /**
     * Draw on top of an RGB processor with specified coordinates.
     *
     * @param processor            the target processor
     * @param currentIndex         current index in the stack
     * @param ignoreZ              do not constrain Z
     * @param ignoreC              do not constrain channel
     * @param ignoreT              do not constrain frame
     * @param drawOutline          draw an outline
     * @param fillOutline          fill the outline
     * @param drawLabel            draw labels
     * @param defaultLineThickness default line thickness
     * @param defaultFillColor     default fill color
     * @param defaultLineColor     default line color
     * @param highlighted          highlighted rois (other ones are drawn darker)
     */
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
    public void mergeWith(ROI2DListData other) {
        for (Roi item : other) {
            add((Roi) item.clone());
        }
    }

    /**
     * Outlines all {@link Roi} in this list by the specified algorithm.
     * All {@link Roi} are replaced by their outline.
     *
     * @param outline       the method
     * @param errorBehavior what to do on errors
     */
    public void outline(RoiOutline outline, InvalidRoiOutlineBehavior errorBehavior) {
        ImmutableList<Roi> input = ImmutableList.copyOf(this);
        clear();
        for (Roi roi : input) {
            Roi outlined = null;
            try {
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
                    case MinimumBoundingRectangle: {
                        outlined = calculateMinimumBoundingRectangle(roi);
                    }
                    break;
                    case OrientedLine: {
                        Roi tmp = calculateMinimumBoundingRectangle(roi);
                        FloatPolygon mbr = tmp.getFloatPolygon();

                        // Extract the points of the rectangle
                        float x1 = mbr.xpoints[0], y1 = mbr.ypoints[0];
                        float x2 = mbr.xpoints[1], y2 = mbr.ypoints[1];
                        float x3 = mbr.xpoints[2], y3 = mbr.ypoints[2];
                        float x4 = mbr.xpoints[3], y4 = mbr.ypoints[3];

                        // Calculate lengths of sides
                        float width = (float) Point2D.distance(x1, y1, x2, y2);  // Horizontal side
                        float height = (float) Point2D.distance(x2, y2, x3, y3); // Vertical side

                        if (width <= height) {
                            // Horizontal sides are shorter or it's a square
                            Point2D.Float midpoint1 = new Point2D.Float((x1 + x2) / 2, (y1 + y2) / 2);
                            Point2D.Float midpoint2 = new Point2D.Float((x3 + x4) / 2, (y3 + y4) / 2);

                            outlined = new Line(midpoint1.x, midpoint1.y, midpoint2.x, midpoint2.y);
                        } else {
                            // Vertical sides are shorter
                            Point2D.Float midpoint1 = new Point2D.Float((x2 + x3) / 2, (y2 + y3) / 2);
                            Point2D.Float midpoint2 = new Point2D.Float((x4 + x1) / 2, (y4 + y1) / 2);

                            outlined = new Line(midpoint1.x, midpoint1.y, midpoint2.x, midpoint2.y);
                        }
                    }
                    break;
                    case FitCircle:
                        outlined = ImageJROIUtils.fitCircleToRoi(roi);
                        break;
                    case FitEllipse:
                        outlined = ImageJROIUtils.fitEllipseToRoi(roi);
                        break;
                    case FitSpline:
                        outlined = ImageJROIUtils.fitSplineToRoi(roi, false, false);
                        break;
                    case DeleteFitSpline:
                        outlined = ImageJROIUtils.fitSplineToRoi(roi, false, true);
                        break;
                    case FitSplineStraighten:
                        outlined = ImageJROIUtils.fitSplineToRoi(roi, true, false);
                        break;
                    case AreaToLine:
                        outlined = ImageJROIUtils.areaToLine(roi);
                        break;
                    case LineToArea:
                        outlined = Roi.convertLineToArea(roi);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported: " + outline);
                }
            }
            catch (Exception e) {
                if(errorBehavior == InvalidRoiOutlineBehavior.Error) {
                    throw e;
                }
            }

            if(outlined != null) {
                // Restore information
                ImageJROIUtils.copyRoiAttributesAndLocation(roi, outlined);

                // Add to list
                add(outlined);
            }
            else if( errorBehavior == InvalidRoiOutlineBehavior.KeepOriginal) {
                add(roi);
            }
            else if(errorBehavior == InvalidRoiOutlineBehavior.Skip) {
                // Do nothing
            }
            else {
                throw new NullPointerException("Unable to outline ROI " + roi);
            }
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
     * @param imp                  the reference image. Can be null to measure on a black image. Warning: If you provide an existing image that should not be changed, make a duplicate!
     * @param measurements         which measurements to extract
     * @param addNameToTable       if true, add the ROI's name to the table
     * @param measurePhysicalSizes if true, physical sizes will be measured if available
     * @return the measurements
     */
    public ResultsTableData measure(ImagePlus imp, ImageStatisticsSetParameter measurements, boolean addNameToTable, boolean measurePhysicalSizes) {
        ResultsTableData result = new ResultsTableData(new ResultsTable());
        if (addNameToTable) {
            result.addStringColumn("Name");
        }
        if (imp != null) {
            Calibration oldCalibration = imp.getCalibration();
            try {
                if (!measurePhysicalSizes) {
                    imp.setCalibration(null);
                }

                CustomAnalyzer analyzer = new CustomAnalyzer(imp, measurements.getNativeValue(), new ResultsTable());
                ResultsTable rtSys = analyzer.getResultsTable();
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
                                    analyzer.measure();
                                    ResultsTableData forRoi;
                                    if (addNameToTable) {
                                        forRoi = new ResultsTableData();
                                        forRoi.addStringColumn("Name");
                                        forRoi.addRows(new ResultsTableData(rtSys));
                                    } else {
                                        forRoi = new ResultsTableData(rtSys);
                                    }
                                    ImageMeasurementUtils.calculateAdditionalMeasurements(measurements, addNameToTable, roi, forRoi);
                                    result.addRows(forRoi);
                                }
                            }
                        }
                    }
                }
            } finally {
                // Restore
                imp.setSliceWithoutUpdate(1);
                imp.setCalibration(oldCalibration);
            }

        } else {
            imp = createDummyImage();

            CustomAnalyzer analyzer = new CustomAnalyzer(imp, measurements.getNativeValue(), new ResultsTable());
            ResultsTable rtSys = analyzer.getResultsTable();
            rtSys.reset();

            for (Roi roi : this) {
                imp.setRoi(roi);
                rtSys.reset();
                analyzer.measure();
                ResultsTableData forRoi = new ResultsTableData(rtSys);
                ImageMeasurementUtils.calculateAdditionalMeasurements(measurements, addNameToTable, roi, forRoi);
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

        // Ensure that the results table is closed
        SwingUtilities.invokeLater(() -> {
            if (ResultsTable.getResultsWindow() != null) {
                ResultsTable.getResultsWindow().close();
            }
        });


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

    public ImagePlus createBlankCanvas(String title, BitDepth bitDepth) {
        return createBlankCanvas(title, bitDepth.getBitDepth());
    }

    public ImagePlus createBlankCanvas(String title, int bitDepth) {
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        int maxC = 0;
        int maxT = 0;

        for (Roi roi : this) {
            maxX = (int) Math.max(maxX, roi.getBounds().x + roi.getBounds().width);
            maxY = (int) Math.max(maxY, roi.getBounds().y + roi.getBounds().height);
            maxZ = (int) Math.max(maxZ, roi.getZPosition() - 1);
            maxC = Math.max(maxC, roi.getCPosition() - 1);
            maxT = Math.max(maxT, roi.getTPosition() - 1);
        }

        return IJ.createHyperStack(title, maxX + 1, maxY + 1, maxC + 1, maxZ + 1, maxT + 1, bitDepth);
    }

    @Override
    public List<Path> exportOverlayToNapari(ImagePlus imp, Path outputDirectory, String prefix, JIPipeProgressInfo progressInfo) {
        if (!isEmpty()) {
            Path outputFile = outputDirectory.resolve(prefix + "_roi2d.tif");

            progressInfo.log("Exporting " + size() + " 2D ROIs ...");
            RoiDrawer roiDrawer = new RoiDrawer();
            roiDrawer.setDrawOver(false);
            ImagePlus rendered = roiDrawer.draw(imp, this, progressInfo);
            IJ.saveAsTiff(rendered, outputFile.toString());

            return Collections.singletonList(outputFile);
        } else {
            return Collections.emptyList();
        }
    }
}
