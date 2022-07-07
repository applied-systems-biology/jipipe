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
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackDrawer;

public class ImageViewerUITracksDisplaySettings implements JIPipeParameterCollection {
    public static String ID = "trackmate-image-viewer-ui-tracks-display";
    private final EventBus eventBus = new EventBus();

    private TrackDrawer trackDrawer = new TrackDrawer();
    private boolean showTracks = true;

    public ImageViewerUITracksDisplaySettings() {
    }

    @JIPipeDocumentation(name = "Track visualization")
    @JIPipeParameter("track-drawer")
    public TrackDrawer getTrackDrawer() {
        return trackDrawer;
    }

    public void setTrackDrawer(TrackDrawer trackDrawer) {
        this.trackDrawer = trackDrawer;
    }

    @JIPipeDocumentation(name = "Show tracks", description = "If enabled, tracks are visible")
    @JIPipeParameter("show-tracks")
    public boolean isShowTracks() {
        return showTracks;
    }

    @JIPipeParameter("show-tracks")
    public void setShowTracks(boolean showTracks) {
        this.showTracks = showTracks;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public static ImageViewerUITracksDisplaySettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, ImageViewerUITracksDisplaySettings.class);
    }
}
