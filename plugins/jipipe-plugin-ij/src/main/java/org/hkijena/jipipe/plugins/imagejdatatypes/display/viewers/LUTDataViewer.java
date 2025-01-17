package org.hkijena.jipipe.plugins.imagejdatatypes.display.viewers;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.LUTData;

public class LUTDataViewer extends ImagePlusDataViewer {
    public LUTDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
    }

    @Override
    protected void loadDataIntoLegacyViewer(JIPipeData data) {
        if(data instanceof LUTData) {
            super.loadDataIntoLegacyViewer(new ImagePlusData(((LUTData) data).toImage(256, 256)));
        }
    }

    @Override
    protected void loadDataIntoVtkViewer(JIPipeData data) {
        super.loadDataIntoVtkViewer(new ImagePlusData(((LUTData) data).toImage(256, 256)));
    }
}
