package org.hkijena.acaq5.extension.datatypes;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQData;
import org.hkijena.acaq5.api.ACAQDocumentation;

@ACAQDocumentation(name = "Greyscale Image")
public class ACAQGreyscaleImageData implements ACAQData {

    private ImagePlus image;

    public ACAQGreyscaleImageData(ImagePlus image) {
        this.image = image;
    }

    public ImagePlus getImage() {
        return image;
    }
}
