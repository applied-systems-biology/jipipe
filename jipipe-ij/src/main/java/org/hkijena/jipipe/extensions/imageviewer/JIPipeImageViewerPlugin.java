package org.hkijena.jipipe.extensions.imageviewer;

import ij.ImagePlus;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.scijava.Disposable;

import javax.swing.*;

public abstract class JIPipeImageViewerPlugin implements JIPipeWorkbenchAccess, Disposable {
    private final JIPipeImageViewer viewerPanel;

    public JIPipeImageViewerPlugin(JIPipeImageViewer viewerPanel) {
        this.viewerPanel = viewerPanel;
    }

    public JIPipeImageViewer getViewerPanel() {
        return viewerPanel;
    }

    public ImagePlus getCurrentImagePlus() {
        return viewerPanel.getImagePlus();
    }

    public ImagePlusData getCurrentImage() {
        return viewerPanel.getImage();
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return getViewerPanel().getWorkbench();
    }

    /**
     * Gets the current data source if any is set
     * @return the data source or null
     */
    public JIPipeDataSource getDataSource() {
        return getViewerPanel().getDataSource();
    }

    /**
     * Called when the current image is changed
     */
    public void onImageChanged() {

    }

    /**
     * Called when the form panel should be recreated
     *
     * @param formPanel the form panel
     */
    public void initializeSettingsPanel(FormPanel formPanel) {

    }

    /**
     * The tool panel category where this tool is shown
     *
     * @return the category
     */
    public abstract String getCategory();

    /**
     * The icon for the category if a new one must be created
     *
     * @return the icon
     */
    public abstract Icon getCategoryIcon();

    /**
     * Called if an overlay was added
     * @param overlay the overlay
     */
    public void onOverlayAdded(Object overlay) {

    }

    /**
     * Called if an overlay was removed
     * @param overlay the overlay
     */
    public void onOverlayRemoved(Object overlay) {

    }

    /**
     * Called if the overlays were cleared
     */
    public void onOverlaysCleared() {

    }
}
