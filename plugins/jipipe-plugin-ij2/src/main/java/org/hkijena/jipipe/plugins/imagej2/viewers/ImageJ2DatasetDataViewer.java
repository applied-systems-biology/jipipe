package org.hkijena.jipipe.plugins.imagej2.viewers;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.plugins.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.viewers.ImagePlusDataViewer;

public class ImageJ2DatasetDataViewer extends ImagePlusDataViewer {
    public ImageJ2DatasetDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
    }

    @Override
    protected void loadDataIntoLegacyViewer(JIPipeData data) {
        if (data instanceof ImageJ2DatasetData) {
            super.loadDataIntoLegacyViewer(((ImageJ2DatasetData) data).wrap());
        }
    }
}
