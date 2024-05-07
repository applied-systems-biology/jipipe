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
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.ijtrackmate.TrackMatePlugin;
import org.hkijena.jipipe.plugins.ijtrackmate.utils.SpotDrawer;

import javax.swing.*;

public class ImageViewerUISpotsDisplayApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static String ID = "org.hkijena.jipipe:trackmate-image-viewer-ui-spots-display";
    private SpotDrawer spotDrawer = new SpotDrawer();
    private boolean showSpots = true;

    public ImageViewerUISpotsDisplayApplicationSettings() {
    }

    public static ImageViewerUISpotsDisplayApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, ImageViewerUISpotsDisplayApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Spot visualization")
    @JIPipeParameter("spot-drawer")
    public SpotDrawer getSpotDrawer() {
        return spotDrawer;
    }

    public void setSpotDrawer(SpotDrawer spotDrawer) {
        this.spotDrawer = spotDrawer;
    }

    @SetJIPipeDocumentation(name = "Show spots", description = "If enabled, spots are visible")
    @JIPipeParameter("show-spots")
    public boolean isShowSpots() {
        return showSpots;
    }

    @JIPipeParameter("show-spots")
    public void setShowSpots(boolean showSpots) {
        this.showSpots = showSpots;
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
