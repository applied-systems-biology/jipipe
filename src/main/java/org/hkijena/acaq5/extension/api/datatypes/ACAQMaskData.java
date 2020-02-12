package org.hkijena.acaq5.extension.api.datatypes;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;

@ACAQDocumentation(name = "Mask", description = "Binary mask image")
public class ACAQMaskData extends ACAQGreyscaleImageData {
    public ACAQMaskData(ImagePlus mask) {
        super(mask);
    }
}
