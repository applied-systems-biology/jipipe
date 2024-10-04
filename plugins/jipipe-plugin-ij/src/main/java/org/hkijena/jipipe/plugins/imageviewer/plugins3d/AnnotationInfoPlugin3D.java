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

package org.hkijena.jipipe.plugins.imageviewer.plugins3d;

import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopLegacyCacheDataViewerWindow;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopCachedDataViewerAnnotationInfoPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.ImagePlusDataLegacyCacheDataViewerWindow;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewerPlugin3D;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * To be used with {@link ImagePlusDataLegacyCacheDataViewerWindow} and other similar implementations that
 * have access to the cache
 */
public class AnnotationInfoPlugin3D extends JIPipeImageViewerPlugin3D {

    private final JIPipeDesktopLegacyCacheDataViewerWindow cacheDataViewerWindow;
    private final JIPipeDesktopCachedDataViewerAnnotationInfoPanel infoPanel;

    public AnnotationInfoPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
        this.cacheDataViewerWindow = viewerPanel.getContextObject(JIPipeDesktopLegacyCacheDataViewerWindow.class);
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

    public JIPipeDesktopLegacyCacheDataViewerWindow getCacheDataViewerWindow() {
        return cacheDataViewerWindow;
    }
}
