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

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.process.ShortProcessor;
import loci.common.DataTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.IFormatWriter;
import loci.formats.ImageWriter;
import loci.formats.MetadataTools;
import loci.formats.gui.AWTImageTools;
import loci.formats.gui.Index16ColorModel;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.LociExporter;
import loci.plugins.in.ImporterOptions;
import loci.plugins.util.ROIHandler;
import loci.plugins.util.RecordedImageProcessor;
import loci.plugins.util.WindowTools;
import ome.xml.meta.OMEXMLMetadataRoot;
import ome.xml.model.Image;
import ome.xml.model.ROI;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyNullPointerException;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * ImageJ image
 */
@JIPipeDocumentation(name = "Image")
public class ImagePlusData implements JIPipeData {

    /**
     * The dimensionality of this data.
     * -1 means that we do not have information about the dimensionality
     */
    public static final int DIMENSIONALITY = -1;

    private ImagePlus image;

    /**
     * Initializes the data from a folder containing a TIFF file
     *
     * @param storageFilePath folder that contains a *.tif file
     */
    public ImagePlusData(Path storageFilePath) {
        Path targetFile = PathUtils.findFileByExtensionIn(storageFilePath, ".tif");
        if(targetFile == null) {
            throw new UserFriendlyNullPointerException("Could not find TIFF file in '" + storageFilePath + "'!",
                    "Unable to find file in location '" + storageFilePath + "'",
                    "ImagePlusData loading",
                    "JIPipe needs to load the image from a folder, but it could not find any matching file.",
                    "Please contact the JIPipe developers about this issue.");
        }
        if(ImageJDataTypesSettings.getInstance().isUseBioFormats()) {
            try {
                ImporterOptions importerOptions = new ImporterOptions();
                importerOptions.setId(storageFilePath.toString());
                importerOptions.setAutoscale(true);
                importerOptions.setColorMode(ImporterOptions.COLOR_MODE_DEFAULT);
                importerOptions.setQuiet(true);
                importerOptions.setShowMetadata(false);
                importerOptions.setShowOMEXML(false);
                importerOptions.setShowROIs(false);
                importerOptions.setSplitChannels(false);
                importerOptions.setSplitFocalPlanes(false);
                importerOptions.setSplitTimepoints(false);
                importerOptions.setStackOrder(ImporterOptions.ORDER_XYCZT);
                importerOptions.setVirtual(false);
                importerOptions.setSwapDimensions(false);
                importerOptions.setWindowless(true);
                ImagePlus[] images = BF.openImagePlus(importerOptions);
                if(images.length == 0) {
                    throw new UserFriendlyNullPointerException("OME loaded empty array!",
                            "Could not load image from '" + targetFile + "'",
                            "ImagePlusData loading",
                            "JIPipe used Bio-Formats to load an image from '" + targetFile + "'. Something went wrong.",
                            "Please contact the JIPipe developers about this issue.");
                }
                if(images.length > 1) {
                    System.err.println("[JIPipe][ImagePlusData from OME] Encountered multiple images! This should not happen. File=" + targetFile);
                }
                image = images[0];
            } catch (IOException | FormatException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            image = IJ.openImage(targetFile.toString());
        }
    }

    /**
     * @param image wrapped image
     */
    public ImagePlusData(ImagePlus image) {
        if (image == null) {
            throw new UserFriendlyNullPointerException("ImagePlus cannot be null!", "No image provided!",
                    "Internal JIPipe image type",
                    "An algorithm tried to pass an empty ImageJ image back to JIPipe. This is not allowed. " +
                            "Either the algorithm inputs are wrong, or there is an error in the program code.",
                    "Please check the inputs via the quick run to see if they are satisfying the algorithm's assumptions. " +
                            "If you cannot solve the issue, please contact the plugin's author.");
        }
        this.image = image;
    }

    @Override
    public void flush() {
//        // Completely remove all references
//        image.flush();
//        image = null;
    }

    public ImagePlus getImage() {
        return image;
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName) {
        if(ImageJDataTypesSettings.getInstance().isUseBioFormats()) {
            Path outputPath = storageFilePath.resolve(name + ".ome.tif");
            exportOME(image, outputPath);
        }
        else {
            Path outputPath = storageFilePath.resolve(name + ".tif");
            IJ.saveAsTiff(image, outputPath.toString());
        }
    }

    /**
     * Copy of the run method in {@link loci.plugins.out.Exporter}, as the plugin code really depends on some UI interaction
     * We also simplified the code, as JIPipe outputs very standard files
     * @param imp the image
     * @param outputPath the output path
     */
    public static void exportOME(ImagePlus imp, Path outputPath) {
        String order = ImporterOptions.ORDER_XYCZT;
        String compression = ImageJDataTypesSettings.getInstance().getBioFormatsCompression().getCompression();

        try (IFormatWriter w = new ImageWriter().getWriter(outputPath.toString())) {
            int ptype = 0;
            int channels = 1;
            switch (imp.getType()) {
                case ImagePlus.GRAY8:
                case ImagePlus.COLOR_256:
                    ptype = FormatTools.UINT8;
                    break;
                case ImagePlus.COLOR_RGB:
                    channels = 3;
                    ptype = FormatTools.UINT8;
                    break;
                case ImagePlus.GRAY16:
                    ptype = FormatTools.UINT16;
                    break;
                case ImagePlus.GRAY32:
                    ptype = FormatTools.FLOAT;
                    break;
            }
            String title = imp.getTitle();

            w.setWriteSequentially(true);
            FileInfo fi = imp.getOriginalFileInfo();
            String xml = fi == null ? null : fi.description == null ? null :
                    fi.description.indexOf("xml") == -1 ? null : fi.description;

            OMEXMLService service = null;
            IMetadata store = null;

            try {
                ServiceFactory factory = new ServiceFactory();
                service = factory.getInstance(OMEXMLService.class);
                store = service.createOMEXMLMetadata(xml);
            }
            catch (DependencyException de) { }
            catch (ServiceException se) { }



            if (store == null) IJ.error("OME-XML Java library not found.");

            OMEXMLMetadataRoot root = (OMEXMLMetadataRoot) store.getRoot();
            if (root.sizeOfROIList()>0){
                while (root.sizeOfROIList() > 0) {
                    ROI roi = root.getROI(0);
                    root.removeROI(roi);
                }
                store.setRoot(root);
            }
            if (xml == null) {
                store.createRoot();
            }
            else if (store.getImageCount() > 1) {
                // the original dataset had multiple series
                // we need to modify the IMetadata to represent the correct series
                ArrayList<Integer> matchingSeries = new ArrayList<Integer>();
                for (int series=0; series<store.getImageCount(); series++) {
                    String type = store.getPixelsType(series).toString();
                    int pixelType = FormatTools.pixelTypeFromString(type);
                    if (pixelType == ptype) {
                        String imageName = store.getImageName(series);
                        if (title.indexOf(imageName) >= 0) {
                            matchingSeries.add(series);
                        }
                    }
                }

                int series = 0;
                if (matchingSeries.size() > 1) {
                    for (int i=0; i<matchingSeries.size(); i++) {
                        int index = matchingSeries.get(i);
                        String name = store.getImageName(index);
                        boolean valid = true;
                        for (int j=0; j<matchingSeries.size(); j++) {
                            if (i != j) {
                                String compName = store.getImageName(matchingSeries.get(j));
                                if (compName.indexOf(name) >= 0) {
                                    valid = false;
                                    break;
                                }
                            }
                        }
                        if (valid) {
                            series = index;
                            break;
                        }
                    }
                }
                else if (matchingSeries.size() == 1) series = matchingSeries.get(0);

                ome.xml.model.Image exportImage = root.getImage(series);
                List<Image> allImages = root.copyImageList();
                for (ome.xml.model.Image img : allImages) {
                    if (!img.equals(exportImage)) {
                        root.removeImage(img);
                    }
                }
                store.setRoot(root);
            }

            store.setPixelsSizeX(new PositiveInteger(imp.getWidth()), 0);
            store.setPixelsSizeY(new PositiveInteger(imp.getHeight()), 0);
            store.setPixelsSizeZ(new PositiveInteger(imp.getNSlices()), 0);
            store.setPixelsSizeC(new PositiveInteger(channels*imp.getNChannels()), 0);
            store.setPixelsSizeT(new PositiveInteger(imp.getNFrames()), 0);

            if (store.getImageID(0) == null) {
                store.setImageID(MetadataTools.createLSID("Image", 0), 0);
            }
            if (store.getPixelsID(0) == null) {
                store.setPixelsID(MetadataTools.createLSID("Pixels", 0), 0);
            }

            // reset the pixel type, unless the only change is signedness
            // this prevents problems if the user changed the bit depth of the image
            boolean applyCalibrationFunction = false;
            try {
                int originalType = -1;
                if (store.getPixelsType(0) != null) {
                    originalType = FormatTools.pixelTypeFromString(
                            store.getPixelsType(0).toString());
                }
                if (ptype != originalType &&
                        (store.getPixelsType(0) == null ||
                                !FormatTools.isSigned(originalType) ||
                                FormatTools.getBytesPerPixel(originalType) !=
                                        FormatTools.getBytesPerPixel(ptype)))
                {
                    store.setPixelsType(PixelType.fromString(
                            FormatTools.getPixelTypeString(ptype)), 0);
                }
                else if (FormatTools.isSigned(originalType)) {
                    applyCalibrationFunction = true;
                }
            }
            catch (EnumerationException e) { }

            if (store.getPixelsBinDataCount(0) == 0 ||
                    store.getPixelsBinDataBigEndian(0, 0) == null)
            {
                store.setPixelsBinDataBigEndian(Boolean.FALSE, 0, 0);
            }
            if (store.getPixelsDimensionOrder(0) == null) {
                try {
                    store.setPixelsDimensionOrder(DimensionOrder.fromString(order), 0);
                }
                catch (EnumerationException e) { }
            }

            LUT[] luts = new LUT[imp.getNChannels()];

            for (int c=0; c<imp.getNChannels(); c++) {
                if (c >= store.getChannelCount(0) || store.getChannelID(0, c) == null) {
                    String lsid = MetadataTools.createLSID("Channel", 0, c);
                    store.setChannelID(lsid, 0, c);
                }
                store.setChannelSamplesPerPixel(new PositiveInteger(channels), 0, 0);

                if (imp instanceof CompositeImage) {
                    luts[c] = ((CompositeImage) imp).getChannelLut(c + 1);
                }
            }

            Calibration cal = imp.getCalibration();

            store.setPixelsPhysicalSizeX(FormatTools.getPhysicalSizeX(cal.pixelWidth, cal.getXUnit()), 0);
            store.setPixelsPhysicalSizeY(FormatTools.getPhysicalSizeY(cal.pixelHeight, cal.getYUnit()), 0);
            store.setPixelsPhysicalSizeZ(FormatTools.getPhysicalSizeZ(cal.pixelDepth, cal.getZUnit()), 0);
            store.setPixelsTimeIncrement(FormatTools.getTime(new Double(cal.frameInterval), cal.getTimeUnit()), 0);

            if (imp.getImageStackSize() !=
                    imp.getNChannels() * imp.getNSlices() * imp.getNFrames())
            {
                store.setPixelsSizeZ(new PositiveInteger(imp.getImageStackSize()), 0);
                store.setPixelsSizeC(new PositiveInteger(1), 0);
                store.setPixelsSizeT(new PositiveInteger(1), 0);
            }

            Object info = imp.getProperty("Info");
            if (info != null) {
                String imageInfo = info.toString();
                if (imageInfo != null) {
                    String[] lines = imageInfo.split("\n");
                    for (String line : lines) {
                        int eq = line.lastIndexOf("=");
                        if (eq > 0) {
                            String key = line.substring(0, eq).trim();
                            String value = line.substring(eq + 1).trim();

                            if (key.endsWith("BitsPerPixel")) {
                                w.setValidBitsPerPixel(Integer.parseInt(value));
                                break;
                            }
                        }
                    }
                }
            }

            // NB: Animation rate code copied from ij.plugin.Animator#doOptions().
            final int rate;
            if (cal.fps != 0.0) {
                rate = (int) cal.fps;
            }
            else if (cal.frameInterval != 0.0 && cal.getTimeUnit().equals("sec")) {
                rate = (int) (1.0 / cal.frameInterval);
            }
            else {
                // NB: Code from ij.plugin.Animator#animationRate initializer.
                // The value is 7 by default in ImageJ, so must be 7 here as well.
                rate = (int) Prefs.getDouble(Prefs.FPS, 7.0);
            }
            if (rate > 0) w.setFramesPerSecond(rate);

            String[] outputFiles = new String[] {outputPath.toString()};

            int sizeZ = store.getPixelsSizeZ(0).getValue();
            int sizeC = store.getPixelsSizeC(0).getValue();
            int sizeT = store.getPixelsSizeT(0).getValue();

            // prompt for options

            String[] codecs = w.getCompressionTypes();
            ImageProcessor proc = imp.getImageStack().getProcessor(1);
            java.awt.Image firstImage = proc.createImage();
            firstImage = AWTImageTools.makeBuffered(firstImage, proc.getColorModel());
            int thisType = AWTImageTools.getPixelType((BufferedImage) firstImage);
            if (proc instanceof ColorProcessor) {
                thisType = FormatTools.UINT8;
            }
            else if (proc instanceof ShortProcessor) {
                thisType = FormatTools.UINT16;
            }

            boolean notSupportedType = !w.isSupportedType(thisType);
            if (notSupportedType) {
                IJ.error("Pixel type (" + FormatTools.getPixelTypeString(thisType) +
                        ") not supported by this format.");
                return;
            }

            boolean in = false;
            //We are now ready to write the image
//            if (f != null) f.delete(); //delete the file.
            if(compression != null)
                w.setCompression(compression);
//            //Save ROI's
//            if (saveRoi != null && saveRoi.booleanValue()) {
//                ROIHandler.saveROIs(store);
//            }
            w.setMetadataRetrieve(store);
            // convert and save slices

            int size = imp.getImageStackSize();
            ImageStack is = imp.getImageStack();
            boolean doStack = w.canDoStacks() && size > 1;
            int start = doStack ? 0 : imp.getCurrentSlice() - 1;
            int end = doStack ? size : start + 1;

            boolean littleEndian = false;
            if (w.getMetadataRetrieve().getPixelsBigEndian(0) != null)
            {
                littleEndian = !w.getMetadataRetrieve().getPixelsBigEndian(0).booleanValue();
            }
            else if (w.getMetadataRetrieve().getPixelsBinDataCount(0) == 0) {
                littleEndian = !w.getMetadataRetrieve().getPixelsBinDataBigEndian(0, 0).booleanValue();
            }
            byte[] plane = null;
            w.setInterleaved(false);

            int[] no = new int[outputFiles.length];
            for (int i=start; i<end; i++) {
                if (doStack) {
                    BF.status(false, "Saving plane " + (i + 1) + "/" + size);
                    BF.progress(false, i, size);
                }
                else BF.status(false, "Saving image");
                proc = is.getProcessor(i + 1);

                if (proc instanceof RecordedImageProcessor) {
                    proc = ((RecordedImageProcessor) proc).getChild();
                }

                int x = proc.getWidth();
                int y = proc.getHeight();

                if (proc instanceof ByteProcessor) {
                    if (applyCalibrationFunction) {
                        // don't alter 'pixels' directly as that will
                        // affect the open ImagePlus
                        byte[] pixels = (byte[]) proc.getPixels();
                        plane = new byte[pixels.length];
                        float[] calibration = proc.getCalibrationTable();
                        for (int pixel=0; pixel<pixels.length; pixel++) {
                            plane[pixel] = (byte) calibration[pixels[pixel] & 0xff];
                        }
                    }
                    else {
                        plane = (byte[]) proc.getPixels();
                    }
                }
                else if (proc instanceof ShortProcessor) {
                    short[] pixels = (short[]) proc.getPixels();
                    if (applyCalibrationFunction) {
                        // don't alter 'pixels' directly as that will
                        // affect the open ImagePlus
                        plane = new byte[pixels.length * 2];
                        float[] calibration = proc.getCalibrationTable();
                        for (int pixel=0; pixel<pixels.length; pixel++) {
                            short v = (short) calibration[pixels[pixel] & 0xffff];
                            DataTools.unpackBytes(
                                    v, plane, pixel * 2, 2, littleEndian);
                        }
                    }
                    else {
                        plane = DataTools.shortsToBytes(pixels, littleEndian);
                    }
                }
                else if (proc instanceof FloatProcessor) {
                    plane = DataTools.floatsToBytes(
                            (float[]) proc.getPixels(), littleEndian);
                }
                else if (proc instanceof ColorProcessor) {
                    byte[][] pix = new byte[3][x*y];
                    ((ColorProcessor) proc).getRGB(pix[0], pix[1], pix[2]);
                    plane = new byte[3 * x * y];
                    System.arraycopy(pix[0], 0, plane, 0, x * y);
                    System.arraycopy(pix[1], 0, plane, x * y, x * y);
                    System.arraycopy(pix[2], 0, plane, 2 * x * y, x * y);

                    if (i == start) {
                        sizeC /= 3;
                    }
                }

                int fileIndex = 0;
                if (doStack) {
                    int[] coords =
                            FormatTools.getZCTCoords(order, sizeZ, sizeC, sizeT, size, i);
                    int realZ = sizeZ;
                    int realC = sizeC;
                    int realT = sizeT;

                    coords[0] = 0;
                    realZ = 1;
                    coords[1] = 0;
                    realC = 1;
                    coords[2] = 0;
                    realT = 1;
                    fileIndex = FormatTools.getIndex(order, realZ, realC, realT,
                            realZ * realC * realT, coords[0], coords[1], coords[2]);
                }

                if (notSupportedType) {
                    IJ.error("Pixel type not supported by this format.");
                }
                else {
                    w.changeOutputFile(outputFiles[fileIndex]);

//                    int currentChannel = FormatTools.getZCTCoords(
//                            ORDER, sizeZ, sizeC, sizeT, imp.getStackSize(), i)[1];

//                    // only save the lookup table if it is not the default grayscale LUT
//                    // saving a LUT for every plane can cause performance issues,
//                    // especially for 16 bit data
//                    // see https://trello.com/c/Qk6NBnPs/92-imagej-ome-tiff-writing-performance
//                    if (!proc.isDefaultLut() && (noLookupTables == null || !noLookupTables)) {
//                        if (luts[currentChannel] != null) {
//                            // expand to 16-bit LUT if necessary
//
//                            int bpp = FormatTools.getBytesPerPixel(thisType);
//                            if (bpp == 1) {
//                                w.setColorModel(luts[currentChannel]);
//                            }
//                            else if (bpp == 2) {
//                                int lutSize = luts[currentChannel].getMapSize();
//                                byte[][] lut = new byte[3][lutSize];
//                                luts[currentChannel].getReds(lut[0]);
//                                luts[currentChannel].getGreens(lut[1]);
//                                luts[currentChannel].getBlues(lut[2]);
//
//                                short[][] newLut = new short[3][65536];
//                                int bins = newLut[0].length / lut[0].length;
//                                for (int c=0; c<newLut.length; c++) {
//                                    for (int q=0; q<newLut[c].length; q++) {
//                                        int index = q / bins;
//                                        newLut[c][q] = (short) ((lut[c][index] * lut[0].length) + (q % bins));
//                                    }
//                                }
//
//                                w.setColorModel(new Index16ColorModel(16, newLut[0].length,
//                                        newLut, littleEndian));
//                            }
//                        }
//                        else {
//                            w.setColorModel(proc.getColorModel());
//                        }
//                    }
                    w.saveBytes(no[fileIndex]++, plane);
                }
            }
            w.close();
        }
        catch (FormatException e) {
            WindowTools.reportException(e);
        }
        catch (IOException e) {
            WindowTools.reportException(e);
        }
    }

    @Override
    public JIPipeData duplicate() {
        ImagePlus imp = image.duplicate();
        imp.setTitle(getImage().getTitle());
        return JIPipeData.createInstance(getClass(), imp);
    }

    /**
     * Returns a duplicate of the contained image
     *
     * @return the duplicate
     */
    public ImagePlus getDuplicateImage() {
        ImagePlus imp = image.duplicate();
        imp.setTitle(getImage().getTitle());
        return imp;
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench) {
        getDuplicateImage().show();
    }

    @Override
    public String toString() {
        return JIPipeDataInfo.getInstance(getClass()).getName() + " (" + image + ")";
    }

    /**
     * Gets the dimensionality of {@link ImagePlusData}
     *
     * @param klass the class
     * @return the dimensionality
     */
    public static int getDimensionalityOf(Class<? extends ImagePlusData> klass) {
        try {
            return klass.getDeclaredField("DIMENSIONALITY").getInt(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
