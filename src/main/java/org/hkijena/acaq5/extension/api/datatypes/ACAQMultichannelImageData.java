package org.hkijena.acaq5.extension.api.datatypes;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQData;
import org.hkijena.acaq5.api.ACAQDocumentation;

@ACAQDocumentation(name = "Multichannel Image")
public class ACAQMultichannelImageData {

    private ImagePlus image;

    public ACAQMultichannelImageData(ImagePlus image) {
        this.image = image;
    }

    public ImagePlus getImage() {
        return image;
    }
}
