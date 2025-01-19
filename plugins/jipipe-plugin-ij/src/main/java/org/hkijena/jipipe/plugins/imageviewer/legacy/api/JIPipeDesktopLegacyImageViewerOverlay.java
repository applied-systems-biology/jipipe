package org.hkijena.jipipe.plugins.imageviewer.legacy.api;

import java.util.Set;

/**
 * Implement this interface to allow overlays to add the necessary plugins into the image viewer
 */
public interface JIPipeDesktopLegacyImageViewerOverlay {
    Set<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> getRequiredLegacyImageViewerPlugins();
}
