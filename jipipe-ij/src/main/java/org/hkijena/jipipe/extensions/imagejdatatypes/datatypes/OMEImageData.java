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
import ij.gui.Roi;
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
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import loci.plugins.util.RecordedImageProcessor;
import loci.plugins.util.WindowTools;
import ome.xml.meta.OMEXMLMetadata;
import ome.xml.meta.OMEXMLMetadataRoot;
import ome.xml.model.Image;
import ome.xml.model.ROI;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.exceptions.UserFriendlyNullPointerException;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesSettings;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMEExporterSettings;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ROIHandler;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Data that is a representation of all information saved in Bio-Formats
 */
@JIPipeDocumentation(name = "OME Image", description = "Image that contains additional OME-XML metadata. " +
        "It can be converted into an image, a ROI list, or an XML text.")
@JIPipeHeavyData
public class OMEImageData implements JIPipeData {

    private final ImagePlus image;
    private ROIListData rois;
    private OMEXMLMetadata metadata;
    private OMEExporterSettings exporterSettings = new OMEExporterSettings();

    public static OMEImageData importFrom(Path storageFilePath) {
        Path targetFile = PathUtils.findFileByExtensionIn(storageFilePath, ".tif");
        if (targetFile == null) {
            throw new UserFriendlyNullPointerException("Could not find TIFF file in '" + storageFilePath + "'!",
                    "Unable to find file in location '" + storageFilePath + "'",
                    "ImagePlusData loading",
                    "JIPipe needs to load the image from a folder, but it could not find any matching file.",
                    "Please contact the JIPipe developers about this issue.");
        }
        try {
            ImporterOptions importerOptions = new ImporterOptions();
            importerOptions.setId(targetFile.toString());
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
            ImportProcess process = new ImportProcess(importerOptions);
            if (!process.execute()) {
                throw new NullPointerException();
            }
            ImagePlusReader reader = new ImagePlusReader(process);
            ImagePlus[] images = reader.openImagePlus();
            if (!importerOptions.isVirtual()) {
                process.getReader().close();
            }

            if (images.length == 0) {
                throw new UserFriendlyNullPointerException("OME loaded empty array!",
                        "Could not load image from '" + targetFile + "'",
                        "ImagePlusData loading",
                        "JIPipe used Bio-Formats to load an image from '" + targetFile + "'. Something went wrong.",
                        "Please contact the JIPipe developers about this issue.");
            }
            if (images.length > 1) {
                System.err.println("[JIPipe][ImagePlusData from OME] Encountered multiple images! This should not happen. File=" + targetFile);
            }
            ImagePlus image = images[0];
            OMEXMLMetadata omexmlMetadata = null;
            if (process.getOMEMetadata() instanceof OMEXMLMetadata) {
                omexmlMetadata = (OMEXMLMetadata) process.getOMEMetadata();
            }
            ROIListData rois = ROIHandler.openROIs(process.getOMEMetadata(), new ImagePlus[]{image});
            return new OMEImageData(image, rois, omexmlMetadata);
        } catch (IOException | FormatException e) {
            throw new RuntimeException(e);
        }
    }

    public OMEImageData(ImagePlus image, ROIListData rois, OMEXMLMetadata metadata) {
        this.image = image;
        this.rois = rois;
        this.metadata = metadata;
    }

    @Override
    public Component preview(int width, int height) {
        return new ImagePlusData(image).preview(width, height);
    }

    public ImagePlus getImage() {
        return image;
    }

    public ROIListData getRois() {
        return rois;
    }

    public OMEXMLMetadata getMetadata() {
        return metadata;
    }

    public OMEExporterSettings getExporterSettings() {
        return exporterSettings;
    }

    public void setExporterSettings(OMEExporterSettings exporterSettings) {
        this.exporterSettings = exporterSettings;
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName) {
        OMEExport(this, storageFilePath.resolve(name + ".ome.tif"), exporterSettings);
    }

    @Override
    public JIPipeData duplicate() {
        ImagePlus imp = image.duplicate();
        imp.setTitle(getImage().getTitle());
        OMEImageData copy = new OMEImageData(imp, new ROIListData(rois), metadata);
        copy.exporterSettings = new OMEExporterSettings(exporterSettings);
        return copy;
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        if (rois != null && !rois.isEmpty()) {
            ROIListData data = new ROIListData(rois);
            for (Roi roi : data) {
                roi.setImage(image);
            }
            data.display(displayName, workbench, source);
        } else {
            (new ImagePlusData(image)).display(displayName, workbench, source);
        }
    }

    @Override
    public String toString() {
        String result = "OME [" + image + "]";
        if (rois != null && !rois.isEmpty())
            result += " + [" + rois + "]";
        if (metadata != null)
            result += " + OME-XML";
        return result;
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

    /**
     * Copy of the run method in {@link loci.plugins.out.Exporter} that allows configuration via a parameter object
     *
     * @param outputPath the output path
     * @param settings   the parameters
     */
    public static void OMEExport(OMEImageData imageData, Path outputPath, OMEExporterSettings settings) {
        String outfile = outputPath.toString();
        boolean splitZ = settings.isSplitZ();
        boolean splitC = settings.isSplitC();
        boolean splitT = settings.isSplitT();
        boolean padded = settings.isPadded();
        boolean saveRoi = settings.isSaveROI();
        String compression = settings.getCompression().getCompression();
        boolean noLookupTables = settings.isNoLUT();
        boolean windowless = true;
        ImagePlus imp = imageData.getImage();

        try (IFormatWriter w = new ImageWriter().getWriter(outfile)) {
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

            // Use existing metadata
            if (imageData.getMetadata() != null) {
                xml = imageData.getMetadata().dumpXML();
            }

            OMEXMLService service = null;
            IMetadata store = null;

            try {
                ServiceFactory factory = new ServiceFactory();
                service = factory.getInstance(OMEXMLService.class);
                store = service.createOMEXMLMetadata(xml);
            } catch (DependencyException de) {
            } catch (ServiceException se) {
            }


            if (store == null) IJ.error("OME-XML Java library not found.");

            OMEXMLMetadataRoot root = (OMEXMLMetadataRoot) store.getRoot();
            if (root.sizeOfROIList() > 0) {
                while (root.sizeOfROIList() > 0) {
                    ROI roi = root.getROI(0);
                    root.removeROI(roi);
                }
                store.setRoot(root);
            }
            if (xml == null) {
                store.createRoot();
            } else if (store.getImageCount() > 1) {
                // the original dataset had multiple series
                // we need to modify the IMetadata to represent the correct series
                ArrayList<Integer> matchingSeries = new ArrayList<Integer>();
                for (int series = 0; series < store.getImageCount(); series++) {
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
                    for (int i = 0; i < matchingSeries.size(); i++) {
                        int index = matchingSeries.get(i);
                        String name = store.getImageName(index);
                        boolean valid = true;
                        for (int j = 0; j < matchingSeries.size(); j++) {
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
                } else if (matchingSeries.size() == 1) series = matchingSeries.get(0);

                ome.xml.model.Image exportImage = root.getImage(series);
                List<ome.xml.model.Image> allImages = root.copyImageList();
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
            store.setPixelsSizeC(new PositiveInteger(channels * imp.getNChannels()), 0);
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
                                        FormatTools.getBytesPerPixel(ptype))) {
                    store.setPixelsType(PixelType.fromString(
                            FormatTools.getPixelTypeString(ptype)), 0);
                } else if (FormatTools.isSigned(originalType)) {
                    applyCalibrationFunction = true;
                }
            } catch (EnumerationException e) {
            }

            if (store.getPixelsBinDataCount(0) == 0 ||
                    store.getPixelsBinDataBigEndian(0, 0) == null) {
                store.setPixelsBinDataBigEndian(Boolean.FALSE, 0, 0);
            }
            if (store.getPixelsDimensionOrder(0) == null) {
                store.setPixelsDimensionOrder(settings.getDimensionOrder(), 0);
            }

            LUT[] luts = new LUT[imp.getNChannels()];

            for (int c = 0; c < imp.getNChannels(); c++) {
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
                    imp.getNChannels() * imp.getNSlices() * imp.getNFrames()) {
                if (!windowless) {
                    IJ.showMessageWithCancel("Bio-Formats Exporter Warning",
                            "The number of planes in the stack (" + imp.getImageStackSize() +
                                    ") does not match the number of expected planes (" +
                                    (imp.getNChannels() * imp.getNSlices() * imp.getNFrames()) + ")." +
                                    "\nIf you select 'OK', only " + imp.getImageStackSize() +
                                    " planes will be exported. If you wish to export all of the " +
                                    "planes,\nselect 'Cancel' and convert the Image5D window " +
                                    "to a stack.");
                }
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
            } else if (cal.frameInterval != 0.0 && cal.getTimeUnit().equals("sec")) {
                rate = (int) (1.0 / cal.frameInterval);
            } else {
                // NB: Code from ij.plugin.Animator#animationRate initializer.
                // The value is 7 by default in ImageJ, so must be 7 here as well.
                rate = (int) Prefs.getDouble(Prefs.FPS, 7.0);
            }
            if (rate > 0) w.setFramesPerSecond(rate);

            String[] outputFiles = new String[]{outfile};

            int sizeZ = store.getPixelsSizeZ(0).getValue();
            int sizeC = store.getPixelsSizeC(0).getValue();
            int sizeT = store.getPixelsSizeT(0).getValue();

            if (splitZ || splitC || splitT) {
                int nFiles = 1;
                if (splitZ) {
                    nFiles *= sizeZ;
                }
                if (splitC) {
                    nFiles *= sizeC;
                }
                if (splitT) {
                    nFiles *= sizeT;
                }

                outputFiles = new String[nFiles];

                int dot = outfile.indexOf(".", outfile.lastIndexOf(File.separator));
                String base = outfile.substring(0, dot);
                String ext = outfile.substring(dot);

                int nextFile = 0;
                for (int t = 0; t < (splitT ? sizeT : 1); t++) {
                    for (int z = 0; z < (splitZ ? sizeZ : 1); z++) {
                        for (int c = 0; c < (splitC ? sizeC : 1); c++) {
                            int index = FormatTools.getIndex(settings.getDimensionOrder().getValue(), sizeZ, sizeC, sizeT, sizeZ * sizeC * sizeT, z, c, t);
                            String pattern = base + (splitZ ? "_Z%z" : "") + (splitC ? "_C%c" : "") + (splitT ? "_T%t" : "") + ext;
                            outputFiles[nextFile++] = FormatTools.getFilename(0, index, store, pattern, padded);
                        }
                    }
                }
            }

            if (!w.getFormat().startsWith("OME")) {
                if (splitZ) {
                    store.setPixelsSizeZ(new PositiveInteger(1), 0);
                }
                if (splitC) {
                    store.setPixelsSizeC(new PositiveInteger(1), 0);
                }
                if (splitT) {
                    store.setPixelsSizeT(new PositiveInteger(1), 0);
                }
            }

            // prompt for options

            String[] codecs = w.getCompressionTypes();
            ImageProcessor proc = imp.getImageStack().getProcessor(1);
            java.awt.Image firstImage = proc.createImage();
            firstImage = AWTImageTools.makeBuffered(firstImage, proc.getColorModel());
            int thisType = AWTImageTools.getPixelType((BufferedImage) firstImage);
            if (proc instanceof ColorProcessor) {
                thisType = FormatTools.UINT8;
            } else if (proc instanceof ShortProcessor) {
                thisType = FormatTools.UINT16;
            }

            boolean notSupportedType = !w.isSupportedType(thisType);
            if (notSupportedType) {
                IJ.error("Pixel type (" + FormatTools.getPixelTypeString(thisType) +
                        ") not supported by this format.");
                return;
            }

            if (codecs != null && codecs.length > 1) {
                boolean selected = false;
                if (compression != null) {
                    for (int i = 0; i < codecs.length; i++) {
                        if (codecs[i].equals(compression)) {
                            selected = true;
                            break;
                        }
                    }
                }
            }
            boolean in = false;
            if (outputFiles.length > 1) {
                for (int i = 0; i < outputFiles.length; i++) {
                    if (new File(outputFiles[i]).exists()) {
                        in = true;
                        break;
                    }
                }
            }
            if (in && !windowless) {
                int ret1 = JOptionPane.showConfirmDialog(null,
                        "Some files already exist. \n" +
                                "Would you like to replace them?", "Replace?",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret1 != JOptionPane.OK_OPTION) {
                    return;
                }
                //Delete the files overwrite does not correctly work
                for (int i = 0; i < outputFiles.length; i++) {
                    new File(outputFiles[i]).delete();
                }
            }
            //We are now ready to write the image
            if (compression != null) {
                w.setCompression(compression);
            }
            //Save ROI's
            if (saveRoi) {
                ROIHandler.saveROIs(store, imageData.getRois());
            }
            w.setMetadataRetrieve(store);
            // convert and save slices

            int size = imp.getImageStackSize();
            ImageStack is = imp.getImageStack();
            boolean doStack = w.canDoStacks() && size > 1;
            int start = doStack ? 0 : imp.getCurrentSlice() - 1;
            int end = doStack ? size : start + 1;

            boolean littleEndian = false;
            if (w.getMetadataRetrieve().getPixelsBigEndian(0) != null) {
                littleEndian = !w.getMetadataRetrieve().getPixelsBigEndian(0).booleanValue();
            } else if (w.getMetadataRetrieve().getPixelsBinDataCount(0) == 0) {
                littleEndian = !w.getMetadataRetrieve().getPixelsBinDataBigEndian(0, 0).booleanValue();
            }
            byte[] plane = null;
            w.setInterleaved(false);

            int[] no = new int[outputFiles.length];
            for (int i = start; i < end; i++) {
                if (doStack) {
                    BF.status(false, "Saving plane " + (i + 1) + "/" + size);
                    BF.progress(false, i, size);
                } else BF.status(false, "Saving image");
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
                        for (int pixel = 0; pixel < pixels.length; pixel++) {
                            plane[pixel] = (byte) calibration[pixels[pixel] & 0xff];
                        }
                    } else {
                        plane = (byte[]) proc.getPixels();
                    }
                } else if (proc instanceof ShortProcessor) {
                    short[] pixels = (short[]) proc.getPixels();
                    if (applyCalibrationFunction) {
                        // don't alter 'pixels' directly as that will
                        // affect the open ImagePlus
                        plane = new byte[pixels.length * 2];
                        float[] calibration = proc.getCalibrationTable();
                        for (int pixel = 0; pixel < pixels.length; pixel++) {
                            short v = (short) calibration[pixels[pixel] & 0xffff];
                            DataTools.unpackBytes(
                                    v, plane, pixel * 2, 2, littleEndian);
                        }
                    } else {
                        plane = DataTools.shortsToBytes(pixels, littleEndian);
                    }
                } else if (proc instanceof FloatProcessor) {
                    plane = DataTools.floatsToBytes(
                            (float[]) proc.getPixels(), littleEndian);
                } else if (proc instanceof ColorProcessor) {
                    byte[][] pix = new byte[3][x * y];
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
                            FormatTools.getZCTCoords(settings.getDimensionOrder().getValue(), sizeZ, sizeC, sizeT, size, i);
                    int realZ = sizeZ;
                    int realC = sizeC;
                    int realT = sizeT;

                    if (!splitZ) {
                        coords[0] = 0;
                        realZ = 1;
                    }
                    if (!splitC) {
                        coords[1] = 0;
                        realC = 1;
                    }
                    if (!splitT) {
                        coords[2] = 0;
                        realT = 1;
                    }
                    fileIndex = FormatTools.getIndex(settings.getDimensionOrder().getValue(), realZ, realC, realT,
                            realZ * realC * realT, coords[0], coords[1], coords[2]);
                }

                w.changeOutputFile(outputFiles[fileIndex]);

                int currentChannel = FormatTools.getZCTCoords(
                        settings.getDimensionOrder().getValue(), sizeZ, sizeC, sizeT, imp.getStackSize(), i)[1];

                // only save the lookup table if it is not the default grayscale LUT
                // saving a LUT for every plane can cause performance issues,
                // especially for 16 bit data
                // see https://trello.com/c/Qk6NBnPs/92-imagej-ome-tiff-writing-performance
                if (!proc.isDefaultLut() && !noLookupTables) {
                    if (luts[currentChannel] != null) {
                        // expand to 16-bit LUT if necessary

                        int bpp = FormatTools.getBytesPerPixel(thisType);
                        if (bpp == 1) {
                            w.setColorModel(luts[currentChannel]);
                        } else if (bpp == 2) {
                            int lutSize = luts[currentChannel].getMapSize();
                            byte[][] lut = new byte[3][lutSize];
                            luts[currentChannel].getReds(lut[0]);
                            luts[currentChannel].getGreens(lut[1]);
                            luts[currentChannel].getBlues(lut[2]);

                            short[][] newLut = new short[3][65536];
                            int bins = newLut[0].length / lut[0].length;
                            for (int c = 0; c < newLut.length; c++) {
                                for (int q = 0; q < newLut[c].length; q++) {
                                    int index = q / bins;
                                    newLut[c][q] = (short) ((lut[c][index] * lut[0].length) + (q % bins));
                                }
                            }

                            w.setColorModel(new Index16ColorModel(16, newLut[0].length,
                                    newLut, littleEndian));
                        }
                    } else {
                        w.setColorModel(proc.getColorModel());
                    }
                }
                w.saveBytes(no[fileIndex]++, plane);
            }
        } catch (FormatException e) {
            WindowTools.reportException(e);
        } catch (IOException e) {
            WindowTools.reportException(e);
        }
    }

    /**
     * Copy of the run method in {@link loci.plugins.out.Exporter}, as the plugin code really depends on some UI interaction
     * We also simplified the code, as JIPipe outputs very standard files
     *
     * @param imp        the image
     * @param outputPath the output path
     */
    public static void simpleOMEExport(ImagePlus imp, Path outputPath) {
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
            } catch (DependencyException de) {
            } catch (ServiceException se) {
            }


            if (store == null) IJ.error("OME-XML Java library not found.");

            OMEXMLMetadataRoot root = (OMEXMLMetadataRoot) store.getRoot();
            if (root.sizeOfROIList() > 0) {
                while (root.sizeOfROIList() > 0) {
                    ROI roi = root.getROI(0);
                    root.removeROI(roi);
                }
                store.setRoot(root);
            }
            if (xml == null) {
                store.createRoot();
            } else if (store.getImageCount() > 1) {
                // the original dataset had multiple series
                // we need to modify the IMetadata to represent the correct series
                ArrayList<Integer> matchingSeries = new ArrayList<Integer>();
                for (int series = 0; series < store.getImageCount(); series++) {
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
                    for (int i = 0; i < matchingSeries.size(); i++) {
                        int index = matchingSeries.get(i);
                        String name = store.getImageName(index);
                        boolean valid = true;
                        for (int j = 0; j < matchingSeries.size(); j++) {
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
                } else if (matchingSeries.size() == 1) series = matchingSeries.get(0);

                ome.xml.model.Image exportImage = root.getImage(series);
                List<Image> allImages = root.copyImageList();
                for (Image img : allImages) {
                    if (!img.equals(exportImage)) {
                        root.removeImage(img);
                    }
                }
                store.setRoot(root);
            }

            store.setPixelsSizeX(new PositiveInteger(imp.getWidth()), 0);
            store.setPixelsSizeY(new PositiveInteger(imp.getHeight()), 0);
            store.setPixelsSizeZ(new PositiveInteger(imp.getNSlices()), 0);
            store.setPixelsSizeC(new PositiveInteger(channels * imp.getNChannels()), 0);
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
                                        FormatTools.getBytesPerPixel(ptype))) {
                    store.setPixelsType(PixelType.fromString(
                            FormatTools.getPixelTypeString(ptype)), 0);
                } else if (FormatTools.isSigned(originalType)) {
                    applyCalibrationFunction = true;
                }
            } catch (EnumerationException e) {
            }

            if (store.getPixelsBinDataCount(0) == 0 ||
                    store.getPixelsBinDataBigEndian(0, 0) == null) {
                store.setPixelsBinDataBigEndian(Boolean.FALSE, 0, 0);
            }
            if (store.getPixelsDimensionOrder(0) == null) {
                try {
                    store.setPixelsDimensionOrder(DimensionOrder.fromString(order), 0);
                } catch (EnumerationException e) {
                }
            }

            LUT[] luts = new LUT[imp.getNChannels()];

            for (int c = 0; c < imp.getNChannels(); c++) {
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
            store.setPixelsTimeIncrement(FormatTools.getTime(cal.frameInterval, cal.getTimeUnit()), 0);

            if (imp.getImageStackSize() !=
                    imp.getNChannels() * imp.getNSlices() * imp.getNFrames()) {
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
            } else if (cal.frameInterval != 0.0 && cal.getTimeUnit().equals("sec")) {
                rate = (int) (1.0 / cal.frameInterval);
            } else {
                // NB: Code from ij.plugin.Animator#animationRate initializer.
                // The value is 7 by default in ImageJ, so must be 7 here as well.
                rate = (int) Prefs.getDouble(Prefs.FPS, 7.0);
            }
            if (rate > 0) w.setFramesPerSecond(rate);

            String[] outputFiles = new String[]{outputPath.toString()};

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
            } else if (proc instanceof ShortProcessor) {
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
            if (compression != null)
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
            if (w.getMetadataRetrieve().getPixelsBigEndian(0) != null) {
                littleEndian = !w.getMetadataRetrieve().getPixelsBigEndian(0).booleanValue();
            } else if (w.getMetadataRetrieve().getPixelsBinDataCount(0) == 0) {
                littleEndian = !w.getMetadataRetrieve().getPixelsBinDataBigEndian(0, 0).booleanValue();
            }
            byte[] plane = null;
            w.setInterleaved(false);

            int[] no = new int[outputFiles.length];
            for (int i = start; i < end; i++) {
                if (doStack) {
                    BF.status(false, "Saving plane " + (i + 1) + "/" + size);
                    BF.progress(false, i, size);
                } else BF.status(false, "Saving image");
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
                        for (int pixel = 0; pixel < pixels.length; pixel++) {
                            plane[pixel] = (byte) calibration[pixels[pixel] & 0xff];
                        }
                    } else {
                        plane = (byte[]) proc.getPixels();
                    }
                } else if (proc instanceof ShortProcessor) {
                    short[] pixels = (short[]) proc.getPixels();
                    if (applyCalibrationFunction) {
                        // don't alter 'pixels' directly as that will
                        // affect the open ImagePlus
                        plane = new byte[pixels.length * 2];
                        float[] calibration = proc.getCalibrationTable();
                        for (int pixel = 0; pixel < pixels.length; pixel++) {
                            short v = (short) calibration[pixels[pixel] & 0xffff];
                            DataTools.unpackBytes(
                                    v, plane, pixel * 2, 2, littleEndian);
                        }
                    } else {
                        plane = DataTools.shortsToBytes(pixels, littleEndian);
                    }
                } else if (proc instanceof FloatProcessor) {
                    plane = DataTools.floatsToBytes(
                            (float[]) proc.getPixels(), littleEndian);
                } else if (proc instanceof ColorProcessor) {
                    byte[][] pix = new byte[3][x * y];
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

                w.changeOutputFile(outputFiles[fileIndex]);
                w.saveBytes(no[fileIndex]++, plane);
            }
        } catch (FormatException | IOException e) {
            WindowTools.reportException(e);
        }
    }
}
