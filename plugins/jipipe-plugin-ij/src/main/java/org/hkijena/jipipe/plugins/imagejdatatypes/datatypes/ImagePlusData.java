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

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.process.*;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeCommonData;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeImageThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesApplicationSettings;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.RGBColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * ImageJ image
 */
@SetJIPipeDocumentation(name = "ImageJ Image", description = "An ImageJ image")
@LabelAsJIPipeHeavyData
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one image file with one of following extensions: *.tif, *.tiff, *.png, *.jpeg, *.jpg, *.png. " +
        "We recommend the usage of TIFF.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/imageplus-data.schema.json")
@ImageTypeInfo
@LabelAsJIPipeCommonData
public class ImagePlusData implements JIPipeData {

    private ImagePlus image;
    private ColorSpace colorSpace = new RGBColorSpace();
    private List<JIPipeData> overlays = new ArrayList<>();

    /**
     * @param image wrapped image
     */
    public ImagePlusData(ImagePlus image) {
        this.image = Objects.requireNonNull(image);
    }


    /**
     * @param image      the wrapped image
     * @param colorSpace the color space. please note that it is ignored if the image is greyscale
     */
    public ImagePlusData(ImagePlus image, ColorSpace colorSpace) {
        this.image = Objects.requireNonNull(image);
        this.colorSpace = colorSpace;
    }

    public static ImagePlus importImagePlusFrom(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp");
        if (targetFile == null) {
            throw new JIPipeValidationRuntimeException(
                    new FileNotFoundException("Unable to find file in location '" + storage + "'"),
                    "Could not find a compatible image file in '" + storage + "'!",
                    "JIPipe needs to load the image from a folder, but it could not find any matching file.",
                    "Please contact the JIPipe developers about this issue.");
        }
        String fileName = targetFile.toString().toLowerCase(Locale.ROOT);
        ImagePlus outputImage;
        if ((fileName.endsWith(".tiff") || fileName.endsWith(".tif")) && ImageJDataTypesApplicationSettings.getInstance().isUseBioFormats()) {
            OMEImageData omeImageData = OMEImageData.importData(storage, progressInfo);
            outputImage = omeImageData.getImage();
        } else {
            progressInfo.log("ImageJ import " + targetFile);
            outputImage = IJ.openImage(targetFile.toString());
        }
        if (outputImage.getOverlay() == null || outputImage.getOverlay().size() == 0) {
            // Import ROI
            Path roiFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".roi", ".zip");
            if (roiFile != null) {
                ROI2DListData rois = ROI2DListData.importData(storage, progressInfo.resolve("Import ROI"));
                Overlay overlay = new Overlay();
                for (Roi roi : rois) {
                    overlay.add(roi);
                }
                outputImage.setOverlay(overlay);
            }
        }
        return outputImage;
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus = importImagePlusFrom(storage, progressInfo);
        ImagePlusData imagePlusData = new ImagePlusData(imagePlus);
        if (storage.exists("overlays") && storage.isDirectory("overlays")) {
            JIPipeDataTable dataTable = JIPipeDataTable.importData(storage.resolve("overlays"), progressInfo.resolve("Import overlays"));
            for (int i = 0; i < dataTable.getRowCount(); i++) {
                imagePlusData.addOverlay(dataTable.getData(i, JIPipeData.class, progressInfo));
            }
        }
        return imagePlusData;
    }

    public static ColorSpace getColorSpaceOf(Class<? extends ImagePlusData> dataType) {
        return (ColorSpace) ReflectionUtils.newInstance(dataType.getAnnotation(ImageTypeInfo.class).colorSpace());
    }

    /**
     * Gets the dimensionality of this
     *
     * @param klass the class
     * @return the dimensionality
     */
    public static int getDimensionalityOf(Class<? extends ImagePlusData> klass) {
        return klass.getAnnotation(ImageTypeInfo.class).numDimensions();
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlusData(data.getImage(), data.getColorSpace());
    }

    /**
     * Gets overlay data
     *
     * @return the overlays
     */
    public List<JIPipeData> getOverlays() {
        return overlays;
    }

    /**
     * Sets overlay data
     *
     * @param overlays the overlay
     */
    public void setOverlays(List<JIPipeData> overlays) {
        this.overlays = overlays;
    }

    public void removeOverlay(JIPipeData overlay) {
        if (overlays != null) {
            overlays.remove(overlay);
        }
    }

    public void addOverlay(JIPipeData overlay) {
        if (overlays == null) {
            overlays = new ArrayList<>();
        }
        overlays.add(overlay);
    }

    public void removeOverlaysOfType(Class<? extends JIPipeData> klass) {
        if (overlays != null) {
            overlays.removeIf(overlay -> klass.isAssignableFrom(overlay.getClass()));
        }
    }

    public int getWidth() {
        return getImage().getWidth();
    }

    public int getHeight() {
        return getImage().getHeight();
    }

    public int getNChannels() {
        return getImage().getNChannels();
    }

    public int getNSlices() {
        return getImage().getNSlices();
    }

    public int getNFrames() {
        return getImage().getNFrames();
    }

    /**
     * Returns true if the image is grayscale
     *
     * @return if the image is grayscale
     */
    public boolean isGrayscale() {
        switch (getImage().getType()) {
            case ImagePlus.GRAY8:
            case ImagePlus.GRAY16:
            case ImagePlus.GRAY32:
                return true;
            case ImagePlus.COLOR_RGB:
            case ImagePlus.COLOR_256:
                return false;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the appropriate Java type that stores the pixel data.
     * Does not return Java primitives, but instead their wrapper classes.
     *
     * @return the pixel type
     */
    public Class<?> getPixelType() {
        switch (getImage().getType()) {
            case ImagePlus.GRAY8:
            case ImagePlus.COLOR_256:
                return Byte.class;
            case ImagePlus.GRAY16:
                return Short.class;
            case ImagePlus.GRAY32:
                return Float.class;
            case ImagePlus.COLOR_RGB:
                return Integer.class;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the appropriate {@link ImageProcessor} that stores the pixel data.
     *
     * @return the {@link ImageProcessor} type
     */
    public Class<? extends ImageProcessor> getImageProcessorType() {
        switch (getImage().getType()) {
            case ImagePlus.GRAY8:
            case ImagePlus.COLOR_256:
                return ByteProcessor.class;
            case ImagePlus.GRAY16:
                return ShortProcessor.class;
            case ImagePlus.GRAY32:
                return FloatProcessor.class;
            case ImagePlus.COLOR_RGB:
                return ColorProcessor.class;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * The ImageJ image
     *
     * @return the image
     */
    public ImagePlus getImage() {
        return image;
    }

    /**
     * The image used by viewers
     *
     * @param duplicate if the returned image is a duplicate (a separate copy)
     * @return the image. is not necessarily equal to getImage()
     */
    public ImagePlus getViewedImage(boolean duplicate) {
        if (duplicate)
            return getDuplicateImage();
        else
            return getImage();
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        if (ImageJDataTypesApplicationSettings.getInstance().isUseBioFormats() && !(image.getType() == ImagePlus.COLOR_RGB && ImageJDataTypesApplicationSettings.getInstance().isSaveRGBWithImageJ())) {
            Path outputPath = PathUtils.ensureExtension(storage.getFileSystemPath().resolve(name), ".ome.tif", ".ome.tiff");
            OMEImageData.simpleOMEExport(image, outputPath);
        } else {
            Path outputPath = PathUtils.ensureExtension(storage.getFileSystemPath().resolve(name), ".tif", ".tiff");
            IJ.saveAsTiff(image, outputPath.toString());
        }
        if (image.getOverlay() != null) {
            ROI2DListData rois = new ROI2DListData();
            for (Roi roi : image.getOverlay()) {
                rois.add(roi);
            }
            rois.exportData(storage, name, forceName, progressInfo.resolve("Save ROI"));
        }
        if (overlays != null && !overlays.isEmpty()) {
            JIPipeDataTable dataTable = new JIPipeDataTable(JIPipeData.class);
            for (JIPipeData overlay : overlays) {
                dataTable.addData(overlay, progressInfo);
            }
            dataTable.exportData(storage.resolve("overlays"), progressInfo.resolve("Export overlays"));
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        ImagePlus imp = ImageJUtils.duplicate(image);
        imp.setTitle(getImage().getTitle());
        return JIPipe.createData(getClass(), imp, colorSpace);
    }

    /**
     * Returns a duplicate of the contained image
     *
     * @return the duplicate
     */
    public ImagePlus getDuplicateImage() {
        ImagePlus imp = ImageJUtils.duplicate(getImage());
        imp.copyAttributes(getImage());
        return imp;
    }

    /**
     * Makes the stored image unique by duplicating it.
     * This is an in-place operation
     */
    public void makeUnique() {
        image = getDuplicateImage();
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        double factorX = 1.0 * width / image.getWidth();
        double factorY = 1.0 * height / image.getHeight();
        double factor = Math.min(factorX, factorY);
        boolean smooth = false;
        int imageWidth = (int) Math.max(1, image.getWidth() * factor);
        int imageHeight = (int) Math.max(1, image.getHeight() * factor);
        ImagePlus rgbImage = ImageJUtils.channelsToRGB(image);
        if (rgbImage.getStackSize() != 1) {
            // Reduce processing time
            rgbImage = new ImagePlus("Preview", rgbImage.getProcessor().duplicate()); // The duplicate is important (calibration!)
        }
        if (rgbImage == image) {
            rgbImage = ImageJUtils.duplicate(rgbImage);
        }
//            if (rgbImage.getType() != ImagePlus.COLOR_RGB) {
//                ImageJUtils.calibrate(rgbImage, ImageJCalibrationMode.AutomaticImageJ, 0, 1);
//            }
        if (rgbImage.getType() != ImagePlus.COLOR_RGB) {
            // Copy LUT
            rgbImage.setLut(image.getProcessor().getLut());

            // Render to RGB
            rgbImage = ImageJUtils.renderToRGBWithLUTIfNeeded(rgbImage, new JIPipeProgressInfo());
        } else {
            // Convert to RGB if necessary (HSB, LAB, ...)
            getColorSpace().convertToRGB(rgbImage, new JIPipeProgressInfo());
        }

        // ROI rendering
        ROI2DListData rois = new ROI2DListData();
        if (image.getRoi() != null)
            rois.add(image.getRoi());
        if (image.getOverlay() != null) {
            rois.addAll(Arrays.asList(image.getOverlay().toArray()));
        }
        if (!rois.isEmpty()) {
            if (rgbImage == image || rgbImage.getProcessor() == image.getProcessor()) {
                rgbImage = ImageJUtils.duplicate(rgbImage);
            }
            rois.draw(rgbImage.getProcessor(),
                    new ImageSliceIndex(0, 0, 0),
                    false,
                    false,
                    false,
                    true,
                    false,
                    false,
                    1,
                    Color.RED,
                    Color.YELLOW,
                    Collections.emptyList());
        }

        ImageProcessor resized = rgbImage.getProcessor().resize(imageWidth, imageHeight, smooth);
        return new JIPipeImageThumbnailData(resized);
    }

    @Override
    public Component preview(int width, int height) {
        double factorX = 1.0 * width / image.getWidth();
        double factorY = 1.0 * height / image.getHeight();
        double factor = Math.min(factorX, factorY);
        boolean smooth = factor < 0;
        int imageWidth = (int) Math.max(1, image.getWidth() * factor);
        int imageHeight = (int) Math.max(1, image.getHeight() * factor);
        ImagePlus rgbImage = ImageJUtils.channelsToRGB(image);
        if (rgbImage.getStackSize() != 1) {
            // Reduce processing time
            rgbImage = new ImagePlus("Preview", rgbImage.getProcessor().duplicate()); // The duplicate is important (calibration!)
        }
        if (rgbImage == image) {
            rgbImage = ImageJUtils.duplicate(rgbImage);
        }
//            if (rgbImage.getType() != ImagePlus.COLOR_RGB) {
//                ImageJUtils.calibrate(rgbImage, ImageJCalibrationMode.AutomaticImageJ, 0, 1);
//            }
        if (rgbImage.getType() != ImagePlus.COLOR_RGB) {
            // Copy LUT
            rgbImage.setLut(image.getProcessor().getLut());

            // Render to RGB
            rgbImage = ImageJUtils.renderToRGBWithLUTIfNeeded(rgbImage, new JIPipeProgressInfo());
        } else {
            // Convert to RGB if necessary (HSB, LAB, ...)
            getColorSpace().convertToRGB(rgbImage, new JIPipeProgressInfo());
        }

        // ROI rendering
        ROI2DListData rois = new ROI2DListData();
        if (image.getRoi() != null)
            rois.add(image.getRoi());
        if (image.getOverlay() != null) {
            rois.addAll(Arrays.asList(image.getOverlay().toArray()));
        }
        if (!rois.isEmpty()) {
            if (rgbImage == image || rgbImage.getProcessor() == image.getProcessor()) {
                rgbImage = ImageJUtils.duplicate(rgbImage);
            }
            rois.draw(rgbImage.getProcessor(),
                    new ImageSliceIndex(0, 0, 0),
                    false,
                    false,
                    false,
                    true,
                    false,
                    false,
                    1,
                    Color.RED,
                    Color.YELLOW,
                    Collections.emptyList());
        }

        ImageProcessor resized = rgbImage.getProcessor().resize(imageWidth, imageHeight, smooth);
        BufferedImage bufferedImage = resized.getBufferedImage();
        return new JLabel(new ImageIcon(bufferedImage));
    }

    @Override
    public String toString() {
        return JIPipeDataInfo.getInstance(getClass()).getName() + " (" + image + ")";
    }

    public ColorSpace getColorSpace() {
        return colorSpace;
    }

    public <T extends JIPipeData> List<T> extractOverlaysOfType(Class<T> klass) {
        List<T> result = new ArrayList<>();
        if (overlays != null) {
            for (JIPipeData overlay : overlays) {
                if (klass.isAssignableFrom(overlay.getClass())) {
                    result.add((T) overlay);
                }
            }
        }
        return result;
    }

    public ImagePlusData shallowCopy() {
        ImagePlusData imagePlusData = new ImagePlusData(getImage());
        imagePlusData.copyMetadata(this);
        return imagePlusData;
    }

    public void copyMetadata(ImagePlusData other) {
        if (other.overlays != null) {
            setOverlays(new ArrayList<>(other.overlays));
        }
    }

    public void ensureComposite() {
        if (!getImage().isComposite()) {
            image = new CompositeImage(image, CompositeImage.COMPOSITE);
        }
    }
}
