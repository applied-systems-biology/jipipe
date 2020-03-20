package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;

@ACAQDocumentation(name = "Greyscale Image")
public class ACAQGreyscaleImageData extends ACAQMultichannelImageData {
    public ACAQGreyscaleImageData(ImagePlus image) {
        super(image);
    }
}
