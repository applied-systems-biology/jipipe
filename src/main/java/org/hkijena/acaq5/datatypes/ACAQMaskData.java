package org.hkijena.acaq5.datatypes;

import ij.ImagePlus;
import org.hkijena.acaq5.ACAQDocumentation;

@ACAQDocumentation(name = "Mask", description = "Binary mask image")
public class ACAQMaskData extends ACAQGreyscaleImageData {
    public ACAQMaskData(ImagePlus mask) {
        super(mask);
    }
}
