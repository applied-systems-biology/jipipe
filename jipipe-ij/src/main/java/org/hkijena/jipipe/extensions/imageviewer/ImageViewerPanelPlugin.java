package org.hkijena.jipipe.extensions.imageviewer;

import ij.ImagePlus;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.FormPanel;

import javax.swing.*;

public abstract class ImageViewerPanelPlugin implements JIPipeWorkbenchAccess {
    private final ImageViewerPanel viewerPanel;

    public ImageViewerPanelPlugin(ImageViewerPanel viewerPanel) {
        this.viewerPanel = viewerPanel;
    }

    public ImageViewerPanel getViewerPanel() {
        return viewerPanel;
    }

    public ImagePlus getCurrentImage() {
        return viewerPanel.getImage();
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return getViewerPanel().getWorkbench();
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
}
