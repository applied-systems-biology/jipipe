package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.ui.cache.JIPipeCachedDataViewerAnnotationInfoPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.nio.file.Path;

/**
 * To be used with {@link org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedImagePlusDataViewerWindow} and other similar implementations that
 * have access to the cache
 */
public class AnnotationInfoPlugin extends ImageViewerPanelPlugin {

    private final JIPipeCacheDataViewerWindow cacheDataViewerWindow;
    private final JIPipeCachedDataViewerAnnotationInfoPanel infoPanel;

    public AnnotationInfoPlugin(ImageViewerPanel viewerPanel, JIPipeCacheDataViewerWindow cacheDataViewerWindow) {
        super(viewerPanel);
        this.infoPanel = new JIPipeCachedDataViewerAnnotationInfoPanel(cacheDataViewerWindow.getWorkbench());
        this.cacheDataViewerWindow = cacheDataViewerWindow;
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
    public void createPalettePanel(FormPanel formPanel) {
        formPanel.addVerticalGlue(infoPanel, null);
    }

    @Override
    public void onImageChanged() {
        if (cacheDataViewerWindow != null && cacheDataViewerWindow.getDataSource() != null) {
            infoPanel.displayAnnotations(cacheDataViewerWindow.getDataSource());
        }
        else {
            infoPanel.displayAnnotations(null);
        }
    }

    public JIPipeCacheDataViewerWindow getCacheDataViewerWindow() {
        return cacheDataViewerWindow;
    }
}
