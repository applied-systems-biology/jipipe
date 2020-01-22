package org.hkijena.acaq5.datatypes;

import ij.ImagePlus;
import org.hkijena.acaq5.ACAQData;

public class ACAQMaskData extends ACAQData {

    private static final String DATATYPE_NAME = "Mask";
    private static final String DATATYPE_DESCRIPTION = "Binary mask data";

    private ImagePlus mask;

    public ACAQMaskData(ImagePlus mask) {
        super(DATATYPE_NAME, DATATYPE_DESCRIPTION);
        this.mask = mask;
    }

    public ImagePlus getMask() {
        return mask;
    }
}
