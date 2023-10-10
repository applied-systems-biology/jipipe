/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.display.spots;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerCacheDataViewerWindow;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPlugin;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.util.List;
import java.util.Map;

public class CachedSpotCollectionDataViewerWindow extends JIPipeImageViewerCacheDataViewerWindow {

    public CachedSpotCollectionDataViewerWindow(JIPipeWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
    }

    @Override
    protected void initializePlugins(List<Class<? extends JIPipeImageViewerPlugin>> plugins, Map<Class<?>, Object> contextObjects) {
        super.initializePlugins(plugins, contextObjects);
        plugins.add(SpotsManagerPlugin2D.class);
    }

    @Override
    protected void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        getImageViewer().clearOverlays();
        SpotsCollectionData data = JIPipe.getDataTypes().convert(virtualData.getData(progressInfo), SpotsCollectionData.class, progressInfo);
        getImageViewer().addOverlay(data);
        getImageViewer().setError(null);
        getImageViewer().setImagePlus(data.getImage());
        fitImageToScreenOnce();
    }

}
