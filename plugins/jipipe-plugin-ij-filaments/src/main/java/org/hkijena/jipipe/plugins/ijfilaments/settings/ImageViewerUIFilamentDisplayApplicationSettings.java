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

package org.hkijena.jipipe.plugins.ijfilaments.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsPlugin;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentsDrawer;

import javax.swing.*;

public class ImageViewerUIFilamentDisplayApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static String ID = "filaments-image-viewer-ui-filaments-display";
    private FilamentsDrawer filamentDrawer = new FilamentsDrawer();
    private boolean showFilaments = true;

    public ImageViewerUIFilamentDisplayApplicationSettings() {
    }

    public static ImageViewerUIFilamentDisplayApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, ImageViewerUIFilamentDisplayApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Filament visualization")
    @JIPipeParameter("filament-drawer")
    public FilamentsDrawer getFilamentDrawer() {
        return filamentDrawer;
    }

    public void setFilamentDrawer(FilamentsDrawer filamentDrawer) {
        this.filamentDrawer = filamentDrawer;
    }

    @SetJIPipeDocumentation(name = "Show spots", description = "If enabled, filaments are visible")
    @JIPipeParameter("show-spots")
    public boolean isShowFilaments() {
        return showFilaments;
    }

    @JIPipeParameter("show-spots")
    public void setShowFilaments(boolean showFilaments) {
        this.showFilaments = showFilaments;
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
        return FilamentsPlugin.RESOURCES.getIconFromResources("data-type-filaments.png");
    }

    @Override
    public String getName() {
        return "Filaments display";
    }

    @Override
    public String getDescription() {
        return "Settings for the filaments manager component of the JIPipe image viewer";
    }
}
