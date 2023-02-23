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

package org.hkijena.jipipe.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;

/**
 * All settings for {@link JIPipeGraphEditorUI}
 */
public class ImageViewerUISettings implements JIPipeParameterCollection {

    public static String ID = "image-viewer-ui";

    private final EventBus eventBus = new EventBus();
    private boolean showSideBar = true;
    private int defaultAnimationSpeed = 75;
    private boolean alwaysClearROIs = true;

    private double zoomBaseSpeed = 0.05;

    private double zoomDynamicSpeed = 0.1;

    public static ImageViewerUISettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, ImageViewerUISettings.class);
    }

    @JIPipeDocumentation(name = "Always clear ROIs", description = "If enabled, viewers will always clear ROI lists if new data is loaded.")
    @JIPipeParameter("always-clear-rois")
    public boolean isAlwaysClearROIs() {
        return alwaysClearROIs;
    }

    @JIPipeParameter("always-clear-rois")
    public void setAlwaysClearROIs(boolean alwaysClearROIs) {
        this.alwaysClearROIs = alwaysClearROIs;
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

    @JIPipeDocumentation(name = "Animation speed", description = "The default animation speed")
    @JIPipeParameter("default-animation-speed")
    public int getDefaultAnimationSpeed() {
        return defaultAnimationSpeed;
    }

    @JIPipeParameter("default-animation-speed")
    public void setDefaultAnimationSpeed(int defaultAnimationSpeed) {
        this.defaultAnimationSpeed = defaultAnimationSpeed;
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

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
