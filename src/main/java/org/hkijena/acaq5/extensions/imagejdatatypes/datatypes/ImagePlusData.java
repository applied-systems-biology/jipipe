package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.utils.PathUtils;

import java.nio.file.Path;

/**
 * ImageJ image
 */
@ACAQDocumentation(name = "Image")
@ACAQOrganization(menuPath = "Images")
public class ImagePlusData implements ACAQData {

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
        image = IJ.openImage(PathUtils.findFileByExtensionIn(storageFilePath, ".tif").toString());
    }

    /**
     * @param image wrapped image
     */
    public ImagePlusData(ImagePlus image) {
        this.image = image;
    }

    public ImagePlus getImage() {
        return image;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {
        IJ.saveAsTiff(image, storageFilePath.resolve(name + ".tif").toString());
    }

    /**
     * Gets the dimensionality of {@link ImagePlusData}
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
