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

package org.hkijena.jipipe.api.settings;

import org.hkijena.jipipe.JIPipe;

import javax.swing.*;

/**
 * Pre-defined application setting sheet categories to be used with {@link JIPipeDefaultApplicationsSettingsSheet}
 */
public enum JIPipeDefaultApplicationSettingsSheetCategory {
    General("General", JIPipe.RESOURCES.getIcon16("actions/wrench.png")),
    Environments("Connected services", JIPipe.RESOURCES.getIcon16("actions/environment.png")),
    Data("Data", JIPipe.RESOURCES.getIcon16("actions/update-cache.png")),
    Samples("Samples", JIPipe.RESOURCES.getIcon16("actions/template.png")),
    Plugins("Plugins", JIPipe.RESOURCES.getIcon16("actions/puzzle-piece.png")),
    ImageViewer("Image viewer", JIPipe.RESOURCES.getIcon16("actions/image.png")),
    UI("User interface", JIPipe.RESOURCES.getIcon16("actions/arrow-pointer.png"));

    private final String category;
    private final Icon icon;

    JIPipeDefaultApplicationSettingsSheetCategory(String category, Icon icon) {

        this.category = category;
        this.icon = icon;
    }

    public String getCategory() {
        return category;
    }

    public Icon getIcon() {
        return icon;
    }
}
