package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Converter;
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
}
