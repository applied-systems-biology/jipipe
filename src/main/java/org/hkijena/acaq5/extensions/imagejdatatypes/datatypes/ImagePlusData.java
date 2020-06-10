package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.exceptions.UserFriendlyNullPointerException;
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
        if (image == null) {
            throw new UserFriendlyNullPointerException("ImagePlus cannot be null!", "No image provided!",
                    "Internal ACAQ5 image type",
                    "An algorithm tried to pass an empty ImageJ image back to ACAQ5. This is not allowed. " +
                            "Either the algorithm inputs are wrong, or there is an error in the program code.",
                    "Please check the inputs via the testbench to see if they are satisfying the algorithm's assumptions. " +
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
    public void saveTo(Path storageFilePath, String name) {
        IJ.saveAsTiff(image, storageFilePath.resolve(name + ".tif").toString());
    }

    @Override
    public ACAQData duplicate() {
        return ACAQData.createInstance(getClass(), image.duplicate());
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

    @Override
    public String toString() {
        return ACAQDataDeclaration.getInstance(getClass()).getName() + " (" + image + ")";
    }
}
