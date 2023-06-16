/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imageviewer.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;

/**
 * All settings for {@link JIPipeGraphEditorUI}
 */
public class ImageViewer2DUISettings extends AbstractJIPipeParameterCollection {

    public static String ID = "image-viewer-ui-2d";
    private boolean showSideBar = true;
    private double defaultAnimationFPS = 24;
    private double zoomBaseSpeed = 0.05;

    private double zoomDynamicSpeed = 0.1;


    private boolean exportAsDisplayed = true;

    public static ImageViewer2DUISettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, ImageViewer2DUISettings.class);
    }

    @JIPipeDocumentation(name = "Export images/movies as displayed", description = "If enabled, the 'Snapshot' menu will images and movies as displayed (including zoom). Otherwise, " +
            "the image in its original size is exported.")
    @JIPipeParameter("export-as-displayed")
    public boolean isExportAsDisplayed() {
        return exportAsDisplayed;
    }

    @JIPipeParameter("export-as-displayed")
    public void setExportAsDisplayed(boolean exportAsDisplayed) {
        this.exportAsDisplayed = exportAsDisplayed;
    }

    @JIPipeDocumentation(name = "Show side bar", description = "If enabled, show a side bar with additional settings and tools")
    @JIPipeParameter("show-side-bar")
    public boolean isShowSideBar() {
        return showSideBar;
    }

    @JIPipeParameter("show-side-bar")
    public void setShowSideBar(boolean showSideBar) {
        this.showSideBar = showSideBar;
    }

    @JIPipeDocumentation(name = "Animation FPS", description = "The default animation FPS")
    @JIPipeParameter("default-animation-fps")
    public double getDefaultAnimationFPS() {
        return defaultAnimationFPS;
    }

    @JIPipeParameter("default-animation-fps")
    public void setDefaultAnimationFPS(double defaultAnimationFPS) {
        this.defaultAnimationFPS = defaultAnimationFPS;
    }

    @JIPipeDocumentation(name = "Zoom base speed", description = "Determines the base speed of zoom operations. The higher, the faster the zoom. The default is 1.05")
    @JIPipeParameter("zoom-base-speed")
    public double getZoomBaseSpeed() {
        return zoomBaseSpeed;
    }

    @JIPipeParameter("zoom-base-speed")
    public void setZoomBaseSpeed(double zoomBaseSpeed) {
        this.zoomBaseSpeed = zoomBaseSpeed;
    }

    @JIPipeDocumentation(name = "Zoom dynamic speed", description = "Determines the dynamic speed of zoom operations, which is added to the base speed based on the scroll wheel speed. The higher, the faster the dynamic zoom. The default is 0.1")
    @JIPipeParameter("zoom-dynamic-speed")
    public double getZoomDynamicSpeed() {
        return zoomDynamicSpeed;
    }

    @JIPipeParameter("zoom-dynamic-speed")
    public void setZoomDynamicSpeed(double zoomDynamicSpeed) {
        this.zoomDynamicSpeed = zoomDynamicSpeed;
    }
}
