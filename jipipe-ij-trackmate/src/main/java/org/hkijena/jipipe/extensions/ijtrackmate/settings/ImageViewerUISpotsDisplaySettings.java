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
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.SpotDrawer;

public class ImageViewerUISpotsDisplaySettings implements JIPipeParameterCollection {
    public static String ID = "trackmate-image-viewer-ui-spots-display";
    private final EventBus eventBus = new EventBus();

    private SpotDrawer spotDrawer = new SpotDrawer();
    private boolean showSpots = true;

    public ImageViewerUISpotsDisplaySettings() {
    }

    public static ImageViewerUISpotsDisplaySettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, ImageViewerUISpotsDisplaySettings.class);
    }

    @JIPipeDocumentation(name = "Spot visualization")
    @JIPipeParameter("spot-drawer")
    public SpotDrawer getSpotDrawer() {
        return spotDrawer;
    }

    public void setSpotDrawer(SpotDrawer spotDrawer) {
        this.spotDrawer = spotDrawer;
    }

    @JIPipeDocumentation(name = "Show spots", description = "If enabled, spots are visible")
    @JIPipeParameter("show-spots")
    public boolean isShowSpots() {
        return showSpots;
    }

    @JIPipeParameter("show-spots")
    public void setShowSpots(boolean showSpots) {
        this.showSpots = showSpots;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
