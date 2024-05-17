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

package org.hkijena.jipipe.plugins.ijtrackmate.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.ijtrackmate.TrackMatePlugin;
import org.hkijena.jipipe.plugins.ijtrackmate.utils.TrackDrawer;

import javax.swing.*;

public class ImageViewerUITracksDisplayApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static String ID = "org.hkijena.jipipe:trackmate-image-viewer-ui-tracks-display";
    private TrackDrawer trackDrawer = new TrackDrawer();
    private boolean showTracks = true;

    public ImageViewerUITracksDisplayApplicationSettings() {
    }

    public static ImageViewerUITracksDisplayApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, ImageViewerUITracksDisplayApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Track visualization")
    @JIPipeParameter("track-drawer")
    public TrackDrawer getTrackDrawer() {
        return trackDrawer;
    }

    public void setTrackDrawer(TrackDrawer trackDrawer) {
        this.trackDrawer = trackDrawer;
    }

    @SetJIPipeDocumentation(name = "Show tracks", description = "If enabled, tracks are visible")
    @JIPipeParameter("show-tracks")
    public boolean isShowTracks() {
        return showTracks;
    }

    @JIPipeParameter("show-tracks")
    public void setShowTracks(boolean showTracks) {
        this.showTracks = showTracks;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.ImageViewer;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return TrackMatePlugin.RESOURCES.getIconFromResources("trackmate-tracker.png");
    }

    @Override
    public String getName() {
        return "Tracks display";
    }

    @Override
    public String getDescription() {
        return "Settings for the track manager component of the JIPipe image viewer";
    }
}
