package org.hkijena.jipipe.plugins.imp.viewers;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.viewers.ImagePlusDataViewer;
import org.hkijena.jipipe.plugins.imp.datatypes.ImpImageData;

public class ImpImageDataViewer extends ImagePlusDataViewer {

    public ImpImageDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
    }

    @Override
    protected void loadDataIntoLegacyViewer(JIPipeData data) {
        if(data instanceof ImpImageData) {
            super.loadDataIntoLegacyViewer(new ImagePlusData(((ImpImageData) data).toImagePlus(true, 8)));
        }
    }

    @Override
    protected void loadDataIntoVtkViewer(JIPipeData data) {
        super.loadDataIntoVtkViewer(new ImagePlusData(((ImpImageData) data).toImagePlus(true, 8)));
    }
}
