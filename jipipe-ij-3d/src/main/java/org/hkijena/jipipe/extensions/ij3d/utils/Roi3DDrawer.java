package org.hkijena.jipipe.extensions.ij3d.utils;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;

public class Roi3DDrawer extends AbstractJIPipeParameterCollection {
    private boolean drawOverReference = true;

    public Roi3DDrawer() {
    }

    public Roi3DDrawer(Roi3DDrawer other) {
        this.drawOverReference = other.drawOverReference;
    }

    public ImagePlus renderToRGB(ROI3DListData roi3DListData, ImagePlus referenceImage, JIPipeProgressInfo progressInfo) {
        return null;
    }
}
