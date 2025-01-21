package org.hkijena.jipipe.plugins.opencv.viewers;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.viewers.ImagePlusDataViewer;
import org.hkijena.jipipe.plugins.opencv.datatypes.OpenCvImageData;

public class OpenCvImageDataViewer extends ImagePlusDataViewer {
    public OpenCvImageDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
    }

    @Override
    protected void loadDataIntoLegacyViewer(JIPipeData data) {
        if (data instanceof OpenCvImageData) {
            super.loadDataIntoLegacyViewer(new ImagePlusData(((OpenCvImageData) data).toImagePlus()));
        }
    }
}
