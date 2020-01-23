package org.hkijena.acaq5.datatypes;

import ij.ImagePlus;
import org.hkijena.acaq5.ACAQData;
import org.hkijena.acaq5.ACAQDocumentation;

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
