package org.hkijena.jipipe.plugins.ij3d.viewers;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.viewers.ImagePlusDataViewer;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.BitDepth;

import javax.swing.*;

public class ROI3DListDataViewer extends ImagePlusDataViewer {
    public ROI3DListDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
    }

    @Override
    protected void loadDataIntoLegacyViewer(JIPipeData data) {
        if (data instanceof ROI3DListData) {
            super.loadDataIntoLegacyViewer(new ImagePlusData(((ROI3DListData) data).createBlankCanvas("ROI", BitDepth.Grayscale8u)));
            getLegacyImageViewer().getViewerPanel2D().getViewerRunnerQueue().enqueue(new AbstractJIPipeRunnable() {
                @Override
                public String getTaskLabel() {
                    return "Converting 3D ROI to 2D ROI";
                }

                @Override
                public void run() {
                    ROI2DListData roi2D = ((ROI3DListData) data).toRoi2D(getProgressInfo());
                    SwingUtilities.invokeLater(() -> {
                        getLegacyImageViewer().addOverlay(roi2D);
                    });
                }
            });
        }
    }

    @Override
    protected void loadDataIntoVtkViewer(JIPipeData data) {
        if (data instanceof ROI3DListData) {
            super.loadDataIntoVtkViewer(new ImagePlusData(((ROI3DListData) data).createBlankCanvas("ROI", BitDepth.Grayscale8u)));
        }
    }
}
