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

package org.hkijena.jipipe.extensions.imageviewer.plugins3d;

import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopCacheDataViewerWindow;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopCachedDataViewerAnnotationInfoPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPlugin3D;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * To be used with {@link org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedImagePlusDataViewerWindow} and other similar implementations that
 * have access to the cache
 */
public class AnnotationInfoPlugin3D extends JIPipeImageViewerPlugin3D {

    private final JIPipeDesktopCacheDataViewerWindow cacheDataViewerWindow;
    private final JIPipeDesktopCachedDataViewerAnnotationInfoPanel infoPanel;

    public AnnotationInfoPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
        this.cacheDataViewerWindow = viewerPanel.getContextObject(JIPipeDesktopCacheDataViewerWindow.class);
        this.infoPanel = new JIPipeDesktopCachedDataViewerAnnotationInfoPanel(cacheDataViewerWindow.getWorkbench());
    }

    @Override
    public String getCategory() {
        return "Annotations";
    }

    @Override
    public Icon getCategoryIcon() {
        return UIUtils.getIconFromResources("data-types/annotation.png");
    }

    @Override
    public void initializeSettingsPanel(JIPipeDesktopFormPanel formPanel) {
        formPanel.addVerticalGlue(infoPanel, null);
    }

    @Override
    public void onImageChanged() {
        if (cacheDataViewerWindow != null && cacheDataViewerWindow.getDataSource() != null) {
            infoPanel.displayAnnotations(cacheDataViewerWindow.getDataSource());
        } else {
            infoPanel.displayAnnotations(null);
        }
    }

    public JIPipeDesktopCacheDataViewerWindow getCacheDataViewerWindow() {
        return cacheDataViewerWindow;
    }
}
