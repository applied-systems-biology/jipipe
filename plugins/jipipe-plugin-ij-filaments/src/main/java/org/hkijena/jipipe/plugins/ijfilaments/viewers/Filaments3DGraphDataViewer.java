package org.hkijena.jipipe.plugins.ijfilaments.viewers;

import ij.ImagePlus;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.display.FilamentsManagerPlugin2D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.viewers.ImagePlusDataViewer;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.BitDepth;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin;

import java.util.ArrayList;
import java.util.List;

import static org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer.DEFAULT_PLUGINS;

public class Filaments3DGraphDataViewer extends ImagePlusDataViewer {
    public Filaments3DGraphDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
    }

    @Override
    protected void loadDataIntoLegacyViewer(JIPipeData data) {
        getLegacyImageViewer().clearOverlays();
        if (data instanceof Filaments3DGraphData) {
            Filaments3DGraphData graphData = (Filaments3DGraphData) data;
            ImagePlus canvas = graphData.createBlankCanvas("Filaments", BitDepth.Grayscale8u);
            super.loadDataIntoLegacyViewer(new ImagePlusData(canvas));
            getLegacyImageViewer().addOverlay(data);
        }
    }

    @Override
    protected List<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> getLegacyImageViewerPlugins() {
        List<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> plugins = new ArrayList<>(DEFAULT_PLUGINS);
        plugins.add(FilamentsManagerPlugin2D.class);
        return plugins;
    }
}
