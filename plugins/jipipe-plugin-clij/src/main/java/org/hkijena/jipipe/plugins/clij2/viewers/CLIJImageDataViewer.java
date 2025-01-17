package org.hkijena.jipipe.plugins.clij2.viewers;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.plugins.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.viewers.ImagePlusDataViewer;

public class CLIJImageDataViewer extends ImagePlusDataViewer {
    public CLIJImageDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
    }

    @Override
    protected void loadDataIntoLegacyViewer(JIPipeData data) {
        if(data instanceof CLIJImageData) {
            super.loadDataIntoLegacyViewer(((CLIJImageData) data).pull());
        }
    }

    @Override
    protected void loadDataIntoVtkViewer(JIPipeData data) {
        if(data instanceof CLIJImageData) {
            super.loadDataIntoVtkViewer(((CLIJImageData) data).pull());
        }
    }
}
