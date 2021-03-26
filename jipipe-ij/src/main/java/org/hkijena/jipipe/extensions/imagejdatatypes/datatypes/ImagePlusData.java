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

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.exceptions.UserFriendlyNullPointerException;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesSettings;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ColoredImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.CacheAwareImagePlusDataViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * ImageJ image
 */
@JIPipeDocumentation(name = "Image")
@JIPipeHeavyData
@JIPipeDataStorageDocumentation("Contains one image file with one of following extensions: *.tif, *.tiff, *.png, *.jpeg, *.jpeg, *.png. " +
        "We recommend the usage of TIFF.")
public class ImagePlusData implements JIPipeData, ColoredImagePlusData {

    /**
     * The dimensionality of this data.
     * -1 means that we do not have information about the dimensionality
     */
    public static final int DIMENSIONALITY = -1;

    private ImagePlus image;
    private ColorSpace colorSpace = new RGBColorSpace();

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

    /**
     * @param image      the wrapped image
     * @param colorSpace the color space. please not that it is ignored if the image is greyscale
     */
    public ImagePlusData(ImagePlus image, ColorSpace colorSpace) {
        if (image == null) {
            throw new UserFriendlyNullPointerException("ImagePlus cannot be null!", "No image provided!",
                    "Internal JIPipe image type",
                    "An algorithm tried to pass an empty ImageJ image back to JIPipe. This is not allowed. " +
                            "Either the algorithm inputs are wrong, or there is an error in the program code.",
                    "Please check the inputs via the quick run to see if they are satisfying the algorithm's assumptions. " +
                            "If you cannot solve the issue, please contact the plugin's author.");
        }
        this.image = image;
        this.colorSpace = colorSpace;
    }

    public static ImagePlus importImagePlusFrom(Path storageFilePath) {
        Path targetFile = PathUtils.findFileByExtensionIn(storageFilePath, ".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp");
        if (targetFile == null) {
            throw new UserFriendlyNullPointerException("Could not find a compatible image file in '" + storageFilePath + "'!",
                    "Unable to find file in location '" + storageFilePath + "'",
                    "ImagePlusData loading",
                    "JIPipe needs to load the image from a folder, but it could not find any matching file.",
                    "Please contact the JIPipe developers about this issue.");
        }
        String fileName = targetFile.toString().toLowerCase();
        if ((fileName.endsWith(".tiff") || fileName.endsWith(".tif")) && ImageJDataTypesSettings.getInstance().isUseBioFormats()) {
            OMEImageData omeImageData = OMEImageData.importFrom(storageFilePath);
            return omeImageData.getImage();
        } else {
            return IJ.openImage(targetFile.toString());
        }
    }

    public static ImagePlusData importFrom(Path storageFilePath) {
        return new ImagePlusData(importImagePlusFrom(storageFilePath));
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

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlusData(data.getImage(), data.getColorSpace());
    }

    public ImagePlus getImage() {
        return image;
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        if (ImageJDataTypesSettings.getInstance().isUseBioFormats() && !(image.getType() == ImagePlus.COLOR_RGB && ImageJDataTypesSettings.getInstance().isSaveRGBWithImageJ())) {
            Path outputPath = storageFilePath.resolve(name + ".ome.tif");
            OMEImageData.simpleOMEExport(image, outputPath);
        } else {
            Path outputPath = storageFilePath.resolve(name + ".tif");
            IJ.saveAsTiff(image, outputPath.toString());
        }
    }

    @Override
    public JIPipeData duplicate() {
        ImagePlus imp = image.duplicate();
        imp.setTitle(getImage().getTitle());
        return JIPipe.createData(getClass(), imp);
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
     * Makes the stored image unique by duplicating it.
     * This is an in-place operation
     */
    public void makeUnique() {
        image = getDuplicateImage();
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        if (source instanceof JIPipeCacheSlotDataSource) {
            CacheAwareImagePlusDataViewerPanel.show(workbench, (JIPipeCacheSlotDataSource) source, displayName);
        } else {
            getDuplicateImage().show();
        }
    }

    @Override
    public Component preview(int width, int height) {
        double factorX = 1.0 * width / image.getWidth();
        double factorY = 1.0 * height / image.getHeight();
        double factor = Math.max(factorX, factorY);
        boolean smooth = factor < 0;
        int imageWidth = (int) (image.getWidth() * factor);
        int imageHeight = (int) (image.getHeight() * factor);
        ImagePlus rgbImage = ImageJUtils.channelsToRGB(image);
        ImageProcessor resized = rgbImage.getProcessor().resize(imageWidth, imageHeight, smooth);
        BufferedImage bufferedImage = resized.getBufferedImage();
        return new JLabel(new ImageIcon(bufferedImage));
    }

    @Override
    public String toString() {
        return JIPipeDataInfo.getInstance(getClass()).getName() + " (" + image + ")";
    }

    @Override
    public ColorSpace getColorSpace() {
        return colorSpace;
    }

}
