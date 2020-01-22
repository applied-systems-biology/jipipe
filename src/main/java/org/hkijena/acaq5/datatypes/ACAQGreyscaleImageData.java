package org.hkijena.acaq5.datatypes;

import ij.ImagePlus;
import org.hkijena.acaq5.ACAQData;

public class ACAQGreyscaleImageData extends ACAQData {

    private static final String DATATYPE_NAME = "Greyscale Image";
    private static final String DATATYPE_DESCRIPTION = "";

    private ImagePlus image;

    public ACAQGreyscaleImageData(ImagePlus image) {
        super(DATATYPE_NAME, DATATYPE_DESCRIPTION);
        this.image = image;
    }

    public ImagePlus getImage() {
        return image;
    }
}
