package org.hkijena.jipipe.extensions.imageviewer.plugins2d;

import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPanel;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanelPlugin2D;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.ui.cache.JIPipeCachedDataViewerAnnotationInfoPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * To be used with {@link org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedImagePlusDataViewerWindow} and other similar implementations that
 * have access to the cache
 */
public class AnnotationInfoPlugin2D extends ImageViewerPanelPlugin2D {

    private final JIPipeCacheDataViewerWindow cacheDataViewerWindow;
    private final JIPipeCachedDataViewerAnnotationInfoPanel infoPanel;

    public AnnotationInfoPlugin2D(JIPipeImageViewerPanel viewerPanel) {
        super(viewerPanel);
        this.cacheDataViewerWindow = viewerPanel.getContextObject(JIPipeCacheDataViewerWindow.class);
        this.infoPanel = new JIPipeCachedDataViewerAnnotationInfoPanel(cacheDataViewerWindow.getWorkbench());
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
    public void initializeSettingsPanel(FormPanel formPanel) {
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

    public JIPipeCacheDataViewerWindow getCacheDataViewerWindow() {
        return cacheDataViewerWindow;
    }
}
