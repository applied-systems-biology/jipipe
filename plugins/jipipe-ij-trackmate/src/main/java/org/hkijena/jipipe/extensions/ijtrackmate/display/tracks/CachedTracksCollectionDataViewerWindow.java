/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.ijtrackmate.display.tracks;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.display.spots.SpotsManagerPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerCacheDataViewerWindow;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPlugin;

import java.util.List;
import java.util.Map;

public class CachedTracksCollectionDataViewerWindow extends JIPipeImageViewerCacheDataViewerWindow {

    public CachedTracksCollectionDataViewerWindow(JIPipeDesktopWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
    }

    @Override
    protected void initializePlugins(List<Class<? extends JIPipeImageViewerPlugin>> plugins, Map<Class<?>, Object> contextObjects) {
        super.initializePlugins(plugins, contextObjects);
        plugins.add(SpotsManagerPlugin2D.class);
        plugins.add(TracksManagerPlugin2D.class);
    }

    @Override
    protected void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        getImageViewer().clearOverlays();
        TrackCollectionData trackCollectionData = JIPipe.getDataTypes().convert(virtualData.getData(progressInfo), TrackCollectionData.class, progressInfo);
        getImageViewer().addOverlay(trackCollectionData);
        getImageViewer().setError(null);
        getImageViewer().setImagePlus(trackCollectionData.getImage());
        fitImageToScreenOnce();
    }

}
