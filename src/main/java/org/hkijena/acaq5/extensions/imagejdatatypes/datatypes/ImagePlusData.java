package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Path;

@ACAQDocumentation(name = "Image")
@ACAQOrganization(menuPath = "Images")
public class ImagePlusData implements ACAQData {

    private ImagePlus image;

    public ImagePlusData(Path storageFilePath) throws IOException {
        image = IJ.openImage(PathUtils.findFileByExtensionIn(storageFilePath, ".tif").toString());
    }

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
