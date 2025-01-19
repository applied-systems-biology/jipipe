package org.hkijena.jipipe.plugins.ijtrackmate.viewers;

import ij.ImagePlus;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.ModelData;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.plugins.ijtrackmate.display.spots.SpotsManagerPlugin2D;
import org.hkijena.jipipe.plugins.ijtrackmate.display.tracks.TracksManagerPlugin2D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.viewers.ImagePlusDataViewer;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin;

import java.util.ArrayList;
import java.util.List;

import static org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer.DEFAULT_PLUGINS;

public class TracksSpotsDataViewer extends ImagePlusDataViewer {
    public TracksSpotsDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
    }

    @Override
    protected void loadDataIntoLegacyViewer(JIPipeData data) {
        getLegacyImageViewer().clearOverlays();
        if(data instanceof ModelData) {
            ImagePlus img = ((ModelData) data).getImage();
            super.loadDataIntoLegacyViewer(new ImagePlusData(img));

            if(data instanceof SpotsCollectionData) {
                getLegacyImageViewer().addOverlay(data);
            }
        }
    }

    @Override
    protected List<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> getLegacyImageViewerPlugins() {
        List<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> plugins = new ArrayList<>(DEFAULT_PLUGINS);
        plugins.add(SpotsManagerPlugin2D.class);
        plugins.add(TracksManagerPlugin2D.class);
        return plugins;
    }
}
