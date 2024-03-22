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

package org.hkijena.jipipe.extensions.imageviewer;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.scijava.Disposable;

import javax.swing.*;

public abstract class JIPipeImageViewerPlugin implements JIPipeDesktopWorkbenchAccess, Disposable {
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

    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return getViewerPanel().getDesktopWorkbench();
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return getViewerPanel().getWorkbench();
    }

    /**
     * Gets the current data source if any is set
     *
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
    public void initializeSettingsPanel(JIPipeDesktopFormPanel formPanel) {

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
     *
     * @param overlay the overlay
     */
    public void onOverlayAdded(Object overlay) {

    }

    /**
     * Called if an overlay was removed
     *
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
