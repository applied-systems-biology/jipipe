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

package org.hkijena.jipipe.plugins.ijfilaments.display;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.BitDepth;
import org.hkijena.jipipe.plugins.imageviewer.ImageLegacyCacheDataViewerWindow;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewerPlugin;

import java.util.List;
import java.util.Map;

public class FilamentsLegacyCacheDataViewerWindow extends ImageLegacyCacheDataViewerWindow {

    public FilamentsLegacyCacheDataViewerWindow(JIPipeDesktopWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
    }

    @Override
    protected void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData data = JIPipe.getDataTypes().convert(virtualData.getData(progressInfo), Filaments3DGraphData.class, progressInfo);
        getImageViewer().setError(null);
        ImagePlus image = data.createBlankCanvas("empty", BitDepth.Grayscale8u);
        getImageViewer().clearOverlays();
        getImageViewer().setImagePlus(image);
        getImageViewer().addOverlay(data);
        fitImageToScreenOnce();
    }

    @Override
    protected void initializePlugins(List<Class<? extends JIPipeImageViewerPlugin>> plugins, Map<Class<?>, Object> contextObjects) {
        super.initializePlugins(plugins, contextObjects);
        plugins.add(FilamentsManagerPlugin2D.class);
        plugins.add(FilamentsManagerPlugin3D.class);
    }
}
