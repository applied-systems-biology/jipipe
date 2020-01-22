package org.hkijena.acaq5.datatypes;

import org.hkijena.acaq5.ACAQData;

public class ACAQROIData extends ACAQData {

    private static final String DATATYPE_NAME = "ROI";
    private static final String DATATYPE_DESCRIPTION = "Collection of ROI";

    public ACAQROIData() {
        super(DATATYPE_NAME, DATATYPE_DESCRIPTION);
    }
}
