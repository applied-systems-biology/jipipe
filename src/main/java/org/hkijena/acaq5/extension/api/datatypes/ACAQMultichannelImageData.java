package org.hkijena.acaq5.extension.api.datatypes;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;

@ACAQDocumentation(name = "Multichannel Image")
public class ACAQMultichannelImageData implements ACAQData {

    private ImagePlus image;

    public ACAQMultichannelImageData(ImagePlus image) {
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
