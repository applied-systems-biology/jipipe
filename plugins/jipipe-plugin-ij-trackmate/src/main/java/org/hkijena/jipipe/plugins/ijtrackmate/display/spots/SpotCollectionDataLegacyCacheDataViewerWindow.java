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

package org.hkijena.jipipe.plugins.ijtrackmate.display.spots;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.plugins.imageviewer.legacy.ImageLegacyCacheDataViewerWindow;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin;

import java.util.List;
import java.util.Map;

public class SpotCollectionDataLegacyCacheDataViewerWindow extends ImageLegacyCacheDataViewerWindow {

    public SpotCollectionDataLegacyCacheDataViewerWindow(JIPipeDesktopWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
    }

    @Override
    protected void initializePlugins(List<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> plugins, Map<Class<?>, Object> contextObjects) {
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
